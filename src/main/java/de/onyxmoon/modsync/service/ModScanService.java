package de.onyxmoon.modsync.service;

import com.hypixel.hytale.codec.ExtraInfo;
import com.hypixel.hytale.codec.util.RawJsonReader;
import com.hypixel.hytale.common.plugin.PluginIdentifier;
import com.hypixel.hytale.common.plugin.PluginManifest;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.PluginClassLoader;
import com.hypixel.hytale.server.core.plugin.PluginManager;
import de.onyxmoon.modsync.ModSync;
import de.onyxmoon.modsync.api.ModListProvider;
import de.onyxmoon.modsync.api.ModListSource;
import de.onyxmoon.modsync.api.PluginType;
import de.onyxmoon.modsync.api.model.*;
import de.onyxmoon.modsync.api.model.provider.ModEntry;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

/**
 * Service for scanning and importing unmanaged mods.
 * Finds JAR/ZIP files in the mods folder that are not tracked by ModSync.
 */
public class ModScanService {
    private static final HytaleLogger LOGGER = HytaleLogger.get(ModSync.LOG_NAME);
    private final ModSync modSync;

    public ModScanService(ModSync modSync) {
        this.modSync = modSync;
    }

    /**
     * Scans for unmanaged mods in the mods and earlyplugins folders.
     *
     * @return List of unmanaged mods found
     */
    public List<UnmanagedMod> scanForUnmanagedMods() {
        ManagedModRegistry registry = modSync.getManagedModStorage().getRegistry();

        // Scan mods folder
        Path modsFolder = modSync.getDownloadService().getModsFolder();
        List<UnmanagedMod> unmanaged = new ArrayList<>(scanFolder(modsFolder, PluginType.PLUGIN, registry));

        // Scan early plugins folder
        Path earlyPluginsFolder = modSync.getDownloadService().getEarlyPluginsFolder();
        if (Files.exists(earlyPluginsFolder)) {
            unmanaged.addAll(scanFolder(earlyPluginsFolder, PluginType.EARLY_PLUGIN, registry));
        }

        return unmanaged;
    }

    private List<UnmanagedMod> scanFolder(Path folder, PluginType pluginType, ManagedModRegistry registry) {
        List<UnmanagedMod> unmanaged = new ArrayList<>();

        try (Stream<Path> files = Files.list(folder)) {
            files.filter(this::isPluginFile)
                    .filter(path -> !isManaged(path, registry))
                    .filter(path -> !isSelf(path))
                    .forEach(path -> {
                        try {
                            UnmanagedMod mod = scanPluginFile(path, pluginType);
                            if (mod != null) {
                                unmanaged.add(mod);
                            }
                        } catch (Exception e) {
                            LOGGER.atWarning().log("Failed to scan file: %s - %s", path.getFileName(), e.getMessage());
                        }
                    });
        } catch (IOException e) {
            LOGGER.atWarning().log("Failed to scan folder: %s - %s", folder, e.getMessage());
        }

        return unmanaged;
    }

    private boolean isPluginFile(Path path) {
        String fileName = path.toString().toLowerCase();
        return fileName.endsWith(".jar") || fileName.endsWith(".zip");
    }

    private boolean isManaged(Path path, ManagedModRegistry registry) {
        String filePath = path.toString();
        return registry.findByFilePath(filePath).isPresent();
    }

    private boolean isSelf(Path path) {
        // Don't include ModSync itself
        String fileName = path.getFileName().toString().toLowerCase();
        return fileName.startsWith("modsync-") || fileName.equals("modsync.jar");
    }

    private UnmanagedMod scanPluginFile(Path path, PluginType pluginType) {
        try {
            String fileName = path.getFileName().toString();
            long fileSize = Files.size(path);
            String fileHash = calculateHash(path);
            PluginIdentifier identifier = readIdentifier(path);

            return new UnmanagedMod(path, fileName, identifier, fileHash, fileSize, pluginType);
        } catch (IOException e) {
            LOGGER.atWarning().log("Failed to read file: %s - %s", path.getFileName(), e.getMessage());
            return null;
        }
    }

    /**
     * Attempts to find a matching CurseForge entry for an unmanaged mod.
     *
     * @param unmanagedMod The unmanaged mod to match
     * @return ImportMatch with the result
     */
    public CompletableFuture<ImportMatch> findMatch(UnmanagedMod unmanagedMod) {
        ModListProvider provider = modSync.getProviderRegistry().getProvider(ModListSource.CURSEFORGE);
        String apiKey = modSync.getConfigStorage().getConfig().getApiKey(ModListSource.CURSEFORGE);

        if (provider == null || apiKey == null || apiKey.isEmpty()) {
            return CompletableFuture.completedFuture(ImportMatch.noMatch(unmanagedMod));
        }

        // Strategy 1: Try slug-based lookup from identifier
        if (unmanagedMod.identifier() != null) {
            String slug = deriveSlug(unmanagedMod.getDisplayName());
            return trySlugMatch(provider, apiKey, unmanagedMod, slug)
                    .thenCompose(match -> {
                        if (match.hasMatch()) {
                            return CompletableFuture.completedFuture(match);
                        }
                        // Strategy 2: Fall back to name search
                        return tryNameSearch(provider, apiKey, unmanagedMod);
                    }).exceptionallyCompose(ex -> tryNameSearch(provider, apiKey, unmanagedMod));
        }

        // No identifier - try name search based on filename
        return tryNameSearch(provider, apiKey, unmanagedMod);
    }

    private CompletableFuture<ImportMatch> trySlugMatch(
            ModListProvider provider, String apiKey, UnmanagedMod unmanagedMod, String slug) {

        return provider.fetchModBySlug(apiKey, slug)
                .thenApply(entry -> ImportMatch.exactMatch(unmanagedMod, entry, "Slug match: " + slug))
                .exceptionally(ex -> {
                    LOGGER.atFine().log("Slug lookup failed for %s: %s", slug, ex.getMessage());
                    return ImportMatch.noMatch(unmanagedMod);
                });
    }

    private CompletableFuture<ImportMatch> tryNameSearch(
            ModListProvider provider, String apiKey, UnmanagedMod unmanagedMod) {

        // getDisplayName() already handles identifier != null case
        String searchTerm = unmanagedMod.getDisplayName();

        return provider.searchMods(apiKey, searchTerm)
                .handle((results, ex) -> {
                    if (ex != null) {
                        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                        if (cause instanceof UnsupportedOperationException) {
                            return new ImportMatch(unmanagedMod, null, ImportMatchConfidence.NONE,
                                    provider.getDisplayName() + " does not support search");
                        }
                        LOGGER.atFine().log("Name search failed for %s: %s", searchTerm, cause.getMessage());
                        return ImportMatch.noMatch(unmanagedMod);
                    }

                    if (results == null || results.isEmpty()) {
                        return ImportMatch.noMatch(unmanagedMod);
                    }

                    // Check for exact name match
                    for (ModEntry entry : results) {
                        if (entry.getName().equalsIgnoreCase(searchTerm)) {
                            return ImportMatch.highConfidenceMatch(unmanagedMod, entry, "Name match: " + searchTerm);
                        }
                    }

                    // Check for slug match
                    String slug = deriveSlug(searchTerm);
                    for (ModEntry entry : results) {
                        if (entry.getSlug() != null && entry.getSlug().equalsIgnoreCase(slug)) {
                            return ImportMatch.highConfidenceMatch(unmanagedMod, entry, "Slug match: " + slug);
                        }
                    }

                    // Return first result as low confidence
                    return ImportMatch.lowConfidenceMatch(unmanagedMod, results.getFirst(),
                            "Best search result for: " + searchTerm);
                });
    }

    /**
     * Imports an unmanaged mod using a matched ModEntry.
     *
     * @param unmanagedMod The unmanaged mod to import
     * @param modEntry     The matched entry from the provider
     */
    public void importWithEntry(UnmanagedMod unmanagedMod, ModEntry modEntry, ModListSource source) {
        // Create InstalledState from the unmanaged mod
        InstalledState installedState = InstalledState.builder()
                .identifier(unmanagedMod.identifier())
                .filePath(unmanagedMod.filePath().toString())
                .fileName(unmanagedMod.fileName())
                .fileSize(unmanagedMod.fileSize())
                .fileHash(unmanagedMod.fileHash())
                .installedAt(Instant.now())
                .lastChecked(Instant.now())
                // Version info from modEntry's latest version
                .installedVersionId(modEntry.getLatestVersion() != null ? modEntry.getLatestVersion().getVersionId() : null)
                .installedVersionNumber(modEntry.getLatestVersion() != null ? modEntry.getLatestVersion().getVersionNumber() : null)
                .build();

        // Create ManagedMod - use modEntry.getPluginType() as source of truth,
        // but fall back to unmanagedMod.pluginType() if not available
        PluginType pluginType = modEntry.getPluginType();

        ModListSource effectiveSource = source != null ? source : ModListSource.CURSEFORGE;

        ManagedMod managedMod = ManagedMod.builder()
                .modId(modEntry.getModId())
                .source(effectiveSource)
                .name(modEntry.getName())
                .slug(modEntry.getSlug())
                .pluginType(pluginType)
                .addedAt(Instant.now())
                .addedViaUrl(null) // Imported, no URL
                .installedState(installedState)
                .build();

        // Add to storage using the same method as AddCommand
        modSync.getManagedModStorage().addMod(managedMod);

        LOGGER.atInfo().log("Imported %s as %s", unmanagedMod.fileName(), modEntry.getName());

    }

    /**
     * Derives a URL slug from a mod name.
     * Converts "MyModName" or "My Mod Name" to "my-mod-name".
     */
    private String deriveSlug(String name) {
        if (name == null) return null;

        // Insert dash before uppercase letters (for CamelCase)
        String slug = name.replaceAll("([a-z])([A-Z])", "$1-$2");
        // Replace spaces and underscores with dashes
        slug = slug.replaceAll("[\\s_]+", "-");
        // Remove non-alphanumeric except dashes
        slug = slug.replaceAll("[^a-zA-Z0-9-]", "");
        // Convert to lowercase
        slug = slug.toLowerCase();
        // Remove multiple consecutive dashes
        slug = slug.replaceAll("-+", "-");
        // Remove leading/trailing dashes
        slug = slug.replaceAll("^-|-$", "");

        return slug;
    }

    private String calculateHash(Path filePath) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] fileBytes = Files.readAllBytes(filePath);
            byte[] hashBytes = digest.digest(fileBytes);
            return "sha256:" + HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    private PluginIdentifier readIdentifier(Path filePath) {
        try {
            URL url = filePath.toUri().toURL();
            URL resource;
            try (PluginClassLoader pluginClassLoader = new PluginClassLoader(new PluginManager(), false, url)) {
                resource = pluginClassLoader.findResource("manifest.json");
            }
            if (resource == null) {
                return null;
            }

            try (
                    InputStream stream = resource.openStream();
                    InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)
            ) {
                char[] buffer = RawJsonReader.READ_BUFFER.get();
                RawJsonReader rawJsonReader = new RawJsonReader(reader, buffer);
                ExtraInfo extraInfo = ExtraInfo.THREAD_LOCAL.get();
                PluginManifest manifest = PluginManifest.CODEC.decodeJson(rawJsonReader, extraInfo);
                assert manifest != null;
                return new PluginIdentifier(manifest.getGroup(), manifest.getName());
            }
        } catch (Exception e) {
            LOGGER.atFine().log("Could not read manifest from %s: %s", filePath.getFileName(), e.getMessage());
            return null;
        }
    }
}

package de.onyxmoon.modsync.service;

import com.hypixel.hytale.codec.ExtraInfo;
import com.hypixel.hytale.codec.util.RawJsonReader;
import com.hypixel.hytale.common.plugin.PluginIdentifier;
import com.hypixel.hytale.common.plugin.PluginManifest;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.PluginClassLoader;
import com.hypixel.hytale.server.core.plugin.PluginManager;
import de.onyxmoon.modsync.ModSync;
import de.onyxmoon.modsync.api.model.InstalledMod;
import de.onyxmoon.modsync.api.model.ManagedModEntry;
import de.onyxmoon.modsync.api.model.ModVersion;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.HexFormat;
import java.util.logging.Level;

/**
 * Service for downloading and installing mods.
 */
public class ModDownloadService {
    private static final HytaleLogger LOGGER = HytaleLogger.get(ModSync.LOG_NAME);
    private final ModSync modSync;
    private final Path modsFolder;
    private final HttpClient httpClient;

    public ModDownloadService(ModSync modSync, Path modsFolder) {
        this.modSync = modSync;
        this.modsFolder = modsFolder;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        // Ensure mods folder exists
        try {
            Files.createDirectories(modsFolder);
        } catch (IOException e) {
            LOGGER.atSevere().log("Failed to create mods folder: {}", modsFolder, e);
        }
    }

    /**
     * Download and install a mod.
     */
    public CompletableFuture<InstalledMod> downloadAndInstall(
            ManagedModEntry entry,
            ModVersion version) {

        String downloadUrl = version.getDownloadUrl();
        if (downloadUrl == null || downloadUrl.isEmpty()) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException("No download URL available for " + entry.getName())
            );
        }

        String fileName = version.getFileName();
        Path targetPath = modsFolder.resolve(fileName);

        LOGGER.atInfo().log("Downloading {} to {}", entry.getName(), targetPath);

        return downloadFile(downloadUrl, targetPath)
                .thenApply(filePath -> {
                    try {
                        String hash = calculateHash(filePath);
                        long fileSize = Files.size(filePath);
                        var manifest = readManifest(filePath);
                        var identifier = new PluginIdentifier(manifest.getGroup(), manifest.getName());

                        InstalledMod installed = InstalledMod.builder()
                                .modId(entry.getModId())
                                .name(entry.getName())
                                .slug(entry.getSlug())
                                .identifier(identifier)
                                .source(entry.getSource())
                                .installedVersionId(version.getVersionId())
                                .installedVersionNumber(version.getVersionNumber())
                                .filePath(filePath.toString())
                                .fileName(fileName)
                                .fileSize(fileSize)
                                .fileHash(hash)
                                .installedAt(Instant.now())
                                .lastChecked(Instant.now())
                                .build();

                        modSync.getInstalledModStorage().addMod(installed);

                        LOGGER.atInfo().log("Successfully installed {} ({})", entry.getName(), version.getVersionNumber());
                        return installed;
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to process downloaded file", e);
                    }
                });
    }

    private CompletableFuture<Path> downloadFile(String url, Path targetPath) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream())
                .thenApply(response -> {
                    if (response.statusCode() != 200) {
                        throw new RuntimeException("Download failed with status: " + response.statusCode());
                    }

                    try (InputStream inputStream = response.body()) {
                        Path tempPath = targetPath.getParent().resolve(targetPath.getFileName() + ".tmp");
                        Files.copy(inputStream, tempPath, StandardCopyOption.REPLACE_EXISTING);
                        Files.move(tempPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                        return targetPath;
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to save downloaded file", e);
                    }
                });
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

    /**
     * Delete an installed mod. Unloads if loaded, then deletes or schedules for deletion on next startup.
     * @return true if file was deleted immediately, false if restart is required
     */
    public CompletableFuture<Boolean> deleteMod(InstalledMod mod) {
        return CompletableFuture.supplyAsync(() -> {
            // Unload if currently loaded
            if (modSync.getPluginManager().getPlugin(mod.getIdentifier()) != null &&
                    Objects.requireNonNull(modSync.getPluginManager().getPlugin(mod.getIdentifier())).isEnabled()) {
                if (!modSync.getPluginManager().unload(mod.getIdentifier())) {
                    throw new RuntimeException("Failed to unload mod");
                }
            }

            Path filePath = Path.of(mod.getFilePath());
            boolean deletedImmediately = true;

            // Try to delete immediately
            if (Files.exists(filePath)) {
                try {
                    Files.delete(filePath);
                    LOGGER.atInfo().log("Deleted mod file: {}", filePath);
                } catch (IOException e) {
                    // File is locked, schedule for deletion on next startup
                    LOGGER.atWarning().log("File locked, scheduling for deletion on next startup: {}", filePath);
                    modSync.addPendingDeletion(filePath.toString());
                    deletedImmediately = false;
                }
            }

            // Remove from storage
            modSync.getInstalledModStorage().removeMod(mod.getSourceId());
            return deletedImmediately;
        });
    }

    private PluginManifest readManifest(Path filePath) {
        try {
            URL url = filePath.toUri().toURL();
            URL resource;
            try (PluginClassLoader pluginClassLoader = new PluginClassLoader(new PluginManager(), false, url)) {
                resource = pluginClassLoader.findResource("manifest.json");
            }
            if (resource == null) {
                LOGGER.at(Level.SEVERE).log("Failed to load pending plugin from '%s'. Failed to load manifest file!", filePath.toString());
                return null;
            }

            try (
                    InputStream stream = resource.openStream();
                    InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)
            ) {
                char[] buffer = RawJsonReader.READ_BUFFER.get();
                RawJsonReader rawJsonReader = new RawJsonReader(reader, buffer);
                ExtraInfo extraInfo = ExtraInfo.THREAD_LOCAL.get();
                return PluginManifest.CODEC.decodeJson(rawJsonReader, extraInfo);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Path getModsFolder() {
        return modsFolder;
    }
}
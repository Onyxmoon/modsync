package de.onyxmoon.modsync.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hypixel.hytale.common.plugin.PluginIdentifier;
import com.hypixel.hytale.logger.HytaleLogger;
import de.onyxmoon.modsync.ModSync;
import de.onyxmoon.modsync.api.ModListSource;
import de.onyxmoon.modsync.api.PluginType;
import de.onyxmoon.modsync.api.model.InstalledState;
import de.onyxmoon.modsync.api.model.ManagedMod;
import de.onyxmoon.modsync.api.model.ManagedModRegistry;
import de.onyxmoon.modsync.storage.model.LockFile;
import de.onyxmoon.modsync.storage.model.ModListFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Unified storage for managed mods using two files:
 * <ul>
 *   <li><b>mods.json</b> - The mod list (what the user wants to track). Versionable, shareable.</li>
 *   <li><b>mods.lock.json</b> - Installation state (what is installed). Machine-specific.</li>
 * </ul>
 *
 * Includes migration logic to convert old format files on first load.
 */
public class ManagedModStorage {
    private static final HytaleLogger LOGGER = HytaleLogger.get(ModSync.LOG_NAME);

    /** Schema version for mods.json and mods.lock.json files */
    public static final int SCHEMA_VERSION = 1;

    private final Path modsJsonPath;
    private final Path modsLockPath;
    private final Path oldManagedModsPath;
    private final Path oldInstalledModsPath;
    private final Gson gson;
    private ManagedModRegistry registry;

    public ManagedModStorage(Path dataFolder) {
        this.modsJsonPath = dataFolder.resolve("mods.json");
        this.modsLockPath = dataFolder.resolve("mods.lock.json");
        this.oldManagedModsPath = dataFolder.resolve("managed_mods.json");
        this.oldInstalledModsPath = dataFolder.resolve("installed_mods.json");
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(Instant.class, new InstantTypeAdapter())
                .create();
        this.registry = load().orElse(ManagedModRegistry.empty());
    }

    /**
     * Save the current registry to disk (both mods.json and mods.lock.json).
     */
    public void save() {
        save(registry);
    }

    /**
     * Save the given registry to disk.
     */
    public void save(ManagedModRegistry registry) {
        this.registry = registry;
        try {
            Files.createDirectories(modsJsonPath.getParent());

            // Build mods.json (without installation state)
            ModListFile modListFile = new ModListFile();
            modListFile.setVersion(SCHEMA_VERSION);
            modListFile.setName(registry.getName());
            modListFile.setCreatedAt(registry.getCreatedAt());
            modListFile.setLastModifiedAt(registry.getLastModifiedAt());

            List<ModListFile.ModListEntry> entries = new ArrayList<>();
            for (ManagedMod mod : registry.getAll()) {
                ModListFile.ModListEntry entry = new ModListFile.ModListEntry(
                        mod.getModId(),
                        mod.getName(),
                        mod.getSlug(),
                        mod.getSource(),
                        mod.getPluginType(),
                        mod.getDesiredVersionId(),
                        mod.getAddedAt(),
                        mod.getAddedViaUrl()
                );
                entries.add(entry);
            }
            modListFile.setMods(entries);

            // Build mods.lock.json (only installation state)
            LockFile lockFile = new LockFile();
            lockFile.setVersion(SCHEMA_VERSION);
            lockFile.setLockedAt(Instant.now());

            Map<String, LockFile.LockedInstallation> installations = new HashMap<>();
            for (ManagedMod mod : registry.getAll()) {
                if (mod.isInstalled()) {
                    InstalledState state = mod.getInstalledState().orElseThrow();
                    LockFile.LockedInstallation installation = new LockFile.LockedInstallation(
                            state.getIdentifier(),
                            state.getInstalledVersionId(),
                            state.getInstalledVersionNumber(),
                            state.getFilePath(),
                            state.getFileName(),
                            state.getFileSize(),
                            state.getFileHash(),
                            state.getInstalledAt(),
                            state.getLastChecked()
                    );
                    installations.put(mod.getSourceId(), installation);
                }
            }
            lockFile.setInstallations(installations);

            // Write both files
            Files.writeString(modsJsonPath, gson.toJson(modListFile));
            Files.writeString(modsLockPath, gson.toJson(lockFile));

            LOGGER.atInfo().log("Saved %d mods to mods.json, %d installations to mods.lock.json",
                    entries.size(), installations.size());
        } catch (IOException e) {
            LOGGER.atSevere().withCause(e).log("Failed to save managed mods");
            throw new RuntimeException("Failed to save managed mods", e);
        }
    }

    /**
     * Load the registry from disk by merging mods.json and mods.lock.json.
     * Attempts migration from old format (managed_mods.json + installed_mods.json) if new files don't exist.
     */
    public Optional<ManagedModRegistry> load() {
        // Check for old format and migrate if needed
        if (!Files.exists(modsJsonPath)) {
            Optional<ManagedModRegistry> migrated = migrateFromOldFormat();
            if (migrated.isPresent()) {
                this.registry = migrated.get();
                save();
                cleanupOldFiles();
                return migrated;
            }

            LOGGER.atInfo().log("No mods.json found, starting fresh");
            return Optional.empty();
        }

        try {
            // Load mods.json
            String modsJson = Files.readString(modsJsonPath);
            ModListFile modListFile = gson.fromJson(modsJson, ModListFile.class);

            // Load mods.lock.json (optional - may not exist if nothing installed)
            LockFile lockFile = new LockFile();
            if (Files.exists(modsLockPath)) {
                String lockJson = Files.readString(modsLockPath);
                lockFile = gson.fromJson(lockJson, LockFile.class);
            }

            // Merge into ManagedModRegistry
            ManagedModRegistry.Builder builder = ManagedModRegistry.builder()
                    .name(modListFile.getName() != null ? modListFile.getName() : "default")
                    .createdAt(modListFile.getCreatedAt() != null ? modListFile.getCreatedAt() : Instant.now())
                    .lastModifiedAt(modListFile.getLastModifiedAt());

            Map<String, LockFile.LockedInstallation> installations = lockFile.getInstallations();
            if (installations == null) {
                installations = new HashMap<>();
            }

            for (ModListFile.ModListEntry entry : modListFile.getMods()) {
                ManagedMod.Builder modBuilder = ManagedMod.builder()
                        .modId(entry.getModId())
                        .name(entry.getName())
                        .slug(entry.getSlug())
                        .source(entry.getSource())
                        .pluginType(entry.getPluginType() != null ? entry.getPluginType() : PluginType.PLUGIN)
                        .desiredVersionId(entry.getDesiredVersionId())
                        .addedAt(entry.getAddedAt())
                        .addedViaUrl(entry.getAddedViaUrl());

                // Check if there's a corresponding lock entry
                String sourceId = entry.getSourceId();
                LockFile.LockedInstallation installation = installations.get(sourceId);
                if (installation != null) {
                    InstalledState state = InstalledState.builder()
                            .identifier(installation.getIdentifier())
                            .installedVersionId(installation.getInstalledVersionId())
                            .installedVersionNumber(installation.getInstalledVersionNumber())
                            .filePath(installation.getFilePath())
                            .fileName(installation.getFileName())
                            .fileSize(installation.getFileSize())
                            .fileHash(installation.getFileHash())
                            .installedAt(installation.getInstalledAt())
                            .lastChecked(installation.getLastChecked())
                            .build();
                    modBuilder.installedState(state);
                }

                builder.addMod(modBuilder.build());
            }

            ManagedModRegistry loadedRegistry = builder.build();
            this.registry = loadedRegistry;
            LOGGER.atInfo().log("Loaded %d mods from mods.json, %d installations from mods.lock.json",
                    modListFile.getMods().size(), installations.size());
            return Optional.of(loadedRegistry);
        } catch (IOException e) {
            LOGGER.atSevere().withCause(e).log("Failed to load managed mods");
            return Optional.empty();
        }
    }

    /**
     * Migrate from old format (managed_mods.json + installed_mods.json).
     */
    private Optional<ManagedModRegistry> migrateFromOldFormat() {
        boolean hasManagedMods = Files.exists(oldManagedModsPath);
        boolean hasInstalledMods = Files.exists(oldInstalledModsPath);

        if (!hasManagedMods && !hasInstalledMods) {
            return Optional.empty();
        }

        LOGGER.atInfo().log("Migrating from old separate storage format...");

        ManagedModRegistry.Builder builder = ManagedModRegistry.builder()
                .name("default")
                .createdAt(Instant.now());

        // Load old managed mods
        OldManagedModList oldManaged = null;
        if (hasManagedMods) {
            try {
                Gson migrationGson = new GsonBuilder()
                        .registerTypeAdapter(Instant.class, new InstantTypeAdapter())
                        .create();
                String json = Files.readString(oldManagedModsPath);
                oldManaged = migrationGson.fromJson(json, OldManagedModList.class);
                if (oldManaged != null && oldManaged.createdAt != null) {
                    builder.createdAt(oldManaged.createdAt);
                }
                LOGGER.atInfo().log("Found %d managed mods in old format",
                        oldManaged != null && oldManaged.mods != null ? oldManaged.mods.size() : 0);
            } catch (IOException e) {
                LOGGER.atWarning().log("Failed to read old managed_mods.json: %s", e.getMessage());
            }
        }

        // Load old installed mods
        OldInstalledModList oldInstalled = null;
        if (hasInstalledMods) {
            try {
                Gson installedGson = new GsonBuilder()
                        .registerTypeAdapter(Instant.class, new InstantTypeAdapter())
                        .create();
                String json = Files.readString(oldInstalledModsPath);
                oldInstalled = installedGson.fromJson(json, OldInstalledModList.class);
                LOGGER.atInfo().log("Found %d installed mods in old format",
                        oldInstalled != null && oldInstalled.mods != null ? oldInstalled.mods.size() : 0);
            } catch (IOException e) {
                LOGGER.atWarning().log("Failed to read old installed_mods.json: %s", e.getMessage());
            }
        }

        // Merge old data into new format
        if (oldManaged != null && oldManaged.mods != null) {
            for (OldManagedModEntry entry : oldManaged.mods) {
                ManagedMod.Builder modBuilder = ManagedMod.builder()
                        .modId(entry.modId)
                        .name(entry.name)
                        .slug(entry.slug)
                        .source(entry.source)
                        .pluginType(entry.pluginType != null ? entry.pluginType : PluginType.PLUGIN)
                        .desiredVersionId(entry.desiredVersionId)
                        .addedAt(entry.addedAt)
                        .addedViaUrl(entry.addedViaUrl);

                // Check if this mod is installed
                if (oldInstalled != null && oldInstalled.mods != null) {
                    String sourceId = entry.source.name().toLowerCase() + ":" + entry.modId;
                    for (OldInstalledMod installed : oldInstalled.mods) {
                        String installedSourceId = installed.source.name().toLowerCase() + ":" + installed.modId;
                        if (sourceId.equals(installedSourceId)) {
                            InstalledState state = InstalledState.builder()
                                    .identifier(installed.identifier)
                                    .installedVersionId(installed.installedVersionId)
                                    .installedVersionNumber(installed.installedVersionNumber)
                                    .filePath(installed.filePath)
                                    .fileName(installed.fileName)
                                    .fileSize(installed.fileSize)
                                    .fileHash(installed.fileHash)
                                    .installedAt(installed.installedAt)
                                    .lastChecked(installed.lastChecked)
                                    .build();
                            modBuilder.installedState(state);
                            break;
                        }
                    }
                }

                builder.addMod(modBuilder.build());
            }
        }

        ManagedModRegistry migrated = builder.lastModifiedAt(Instant.now()).build();
        LOGGER.atInfo().log("Migration complete: %d mods migrated", migrated.size());
        return Optional.of(migrated);
    }

    /**
     * Rename old files to .bak after successful migration.
     */
    private void cleanupOldFiles() {
        try {
            if (Files.exists(oldManagedModsPath)) {
                Path backup = oldManagedModsPath.resolveSibling("managed_mods.json.bak");
                Files.move(oldManagedModsPath, backup);
                LOGGER.atInfo().log("Backed up old managed_mods.json");
            }
            if (Files.exists(oldInstalledModsPath)) {
                Path backup = oldInstalledModsPath.resolveSibling("installed_mods.json.bak");
                Files.move(oldInstalledModsPath, backup);
                LOGGER.atInfo().log("Backed up old installed_mods.json");
            }
        } catch (IOException e) {
            LOGGER.atWarning().log("Failed to backup old files: %s", e.getMessage());
        }
    }

    /**
     * Get the current registry.
     */
    public ManagedModRegistry getRegistry() {
        return registry;
    }

    /**
     * Find a mod by its plugin identifier string (format: "group:name").
     */
    public Optional<ManagedMod> findByIdentifier(String identifier) {
        return registry.findByIdentifier(identifier);
    }

    /**
     * Find a mod by its source ID.
     */
    public Optional<ManagedMod> findBySourceId(String sourceId) {
        return registry.findBySourceId(sourceId);
    }

    /**
     * Add a mod to the registry and save.
     */
    public void addMod(ManagedMod mod) {
        this.registry = registry.toBuilder()
                .addMod(mod)
                .lastModifiedAt(Instant.now())
                .build();
        save();
    }

    /**
     * Remove a mod from the registry and save.
     */
    public void removeMod(String sourceId) {
        this.registry = registry.toBuilder()
                .removeMod(sourceId)
                .lastModifiedAt(Instant.now())
                .build();
        save();
    }

    /**
     * Update a mod in the registry and save.
     */
    public void updateMod(ManagedMod mod) {
        this.registry = registry.toBuilder()
                .removeMod(mod.getSourceId())
                .addMod(mod)
                .lastModifiedAt(Instant.now())
                .build();
        save();
    }

    /**
     * Reload the registry from disk.
     */
    public void reload() {
        this.registry = load().orElse(ManagedModRegistry.empty());
    }

    // ===== Migration POJOs (private, for reading old format only) =====

    private static class OldManagedModList {
        String name;
        List<OldManagedModEntry> mods;
        Instant createdAt;
        Instant lastModifiedAt;
    }

    private static class OldManagedModEntry {
        String modId;
        ModListSource source;
        String slug;
        String name;
        PluginType pluginType;
        String desiredVersionId;
        Instant addedAt;
        String addedViaUrl;
    }

    private static class OldInstalledModList {
        List<OldInstalledMod> mods;
        Instant lastScanAt;
    }

    private static class OldInstalledMod {
        String modId;
        String name;
        String slug;
        PluginIdentifier identifier;
        ModListSource source;
        PluginType pluginType;
        String installedVersionId;
        String installedVersionNumber;
        String filePath;
        String fileName;
        long fileSize;
        String fileHash;
        Instant installedAt;
        Instant lastChecked;
    }
}
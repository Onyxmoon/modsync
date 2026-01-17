package de.onyxmoon.modsync.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hypixel.hytale.logger.HytaleLogger;
import de.onyxmoon.modsync.ModSync;
import de.onyxmoon.modsync.api.model.InstalledMod;
import de.onyxmoon.modsync.api.model.InstalledModRegistry;
import de.onyxmoon.modsync.storage.model.StoredInstalledMods;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Optional;

/**
 * JSON-based storage for installed mods registry.
 */
public class InstalledModStorage {
    private static final HytaleLogger LOGGER = HytaleLogger.get(ModSync.LOG_NAME);
    private final Path storagePath;
    private final Gson gson;
    private InstalledModRegistry registry;

    public InstalledModStorage(Path dataFolder) {
        this.storagePath = dataFolder.resolve("installed_mods.json");
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(Instant.class, new InstantTypeAdapter())
                .create();
        this.registry = load().orElse(InstalledModRegistry.builder().build());
    }

    /**
     * Save the current registry to disk.
     */
    public void save() {
        save(registry);
    }

    /**
     * Save the given registry to disk.
     */
    public void save(InstalledModRegistry registry) {
        this.registry = registry;
        try {
            Files.createDirectories(storagePath.getParent());
            StoredInstalledMods stored = new StoredInstalledMods(
                    new ArrayList<>(registry.getAll()),
                    registry.getLastScanAt(),
                    Instant.now()
            );
            String json = gson.toJson(stored);
            Files.writeString(storagePath, json);
            LOGGER.atInfo().log("Installed mods saved successfully ({} mods)", registry.size());
        } catch (IOException e) {
            LOGGER.atSevere().log("Failed to save installed mods", e);
            throw new RuntimeException("Failed to save installed mods", e);
        }
    }

    /**
     * Load the registry from disk.
     */
    public Optional<InstalledModRegistry> load() {
        if (!Files.exists(storagePath)) {
            LOGGER.atInfo().log("No installed mods file found, starting fresh");
            return Optional.empty();
        }

        try {
            String json = Files.readString(storagePath);
            StoredInstalledMods stored = gson.fromJson(json, StoredInstalledMods.class);

            InstalledModRegistry.Builder builder = InstalledModRegistry.builder()
                    .lastScanAt(stored.getLastScanAt());

            if (stored.getMods() != null) {
                for (InstalledMod mod : stored.getMods()) {
                    builder.addMod(mod);
                }
            }

            InstalledModRegistry loadedRegistry = builder.build();
            this.registry = loadedRegistry;
            LOGGER.atInfo().log("Installed mods loaded successfully ({} mods)", loadedRegistry.size());
            return Optional.of(loadedRegistry);
        } catch (IOException e) {
            LOGGER.atSevere().log("Failed to load installed mods", e);
            return Optional.empty();
        }
    }

    /**
     * Get the current registry.
     */
    public InstalledModRegistry getRegistry() {
        return registry;
    }

    /**
     * Add a mod to the registry and save.
     */
    public void addMod(InstalledMod mod) {
        this.registry = registry.toBuilder()
                .addMod(mod)
                .lastScanAt(Instant.now())
                .build();
        save();
    }

    /**
     * Remove a mod from the registry and save.
     */
    public void removeMod(String sourceId) {
        this.registry = registry.toBuilder()
                .removeMod(sourceId)
                .lastScanAt(Instant.now())
                .build();
        save();
    }

    /**
     * Update a mod in the registry and save.
     */
    public void updateMod(InstalledMod mod) {
        // Remove old and add new (same effect as update)
        this.registry = registry.toBuilder()
                .removeMod(mod.getSourceId())
                .addMod(mod)
                .lastScanAt(Instant.now())
                .build();
        save();
    }

    /**
     * Reload the registry from disk.
     */
    public void reload() {
        this.registry = load().orElse(InstalledModRegistry.builder().build());
    }
}
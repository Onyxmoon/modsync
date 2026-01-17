package de.onyxmoon.modsync.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hypixel.hytale.logger.HytaleLogger;
import de.onyxmoon.modsync.ModSync;
import de.onyxmoon.modsync.api.model.ManagedModEntry;
import de.onyxmoon.modsync.api.model.ManagedModList;
import de.onyxmoon.modsync.storage.model.StoredManagedModList;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Optional;

/**
 * JSON-based storage for managed mod lists.
 */
public class ManagedModListStorage {
    private static final HytaleLogger LOGGER = HytaleLogger.get(ModSync.LOG_NAME);
    private final Path storagePath;
    private final Gson gson;
    private ManagedModList modList;

    public ManagedModListStorage(Path dataFolder) {
        this.storagePath = dataFolder.resolve("managed_mods.json");
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(Instant.class, new InstantTypeAdapter())
                .create();
        this.modList = load().orElse(ManagedModList.builder().name("default").build());
    }

    /**
     * Save the current mod list to disk.
     */
    public void save() {
        save(modList);
    }

    /**
     * Save the given mod list to disk.
     */
    public void save(ManagedModList modList) {
        this.modList = modList;
        try {
            Files.createDirectories(storagePath.getParent());
            StoredManagedModList stored = new StoredManagedModList(
                    modList.getName(),
                    new ArrayList<>(modList.getMods()),
                    modList.getCreatedAt(),
                    modList.getLastModifiedAt(),
                    Instant.now()
            );
            String json = gson.toJson(stored);
            Files.writeString(storagePath, json);
            LOGGER.atInfo().log("Managed mod list saved successfully ({} mods)", modList.size());
        } catch (IOException e) {
            LOGGER.atSevere().log("Failed to save managed mod list", e);
            throw new RuntimeException("Failed to save managed mod list", e);
        }
    }

    /**
     * Load the mod list from disk.
     */
    public Optional<ManagedModList> load() {
        if (!Files.exists(storagePath)) {
            LOGGER.atInfo().log("No managed mod list file found, starting fresh");
            return Optional.empty();
        }

        try {
            String json = Files.readString(storagePath);
            StoredManagedModList stored = gson.fromJson(json, StoredManagedModList.class);

            ManagedModList.Builder builder = ManagedModList.builder()
                    .name(stored.getName() != null ? stored.getName() : "default")
                    .createdAt(stored.getCreatedAt() != null ? stored.getCreatedAt() : Instant.now())
                    .lastModifiedAt(stored.getLastModifiedAt());

            if (stored.getMods() != null) {
                for (ManagedModEntry mod : stored.getMods()) {
                    builder.addMod(mod);
                }
            }

            ManagedModList loadedList = builder.build();
            this.modList = loadedList;
            LOGGER.atInfo().log("Managed mod list loaded successfully ({} mods)", loadedList.size());
            return Optional.of(loadedList);
        } catch (IOException e) {
            LOGGER.atSevere().log("Failed to load managed mod list", e);
            return Optional.empty();
        }
    }

    /**
     * Get the current mod list.
     */
    public ManagedModList getModList() {
        return modList;
    }

    /**
     * Add a mod to the list and save.
     */
    public void addMod(ManagedModEntry mod) {
        this.modList = modList.toBuilder()
                .addMod(mod)
                .lastModifiedAt(Instant.now())
                .build();
        save();
    }

    /**
     * Remove a mod from the list and save.
     */
    public void removeMod(String sourceId) {
        this.modList = modList.toBuilder()
                .removeMod(sourceId)
                .lastModifiedAt(Instant.now())
                .build();
        save();
    }

    /**
     * Update a mod in the list and save.
     */
    public void updateMod(ManagedModEntry mod) {
        this.modList = modList.toBuilder()
                .removeMod(mod.getSourceId())
                .addMod(mod)
                .lastModifiedAt(Instant.now())
                .build();
        save();
    }

    /**
     * Reload the mod list from disk.
     */
    public void reload() {
        this.modList = load().orElse(ManagedModList.builder().name("default").build());
    }
}
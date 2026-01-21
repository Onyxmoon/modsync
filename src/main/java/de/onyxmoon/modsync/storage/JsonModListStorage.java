package de.onyxmoon.modsync.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hypixel.hytale.logger.HytaleLogger;
import de.onyxmoon.modsync.ModSync;
import de.onyxmoon.modsync.api.model.provider.ModList;
import de.onyxmoon.modsync.storage.model.StoredModList;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;

/**
 * JSON-based storage for mod lists.
 */
public class JsonModListStorage {
    private static final HytaleLogger LOGGER = HytaleLogger.get(ModSync.LOG_NAME);
    private final Path modListPath;
    private final Gson gson;

    public JsonModListStorage(Path dataFolder) {
        this.modListPath = dataFolder.resolve("modlist.json");
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(Instant.class, new InstantTypeAdapter())
                .create();
    }

    public void save(ModList modList) {
        try {
            Files.createDirectories(modListPath.getParent());
            StoredModList stored = new StoredModList(modList, Instant.now());
            String json = gson.toJson(stored);
            Files.writeString(modListPath, json);
            LOGGER.atInfo().log("Mod list saved successfully (%d mods)", modList.getMods().size());
        } catch (IOException e) {
            LOGGER.atSevere().withCause(e).log("Failed to save mod list");
            throw new RuntimeException("Failed to save mod list", e);
        }
    }

    public Optional<ModList> load() {
        if (!Files.exists(modListPath)) {
            LOGGER.atSevere().log("Mod list file not found");
            return Optional.empty();
        }

        try {
            String json = Files.readString(modListPath);
            StoredModList stored = gson.fromJson(json, StoredModList.class);
            LOGGER.atInfo().log("Mod list loaded successfully (%d mods)",
                       stored.getModList().getMods().size());
            return Optional.of(stored.getModList());
        } catch (IOException e) {
            LOGGER.atSevere().withCause(e).log("Failed to load mod list");
            return Optional.empty();
        }
    }

    public Optional<Instant> getLastUpdateTime() {
        if (!Files.exists(modListPath)) {
            return Optional.empty();
        }

        try {
            String json = Files.readString(modListPath);
            StoredModList stored = gson.fromJson(json, StoredModList.class);
            return Optional.of(stored.getStoredAt());
        } catch (IOException e) {
            LOGGER.atSevere().withCause(e).log("Failed to read last update time");
            return Optional.empty();
        }
    }
}
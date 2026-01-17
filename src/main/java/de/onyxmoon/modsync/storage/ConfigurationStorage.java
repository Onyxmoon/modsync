package de.onyxmoon.modsync.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;import com.hypixel.hytale.logger.HytaleLogger;
import de.onyxmoon.modsync.ModSync;
import de.onyxmoon.modsync.storage.model.PluginConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Handles persistent storage of plugin configuration.
 */
public class ConfigurationStorage {
    private static final HytaleLogger LOGGER = HytaleLogger.get(ModSync.LOG_NAME);
    private final Path configPath;
    private final Gson gson;
    private PluginConfig config;

    public ConfigurationStorage(Path dataFolder) {
        this.configPath = dataFolder.resolve("config.json");
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();
        this.config = load();
    }

    private PluginConfig load() {
        if (!Files.exists(configPath)) {
            LOGGER.atInfo().log("Config file not found, creating default configuration");
            return new PluginConfig();
        }

        try {
            String json = Files.readString(configPath);
            PluginConfig loaded = gson.fromJson(json, PluginConfig.class);
            LOGGER.atInfo().log("Configuration loaded successfully");
            return loaded;
        } catch (IOException e) {
            LOGGER.atSevere().log("Failed to load configuration", e);
            return new PluginConfig();
        }
    }

    public void save() {
        try {
            Files.createDirectories(configPath.getParent());
            String json = gson.toJson(config);
            Files.writeString(configPath, json);
            LOGGER.atInfo().log("Configuration saved successfully");
        } catch (IOException e) {
            LOGGER.atSevere().log("Failed to save configuration", e);
        }
    }

    public PluginConfig getConfig() {
        return config;
    }

    public void reload() {
        this.config = load();
    }
}
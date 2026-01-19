package de.onyxmoon.modsync;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.plugin.PluginManager;
import de.onyxmoon.modsync.command.*;
import de.onyxmoon.modsync.provider.ProviderRegistry;
import de.onyxmoon.modsync.provider.UrlParserRegistry;
import de.onyxmoon.modsync.scheduler.UpdateScheduler;
import de.onyxmoon.modsync.service.ModDownloadService;
import de.onyxmoon.modsync.storage.ConfigurationStorage;
import de.onyxmoon.modsync.storage.InstalledModStorage;
import de.onyxmoon.modsync.storage.JsonModListStorage;
import de.onyxmoon.modsync.storage.ManagedModListStorage;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Main plugin class for ModSync.
 */
public class ModSync extends JavaPlugin {
    public static final String LOG_NAME = "ModSync";
    public static final HytaleLogger LOGGER = HytaleLogger.get(ModSync.LOG_NAME);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String PENDING_DELETIONS_FILE = "pending_deletions.json";

    private ProviderRegistry providerRegistry;
    private UrlParserRegistry urlParserRegistry;
    private ConfigurationStorage configStorage;
    private JsonModListStorage modListStorage;
    private InstalledModStorage installedModStorage;
    private ManagedModListStorage managedModListStorage;
    private ModDownloadService downloadService;
    private UpdateScheduler updateScheduler;
    private PluginManager pluginManager;

    public ModSync(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    public void setup() {
        LOGGER.atInfo().log("Setting up ModSync...");

        // Initialize storage - get plugin's data directory
        Path dataFolder = getDataDirectory();
        this.configStorage = new ConfigurationStorage(dataFolder);
        this.modListStorage = new JsonModListStorage(dataFolder);
        this.installedModStorage = new InstalledModStorage(dataFolder);
        this.managedModListStorage = new ManagedModListStorage(dataFolder);

        // Initialize provider and parser registries
        this.providerRegistry = new ProviderRegistry();
        this.urlParserRegistry = new UrlParserRegistry();

        // Initialize download service (mods folder is in the server's mods directory)
        Path modsFolder = dataFolder.getParent();
        this.downloadService = new ModDownloadService(this, modsFolder);

        // Initialize update scheduler
        this.updateScheduler = new UpdateScheduler(this);

        // Initialize plugin manager
        this.pluginManager = PluginManager.get();

        LOGGER.atInfo().log("ModSync setup complete");
    }

    @Override
    public void start() {
        LOGGER.atInfo().log("Starting ModSync...");

        // Register commands
        registerCommands();

        // Initialize scheduler (handles startup updates if configured)
        updateScheduler.initialize();

        LOGGER.atInfo().log("ModSync started");
    }

    private void registerCommands() {
        ModSyncCommand rootCommand = new ModSyncCommand(this);

        // Add subcommands to root command
        rootCommand.addSubCommand(new SetKeyCommand(this));
        rootCommand.addSubCommand(new StatusCommand(this));
        rootCommand.addSubCommand(new ReloadCommand(this));

        // New commands for mod management
        rootCommand.addSubCommand(new AddCommand(this));
        rootCommand.addSubCommand(new ListCommand(this));
        rootCommand.addSubCommand(new RemoveCommand(this));
        rootCommand.addSubCommand(new InstallCommand(this));
        rootCommand.addSubCommand(new CheckCommand(this));
        rootCommand.addSubCommand(new UpgradeCommand(this));

        // Register only the root command
        getCommandRegistry().registerCommand(rootCommand);

        LOGGER.atInfo().log("Commands registered");
    }

    @Override
    public void shutdown() {
        LOGGER.atInfo().log("Shutting down ModSync...");

        // Cancel scheduled tasks
        if (updateScheduler != null) {
            updateScheduler.shutdown();
        }

        // Save all storage
        if (configStorage != null) {
            configStorage.save();
        }
        if (installedModStorage != null) {
            installedModStorage.save();
        }
        if (managedModListStorage != null) {
            managedModListStorage.save();
        }

        LOGGER.atInfo().log("ModSync shut down");
    }

    /**
     * Adds a file path to the pending deletion list.
     * The file will be deleted on next server startup by the bootstrap plugin.
     */
    public void addPendingDeletion(String filePath) {
        Path pendingFile = getDataDirectory().resolve(PENDING_DELETIONS_FILE);
        List<String> pendingPaths = new ArrayList<>();

        // Load existing list
        if (Files.exists(pendingFile)) {
            try {
                String json = Files.readString(pendingFile);
                List<String> existing = GSON.fromJson(json, new TypeToken<List<String>>(){}.getType());
                if (existing != null) {
                    pendingPaths.addAll(existing);
                }
            } catch (IOException e) {
                LOGGER.atWarning().log("Failed to read pending deletions: {}", e.getMessage());
            }
        }

        // Add new path if not already present
        if (!pendingPaths.contains(filePath)) {
            pendingPaths.add(filePath);

            try {
                Files.createDirectories(pendingFile.getParent());
                Files.writeString(pendingFile, GSON.toJson(pendingPaths));
                LOGGER.atInfo().log("Added to pending deletions: {}", filePath);
            } catch (IOException e) {
                LOGGER.atSevere().log("Failed to save pending deletions: {}", e.getMessage());
            }
        }
    }

    // Getters
    public ProviderRegistry getProviderRegistry() {
        return providerRegistry;
    }

    public UrlParserRegistry getUrlParserRegistry() {
        return urlParserRegistry;
    }

    public ConfigurationStorage getConfigStorage() {
        return configStorage;
    }

    public JsonModListStorage getModListStorage() {
        return modListStorage;
    }

    public InstalledModStorage getInstalledModStorage() {
        return installedModStorage;
    }

    public ManagedModListStorage getManagedModListStorage() {
        return managedModListStorage;
    }

    public ModDownloadService getDownloadService() {
        return downloadService;
    }

    public UpdateScheduler getUpdateScheduler() {
        return updateScheduler;
    }

    public PluginManager getPluginManager() {
        return pluginManager;
    }
}
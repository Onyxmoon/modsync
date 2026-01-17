package de.onyxmoon.modsync.scheduler;

import com.hypixel.hytale.logger.HytaleLogger;
import de.onyxmoon.modsync.ModSync;
import de.onyxmoon.modsync.api.ModListProvider;
import de.onyxmoon.modsync.api.model.ModList;
import de.onyxmoon.modsync.storage.model.PluginConfig;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Manages scheduled updates of mod lists.
 */
public class UpdateScheduler {
    private static final HytaleLogger LOGGER = HytaleLogger.get(ModSync.LOG_NAME);
    private final ModSync plugin;
    private final ScheduledExecutorService executor;
    private ScheduledFuture<?> periodicTask;

    public UpdateScheduler(ModSync plugin) {
        this.plugin = plugin;
        this.executor = Executors.newSingleThreadScheduledExecutor();
    }

    /**
     * Initialize scheduler based on configuration.
     */
    public void initialize() {
        PluginConfig config = plugin.getConfigStorage().getConfig();

        // Handle startup update
        if (config.getUpdateMode() == UpdateMode.STARTUP ||
            config.getUpdateMode() == UpdateMode.BOTH) {
            scheduleStartupUpdate();
        }

        // Handle periodic updates
        if (config.getUpdateMode() == UpdateMode.PERIODIC ||
            config.getUpdateMode() == UpdateMode.BOTH) {
            schedulePeriodicUpdates(config.getUpdateIntervalMinutes());
        }
    }

    /**
     * Schedule update on server start (delayed by 30 seconds).
     */
    private void scheduleStartupUpdate() {
        executor.schedule(
            () -> performUpdate()
                .exceptionally(ex -> {
                    LOGGER.atSevere().log("Startup update failed", ex);
                    return null;
                }),
            30,
            TimeUnit.SECONDS
        );
    }

    /**
     * Schedule periodic updates.
     *
     * @param intervalMinutes Interval in minutes
     */
    public void schedulePeriodicUpdates(int intervalMinutes) {
        // Cancel existing task if any
        if (periodicTask != null && !periodicTask.isCancelled()) {
            periodicTask.cancel(false);
        }

        periodicTask = executor.scheduleAtFixedRate(
            () -> performUpdate()
                .exceptionally(ex -> {
                    LOGGER.atSevere().log("Periodic update failed", ex);
                    return null;
                }),
            intervalMinutes,
            intervalMinutes,
            TimeUnit.MINUTES
        );

        LOGGER.atInfo().log("Scheduled periodic updates every {} minutes", intervalMinutes);
    }

    /**
     * Cancel all scheduled tasks.
     */
    public void shutdown() {
        if (periodicTask != null && !periodicTask.isCancelled()) {
            periodicTask.cancel(true);
        }
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Perform update (can be called manually or by scheduler).
     *
     * @return CompletableFuture containing the updated ModList
     */
    public CompletableFuture<ModList> performUpdate() {
        PluginConfig config = plugin.getConfigStorage().getConfig();

        String apiKey = config.getApiKey(config.getCurrentSource());
        if (apiKey == null) {
            return CompletableFuture.failedFuture(
                new IllegalStateException("No API key configured for " +
                                         config.getCurrentSource().getDisplayName())
            );
        }

        String projectId = config.getCurrentProjectId();
        if (projectId == null) {
            return CompletableFuture.failedFuture(
                new IllegalStateException("No project ID configured")
            );
        }

        ModListProvider provider = plugin.getProviderRegistry()
            .getProvider(config.getCurrentSource());

        LOGGER.atInfo().log("Starting mod list update from {} for project {}",
                   config.getCurrentSource().getDisplayName(), projectId);

        return provider.fetchModList(apiKey, projectId)
            .thenApply(modList -> {
                // Save to storage
                plugin.getModListStorage().save(modList);

                LOGGER.atInfo().log("Mod list updated successfully: {} mods loaded from {}",
                           modList.getMods().size(),
                           modList.getSource().getDisplayName());

                return modList;
            })
            .exceptionally(ex -> {
                LOGGER.atSevere().log("Failed to fetch mod list from {}",
                            config.getCurrentSource().getDisplayName(), ex);
                throw new RuntimeException("Update failed: " + ex.getMessage(), ex);
            });
    }
}
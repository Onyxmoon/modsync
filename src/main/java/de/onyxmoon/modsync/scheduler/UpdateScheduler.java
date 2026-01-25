package de.onyxmoon.modsync.scheduler;

import com.hypixel.hytale.logger.HytaleLogger;
import de.onyxmoon.modsync.ModSync;
import de.onyxmoon.modsync.api.ModProvider;
import de.onyxmoon.modsync.api.model.provider.ModList;
import de.onyxmoon.modsync.storage.model.PluginConfig;

import java.util.Optional;
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
    private static final int STARTUP_DELAY_SECONDS = 30;
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
                    LOGGER.atSevere().withCause(ex).log("Startup update failed");
                    return null;
                }),
            STARTUP_DELAY_SECONDS,
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
                    LOGGER.atSevere().withCause(ex).log("Periodic update failed");
                    return null;
                }),
            intervalMinutes,
            intervalMinutes,
            TimeUnit.MINUTES
        );

        LOGGER.atInfo().log("Scheduled periodic updates every %d minutes", intervalMinutes);
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

        Optional<ModList> storedList = plugin.getModListStorage().load();
        if (storedList.isEmpty()) {
            return CompletableFuture.failedFuture(
                new IllegalStateException("No stored mod list found. Run an update or import first.")
            );
        }

        String source = storedList.get().getSource();
        ModProvider provider = plugin.getProviderRegistry().getProvider(source);

        String apiKey = config.getApiKey(source);
        if (provider.requiresApiKey() && apiKey == null) {
            return CompletableFuture.failedFuture(
                new IllegalStateException("No API key configured for " + provider.getDisplayName())
            );
        }

        String projectId = storedList.get().getProjectId();

        LOGGER.atInfo().log("Starting mod list update from %s for project %s",
                   provider.getDisplayName(), projectId);

        return provider.fetchModList(apiKey, projectId)
            .thenApply(modList -> {
                // Save to storage
                plugin.getModListStorage().save(modList);

                LOGGER.atInfo().log("Mod list updated successfully: %d mods loaded from %s",
                           modList.getMods().size(),
                           modList.getSource());

                return modList;
            })
            .exceptionally(ex -> {
                LOGGER.atSevere().withCause(ex).log("Failed to fetch mod list from %s", source);
                throw new RuntimeException("Update failed: " + ex.getMessage(), ex);
            });
    }
}

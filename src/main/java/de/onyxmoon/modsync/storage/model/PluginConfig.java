package de.onyxmoon.modsync.storage.model;

import de.onyxmoon.modsync.api.ModListSource;
import de.onyxmoon.modsync.api.ReleaseChannel;
import de.onyxmoon.modsync.scheduler.UpdateMode;

import java.util.HashMap;
import java.util.Map;

/**
 * Plugin configuration model.
 */
public class PluginConfig {
    /**
     * Default path for early plugins folder, relative to server root.
     */
    public static final String DEFAULT_EARLY_PLUGINS_PATH = "earlyplugins";

    private Map<ModListSource, String> apiKeys;
    
    private String currentProjectId;
    
    // Paths
    private String earlyPluginsPath;
    
    // Upgrade config
    private UpdateMode updateMode;
    private int updateIntervalMinutes;
    private boolean updateOnStartup;

    // Release channel configuration
    private ReleaseChannel defaultReleaseChannel;

    // Self-update configuration
    private boolean checkForPluginUpdates;
    private boolean includePrereleases;

    public PluginConfig() {
        this.apiKeys = new HashMap<>();
        this.updateMode = UpdateMode.MANUAL;
        this.updateIntervalMinutes = 60;
        this.updateOnStartup = false;
        this.earlyPluginsPath = DEFAULT_EARLY_PLUGINS_PATH;
        this.checkForPluginUpdates = true;
        this.includePrereleases = false;
    }

    public Map<ModListSource, String> getApiKeys() {
        return apiKeys;
    }

    public void setApiKeys(Map<ModListSource, String> apiKeys) {
        this.apiKeys = apiKeys;
    }

    public String getApiKey(ModListSource source) {
        return apiKeys.get(source);
    }

    public void setApiKey(ModListSource source, String apiKey) {
        apiKeys.put(source, apiKey);
    }

    public String getCurrentProjectId() {
        return currentProjectId;
    }

    public void setCurrentProjectId(String currentProjectId) {
        this.currentProjectId = currentProjectId;
    }

    public UpdateMode getUpdateMode() {
        return updateMode;
    }

    public void setUpdateMode(UpdateMode updateMode) {
        this.updateMode = updateMode;
    }

    public int getUpdateIntervalMinutes() {
        return updateIntervalMinutes;
    }

    public void setUpdateIntervalMinutes(int updateIntervalMinutes) {
        this.updateIntervalMinutes = updateIntervalMinutes;
    }

    public boolean isUpdateOnStartup() {
        return updateOnStartup;
    }

    public void setUpdateOnStartup(boolean updateOnStartup) {
        this.updateOnStartup = updateOnStartup;
    }

    /**
     * Gets the path for the early plugins folder.
     * Can be absolute or relative to server root.
     *
     * @return the early plugins path, or default if not set
     */
    public String getEarlyPluginsPath() {
        return earlyPluginsPath != null ? earlyPluginsPath : DEFAULT_EARLY_PLUGINS_PATH;
    }

    public void setEarlyPluginsPath(String earlyPluginsPath) {
        this.earlyPluginsPath = earlyPluginsPath;
    }

    /**
     * Whether to check for ModSync plugin updates on startup.
     *
     * @return true if plugin update checks are enabled
     */
    public boolean isCheckForPluginUpdates() {
        return checkForPluginUpdates;
    }

    public void setCheckForPluginUpdates(boolean checkForPluginUpdates) {
        this.checkForPluginUpdates = checkForPluginUpdates;
    }

    /**
     * Whether to include prerelease versions in update checks.
     *
     * @return true if prereleases should be included
     */
    public boolean isIncludePrereleases() {
        return includePrereleases;
    }

    public void setIncludePrereleases(boolean includePrereleases) {
        this.includePrereleases = includePrereleases;
    }

    /**
     * Gets the default release channel for managed mods.
     *
     * @return the default release channel, or RELEASE if not set
     */
    public ReleaseChannel getDefaultReleaseChannel() {
        return defaultReleaseChannel != null ? defaultReleaseChannel : ReleaseChannel.RELEASE;
    }

    public void setDefaultReleaseChannel(ReleaseChannel defaultReleaseChannel) {
        this.defaultReleaseChannel = defaultReleaseChannel;
    }
}

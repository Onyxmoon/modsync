package de.onyxmoon.modsync.storage.model;

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

    /**
     * Default update interval in minutes.
     */
    public static final int DEFAULT_UPDATE_INTERVAL_MINUTES = 60;

    /**
     * API keys per provider source identifier (e.g., "curseforge", "modtale").
     */
    private Map<String, String> apiKeys;
    
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

    // Admin welcome message configuration
    private boolean disableAdminWelcomeMessage;

    public PluginConfig() {
        this.apiKeys = new HashMap<>();
        this.updateMode = UpdateMode.MANUAL;
        this.updateIntervalMinutes = DEFAULT_UPDATE_INTERVAL_MINUTES;
        this.updateOnStartup = false;
        this.earlyPluginsPath = DEFAULT_EARLY_PLUGINS_PATH;
        this.checkForPluginUpdates = true;
        this.includePrereleases = false;
        this.disableAdminWelcomeMessage = false;
    }

    public Map<String, String> getApiKeys() {
        return apiKeys;
    }

    public void setApiKeys(Map<String, String> apiKeys) {
        this.apiKeys = apiKeys;
    }

    /**
     * Gets the API key for a provider source.
     *
     * @param source the source identifier (e.g., "curseforge", "modtale")
     * @return the API key, or null if not set
     */
    public String getApiKey(String source) {
        return apiKeys.get(source != null ? source.toLowerCase() : null);
    }

    /**
     * Sets the API key for a provider source.
     *
     * @param source the source identifier (e.g., "curseforge", "modtale")
     * @param apiKey the API key
     */
    public void setApiKey(String source, String apiKey) {
        apiKeys.put(source != null ? source.toLowerCase() : null, apiKey);
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
     * Whether to disable the admin welcome message on player join.
     *
     * @return true if admin welcome messages are disabled
     */
    public boolean isDisableAdminWelcomeMessage() {
        return disableAdminWelcomeMessage;
    }

    public void setDisableAdminWelcomeMessage(boolean disableAdminWelcomeMessage) {
        this.disableAdminWelcomeMessage = disableAdminWelcomeMessage;
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

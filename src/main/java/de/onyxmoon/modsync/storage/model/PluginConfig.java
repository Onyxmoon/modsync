package de.onyxmoon.modsync.storage.model;

import de.onyxmoon.modsync.api.ModListSource;
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
    private ModListSource currentSource;
    private UpdateMode updateMode;
    private int updateIntervalMinutes;
    private boolean updateOnStartup;
    private String earlyPluginsPath;

    public PluginConfig() {
        this.apiKeys = new HashMap<>();
        this.currentSource = ModListSource.CURSEFORGE;
        this.updateMode = UpdateMode.MANUAL;
        this.updateIntervalMinutes = 60;
        this.updateOnStartup = false;
        this.earlyPluginsPath = DEFAULT_EARLY_PLUGINS_PATH;
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

    public ModListSource getCurrentSource() {
        return currentSource;
    }

    public void setCurrentSource(ModListSource currentSource) {
        this.currentSource = currentSource;
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
}
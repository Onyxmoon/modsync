package de.onyxmoon.modsync.api;

/**
 * Enum representing the type of plugin based on when it loads.
 */
public enum PluginType {
    /**
     * Standard plugin that loads after the server starts.
     * Installed to the mods/ folder.
     */
    PLUGIN("Plugin", "mods"),

    /**
     * Early plugin (bootstrap) that loads before standard plugins.
     * Installed to the earlyplugins/ folder.
     */
    EARLY_PLUGIN("Early Plugin", "earlyplugins");

    private final String displayName;
    private final String folderName;

    PluginType(String displayName, String folderName) {
        this.displayName = displayName;
        this.folderName = folderName;
    }

    /**
     * Gets the human-readable display name for this plugin type.
     *
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Gets the folder name where this plugin type should be installed.
     *
     * @return the folder name (e.g., "mods" or "earlyplugins")
     */
    public String getFolderName() {
        return folderName;
    }
}
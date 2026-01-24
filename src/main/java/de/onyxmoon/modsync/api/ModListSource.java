package de.onyxmoon.modsync.api;

/**
 * Enum representing supported mod list sources.
 */
public enum ModListSource {
    /**
     * CurseForge mod platform
     */
    CURSEFORGE("CurseForge"),

    /**
     * Modrinth mod platform (future support)
     */
    MODRINTH("Modrinth"),

    /**
     * Custom mod source (future support)
     */
    CUSTOM("Custom"),

    /**
     * CFWidget API (CurseForge widget data)
     */
    CFWIDGET("CFWidget");

    private final String displayName;

    ModListSource(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Gets the human-readable display name for this source.
     *
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }
}

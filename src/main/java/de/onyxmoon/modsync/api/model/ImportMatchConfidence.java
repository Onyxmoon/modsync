package de.onyxmoon.modsync.api.model;

/**
 * Confidence level for import matching.
 * Used to indicate how confident we are that an unmanaged mod matches a CurseForge entry.
 */
public enum ImportMatchConfidence {
    /**
     * Exact match - slug derived from identifier matches exactly.
     */
    EXACT("Exact match"),

    /**
     * High confidence - name search returned a very close match.
     */
    HIGH("High confidence"),

    /**
     * Low confidence - name search returned a possible match but uncertain.
     */
    LOW("Low confidence"),

    /**
     * No match found.
     */
    NONE("No match");

    private final String displayName;

    ImportMatchConfidence(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns true if this confidence level is good enough for auto-import.
     */
    public boolean isAutoImportable() {
        return this == EXACT || this == HIGH;
    }
}
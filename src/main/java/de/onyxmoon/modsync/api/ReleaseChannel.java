package de.onyxmoon.modsync.api;

/**
 * Defines the release channel preference for mod version selection.
 * <p>
 * When selecting a version, the system picks the newest version that matches
 * the allowed release types for the channel. For example, if set to BETA and
 * there's a newer Release than the newest Beta, the Release will be chosen.
 * </p>
 */
public enum ReleaseChannel {
    /**
     * Only stable release versions.
     */
    RELEASE("Release", 1),

    /**
     * Beta and release versions (newest of either).
     */
    BETA("Beta", 2),

    /**
     * All versions including alpha, beta, and release (newest of any).
     */
    ALPHA("Alpha", 3);

    private final String displayName;
    private final int permissiveness;

    ReleaseChannel(String displayName, int permissiveness) {
        this.displayName = displayName;
        this.permissiveness = permissiveness;
    }

    /**
     * Gets the display name for this channel.
     *
     * @return the human-readable name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Checks if a release type from a provider is allowed by this channel.
     * <p>
     * The comparison is case-insensitive. Unknown release types are allowed
     * to ensure forward compatibility with new provider release types.
     * </p>
     *
     * @param releaseType the release type string from the provider (e.g., "Release", "Beta", "Alpha")
     * @return true if this channel allows the given release type
     */
    public boolean allows(String releaseType) {
        if (releaseType == null || releaseType.isBlank()) {
            return true; // Unknown = allow (forward compatibility)
        }

        String normalized = releaseType.trim().toLowerCase();

        return switch (this) {
            case RELEASE -> "release".equals(normalized);
            case BETA -> "release".equals(normalized) || "beta".equals(normalized);
            case ALPHA -> true; // Allow all
        };
    }

    /**
     * Parses a string to a ReleaseChannel.
     * <p>
     * Accepts the enum name or display name, case-insensitive.
     * Returns RELEASE as default for invalid input.
     * </p>
     *
     * @param value the string to parse
     * @return the matching ReleaseChannel, or RELEASE if not found
     */
    public static ReleaseChannel fromString(String value) {
        if (value == null || value.isBlank()) {
            return RELEASE;
        }

        String normalized = value.trim().toLowerCase();

        for (ReleaseChannel channel : values()) {
            if (channel.name().toLowerCase().equals(normalized)
                    || channel.displayName.toLowerCase().equals(normalized)) {
                return channel;
            }
        }

        return RELEASE;
    }

    /**
     * Parses a string to a ReleaseChannel, returning null if not found.
     *
     * @param value the string to parse
     * @return the matching ReleaseChannel, or null if not found
     */
    public static ReleaseChannel fromStringOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim().toLowerCase();

        for (ReleaseChannel channel : values()) {
            if (channel.name().toLowerCase().equals(normalized)
                    || channel.displayName.toLowerCase().equals(normalized)) {
                return channel;
            }
        }

        return null;
    }
}
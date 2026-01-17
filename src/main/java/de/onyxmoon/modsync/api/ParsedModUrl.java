package de.onyxmoon.modsync.api;

/**
 * Represents parsed information from a mod URL.
 *
 * @param source the mod source (CurseForge, Modrinth, etc.)
 * @param modId the numeric mod ID (may be null if only slug is known)
 * @param slug the URL-friendly mod identifier
 * @param versionId specific version ID (null for latest)
 */
public record ParsedModUrl(
        ModListSource source,
        String modId,
        String slug,
        String versionId
) {
    /**
     * Check if this parsed URL has a specific version.
     */
    public boolean hasSpecificVersion() {
        return versionId != null;
    }

    /**
     * Check if this parsed URL has a mod ID.
     */
    public boolean hasModId() {
        return modId != null;
    }

    /**
     * Check if this parsed URL has a slug.
     */
    public boolean hasSlug() {
        return slug != null;
    }
}
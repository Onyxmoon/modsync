package de.onyxmoon.modsync.service.selfupgrade.model;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a semantic version (major.minor.patch[-prerelease]).
 * Supports comparison for update detection.
 */
public final class SemanticVersion implements Comparable<SemanticVersion> {
    private static final Pattern VERSION_PATTERN =
            Pattern.compile("^(\\d+)\\.(\\d+)\\.(\\d+)(?:-([a-zA-Z0-9.-]+))?$");

    private final int major;
    private final int minor;
    private final int patch;
    private final String prerelease;

    private SemanticVersion(int major, int minor, int patch, String prerelease) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.prerelease = prerelease;
    }

    /**
     * Parses a version string into a SemanticVersion.
     *
     * @param version the version string to parse (e.g., "1.0.0", "v1.0.0-alpha")
     * @return the parsed SemanticVersion
     * @throws IllegalArgumentException if the version string is invalid
     */
    public static SemanticVersion parse(String version) {
        if (version == null || version.isBlank()) {
            throw new IllegalArgumentException("Version string cannot be null or empty");
        }

        // Remove 'v' prefix if present
        String normalized = version.startsWith("v") ? version.substring(1) : version;

        Matcher matcher = VERSION_PATTERN.matcher(normalized);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid semantic version: " + version);
        }

        int major = Integer.parseInt(matcher.group(1));
        int minor = Integer.parseInt(matcher.group(2));
        int patch = Integer.parseInt(matcher.group(3));
        String prerelease = matcher.group(4); // May be null

        return new SemanticVersion(major, minor, patch, prerelease);
    }

    /**
     * Attempts to parse a version string, returning null if invalid.
     *
     * @param version the version string to parse
     * @return the parsed SemanticVersion, or null if parsing fails
     */
    public static SemanticVersion tryParse(String version) {
        try {
            return parse(version);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public int getMajor() {
        return major;
    }

    public int getMinor() {
        return minor;
    }

    public int getPatch() {
        return patch;
    }

    public String getPrerelease() {
        return prerelease;
    }

    public boolean isPrerelease() {
        return prerelease != null;
    }

    @Override
    public int compareTo(SemanticVersion other) {
        // Compare major.minor.patch
        int result = Integer.compare(this.major, other.major);
        if (result != 0) return result;

        result = Integer.compare(this.minor, other.minor);
        if (result != 0) return result;

        result = Integer.compare(this.patch, other.patch);
        if (result != 0) return result;

        // Prerelease comparison:
        // - No prerelease > any prerelease (1.0.0 > 1.0.0-alpha)
        // - Compare prerelease strings alphabetically
        if (this.prerelease == null && other.prerelease == null) return 0;
        if (this.prerelease == null) return 1;  // This is stable, other is prerelease
        if (other.prerelease == null) return -1; // Other is stable, this is prerelease

        return this.prerelease.compareTo(other.prerelease);
    }

    /**
     * Returns true if this version is newer than the other version.
     *
     * @param other the version to compare against
     * @return true if this version is newer
     */
    public boolean isNewerThan(SemanticVersion other) {
        return this.compareTo(other) > 0;
    }

    @Override
    public String toString() {
        return prerelease == null
                ? String.format("%d.%d.%d", major, minor, patch)
                : String.format("%d.%d.%d-%s", major, minor, patch, prerelease);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SemanticVersion that = (SemanticVersion) o;
        return major == that.major && minor == that.minor && patch == that.patch
                && Objects.equals(prerelease, that.prerelease);
    }

    @Override
    public int hashCode() {
        return Objects.hash(major, minor, patch, prerelease);
    }
}
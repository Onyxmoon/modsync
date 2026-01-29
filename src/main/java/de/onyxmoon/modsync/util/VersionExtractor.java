package de.onyxmoon.modsync.util;

import com.hypixel.hytale.common.semver.Semver;
import java.nio.file.Path;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for extracting version information from mod files.
 * Tries multiple sources in priority order: manifest.json → filename → null
 */
public final class VersionExtractor {
    private VersionExtractor() {
        // Utility class - prevent instantiation
    }

    /**
     * Extracts version from a mod file using multiple fallback strategies.
     * 
     * @param modFile Path to the mod file (JAR or ZIP)
     * @return Extracted version string, or null if no version could be found
     */
    public static String extractVersion(Path modFile) {
        // 1. Try manifest version (highest priority)
        // 2. Try filename version (fallback)
        Optional<Semver> manifestVersion = ManifestReader.readVersion(modFile);
        return manifestVersion.map(Semver::toString).orElseGet(()
                -> extractVersionFromFilename(modFile.getFileName().toString()));
    }

    /**
     * Extracts version from filename using pattern matching.
     * Supports formats like:
     * - MyMod-1.2.3.jar
     * - MyMod_v1.2.3.zip  
     * - MyMod 1.2.3.jar
     * - MyMod-v1.2.3-beta.jar
     * - MyModR1.jar
     * 
     * @param filename The filename to extract version from
     * @return Extracted version string, or null if no version found
     */
    public static String extractVersionFromFilename(String filename) {
        // Pattern for various version formats:
        // Group 1: version/ver prefix with semantic version (1.2.3-beta)
        // Group 5: semantic version without prefix (1.2.3)
        // Group 7: simple version like R1, Alpha2
        Pattern versionPattern = Pattern.compile(
            "(?:[-_v\\s]|version|ver\\.?)\\s*(\\d+(\\.\\d+)+(-[a-zA-Z0-9\\.]+)?)|" +
            "(\\d+(\\.\\d+)+)|" +
            "([a-zA-Z]+\\d+)",
            Pattern.CASE_INSENSITIVE
        );

        Matcher matcher = versionPattern.matcher(filename);
        if (matcher.find()) {
            // Return first non-null group
            for (int i = 1; i <= matcher.groupCount(); i++) {
                if (matcher.group(i) != null) {
                    return normalizeVersion(matcher.group(i));
                }
            }
        }
        return null;
    }

    /**
     * Normalizes a version string to canonical SemVer if possible.
     * Returns the original string if it is not a valid SemVer.
     */
    public static String normalizeVersion(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        Optional<Semver> semver = parseSemver(raw);
        return semver.map(Semver::toString).orElse(raw);
    }

    /**
     * Attempts to parse a SemVer string, returning empty on failure.
     */
    public static Optional<Semver> parseSemver(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String normalized = raw.startsWith("v") || raw.startsWith("V")
                ? raw.substring(1)
                : raw;
        try {
            return Optional.of(Semver.fromString(normalized));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    /**
     * Returns the filename without common archive extensions.
     */
    public static String baseFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return null;
        }
        String lower = filename.toLowerCase();
        if (lower.endsWith(".jar") || lower.endsWith(".zip")) {
            return filename.substring(0, filename.length() - 4);
        }
        return filename;
    }
}

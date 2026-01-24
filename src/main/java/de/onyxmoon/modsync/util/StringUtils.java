package de.onyxmoon.modsync.util;

/**
 * Utility class for common string operations.
 */
public final class StringUtils {

    private StringUtils() {
        // Utility class - prevent instantiation
    }

    /**
     * Checks if a string is null, empty, or contains only whitespace.
     *
     * @param s the string to check
     * @return true if the string is null, empty, or blank
     */
    public static boolean isNullOrBlank(String s) {
        return s == null || s.isBlank();
    }

    /**
     * Checks if a string is not null and not blank.
     *
     * @param s the string to check
     * @return true if the string has content
     */
    public static boolean hasContent(String s) {
        return s != null && !s.isBlank();
    }

    /**
     * Returns the string if it has content, otherwise returns the default value.
     *
     * @param s            the string to check
     * @param defaultValue the default value to return if s is null or blank
     * @return s if it has content, otherwise defaultValue
     */
    public static String defaultIfBlank(String s, String defaultValue) {
        return hasContent(s) ? s : defaultValue;
    }
}
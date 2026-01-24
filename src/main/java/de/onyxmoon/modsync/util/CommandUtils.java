package de.onyxmoon.modsync.util;

import com.hypixel.hytale.server.core.Message;
import de.onyxmoon.modsync.api.model.InstalledState;
import de.onyxmoon.modsync.api.model.ManagedMod;

import java.awt.*;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility methods shared across commands.
 */
public final class CommandUtils {

    private static final Pattern SEMVER_LIKE = Pattern.compile("(?i)\\bv?(\\d+\\.\\d+(?:\\.\\d+)?)");

    private CommandUtils() {
        // Utility class
    }

    /**
     * Formats a mod for display in command output.
     * Format: • Name (identifier/slug) [status]
     *
     * @param mod the mod to format
     * @return formatted Message
     */
    public static Message formatModLine(ManagedMod mod) {
        Message line = Message.raw(mod.getName()).color(Color.YELLOW);

        // Show identifier if installed, otherwise slug
        if (mod.isInstalled() && mod.getInstalledState().isPresent()) {
            String identifier = mod.getInstalledState().get().getIdentifier().toString();
            line = line.insert(Message.raw(" (" + identifier + ")").color(Color.CYAN));
        } else {
            line = line.insert(Message.raw(" (" + mod.getSlug() + ")").color(Color.GRAY));
        }

        // Show version if installed
        if (mod.isInstalled()) {
            String version = mod.getInstalledState()
                    .map(InstalledState::getInstalledVersionNumber)
                    .orElse("?");
            line = line.insert(Message.raw(version).color(Color.GRAY));
        }

        return line;
    }

    /**
     * Formats a mod for display with installation status.
     * Format: • Name (identifier/slug) [status]
     *
     * @param mod the mod to format
     * @return formatted Message with status
     */
    public static Message formatModLineWithStatus(ManagedMod mod) {
        Message line = formatModLine(mod);

        String status = mod.isInstalled() ? "[installed]" : "[not installed]";
        Color statusColor = mod.isInstalled() ? Color.GREEN : Color.GRAY;
        line = line.insert(Message.raw(" " + status).color(statusColor));

        return line;
    }

    /**
     * Strips surrounding quotes from a string.
     * Handles both single and double quotes.
     *
     * @param input the input string
     * @return the string without surrounding quotes
     */
    public static String stripQuotes(String input) {
        if (input == null || input.length() < 2) {
            return input;
        }
        if ((input.startsWith("\"") && input.endsWith("\"")) ||
            (input.startsWith("'") && input.endsWith("'"))) {
            return input.substring(1, input.length() - 1);
        }
        return input;
    }

    /**
     * Extracts a user-friendly error message from an exception.
     * Unwraps CompletionException and similar wrappers.
     *
     * @param ex the exception
     * @return a user-friendly error message
     */
    public static String extractErrorMessage(Throwable ex) {
        if (ex == null) {
            return "Unknown error";
        }
        // Unwrap CompletionException and similar wrappers
        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
        String msg = cause.getMessage();
        return msg != null ? msg : cause.getClass().getSimpleName();
    }

    /**
     * Extracts a semver-like numeric version from a raw string.
     * Matches patterns like v1.2, 1.2.3, or 1.2.3-foo (returns 1.2.3).
     */
    public static Optional<String> extractSemverLike(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        Matcher matcher = SEMVER_LIKE.matcher(raw);
        String best = null;
        while (matcher.find()) {
            String match = matcher.group(1);
            if (best == null || match.length() > best.length()) {
                best = match;
            }
        }
        return Optional.ofNullable(best);
    }

    /**
     * Formats old/new version display values.
     * Uses extracted semver-like values only if both sides are present.
     */
    public static Optional<VersionLine> formatVersionLine(String installedRaw, String latestRaw) {
        String installed = installedRaw == null ? "" : installedRaw;
        String latest = latestRaw == null ? "" : latestRaw;
        if (installed.isBlank() || latest.isBlank()) {
            return Optional.empty();
        }
        Optional<String> installedExtracted = extractSemverLike(installed);
        Optional<String> latestExtracted = extractSemverLike(latest);
        if (installedExtracted.isPresent() && latestExtracted.isPresent()) {
            return Optional.of(new VersionLine(installedExtracted.get(), latestExtracted.get()));
        }
        return Optional.of(new VersionLine(installed, latest));
    }

    public record VersionLine(String oldDisplay, String newDisplay) {
    }
}

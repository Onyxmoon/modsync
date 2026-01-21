package de.onyxmoon.modsync.util;

import com.hypixel.hytale.server.core.Message;
import de.onyxmoon.modsync.api.model.InstalledState;
import de.onyxmoon.modsync.api.model.ManagedMod;

/**
 * Utility methods shared across commands.
 */
public final class CommandUtils {

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
        Message line = Message.raw(mod.getName()).color("yellow");

        // Show identifier if installed, otherwise slug
        if (mod.isInstalled() && mod.getInstalledState().isPresent()) {
            String identifier = mod.getInstalledState().get().getIdentifier().toString();
            line = line.insert(Message.raw(" (" + identifier + ")").color("aqua"));
        } else {
            line = line.insert(Message.raw(" (" + mod.getSlug() + ")").color("gray"));
        }

        // Show version if installed
        if (mod.isInstalled()) {
            String version = mod.getInstalledState()
                    .map(InstalledState::getInstalledVersionNumber)
                    .orElse("?");
            line = line.insert(Message.raw(" v" + version).color("dark_gray"));
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
        String statusColor = mod.isInstalled() ? "green" : "gray";
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
}
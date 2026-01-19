package de.onyxmoon.modsync.bootstrap;

import com.hypixel.hytale.plugin.early.ClassTransformer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Bootstrap plugin that deletes pending mod files before the server loads them.
 *
 * This runs before normal plugins are loaded, so we can delete files that were
 * locked during runtime (because they were loaded as plugins).
 */
public class ModSyncBootstrap implements ClassTransformer {
    private static final String PENDING_DELETIONS_FILE = "pending_deletions.json";
    private static final Pattern STRING_PATTERN = Pattern.compile("\"((?:[^\"\\\\]|\\\\.)*)\"");
    private static boolean initialized = false;

    static {
        // Run deletion logic when class is first loaded
        processPendingDeletions();
    }

    /**
     * Process pending deletions from the ModSync data directory.
     * Called once when the class is loaded by the ServiceLoader.
     */
    private static synchronized void processPendingDeletions() {
        if (initialized) {
            return;
        }
        initialized = true;

        // Try to find the pending_deletions.json in possible locations
        Path pendingFile = findPendingDeletionsFile();

        if (pendingFile == null || !Files.exists(pendingFile)) {
            System.out.println("[ModSync Bootstrap] No pending deletions found.");
            return;
        }

        System.out.println("[ModSync Bootstrap] Processing pending deletions from: " + pendingFile);

        try {
            String json = Files.readString(pendingFile);
            List<String> pendingPaths = parseJsonStringArray(json);

            if (pendingPaths == null || pendingPaths.isEmpty()) {
                Files.deleteIfExists(pendingFile);
                System.out.println("[ModSync Bootstrap] Pending deletions list was empty.");
                return;
            }

            List<String> stillPending = new ArrayList<>();
            int deleted = 0;
            int failed = 0;

            for (String pathStr : pendingPaths) {
                Path path = Path.of(pathStr);
                if (Files.exists(path)) {
                    try {
                        Files.delete(path);
                        System.out.println("[ModSync Bootstrap] Deleted: " + path);
                        deleted++;
                    } catch (IOException e) {
                        System.err.println("[ModSync Bootstrap] Failed to delete (will retry): " + path + " - " + e.getMessage());
                        stillPending.add(pathStr);
                        failed++;
                    }
                } else {
                    System.out.println("[ModSync Bootstrap] File already gone: " + path);
                    deleted++;
                }
            }

            // Update or remove the pending file
            if (stillPending.isEmpty()) {
                Files.deleteIfExists(pendingFile);
                System.out.println("[ModSync Bootstrap] All pending deletions processed. Deleted: " + deleted);
            } else {
                Files.writeString(pendingFile, toJsonStringArray(stillPending));
                System.out.println("[ModSync Bootstrap] Deletions processed. Deleted: " + deleted + ", Still pending: " + failed);
            }

        } catch (IOException e) {
            System.err.println("[ModSync Bootstrap] Failed to process pending deletions: " + e.getMessage());
        }
    }

    /**
     * Parse a simple JSON string array without external dependencies.
     * Format: ["string1", "string2", ...]
     */
    private static List<String> parseJsonStringArray(String json) {
        List<String> result = new ArrayList<>();
        Matcher matcher = STRING_PATTERN.matcher(json);
        while (matcher.find()) {
            String value = matcher.group(1);
            // Unescape common JSON escape sequences
            value = value.replace("\\\\", "\u0000")  // Temp placeholder
                         .replace("\\\"", "\"")
                         .replace("\\n", "\n")
                         .replace("\\r", "\r")
                         .replace("\\t", "\t")
                         .replace("\u0000", "\\");   // Restore backslashes
            result.add(value);
        }
        return result;
    }

    /**
     * Convert a list of strings to a JSON array string.
     */
    private static String toJsonStringArray(List<String> strings) {
        StringBuilder sb = new StringBuilder("[\n");
        for (int i = 0; i < strings.size(); i++) {
            String escaped = strings.get(i)
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
            sb.append("  \"").append(escaped).append("\"");
            if (i < strings.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Find the pending_deletions.json file.
     * Checks multiple possible locations for the ModSync data directory.
     */
    private static Path findPendingDeletionsFile() {
        // The server working directory
        Path workDir = Path.of(System.getProperty("user.dir"));

        // Possible locations for the ModSync data directory
        List<Path> possiblePaths = List.of(
            // Standard location with group prefix: mods/Onyxmoon_ModSync/
            workDir.resolve("mods").resolve("Onyxmoon_ModSync").resolve(PENDING_DELETIONS_FILE),
            // Without group prefix
            workDir.resolve("mods").resolve("ModSync").resolve(PENDING_DELETIONS_FILE),
            // Alternative casing
            workDir.resolve("mods").resolve("modsync").resolve(PENDING_DELETIONS_FILE),
            // Alternative: plugins folder
            workDir.resolve("plugins").resolve("Onyxmoon_ModSync").resolve(PENDING_DELETIONS_FILE),
            workDir.resolve("plugins").resolve("ModSync").resolve(PENDING_DELETIONS_FILE)
        );

        for (Path path : possiblePaths) {
            if (Files.exists(path)) {
                return path;
            }
        }

        // Return the most likely default path even if it doesn't exist
        return possiblePaths.get(0);
    }

    @Override
    public byte[] transform(String className, String path, byte[] bytes) {
        // No-op transformer - we just use this to hook into the early loading phase
        return bytes;
    }

    @Override
    public int priority() {
        // High priority to run early
        return 1000;
    }
}
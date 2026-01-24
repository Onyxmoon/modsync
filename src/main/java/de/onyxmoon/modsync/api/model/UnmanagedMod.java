package de.onyxmoon.modsync.api.model;

import com.hypixel.hytale.common.plugin.PluginIdentifier;
import de.onyxmoon.modsync.api.PluginType;

import java.nio.file.Path;

/**
 * Represents an unmanaged mod found in the mods or earlyplugins folder.
 * These are JAR files that exist on disk but are not tracked by ModSync.
 *
 * @param filePath   Full path to the JAR file
 * @param fileName   Name of the JAR file
 * @param identifier Plugin identifier extracted from manifest (group:name), may be null if manifest is unreadable
 * @param fileHash   SHA-256 hash of the file
 * @param fileSize   Size of the file in bytes
 * @param pluginType Whether this is a regular plugin or early plugin
 */
public record UnmanagedMod(
        Path filePath,
        String fileName,
        PluginIdentifier identifier,
        String fileHash,
        long fileSize,
        PluginType pluginType
) {
    /**
     * Returns the mod name from the identifier, or the filename without extension as fallback.
     */
    public String getDisplayName() {
        if (identifier != null) {
            // PluginIdentifier.toString() returns "group:name" format
            String identStr = identifier.toString();
            int colonIndex = identStr.indexOf(':');
            if (colonIndex >= 0 && colonIndex < identStr.length() - 1) {
                return identStr.substring(colonIndex + 1);
            }
            return identStr;
        }
        // Remove .jar extension
        if (fileName.endsWith(".jar")) {
            return fileName.substring(0, fileName.length() - 4);
        }
        return fileName;
    }

    /**
     * Returns the identifier as string (group:name), or null if no identifier.
     */
    public String getIdentifierString() {
        if (identifier == null) {
            return null;
        }
        return identifier.toString();
    }

    /**
     * Returns a human-readable file size.
     */
    public String getFormattedSize() {
        if (fileSize < 1024) {
            return fileSize + " B";
        } else if (fileSize < 1024 * 1024) {
            return String.format("%.1f KB", fileSize / 1024.0);
        } else {
            return String.format("%.1f MB", fileSize / (1024.0 * 1024.0));
        }
    }
}
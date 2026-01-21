package de.onyxmoon.modsync.storage.model;

import com.hypixel.hytale.common.plugin.PluginIdentifier;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * JSON structure for mods.lock.json - the installation state file.
 * This file is machine-specific and should NOT be shared or versioned in Git.
 * Contains the actual installation state (paths, hashes, identifiers).
 *
 * @see de.onyxmoon.modsync.storage.ManagedModStorage#SCHEMA_VERSION
 */
public class LockFile {
    private int version;
    private Instant lockedAt;
    private Map<String, LockedInstallation> installations;

    public LockFile() {
        this.installations = new HashMap<>();
    }

    public LockFile(int version, Instant lockedAt, Map<String, LockedInstallation> installations) {
        this.version = version;
        this.lockedAt = lockedAt;
        this.installations = installations != null ? installations : new HashMap<>();
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public Instant getLockedAt() {
        return lockedAt;
    }

    public void setLockedAt(Instant lockedAt) {
        this.lockedAt = lockedAt;
    }

    public Map<String, LockedInstallation> getInstallations() {
        return installations;
    }

    public void setInstallations(Map<String, LockedInstallation> installations) {
        this.installations = installations;
    }

    /**
     * A single installation entry in mods.lock.json.
     * Keyed by sourceId (e.g., "curseforge:12345") in the parent map.
     */
    public static class LockedInstallation {
        private PluginIdentifier identifier;
        private String installedVersionId;
        private String installedVersionNumber;
        private String filePath;
        private String fileName;
        private long fileSize;
        private String fileHash;
        private Instant installedAt;
        private Instant lastChecked;

        public LockedInstallation() {
        }

        public LockedInstallation(PluginIdentifier identifier, String installedVersionId,
                                  String installedVersionNumber, String filePath, String fileName,
                                  long fileSize, String fileHash, Instant installedAt, Instant lastChecked) {
            this.identifier = identifier;
            this.installedVersionId = installedVersionId;
            this.installedVersionNumber = installedVersionNumber;
            this.filePath = filePath;
            this.fileName = fileName;
            this.fileSize = fileSize;
            this.fileHash = fileHash;
            this.installedAt = installedAt;
            this.lastChecked = lastChecked;
        }

        public PluginIdentifier getIdentifier() {
            return identifier;
        }

        public void setIdentifier(PluginIdentifier identifier) {
            this.identifier = identifier;
        }

        public String getInstalledVersionId() {
            return installedVersionId;
        }

        public void setInstalledVersionId(String installedVersionId) {
            this.installedVersionId = installedVersionId;
        }

        public String getInstalledVersionNumber() {
            return installedVersionNumber;
        }

        public void setInstalledVersionNumber(String installedVersionNumber) {
            this.installedVersionNumber = installedVersionNumber;
        }

        public String getFilePath() {
            return filePath;
        }

        public void setFilePath(String filePath) {
            this.filePath = filePath;
        }

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public long getFileSize() {
            return fileSize;
        }

        public void setFileSize(long fileSize) {
            this.fileSize = fileSize;
        }

        public String getFileHash() {
            return fileHash;
        }

        public void setFileHash(String fileHash) {
            this.fileHash = fileHash;
        }

        public Instant getInstalledAt() {
            return installedAt;
        }

        public void setInstalledAt(Instant installedAt) {
            this.installedAt = installedAt;
        }

        public Instant getLastChecked() {
            return lastChecked;
        }

        public void setLastChecked(Instant lastChecked) {
            this.lastChecked = lastChecked;
        }
    }
}
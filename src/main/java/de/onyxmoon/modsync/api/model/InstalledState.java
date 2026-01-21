package de.onyxmoon.modsync.api.model;

import com.hypixel.hytale.common.plugin.PluginIdentifier;

import java.time.Instant;
import java.util.Objects;

/**
 * Represents the installed state of a managed mod.
 * This is an embedded value object within ManagedMod that captures
 * all information about the currently installed version of the mod.
 *
 * <p>This class is immutable. Use {@link #toBuilder()} to create modified copies.</p>
 */
public final class InstalledState {

    private final PluginIdentifier identifier;
    private final String installedVersionId;
    private final String installedVersionNumber;
    private final String filePath;
    private final String fileName;
    private final long fileSize;
    private final String fileHash;
    private final Instant installedAt;
    private final Instant lastChecked;

    private InstalledState(Builder builder) {
        this.identifier = builder.identifier;
        this.installedVersionId = builder.installedVersionId;
        this.installedVersionNumber = builder.installedVersionNumber;
        this.filePath = builder.filePath;
        this.fileName = builder.fileName;
        this.fileSize = builder.fileSize;
        this.fileHash = builder.fileHash;
        this.installedAt = builder.installedAt;
        this.lastChecked = builder.lastChecked;
    }

    public PluginIdentifier getIdentifier() {
        return identifier;
    }

    public String getInstalledVersionId() {
        return installedVersionId;
    }

    public String getInstalledVersionNumber() {
        return installedVersionNumber;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getFileName() {
        return fileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public String getFileHash() {
        return fileHash;
    }

    public Instant getInstalledAt() {
        return installedAt;
    }

    public Instant getLastChecked() {
        return lastChecked;
    }

    public Builder toBuilder() {
        return new Builder()
                .identifier(this.identifier)
                .installedVersionId(this.installedVersionId)
                .installedVersionNumber(this.installedVersionNumber)
                .filePath(this.filePath)
                .fileName(this.fileName)
                .fileSize(this.fileSize)
                .fileHash(this.fileHash)
                .installedAt(this.installedAt)
                .lastChecked(this.lastChecked);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InstalledState that = (InstalledState) o;
        return fileSize == that.fileSize &&
                Objects.equals(identifier, that.identifier) &&
                Objects.equals(installedVersionId, that.installedVersionId) &&
                Objects.equals(installedVersionNumber, that.installedVersionNumber) &&
                Objects.equals(filePath, that.filePath) &&
                Objects.equals(fileName, that.fileName) &&
                Objects.equals(fileHash, that.fileHash) &&
                Objects.equals(installedAt, that.installedAt) &&
                Objects.equals(lastChecked, that.lastChecked);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifier, installedVersionId, installedVersionNumber,
                filePath, fileName, fileSize, fileHash, installedAt, lastChecked);
    }

    @Override
    public String toString() {
        return "InstalledState{" +
                "identifier=" + identifier +
                ", installedVersionId='" + installedVersionId + '\'' +
                ", installedVersionNumber='" + installedVersionNumber + '\'' +
                ", filePath='" + filePath + '\'' +
                ", fileName='" + fileName + '\'' +
                ", fileSize=" + fileSize +
                ", fileHash='" + fileHash + '\'' +
                ", installedAt=" + installedAt +
                ", lastChecked=" + lastChecked +
                '}';
    }

    public static final class Builder {
        private PluginIdentifier identifier;
        private String installedVersionId;
        private String installedVersionNumber;
        private String filePath;
        private String fileName;
        private long fileSize;
        private String fileHash;
        private Instant installedAt;
        private Instant lastChecked;

        private Builder() {
        }

        public Builder identifier(PluginIdentifier identifier) {
            this.identifier = identifier;
            return this;
        }

        public Builder installedVersionId(String installedVersionId) {
            this.installedVersionId = installedVersionId;
            return this;
        }

        public Builder installedVersionNumber(String installedVersionNumber) {
            this.installedVersionNumber = installedVersionNumber;
            return this;
        }

        public Builder filePath(String filePath) {
            this.filePath = filePath;
            return this;
        }

        public Builder fileName(String fileName) {
            this.fileName = fileName;
            return this;
        }

        public Builder fileSize(long fileSize) {
            this.fileSize = fileSize;
            return this;
        }

        public Builder fileHash(String fileHash) {
            this.fileHash = fileHash;
            return this;
        }

        public Builder installedAt(Instant installedAt) {
            this.installedAt = installedAt;
            return this;
        }

        public Builder lastChecked(Instant lastChecked) {
            this.lastChecked = lastChecked;
            return this;
        }

        public InstalledState build() {
            return new InstalledState(this);
        }
    }
}
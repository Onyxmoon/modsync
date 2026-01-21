package de.onyxmoon.modsync.api.model.provider;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Immutable model representing a mod version.
 * This is an API response model from mod providers (e.g., CurseForge).
 */
public final class ModVersion {
    private final String versionId;
    private final String versionNumber;
    private final String fileName;
    private final long fileSize;
    private final String downloadUrl;
    private final List<String> gameVersions;
    private final String releaseType;
    private final Instant uploadedAt;
    private final String changelog;

    private ModVersion(Builder builder) {
        this.versionId = Objects.requireNonNull(builder.versionId, "versionId cannot be null");
        this.versionNumber = Objects.requireNonNull(builder.versionNumber, "versionNumber cannot be null");
        this.fileName = Objects.requireNonNull(builder.fileName, "fileName cannot be null");
        this.fileSize = builder.fileSize;
        this.downloadUrl = builder.downloadUrl;
        this.gameVersions = builder.gameVersions != null ? List.copyOf(builder.gameVersions) : List.of();
        this.releaseType = builder.releaseType;
        this.uploadedAt = builder.uploadedAt;
        this.changelog = builder.changelog;
    }

    public String getVersionId() {
        return versionId;
    }

    public String getVersionNumber() {
        return versionNumber;
    }

    public String getFileName() {
        return fileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public List<String> getGameVersions() {
        return gameVersions;
    }

    public String getReleaseType() {
        return releaseType;
    }

    public Instant getUploadedAt() {
        return uploadedAt;
    }

    public String getChangelog() {
        return changelog;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ModVersion that = (ModVersion) o;
        return fileSize == that.fileSize &&
               Objects.equals(versionId, that.versionId) &&
               Objects.equals(versionNumber, that.versionNumber) &&
               Objects.equals(fileName, that.fileName) &&
               Objects.equals(downloadUrl, that.downloadUrl) &&
               Objects.equals(gameVersions, that.gameVersions) &&
               Objects.equals(releaseType, that.releaseType) &&
               Objects.equals(uploadedAt, that.uploadedAt) &&
               Objects.equals(changelog, that.changelog);
    }

    @Override
    public int hashCode() {
        return Objects.hash(versionId, versionNumber, fileName, fileSize, downloadUrl,
                          gameVersions, releaseType, uploadedAt, changelog);
    }

    @Override
    public String toString() {
        return "ModVersion{" +
               "versionId='" + versionId + '\'' +
               ", versionNumber='" + versionNumber + '\'' +
               ", fileName='" + fileName + '\'' +
               ", fileSize=" + fileSize +
               ", downloadUrl='" + downloadUrl + '\'' +
               ", gameVersions=" + gameVersions +
               ", releaseType='" + releaseType + '\'' +
               ", uploadedAt=" + uploadedAt +
               '}';
    }

    public static class Builder {
        private String versionId;
        private String versionNumber;
        private String fileName;
        private long fileSize;
        private String downloadUrl;
        private List<String> gameVersions;
        private String releaseType;
        private Instant uploadedAt;
        private String changelog;

        public Builder versionId(String versionId) {
            this.versionId = versionId;
            return this;
        }

        public Builder versionNumber(String versionNumber) {
            this.versionNumber = versionNumber;
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

        public Builder downloadUrl(String downloadUrl) {
            this.downloadUrl = downloadUrl;
            return this;
        }

        public Builder gameVersions(List<String> gameVersions) {
            this.gameVersions = gameVersions;
            return this;
        }

        public Builder releaseType(String releaseType) {
            this.releaseType = releaseType;
            return this;
        }

        public Builder uploadedAt(Instant uploadedAt) {
            this.uploadedAt = uploadedAt;
            return this;
        }

        public Builder changelog(String changelog) {
            this.changelog = changelog;
            return this;
        }

        public ModVersion build() {
            return new ModVersion(this);
        }
    }
}
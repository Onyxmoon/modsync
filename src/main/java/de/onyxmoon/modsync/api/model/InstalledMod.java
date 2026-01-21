package de.onyxmoon.modsync.api.model;

import com.hypixel.hytale.common.plugin.PluginIdentifier;
import de.onyxmoon.modsync.api.ModListSource;
import de.onyxmoon.modsync.api.PluginType;

import java.time.Instant;
import java.util.Objects;

/**
 * Immutable model representing an installed mod on disk.
 */
public final class InstalledMod {
    private final String modId;
    private final String name;
    private final String slug;
    private final PluginIdentifier identifier;
    private final ModListSource source;
    private final PluginType pluginType;
    private final String installedVersionId;
    private final String installedVersionNumber;
    private final String filePath;
    private final String fileName;
    private final long fileSize;
    private final String fileHash;
    private final Instant installedAt;
    private final Instant lastChecked;

    private InstalledMod(Builder builder) {
        this.modId = Objects.requireNonNull(builder.modId, "modId cannot be null");
        this.name = Objects.requireNonNull(builder.name, "name cannot be null");
        this.slug = builder.slug;
        this.source = Objects.requireNonNull(builder.source, "source cannot be null");
        this.identifier = Objects.requireNonNull(builder.identifier, "identifier cannot be null");
        this.pluginType = builder.pluginType != null ? builder.pluginType : PluginType.PLUGIN;
        this.installedVersionId = builder.installedVersionId;
        this.installedVersionNumber = builder.installedVersionNumber;
        this.filePath = Objects.requireNonNull(builder.filePath, "filePath cannot be null");
        this.fileName = Objects.requireNonNull(builder.fileName, "fileName cannot be null");
        this.fileSize = builder.fileSize;
        this.fileHash = builder.fileHash;
        this.installedAt = Objects.requireNonNull(builder.installedAt, "installedAt cannot be null");
        this.lastChecked = builder.lastChecked;
    }

    public String getModId() {
        return modId;
    }

    public String getName() {
        return name;
    }

    public String getSlug() {
        return slug;
    }

    public PluginIdentifier getIdentifier() { return identifier; }

    public ModListSource getSource() {
        return source;
    }

    public PluginType getPluginType() {
        return pluginType != null ? pluginType : PluginType.PLUGIN;
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

    /**
     * Creates a unique source identifier for this mod.
     * Format: "source:modId" (e.g., "curseforge:12345")
     */
    public String getSourceId() {
        return source.name().toLowerCase() + ":" + modId;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a new builder pre-filled with this mod's values.
     */
    public Builder toBuilder() {
        return new Builder()
                .modId(modId)
                .name(name)
                .slug(slug)
                .identifier(identifier)
                .source(source)
                .pluginType(pluginType)
                .installedVersionId(installedVersionId)
                .installedVersionNumber(installedVersionNumber)
                .filePath(filePath)
                .fileName(fileName)
                .fileSize(fileSize)
                .fileHash(fileHash)
                .installedAt(installedAt)
                .lastChecked(lastChecked);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InstalledMod that = (InstalledMod) o;
        return fileSize == that.fileSize &&
               Objects.equals(modId, that.modId) &&
               Objects.equals(name, that.name) &&
               Objects.equals(slug, that.slug) &&
               Objects.equals(identifier, that.identifier) &&
               source == that.source &&
               pluginType == that.pluginType &&
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
        return Objects.hash(modId, name, slug, identifier, source, pluginType, installedVersionId, installedVersionNumber,
                           filePath, fileName, fileSize, fileHash, installedAt, lastChecked);
    }

    @Override
    public String toString() {
        return "InstalledMod{" +
               "modId='" + modId + '\'' +
               ", name='" + name + '\'' +
               ", source=" + source +
               ", pluginType=" + pluginType +
               ", version='" + installedVersionNumber + '\'' +
               ", fileName='" + fileName + '\'' +
               '}';
    }

    public static class Builder {
        private String modId;
        private String name;
        private String slug;
        private PluginIdentifier identifier;
        private ModListSource source;
        private PluginType pluginType;
        private String installedVersionId;
        private String installedVersionNumber;
        private String filePath;
        private String fileName;
        private long fileSize;
        private String fileHash;
        private Instant installedAt;
        private Instant lastChecked;

        public Builder modId(String modId) {
            this.modId = modId;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder slug(String slug) {
            this.slug = slug;
            return this;
        }

        public Builder identifier(PluginIdentifier identifier) {
            this.identifier = identifier;
            return this;
        }

        public Builder source(ModListSource source) {
            this.source = source;
            return this;
        }

        public Builder pluginType(PluginType pluginType) {
            this.pluginType = pluginType;
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

        public InstalledMod build() {
            return new InstalledMod(this);
        }
    }
}
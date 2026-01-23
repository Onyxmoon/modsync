package de.onyxmoon.modsync.api.model.provider;

import de.onyxmoon.modsync.api.PluginType;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Immutable model representing a single mod entry.
 * This is an API response model from mod providers (e.g., CurseForge).
 */
public final class ModEntry {
    private final String modId;
    private final String name;
    private final String slug;
    private final String summary;
    private final List<ModAuthor> authors;
    private final ModVersion latestVersion;
    private final List<ModVersion> availableVersions;
    private final List<String> categories;
    private final PluginType pluginType;
    private final int downloadCount;
    private final String websiteUrl;
    private final String logoUrl;
    private final Instant lastUpdated;

    private ModEntry(Builder builder) {
        this.modId = Objects.requireNonNull(builder.modId, "modId cannot be null");
        this.name = Objects.requireNonNull(builder.name, "name cannot be null");
        this.slug = builder.slug;
        this.summary = builder.summary;
        this.authors = builder.authors != null ? List.copyOf(builder.authors) : List.of();
        this.latestVersion = builder.latestVersion;
        this.availableVersions = builder.availableVersions != null ? List.copyOf(builder.availableVersions) : List.of();
        this.categories = builder.categories != null ? List.copyOf(builder.categories) : List.of();
        this.pluginType = builder.pluginType != null ? builder.pluginType : PluginType.PLUGIN;
        this.downloadCount = builder.downloadCount;
        this.websiteUrl = builder.websiteUrl;
        this.logoUrl = builder.logoUrl;
        this.lastUpdated = builder.lastUpdated;
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

    public String getSummary() {
        return summary;
    }

    public List<ModAuthor> getAuthors() {
        return authors;
    }

    public ModVersion getLatestVersion() {
        return latestVersion;
    }

    /**
     * Gets all available versions from the provider.
     * Versions are typically sorted by upload date (newest first).
     *
     * @return list of available versions, empty if not provided
     */
    public List<ModVersion> getAvailableVersions() {
        return availableVersions;
    }

    public List<String> getCategories() {
        return categories;
    }

    public PluginType getPluginType() {
        return pluginType;
    }

    public int getDownloadCount() {
        return downloadCount;
    }

    public String getWebsiteUrl() {
        return websiteUrl;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public Instant getLastUpdated() {
        return lastUpdated;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ModEntry modEntry = (ModEntry) o;
        return downloadCount == modEntry.downloadCount &&
               Objects.equals(modId, modEntry.modId) &&
               Objects.equals(name, modEntry.name) &&
               Objects.equals(slug, modEntry.slug) &&
               Objects.equals(summary, modEntry.summary) &&
               Objects.equals(authors, modEntry.authors) &&
               Objects.equals(latestVersion, modEntry.latestVersion) &&
               Objects.equals(categories, modEntry.categories) &&
               pluginType == modEntry.pluginType &&
               Objects.equals(websiteUrl, modEntry.websiteUrl) &&
               Objects.equals(logoUrl, modEntry.logoUrl) &&
               Objects.equals(lastUpdated, modEntry.lastUpdated);
    }

    @Override
    public int hashCode() {
        return Objects.hash(modId, name, slug, summary, authors, latestVersion,
                          categories, pluginType, downloadCount, websiteUrl, logoUrl, lastUpdated);
    }

    @Override
    public String toString() {
        return "ModEntry{" +
               "modId='" + modId + '\'' +
               ", name='" + name + '\'' +
               ", slug='" + slug + '\'' +
               ", pluginType=" + pluginType +
               ", authors=" + authors +
               ", downloadCount=" + downloadCount +
               '}';
    }

    public static class Builder {
        private String modId;
        private String name;
        private String slug;
        private String summary;
        private List<ModAuthor> authors;
        private ModVersion latestVersion;
        private List<ModVersion> availableVersions;
        private List<String> categories;
        private PluginType pluginType;
        private int downloadCount;
        private String websiteUrl;
        private String logoUrl;
        private Instant lastUpdated;

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

        public Builder summary(String summary) {
            this.summary = summary;
            return this;
        }

        public Builder authors(List<ModAuthor> authors) {
            this.authors = authors;
            return this;
        }

        public Builder latestVersion(ModVersion latestVersion) {
            this.latestVersion = latestVersion;
            return this;
        }

        public Builder availableVersions(List<ModVersion> availableVersions) {
            this.availableVersions = availableVersions;
            return this;
        }

        public Builder categories(List<String> categories) {
            this.categories = categories;
            return this;
        }

        public Builder pluginType(PluginType pluginType) {
            this.pluginType = pluginType;
            return this;
        }

        public Builder downloadCount(int downloadCount) {
            this.downloadCount = downloadCount;
            return this;
        }

        public Builder websiteUrl(String websiteUrl) {
            this.websiteUrl = websiteUrl;
            return this;
        }

        public Builder logoUrl(String logoUrl) {
            this.logoUrl = logoUrl;
            return this;
        }

        public Builder lastUpdated(Instant lastUpdated) {
            this.lastUpdated = lastUpdated;
            return this;
        }

        public ModEntry build() {
            return new ModEntry(this);
        }
    }
}
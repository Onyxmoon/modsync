package de.onyxmoon.modsync.api.model;

import de.onyxmoon.modsync.api.ModListSource;

import java.time.Instant;
import java.util.Objects;

/**
 * Immutable model representing a mod entry in the user's managed mod list.
 * This represents a mod the user wants to track/install, not one that is already installed.
 */
public final class ManagedModEntry {
    private final String modId;
    private final ModListSource source;
    private final String slug;
    private final String name;
    private final String desiredVersionId;
    private final Instant addedAt;
    private final String addedViaUrl;

    private ManagedModEntry(Builder builder) {
        this.modId = Objects.requireNonNull(builder.modId, "modId cannot be null");
        this.source = Objects.requireNonNull(builder.source, "source cannot be null");
        this.slug = builder.slug;
        this.name = Objects.requireNonNull(builder.name, "name cannot be null");
        this.desiredVersionId = builder.desiredVersionId;
        this.addedAt = Objects.requireNonNull(builder.addedAt, "addedAt cannot be null");
        this.addedViaUrl = builder.addedViaUrl;
    }

    public String getModId() {
        return modId;
    }

    public ModListSource getSource() {
        return source;
    }

    public String getSlug() {
        return slug;
    }

    public String getName() {
        return name;
    }

    /**
     * Get the desired version ID. If null, the latest version should be used.
     */
    public String getDesiredVersionId() {
        return desiredVersionId;
    }

    /**
     * Check if this entry wants the latest version.
     */
    public boolean wantsLatestVersion() {
        return desiredVersionId == null;
    }

    public Instant getAddedAt() {
        return addedAt;
    }

    /**
     * Get the original URL used to add this mod, if any.
     */
    public String getAddedViaUrl() {
        return addedViaUrl;
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
     * Creates a new builder pre-filled with this entry's values.
     */
    public Builder toBuilder() {
        return new Builder()
                .modId(modId)
                .source(source)
                .slug(slug)
                .name(name)
                .desiredVersionId(desiredVersionId)
                .addedAt(addedAt)
                .addedViaUrl(addedViaUrl);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ManagedModEntry that = (ManagedModEntry) o;
        return Objects.equals(modId, that.modId) &&
               source == that.source &&
               Objects.equals(slug, that.slug) &&
               Objects.equals(name, that.name) &&
               Objects.equals(desiredVersionId, that.desiredVersionId) &&
               Objects.equals(addedAt, that.addedAt) &&
               Objects.equals(addedViaUrl, that.addedViaUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(modId, source, slug, name, desiredVersionId, addedAt, addedViaUrl);
    }

    @Override
    public String toString() {
        return "ManagedModEntry{" +
               "modId='" + modId + '\'' +
               ", source=" + source +
               ", name='" + name + '\'' +
               ", desiredVersion=" + (desiredVersionId != null ? desiredVersionId : "latest") +
               '}';
    }

    public static class Builder {
        private String modId;
        private ModListSource source;
        private String slug;
        private String name;
        private String desiredVersionId;
        private Instant addedAt;
        private String addedViaUrl;

        public Builder modId(String modId) {
            this.modId = modId;
            return this;
        }

        public Builder source(ModListSource source) {
            this.source = source;
            return this;
        }

        public Builder slug(String slug) {
            this.slug = slug;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder desiredVersionId(String desiredVersionId) {
            this.desiredVersionId = desiredVersionId;
            return this;
        }

        public Builder addedAt(Instant addedAt) {
            this.addedAt = addedAt;
            return this;
        }

        public Builder addedViaUrl(String addedViaUrl) {
            this.addedViaUrl = addedViaUrl;
            return this;
        }

        public ManagedModEntry build() {
            return new ManagedModEntry(this);
        }
    }
}
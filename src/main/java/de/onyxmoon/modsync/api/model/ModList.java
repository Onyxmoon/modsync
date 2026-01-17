package de.onyxmoon.modsync.api.model;

import de.onyxmoon.modsync.api.ModListSource;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Immutable model representing a mod list from any source.
 */
public final class ModList {
    private final ModListSource source;
    private final String projectId;
    private final String projectName;
    private final List<ModEntry> mods;
    private final Instant fetchedAt;
    private final String sourceVersion;

    private ModList(Builder builder) {
        this.source = Objects.requireNonNull(builder.source, "source cannot be null");
        this.projectId = Objects.requireNonNull(builder.projectId, "projectId cannot be null");
        this.projectName = builder.projectName;
        this.mods = Objects.requireNonNull(builder.mods, "mods cannot be null");
        this.fetchedAt = Objects.requireNonNull(builder.fetchedAt, "fetchedAt cannot be null");
        this.sourceVersion = builder.sourceVersion;
    }

    public ModListSource getSource() {
        return source;
    }

    public String getProjectId() {
        return projectId;
    }

    public String getProjectName() {
        return projectName;
    }

    public List<ModEntry> getMods() {
        return mods;
    }

    public Instant getFetchedAt() {
        return fetchedAt;
    }

    public String getSourceVersion() {
        return sourceVersion;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ModList modList = (ModList) o;
        return source == modList.source &&
               Objects.equals(projectId, modList.projectId) &&
               Objects.equals(projectName, modList.projectName) &&
               Objects.equals(mods, modList.mods) &&
               Objects.equals(fetchedAt, modList.fetchedAt) &&
               Objects.equals(sourceVersion, modList.sourceVersion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(source, projectId, projectName, mods, fetchedAt, sourceVersion);
    }

    @Override
    public String toString() {
        return "ModList{" +
               "source=" + source +
               ", projectId='" + projectId + '\'' +
               ", projectName='" + projectName + '\'' +
               ", modCount=" + mods.size() +
               ", fetchedAt=" + fetchedAt +
               '}';
    }

    public static class Builder {
        private ModListSource source;
        private String projectId;
        private String projectName;
        private List<ModEntry> mods;
        private Instant fetchedAt;
        private String sourceVersion;

        public Builder source(ModListSource source) {
            this.source = source;
            return this;
        }

        public Builder projectId(String projectId) {
            this.projectId = projectId;
            return this;
        }

        public Builder projectName(String projectName) {
            this.projectName = projectName;
            return this;
        }

        public Builder mods(List<ModEntry> mods) {
            this.mods = mods != null ? List.copyOf(mods) : List.of();
            return this;
        }

        public Builder fetchedAt(Instant fetchedAt) {
            this.fetchedAt = fetchedAt;
            return this;
        }

        public Builder sourceVersion(String sourceVersion) {
            this.sourceVersion = sourceVersion;
            return this;
        }

        public ModList build() {
            return new ModList(this);
        }
    }
}
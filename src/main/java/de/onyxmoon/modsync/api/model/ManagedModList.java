package de.onyxmoon.modsync.api.model;

import de.onyxmoon.modsync.api.ModListSource;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents the user's curated list of mods they want to manage.
 * This is independent of any remote modpack - it's the user's own list.
 */
public final class ManagedModList {
    private final String name;
    private final Map<String, ManagedModEntry> mods;
    private final Instant createdAt;
    private final Instant lastModifiedAt;

    private ManagedModList(Builder builder) {
        this.name = Objects.requireNonNull(builder.name, "name cannot be null");
        this.mods = builder.mods != null ? Map.copyOf(builder.mods) : Map.of();
        this.createdAt = Objects.requireNonNull(builder.createdAt, "createdAt cannot be null");
        this.lastModifiedAt = builder.lastModifiedAt != null ? builder.lastModifiedAt : builder.createdAt;
    }

    public String getName() {
        return name;
    }

    /**
     * Get all mod entries in this list.
     */
    public List<ManagedModEntry> getMods() {
        return List.copyOf(mods.values());
    }

    /**
     * Find a mod by its source ID.
     *
     * @param sourceId the combined source ID (e.g., "curseforge:12345")
     * @return the mod entry if found
     */
    public Optional<ManagedModEntry> findBySourceId(String sourceId) {
        return Optional.ofNullable(mods.get(sourceId));
    }

    /**
     * Find a mod by its source and mod ID.
     */
    public Optional<ManagedModEntry> findBySourceId(ModListSource source, String modId) {
        String sourceId = source.name().toLowerCase() + ":" + modId;
        return findBySourceId(sourceId);
    }

    /**
     * Find a mod by its slug
     */
    public Optional<ManagedModEntry> findBySlug(String slug) {
        return mods.values().stream()
                .filter(mod -> mod.getSlug() != null && mod.getSlug().equalsIgnoreCase(slug))
                .findFirst();
    }

    /**
     * Find a mod by its name
     */
    public Optional<ManagedModEntry> findByName(String name) {
        return mods.values().stream()
                .filter(mod -> mod.getName().equalsIgnoreCase(name))
                .findFirst();
    }

    /**
     * Get all mods from a specific source.
     */
    public List<ManagedModEntry> getBySource(ModListSource source) {
        return mods.values().stream()
                .filter(mod -> mod.getSource() == source)
                .collect(Collectors.toList());
    }

    /**
     * Check if a mod is in the list.
     */
    public boolean contains(String sourceId) {
        return mods.containsKey(sourceId);
    }

    /**
     * Check if a mod is in the list.
     */
    public boolean contains(ModListSource source, String modId) {
        String sourceId = source.name().toLowerCase() + ":" + modId;
        return contains(sourceId);
    }

    /**
     * Get the number of mods in this list.
     */
    public int size() {
        return mods.size();
    }

    /**
     * Check if this list is empty.
     */
    public boolean isEmpty() {
        return mods.isEmpty();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastModifiedAt() {
        return lastModifiedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a new builder pre-filled with this list's values.
     */
    public Builder toBuilder() {
        return new Builder()
                .name(name)
                .mods(new HashMap<>(mods))
                .createdAt(createdAt)
                .lastModifiedAt(lastModifiedAt);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ManagedModList that = (ManagedModList) o;
        return Objects.equals(name, that.name) &&
               Objects.equals(mods, that.mods) &&
               Objects.equals(createdAt, that.createdAt) &&
               Objects.equals(lastModifiedAt, that.lastModifiedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, mods, createdAt, lastModifiedAt);
    }

    @Override
    public String toString() {
        return "ManagedModList{" +
               "name='" + name + '\'' +
               ", modCount=" + mods.size() +
               ", lastModifiedAt=" + lastModifiedAt +
               '}';
    }

    public static class Builder {
        private String name = "default";
        private Map<String, ManagedModEntry> mods = new HashMap<>();
        private Instant createdAt;
        private Instant lastModifiedAt;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder mods(Map<String, ManagedModEntry> mods) {
            this.mods = mods;
            return this;
        }

        public Builder addMod(ManagedModEntry mod) {
            this.mods.put(mod.getSourceId(), mod);
            return this;
        }

        public Builder removeMod(String sourceId) {
            this.mods.remove(sourceId);
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder lastModifiedAt(Instant lastModifiedAt) {
            this.lastModifiedAt = lastModifiedAt;
            return this;
        }

        public ManagedModList build() {
            if (createdAt == null) {
                createdAt = Instant.now();
            }
            return new ManagedModList(this);
        }
    }
}
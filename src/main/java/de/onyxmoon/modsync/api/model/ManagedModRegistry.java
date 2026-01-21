package de.onyxmoon.modsync.api.model;

import de.onyxmoon.modsync.api.ModListSource;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Registry for all managed mods.
 * Provides unified lookup capabilities by sourceId, name, slug, and identifier.
 *
 * <p>This class is immutable. Use {@link #toBuilder()} to create modified copies.</p>
 */
public final class ManagedModRegistry {

    private final String name;
    private final Map<String, ManagedMod> mods;
    private final Instant createdAt;
    private final Instant lastModifiedAt;

    private ManagedModRegistry(Builder builder) {
        this.name = builder.name;
        this.mods = Collections.unmodifiableMap(new LinkedHashMap<>(builder.mods));
        this.createdAt = builder.createdAt;
        this.lastModifiedAt = builder.lastModifiedAt;
    }

    // ----- Lookup methods -----

    /**
     * Finds a mod by its source ID.
     *
     * @param sourceId the source ID (e.g., "curseforge:12345")
     * @return an Optional containing the mod if found
     */
    public Optional<ManagedMod> findBySourceId(String sourceId) {
        return Optional.ofNullable(mods.get(sourceId));
    }

    /**
     * Finds a mod by its source and mod ID.
     *
     * @param source the mod source
     * @param modId the mod ID
     * @return an Optional containing the mod if found
     */
    public Optional<ManagedMod> findBySourceId(ModListSource source, String modId) {
        return findBySourceId(source.name().toLowerCase() + ":" + modId);
    }

    /**
     * Finds a mod by its name (case-insensitive).
     *
     * @param name the mod name to search for
     * @return an Optional containing the first matching mod
     */
    public Optional<ManagedMod> findByName(String name) {
        return mods.values().stream()
                .filter(mod -> mod.getName() != null && mod.getName().equalsIgnoreCase(name))
                .findFirst();
    }

    /**
     * Finds a mod by its slug (case-insensitive).
     *
     * @param slug the slug to search for
     * @return an Optional containing the first matching mod
     */
    public Optional<ManagedMod> findBySlug(String slug) {
        return mods.values().stream()
                .filter(mod -> mod.getSlug() != null && mod.getSlug().equalsIgnoreCase(slug))
                .findFirst();
    }

    /**
     * Finds a mod by its plugin identifier string (group:name format).
     * Only searches installed mods.
     *
     * @param identifier the plugin identifier (e.g., "Onyxmoon:ModSync")
     * @return an Optional containing the mod if found
     */
    public Optional<ManagedMod> findByIdentifier(String identifier) {
        if (identifier == null || !identifier.contains(":")) {
            return Optional.empty();
        }
        return mods.values().stream()
                .filter(ManagedMod::isInstalled)
                .filter(mod -> mod.getIdentifierString()
                        .map(id -> id.equalsIgnoreCase(identifier))
                        .orElse(false))
                .findFirst();
    }

    /**
     * Finds a mod by file path.
     * Only searches installed mods.
     *
     * @param filePath the file path to search for
     * @return an Optional containing the mod if found
     */
    public Optional<ManagedMod> findByFilePath(String filePath) {
        return mods.values().stream()
                .filter(ManagedMod::isInstalled)
                .filter(mod -> mod.getInstalledState()
                        .map(state -> filePath.equals(state.getFilePath()))
                        .orElse(false))
                .findFirst();
    }

    // ----- Collection methods -----

    /**
     * Gets all managed mods.
     *
     * @return an unmodifiable list of all mods
     */
    public List<ManagedMod> getAll() {
        return new ArrayList<>(mods.values());
    }

    /**
     * Gets all installed mods.
     *
     * @return a list of mods that have an installed state
     */
    public List<ManagedMod> getInstalled() {
        return mods.values().stream()
                .filter(ManagedMod::isInstalled)
                .collect(Collectors.toList());
    }

    /**
     * Gets all mods that are not installed.
     *
     * @return a list of mods without an installed state
     */
    public List<ManagedMod> getNotInstalled() {
        return mods.values().stream()
                .filter(mod -> !mod.isInstalled())
                .collect(Collectors.toList());
    }

    /**
     * Gets all mods from a specific source.
     *
     * @param source the source to filter by
     * @return a list of mods from that source
     */
    public List<ManagedMod> getBySource(ModListSource source) {
        return mods.values().stream()
                .filter(mod -> mod.getSource() == source)
                .collect(Collectors.toList());
    }

    /**
     * Checks if a mod with the given source ID exists.
     *
     * @param sourceId the source ID to check
     * @return true if the mod exists
     */
    public boolean contains(String sourceId) {
        return mods.containsKey(sourceId);
    }

    /**
     * Checks if a mod with the given source and mod ID exists.
     *
     * @param source the mod source
     * @param modId the mod ID
     * @return true if the mod exists
     */
    public boolean contains(ModListSource source, String modId) {
        return contains(source.name().toLowerCase() + ":" + modId);
    }

    /**
     * Checks if a mod with the given source ID is installed.
     *
     * @param sourceId the source ID to check
     * @return true if the mod exists and is installed
     */
    public boolean isInstalled(String sourceId) {
        return findBySourceId(sourceId)
                .map(ManagedMod::isInstalled)
                .orElse(false);
    }

    public int size() {
        return mods.size();
    }

    public boolean isEmpty() {
        return mods.isEmpty();
    }

    public String getName() {
        return name;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastModifiedAt() {
        return lastModifiedAt;
    }

    public Builder toBuilder() {
        Builder builder = new Builder()
                .name(this.name)
                .createdAt(this.createdAt)
                .lastModifiedAt(this.lastModifiedAt);
        this.mods.values().forEach(builder::addMod);
        return builder;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates an empty registry with default values.
     *
     * @return a new empty registry
     */
    public static ManagedModRegistry empty() {
        return builder()
                .name("default")
                .createdAt(Instant.now())
                .lastModifiedAt(Instant.now())
                .build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ManagedModRegistry that = (ManagedModRegistry) o;
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
        return "ManagedModRegistry{" +
                "name='" + name + '\'' +
                ", size=" + mods.size() +
                ", createdAt=" + createdAt +
                ", lastModifiedAt=" + lastModifiedAt +
                '}';
    }

    public static final class Builder {
        private String name = "default";
        private final Map<String, ManagedMod> mods = new LinkedHashMap<>();
        private Instant createdAt = Instant.now();
        private Instant lastModifiedAt = Instant.now();

        private Builder() {
        }

        public Builder name(String name) {
            this.name = name;
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

        /**
         * Adds a mod to the registry.
         * If a mod with the same sourceId exists, it will be replaced.
         *
         * @param mod the mod to add
         * @return this builder
         */
        public Builder addMod(ManagedMod mod) {
            if (mod != null) {
                this.mods.put(mod.getSourceId(), mod);
            }
            return this;
        }

        /**
         * Removes a mod from the registry.
         *
         * @param sourceId the source ID of the mod to remove
         * @return this builder
         */
        public Builder removeMod(String sourceId) {
            this.mods.remove(sourceId);
            return this;
        }

        /**
         * Clears all mods from the registry.
         *
         * @return this builder
         */
        public Builder clearMods() {
            this.mods.clear();
            return this;
        }

        public ManagedModRegistry build() {
            return new ManagedModRegistry(this);
        }
    }
}
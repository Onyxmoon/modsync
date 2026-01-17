package de.onyxmoon.modsync.api.model;

import de.onyxmoon.modsync.api.ModListSource;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Registry containing all installed mods with lookup capabilities.
 */
public final class InstalledModRegistry {
    private final Map<String, InstalledMod> modsBySourceId;
    private final Instant lastScanAt;

    private InstalledModRegistry(Builder builder) {
        this.modsBySourceId = builder.mods != null
                ? Map.copyOf(builder.mods)
                : Map.of();
        this.lastScanAt = builder.lastScanAt;
    }

    /**
     * Find an installed mod by its source and mod ID.
     *
     * @param source the mod source
     * @param modId the mod ID from the source
     * @return the installed mod if found
     */
    public Optional<InstalledMod> findBySourceId(ModListSource source, String modId) {
        String sourceId = source.name().toLowerCase() + ":" + modId;
        return Optional.ofNullable(modsBySourceId.get(sourceId));
    }

    /**
     * Find an installed mod by its combined source ID.
     *
     * @param sourceId the combined source ID (e.g., "curseforge:12345")
     * @return the installed mod if found
     */
    public Optional<InstalledMod> findBySourceId(String sourceId) {
        return Optional.ofNullable(modsBySourceId.get(sourceId));
    }

    /**
     * Find an installed mod by its file path.
     *
     * @param filePath the file path to search for
     * @return the installed mod if found
     */
    public Optional<InstalledMod> findByFilePath(String filePath) {
        return modsBySourceId.values().stream()
                .filter(mod -> mod.getFilePath().equals(filePath))
                .findFirst();
    }

    /**
     * Find an installed mod by its slug.
     *
     * @param slug the mod slug
     * @return the installed mod if found
     */
    public Optional<InstalledMod> findBySlug(String slug) {
        return modsBySourceId.values().stream()
                .filter(mod -> slug.equals(mod.getSlug()))
                .findFirst();
    }

    /**
     * Get all installed mods.
     *
     * @return unmodifiable list of all installed mods
     */
    public List<InstalledMod> getAll() {
        return List.copyOf(modsBySourceId.values());
    }

    /**
     * Get all installed mods from a specific source.
     *
     * @param source the mod source to filter by
     * @return list of installed mods from the specified source
     */
    public List<InstalledMod> getBySource(ModListSource source) {
        return modsBySourceId.values().stream()
                .filter(mod -> mod.getSource() == source)
                .collect(Collectors.toList());
    }

    /**
     * Get the number of installed mods.
     *
     * @return the count of installed mods
     */
    public int size() {
        return modsBySourceId.size();
    }

    /**
     * Check if any mods are installed.
     *
     * @return true if no mods are installed
     */
    public boolean isEmpty() {
        return modsBySourceId.isEmpty();
    }

    /**
     * Check if a mod is installed.
     *
     * @param source the mod source
     * @param modId the mod ID
     * @return true if the mod is installed
     */
    public boolean isInstalled(ModListSource source, String modId) {
        return findBySourceId(source, modId).isPresent();
    }

    /**
     * Get when the registry was last scanned/updated.
     *
     * @return the last scan timestamp, or null if never scanned
     */
    public Instant getLastScanAt() {
        return lastScanAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a new builder pre-filled with this registry's mods.
     */
    public Builder toBuilder() {
        return new Builder()
                .mods(new HashMap<>(modsBySourceId))
                .lastScanAt(lastScanAt);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InstalledModRegistry that = (InstalledModRegistry) o;
        return Objects.equals(modsBySourceId, that.modsBySourceId) &&
               Objects.equals(lastScanAt, that.lastScanAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(modsBySourceId, lastScanAt);
    }

    @Override
    public String toString() {
        return "InstalledModRegistry{" +
               "modCount=" + modsBySourceId.size() +
               ", lastScanAt=" + lastScanAt +
               '}';
    }

    public static class Builder {
        private Map<String, InstalledMod> mods = new HashMap<>();
        private Instant lastScanAt;

        public Builder mods(Map<String, InstalledMod> mods) {
            this.mods = mods;
            return this;
        }

        public Builder addMod(InstalledMod mod) {
            this.mods.put(mod.getSourceId(), mod);
            return this;
        }

        public Builder removeMod(String sourceId) {
            this.mods.remove(sourceId);
            return this;
        }

        public Builder lastScanAt(Instant lastScanAt) {
            this.lastScanAt = lastScanAt;
            return this;
        }

        public InstalledModRegistry build() {
            return new InstalledModRegistry(this);
        }
    }
}
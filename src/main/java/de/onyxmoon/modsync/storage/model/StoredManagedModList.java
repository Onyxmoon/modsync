package de.onyxmoon.modsync.storage.model;

import de.onyxmoon.modsync.api.model.ManagedModEntry;

import java.time.Instant;
import java.util.List;

/**
 * Wrapper for managed mod list with storage metadata.
 */
public class StoredManagedModList {
    private String name;
    private List<ManagedModEntry> mods;
    private Instant createdAt;
    private Instant lastModifiedAt;
    private Instant storedAt;

    public StoredManagedModList() {
    }

    public StoredManagedModList(String name, List<ManagedModEntry> mods,
                                 Instant createdAt, Instant lastModifiedAt, Instant storedAt) {
        this.name = name;
        this.mods = mods;
        this.createdAt = createdAt;
        this.lastModifiedAt = lastModifiedAt;
        this.storedAt = storedAt;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<ManagedModEntry> getMods() {
        return mods;
    }

    public void setMods(List<ManagedModEntry> mods) {
        this.mods = mods;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getLastModifiedAt() {
        return lastModifiedAt;
    }

    public void setLastModifiedAt(Instant lastModifiedAt) {
        this.lastModifiedAt = lastModifiedAt;
    }

    public Instant getStoredAt() {
        return storedAt;
    }

    public void setStoredAt(Instant storedAt) {
        this.storedAt = storedAt;
    }
}
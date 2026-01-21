package de.onyxmoon.modsync.storage.model;

import de.onyxmoon.modsync.api.model.provider.ModList;

import java.time.Instant;

/**
 * Wrapper for ModList with storage metadata.
 */
public class StoredModList {
    private ModList modList;
    private Instant storedAt;

    public StoredModList() {
    }

    public StoredModList(ModList modList, Instant storedAt) {
        this.modList = modList;
        this.storedAt = storedAt;
    }

    public ModList getModList() {
        return modList;
    }

    public void setModList(ModList modList) {
        this.modList = modList;
    }

    public Instant getStoredAt() {
        return storedAt;
    }

    public void setStoredAt(Instant storedAt) {
        this.storedAt = storedAt;
    }
}
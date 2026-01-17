package de.onyxmoon.modsync.storage.model;

import de.onyxmoon.modsync.api.model.InstalledMod;

import java.time.Instant;
import java.util.List;

/**
 * Wrapper for installed mods with storage metadata.
 */
public class StoredInstalledMods {
    private List<InstalledMod> mods;
    private Instant lastScanAt;
    private Instant storedAt;

    public StoredInstalledMods() {
    }

    public StoredInstalledMods(List<InstalledMod> mods, Instant lastScanAt, Instant storedAt) {
        this.mods = mods;
        this.lastScanAt = lastScanAt;
        this.storedAt = storedAt;
    }

    public List<InstalledMod> getMods() {
        return mods;
    }

    public void setMods(List<InstalledMod> mods) {
        this.mods = mods;
    }

    public Instant getLastScanAt() {
        return lastScanAt;
    }

    public void setLastScanAt(Instant lastScanAt) {
        this.lastScanAt = lastScanAt;
    }

    public Instant getStoredAt() {
        return storedAt;
    }

    public void setStoredAt(Instant storedAt) {
        this.storedAt = storedAt;
    }
}
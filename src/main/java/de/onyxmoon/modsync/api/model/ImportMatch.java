package de.onyxmoon.modsync.api.model;

import de.onyxmoon.modsync.api.model.provider.ModEntry;

import javax.annotation.Nullable;

/**
 * Represents a potential match between an unmanaged mod and a CurseForge entry.
 *
 * @param unmanagedMod The unmanaged mod that was scanned
 * @param matchedEntry The CurseForge entry that was matched, or null if no match
 * @param confidence   The confidence level of the match
 * @param matchReason  Human-readable explanation of how the match was made
 */
public record ImportMatch(
        UnmanagedMod unmanagedMod,
        @Nullable ModEntry matchedEntry,
        ImportMatchConfidence confidence,
        @Nullable String matchReason
) {
    /**
     * Creates a match with no result found.
     */
    public static ImportMatch noMatch(UnmanagedMod unmanagedMod) {
        return new ImportMatch(unmanagedMod, null, ImportMatchConfidence.NONE, null);
    }

    /**
     * Creates an exact match (slug-based).
     */
    public static ImportMatch exactMatch(UnmanagedMod unmanagedMod, ModEntry entry, String reason) {
        return new ImportMatch(unmanagedMod, entry, ImportMatchConfidence.EXACT, reason);
    }

    /**
     * Creates a high-confidence match (name search).
     */
    public static ImportMatch highConfidenceMatch(UnmanagedMod unmanagedMod, ModEntry entry, String reason) {
        return new ImportMatch(unmanagedMod, entry, ImportMatchConfidence.HIGH, reason);
    }

    /**
     * Creates a low-confidence match (fuzzy).
     */
    public static ImportMatch lowConfidenceMatch(UnmanagedMod unmanagedMod, ModEntry entry, String reason) {
        return new ImportMatch(unmanagedMod, entry, ImportMatchConfidence.LOW, reason);
    }

    /**
     * Returns true if a match was found.
     */
    public boolean hasMatch() {
        return matchedEntry != null && confidence != ImportMatchConfidence.NONE;
    }

    /**
     * Returns true if the match is confident enough for automatic import.
     */
    public boolean isAutoImportable() {
        return hasMatch() && confidence.isAutoImportable();
    }
}
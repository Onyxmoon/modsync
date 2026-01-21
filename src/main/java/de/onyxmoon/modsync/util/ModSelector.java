package de.onyxmoon.modsync.util;

import de.onyxmoon.modsync.api.model.ManagedMod;
import de.onyxmoon.modsync.api.model.ManagedModRegistry;

import java.util.List;
import java.util.Optional;

/**
 * Utility class for selecting mods from the registry by various criteria.
 * Centralizes the lookup logic used across multiple commands.
 */
public final class ModSelector {

    private ModSelector() {
        // Utility class
    }

    /**
     * Result of a mod selection operation.
     */
    public sealed interface SelectionResult {
        record Found(ManagedMod mod) implements SelectionResult {}
        record NotFound(String query) implements SelectionResult {}
        record InvalidIndex(int index, int maxIndex) implements SelectionResult {}
        record EmptyRegistry() implements SelectionResult {}
    }

    /**
     * Finds a mod by a flexible query string.
     * Tries in order: index (1-based), name, slug, identifier.
     *
     * @param registry the mod registry to search
     * @param query the search query (index number, name, slug, or identifier)
     * @return the selection result
     */
    public static SelectionResult find(ManagedModRegistry registry, String query) {
        if (registry.isEmpty()) {
            return new SelectionResult.EmptyRegistry();
        }

        if (query == null || query.isBlank()) {
            return new SelectionResult.NotFound("");
        }

        // Try to parse as 1-based index
        try {
            int index = Integer.parseInt(query.trim());
            return findByIndex(registry, index);
        } catch (NumberFormatException ignored) {
            // Not a number, continue with name/slug/identifier lookup
        }

        return findByNameOrSlugOrIdentifier(registry, query);
    }

    /**
     * Finds a mod by 1-based index in the registry list.
     *
     * @param registry the mod registry
     * @param index the 1-based index
     * @return the selection result
     */
    public static SelectionResult findByIndex(ManagedModRegistry registry, int index) {
        if (registry.isEmpty()) {
            return new SelectionResult.EmptyRegistry();
        }

        List<ManagedMod> mods = registry.getAll();
        if (index < 1 || index > mods.size()) {
            return new SelectionResult.InvalidIndex(index, mods.size());
        }

        return new SelectionResult.Found(mods.get(index - 1));
    }

    /**
     * Finds a mod by name, slug, or identifier (case-insensitive).
     *
     * @param registry the mod registry
     * @param query the name, slug, or identifier to search for
     * @return the selection result
     */
    public static SelectionResult findByNameOrSlugOrIdentifier(ManagedModRegistry registry, String query) {
        if (registry.isEmpty()) {
            return new SelectionResult.EmptyRegistry();
        }

        // Try by name (case-insensitive)
        Optional<ManagedMod> modOpt = registry.findByName(query);

        // Try by slug (case-insensitive)
        if (modOpt.isEmpty()) {
            modOpt = registry.findBySlug(query);
        }

        // Try by identifier (group:name) if contains colon
        if (modOpt.isEmpty() && query.contains(":")) {
            modOpt = registry.findByIdentifier(query);
        }

        return modOpt
                .map(mod -> (SelectionResult) new SelectionResult.Found(mod))
                .orElse(new SelectionResult.NotFound(query));
    }

    /**
     * Convenience method to get the mod directly or empty if not found.
     *
     * @param registry the mod registry
     * @param query the search query
     * @return Optional containing the mod if found
     */
    public static Optional<ManagedMod> findMod(ManagedModRegistry registry, String query) {
        SelectionResult result = find(registry, query);
        if (result instanceof SelectionResult.Found found) {
            return Optional.of(found.mod());
        }
        return Optional.empty();
    }
}
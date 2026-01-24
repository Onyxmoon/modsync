package de.onyxmoon.modsync.api;

import de.onyxmoon.modsync.api.model.provider.ModEntry;
import de.onyxmoon.modsync.api.model.provider.ModList;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Service Provider Interface for mod list providers.
 * Implementations should be registered via META-INF/services/de.onyxmoon.modsync.api.ModListProvider
 */
public interface ModListProvider extends ModUrlParser {

    /**
     * Gets the source type this provider handles.
     *
     * @return the mod list source
     */
    ModListSource getSource();

    /**
     * Fetches the mod list from the source.
     *
     * @param apiKey API key for authentication (may be null for sources that don't require it)
     * @param projectId Project/modpack identifier
     * @return CompletableFuture containing the mod list
     */
    CompletableFuture<ModList> fetchModList(String apiKey, String projectId);

    /**
     * Validates an API key without fetching data.
     *
     * @param apiKey API key to validate
     * @return CompletableFuture containing true if valid, false otherwise
     */
    CompletableFuture<Boolean> validateApiKey(String apiKey);

    /**
     * Checks whether this provider requires an API key.
     *
     * @return true if an API key is required, false otherwise
     */
    boolean requiresApiKey();

    /**
     * Gets the human-readable name for this provider.
     *
     * @return the display name
     */
    String getDisplayName();

    /**
     * Gets the maximum number of requests per minute allowed by this provider.
     *
     * @return the rate limit (requests per minute)
     */
    int getRateLimit();

    /**
     * Priority used when multiple providers can parse the same URL.
     * Higher values are tried first.
     *
     * @return priority value
     */
    default int getUrlParsePriority() {
        return 0;
    }

    /**
     * Fetches a single mod by its ID.
     *
     * @param apiKey API key for authentication (may be null for sources that don't require it)
     * @param modId the mod identifier
     * @return CompletableFuture containing the mod entry
     */
    CompletableFuture<ModEntry> fetchMod(String apiKey, String modId);

    /**
     * Fetches a single mod by its slug (URL-friendly name).
     *
     * @param apiKey API key for authentication (may be null for sources that don't require it)
     * @param slug the mod slug
     * @return CompletableFuture containing the mod entry
     */
    CompletableFuture<ModEntry> fetchModBySlug(String apiKey, String slug);

    /**
     * Searches for mods by name or keyword.
     * This is used for import matching to find potential matches for unmanaged mods.
     *
     * <p>Default implementation returns an empty list. Providers should override
     * this method if they support search functionality.</p>
     *
     * @param apiKey     API key for authentication (may be null for sources that don't require it)
     * @param searchTerm the search term (mod name or keyword)
     * @return CompletableFuture containing a list of matching mod entries
     */
    default CompletableFuture<List<ModEntry>> searchMods(String apiKey, String searchTerm) {
        return CompletableFuture.completedFuture(List.of());
    }
}

package de.onyxmoon.modsync.service;

import de.onyxmoon.modsync.ModSync;
import de.onyxmoon.modsync.api.InvalidModUrlException;
import de.onyxmoon.modsync.api.ModProvider;
import de.onyxmoon.modsync.api.ParsedModUrl;
import de.onyxmoon.modsync.api.model.provider.ModEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Service for fetching mod information from providers.
 * Handles provider iteration, API key validation, and fallback logic.
 */
public class ProviderFetchService {
    private final ModSync modSync;

    public ProviderFetchService(ModSync modSync) {
        this.modSync = modSync;
    }

    /**
     * Result of a successful fetch operation.
     *
     * @param provider  the provider that resolved the URL
     * @param parsedUrl the parsed URL information
     * @param modEntry  the fetched mod entry
     */
    public record FetchResult(
            ModProvider provider,
            ParsedModUrl parsedUrl,
            ModEntry modEntry
    ) {
    }

    /**
     * Attempts to fetch mod information from available providers for the given URL.
     * Tries each provider in order until one succeeds.
     *
     * @param url               the mod URL to fetch
     * @param onMissingApiKey   callback for each provider that requires an API key but doesn't have one configured
     * @return a CompletableFuture containing the FetchResult, or null if no provider could resolve the URL
     */
    public CompletableFuture<FetchResult> fetchFromUrl(String url, Consumer<String> onMissingApiKey) {
        List<ModProvider> providers = modSync.getUrlParserRegistry().findProviders(url);
        if (providers.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        List<String> missingApiKeys = new ArrayList<>();
        return fetchFromProviders(url, providers, missingApiKeys, 0)
                .whenComplete((result, ex) -> {
                    // Report all missing API keys after the fetch completes
                    if (onMissingApiKey != null) {
                        missingApiKeys.forEach(onMissingApiKey);
                    }
                });
    }

    /**
     * Returns a list of provider display names that can parse the given URL.
     *
     * @param url the URL to check
     * @return list of provider display names
     */
    public List<String> getProviderNamesForUrl(String url) {
        return modSync.getUrlParserRegistry().findProviders(url).stream()
                .map(ModProvider::getDisplayName)
                .toList();
    }

    private CompletableFuture<FetchResult> fetchFromProviders(
            String url,
            List<ModProvider> providers,
            List<String> missingApiKeys,
            int index) {

        if (index >= providers.size()) {
            return CompletableFuture.completedFuture(null);
        }

        ModProvider provider = providers.get(index);

        // Try to parse the URL
        ParsedModUrl parsedUrl;
        try {
            parsedUrl = provider.parse(url);
        } catch (InvalidModUrlException e) {
            return fetchFromProviders(url, providers, missingApiKeys, index + 1);
        }

        // Check if we have the required API key
        String source = provider.getSource();
        String apiKey = modSync.getConfigStorage().getConfig().getApiKey(source);

        if (provider.requiresApiKey() && (apiKey == null || apiKey.isBlank())) {
            missingApiKeys.add(provider.getDisplayName());
            return fetchFromProviders(url, providers, missingApiKeys, index + 1);
        }

        // Need either modId or slug to fetch
        if (!parsedUrl.hasModId() && parsedUrl.slug() == null) {
            return fetchFromProviders(url, providers, missingApiKeys, index + 1);
        }

        // Fetch the mod entry
        CompletableFuture<ModEntry> fetchFuture = parsedUrl.hasModId()
                ? provider.fetchMod(apiKey, parsedUrl.modId())
                : provider.fetchModBySlug(apiKey, parsedUrl.slug());

        return fetchFuture
                .handle((result, ex) -> {
                    if (ex == null && result != null) {
                        return CompletableFuture.completedFuture(new FetchResult(provider, parsedUrl, result));
                    }
                    // Try next provider on failure
                    return fetchFromProviders(url, providers, missingApiKeys, index + 1);
                })
                .thenCompose(future -> future);
    }
}

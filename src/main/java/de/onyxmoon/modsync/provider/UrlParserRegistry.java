package de.onyxmoon.modsync.provider;

import de.onyxmoon.modsync.api.ModListProvider;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Registry for URL parsers.
 */
public class UrlParserRegistry {

    private final List<ModListProvider> providers;

    public UrlParserRegistry(ProviderRegistry providerRegistry) {
        this.providers = new ArrayList<>(providerRegistry.getProviders());
    }

    /**
     * Find providers that can handle the given URL, sorted by priority.
     *
     * @param url the URL to parse
     * @return providers that can parse the URL
     */
    public List<ModListProvider> findProviders(String url) {
        return providers.stream()
                .filter(provider -> provider.canParse(url))
                .sorted(Comparator.comparingInt(ModListProvider::getUrlParsePriority).reversed())
                .toList();
    }

    /**
     * Get all registered providers that can parse URLs.
     *
     * @return list of providers
     */
    public List<ModListProvider> getProviders() {
        return List.copyOf(providers);
    }
}

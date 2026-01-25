package de.onyxmoon.modsync.provider;

import de.onyxmoon.modsync.api.ModProvider;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Registry for URL parsers.
 */
public record UrlParserRegistry(List<ModProvider> providers) {

    public UrlParserRegistry(ProviderRegistry providers) {
        this(new ArrayList<>(providers.getProviders()));
    }

    /**
     * Find providers that can handle the given URL, sorted by priority.
     *
     * @param url the URL to parse
     * @return providers that can parse the URL
     */
    public List<ModProvider> findProviders(String url) {
        return providers.stream()
                .filter(provider -> provider.canParse(url))
                .sorted(Comparator.comparingInt(ModProvider::getUrlParsePriority).reversed())
                .toList();
    }

    /**
     * Get all registered providers that can parse URLs.
     *
     * @return list of providers
     */
    @Override
    public List<ModProvider> providers() {
        return List.copyOf(providers);
    }
}

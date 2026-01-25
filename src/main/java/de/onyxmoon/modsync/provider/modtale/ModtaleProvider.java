package de.onyxmoon.modsync.provider.modtale;

import de.onyxmoon.modsync.api.InvalidModUrlException;
import de.onyxmoon.modsync.api.ModListProvider;
import de.onyxmoon.modsync.api.ModListSource;
import de.onyxmoon.modsync.api.ParsedModUrl;
import de.onyxmoon.modsync.api.model.provider.ModEntry;
import de.onyxmoon.modsync.api.model.provider.ModList;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Modtale implementation of ModListProvider.
 * Registered via META-INF/services/de.onyxmoon.modsync.api.ModListProvider
 */
public class ModtaleProvider implements ModListProvider {
    private static final int RATE_LIMIT = 300;
    private static final int URL_PRIORITY = 80;
    private final ModtaleAdapter adapter;
    private final ModtaleUrlParser urlParser;

    public ModtaleProvider() {
        this.adapter = new ModtaleAdapter();
        this.urlParser = new ModtaleUrlParser();
    }

    @Override
    public ModListSource getSource() {
        return ModListSource.MODTALE;
    }

    @Override
    public CompletableFuture<ModList> fetchModList(String apiKey, String projectId) {
        ModtaleClient client = new ModtaleClient(apiKey);
        return client.getProject(projectId)
                .thenApply(adapter::adaptToModList);
    }

    @Override
    public CompletableFuture<Boolean> validateApiKey(String apiKey) {
        ModtaleClient client = new ModtaleClient(apiKey);
        return client.validateKey();
    }

    @Override
    public boolean requiresApiKey() {
        return true;
    }

    @Override
    public String getDisplayName() {
        return "Modtale";
    }

    @Override
    public int getRateLimit() {
        return RATE_LIMIT;
    }

    @Override
    public int getUrlParsePriority() {
        return URL_PRIORITY;
    }

    @Override
    public boolean canParse(String url) {
        return urlParser.canParse(url);
    }

    @Override
    public ParsedModUrl parse(String url) throws InvalidModUrlException {
        return urlParser.parse(url);
    }

    @Override
    public CompletableFuture<ModEntry> fetchMod(String apiKey, String modId) {
        ModtaleClient client = new ModtaleClient(apiKey);
        return client.getProject(modId)
                .thenApply(adapter::adaptToModEntry);
    }

    @Override
    public CompletableFuture<ModEntry> fetchModBySlug(String apiKey, String slug) {
        ModtaleClient client = new ModtaleClient(apiKey);
        return client.searchProjects(slug, 10, 0)
                .thenApply(adapter::adaptSearch)
                .thenApply(results -> findBestMatch(results, slug));
    }

    @Override
    public CompletableFuture<List<ModEntry>> searchMods(String apiKey, String searchTerm) {
        ModtaleClient client = new ModtaleClient(apiKey);
        return client.searchProjects(searchTerm, 10, 0)
                .thenApply(adapter::adaptSearch);
    }

    private ModEntry findBestMatch(List<ModEntry> results, String slug) {
        if (results == null || results.isEmpty()) {
            throw new ModtaleApiException("Mod not found: " + slug, 404);
        }
        String normalized = slug.toLowerCase();
        return results.stream()
                .filter(entry -> entry.getName() != null && entry.getName().equalsIgnoreCase(slug))
                .findFirst()
                .orElseGet(() -> results.stream()
                        .filter(entry -> entry.getSlug() != null && entry.getSlug().equalsIgnoreCase(normalized))
                        .findFirst()
                        .orElse(results.get(0)));
    }
}

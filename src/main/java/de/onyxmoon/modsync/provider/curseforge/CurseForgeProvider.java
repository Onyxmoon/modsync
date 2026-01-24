package de.onyxmoon.modsync.provider.curseforge;

import de.onyxmoon.modsync.api.ModListProvider;
import de.onyxmoon.modsync.api.ModListSource;
import de.onyxmoon.modsync.api.model.provider.ModEntry;
import de.onyxmoon.modsync.api.model.provider.ModList;
import de.onyxmoon.modsync.provider.curseforge.client.CurseForgeClient;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * CurseForge implementation of ModListProvider.
 * Registered via META-INF/services/de.onyxmoon.modsync.api.ModListProvider
 */
public class CurseForgeProvider implements ModListProvider {

    private static final int RATE_LIMIT = 60; // 60 requests per minute
    private final CurseForgeAdapter adapter;

    public CurseForgeProvider() {
        this.adapter = new CurseForgeAdapter();
    }

    @Override
    public ModListSource getSource() {
        return ModListSource.CURSEFORGE;
    }

    @Override
    public CompletableFuture<ModList> fetchModList(String apiKey, String projectId) {
        CurseForgeClient client = new CurseForgeClient(apiKey);

        // First, get the modpack details to get the project name
        return client.getMod(projectId)
                .thenCompose(modResponse -> {
                    String projectName = modResponse.getData().getName();

                    // Then search for mods in the modpack
                    // For now, we search using the project name
                    // A more sophisticated implementation could parse modpack files
                    return client.searchMods(projectName, 50, 0)
                            .thenApply(searchResponse ->
                                    adapter.adaptToModList(searchResponse, projectId, projectName)
                            );
                });
    }

    @Override
    public CompletableFuture<Boolean> validateApiKey(String apiKey) {
        CurseForgeClient client = new CurseForgeClient(apiKey);
        return client.validateKey();
    }

    @Override
    public boolean requiresApiKey() {
        return true;
    }

    @Override
    public String getDisplayName() {
        return "CurseForge";
    }

    @Override
    public int getRateLimit() {
        return RATE_LIMIT;
    }

    @Override
    public CompletableFuture<ModEntry> fetchMod(String apiKey, String modId) {
        CurseForgeClient client = new CurseForgeClient(apiKey);
        return client.getMod(modId)
                .thenApply(response -> adapter.adaptToModEntry(response.getData()));
    }

    @Override
    public CompletableFuture<ModEntry> fetchModBySlug(String apiKey, String slug) {
        CurseForgeClient client = new CurseForgeClient(apiKey);
        return client.getModBySlug(slug)
                .thenApply(response -> adapter.adaptToModEntry(response.getData()));
    }

    @Override
    public CompletableFuture<List<ModEntry>> searchMods(String apiKey, String searchTerm) {
        CurseForgeClient client = new CurseForgeClient(apiKey);
        return client.searchMods(searchTerm, 10, 0)
                .thenApply(response -> {
                    if (response.getData() == null || response.getData().isEmpty()) {
                        return List.of();
                    }
                    return response.getData().stream()
                            .map(adapter::adaptToModEntry)
                            .toList();
                });
    }
}
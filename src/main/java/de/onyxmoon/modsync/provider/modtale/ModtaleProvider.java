package de.onyxmoon.modsync.provider.modtale;

import com.hypixel.hytale.logger.HytaleLogger;
import de.onyxmoon.modsync.ModSync;
import de.onyxmoon.modsync.api.InvalidModUrlException;
import de.onyxmoon.modsync.api.ModProviderWithDownloadHandler;
import de.onyxmoon.modsync.api.ParsedModUrl;
import de.onyxmoon.modsync.api.model.provider.ModEntry;
import de.onyxmoon.modsync.api.model.provider.ModList;
import de.onyxmoon.modsync.provider.modtale.client.ModtaleApiException;
import de.onyxmoon.modsync.provider.modtale.client.ModtaleClient;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Modtale implementation of ModProvider.
 * Implements DownloadHandler for authenticated downloads with X-MODTALE-KEY header.
 * Registered via META-INF/services/de.onyxmoon.modsync.api.ModProvider
 */
public class ModtaleProvider implements ModProviderWithDownloadHandler {

    private static final String SOURCE = "modtale";
    private static final String DISPLAY_NAME = "Modtale";

    private static final int RATE_LIMIT = 300;
    private static final int URL_PRIORITY = 80;

    private final ModtaleAdapter adapter;
    private final ModtaleUrlParser urlParser;
    private final ModtaleDownloader downloader;

    public ModtaleProvider() {
        this.adapter = new ModtaleAdapter();
        this.urlParser = new ModtaleUrlParser(SOURCE);
        this.downloader = new ModtaleDownloader();
    }

    @Override
    public String getSource() {
        return SOURCE;
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
        return DISPLAY_NAME;
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
        return searchForSlug(apiKey, slug)
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
                .filter(entry -> entry.getName().equalsIgnoreCase(slug))
                .findFirst()
                .orElseGet(() -> results.stream()
                        .filter(entry -> entry.getSlug() != null && entry.getSlug().equalsIgnoreCase(normalized))
                        .findFirst()
                        .orElse(results.get(0)));
    }

    private CompletableFuture<List<ModEntry>> searchForSlug(String apiKey, String slug) {
        String normalized = slug == null ? "" : slug.trim();
        if (normalized.isEmpty()) {
            return CompletableFuture.completedFuture(List.of());
        }

        String spaced = normalized.replace('-', ' ').trim();

        return searchMods(apiKey, normalized)
                .thenCompose(results -> {
                    if (!results.isEmpty() || spaced.equalsIgnoreCase(normalized)) {
                        return CompletableFuture.completedFuture(results);
                    }
                    return searchMods(apiKey, spaced);
                });
    }

    // ==================== DownloadHandler Implementation ====================

    @Override
    public CompletableFuture<DownloadResult> download(String downloadUrl, String apiKey, Path targetDir) {
        return downloader.download(downloadUrl, apiKey, targetDir);
    }
}

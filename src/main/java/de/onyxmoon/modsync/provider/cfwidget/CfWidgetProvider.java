package de.onyxmoon.modsync.provider.cfwidget;

import de.onyxmoon.modsync.api.InvalidModUrlException;
import de.onyxmoon.modsync.api.ModProvider;
import de.onyxmoon.modsync.api.ParsedModUrl;
import de.onyxmoon.modsync.api.model.provider.ModEntry;
import de.onyxmoon.modsync.api.model.provider.ModList;
import de.onyxmoon.modsync.provider.cfwidget.client.CfWidgetApiException;
import de.onyxmoon.modsync.provider.cfwidget.client.CfWidgetClient;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * CFWidget implementation of ModProvider.
 * Registered via META-INF/services/de.onyxmoon.modsync.api.ModProvider
 */
public class CfWidgetProvider implements ModProvider {
    private static final String SOURCE = "cfwidget";
    private static final String DISPLAY_NAME = "CFWidget";

    private static final int RATE_LIMIT = 30;
    private static final int URL_PRIORITY = 50;
    private static final String HYTALE_MODS_PATH = "hytale/mods/";
    private static final String HYTALE_BOOTSTRAP_PATH = "hytale/bootstrap/";


    private final CfWidgetClient client;
    private final CfWidgetAdapter adapter;
    private final CfWidgetUrlParser urlParser;

    public CfWidgetProvider() {
        this.client = new CfWidgetClient();
        this.adapter = new CfWidgetAdapter();
        this.urlParser = new CfWidgetUrlParser(SOURCE);
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
    public String getSource() {
        return SOURCE;
    }

    @Override
    public CompletableFuture<ModList> fetchModList(String apiKey, String projectId) {
        return fetchMod(apiKey, projectId)
                .thenApply(entry -> ModList.builder()
                        .source(SOURCE)
                        .projectId(projectId)
                        .projectName(entry.getName())
                        .mods(List.of(entry))
                        .fetchedAt(Instant.now())
                        .build());
    }

    @Override
    public CompletableFuture<Boolean> validateApiKey(String apiKey) {
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public boolean requiresApiKey() {
        return false;
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
    public CompletableFuture<ModEntry> fetchMod(String apiKey, String modId) {
        return client.getProject(modId)
                .thenApply(project -> adapter.adaptToModEntry(project, modId));
    }

    @Override
    public CompletableFuture<ModEntry> fetchModBySlug(String apiKey, String slug) {
        if (slug == null || slug.isBlank()) {
            return CompletableFuture.failedFuture(new CfWidgetApiException("Missing slug", 400));
        }

        String normalized = slug.trim();
        if (normalized.contains("/") || normalized.matches("\\d+")) {
            return client.getProject(normalized)
                    .thenApply(project -> adapter.adaptToModEntry(project, normalized));
        }

        String modsPath = HYTALE_MODS_PATH + normalized;
        return client.getProject(modsPath)
                .thenApply(project -> adapter.adaptToModEntry(project, modsPath))
                .exceptionallyCompose(ex -> {
                    if (ex.getCause() instanceof CfWidgetApiException apiEx && apiEx.getStatusCode() == 404) {
                        String bootstrapPath = HYTALE_BOOTSTRAP_PATH + normalized;
                        return client.getProject(bootstrapPath)
                                .thenApply(project -> adapter.adaptToModEntry(project, bootstrapPath));
                    }
                    return CompletableFuture.failedFuture(ex);
                });
    }

    @Override
    public CompletableFuture<List<ModEntry>> searchMods(String apiKey, String searchTerm) {
        return CompletableFuture.failedFuture(
                new UnsupportedOperationException("CFWidget does not support search")
        );
    }
}

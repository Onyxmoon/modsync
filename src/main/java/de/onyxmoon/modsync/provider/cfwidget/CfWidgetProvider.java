package de.onyxmoon.modsync.provider.cfwidget;

import com.google.gson.JsonObject;
import de.onyxmoon.modsync.api.InvalidModUrlException;
import de.onyxmoon.modsync.api.ModListProvider;
import de.onyxmoon.modsync.api.ModListSource;
import de.onyxmoon.modsync.api.ParsedModUrl;
import de.onyxmoon.modsync.api.model.provider.ModEntry;
import de.onyxmoon.modsync.api.model.provider.ModList;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * CFWidget implementation of ModListProvider.
 * Registered via META-INF/services/de.onyxmoon.modsync.api.ModListProvider
 */
public class CfWidgetProvider implements ModListProvider {

    private static final int RATE_LIMIT = 30;
    private static final int URL_PRIORITY = 50;
    private static final String HYTALE_MODS_PATH = "hytale/mods/";
    private static final String HYTALE_BOOTSTRAP_PATH = "hytale/bootstrap/";
    private static final Pattern CF_WIDGET_HOST = Pattern.compile("^(?:api\\.)?cfwidget\\.com$", Pattern.CASE_INSENSITIVE);
    private static final Pattern CURSEFORGE_URL = Pattern.compile(
            "^https?://(?:www\\.)?curseforge\\.com/hytale/(mods|bootstrap)/([\\w-]+)(?:/files(?:/(\\d+))?)?/?$",
            Pattern.CASE_INSENSITIVE
    );

    private final CfWidgetClient client;
    private final CfWidgetAdapter adapter;

    public CfWidgetProvider() {
        this.client = new CfWidgetClient();
        this.adapter = new CfWidgetAdapter();
    }

    @Override
    public ModListSource getSource() {
        return ModListSource.CFWIDGET;
    }

    @Override
    public CompletableFuture<ModList> fetchModList(String apiKey, String projectId) {
        return fetchMod(apiKey, projectId)
                .thenApply(entry -> ModList.builder()
                        .source(ModListSource.CFWIDGET)
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
        return "CFWidget";
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
        if (url == null || url.isBlank()) {
            return false;
        }
        String trimmed = url.trim();
        if (CURSEFORGE_URL.matcher(trimmed).matches()) {
            return true;
        }
        try {
            URI uri = URI.create(trimmed);
            String host = uri.getHost();
            String path = uri.getPath();
            return host != null && CF_WIDGET_HOST.matcher(host).matches()
                    && path != null && !path.isBlank() && !path.endsWith(".png");
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public ParsedModUrl parse(String url) throws InvalidModUrlException {
        if (url == null || url.isBlank()) {
            throw new InvalidModUrlException(url, "URL cannot be null or empty");
        }
        String trimmed = url.trim();
        Matcher cfMatcher = CURSEFORGE_URL.matcher(trimmed);
        if (cfMatcher.matches()) {
            String category = cfMatcher.group(1);
            String slug = cfMatcher.group(2);
            String versionId = cfMatcher.group(3);
            String path = "hytale/" + category + "/" + slug;
            return new ParsedModUrl(ModListSource.CFWIDGET, path, slug, versionId);
        }
        try {
            URI uri = URI.create(trimmed);
            String host = uri.getHost();
            if (host == null || !CF_WIDGET_HOST.matcher(host).matches()) {
                throw new InvalidModUrlException(url, "URL does not match CFWidget pattern");
            }
            String path = uri.getPath();
            if (path == null || path.isBlank()) {
                throw new InvalidModUrlException(url, "URL does not contain a project path");
            }
            if (path.endsWith(".png")) {
                throw new InvalidModUrlException(url, "Image URLs are not supported");
            }
            String normalized = path.startsWith("/") ? path.substring(1) : path;
            String slug = extractSlug(normalized);
            return new ParsedModUrl(ModListSource.CFWIDGET, normalized, slug, null);
        } catch (InvalidModUrlException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidModUrlException(url, "URL does not match CFWidget pattern");
        }
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

    private static String extractSlug(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        String trimmed = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
        int slashIndex = trimmed.lastIndexOf('/');
        if (slashIndex < 0) {
            return trimmed;
        }
        return trimmed.substring(slashIndex + 1);
    }
}

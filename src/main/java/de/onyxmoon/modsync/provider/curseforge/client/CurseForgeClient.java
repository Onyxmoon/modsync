package de.onyxmoon.modsync.provider.curseforge.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.onyxmoon.modsync.provider.curseforge.model.CurseForgeModResponse;
import de.onyxmoon.modsync.provider.curseforge.model.CurseForgeSearchResponse;
import de.onyxmoon.modsync.storage.InstantTypeAdapter;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

/**
 * HTTP client for CurseForge API.
 */
public class CurseForgeClient {
    private static final String BASE_URL = "https://api.curseforge.com/v1";
    private static final String GAME_ID = "70216";

    private final HttpClient httpClient;
    private final Gson gson;
    private final String apiKey;

    public CurseForgeClient(String apiKey) {
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.gson = new GsonBuilder()
                .registerTypeAdapter(Instant.class, new InstantTypeAdapter())
                .create();
    }

    /**
     * Search for mods.
     *
     * @param searchFilter Search term
     * @param pageSize Results per page (max 50)
     * @param index Pagination index
     * @return CompletableFuture containing search results
     */
    public CompletableFuture<CurseForgeSearchResponse> searchMods(
            String searchFilter, int pageSize, int index) {

        String url = String.format(
                "%s/mods/search?gameId=%s&searchFilter=%s&pageSize=%d&index=%d",
                BASE_URL, GAME_ID, searchFilter, pageSize, index
        );

        return executeRequest(url, CurseForgeSearchResponse.class);
    }

    /**
     * Get mod details by ID.
     *
     * @param modId Mod identifier
     * @return CompletableFuture containing mod details
     */
    public CompletableFuture<CurseForgeModResponse> getMod(String modId) {
        String url = String.format("%s/mods/%s", BASE_URL, modId);
        return executeRequest(url, CurseForgeModResponse.class);
    }

    /**
     * Get mod details by slug (URL-friendly name).
     * Uses the search API with slug filter to find the mod.
     *
     * @param slug Mod slug (e.g., "example-mod")
     * @return CompletableFuture containing mod details
     * @throws CurseForgeApiException if mod not found
     */
    public CompletableFuture<CurseForgeModResponse> getModBySlug(String slug) {
        String encodedSlug = URLEncoder.encode(slug, StandardCharsets.UTF_8);
        String url = String.format(
                "%s/mods/search?gameId=%s&slug=%s&pageSize=1",
                BASE_URL, GAME_ID, encodedSlug
        );

        return executeRequest(url, CurseForgeSearchResponse.class)
                .thenApply(response -> {
                    if (response.getData() == null || response.getData().isEmpty()) {
                        throw new CurseForgeApiException("Mod not found: " + slug, 404);
                    }
                    // Wrap the search result in a ModResponse
                    CurseForgeModResponse modResponse = new CurseForgeModResponse();
                    modResponse.setData(response.getData().get(0));
                    return modResponse;
                });
    }

    /**
     * Validate API key by making a simple request.
     *
     * @return CompletableFuture containing true if valid, false otherwise
     */
    public CompletableFuture<Boolean> validateKey() {
        String url = String.format("%s/games", BASE_URL);

        return executeRequest(url, Object.class)
                .thenApply(response -> true)
                .exceptionally(ex -> false);
    }

    private <T> CompletableFuture<T> executeRequest(String url, Class<T> responseType) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("x-api-key", apiKey)
                .header("Accept", "application/json")
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        return gson.fromJson(response.body(), responseType);
                    } else if (response.statusCode() == 401 || response.statusCode() == 403) {
                        throw new CurseForgeApiException("Invalid API key", response.statusCode());
                    } else if (response.statusCode() == 429) {
                        throw new CurseForgeApiException("Rate limit exceeded", response.statusCode());
                    } else {
                        throw new CurseForgeApiException(
                                "API request failed: " + response.statusCode(),
                                response.statusCode()
                        );
                    }
                });
    }
}
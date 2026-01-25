package de.onyxmoon.modsync.provider.modtale;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.onyxmoon.modsync.provider.modtale.model.ModtaleProjectResponse;
import de.onyxmoon.modsync.provider.modtale.model.ModtaleSearchResponse;
import de.onyxmoon.modsync.storage.InstantTypeAdapter;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * HTTP client for Modtale API.
 */
public class ModtaleClient {
    private static final String BASE_URL = "https://api.modtale.net";
    private static final String API_PATH = "/api/v1";

    /**
     * Shared HttpClient instance for all ModtaleClient instances.
     */
    private static final HttpClient SHARED_HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private static final Gson SHARED_GSON = new GsonBuilder()
            .registerTypeAdapter(Instant.class, new InstantTypeAdapter())
            .create();

    private final String apiKey;

    public ModtaleClient(String apiKey) {
        this.apiKey = Objects.requireNonNull(apiKey, "apiKey cannot be null");
    }

    public CompletableFuture<ModtaleProjectResponse> getProject(String projectId) {
        String url = BASE_URL + API_PATH + "/projects/" + projectId;
        return executeRequest(url, ModtaleProjectResponse.class);
    }

    public CompletableFuture<ModtaleSearchResponse> searchProjects(String searchTerm, int size, int page) {
        String encoded = URLEncoder.encode(searchTerm, StandardCharsets.UTF_8);
        String url = String.format("%s%s/projects?search=%s&size=%d&page=%d&sort=relevance",
                BASE_URL, API_PATH, encoded, size, page);
        return executeRequest(url, ModtaleSearchResponse.class);
    }

    public CompletableFuture<Boolean> validateKey() {
        String url = BASE_URL + API_PATH + "/user/me";
        return executeRequest(url, Object.class)
                .thenApply(response -> true)
                .exceptionally(ex -> false);
    }

    private <T> CompletableFuture<T> executeRequest(String url, Class<T> responseType) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("X-MODTALE-KEY", apiKey)
                .header("Accept", "application/json")
                .GET()
                .build();

        return SHARED_HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    int status = response.statusCode();
                    if (status == 200) {
                        return SHARED_GSON.fromJson(response.body(), responseType);
                    } else if (status == 401 || status == 403) {
                        throw new ModtaleApiException("Invalid API key", status);
                    } else if (status == 429) {
                        throw new ModtaleApiException("Rate limit exceeded", status);
                    } else {
                        throw new ModtaleApiException("API request failed: " + status, status);
                    }
                });
    }
}

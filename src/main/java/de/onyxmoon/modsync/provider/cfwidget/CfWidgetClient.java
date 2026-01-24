package de.onyxmoon.modsync.provider.cfwidget;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.concurrent.CompletableFuture;

/**
 * HTTP client for CFWidget API.
 */
public class CfWidgetClient {
    private static final String BASE_URL = "https://api.cfwidget.com";

    private final HttpClient httpClient;
    private final Gson gson;

    public CfWidgetClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.gson = new GsonBuilder().create();
    }

    public CompletableFuture<JsonObject> getProject(String pathOrId) {
        String encodedPath = encodePath(pathOrId);
        String url = BASE_URL + "/" + encodedPath;
        return executeRequest(url);
    }

    private CompletableFuture<JsonObject> executeRequest(String url) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    int status = response.statusCode();
                    if (status == 200) {
                        return gson.fromJson(response.body(), JsonObject.class);
                    }
                    throw new CfWidgetApiException(
                            "CFWidget request failed: " + status,
                            status
                    );
                });
    }

    private static String encodePath(String pathOrId) {
        if (pathOrId == null) {
            return "";
        }
        String trimmed = pathOrId.trim();
        if (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1);
        }
        if (trimmed.isEmpty()) {
            return trimmed;
        }
        return Arrays.stream(trimmed.split("/"))
                .map(part -> URLEncoder.encode(part, StandardCharsets.UTF_8).replace("+", "%20"))
                .collect(Collectors.joining("/"));
    }
}

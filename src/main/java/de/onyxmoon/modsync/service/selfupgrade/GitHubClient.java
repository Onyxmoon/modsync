package de.onyxmoon.modsync.service.selfupgrade;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hypixel.hytale.logger.HytaleLogger;
import de.onyxmoon.modsync.ModSync;
import de.onyxmoon.modsync.service.selfupgrade.model.GitHubApiException;
import de.onyxmoon.modsync.service.selfupgrade.model.GitHubRelease;
import de.onyxmoon.modsync.storage.InstantTypeAdapter;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

/**
 * HTTP client for GitHub Releases API.
 * Handles rate limiting and caching to minimize API calls.
 */
public class GitHubClient {
    private static final HytaleLogger LOGGER = HytaleLogger.get(ModSync.LOG_NAME);
    private static final String GITHUB_API_BASE = "https://api.github.com";
    private static final String REPO_OWNER = "Onyxmoon";
    private static final String REPO_NAME = "modsync";

    private final HttpClient httpClient;
    private final Gson gson;

    // Simple cache to avoid hitting rate limits
    private GitHubRelease cachedRelease;
    private Instant cacheExpiry;
    private static final Duration CACHE_DURATION = Duration.ofMinutes(15);

    public GitHubClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        this.gson = new GsonBuilder()
                .registerTypeAdapter(Instant.class, new InstantTypeAdapter())
                .create();
    }

    /**
     * Fetches the latest release from GitHub.
     * Results are cached for 15 minutes to respect rate limits (60/hour unauthenticated).
     *
     * @return CompletableFuture containing the latest release
     */
    public CompletableFuture<GitHubRelease> getLatestRelease() {
        // Return cached result if still valid
        if (cachedRelease != null && cacheExpiry != null && Instant.now().isBefore(cacheExpiry)) {
            LOGGER.atFine().log("Using cached GitHub release info");
            return CompletableFuture.completedFuture(cachedRelease);
        }

        String url = String.format("%s/repos/%s/%s/releases/latest",
                GITHUB_API_BASE, REPO_OWNER, REPO_NAME);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "ModSync-Plugin")
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        GitHubRelease release = gson.fromJson(response.body(), GitHubRelease.class);
                        // Cache the result
                        cachedRelease = release;
                        cacheExpiry = Instant.now().plus(CACHE_DURATION);
                        return release;
                    } else if (response.statusCode() == 403) {
                        // Check for rate limit
                        String remaining = response.headers().firstValue("X-RateLimit-Remaining").orElse("?");
                        throw new GitHubApiException("Rate limited. Remaining: " + remaining, 403);
                    } else if (response.statusCode() == 404) {
                        throw new GitHubApiException("No releases found", 404);
                    } else {
                        throw new GitHubApiException("GitHub API error: " + response.statusCode(),
                                response.statusCode());
                    }
                });
    }

    /**
     * Clears the cache, forcing the next request to hit the API.
     */
    public void clearCache() {
        cachedRelease = null;
        cacheExpiry = null;
    }
}
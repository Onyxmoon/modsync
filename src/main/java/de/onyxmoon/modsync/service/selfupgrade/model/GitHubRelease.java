package de.onyxmoon.modsync.service.selfupgrade.model;

import com.google.gson.annotations.SerializedName;

import java.time.Instant;
import java.util.List;

/**
 * Model for GitHub Release API response.
 */
public class GitHubRelease {
    @SerializedName("tag_name")
    private String tagName;

    private String name;
    private String body;
    private boolean prerelease;
    private boolean draft;

    @SerializedName("published_at")
    private Instant publishedAt;

    @SerializedName("html_url")
    private String htmlUrl;

    private List<GitHubAsset> assets;

    public String getTagName() {
        return tagName;
    }

    public String getName() {
        return name;
    }

    public String getBody() {
        return body;
    }

    public boolean isPrerelease() {
        return prerelease;
    }

    public boolean isDraft() {
        return draft;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public String getHtmlUrl() {
        return htmlUrl;
    }

    public List<GitHubAsset> getAssets() {
        return assets;
    }

    /**
     * Extracts version string from tag name (removes 'v' prefix if present).
     *
     * @return the version string without 'v' prefix
     */
    public String getVersion() {
        if (tagName == null) return null;
        return tagName.startsWith("v") ? tagName.substring(1) : tagName;
    }

    /**
     * Finds the main plugin JAR asset.
     *
     * @return the main JAR asset, or null if not found
     */
    public GitHubAsset findMainJar() {
        if (assets == null) return null;
        return assets.stream()
                .filter(a -> a.getName().startsWith("modsync-") &&
                        !a.getName().contains("bootstrap") &&
                        a.getName().endsWith(".jar") &&
                        !a.getName().contains("sources") &&
                        !a.getName().contains("javadoc"))
                .findFirst()
                .orElse(null);
    }

    /**
     * Finds the bootstrap JAR asset.
     *
     * @return the bootstrap JAR asset, or null if not found
     */
    public GitHubAsset findBootstrapJar() {
        if (assets == null) return null;
        return assets.stream()
                .filter(a -> a.getName().contains("bootstrap") &&
                        a.getName().endsWith(".jar") &&
                        !a.getName().contains("sources") &&
                        !a.getName().contains("javadoc"))
                .findFirst()
                .orElse(null);
    }
}
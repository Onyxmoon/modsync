package de.onyxmoon.modsync.service.selfupgrade.model;

import com.google.gson.annotations.SerializedName;

/**
 * Model for a GitHub Release asset.
 */
public class GitHubAsset {
    private String name;
    private long size;

    @SerializedName("browser_download_url")
    private String browserDownloadUrl;

    @SerializedName("content_type")
    private String contentType;

    public String getName() {
        return name;
    }

    public long getSize() {
        return size;
    }

    public String getBrowserDownloadUrl() {
        return browserDownloadUrl;
    }

    public String getContentType() {
        return contentType;
    }
}
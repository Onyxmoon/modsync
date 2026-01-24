package de.onyxmoon.modsync.provider.cfwidget;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.onyxmoon.modsync.api.PluginType;
import de.onyxmoon.modsync.api.model.provider.ModEntry;
import de.onyxmoon.modsync.api.model.provider.ModVersion;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for converting CFWidget JSON responses into internal models.
 */
public class CfWidgetAdapter {

    public ModEntry adaptToModEntry(JsonObject project, String requestedPath) {
        String modId = stringValue(project, "id");
        if (modId == null || modId.isBlank()) {
            modId = requestedPath != null ? requestedPath : "unknown";
        }

        String name = firstNonBlank(
                stringValue(project, "name"),
                stringValue(project, "title"),
                stringValue(project, "slug"),
                modId
        );

        String websiteUrl = extractWebsiteUrl(project);
        String slug = extractSlug(websiteUrl);
        String summary = firstNonBlank(
                stringValue(project, "summary"),
                stringValue(project, "description")
        );

        List<String> categories = extractCategories(project);
        PluginType pluginType = extractPluginType(websiteUrl);
        int downloadCount = extractDownloadCount(project);
        String logoUrl = stringValue(project, "thumbnail");
        Instant lastUpdated = parseInstant(firstNonBlank(
                stringValue(project, "updated_at"),
                stringValue(project, "last_updated"),
                stringValue(project, "created_at")
        ));

        ModVersion latest = adaptDownload(project);
        List<ModVersion> versions = adaptFiles(project, latest);

        return ModEntry.builder()
                .modId(modId)
                .name(name)
                .slug(slug)
                .summary(summary)
                .latestVersion(latest)
                .availableVersions(versions)
                .categories(categories)
                .pluginType(pluginType)
                .downloadCount(downloadCount)
                .websiteUrl(websiteUrl)
                .logoUrl(logoUrl)
                .lastUpdated(lastUpdated)
                .build();
    }

    private ModVersion adaptDownload(JsonObject project) {
        JsonObject download = objectValue(project, "download");
        if (download == null) {
            return null;
        }
        return adaptVersion(download, "latest");
    }

    private List<ModVersion> adaptFiles(JsonObject project, ModVersion latest) {
        JsonArray files = arrayValue(project, "files");
        if (files == null || files.isEmpty()) {
            return latest != null ? List.of(latest) : List.of();
        }

        List<ModVersion> versions = new ArrayList<>();
        for (JsonElement element : files) {
            if (!element.isJsonObject()) {
                continue;
            }
            ModVersion version = adaptVersion(element.getAsJsonObject(), "file");
            if (version != null) {
                versions.add(version);
            }
        }
        if (versions.isEmpty() && latest != null) {
            versions.add(latest);
        }
        return versions;
    }

    private ModVersion adaptVersion(JsonObject file, String fallbackIdPrefix) {
        String versionId = firstNonBlank(
                stringValue(file, "id"),
                stringValue(file, "fileId"),
                stringValue(file, "file_id")
        );
        String versionNumber = firstNonBlank(
                stringValue(file, "version"),
                stringValue(file, "display"),
                stringValue(file, "name"),
                stringValue(file, "fileName"),
                stringValue(file, "filename")
        );
        String fileName = firstNonBlank(
                stringValue(file, "fileName"),
                stringValue(file, "filename"),
                stringValue(file, "display"),
                stringValue(file, "name"),
                versionNumber
        );

        if (versionId == null || versionId.isBlank()) {
            versionId = fallbackIdPrefix + "-" + (versionNumber != null ? versionNumber : "latest");
        }
        if (versionNumber == null || versionNumber.isBlank()) {
            versionNumber = fileName != null ? fileName : "latest";
        }

        String downloadUrl = firstNonBlank(
                stringValue(file, "url"),
                stringValue(file, "downloadUrl")
        );
        long fileSize = longValue(file, "size");
        String releaseType = stringValue(file, "type");
        Instant uploadedAt = parseInstant(firstNonBlank(
                stringValue(file, "uploaded_at"),
                stringValue(file, "uploadedAt"),
                stringValue(file, "date")
        ));
        List<String> versions = extractStringArray(file, "versions");

        return ModVersion.builder()
                .versionId(versionId)
                .versionNumber(versionNumber)
                .fileName(fileName != null ? fileName : versionNumber)
                .fileSize(fileSize)
                .downloadUrl(downloadUrl)
                .gameVersions(versions)
                .releaseType(releaseType)
                .uploadedAt(uploadedAt)
                .build();
    }

    private static String extractWebsiteUrl(JsonObject project) {
        JsonObject urls = objectValue(project, "urls");
        if (urls == null) {
            return null;
        }
        return firstNonBlank(
                stringValue(urls, "curseforge"),
                stringValue(urls, "curse"),
                stringValue(urls, "website")
        );
    }

    private static List<String> extractCategories(JsonObject project) {
        JsonArray categories = arrayValue(project, "categories");
        if (categories == null || categories.isEmpty()) {
            return List.of();
        }
        List<String> names = new ArrayList<>();
        for (JsonElement element : categories) {
            if (element.isJsonPrimitive()) {
                names.add(element.getAsString());
            } else if (element.isJsonObject()) {
                String name = stringValue(element.getAsJsonObject(), "name");
                if (name != null && !name.isBlank()) {
                    names.add(name);
                }
            }
        }
        return names;
    }

    private static int extractDownloadCount(JsonObject project) {
        JsonObject downloads = objectValue(project, "downloads");
        if (downloads != null) {
            Integer total = intValue(downloads, "total");
            if (total != null) {
                return total;
            }
        }
        Integer count = intValue(project, "downloads");
        return count != null ? count : 0;
    }

    private static PluginType extractPluginType(String websiteUrl) {
        if (websiteUrl != null && websiteUrl.contains("/bootstrap/")) {
            return PluginType.EARLY_PLUGIN;
        }
        return PluginType.PLUGIN;
    }

    private static String extractSlug(String websiteUrl) {
        if (websiteUrl == null || websiteUrl.isBlank()) {
            return null;
        }
        String trimmed = websiteUrl;
        int queryIndex = trimmed.indexOf('?');
        if (queryIndex >= 0) {
            trimmed = trimmed.substring(0, queryIndex);
        }
        if (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        int slashIndex = trimmed.lastIndexOf('/');
        if (slashIndex < 0 || slashIndex == trimmed.length() - 1) {
            return null;
        }
        return trimmed.substring(slashIndex + 1);
    }

    private static List<String> extractStringArray(JsonObject obj, String key) {
        JsonArray array = arrayValue(obj, key);
        if (array == null || array.isEmpty()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (JsonElement element : array) {
            if (element.isJsonPrimitive()) {
                values.add(element.getAsString());
            }
        }
        return values;
    }

    private static String stringValue(JsonObject obj, String key) {
        if (obj == null || key == null || !obj.has(key)) {
            return null;
        }
        JsonElement element = obj.get(key);
        if (element == null || element.isJsonNull()) {
            return null;
        }
        if (element.isJsonPrimitive()) {
            if (element.getAsJsonPrimitive().isNumber()) {
                return element.getAsNumber().toString();
            }
            return element.getAsString();
        }
        return null;
    }

    private static JsonObject objectValue(JsonObject obj, String key) {
        if (obj == null || key == null || !obj.has(key)) {
            return null;
        }
        JsonElement element = obj.get(key);
        if (element != null && element.isJsonObject()) {
            return element.getAsJsonObject();
        }
        return null;
    }

    private static JsonArray arrayValue(JsonObject obj, String key) {
        if (obj == null || key == null || !obj.has(key)) {
            return null;
        }
        JsonElement element = obj.get(key);
        if (element != null && element.isJsonArray()) {
            return element.getAsJsonArray();
        }
        return null;
    }

    private static Integer intValue(JsonObject obj, String key) {
        String value = stringValue(obj, key);
        if (value == null) {
            JsonElement element = obj != null ? obj.get(key) : null;
            if (element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
                return element.getAsInt();
            }
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static long longValue(JsonObject obj, String key) {
        JsonElement element = obj != null ? obj.get(key) : null;
        if (element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
            return element.getAsLong();
        }
        String value = stringValue(obj, key);
        if (value == null) {
            return 0L;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static Instant parseInstant(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (Exception ignored) {
            return null;
        }
    }
}

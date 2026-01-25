package de.onyxmoon.modsync.provider.modtale;

import de.onyxmoon.modsync.api.PluginType;
import de.onyxmoon.modsync.api.model.provider.ModAuthor;
import de.onyxmoon.modsync.api.model.provider.ModEntry;
import de.onyxmoon.modsync.api.model.provider.ModList;
import de.onyxmoon.modsync.api.model.provider.ModVersion;
import de.onyxmoon.modsync.provider.modtale.model.ModtaleProjectResponse;
import de.onyxmoon.modsync.provider.modtale.model.ModtaleSearchResponse;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;

/**
 * Adapter to convert Modtale API responses to canonical models.
 */
public class ModtaleAdapter {
    private static final String SOURCE = "modtale";
    private static final String BASE_URL = "https://api.modtale.net";

    public ModList adaptToModList(ModtaleProjectResponse project) {
        ModEntry entry = adaptToModEntry(project);
        return ModList.builder()
                .source(SOURCE)
                .projectId(project.getId())
                .projectName(project.getTitle())
                .mods(List.of(entry))
                .fetchedAt(Instant.now())
                .build();
    }

    public ModEntry adaptToModEntry(ModtaleProjectResponse project) {
        List<ModVersion> versions = adaptVersions(project.getVersions(), project.getId());
        ModVersion latest = versions.isEmpty() ? null : versions.get(0);

        return ModEntry.builder()
                .modId(project.getId())
                .name(project.getTitle())
                .slug(slugify(project.getTitle()))
                .summary(project.getDescription())
                .authors(project.getAuthor() != null ? List.of(new ModAuthor(project.getAuthor(), null)) : List.of())
                .latestVersion(latest)
                .availableVersions(versions)
                .categories(project.getTags())
                .pluginType(adaptPluginType(project.getClassification()))
                .downloadCount(project.getDownloadCount())
                .websiteUrl(project.getWebsiteUrl())
                .logoUrl(project.getImageUrl())
                .lastUpdated(parseUpdatedAt(project.getUpdatedAt()))
                .build();
    }

    public List<ModEntry> adaptSearch(ModtaleSearchResponse response) {
        if (response.getContent() == null || response.getContent().isEmpty()) {
            return List.of();
        }
        return response.getContent().stream()
                .map(this::adaptToModEntry)
                .toList();
    }

    public ModEntry adaptToModEntry(ModtaleSearchResponse.ProjectSummary summary) {
        return ModEntry.builder()
                .modId(summary.getId())
                .name(summary.getTitle())
                .slug(slugify(summary.getTitle()))
                .summary(summary.getDescription())
                .authors(summary.getAuthor() != null ? List.of(new ModAuthor(summary.getAuthor(), null)) : List.of())
                .latestVersion(null)
                .availableVersions(List.of())
                .categories(summary.getTags())
                .pluginType(adaptPluginType(summary.getClassification()))
                .downloadCount(summary.getDownloads())
                .websiteUrl(summary.getWebsiteUrl())
                .logoUrl(summary.getImageUrl())
                .lastUpdated(parseUpdatedAt(summary.getUpdatedAt()))
                .build();
    }

    private List<ModVersion> adaptVersions(List<ModtaleProjectResponse.ProjectVersion> versions, String projectId) {
        if (versions == null || versions.isEmpty()) {
            return List.of();
        }
        return versions.stream()
                .map(version -> adaptVersion(version, projectId))
                .sorted(Comparator.comparing(ModVersion::getUploadedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private ModVersion adaptVersion(ModtaleProjectResponse.ProjectVersion version, String projectId) {
        String downloadUrl = resolveDownloadUrl(version.getFileUrl(), projectId, version.getId(), version.getVersionNumber());
        String fileName = extractFileName(downloadUrl, version.getVersionNumber(), projectId);

        return ModVersion.builder()
                .versionId(version.getId())
                .versionNumber(version.getVersionNumber())
                .fileName(fileName)
                .fileSize(version.getFileSize())
                .downloadUrl(downloadUrl)
                .gameVersions(version.getGameVersions())
                .releaseType(normalizeReleaseType(version.getChannel()))
                .uploadedAt(parseUpdatedAt(version.getUploadedAt()))
                .changelog(version.getChangelog())
                .build();
    }

    private String resolveDownloadUrl(String fileUrl, String projectId, String versionId, String versionNumber) {
        if (fileUrl != null && !fileUrl.isBlank()
                && (fileUrl.startsWith("http://") || fileUrl.startsWith("https://"))) {
            return fileUrl;
        }
        if (projectId == null || projectId.isBlank()) {
            return null;
        }
        String versionPath = versionNumber != null && !versionNumber.isBlank()
                ? versionNumber
                : (versionId != null && !versionId.isBlank() ? versionId : "latest");
        return BASE_URL + "/api/v1/projects/" + projectId + "/versions/" + versionPath + "/download";
    }

    private String extractFileName(String downloadUrl, String versionNumber, String projectId) {
        if (downloadUrl == null || downloadUrl.isBlank()) {
            return buildFallbackFileName(projectId, versionNumber);
        }
        try {
            URI uri = URI.create(downloadUrl);
            String path = uri.getPath();
            if (path == null || path.isBlank()) {
                return buildFallbackFileName(projectId, versionNumber);
            }
            int slashIndex = path.lastIndexOf('/');
            String name = slashIndex >= 0 ? path.substring(slashIndex + 1) : path;
            if (name.isBlank()) {
                return buildFallbackFileName(projectId, versionNumber);
            }
            if ("download".equalsIgnoreCase(name) || !name.contains(".")) {
                return buildFallbackFileName(projectId, versionNumber);
            }
            return name;
        } catch (Exception e) {
            return buildFallbackFileName(projectId, versionNumber);
        }
    }

    private String buildFallbackFileName(String projectId, String versionNumber) {
        String base = projectId != null && !projectId.isBlank() ? projectId : "modtale";
        String safeBase = base.replaceAll("[^a-zA-Z0-9_-]", "-");
        String safeVersion = versionNumber != null && !versionNumber.isBlank()
                ? versionNumber.replaceAll("[^a-zA-Z0-9._-]", "-")
                : "latest";
        return safeBase + "-" + safeVersion + ".jar";
    }

    private PluginType adaptPluginType(String classification) {
        if (classification == null) {
            return PluginType.PLUGIN;
        }
        return switch (classification.toUpperCase()) {
            case "PLUGIN" -> PluginType.PLUGIN;
            default -> PluginType.PLUGIN;
        };
    }

    private Instant parseUpdatedAt(String updatedAt) {
        if (updatedAt == null || updatedAt.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(updatedAt);
        } catch (DateTimeParseException ignored) {
            try {
                return LocalDate.parse(updatedAt).atStartOfDay().toInstant(ZoneOffset.UTC);
            } catch (DateTimeParseException ignored2) {
                return null;
            }
        }
    }

    private String normalizeReleaseType(String channel) {
        if (channel == null) {
            return null;
        }
        return channel.toLowerCase();
    }

    private String slugify(String title) {
        if (title == null) {
            return null;
        }
        String trimmed = title.trim().toLowerCase();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
    }
}

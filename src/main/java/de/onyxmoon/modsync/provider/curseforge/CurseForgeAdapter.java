package de.onyxmoon.modsync.provider.curseforge;

import de.onyxmoon.modsync.api.ModListSource;
import de.onyxmoon.modsync.api.PluginType;
import de.onyxmoon.modsync.api.model.provider.ModAuthor;
import de.onyxmoon.modsync.api.model.provider.ModEntry;
import de.onyxmoon.modsync.api.model.provider.ModList;
import de.onyxmoon.modsync.api.model.provider.ModVersion;
import de.onyxmoon.modsync.provider.curseforge.model.CurseForgeModResponse;
import de.onyxmoon.modsync.provider.curseforge.model.CurseForgeSearchResponse;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Adapter to convert CurseForge API responses to canonical models.
 */
public class CurseForgeAdapter {

    /**
     * CurseForge class ID for Bootstrap plugins (Early Plugins).
     */
    private static final int BOOTSTRAP_CLASS_ID = 9281;

    public ModList adaptToModList(
            CurseForgeSearchResponse searchResponse,
            String projectId,
            String projectName) {

        List<ModEntry> mods = searchResponse.getData().stream()
                .map(this::adaptToModEntry)
                .collect(Collectors.toList());

        return ModList.builder()
                .source(ModListSource.CURSEFORGE)
                .projectId(projectId)
                .projectName(projectName != null ? projectName : projectId)
                .mods(mods)
                .fetchedAt(Instant.now())
                .build();
    }

    public ModEntry adaptToModEntry(CurseForgeModResponse.ModData cfMod) {
        List<ModVersion> allVersions = adaptAllFiles(cfMod.getLatestFiles(), cfMod.getId());
        ModVersion latestVersion = allVersions.isEmpty() ? null : allVersions.get(0);

        return ModEntry.builder()
                .modId(String.valueOf(cfMod.getId()))
                .name(cfMod.getName())
                .slug(cfMod.getSlug())
                .summary(cfMod.getSummary())
                .authors(adaptAuthors(cfMod.getAuthors()))
                .latestVersion(latestVersion)
                .availableVersions(allVersions)
                .categories(adaptCategories(cfMod.getCategories()))
                .pluginType(adaptPluginType(cfMod.getClassId()))
                .downloadCount(cfMod.getDownloadCount())
                .websiteUrl(cfMod.getLinks() != null ? cfMod.getLinks().getWebsiteUrl() : null)
                .logoUrl(cfMod.getLogo() != null ? cfMod.getLogo().getUrl() : null)
                .lastUpdated(cfMod.getDateModified())
                .build();
    }

    /**
     * Adapts all available files from CurseForge to ModVersion objects.
     * Files are returned in the same order as from CurseForge (typically newest first).
     *
     * @param files the list of file data from CurseForge
     * @param modId the mod ID (used for download URL fallback)
     * @return list of ModVersion objects
     */
    private List<ModVersion> adaptAllFiles(List<CurseForgeModResponse.FileData> files, int modId) {
        if (files == null || files.isEmpty()) {
            return List.of();
        }

        return files.stream()
                .map(file -> adaptSingleFile(file, modId))
                .collect(Collectors.toList());
    }

    /**
     * Adapts a single CurseForge file to a ModVersion.
     */
    private ModVersion adaptSingleFile(CurseForgeModResponse.FileData file, int modId) {
        String downloadUrl = file.getDownloadUrl();
        if (downloadUrl == null || downloadUrl.isEmpty()) {
            // Fallback
            downloadUrl = String.format(
                    "https://www.curseforge.com/api/v1/mods/%d/files/%d/download",
                    modId, file.getId()
            );
        }

        return ModVersion.builder()
                .versionId(String.valueOf(file.getId()))
                .versionNumber(file.getDisplayName() != null ? file.getDisplayName() : file.getFileName())
                .fileName(file.getFileName())
                .fileSize(file.getFileLength())
                .downloadUrl(downloadUrl)
                .gameVersions(file.getGameVersions() != null ? file.getGameVersions() : List.of())
                .releaseType(convertReleaseType(file.getReleaseType()))
                .uploadedAt(file.getFileDate())
                .build();
    }

    /**
     * Converts CurseForge numeric release type to string.
     * CurseForge uses: 1=Release, 2=Beta, 3=Alpha
     */
    private String convertReleaseType(int releaseType) {
        return switch (releaseType) {
            case 1 -> "release";
            case 2 -> "beta";
            case 3 -> "alpha";
            default -> "release"; // Safe default
        };
    }

    private List<ModAuthor> adaptAuthors(List<CurseForgeModResponse.AuthorData> cfAuthors) {
        if (cfAuthors == null) {
            return List.of();
        }

        return cfAuthors.stream()
                .map(author -> new ModAuthor(author.getName(), author.getUrl()))
                .collect(Collectors.toList());
    }

    private List<String> adaptCategories(List<CurseForgeModResponse.CategoryData> cfCategories) {
        if (cfCategories == null) {
            return List.of();
        }

        return cfCategories.stream()
                .map(CurseForgeModResponse.CategoryData::getName)
                .collect(Collectors.toList());
    }

    private PluginType adaptPluginType(Integer classId) {
        if (classId != null && classId == BOOTSTRAP_CLASS_ID) {
            return PluginType.EARLY_PLUGIN;
        }
        return PluginType.PLUGIN;
    }
}
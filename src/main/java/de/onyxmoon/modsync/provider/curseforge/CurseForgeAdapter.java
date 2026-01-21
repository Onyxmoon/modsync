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
        return ModEntry.builder()
                .modId(String.valueOf(cfMod.getId()))
                .name(cfMod.getName())
                .slug(cfMod.getSlug())
                .summary(cfMod.getSummary())
                .authors(adaptAuthors(cfMod.getAuthors()))
                .latestVersion(adaptLatestFile(cfMod.getLatestFiles()))
                .categories(adaptCategories(cfMod.getCategories()))
                .pluginType(adaptPluginType(cfMod.getClassId()))
                .downloadCount(cfMod.getDownloadCount())
                .websiteUrl(cfMod.getLinks() != null ? cfMod.getLinks().getWebsiteUrl() : null)
                .logoUrl(cfMod.getLogo() != null ? cfMod.getLogo().getUrl() : null)
                .lastUpdated(cfMod.getDateModified())
                .build();
    }

    private ModVersion adaptLatestFile(List<CurseForgeModResponse.FileData> files) {
        if (files == null || files.isEmpty()) {
            return null;
        }

        CurseForgeModResponse.FileData latest = files.get(0);

        return ModVersion.builder()
                .versionId(String.valueOf(latest.getId()))
                .versionNumber(latest.getDisplayName() != null ? latest.getDisplayName() : latest.getFileName())
                .fileName(latest.getFileName())
                .fileSize(latest.getFileLength())
                .downloadUrl(latest.getDownloadUrl())
                .gameVersions(latest.getGameVersions() != null ? latest.getGameVersions() : List.of())
                .releaseType(latest.getReleaseType())
                .uploadedAt(latest.getFileDate())
                .build();
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
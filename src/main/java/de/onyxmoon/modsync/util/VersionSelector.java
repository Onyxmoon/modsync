package de.onyxmoon.modsync.util;

import de.onyxmoon.modsync.api.ReleaseChannel;
import de.onyxmoon.modsync.api.model.ManagedMod;
import de.onyxmoon.modsync.api.model.provider.ModEntry;
import de.onyxmoon.modsync.api.model.provider.ModVersion;
import de.onyxmoon.modsync.storage.model.PluginConfig;

import java.util.Comparator;
import java.util.List;

/**
 * Utility class for selecting the appropriate mod version based on release channel preferences.
 */
public final class VersionSelector {

    private VersionSelector() {
        // Utility class
    }

    /**
     * Selects the appropriate version for a mod based on:
     * <ol>
     *   <li>Per-mod release channel override (if set)</li>
     *   <li>Global default release channel from config</li>
     *   <li>Pinned version ID (if mod has desiredVersionId set)</li>
     * </ol>
     *
     * @param mod    the managed mod with potential release channel override
     * @param entry  the mod entry containing available versions
     * @param config the plugin configuration with global default channel
     * @return the selected version, or null if no suitable version found
     */
    public static ModVersion selectVersion(ManagedMod mod, ModEntry entry, PluginConfig config) {
        // If pinned to a specific version, try to find it
        if (!mod.wantsLatestVersion()) {
            ModVersion pinned = findByVersionId(entry.getAvailableVersions(), mod.getDesiredVersionId());
            if (pinned != null) {
                return pinned;
            }
            // Fall through to channel-based selection if pinned version not found
        }

        // Determine effective release channel
        ReleaseChannel channel = getEffectiveChannel(mod, config);

        // Select best version matching the channel
        return selectBestVersion(entry.getAvailableVersions(), channel);
    }

    /**
     * Gets the effective release channel for a mod.
     * Returns the per-mod override if set, otherwise the global default.
     *
     * @param mod    the managed mod
     * @param config the plugin configuration
     * @return the effective release channel
     */
    public static ReleaseChannel getEffectiveChannel(ManagedMod mod, PluginConfig config) {
        ReleaseChannel override = mod.getReleaseChannelOverride();
        return override != null ? override : config.getDefaultReleaseChannel();
    }

    /**
     * Selects the best version from a list that matches the release channel.
     * <p>
     * Filters versions by the channel's allowed release types, then returns
     * the newest one by upload date.
     * </p>
     *
     * @param versions the list of available versions
     * @param channel  the release channel preference
     * @return the best matching version, or null if no version matches the channel
     */
    public static ModVersion selectBestVersion(List<ModVersion> versions, ReleaseChannel channel) {
        if (versions == null || versions.isEmpty()) {
            return null;
        }

        // Filter by channel, then find the newest by upload date
        return versions.stream()
                .filter(v -> channel.allows(v.getReleaseType()))
                .max(Comparator.comparing(ModVersion::getUploadedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(null);
    }

    /**
     * Finds a version by its version ID.
     *
     * @param versions  the list of available versions
     * @param versionId the version ID to find
     * @return the matching version, or null if not found
     */
    public static ModVersion findByVersionId(List<ModVersion> versions, String versionId) {
        if (versions == null || versionId == null) {
            return null;
        }

        return versions.stream()
                .filter(v -> versionId.equals(v.getVersionId()))
                .findFirst()
                .orElse(null);
    }
}
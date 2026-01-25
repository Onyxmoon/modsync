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
     * Result of a version selection operation.
     *
     * @param version          the selected version (null if none found)
     * @param requestedChannel the originally requested channel
     * @param actualChannel    the channel that was used (may differ if fallback occurred)
     * @param usedFallback     true if a less strict channel was used as fallback
     */
    public record SelectionResult(
            ModVersion version,
            ReleaseChannel requestedChannel,
            ReleaseChannel actualChannel,
            boolean usedFallback
    ) {
        /**
         * Creates a result for a successful selection without fallback.
         */
        public static SelectionResult of(ModVersion version, ReleaseChannel channel) {
            return new SelectionResult(version, channel, channel, false);
        }

        /**
         * Creates a result for a successful selection with fallback.
         */
        public static SelectionResult withFallback(ModVersion version, ReleaseChannel requested, ReleaseChannel actual) {
            return new SelectionResult(version, requested, actual, true);
        }

        /**
         * Creates a result when no version was found.
         */
        public static SelectionResult notFound(ReleaseChannel channel) {
            return new SelectionResult(null, channel, channel, false);
        }
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
     * @deprecated Use {@link #selectVersionWithFallback(ManagedMod, ModEntry, PluginConfig)} instead
     */
    @Deprecated
    public static ModVersion selectVersion(ManagedMod mod, ModEntry entry, PluginConfig config) {
        return selectVersionWithFallback(mod, entry, config).version();
    }

    /**
     * Selects the appropriate version for a mod with automatic fallback to less strict channels.
     * <p>
     * If no version is found for the configured channel, automatically falls back:
     * RELEASE → BETA → ALPHA
     * </p>
     *
     * @param mod    the managed mod with potential release channel override
     * @param entry  the mod entry containing available versions
     * @param config the plugin configuration with global default channel
     * @return selection result containing the version and fallback info
     */
    public static SelectionResult selectVersionWithFallback(ManagedMod mod, ModEntry entry, PluginConfig config) {
        // If pinned to a specific version, try to find it
        if (!mod.wantsLatestVersion()) {
            ModVersion pinned = findByVersionId(entry.getAvailableVersions(), mod.getDesiredVersionId());
            if (pinned != null) {
                return SelectionResult.of(pinned, getEffectiveChannel(mod, config));
            }
            // Fall through to channel-based selection if pinned version not found
        }

        // Determine effective release channel
        ReleaseChannel requestedChannel = getEffectiveChannel(mod, config);

        // Try to select with the requested channel
        ModVersion version = selectBestVersion(entry.getAvailableVersions(), requestedChannel);
        if (version != null) {
            return SelectionResult.of(version, requestedChannel);
        }

        // Fallback to less strict channels
        for (ReleaseChannel fallbackChannel : getFallbackChannels(requestedChannel)) {
            version = selectBestVersion(entry.getAvailableVersions(), fallbackChannel);
            if (version != null) {
                return SelectionResult.withFallback(version, requestedChannel, fallbackChannel);
            }
        }

        return SelectionResult.notFound(requestedChannel);
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
     * Gets the fallback channels for a given channel, ordered from most to least strict.
     * <p>
     * Fallback order: RELEASE → BETA → ALPHA
     * </p>
     *
     * @param channel the current channel
     * @return array of fallback channels (may be empty if already at ALPHA)
     */
    private static ReleaseChannel[] getFallbackChannels(ReleaseChannel channel) {
        return switch (channel) {
            case RELEASE -> new ReleaseChannel[]{ReleaseChannel.BETA, ReleaseChannel.ALPHA};
            case BETA -> new ReleaseChannel[]{ReleaseChannel.ALPHA};
            case ALPHA -> new ReleaseChannel[0];
        };
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
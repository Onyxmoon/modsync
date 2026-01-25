package de.onyxmoon.modsync.storage.model;

import de.onyxmoon.modsync.api.PluginType;
import de.onyxmoon.modsync.api.ReleaseChannel;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * JSON structure for mods.json - the mod list file.
 * This file is versionable and can be shared across servers.
 * Does NOT contain installation state (that's in mods.lock.json).
 *
 * @see de.onyxmoon.modsync.storage.ManagedModStorage#SCHEMA_VERSION
 */
public class ModListFile {
    private int version;
    private String name;
    private Instant createdAt;
    private Instant lastModifiedAt;
    private List<ModListEntry> mods;

    public ModListFile() {
        this.mods = new ArrayList<>();
    }

    public ModListFile(int version, String name, Instant createdAt, Instant lastModifiedAt, List<ModListEntry> mods) {
        this.version = version;
        this.name = name;
        this.createdAt = createdAt;
        this.lastModifiedAt = lastModifiedAt;
        this.mods = mods != null ? mods : new ArrayList<>();
    }

    public int getVersion() {
        return version;
    }

    public String getName() {
        return name;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastModifiedAt() {
        return lastModifiedAt;
    }

    public List<ModListEntry> getMods() {
        return mods;
    }

    /**
     * A single mod entry in mods.json.
     * Contains only the "intent" - what the user wants to track.
     */
    public static class ModListEntry {
        private String modId;
        private String name;
        private String slug;
        private String source;
        private PluginType pluginType;
        private String desiredVersionId;
        private Instant addedAt;
        private String addedViaUrl;
        private ReleaseChannel releaseChannelOverride;

        public ModListEntry() {
        }

        public ModListEntry(String modId, String name, String slug, String source,
                           PluginType pluginType, String desiredVersionId,
                           Instant addedAt, String addedViaUrl, ReleaseChannel releaseChannelOverride) {
            this.modId = modId;
            this.name = name;
            this.slug = slug;
            this.source = source;
            this.pluginType = pluginType;
            this.desiredVersionId = desiredVersionId;
            this.addedAt = addedAt;
            this.addedViaUrl = addedViaUrl;
            this.releaseChannelOverride = releaseChannelOverride;
        }

        public String getModId() {
            return modId;
        }

        public String getName() {
            return name;
        }

        public String getSlug() {
            return slug;
        }

        public String getSource() {
            return source;
        }

        public PluginType getPluginType() {
            return pluginType;
        }

        public String getDesiredVersionId() {
            return desiredVersionId;
        }

        public Instant getAddedAt() {
            return addedAt;
        }

        public String getAddedViaUrl() {
            return addedViaUrl;
        }

        public ReleaseChannel getReleaseChannelOverride() {
            return releaseChannelOverride;
        }

        /**
         * Get the source ID for this entry (e.g., "curseforge:12345").
         */
        public String getSourceId() {
            return source.toLowerCase() + ":" + modId;
        }
    }
}

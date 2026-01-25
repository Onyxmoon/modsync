package de.onyxmoon.modsync.api.model;

import com.hypixel.hytale.common.plugin.PluginIdentifier;
import de.onyxmoon.modsync.api.PluginType;
import de.onyxmoon.modsync.api.ReleaseChannel;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * <p>This class is immutable. Use {@link #toBuilder()} to create modified copies.</p>
 */
public final class ManagedMod {

    // Base info (always present)
    private final String modId;
    private final String name;
    private final String slug;
    private final String source;
    private final PluginType pluginType;

    // Tracking info
    private final String desiredVersionId;
    private final Instant addedAt;
    private final String addedViaUrl;

    // Installed state (optional - null means not installed)
    private final InstalledState installedState;

    // Release channel override (optional - null means use global default)
    private final ReleaseChannel releaseChannelOverride;

    private ManagedMod(Builder builder) {
        this.modId = builder.modId;
        this.name = builder.name;
        this.slug = builder.slug;
        this.source = builder.source;
        this.pluginType = builder.pluginType;
        this.desiredVersionId = builder.desiredVersionId;
        this.addedAt = builder.addedAt;
        this.addedViaUrl = builder.addedViaUrl;
        this.installedState = builder.installedState;
        this.releaseChannelOverride = builder.releaseChannelOverride;
    }

    /**
     * Returns the unique source identifier in the format "source:modId".
     * This is the primary key for identifying mods across sources.
     *
     * @return the source ID (e.g., "curseforge:12345")
     */
    public String getSourceId() {
        return source + ":" + modId;
    }

    /**
     * Checks if this mod is currently installed.
     *
     * @return true if the mod has an installed state, false otherwise
     */
    public boolean isInstalled() {
        return installedState != null;
    }

    /**
     * Returns whether the user wants to track the latest version.
     *
     * @return true if desiredVersionId is null (meaning always use latest)
     */
    public boolean wantsLatestVersion() {
        return desiredVersionId == null;
    }

    /**
     * Gets the installed state if the mod is installed.
     *
     * @return an Optional containing the installed state, or empty if not installed
     */
    public Optional<InstalledState> getInstalledState() {
        return Optional.ofNullable(installedState);
    }

    /**
     * Gets the plugin identifier if the mod is installed.
     *
     * @return an Optional containing the plugin identifier (group:name), or empty if not installed
     */
    public Optional<PluginIdentifier> getIdentifier() {
        return installedState != null
                ? Optional.ofNullable(installedState.getIdentifier())
                : Optional.empty();
    }

    /**
     * Gets the identifier string in "group:name" format if installed.
     *
     * @return an Optional containing the identifier string, or empty if not installed
     */
    public Optional<String> getIdentifierString() {
        return getIdentifier()
                .map(PluginIdentifier::toString);
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

    /**
     * Gets the release channel override for this mod.
     *
     * @return the release channel override, or null if using global default
     */
    public ReleaseChannel getReleaseChannelOverride() {
        return releaseChannelOverride;
    }

    public Builder toBuilder() {
        return new Builder()
                .modId(this.modId)
                .name(this.name)
                .slug(this.slug)
                .source(this.source)
                .pluginType(this.pluginType)
                .desiredVersionId(this.desiredVersionId)
                .addedAt(this.addedAt)
                .addedViaUrl(this.addedViaUrl)
                .installedState(this.installedState)
                .releaseChannelOverride(this.releaseChannelOverride);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ManagedMod that = (ManagedMod) o;
        return Objects.equals(modId, that.modId) &&
                Objects.equals(name, that.name) &&
                Objects.equals(slug, that.slug) &&
                Objects.equals(source, that.source) &&
                pluginType == that.pluginType &&
                Objects.equals(desiredVersionId, that.desiredVersionId) &&
                Objects.equals(addedAt, that.addedAt) &&
                Objects.equals(addedViaUrl, that.addedViaUrl) &&
                Objects.equals(installedState, that.installedState);
    }

    @Override
    public int hashCode() {
        return Objects.hash(modId, name, slug, source, pluginType,
                desiredVersionId, addedAt, addedViaUrl, installedState);
    }

    @Override
    public String toString() {
        return "ManagedMod{" +
                "modId='" + modId + '\'' +
                ", name='" + name + '\'' +
                ", slug='" + slug + '\'' +
                ", source=" + source +
                ", pluginType=" + pluginType +
                ", desiredVersionId='" + desiredVersionId + '\'' +
                ", addedAt=" + addedAt +
                ", addedViaUrl='" + addedViaUrl + '\'' +
                ", installedState=" + installedState +
                ", releaseChannelOverride=" + releaseChannelOverride +
                '}';
    }

    public static final class Builder {
        private String modId;
        private String name;
        private String slug;
        private String source;
        private PluginType pluginType = PluginType.PLUGIN;
        private String desiredVersionId;
        private Instant addedAt;
        private String addedViaUrl;
        private InstalledState installedState;
        private ReleaseChannel releaseChannelOverride;

        private Builder() {
        }

        public Builder modId(String modId) {
            this.modId = modId;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder slug(String slug) {
            this.slug = slug;
            return this;
        }

        public Builder source(String source) {
            this.source = source;
            return this;
        }

        public Builder pluginType(PluginType pluginType) {
            this.pluginType = pluginType;
            return this;
        }

        public Builder desiredVersionId(String desiredVersionId) {
            this.desiredVersionId = desiredVersionId;
            return this;
        }

        public Builder addedAt(Instant addedAt) {
            this.addedAt = addedAt;
            return this;
        }

        public Builder addedViaUrl(String addedViaUrl) {
            this.addedViaUrl = addedViaUrl;
            return this;
        }

        public Builder installedState(InstalledState installedState) {
            this.installedState = installedState;
            return this;
        }

        public Builder releaseChannelOverride(ReleaseChannel releaseChannelOverride) {
            this.releaseChannelOverride = releaseChannelOverride;
            return this;
        }

        public ManagedMod build() {
            return new ManagedMod(this);
        }
    }
}
package de.onyxmoon.modsync.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.onyxmoon.modsync.ModSync;
import de.onyxmoon.modsync.api.ModListSource;
import de.onyxmoon.modsync.api.ReleaseChannel;
import de.onyxmoon.modsync.command.config.ChannelCommand;
import de.onyxmoon.modsync.storage.model.PluginConfig;
import de.onyxmoon.modsync.util.PermissionHelper;

import javax.annotation.Nonnull;
import java.awt.*;

/**
 * Command: /modsync config
 * Shows the current plugin configuration.
 * <p>
 * Subcommands:
 * - /modsync config channel [release|beta|alpha] - View or set default channel
 */
public class ConfigCommand extends AbstractPlayerCommand {
    private final ModSync modSync;

    public ConfigCommand(ModSync modSync) {
        super("config", "View and modify configuration");
        this.modSync = modSync;

        // Add subcommands
        this.addSubCommand(new ChannelCommand(modSync));
    }

    @Override
    protected void execute(@Nonnull CommandContext commandContext,
                          @Nonnull Store<EntityStore> store,
                          @Nonnull Ref<EntityStore> ref,
                          @Nonnull PlayerRef playerRef,
                          @Nonnull World world) {
        if (!PermissionHelper.checkAdminPermission(playerRef)) {
            return;
        }

        PluginConfig config = modSync.getConfigStorage().getConfig();

        playerRef.sendMessage(Message.raw("=== ModSync Configuration ===").color(Color.CYAN));

        // Default release channel
        ReleaseChannel channel = config.getDefaultReleaseChannel();
        playerRef.sendMessage(Message.raw("  Default Channel: ").color(Color.GRAY)
                .insert(Message.raw(channel.getDisplayName()).color(Color.YELLOW))
                .insert(Message.raw(" (" + getChannelDescription(channel) + ")").color(Color.DARK_GRAY)));

        // Current source
        ModListSource source = config.getCurrentSource();
        playerRef.sendMessage(Message.raw("  Current Source: ").color(Color.GRAY)
                .insert(Message.raw(source.getDisplayName()).color(Color.WHITE)));

        // API key status
        String apiKey = config.getApiKey(source);
        boolean hasKey = apiKey != null && !apiKey.isEmpty();
        playerRef.sendMessage(Message.raw("  API Key: ").color(Color.GRAY)
                .insert(Message.raw(hasKey ? "Set" : "Not set").color(hasKey ? Color.GREEN : Color.RED)));

        // Self-update settings
        playerRef.sendMessage(Message.raw("  Check Plugin Updates: ").color(Color.GRAY)
                .insert(Message.raw(config.isCheckForPluginUpdates() ? "Yes" : "No").color(Color.WHITE)));
        playerRef.sendMessage(Message.raw("  Include Prereleases: ").color(Color.GRAY)
                .insert(Message.raw(config.isIncludePrereleases() ? "Yes" : "No").color(Color.WHITE)));

        playerRef.sendMessage(Message.raw(""));
        playerRef.sendMessage(Message.raw("Use ").color(Color.GRAY)
                .insert(Message.raw("/modsync config channel <release|beta|alpha>").color(Color.WHITE))
                .insert(Message.raw(" to change default channel.").color(Color.GRAY)));
    }

    private String getChannelDescription(ReleaseChannel channel) {
        return switch (channel) {
            case RELEASE -> "Only stable releases";
            case BETA -> "Beta and release versions";
            case ALPHA -> "All versions including alpha";
        };
    }
}
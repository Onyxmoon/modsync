package de.onyxmoon.modsync.command.config;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.onyxmoon.modsync.ModSync;
import de.onyxmoon.modsync.api.ReleaseChannel;
import de.onyxmoon.modsync.storage.model.PluginConfig;
import de.onyxmoon.modsync.util.PermissionHelper;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nonnull;
import java.awt.*;

/**
 * Command: /modsync config channel [release|beta|alpha]
 * View or set the default release channel.
 * <p>
 * Usage:
 * - /modsync config channel              - Show current default channel
 * - /modsync config channel release      - Set to release only
 * - /modsync config channel beta         - Set to beta + release
 * - /modsync config channel alpha        - Set to all versions
 */
public class ChannelCommand extends AbstractPlayerCommand {
    private final ModSync modSync;

    public ChannelCommand(ModSync modSync) {
        super("channel", "View default release channel");
        this.modSync = modSync;
        this.addUsageVariant(new ChannelSetCommand(modSync));
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

        // Show current value
        ReleaseChannel channel = config.getDefaultReleaseChannel();
        playerRef.sendMessage(Message.raw("Default release channel: ").color(Color.GRAY)
                .insert(Message.raw(channel.getDisplayName()).color(Color.YELLOW)));
        playerRef.sendMessage(Message.raw(getChannelDescription(channel)).color(Color.GRAY));
        playerRef.sendMessage(Message.raw(""));
        playerRef.sendMessage(Message.raw("To change: ").color(Color.GRAY)
                .insert(Message.raw("/modsync config channel <release|beta|alpha>").color(Color.WHITE)));
    }

    public static class ChannelSetCommand extends AbstractPlayerCommand {
        private final ModSync modSync;
        private final RequiredArg<String> channelArg = this.withRequiredArg(
                "channel",
                "release | beta | alpha",
                ArgTypes.STRING
        );

        public ChannelSetCommand(ModSync modSync) {
            super("Set default release channel");
            this.modSync = modSync;
        }

        @Override
        protected void execute(@NonNullDecl CommandContext commandContext,
                               @NonNullDecl Store<EntityStore> store,
                               @NonNullDecl Ref<EntityStore> ref,
                               @NonNullDecl PlayerRef playerRef,
                               @NonNullDecl World world) {
            String channelStr = commandContext.get(channelArg);
            PluginConfig config = modSync.getConfigStorage().getConfig();

            // Set new value
            ReleaseChannel channel = ReleaseChannel.fromStringOrNull(channelStr);
            if (channel == null) {
                playerRef.sendMessage(Message.raw("Invalid channel: " + channelStr).color(Color.RED));
                playerRef.sendMessage(Message.raw("Valid options: release, beta, alpha").color(Color.GRAY));
                return;
            }

            ReleaseChannel previous = config.getDefaultReleaseChannel();
            config.setDefaultReleaseChannel(channel);
            modSync.getConfigStorage().save();

            playerRef.sendMessage(Message.raw("Default release channel set to: ").color(Color.GREEN)
                    .insert(Message.raw(channel.getDisplayName()).color(Color.YELLOW)));
            playerRef.sendMessage(Message.raw(getChannelDescription(channel)).color(Color.GRAY));

            if (previous != channel) {
                playerRef.sendMessage(Message.raw("Tip: ").color(Color.GRAY)
                        .insert(Message.raw("Use /modsync check to see if updates are available.").color(Color.WHITE)));
            }
        }
    }

    private static String getChannelDescription(ReleaseChannel channel) {
        return switch (channel) {
            case RELEASE -> "Only stable releases";
            case BETA -> "Beta and release versions";
            case ALPHA -> "All versions including alpha";
        };
    }
}
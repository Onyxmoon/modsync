package de.onyxmoon.modsync.command;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import de.onyxmoon.modsync.ModSync;
import de.onyxmoon.modsync.api.ReleaseChannel;
import de.onyxmoon.modsync.api.model.ManagedMod;
import de.onyxmoon.modsync.api.model.ManagedModRegistry;
import de.onyxmoon.modsync.util.CommandUtils;
import de.onyxmoon.modsync.util.ModSelector;
import de.onyxmoon.modsync.util.ModSelector.SelectionResult;
import de.onyxmoon.modsync.util.PermissionHelper;

import javax.annotation.Nonnull;
import java.awt.*;

/**
 * Command: /modsync setchannel <target> <channel>
 * Sets the release channel override for a specific mod.
 * <p>
 * Usage:
 * - /modsync setchannel ModName release  - Set mod to release only
 * - /modsync setchannel ModName beta     - Set mod to beta+release
 * - /modsync setchannel ModName alpha    - Set mod to all channels
 * - /modsync setchannel ModName default  - Remove override (use global)
 */
public class SetChannelCommand extends CommandBase {
    private final ModSync modSync;
    private final RequiredArg<String> targetArg = this.withRequiredArg(
            "target",
            "name | slug | identifier",
            ArgTypes.STRING
    );
    private final RequiredArg<String> channelArg = this.withRequiredArg(
            "channel",
            "release | beta | alpha | default",
            ArgTypes.STRING
    );

    public SetChannelCommand(ModSync modSync) {
        super("setchannel", "Set release channel for a mod");
        this.modSync = modSync;
    }

    @Override
    protected void executeSync(@Nonnull CommandContext commandContext) {
        if (!PermissionHelper.checkAdminPermission(commandContext)) {
            return;
        }

        CommandSender sender = commandContext.sender();
        String target = CommandUtils.stripQuotes(commandContext.get(targetArg));
        String channelStr = commandContext.get(channelArg);

        ManagedModRegistry registry = modSync.getManagedModStorage().getRegistry();
        SelectionResult result = ModSelector.findByNameOrSlugOrIdentifier(registry, target);

        switch (result) {
            case SelectionResult.Found found -> {
                ManagedMod mod = found.mod();

                // Parse channel - "default" means null (use global)
                ReleaseChannel channel = null;
                if (!"default".equalsIgnoreCase(channelStr)) {
                    channel = ReleaseChannel.fromStringOrNull(channelStr);
                    if (channel == null) {
                        sender.sendMessage(Message.raw("Invalid channel: " + channelStr).color(Color.RED));
                        sender.sendMessage(Message.raw("Valid options: release, beta, alpha, default").color(Color.GRAY));
                        return;
                    }
                }

                // Update mod with new channel override
                ManagedMod updatedMod = mod.toBuilder()
                        .releaseChannelOverride(channel)
                        .build();
                modSync.getManagedModStorage().updateMod(updatedMod);

                if (channel != null) {
                    sender.sendMessage(Message.raw("Set release channel for ").color(Color.GREEN)
                            .insert(Message.raw(mod.getName()).color(Color.WHITE))
                            .insert(Message.raw(" to ").color(Color.GREEN))
                            .insert(Message.raw(channel.getDisplayName()).color(Color.YELLOW)));
                } else {
                    sender.sendMessage(Message.raw("Removed release channel override for ").color(Color.GREEN)
                            .insert(Message.raw(mod.getName()).color(Color.WHITE))
                            .insert(Message.raw(" (using global default)").color(Color.GRAY)));
                }
            }
            case SelectionResult.NotFound notFound ->
                sender.sendMessage(Message.raw("Mod not found: " + notFound.query()).color(Color.RED));
            case SelectionResult.InvalidIndex ignored ->
                sender.sendMessage(Message.raw("Use name, slug, or identifier to specify the mod.").color(Color.RED));
            case SelectionResult.EmptyRegistry ignored ->
                sender.sendMessage(Message.raw("No mods in list.").color(Color.RED));
        }
    }
}

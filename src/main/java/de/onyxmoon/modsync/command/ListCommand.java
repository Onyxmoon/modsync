package de.onyxmoon.modsync.command;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import de.onyxmoon.modsync.ModSync;
import de.onyxmoon.modsync.api.ReleaseChannel;
import de.onyxmoon.modsync.api.model.InstalledState;
import de.onyxmoon.modsync.api.model.ManagedMod;
import de.onyxmoon.modsync.api.model.ManagedModRegistry;
import de.onyxmoon.modsync.util.PermissionHelper;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.List;

/**
 * Command: /modsync list
 * Lists all mods in the managed mod list with their installation status.
 */
public class ListCommand extends CommandBase {
    private final ModSync modSync;

    public ListCommand(ModSync modSync) {
        super("list", "List managed mods");
        this.modSync = modSync;
    }

    @Override
    protected void executeSync(@Nonnull CommandContext commandContext) {
        if (!PermissionHelper.checkAdminPermission(commandContext)) {
            return;
        }

        CommandSender sender = commandContext.sender();
        ManagedModRegistry registry = modSync.getManagedModStorage().getRegistry();
        List<ManagedMod> mods = registry.getAll();

        if (mods.isEmpty()) {
            sender.sendMessage(Message.parse("&n=== &nManaged &nMods &n===").color(Color.ORANGE));
            sender.sendMessage(Message.raw("No mods in list. Use ").color(Color.GRAY)
                    .insert(Message.raw("/modsync add <url>").color(Color.WHITE))
                    .insert(Message.raw(" to add mods.").color(Color.GRAY)));
            return;
        }

        sender.sendMessage(Message.raw("=== Managed Mods (" + mods.size() + ") ===").color(Color.ORANGE));

        for (ManagedMod mod : mods) {
            // First line: > ModName (version) [STATUS]
            String versionInfo;
            if (mod.isInstalled()) {
                versionInfo = mod.getInstalledState()
                        .map(InstalledState::getInstalledVersionNumber)
                        .orElse("?");
            } else {
                versionInfo = mod.wantsLatestVersion() ? "latest" : mod.getDesiredVersionId();
            }

            Message firstLine = Message.raw("> ").color(Color.ORANGE)
                    .insert(Message.raw(mod.getName()).color(Color.WHITE))
                    .insert(Message.raw(" (" + versionInfo + ")").color(Color.GRAY))
                    .insert(Message.raw(" ").insert(getStatusMessage(mod)));

            // Show release channel override in first line if set
            ReleaseChannel channelOverride = mod.getReleaseChannelOverride();
            if (channelOverride != null) {
                firstLine = firstLine.insert(Message.raw(" [" + channelOverride.getDisplayName() + "]").color(Color.YELLOW));
            }

            sender.sendMessage(firstLine);

            // Second line: Identifier | Type | Source
            String identifier = mod.getIdentifierString().orElse("-");
            String typeAbbrev = mod.getPluginType().getDisplayName();
            String source = mod.getSource().getDisplayName();

            Message secondLine = Message.raw("    ").color(Color.GRAY)
                    .insert(Message.raw(identifier).color(Color.CYAN))
                    .insert(Message.raw(" | ").color(Color.GRAY))
                    .insert(Message.raw(typeAbbrev).color(Color.YELLOW))
                    .insert(Message.raw(" | ").color(Color.GRAY))
                    .insert(Message.raw(source).color(Color.GRAY));

            sender.sendMessage(secondLine);
        }

        // Summary
        int installed = registry.getInstalled().size();
        int notInstalled = registry.getNotInstalled().size();

        sender.sendMessage(Message.raw(""));
        sender.sendMessage(Message.raw("Installed: ").color(Color.GRAY)
                .insert(Message.raw(String.valueOf(installed)).color(Color.GREEN))
                .insert(Message.raw(" | Not installed: ").color(Color.GRAY))
                .insert(Message.raw(String.valueOf(notInstalled)).color(Color.RED)));
    }

    private Message getStatusMessage(ManagedMod mod) {
        if (!mod.isInstalled()) {
            return Message.raw("[NOT INSTALLED]").color(Color.RED);
        }

        // TODO: Check for updates (compare installed version with latest available)
        // For now, just show installed
        return Message.raw("[INSTALLED]").color(Color.GREEN);
    }
}

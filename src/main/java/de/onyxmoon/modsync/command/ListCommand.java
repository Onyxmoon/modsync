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
import de.onyxmoon.modsync.api.model.InstalledMod;
import de.onyxmoon.modsync.api.model.ManagedModEntry;
import de.onyxmoon.modsync.api.model.ManagedModList;
import de.onyxmoon.modsync.util.PermissionHelper;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;

/**
 * Command: /modsync list
 * Lists all mods in the managed mod list with their installation status.
 */
public class ListCommand extends AbstractPlayerCommand {
    private final ModSync plugin;

    public ListCommand(ModSync plugin) {
        super("list", "List managed mods");
        this.plugin = plugin;
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

        ManagedModList modList = plugin.getManagedModListStorage().getModList();
        List<ManagedModEntry> mods = modList.getMods();

        if (mods.isEmpty()) {
            playerRef.sendMessage(Message.raw("=== Managed Mods ===").color("gold"));
            playerRef.sendMessage(Message.raw("No mods in list. Use ").color("gray")
                    .insert(Message.raw("/modsync add <url>").color("white"))
                    .insert(Message.raw(" to add mods.").color("gray")));
            return;
        }

        playerRef.sendMessage(Message.raw("=== Managed Mods (" + mods.size() + ") ===").color("gold"));

        for (ManagedModEntry entry : mods) {
            Optional<InstalledMod> installed = plugin.getInstalledModStorage()
                    .getRegistry()
                    .findBySourceId(entry.getSource(), entry.getModId());

            Message status = getStatusMessage(installed);

            // Show installed version if available, otherwise show desired version
            String versionInfo;
            if (installed.isPresent()) {
                versionInfo = installed.get().getInstalledVersionNumber();
            } else {
                versionInfo = entry.wantsLatestVersion() ? "latest" : entry.getDesiredVersionId();
            }

            Message line = status
                    .insert(Message.raw(" " + entry.getName()).color("white"));

            // Show identifier for installed mods
            if (installed.isPresent()) {
                line = line.insert(Message.raw(" [" + installed.get().getIdentifier().toString() + "]").color("aqua"));
            }

            // Show plugin type
            String typeAbbrev = entry.getPluginType().getDisplayName();
            line = line.insert(Message.raw(" [" + typeAbbrev + "]").color("light_purple"))
                    .insert(Message.raw(" (" + entry.getSource().getDisplayName() + ")").color("gray"))
                    .insert(Message.raw(" [" + versionInfo + "]").color("dark_gray"));

            playerRef.sendMessage(line);
        }

        // Summary
        int installed = countInstalled(mods);
        int notInstalled = mods.size() - installed;

        playerRef.sendMessage(Message.raw(""));
        playerRef.sendMessage(Message.raw("Installed: ").color("gray")
                .insert(Message.raw(String.valueOf(installed)).color("green"))
                .insert(Message.raw(" | Not installed: ").color("gray"))
                .insert(Message.raw(String.valueOf(notInstalled)).color("red")));
    }

    private Message getStatusMessage(Optional<InstalledMod> installed) {
        if (installed.isEmpty()) {
            return Message.raw("[NOT INSTALLED]").color("red");
        }

        // TODO: Check for updates (compare installed version with latest available)
        // For now, just show installed
        return Message.raw("[INSTALLED]").color("green");
    }

    private int countInstalled(List<ManagedModEntry> mods) {
        return (int) mods.stream()
                .filter(entry -> plugin.getInstalledModStorage()
                        .getRegistry()
                        .findBySourceId(entry.getSource(), entry.getModId())
                        .isPresent())
                .count();
    }
}

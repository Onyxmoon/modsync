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
import de.onyxmoon.modsync.util.PermissionHelper;

import javax.annotation.Nonnull;

/**
 * Command: /modsync reload
 * Reloads configuration.
 */
public class ReloadCommand extends AbstractPlayerCommand {
    private final ModSync plugin;

    public ReloadCommand(ModSync plugin) {
        super("reload", "Reload configuration");
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

        playerRef.sendMessage(Message.raw("Reloading configuration...").color("yellow"));

        try {
            plugin.getConfigStorage().reload();
            playerRef.sendMessage(Message.raw("Configuration reloaded successfully!").color("green"));
        } catch (Exception e) {
            playerRef.sendMessage(Message.raw("Failed to reload configuration: " + e.getMessage()).color("red"));
        }
    }
}

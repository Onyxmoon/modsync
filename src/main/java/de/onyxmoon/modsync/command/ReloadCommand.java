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
import java.awt.*;

/**
 * Command: /modsync reload
 * Reloads configuration and mod registry.
 */
public class ReloadCommand extends AbstractPlayerCommand {
    private final ModSync modSync;

    public ReloadCommand(ModSync modSync) {
        super("reload", "Reload configuration");
        this.modSync = modSync;
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

        playerRef.sendMessage(Message.raw("Reloading configuration...").color(Color.YELLOW));

        try {
            modSync.getConfigStorage().reload();
            modSync.getManagedModStorage().reload();
            playerRef.sendMessage(Message.raw("Configuration reloaded successfully!").color(Color.GREEN));
        } catch (Exception e) {
            playerRef.sendMessage(Message.raw("Failed to reload configuration: " + e.getMessage()).color(Color.RED));
        }
    }
}
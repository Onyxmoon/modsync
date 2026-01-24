package de.onyxmoon.modsync.command;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import de.onyxmoon.modsync.ModSync;
import de.onyxmoon.modsync.util.PermissionHelper;

import javax.annotation.Nonnull;
import java.awt.*;

/**
 * Command: /modsync reload
 * Reloads configuration and mod registry.
 */
public class ReloadCommand extends CommandBase {
    private final ModSync modSync;

    public ReloadCommand(ModSync modSync) {
        super("reload", "Reload configuration");
        this.modSync = modSync;
    }

    @Override
    protected void executeSync(@Nonnull CommandContext commandContext) {
        if (!PermissionHelper.checkAdminPermission(commandContext)) {
            return;
        }

        CommandSender sender = commandContext.sender();
        sender.sendMessage(Message.raw("Reloading configuration...").color(Color.YELLOW));

        try {
            modSync.getConfigStorage().reload();
            modSync.getManagedModStorage().reload();
            sender.sendMessage(Message.raw("Configuration reloaded successfully!").color(Color.GREEN));
        } catch (Exception e) {
            sender.sendMessage(Message.raw("Failed to reload configuration: " + e.getMessage()).color(Color.RED));
        }
    }
}

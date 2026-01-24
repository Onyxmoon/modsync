package de.onyxmoon.modsync.command;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import de.onyxmoon.modsync.ModSync;
import de.onyxmoon.modsync.api.model.UnmanagedMod;
import de.onyxmoon.modsync.util.PermissionHelper;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.List;

/**
 * Command: /modsync scan
 * Lists all unmanaged mods (JAR/ZIP files in mods folder not tracked by ModSync).
 */
public class ScanCommand extends CommandBase {
    private final ModSync modSync;

    public ScanCommand(ModSync modSync) {
        super("scan", "List unmanaged mods");
        this.modSync = modSync;
    }

    @Override
    protected void executeSync(@Nonnull CommandContext commandContext) {
        if (!PermissionHelper.checkAdminPermission(commandContext)) {
            return;
        }

        CommandSender sender = commandContext.sender();
        sender.sendMessage(Message.raw("Scanning for unmanaged mods...").color(Color.GRAY));

        List<UnmanagedMod> unmanaged = modSync.getScanService().scanForUnmanagedMods();

        if (unmanaged.isEmpty()) {
            sender.sendMessage(Message.raw("No unmanaged mods found.").color(Color.GREEN));
            sender.sendMessage(Message.raw("All mods in the mods folder are tracked by ModSync.").color(Color.GRAY));
            return;
        }

        sender.sendMessage(Message.raw("=== Unmanaged Mods (" + unmanaged.size() + ") ===").color(Color.CYAN));

        for (int i = 0; i < unmanaged.size(); i++) {
            UnmanagedMod mod = unmanaged.get(i);

            // Build the message line
            Message line = Message.raw((i + 1) + ". ").color(Color.GRAY);

            // Filename
            line = line.insert(Message.raw(mod.fileName()).color(Color.WHITE));

            // Plugin type badge
            String typeBadge = mod.pluginType().getDisplayName();
            line = line.insert(Message.raw(" [" + typeBadge + "]").color(Color.YELLOW));

            // Size
            line = line.insert(Message.raw(" (" + mod.getFormattedSize() + ")").color(Color.GRAY));

            sender.sendMessage(line);

            // Second line: identifier if available
            String identifierStr = mod.getIdentifierString();
            if (identifierStr != null) {
                sender.sendMessage(Message.raw("   Identifier: ").color(Color.GRAY)
                        .insert(Message.raw(identifierStr).color(Color.YELLOW)));
            } else {
                sender.sendMessage(Message.raw("   Identifier: ").color(Color.GRAY)
                        .insert(Message.raw("(unknown)").color(Color.GRAY)));
            }
        }

        sender.sendMessage(Message.raw(""));
        sender.sendMessage(Message.raw("Use ").color(Color.GRAY)
                .insert(Message.raw("/modsync import").color(Color.WHITE))
                .insert(Message.raw("   to try to import all mods or").color(Color.GRAY))
                .insert(Message.raw("/modsync import <filename|identifier> --url==[url]").color(Color.WHITE))
                .insert(Message.raw(" to import a specific mod.").color(Color.GRAY)));
    }
}

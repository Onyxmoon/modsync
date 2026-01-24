package de.onyxmoon.modsync.util;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import de.onyxmoon.modsync.api.model.ManagedMod;

import java.awt.Color;

/**
 * Utility class for consistent message formatting in mod-related commands.
 */
public final class CommandMessageFormatter {

    private CommandMessageFormatter() {
        // Utility class - prevent instantiation
    }

    /**
     * Sends a mod status line with the format: "> ModName [STATUS]"
     *
     * @param sender      the command sender
     * @param mod         the managed mod
     * @param status      the status text (e.g., "UPGRADED", "FAILED")
     * @param statusColor the color for the status text
     */
    public static void sendModStatus(CommandSender sender, ManagedMod mod, String status, Color statusColor) {
        Message firstLine = Message.raw("> ").color(Color.ORANGE)
                .insert(Message.raw(mod.getName()).color(Color.WHITE))
                .insert(Message.raw(" [" + status + "]").color(statusColor));
        sender.sendMessage(firstLine);
        sendIdentifierLine(sender, mod);
    }

    /**
     * Sends a mod status line with version information.
     *
     * @param sender      the command sender
     * @param mod         the managed mod
     * @param oldVersion  the old version number
     * @param newVersion  the new version number
     * @param status      the status text
     * @param statusColor the color for the status text
     */
    public static void sendModStatusWithVersion(CommandSender sender, ManagedMod mod,
                                                 String oldVersion, String newVersion,
                                                 String status, Color statusColor) {
        Message firstLine = Message.raw("> ").color(Color.ORANGE)
                .insert(Message.raw(mod.getName()).color(Color.WHITE))
                .insert(Message.raw(" [" + status + "]").color(statusColor));
        sender.sendMessage(firstLine);
        sendIdentifierLine(sender, mod);
        sendVersionLine(sender, oldVersion, newVersion);
    }

    /**
     * Sends the mod's identifier on an indented line.
     *
     * @param sender the command sender
     * @param mod    the managed mod
     */
    public static void sendIdentifierLine(CommandSender sender, ManagedMod mod) {
        String identifier = mod.getIdentifierString().orElse("-");
        sender.sendMessage(Message.raw("    ").color(Color.GRAY)
                .insert(Message.raw(identifier).color(Color.CYAN)));
    }

    /**
     * Sends a detail line with custom text (indented).
     *
     * @param sender the command sender
     * @param text   the text to display
     * @param color  the text color
     */
    public static void sendDetailLine(CommandSender sender, String text, Color color) {
        sender.sendMessage(Message.raw("    ").color(Color.GRAY)
                .insert(Message.raw(text).color(color)));
    }

    /**
     * Sends a version comparison line (oldVersion -> newVersion).
     *
     * @param sender     the command sender
     * @param oldVersion the old version number
     * @param newVersion the new version number
     */
    public static void sendVersionLine(CommandSender sender, String oldVersion, String newVersion) {
        CommandUtils.formatVersionLine(oldVersion, newVersion)
                .ifPresent(line -> sender.sendMessage(Message.raw("    ").color(Color.GRAY)
                        .insert(Message.raw(line.oldDisplay()).color(Color.RED))
                        .insert(Message.raw(" -> ").color(Color.GRAY))
                        .insert(Message.raw(line.newDisplay()).color(Color.GREEN))));
    }
}

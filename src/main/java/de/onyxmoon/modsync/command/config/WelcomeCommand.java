package de.onyxmoon.modsync.command.config;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import de.onyxmoon.modsync.ModSync;
import de.onyxmoon.modsync.storage.model.PluginConfig;
import de.onyxmoon.modsync.util.PermissionHelper;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.Locale;

/**
 * Command: /modsync config welcome [on|off]
 * View or set admin welcome messages.
 */
public class WelcomeCommand extends CommandBase {
    private final ModSync modSync;

    public WelcomeCommand(ModSync modSync) {
        super("welcome", "View or set admin welcome message");
        this.modSync = modSync;
        this.addUsageVariant(new WelcomeSetCommand(modSync));
    }

    @Override
    protected void executeSync(@Nonnull CommandContext commandContext) {
        if (!PermissionHelper.checkAdminPermission(commandContext)) {
            return;
        }

        CommandSender sender = commandContext.sender();
        PluginConfig config = modSync.getConfigStorage().getConfig();
        boolean enabled = !config.isDisableAdminWelcomeMessage();

        sender.sendMessage(Message.raw("Admin welcome message: ").color(Color.GRAY)
                .insert(Message.raw(enabled ? "Enabled" : "Disabled").color(enabled ? Color.GREEN : Color.RED)));
        sender.sendMessage(Message.raw(""));
        sender.sendMessage(Message.raw("To change: ").color(Color.GRAY)
                .insert(Message.raw("/modsync config welcome <on|off>").color(Color.WHITE)));
    }

    public static class WelcomeSetCommand extends CommandBase {
        private final ModSync modSync;
        private final RequiredArg<String> stateArg = this.withRequiredArg(
                "state",
                "on | off",
                ArgTypes.STRING
        );

        public WelcomeSetCommand(ModSync modSync) {
            super("Set admin welcome message");
            this.modSync = modSync;
        }

        @Override
        protected void executeSync(@NonNullDecl CommandContext commandContext) {
            if (!PermissionHelper.checkAdminPermission(commandContext)) {
                return;
            }

            CommandSender sender = commandContext.sender();
            String state = commandContext.get(stateArg);
            PluginConfig config = modSync.getConfigStorage().getConfig();

            Boolean enabled = parseEnabled(state);
            if (enabled == null) {
                sender.sendMessage(Message.raw("Invalid value: " + state).color(Color.RED));
                sender.sendMessage(Message.raw("Valid options: on, off").color(Color.GRAY));
                return;
            }

            config.setDisableAdminWelcomeMessage(!enabled);
            modSync.getConfigStorage().save();

            sender.sendMessage(Message.raw("Admin welcome message: ").color(Color.GREEN)
                    .insert(Message.raw(enabled ? "Enabled" : "Disabled").color(enabled ? Color.GREEN : Color.RED)));
        }

        private static Boolean parseEnabled(String input) {
            if (input == null) {
                return null;
            }

            return switch (input.toLowerCase(Locale.ROOT)) {
                case "on", "true", "yes", "enable", "enabled" -> true;
                case "off", "false", "no", "disable", "disabled" -> false;
                default -> null;
            };
        }
    }
}

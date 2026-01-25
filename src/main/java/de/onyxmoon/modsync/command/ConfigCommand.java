package de.onyxmoon.modsync.command;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import de.onyxmoon.modsync.ModSync;
import de.onyxmoon.modsync.api.ModProvider;
import de.onyxmoon.modsync.api.ReleaseChannel;
import de.onyxmoon.modsync.command.config.ChannelCommand;
import de.onyxmoon.modsync.command.config.KeyCommand;
import de.onyxmoon.modsync.storage.model.PluginConfig;
import de.onyxmoon.modsync.util.PermissionHelper;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Command: /modsync config
 * Shows the current plugin configuration.
 * <p>
 * Subcommands:
 * - /modsync config channel [release|beta|alpha] - View or set default channel
 * - /modsync config key <provider> <key>        - Set API key for a provider
 */
public class ConfigCommand extends CommandBase {
    private final ModSync modSync;

    public ConfigCommand(ModSync modSync) {
        super("config", "View and modify configuration");
        this.modSync = modSync;

        // Add subcommands
        this.addSubCommand(new ChannelCommand(modSync));
        this.addSubCommand(new KeyCommand(modSync));
    }

    @Override
    protected void executeSync(@Nonnull CommandContext commandContext) {
        if (!PermissionHelper.checkAdminPermission(commandContext)) {
            return;
        }

        CommandSender sender = commandContext.sender();
        PluginConfig config = modSync.getConfigStorage().getConfig();

        sender.sendMessage(Message.raw("=== ModSync Configuration ===").color(Color.CYAN));

        // Default release channel
        ReleaseChannel channel = config.getDefaultReleaseChannel();
        sender.sendMessage(Message.raw("> ").color(Color.ORANGE)
                .insert(Message.raw("Default Channel").color(Color.WHITE))
                .insert(Message.raw(": ").color(Color.GRAY))
                .insert(Message.raw(channel.getDisplayName()).color(Color.YELLOW))
                .insert(Message.raw(" (" + getChannelDescription(channel) + ")").color(Color.GRAY)));

        sender.sendMessage(Message.raw("> ").color(Color.ORANGE)
                .insert(Message.raw("API Keys").color(Color.WHITE)));

        List<ModProvider> providers = new ArrayList<>(modSync.getProviderRegistry().getProviders());
        providers.sort(Comparator.comparing(ModProvider::getDisplayName, String.CASE_INSENSITIVE_ORDER));
        for (ModProvider provider : providers) {
            if (!provider.requiresApiKey()) {
                sender.sendMessage(Message.raw("    ").color(Color.GRAY)
                        .insert(Message.raw(provider.getDisplayName()).color(Color.WHITE))
                        .insert(Message.raw(" | ").color(Color.GRAY))
                        .insert(Message.raw("Not required").color(Color.GREEN)));
                continue;
            }

            String apiKey = config.getApiKey(provider.getSource());
            boolean hasKey = apiKey != null && !apiKey.isEmpty();
            sender.sendMessage(Message.raw("    ").color(Color.GRAY)
                    .insert(Message.raw(provider.getDisplayName()).color(Color.WHITE))
                    .insert(Message.raw(" | ").color(Color.GRAY))
                    .insert(Message.raw(hasKey ? "Set" : "Not set").color(hasKey ? Color.GREEN : Color.RED)));
        }

        // Self-update settings
        sender.sendMessage(Message.raw("  Check Plugin Updates: ").color(Color.GRAY)
                .insert(Message.raw(config.isCheckForPluginUpdates() ? "Yes" : "No").color(Color.WHITE)));
        sender.sendMessage(Message.raw("  Include Prereleases: ").color(Color.GRAY)
                .insert(Message.raw(config.isIncludePrereleases() ? "Yes" : "No").color(Color.WHITE)));

        sender.sendMessage(Message.raw(""));
        sender.sendMessage(Message.raw(""));
        sender.sendMessage(Message.raw("Use ").color(Color.GRAY)
                .insert(Message.raw("/modsync config channel <release|beta|alpha>").color(Color.WHITE))
                .insert(Message.raw(" to change default channel.").color(Color.GRAY)));
        sender.sendMessage(Message.raw("Use ").color(Color.GRAY)
                .insert(Message.raw("/modsync config key <provider> <key>").color(Color.WHITE))
                .insert(Message.raw(" to set API keys.").color(Color.GRAY)));
    }

    private String getChannelDescription(ReleaseChannel channel) {
        return switch (channel) {
            case RELEASE -> "Only stable releases";
            case BETA -> "Beta and release versions";
            case ALPHA -> "All versions including alpha";
        };
    }
}

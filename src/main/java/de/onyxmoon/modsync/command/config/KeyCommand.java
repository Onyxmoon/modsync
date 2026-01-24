package de.onyxmoon.modsync.command.config;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import de.onyxmoon.modsync.ModSync;
import de.onyxmoon.modsync.api.ModListProvider;
import de.onyxmoon.modsync.api.ModListSource;
import de.onyxmoon.modsync.util.PermissionHelper;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * Command: /modsync config key <provider> <key>
 * View or set API keys for providers.
 */
public class KeyCommand extends CommandBase {
    private final ModSync modSync;

    public KeyCommand(ModSync modSync) {
        super("key", "View or set API keys");
        this.modSync = modSync;
        this.addUsageVariant(new KeySetCommand(modSync));
    }

    @Override
    protected void executeSync(@Nonnull CommandContext commandContext) {
        if (!PermissionHelper.checkAdminPermission(commandContext)) {
            return;
        }

        CommandSender sender = commandContext.sender();
        List<ModListProvider> providers = new ArrayList<>(modSync.getProviderRegistry().getProviders());
        providers.sort(Comparator.comparing(ModListProvider::getDisplayName, String.CASE_INSENSITIVE_ORDER));

        sender.sendMessage(Message.raw("=== Provider API Keys ===").color(Color.CYAN));
        for (ModListProvider provider : providers) {
            if (!provider.requiresApiKey()) {
                sender.sendMessage(Message.raw("  " + provider.getDisplayName() + ": ").color(Color.GRAY)
                        .insert(Message.raw("Not required").color(Color.GREEN)));
                continue;
            }

            String apiKey = modSync.getConfigStorage().getConfig().getApiKey(provider.getSource());
            boolean hasKey = apiKey != null && !apiKey.isBlank();
            sender.sendMessage(Message.raw("  " + provider.getDisplayName() + ": ").color(Color.GRAY)
                    .insert(Message.raw(hasKey ? "Set" : "Not set").color(hasKey ? Color.GREEN : Color.RED)));
        }

        sender.sendMessage(Message.raw(""));
        sender.sendMessage(Message.raw("To set a key: ").color(Color.GRAY)
                .insert(Message.raw("/modsync config key <provider> <key>").color(Color.WHITE)));
    }

    private static ModListSource resolveSource(String input, Collection<ModListSource> available) {
        for (ModListSource source : available) {
            if (source.name().equalsIgnoreCase(input) ||
                source.getDisplayName().equalsIgnoreCase(input)) {
                return source;
            }
        }
        return null;
    }

    public static class KeySetCommand extends CommandBase {
        private final ModSync modSync;
        private final RequiredArg<String> providerArg = this.withRequiredArg(
                "provider",
                "provider name (e.g. curseforge)",
                ArgTypes.STRING
        );
        private final RequiredArg<String> keyArg = this.withRequiredArg(
                "key",
                "API key for provider",
                ArgTypes.STRING
        );

        public KeySetCommand(ModSync modSync) {
            super("Set API key for a provider");
            this.modSync = modSync;
        }

        @Override
        protected void executeSync(@Nonnull CommandContext commandContext) {
            if (!PermissionHelper.checkAdminPermission(commandContext)) {
                return;
            }

            CommandSender sender = commandContext.sender();
            String providerName = commandContext.get(providerArg);
            String key = commandContext.get(keyArg);

            List<ModListSource> available = new ArrayList<>(modSync.getProviderRegistry().getAvailableSources());
            available.sort(Comparator.comparing(ModListSource::getDisplayName, String.CASE_INSENSITIVE_ORDER));

            ModListSource source = resolveSource(providerName, available);
            if (source == null) {
                sender.sendMessage(Message.raw("Unknown provider: " + providerName).color(Color.RED));
                sender.sendMessage(Message.raw("Available providers: " +
                        String.join(", ", available.stream()
                                .map(ModListSource::getDisplayName)
                                .toList()))
                        .color(Color.GRAY));
                return;
            }

            ModListProvider provider = modSync.getProviderRegistry().getProvider(source);
            if (!provider.requiresApiKey()) {
                sender.sendMessage(Message.raw(provider.getDisplayName() + " does not support API keys.").color(Color.YELLOW));
                return;
            }

            sender.sendMessage(Message.raw("Validating API key for " + provider.getDisplayName() + "...").color(Color.YELLOW));
            provider.validateApiKey(key)
                    .thenAccept(valid -> {
                        if (valid) {
                            modSync.getConfigStorage().getConfig().setApiKey(source, key);
                            modSync.getConfigStorage().save();
                            sender.sendMessage(Message.raw("API key set successfully for " + provider.getDisplayName()).color(Color.GREEN));
                        } else {
                            sender.sendMessage(Message.raw("Invalid API key for " + provider.getDisplayName()).color(Color.RED));
                        }
                    })
                    .exceptionally(ex -> {
                        sender.sendMessage(Message.raw("Error validating API key: " + ex.getMessage()).color(Color.RED));
                        return null;
                    });
        }
    }
}

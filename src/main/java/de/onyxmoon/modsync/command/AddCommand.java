package de.onyxmoon.modsync.command;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import de.onyxmoon.modsync.ModSync;
import de.onyxmoon.modsync.api.model.ManagedMod;
import de.onyxmoon.modsync.service.ProviderFetchService;
import de.onyxmoon.modsync.util.PermissionHelper;

import javax.annotation.Nonnull;
import java.awt.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Command: /modsync add <url>
 * Adds a mod to the managed mod list by URL.
 */
public class AddCommand extends CommandBase {
    private final ModSync modSync;
    private final RequiredArg<String> urlArg = this.withRequiredArg(
            "url",
            "URL to the mod (e.g., https://www.curseforge.com/hytale/mods/example-mod)",
            ArgTypes.STRING
    );

    public AddCommand(ModSync modSync) {
        super("add", "Add a mod by URL");
        this.modSync = modSync;
    }

    @Override
    protected void executeSync(@Nonnull CommandContext commandContext) {
        if (!PermissionHelper.checkAdminPermission(commandContext)) {
            return;
        }

        CommandSender sender = commandContext.sender();
        String url = commandContext.get(urlArg);

        if (url.isEmpty()) {
            sender.sendMessage(Message.raw("Usage: /modsync add <url>").color(Color.RED));
            return;
        }

        ProviderFetchService fetchService = modSync.getFetchService();
        List<String> providerNames = fetchService.getProviderNamesForUrl(url);

        if (providerNames.isEmpty()) {
            sender.sendMessage(Message.raw("No provider supports this URL.").color(Color.RED));
            return;
        }

        sender.sendMessage(Message.raw("Fetching mod info...").color(Color.YELLOW));

        List<String> missingApiKeys = new ArrayList<>();

        fetchService.fetchFromUrl(url, missingApiKeys::add)
            .thenAccept(result -> {
                if (result == null) {
                    sender.sendMessage(Message.raw("No provider could resolve the URL. Tried: " + String.join(", ", providerNames)).color(Color.RED));
                    if (!missingApiKeys.isEmpty()) {
                        sender.sendMessage(Message.raw("No API key set for: " + String.join(", ", missingApiKeys)).color(Color.RED));
                        sender.sendMessage(Message.raw("Use ").color(Color.GRAY)
                                .insert(Message.raw("/modsync config key <provider> <key>").color(Color.WHITE))
                                .insert(Message.raw(" to set API keys.").color(Color.GRAY)));
                    }
                    return;
                }
                var modEntry = result.modEntry();
                var registry = modSync.getManagedModStorage().getRegistry();
                if (registry.findBySourceId(result.provider().getSource(), modEntry.getModId()).isPresent()
                        || (modEntry.getSlug() != null && registry.findBySlug(modEntry.getSlug()).isPresent())) {
                    sender.sendMessage(Message.raw("Mod already in list: " + modEntry.getName()).color(Color.RED));
                    return;
                }
                // Create managed mod (without installedState - will be set on install)
                ManagedMod managedMod = ManagedMod.builder()
                        .modId(modEntry.getModId())
                        .source(result.provider().getSource())
                        .slug(modEntry.getSlug())
                        .name(modEntry.getName())
                        .pluginType(modEntry.getPluginType())
                        .desiredVersionId(result.parsedUrl().versionId())
                        .addedAt(Instant.now())
                        .addedViaUrl(url)
                        .build();

                // Add to managed storage
                modSync.getManagedModStorage().addMod(managedMod);

                sender.sendMessage(Message.raw("Added: ").color(Color.GREEN)
                        .insert(Message.raw(modEntry.getName()).color(Color.WHITE))
                        .insert(Message.raw(" (" + modEntry.getSlug() + ")").color(Color.GRAY))
                        .insert(Message.raw(" [" + modEntry.getPluginType().getDisplayName() + "]").color(Color.CYAN)));
                sender.sendMessage(Message.raw("Use ").color(Color.GRAY)
                        .insert(Message.raw("/modsync install").color(Color.WHITE))
                        .insert(Message.raw(" to download").color(Color.GRAY)));
            })
            .exceptionally(ex -> {
                sender.sendMessage(Message.raw("Failed to fetch mod: " + ex.getMessage()).color(Color.RED));
                return null;
            });
    }
}

package de.onyxmoon.modsync.command;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import de.onyxmoon.modsync.ModSync;
import de.onyxmoon.modsync.api.InvalidModUrlException;
import de.onyxmoon.modsync.api.ModListProvider;
import de.onyxmoon.modsync.api.ParsedModUrl;
import de.onyxmoon.modsync.api.model.ManagedMod;
import de.onyxmoon.modsync.util.PermissionHelper;

import javax.annotation.Nonnull;
import java.awt.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

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

        List<ModListProvider> providers = modSync.getUrlParserRegistry().findProviders(url);
        if (providers.isEmpty()) {
            sender.sendMessage(Message.raw("No provider supports this URL.").color(Color.RED));
            return;
        }

        List<String> missingApiKeys = new ArrayList<>();

        sender.sendMessage(Message.raw("Fetching mod info...").color(Color.YELLOW));

        List<String> providerNames = providers.stream()
                .map(ModListProvider::getDisplayName)
                .toList();

        fetchFromProviders(url, providers, missingApiKeys, 0)
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
                        .desiredVersionId(result.parsed().versionId())
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

    private CompletableFuture<FetchResult> fetchFromProviders(
            String url,
            List<ModListProvider> providers,
            List<String> missingApiKeys,
            int index) {
        if (index >= providers.size()) {
            return CompletableFuture.completedFuture(null);
        }

        ModListProvider provider = providers.get(index);
        ParsedModUrl parsed;
        try {
            parsed = provider.parse(url);
        } catch (InvalidModUrlException e) {
            return fetchFromProviders(url, providers, missingApiKeys, index + 1);
        }

        String apiKey = modSync.getConfigStorage().getConfig().getApiKey(provider.getSource());
        if (provider.requiresApiKey() && (apiKey == null || apiKey.isBlank())) {
            missingApiKeys.add(provider.getDisplayName());
            return fetchFromProviders(url, providers, missingApiKeys, index + 1);
        }

        CompletableFuture<de.onyxmoon.modsync.api.model.provider.ModEntry> fetchFuture =
                parsed.hasModId()
                        ? provider.fetchMod(apiKey, parsed.modId())
                        : provider.fetchModBySlug(apiKey, parsed.slug());

        return fetchFuture.handle((result, ex) -> {
            if (ex == null) {
                return CompletableFuture.completedFuture(new FetchResult(provider, parsed, result));
            }
            return fetchFromProviders(url, providers, missingApiKeys, index + 1);
        }).thenCompose(future -> future);
    }

    private record FetchResult(
            ModListProvider provider,
            ParsedModUrl parsed,
            de.onyxmoon.modsync.api.model.provider.ModEntry modEntry
    ) {
    }
}

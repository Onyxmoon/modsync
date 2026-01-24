package de.onyxmoon.modsync.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.onyxmoon.modsync.ModSync;
import de.onyxmoon.modsync.api.InvalidModUrlException;
import de.onyxmoon.modsync.api.ModListProvider;
import de.onyxmoon.modsync.api.ModUrlParser;
import de.onyxmoon.modsync.api.ParsedModUrl;
import de.onyxmoon.modsync.api.model.ManagedMod;
import de.onyxmoon.modsync.util.PermissionHelper;

import javax.annotation.Nonnull;
import java.awt.*;
import java.time.Instant;
import java.util.Optional;

/**
 * Command: /modsync add <url>
 * Adds a mod to the managed mod list by URL.
 */
public class AddCommand extends AbstractPlayerCommand {
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
    protected void execute(@Nonnull CommandContext commandContext,
                          @Nonnull Store<EntityStore> store,
                          @Nonnull Ref<EntityStore> ref,
                          @Nonnull PlayerRef playerRef,
                          @Nonnull World world) {
        if (!PermissionHelper.checkAdminPermission(playerRef)) {
            return;
        }

        String url = commandContext.get(urlArg);

        if (url.isEmpty()) {
            playerRef.sendMessage(Message.raw("Usage: /modsync add <url>").color(Color.RED));
            return;
        }

        // Find a parser for this URL
        Optional<ModUrlParser> parserOpt = modSync.getUrlParserRegistry().findParser(url);
        if (parserOpt.isEmpty()) {
            playerRef.sendMessage(Message.raw("Unsupported URL format. Supported: CurseForge").color(Color.RED));
            return;
        }

        ModUrlParser parser = parserOpt.get();
        ParsedModUrl parsed;
        try {
            parsed = parser.parse(url);
        } catch (InvalidModUrlException e) {
            playerRef.sendMessage(Message.raw("Invalid URL: " + e.getMessage()).color(Color.RED));
            return;
        }

        // Check if already in list
        if (parsed.hasSlug() && modSync.getManagedModStorage().getRegistry().findBySlug(parsed.slug()).isPresent()) {
            playerRef.sendMessage(Message.raw("Mod already in list: " + parsed.slug()).color(Color.RED));
            return;
        }

        // Check if we have a provider for this source
        if (!modSync.getProviderRegistry().hasProvider(parsed.source())) {
            playerRef.sendMessage(Message.raw("Source not yet supported: " + parsed.source().getDisplayName()).color(Color.RED));
            return;
        }

        ModListProvider provider = modSync.getProviderRegistry().getProvider(parsed.source());
        String apiKey = modSync.getConfigStorage().getConfig().getApiKey(parsed.source());

        if (provider.requiresApiKey() && apiKey == null) {
            playerRef.sendMessage(Message.raw("No API key set for " + parsed.source().getDisplayName() + ". Use: /modsync setkey <key>").color(Color.RED));
            return;
        }

        playerRef.sendMessage(Message.raw("Fetching mod info...").color(Color.YELLOW));

        provider.fetchModBySlug(apiKey, parsed.slug())
            .thenAccept(modEntry -> {
                // Create managed mod (without installedState - will be set on install)
                ManagedMod managedMod = ManagedMod.builder()
                        .modId(modEntry.getModId())
                        .source(parsed.source())
                        .slug(modEntry.getSlug())
                        .name(modEntry.getName())
                        .pluginType(modEntry.getPluginType())
                        .desiredVersionId(parsed.versionId())
                        .addedAt(Instant.now())
                        .addedViaUrl(url)
                        .build();

                // Add to managed storage
                modSync.getManagedModStorage().addMod(managedMod);

                playerRef.sendMessage(Message.raw("Added: ").color(Color.GREEN)
                        .insert(Message.raw(modEntry.getName()).color(Color.WHITE))
                        .insert(Message.raw(" (" + modEntry.getSlug() + ")").color(Color.GRAY))
                        .insert(Message.raw(" [" + modEntry.getPluginType().getDisplayName() + "]").color(Color.CYAN)));
                playerRef.sendMessage(Message.raw("Use ").color(Color.GRAY)
                        .insert(Message.raw("/modsync install").color(Color.WHITE))
                        .insert(Message.raw(" to download").color(Color.GRAY)));
            })
            .exceptionally(ex -> {
                playerRef.sendMessage(Message.raw("Failed to fetch mod: " + ex.getMessage()).color(Color.RED));
                return null;
            });
    }
}
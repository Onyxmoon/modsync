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
import de.onyxmoon.modsync.api.model.ManagedModEntry;
import de.onyxmoon.modsync.util.PermissionHelper;

import javax.annotation.Nonnull;
import java.time.Instant;
import java.util.Optional;

/**
 * Command: /modsync add <url>
 * Adds a mod to the managed mod list by URL.
 */
public class AddCommand extends AbstractPlayerCommand {
    private final ModSync plugin;
    private final RequiredArg<String> urlArg = this.withRequiredArg(
            "url",
            "URL to the mod (e.g., https://www.curseforge.com/hytale/mods/example-mod)",
            ArgTypes.STRING
    );

    public AddCommand(ModSync plugin) {
        super("add", "Add a mod by URL");
        this.plugin = plugin;
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
            playerRef.sendMessage(Message.raw("Usage: /modsync add <url>").color("red"));
            return;
        }

        // Find a parser for this URL
        Optional<ModUrlParser> parserOpt = plugin.getUrlParserRegistry().findParser(url);
        if (parserOpt.isEmpty()) {
            playerRef.sendMessage(Message.raw("Unsupported URL format. Supported: CurseForge").color("red"));
            return;
        }

        ModUrlParser parser = parserOpt.get();
        ParsedModUrl parsed;
        try {
            parsed = parser.parse(url);
        } catch (InvalidModUrlException e) {
            playerRef.sendMessage(Message.raw("Invalid URL: " + e.getMessage()).color("red"));
            return;
        }

        // Check if already in list
        if (parsed.hasSlug() && plugin.getManagedModListStorage().getModList().findBySlug(parsed.slug()).isPresent()) {
            playerRef.sendMessage(Message.raw("Mod already in list: " + parsed.slug()).color("red"));
            return;
        }

        // Check if we have a provider for this source
        if (!plugin.getProviderRegistry().hasProvider(parsed.source())) {
            playerRef.sendMessage(Message.raw("Source not yet supported: " + parsed.source().getDisplayName()).color("red"));
            return;
        }

        ModListProvider provider = plugin.getProviderRegistry().getProvider(parsed.source());
        String apiKey = plugin.getConfigStorage().getConfig().getApiKey(parsed.source());

        if (provider.requiresApiKey() && apiKey == null) {
            playerRef.sendMessage(Message.raw("No API key set for " + parsed.source().getDisplayName() + ". Use: /modsync setkey <key>").color("red"));
            return;
        }

        playerRef.sendMessage(Message.raw("Fetching mod info...").color("yellow"));

        provider.fetchModBySlug(apiKey, parsed.slug())
            .thenAccept(modEntry -> {
                // Create managed mod entry
                ManagedModEntry entry = ManagedModEntry.builder()
                        .modId(modEntry.getModId())
                        .source(parsed.source())
                        .slug(modEntry.getSlug())
                        .name(modEntry.getName())
                        .pluginType(modEntry.getPluginType())
                        .desiredVersionId(parsed.versionId())
                        .addedAt(Instant.now())
                        .addedViaUrl(url)
                        .build();

                // Add to managed list
                plugin.getManagedModListStorage().addMod(entry);

                playerRef.sendMessage(Message.raw("Added: ").color("green")
                        .insert(Message.raw(modEntry.getName()).color("white"))
                        .insert(Message.raw(" (" + modEntry.getSlug() + ")").color("gray"))
                        .insert(Message.raw(" [" + modEntry.getPluginType().getDisplayName() + "]").color("aqua")));
                playerRef.sendMessage(Message.raw("Use ").color("gray")
                        .insert(Message.raw("/modsync install").color("white"))
                        .insert(Message.raw(" to download").color("gray")));
            })
            .exceptionally(ex -> {
                playerRef.sendMessage(Message.raw("Failed to fetch mod: " + ex.getMessage()).color("red"));
                return null;
            });
    }
}

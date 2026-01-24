package de.onyxmoon.modsync.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.onyxmoon.modsync.ModSync;
import de.onyxmoon.modsync.api.InvalidModUrlException;
import de.onyxmoon.modsync.api.ModListProvider;
import de.onyxmoon.modsync.api.ModListSource;
import de.onyxmoon.modsync.api.ModUrlParser;
import de.onyxmoon.modsync.api.ParsedModUrl;
import de.onyxmoon.modsync.api.model.ImportMatch;
import de.onyxmoon.modsync.api.model.UnmanagedMod;
import de.onyxmoon.modsync.api.model.provider.ModEntry;
import de.onyxmoon.modsync.service.ModScanService;
import de.onyxmoon.modsync.util.CommandUtils;
import de.onyxmoon.modsync.util.PermissionHelper;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Command: /modsync import [target] [url]
 * Import an unmanaged mod into the ModSync registry.
 * <p>
 * Usage:
 * - /modsync import                            - Import all with auto-match (EXACT/HIGH only)
 * - /modsync import mymod.jar                  - Auto-match a specific mod
 * - /modsync import mymod.jar --url=<url>      - Manual import with URL
 * - /modsync import MyMod:Name                 - Import by identifier
 */
public class ImportCommand extends AbstractPlayerCommand {
    private final ModSync modSync;

    public ImportCommand(ModSync modSync) {
        super("import", "Import unmanaged mods");
        this.modSync = modSync;

        this.addUsageVariant(new ImportSpecificModCommand(modSync));
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

        // Find unmanaged mods
        ModScanService scanService = modSync.getScanService();
        List<UnmanagedMod> unmanaged = scanService.scanForUnmanagedMods();

        if (!unmanaged.isEmpty()) {
            for (UnmanagedMod mod : unmanaged) {
                autoMatchImport(modSync, playerRef, mod);
            }
        } else {
            playerRef.sendMessage(Message.raw("No unmanaged mods found.").color(Color.RED));
            playerRef.sendMessage(Message.raw("Use /modsync scan to see unmanaged mods.").color(Color.GRAY));
        }
    }

    private static Optional<UnmanagedMod> findUnmanagedMod(List<UnmanagedMod> unmanaged, String target) {
        // Try filename match (exact)
        for (UnmanagedMod mod : unmanaged) {
            if (mod.fileName().equalsIgnoreCase(target)) {
                return Optional.of(mod);
            }
        }

        // Try filename match (without extension)
        String targetNoExt = target.replace(".jar", "").replace(".zip", "");
        for (UnmanagedMod mod : unmanaged) {
            String modNameNoExt = mod.fileName().replace(".jar", "").replace(".zip", "");
            if (modNameNoExt.equalsIgnoreCase(targetNoExt)) {
                return Optional.of(mod);
            }
        }

        // Try identifier match
        for (UnmanagedMod mod : unmanaged) {
            String identifier = mod.getIdentifierString();
            if (identifier != null && identifier.equalsIgnoreCase(target)) {
                return Optional.of(mod);
            }
        }

        // Try mod name match (from identifier)
        for (UnmanagedMod mod : unmanaged) {
            if (mod.identifier() != null && mod.getDisplayName().equalsIgnoreCase(target)) {
                return Optional.of(mod);
            }
        }

        return Optional.empty();
    }

    private static void importWithUrl(ModSync modSync, PlayerRef playerRef, UnmanagedMod unmanagedMod, String url) {
        playerRef.sendMessage(Message.raw("Importing ").color(Color.GRAY)
                .insert(Message.raw(unmanagedMod.fileName()).color(Color.WHITE))
                .insert(Message.raw(" with URL...").color(Color.GRAY)));

        // Find a parser for this URL
        Optional<ModUrlParser> parserOpt = modSync.getUrlParserRegistry().findParser(url);
        if (parserOpt.isEmpty()) {
            playerRef.sendMessage(Message.raw("Unsupported URL format. Supported: CurseForge").color(Color.RED));
            return;
        }

        ModUrlParser parser = parserOpt.get();
        ParsedModUrl parsedUrl;
        try {
            parsedUrl = parser.parse(url);
        } catch (InvalidModUrlException e) {
            playerRef.sendMessage(Message.raw("Invalid URL: " + e.getMessage()).color(Color.RED));
            return;
        }

        ModListSource source = parsedUrl.source();
        String apiKey = modSync.getConfigStorage().getConfig().getApiKey(source);

        if (apiKey == null || apiKey.isEmpty()) {
            playerRef.sendMessage(Message.raw("No API key configured for " + source.getDisplayName()).color(Color.RED));
            return;
        }

        ModListProvider provider = modSync.getProviderRegistry().getProvider(source);
        if (provider == null) {
            playerRef.sendMessage(Message.raw("No provider available for " + source.getDisplayName()).color(Color.RED));
            return;
        }

        // Fetch mod entry
        CompletableFuture<ModEntry> fetchFuture;
        if (parsedUrl.modId() != null) {
            fetchFuture = provider.fetchMod(apiKey, parsedUrl.modId());
        } else if (parsedUrl.slug() != null) {
            fetchFuture = provider.fetchModBySlug(apiKey, parsedUrl.slug());
        } else {
            playerRef.sendMessage(Message.raw("Could not extract mod ID or slug from URL").color(Color.RED));
            return;
        }

        fetchFuture
                .thenAccept(modEntry -> {
                    modSync.getScanService().importWithEntry(unmanagedMod, modEntry);
                    playerRef.sendMessage(Message.raw("Successfully imported as: ").color(Color.GREEN)
                            .insert(Message.raw(modEntry.getName()).color(Color.YELLOW)));
                })
                .exceptionally(ex -> {
                    playerRef.sendMessage(Message.raw("Import failed: " + CommandUtils.extractErrorMessage(ex)).color(Color.RED));
                    return null;
                });
    }

    private static void autoMatchImport(ModSync modSync, PlayerRef playerRef, UnmanagedMod unmanagedMod) {
        playerRef.sendMessage(Message.raw("Searching for match for ").color(Color.GRAY)
                .insert(Message.raw(unmanagedMod.fileName()).color(Color.WHITE))
                .insert(Message.raw("...").color(Color.GRAY)));

        modSync.getScanService().findMatch(unmanagedMod)
                .thenAccept(match -> handleMatchResult(modSync, playerRef, match))
                .exceptionally(ex -> {
                    playerRef.sendMessage(Message.raw("Match search failed: " + CommandUtils.extractErrorMessage(ex)).color(Color.RED));
                    return null;
                });
    }

    private static void handleMatchResult(ModSync modSync, PlayerRef playerRef, ImportMatch match) {
        UnmanagedMod unmanagedMod = match.unmanagedMod();

        if (!match.hasMatch()) {
            playerRef.sendMessage(Message.raw("No match found for: ").color(Color.YELLOW)
                    .insert(Message.raw(unmanagedMod.fileName()).color(Color.WHITE)));
            playerRef.sendMessage(Message.raw("Use ").color(Color.GRAY)
                    .insert(Message.raw("/modsync import " + unmanagedMod.fileName() + " --url=<url>").color(Color.WHITE))
                    .insert(Message.raw(" to import manually.").color(Color.GRAY)));
            return;
        }

        ModEntry modEntry = match.matchedEntry();

        if (match.isAutoImportable()) {
            // High confidence - auto import
            assert modEntry != null;
            modSync.getScanService().importWithEntry(unmanagedMod, modEntry);
            playerRef.sendMessage(Message.raw("Match found: ").color(Color.GREEN)
                    .insert(Message.raw(modEntry.getName()).color(Color.YELLOW))
                    .insert(Message.raw(" (" + match.confidence().getDisplayName() + ")").color(Color.GRAY)));
            playerRef.sendMessage(Message.raw("Successfully imported!").color(Color.GREEN));
        } else {
            // Low confidence - show match but don't auto import
            assert modEntry != null;
            playerRef.sendMessage(Message.raw("Possible match found: ").color(Color.YELLOW)
                    .insert(Message.raw(modEntry.getName()).color(Color.WHITE))
                    .insert(Message.raw(" (" + match.confidence().getDisplayName() + ")").color(Color.GRAY)));
            if (match.matchReason() != null) {
                playerRef.sendMessage(Message.raw("Reason: " + match.matchReason()).color(Color.GRAY));
            }
            playerRef.sendMessage(Message.raw(""));
            playerRef.sendMessage(Message.raw("To confirm, use: ").color(Color.GRAY)
                    .insert(Message.raw("/modsync import " + unmanagedMod.fileName() + " https://curseforge.com/hytale/mods/" + modEntry.getSlug()).color(Color.WHITE)));
        }
    }

    public static class ImportSpecificModCommand extends AbstractPlayerCommand {
        private final ModSync modSync;
        private final RequiredArg<String> targetArg = this.withRequiredArg(
                "target",
                "filename, identifier, or empty for all",
                ArgTypes.STRING
        );

        private final OptionalArg<String> urlArg = this.withOptionalArg(
                "url",
                "URL (optional)",
                ArgTypes.STRING
        );

        public ImportSpecificModCommand(ModSync modSync) {
            super("Set default release channel");
            this.modSync = modSync;
        }

        @Override
        protected void execute(@NonNullDecl CommandContext commandContext,
                               @NonNullDecl Store<EntityStore> store,
                               @NonNullDecl Ref<EntityStore> ref,
                               @NonNullDecl PlayerRef playerRef,
                               @NonNullDecl World world) {

            if (!PermissionHelper.checkAdminPermission(playerRef)) {
                return;
            }

            String target = commandContext.get(targetArg);
            String url = commandContext.get(urlArg);

            // Find the unmanaged mod
            ModScanService scanService = modSync.getScanService();
            List<UnmanagedMod> unmanaged = scanService.scanForUnmanagedMods();

            Optional<UnmanagedMod> found = findUnmanagedMod(unmanaged, target);
            if (found.isEmpty()) {
                playerRef.sendMessage(Message.raw("Unmanaged mod not found: " + target).color(Color.RED));
                playerRef.sendMessage(Message.raw("Use /modsync scan to see unmanaged mods.").color(Color.GRAY));
                return;
            }

            UnmanagedMod unmanagedMod = found.get();

            if (url != null && !url.isEmpty()) {
                // Manual import with URL
                importWithUrl(modSync, playerRef, unmanagedMod, url);
            } else {
                // Auto-match
                autoMatchImport(modSync, playerRef, unmanagedMod);
            }

        }
    }
}


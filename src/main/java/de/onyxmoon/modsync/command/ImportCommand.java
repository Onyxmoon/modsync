package de.onyxmoon.modsync.command;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
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
public class ImportCommand extends CommandBase {
    private final ModSync modSync;

    public ImportCommand(ModSync modSync) {
        super("import", "Import unmanaged mods");
        this.modSync = modSync;

        this.addUsageVariant(new ImportSpecificModCommand(modSync));
    }

    @Override
    protected void executeSync(@Nonnull CommandContext commandContext) {
        if (!PermissionHelper.checkAdminPermission(commandContext)) {
            return;
        }

        CommandSender sender = commandContext.sender();
        // Find unmanaged mods
        ModScanService scanService = modSync.getScanService();
        List<UnmanagedMod> unmanaged = scanService.scanForUnmanagedMods();

        if (!unmanaged.isEmpty()) {
            for (UnmanagedMod mod : unmanaged) {
                autoMatchImport(modSync, sender, mod);
            }
        } else {
            sender.sendMessage(Message.raw("No unmanaged mods found.").color(Color.RED));
            sender.sendMessage(Message.raw("Use /modsync scan to see unmanaged mods.").color(Color.GRAY));
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

    private static void importWithUrl(ModSync modSync, CommandSender sender, UnmanagedMod unmanagedMod, String url) {
        sender.sendMessage(Message.raw("Importing ").color(Color.GRAY)
                .insert(Message.raw(unmanagedMod.fileName()).color(Color.WHITE))
                .insert(Message.raw(" with URL...").color(Color.GRAY)));

        // Find a parser for this URL
        Optional<ModUrlParser> parserOpt = modSync.getUrlParserRegistry().findParser(url);
        if (parserOpt.isEmpty()) {
            sender.sendMessage(Message.raw("Unsupported URL format. Supported: CurseForge").color(Color.RED));
            return;
        }

        ModUrlParser parser = parserOpt.get();
        ParsedModUrl parsedUrl;
        try {
            parsedUrl = parser.parse(url);
        } catch (InvalidModUrlException e) {
            sender.sendMessage(Message.raw("Invalid URL: " + e.getMessage()).color(Color.RED));
            return;
        }

        ModListSource source = parsedUrl.source();
        String apiKey = modSync.getConfigStorage().getConfig().getApiKey(source);

        if (apiKey == null || apiKey.isEmpty()) {
            sender.sendMessage(Message.raw("No API key configured for " + source.getDisplayName()).color(Color.RED));
            return;
        }

        ModListProvider provider = modSync.getProviderRegistry().getProvider(source);
        if (provider == null) {
            sender.sendMessage(Message.raw("No provider available for " + source.getDisplayName()).color(Color.RED));
            return;
        }

        // Fetch mod entry
        CompletableFuture<ModEntry> fetchFuture;
        if (parsedUrl.modId() != null) {
            fetchFuture = provider.fetchMod(apiKey, parsedUrl.modId());
        } else if (parsedUrl.slug() != null) {
            fetchFuture = provider.fetchModBySlug(apiKey, parsedUrl.slug());
        } else {
            sender.sendMessage(Message.raw("Could not extract mod ID or slug from URL").color(Color.RED));
            return;
        }

        fetchFuture
                .thenAccept(modEntry -> {
                    modSync.getScanService().importWithEntry(unmanagedMod, modEntry);
                    sender.sendMessage(Message.raw("Successfully imported as: ").color(Color.GREEN)
                            .insert(Message.raw(modEntry.getName()).color(Color.YELLOW)));
                })
                .exceptionally(ex -> {
                    sender.sendMessage(Message.raw("Import failed: " + CommandUtils.extractErrorMessage(ex)).color(Color.RED));
                    return null;
                });
    }

    private static void autoMatchImport(ModSync modSync, CommandSender sender, UnmanagedMod unmanagedMod) {
        sender.sendMessage(Message.raw("Searching for match for ").color(Color.GRAY)
                .insert(Message.raw(unmanagedMod.fileName()).color(Color.WHITE))
                .insert(Message.raw("...").color(Color.GRAY)));

        modSync.getScanService().findMatch(unmanagedMod)
                .thenAccept(match -> handleMatchResult(modSync, sender, match))
                .exceptionally(ex -> {
                    sender.sendMessage(Message.raw("Match search failed: " + CommandUtils.extractErrorMessage(ex)).color(Color.RED));
                    return null;
                });
    }

    private static void handleMatchResult(ModSync modSync, CommandSender sender, ImportMatch match) {
        UnmanagedMod unmanagedMod = match.unmanagedMod();

        if (!match.hasMatch()) {
            sender.sendMessage(Message.raw("No match found for: ").color(Color.YELLOW)
                    .insert(Message.raw(unmanagedMod.fileName()).color(Color.WHITE)));
            sender.sendMessage(Message.raw("Use ").color(Color.GRAY)
                    .insert(Message.raw("/modsync import " + unmanagedMod.fileName() + " --url=<url>").color(Color.WHITE))
                    .insert(Message.raw(" to import manually.").color(Color.GRAY)));
            return;
        }

        ModEntry modEntry = match.matchedEntry();

        if (match.isAutoImportable()) {
            // High confidence - auto import
            assert modEntry != null;
            modSync.getScanService().importWithEntry(unmanagedMod, modEntry);
            sender.sendMessage(Message.raw("Match found: ").color(Color.GREEN)
                    .insert(Message.raw(modEntry.getName()).color(Color.YELLOW))
                    .insert(Message.raw(" (" + match.confidence().getDisplayName() + ")").color(Color.GRAY)));
            sender.sendMessage(Message.raw("Successfully imported!").color(Color.GREEN));
        } else {
            // Low confidence - show match but don't auto import
            assert modEntry != null;
            sender.sendMessage(Message.raw("Possible match found: ").color(Color.YELLOW)
                    .insert(Message.raw(modEntry.getName()).color(Color.WHITE))
                    .insert(Message.raw(" (" + match.confidence().getDisplayName() + ")").color(Color.GRAY)));
            if (match.matchReason() != null) {
                sender.sendMessage(Message.raw("Reason: " + match.matchReason()).color(Color.GRAY));
            }
            sender.sendMessage(Message.raw(""));
            sender.sendMessage(Message.raw("To confirm, use: ").color(Color.GRAY)
                    .insert(Message.raw("/modsync import " + unmanagedMod.fileName() + " https://curseforge.com/hytale/mods/" + modEntry.getSlug()).color(Color.WHITE)));
        }
    }

    public static class ImportSpecificModCommand extends CommandBase {
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
        protected void executeSync(@NonNullDecl CommandContext commandContext) {
            if (!PermissionHelper.checkAdminPermission(commandContext)) {
                return;
            }

            CommandSender sender = commandContext.sender();
            String target = commandContext.get(targetArg);
            String url = commandContext.get(urlArg);

            // Find the unmanaged mod
            ModScanService scanService = modSync.getScanService();
            List<UnmanagedMod> unmanaged = scanService.scanForUnmanagedMods();

            Optional<UnmanagedMod> found = findUnmanagedMod(unmanaged, target);
            if (found.isEmpty()) {
                sender.sendMessage(Message.raw("Unmanaged mod not found: " + target).color(Color.RED));
                sender.sendMessage(Message.raw("Use /modsync scan to see unmanaged mods.").color(Color.GRAY));
                return;
            }

            UnmanagedMod unmanagedMod = found.get();

            if (url != null && !url.isEmpty()) {
                // Manual import with URL
                importWithUrl(modSync, sender, unmanagedMod, url);
            } else {
                // Auto-match
                autoMatchImport(modSync, sender, unmanagedMod);
            }

        }
    }
}


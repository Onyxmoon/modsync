package de.onyxmoon.modsync.command;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import de.onyxmoon.modsync.ModSync;
import de.onyxmoon.modsync.api.ModListProvider;
import de.onyxmoon.modsync.api.model.InstalledState;
import de.onyxmoon.modsync.api.model.ManagedMod;
import de.onyxmoon.modsync.api.model.ManagedModRegistry;
import de.onyxmoon.modsync.api.model.provider.ModVersion;
import de.onyxmoon.modsync.util.CommandUtils;
import de.onyxmoon.modsync.util.ModSelector;
import de.onyxmoon.modsync.util.ModSelector.SelectionResult;
import de.onyxmoon.modsync.util.PermissionHelper;
import de.onyxmoon.modsync.util.VersionSelector;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Command: /modsync install <target>
 * Installs mods from the managed mod list.
 * <p>
 * Usage:
 * - /modsync install               - Installs all uninstalled mods
 * - /modsync install [name]        - Installs mod by name
 * - /modsync install [slug]        - Installs mod by slug
 * - /modsync install [group:name]  - Installs mod by identifier
 */
public class InstallCommand extends CommandBase {
    private final ModSync modSync;
    private final OptionalArg<String> targetArg = this.withOptionalArg(
            "target",
            "name | slug | identifier",
            ArgTypes.STRING
    );

    public InstallCommand(ModSync modSync) {
        super("install", "Install mods from list");
        this.modSync = modSync;
    }

    @Override
    protected void executeSync(@Nonnull CommandContext commandContext) {
        if (!PermissionHelper.checkAdminPermission(commandContext)) {
            return;
        }

        CommandSender sender = commandContext.sender();
        ManagedModRegistry registry = modSync.getManagedModStorage().getRegistry();

        if (registry.isEmpty()) {
            sender.sendMessage(Message.raw("No mods in list. Use ").color(Color.RED)
                    .insert(Message.raw("/modsync add <url>").color(Color.WHITE))
                    .insert(Message.raw(" first.").color(Color.RED)));
            return;
        }

        String target = CommandUtils.stripQuotes(commandContext.get(targetArg));

        if (target == null || target.isEmpty()) {
            installAllMods(sender, registry);
        } else {
            installSpecificMod(sender, registry, target);
        }
    }

    private void showHelp(CommandSender sender, ManagedModRegistry registry) {
        sender.sendMessage(Message.raw("Usage: ").color(Color.CYAN)
                .insert(Message.raw("/modsync install <name|slug|identifier>").color(Color.WHITE)));
        sender.sendMessage(Message.raw("       ").color(Color.CYAN)
                .insert(Message.raw("/modsync install all").color(Color.WHITE))
                .insert(Message.raw(" to install all").color(Color.GRAY)));
        sender.sendMessage(Message.raw("Tip: ").color(Color.GRAY)
                .insert(Message.raw("Use quotes for names with spaces: ").color(Color.GRAY))
                .insert(Message.raw("\"My Mod\"").color(Color.YELLOW)));
        sender.sendMessage(Message.raw(""));

        // Show not installed mods
        List<ManagedMod> notInstalled = registry.getNotInstalled();
        if (notInstalled.isEmpty()) {
            sender.sendMessage(Message.raw("All mods are already installed.").color(Color.GREEN));
        } else {
            sender.sendMessage(Message.raw("=== Not Installed (" + notInstalled.size() + ") ===").color(Color.CYAN));
            for (ManagedMod mod : notInstalled) {
                sender.sendMessage(CommandUtils.formatModLine(mod));
            }
        }
    }

    private void installSpecificMod(CommandSender sender, ManagedModRegistry registry, String target) {
        SelectionResult result = ModSelector.findByNameOrSlugOrIdentifier(registry, target);

        switch (result) {
            case SelectionResult.Found found -> {
                ManagedMod mod = found.mod();
                if (mod.isInstalled()) {
                    sender.sendMessage(Message.raw("Mod already installed: " + CommandUtils.formatModLine(mod)).color(Color.YELLOW));
                    return;
                }
                sender.sendMessage(Message.raw("Installing " + mod.getName() + "...").color(Color.YELLOW));
                installMod(sender, mod);
            }
            case SelectionResult.NotFound notFound -> {
                sender.sendMessage(Message.raw("Mod not found: " + notFound.query()).color(Color.RED));
                sender.sendMessage(Message.raw(""));
                showHelp(sender, registry);
            }
            case SelectionResult.InvalidIndex ignored ->
                sender.sendMessage(Message.raw("Use name, slug, or identifier to install mods.").color(Color.RED));
            case SelectionResult.EmptyRegistry ignored ->
                sender.sendMessage(Message.raw("No mods in list.").color(Color.RED));
        }
    }

    private void installAllMods(CommandSender sender, ManagedModRegistry registry) {
        List<ManagedMod> notInstalled = registry.getNotInstalled();

        if (notInstalled.isEmpty()) {
            sender.sendMessage(Message.raw("All mods are already installed.").color(Color.GREEN));
            return;
        }

        sender.sendMessage(Message.raw("Installing " + notInstalled.size() + " mod(s)...").color(Color.YELLOW));

        AtomicInteger success = new AtomicInteger(0);
        AtomicInteger skipped = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);

        CompletableFuture<?>[] futures = notInstalled.stream()
                .map(mod -> installModAsync(mod, sender, skipped)
                        .thenAccept(installedState -> {
                            if (installedState != null) {
                                success.incrementAndGet();
                                sender.sendMessage(Message.raw("  Installed: ").insert(CommandUtils.formatModLine(mod)).color(Color.GREEN));
                            }
                        })
                        .exceptionally(ex -> {
                            failed.incrementAndGet();
                            String errorMsg = CommandUtils.extractErrorMessage(ex);
                            sender.sendMessage(Message.raw("  Failed: " + CommandUtils.formatModLine(mod).getRawText() + " - " + errorMsg).color(Color.RED));
                            return null;
                        }))
                .toArray(CompletableFuture[]::new);

        CompletableFuture.allOf(futures)
                .thenRun(() -> {
                    Message summary = Message.raw("Installation complete. ").color(Color.GRAY)
                            .insert(Message.raw("Success: " + success.get()).color(Color.GREEN));
                    if (skipped.get() > 0) {
                        summary = summary.insert(Message.raw(" | ").color(Color.GRAY))
                                .insert(Message.raw("Skipped: " + skipped.get()).color(Color.YELLOW));
                    }
                    if (failed.get() > 0) {
                        summary = summary.insert(Message.raw(" | ").color(Color.GRAY))
                                .insert(Message.raw("Failed: " + failed.get()).color(Color.RED));
                    }
                    sender.sendMessage(summary);
                    if (success.get() > 0) {
                        sender.sendMessage(Message.raw("Server restart required to load mods.").color(Color.CYAN));
                    }
                });
    }

    private void installMod(CommandSender sender, ManagedMod mod) {
        installModAsync(mod, null, null)
                .thenAccept(installedState -> {
                    if (installedState != null) {
                        sender.sendMessage(Message.raw("Installed: ").insert(CommandUtils.formatModLine(mod)).color(Color.GREEN)
                                .insert(Message.raw(" (" + installedState.getInstalledVersionNumber() + ")").color(Color.GRAY)));
                        sender.sendMessage(Message.raw("Server restart required to load the mod.").color(Color.CYAN));
                    } else {
                        sender.sendMessage(Message.raw("Skipped: ").insert(CommandUtils.formatModLine(mod)).insert(" - Download URL not available").color(Color.YELLOW));
                    }
                })
                .exceptionally(ex -> {
                    String errorMsg = CommandUtils.extractErrorMessage(ex);
                    sender.sendMessage(Message.raw("Failed to install ").insert(CommandUtils.formatModLine(mod)).insert(": " + errorMsg).color(Color.RED));
                    return null;
                });
    }

    private CompletableFuture<InstalledState> installModAsync(
            ManagedMod mod,
            CommandSender sender,
            AtomicInteger skippedCounter) {

        if (!modSync.getProviderRegistry().hasProvider(mod.getSource())) {
            return CompletableFuture.failedFuture(
                    new UnsupportedOperationException("No provider for source: " + mod.getSource())
            );
        }

        ModListProvider provider = modSync.getProviderRegistry().getProvider(mod.getSource());
        String apiKey = modSync.getConfigStorage().getConfig().getApiKey(mod.getSource());

        if (provider.requiresApiKey() && apiKey == null) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException("No API key set for " + mod.getSource().getDisplayName())
            );
        }

        return provider.fetchMod(apiKey, mod.getModId())
                .thenCompose(modEntry -> {
                    ModVersion version = VersionSelector.selectVersion(
                            mod, modEntry, modSync.getConfigStorage().getConfig());

                    if (version == null) {
                        return CompletableFuture.failedFuture(
                                new IllegalStateException("No version available for " + mod)
                        );
                    }

                    if (version.getDownloadUrl() == null || version.getDownloadUrl().isBlank()) {
                        if (skippedCounter != null) {
                            skippedCounter.incrementAndGet();
                        }
                        if (sender != null) {
                            sender.sendMessage(Message.raw("  Skipped: ").insert(CommandUtils.formatModLine(mod)).insert(" - Download URL not available").color(Color.YELLOW));
                        }
                        return CompletableFuture.completedFuture(null);
                    }

                    return modSync.getDownloadService().downloadAndInstall(mod, version)
                            .thenApply(installedState -> {
                                ManagedMod updatedMod = mod.toBuilder()
                                        .installedState(installedState)
                                        .build();
                                modSync.getManagedModStorage().updateMod(updatedMod);
                                return installedState;
                            });
                });
    }
}

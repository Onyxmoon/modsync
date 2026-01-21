package de.onyxmoon.modsync.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
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

import javax.annotation.Nonnull;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Command: /modsync install <target>
 * Installs mods from the managed mod list.
 * <p>
 * Usage:
 * - /modsync install               - Installs all uninstalled mods
 * - /modsync install <name>        - Installs mod by name
 * - /modsync install <slug>        - Installs mod by slug
 * - /modsync install <group:name>  - Installs mod by identifier
 */
public class InstallCommand extends AbstractPlayerCommand {
    private final ModSync plugin;
    private final OptionalArg<String> targetArg = this.withOptionalArg(
            "target",
            "name | slug | identifier",
            ArgTypes.STRING
    );

    public InstallCommand(ModSync plugin) {
        super("install", "Install mods from list");
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

        ManagedModRegistry registry = plugin.getManagedModStorage().getRegistry();

        if (registry.isEmpty()) {
            playerRef.sendMessage(Message.raw("No mods in list. Use ").color("red")
                    .insert(Message.raw("/modsync add <url>").color("white"))
                    .insert(Message.raw(" first.").color("red")));
            return;
        }

        String target = CommandUtils.stripQuotes(commandContext.get(targetArg));

        if (target == null || target.isEmpty()) {
            installAllMods(playerRef, registry);
        } else {
            installSpecificMod(playerRef, registry, target);
        }
    }

    private void showHelp(PlayerRef playerRef, ManagedModRegistry registry) {
        playerRef.sendMessage(Message.raw("Usage: ").color("gold")
                .insert(Message.raw("/modsync install <name|slug|identifier>").color("white")));
        playerRef.sendMessage(Message.raw("       ").color("gold")
                .insert(Message.raw("/modsync install all").color("white"))
                .insert(Message.raw(" to install all").color("gray")));
        playerRef.sendMessage(Message.raw("Tip: ").color("gray")
                .insert(Message.raw("Use quotes for names with spaces: ").color("gray"))
                .insert(Message.raw("\"My Mod\"").color("yellow")));
        playerRef.sendMessage(Message.raw(""));

        // Show not installed mods
        List<ManagedMod> notInstalled = registry.getNotInstalled();
        if (notInstalled.isEmpty()) {
            playerRef.sendMessage(Message.raw("All mods are already installed.").color("green"));
        } else {
            playerRef.sendMessage(Message.raw("=== Not Installed (" + notInstalled.size() + ") ===").color("gold"));
            for (ManagedMod mod : notInstalled) {
                playerRef.sendMessage(CommandUtils.formatModLine(mod));
            }
        }
    }

    private void installSpecificMod(PlayerRef playerRef, ManagedModRegistry registry, String target) {
        SelectionResult result = ModSelector.findByNameOrSlugOrIdentifier(registry, target);

        switch (result) {
            case SelectionResult.Found found -> {
                ManagedMod mod = found.mod();
                if (mod.isInstalled()) {
                    playerRef.sendMessage(Message.raw("Mod already installed: " + CommandUtils.formatModLine(mod)).color("yellow"));
                    return;
                }
                playerRef.sendMessage(Message.raw("Installing " + mod.getName() + "...").color("yellow"));
                installMod(playerRef, mod);
            }
            case SelectionResult.NotFound notFound -> {
                playerRef.sendMessage(Message.raw("Mod not found: " + notFound.query()).color("red"));
                playerRef.sendMessage(Message.raw(""));
                showHelp(playerRef, registry);
            }
            case SelectionResult.InvalidIndex ignored ->
                playerRef.sendMessage(Message.raw("Use name, slug, or identifier to install mods.").color("red"));
            case SelectionResult.EmptyRegistry ignored ->
                playerRef.sendMessage(Message.raw("No mods in list.").color("red"));
        }
    }

    private void installAllMods(PlayerRef playerRef, ManagedModRegistry registry) {
        List<ManagedMod> notInstalled = registry.getNotInstalled();

        if (notInstalled.isEmpty()) {
            playerRef.sendMessage(Message.raw("All mods are already installed.").color("green"));
            return;
        }

        playerRef.sendMessage(Message.raw("Installing " + notInstalled.size() + " mod(s)...").color("yellow"));

        AtomicInteger success = new AtomicInteger(0);
        AtomicInteger skipped = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);

        CompletableFuture<?>[] futures = notInstalled.stream()
                .map(mod -> installModAsync(mod, playerRef, skipped)
                        .thenAccept(installedState -> {
                            if (installedState != null) {
                                success.incrementAndGet();
                                playerRef.sendMessage(Message.raw("  Installed: ").insert(CommandUtils.formatModLine(mod)).color("green"));
                            }
                        })
                        .exceptionally(ex -> {
                            failed.incrementAndGet();
                            String errorMsg = CommandUtils.extractErrorMessage(ex);
                            playerRef.sendMessage(Message.raw("  Failed: " + CommandUtils.formatModLine(mod).getRawText() + " - " + errorMsg).color("red"));
                            return null;
                        }))
                .toArray(CompletableFuture[]::new);

        CompletableFuture.allOf(futures)
                .thenRun(() -> {
                    Message summary = Message.raw("Installation complete. ").color("gray")
                            .insert(Message.raw("Success: " + success.get()).color("green"));
                    if (skipped.get() > 0) {
                        summary = summary.insert(Message.raw(" | ").color("gray"))
                                .insert(Message.raw("Skipped: " + skipped.get()).color("yellow"));
                    }
                    if (failed.get() > 0) {
                        summary = summary.insert(Message.raw(" | ").color("gray"))
                                .insert(Message.raw("Failed: " + failed.get()).color("red"));
                    }
                    playerRef.sendMessage(summary);
                    if (success.get() > 0) {
                        playerRef.sendMessage(Message.raw("Server restart required to load mods.").color("gold"));
                    }
                });
    }

    private void installMod(PlayerRef playerRef, ManagedMod mod) {
        installModAsync(mod, null, null)
                .thenAccept(installedState -> {
                    if (installedState != null) {
                        playerRef.sendMessage(Message.raw("Installed: ").insert(CommandUtils.formatModLine(mod)).color("green")
                                .insert(Message.raw(" (" + installedState.getInstalledVersionNumber() + ")").color("gray")));
                        playerRef.sendMessage(Message.raw("Server restart required to load the mod.").color("gold"));
                    } else {
                        playerRef.sendMessage(Message.raw("Skipped: ").insert(CommandUtils.formatModLine(mod)).insert(" - Download URL not available").color("yellow"));
                    }
                })
                .exceptionally(ex -> {
                    String errorMsg = CommandUtils.extractErrorMessage(ex);
                    playerRef.sendMessage(Message.raw("Failed to install ").insert(CommandUtils.formatModLine(mod)).insert(": " + errorMsg).color("red"));
                    return null;
                });
    }

    private CompletableFuture<InstalledState> installModAsync(
            ManagedMod mod,
            PlayerRef playerRef,
            AtomicInteger skippedCounter) {

        if (!plugin.getProviderRegistry().hasProvider(mod.getSource())) {
            return CompletableFuture.failedFuture(
                    new UnsupportedOperationException("No provider for source: " + mod.getSource())
            );
        }

        ModListProvider provider = plugin.getProviderRegistry().getProvider(mod.getSource());
        String apiKey = plugin.getConfigStorage().getConfig().getApiKey(mod.getSource());

        if (provider.requiresApiKey() && apiKey == null) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException("No API key set for " + mod.getSource().getDisplayName())
            );
        }

        return provider.fetchMod(apiKey, mod.getModId())
                .thenCompose(modEntry -> {
                    ModVersion version = modEntry.getLatestVersion();

                    if (version == null) {
                        return CompletableFuture.failedFuture(
                                new IllegalStateException("No version available for " + CommandUtils.formatModLine(mod).getAnsiMessage())
                        );
                    }

                    if (version.getDownloadUrl() == null || version.getDownloadUrl().isBlank()) {
                        if (skippedCounter != null) {
                            skippedCounter.incrementAndGet();
                        }
                        if (playerRef != null) {
                            playerRef.sendMessage(Message.raw("  Skipped: ").insert(CommandUtils.formatModLine(mod)).insert(" - Download URL not available").color("yellow"));
                        }
                        return CompletableFuture.completedFuture(null);
                    }

                    return plugin.getDownloadService().downloadAndInstall(mod, version)
                            .thenApply(installedState -> {
                                ManagedMod updatedMod = mod.toBuilder()
                                        .installedState(installedState)
                                        .build();
                                plugin.getManagedModStorage().updateMod(updatedMod);
                                return installedState;
                            });
                });
    }
}
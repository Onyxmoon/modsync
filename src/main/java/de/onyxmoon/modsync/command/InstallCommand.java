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
import de.onyxmoon.modsync.util.PermissionHelper;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Command: /modsync install [name]
 * Installs mods from the managed mod list.
 */
public class InstallCommand extends AbstractPlayerCommand {
    private final ModSync plugin;
    private final OptionalArg<String> nameArg = this.withOptionalArg(
            "name",
            "Mod name or slug to install (omit to install all)",
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

        String name = commandContext.provided(nameArg) ? commandContext.get(nameArg) : "";

        ManagedModRegistry registry = plugin.getManagedModStorage().getRegistry();

        if (registry.isEmpty()) {
            playerRef.sendMessage(Message.raw("No mods in list. Use ").color("red")
                    .insert(Message.raw("/modsync add <url>").color("white"))
                    .insert(Message.raw(" first.").color("red")));
            return;
        }

        if (!name.isEmpty()) {
            // Install specific mod
            installSpecificMod(playerRef, registry, name);
        } else {
            // Install all mods
            installAllMods(playerRef, registry);
        }
    }

    private void installSpecificMod(PlayerRef playerRef, ManagedModRegistry registry, String nameOrSlug) {
        // Find by name or slug
        Optional<ManagedMod> modOpt = registry.findByName(nameOrSlug);
        if (modOpt.isEmpty()) {
            modOpt = registry.findBySlug(nameOrSlug);
        }

        // Try by identifier (group:name) if contains colon
        if (modOpt.isEmpty() && nameOrSlug.contains(":")) {
            modOpt = registry.findByIdentifier(nameOrSlug);
        }

        if (modOpt.isEmpty()) {
            playerRef.sendMessage(Message.raw("Mod not found in list: " + nameOrSlug).color("red"));
            return;
        }

        ManagedMod mod = modOpt.get();

        // Check if already installed
        if (mod.isInstalled()) {
            playerRef.sendMessage(Message.raw("Mod already installed: " + mod.getName()).color("yellow"));
            return;
        }

        playerRef.sendMessage(Message.raw("Installing " + mod.getName() + "...").color("yellow"));
        installMod(playerRef, mod);
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
                                playerRef.sendMessage(Message.raw("  Installed: " + mod.getName()).color("green"));
                            }
                        })
                        .exceptionally(ex -> {
                            failed.incrementAndGet();
                            String errorMsg = extractErrorMessage(ex);
                            playerRef.sendMessage(Message.raw("  Failed: " + mod.getName() + " - " + errorMsg).color("red"));
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

    private String extractErrorMessage(Throwable ex) {
        if (ex == null) {
            return "Unknown error";
        }
        // Unwrap CompletionException
        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
        String msg = cause.getMessage();
        return msg != null ? msg : cause.getClass().getSimpleName();
    }

    private void installMod(PlayerRef playerRef, ManagedMod mod) {
        installModAsync(mod, null, null)
                .thenAccept(installedState -> {
                    if (installedState != null) {
                        playerRef.sendMessage(Message.raw("Installed: " + mod.getName() + " ").color("green")
                                .insert(Message.raw("(" + installedState.getInstalledVersionNumber() + ")").color("gray")));
                        playerRef.sendMessage(Message.raw("Server restart required to load the mod.").color("gold"));
                    } else {
                        playerRef.sendMessage(Message.raw("Skipped: " + mod.getName() + " - Download URL not available").color("yellow"));
                    }
                })
                .exceptionally(ex -> {
                    String errorMsg = extractErrorMessage(ex);
                    playerRef.sendMessage(Message.raw("Failed to install " + mod.getName() + ": " + errorMsg).color("red"));
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
                                new IllegalStateException("No version available for " + mod.getName())
                        );
                    }

                    if (version.getDownloadUrl() == null || version.getDownloadUrl().isBlank()) {
                        if (skippedCounter != null) {
                            skippedCounter.incrementAndGet();
                        }
                        if (playerRef != null) {
                            playerRef.sendMessage(Message.raw("  Skipped: " + mod.getName() + " - Download URL not available").color("yellow"));
                        }
                        // Return null to indicate skipped (not failed)
                        return CompletableFuture.completedFuture(null);
                    }

                    // Download and install
                    return plugin.getDownloadService().downloadAndInstall(mod, version)
                            .thenApply(installedState -> {
                                // Update the mod with the installed state
                                ManagedMod updatedMod = mod.toBuilder()
                                        .installedState(installedState)
                                        .build();
                                plugin.getManagedModStorage().updateMod(updatedMod);
                                return installedState;
                            });
                });
    }
}
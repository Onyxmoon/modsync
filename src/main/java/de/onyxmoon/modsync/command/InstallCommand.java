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
import de.onyxmoon.modsync.api.model.ManagedModEntry;
import de.onyxmoon.modsync.api.model.ManagedModList;
import de.onyxmoon.modsync.api.model.ModVersion;
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

        ManagedModList modList = plugin.getManagedModListStorage().getModList();

        if (modList.isEmpty()) {
            playerRef.sendMessage(Message.raw("No mods in list. Use ").color("red")
                    .insert(Message.raw("/modsync add <url>").color("white"))
                    .insert(Message.raw(" first.").color("red")));
            return;
        }

        if (!name.isEmpty()) {
            // Install specific mod
            installSpecificMod(playerRef, modList, name);
        } else {
            // Install all mods
            installAllMods(playerRef, modList);
        }
    }

    private void installSpecificMod(PlayerRef playerRef, ManagedModList modList, String nameOrSlug) {
        // Find by name or slug
        Optional<ManagedModEntry> entryOpt = modList.findByName(nameOrSlug);
        if (entryOpt.isEmpty()) {
            entryOpt = modList.findBySlug(nameOrSlug);
        }

        if (entryOpt.isEmpty()) {
            playerRef.sendMessage(Message.raw("Mod not found in list: " + nameOrSlug).color("red"));
            return;
        }

        ManagedModEntry entry = entryOpt.get();

        // Check if already installed
        if (plugin.getInstalledModStorage().getRegistry()
                .findBySourceId(entry.getSource(), entry.getModId()).isPresent()) {
            playerRef.sendMessage(Message.raw("Mod already installed: " + entry.getName()).color("yellow"));
            return;
        }

        playerRef.sendMessage(Message.raw("Installing " + entry.getName() + "...").color("yellow"));
        installMod(playerRef, entry);
    }

    private void installAllMods(PlayerRef playerRef, ManagedModList modList) {
        List<ManagedModEntry> notInstalled = modList.getMods().stream()
                .filter(entry -> plugin.getInstalledModStorage().getRegistry()
                        .findBySourceId(entry.getSource(), entry.getModId()).isEmpty())
                .toList();

        if (notInstalled.isEmpty()) {
            playerRef.sendMessage(Message.raw("All mods are already installed.").color("green"));
            return;
        }

        playerRef.sendMessage(Message.raw("Installing " + notInstalled.size() + " mod(s)...").color("yellow"));

        AtomicInteger success = new AtomicInteger(0);
        AtomicInteger skipped = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);

        CompletableFuture<?>[] futures = notInstalled.stream()
                .map(entry -> installModAsync(entry, playerRef, skipped)
                        .thenAccept(installed -> {
                            if (installed != null) {
                                success.incrementAndGet();
                                playerRef.sendMessage(Message.raw("  Installed: " + entry.getName()).color("green"));
                            }
                        })
                        .exceptionally(ex -> {
                            failed.incrementAndGet();
                            String errorMsg = extractErrorMessage(ex);
                            playerRef.sendMessage(Message.raw("  Failed: " + entry.getName() + " - " + errorMsg).color("red"));
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

    private void installMod(PlayerRef playerRef, ManagedModEntry entry) {
        installModAsync(entry, null, null)
                .thenAccept(installed -> {
                    if (installed != null) {
                        playerRef.sendMessage(Message.raw("Installed: " + entry.getName() + " ").color("green")
                                .insert(Message.raw("(" + installed.getInstalledVersionNumber() + ")").color("gray")));
                        playerRef.sendMessage(Message.raw("Server restart required to load the mod.").color("gold"));
                    } else {
                        playerRef.sendMessage(Message.raw("Skipped: " + entry.getName() + " - Download URL not available").color("yellow"));
                    }
                })
                .exceptionally(ex -> {
                    String errorMsg = extractErrorMessage(ex);
                    playerRef.sendMessage(Message.raw("Failed to install " + entry.getName() + ": " + errorMsg).color("red"));
                    return null;
                });
    }

    private CompletableFuture<de.onyxmoon.modsync.api.model.InstalledMod> installModAsync(
            ManagedModEntry entry,
            PlayerRef playerRef,
            AtomicInteger skippedCounter) {

        if (!plugin.getProviderRegistry().hasProvider(entry.getSource())) {
            return CompletableFuture.failedFuture(
                    new UnsupportedOperationException("No provider for source: " + entry.getSource())
            );
        }

        ModListProvider provider = plugin.getProviderRegistry().getProvider(entry.getSource());
        String apiKey = plugin.getConfigStorage().getConfig().getApiKey(entry.getSource());

        if (provider.requiresApiKey() && apiKey == null) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException("No API key set for " + entry.getSource().getDisplayName())
            );
        }

        return provider.fetchMod(apiKey, entry.getModId())
                .thenCompose(modEntry -> {
                    ModVersion version = modEntry.getLatestVersion();

                    if (version == null) {
                        return CompletableFuture.failedFuture(
                                new IllegalStateException("No version available for " + entry.getName())
                        );
                    }

                    if (version.getDownloadUrl() == null || version.getDownloadUrl().isBlank()) {
                        if (skippedCounter != null) {
                            skippedCounter.incrementAndGet();
                        }
                        if (playerRef != null) {
                            playerRef.sendMessage(Message.raw("  Skipped: " + entry.getName() + " - Download URL not available").color("yellow"));
                        }
                        // Return null to indicate skipped (not failed)
                        return CompletableFuture.completedFuture(null);
                    }

                    // Download and install
                    return plugin.getDownloadService().downloadAndInstall(entry, version);
                });
    }
}
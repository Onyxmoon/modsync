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
 * Command: /modsync upgrade [name]
 * Upgrades installed mods to their latest versions.
 */
public class UpgradeCommand extends AbstractPlayerCommand {
    private final ModSync plugin;
    private final OptionalArg<String> nameArg = this.withOptionalArg(
            "name",
            "Mod name or slug to upgrade (omit to upgrade all)",
            ArgTypes.STRING
    );

    public UpgradeCommand(ModSync plugin) {
        super("upgrade", "Upgrade installed mods to latest version");
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
            playerRef.sendMessage(Message.raw("No mods in list.").color("red"));
            return;
        }

        if (!name.isEmpty()) {
            upgradeSpecificMod(playerRef, registry, name);
        } else {
            upgradeAllMods(playerRef, registry);
        }
    }

    private void upgradeSpecificMod(PlayerRef playerRef, ManagedModRegistry registry, String nameOrSlug) {
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

        if (!mod.isInstalled()) {
            playerRef.sendMessage(Message.raw("Mod is not installed: " + mod.getName()).color("red"));
            playerRef.sendMessage(Message.raw("Use ").color("gray")
                    .insert(Message.raw("/modsync install " + nameOrSlug).color("white"))
                    .insert(Message.raw(" to install it first.").color("gray")));
            return;
        }

        playerRef.sendMessage(Message.raw("Checking for update: " + mod.getName() + "...").color("yellow"));

        upgradeMod(mod)
                .thenAccept(result -> {
                    if (result == UpgradeResult.UPGRADED) {
                        playerRef.sendMessage(Message.raw("Upgraded: " + mod.getName()).color("green"));
                        playerRef.sendMessage(Message.raw("Server restart required to load the new version.").color("gold"));
                    } else if (result == UpgradeResult.UP_TO_DATE) {
                        playerRef.sendMessage(Message.raw("Already up to date: " + mod.getName()).color("green"));
                    } else {
                        playerRef.sendMessage(Message.raw("Skipped: " + mod.getName() + " - Download URL not available").color("yellow"));
                    }
                })
                .exceptionally(ex -> {
                    String errorMsg = extractErrorMessage(ex);
                    playerRef.sendMessage(Message.raw("Failed to upgrade " + mod.getName() + ": " + errorMsg).color("red"));
                    return null;
                });
    }

    private void upgradeAllMods(PlayerRef playerRef, ManagedModRegistry registry) {
        // Get all installed mods
        List<ManagedMod> installedMods = registry.getInstalled();

        if (installedMods.isEmpty()) {
            playerRef.sendMessage(Message.raw("No installed mods to upgrade.").color("yellow"));
            return;
        }

        playerRef.sendMessage(Message.raw("Checking " + installedMods.size() + " mod(s) for updates...").color("yellow"));

        AtomicInteger upgraded = new AtomicInteger(0);
        AtomicInteger upToDate = new AtomicInteger(0);
        AtomicInteger skipped = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);

        CompletableFuture<?>[] futures = installedMods.stream()
                .map(mod -> upgradeMod(mod)
                        .thenAccept(result -> {
                            switch (result) {
                                case UPGRADED:
                                    upgraded.incrementAndGet();
                                    playerRef.sendMessage(Message.raw("  Upgraded: " + mod.getName()).color("green"));
                                    break;
                                case UP_TO_DATE:
                                    upToDate.incrementAndGet();
                                    break;
                                case SKIPPED:
                                    skipped.incrementAndGet();
                                    playerRef.sendMessage(Message.raw("  Skipped: " + mod.getName() + " - Download URL not available").color("yellow"));
                                    break;
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
                    playerRef.sendMessage(Message.raw("=== Upgrade Complete ===").color("gold"));

                    Message summary = Message.raw("Upgraded: ").color("gray")
                            .insert(Message.raw(String.valueOf(upgraded.get())).color("green"))
                            .insert(Message.raw(" | Up to date: ").color("gray"))
                            .insert(Message.raw(String.valueOf(upToDate.get())).color("white"));

                    if (skipped.get() > 0) {
                        summary = summary.insert(Message.raw(" | Skipped: ").color("gray"))
                                .insert(Message.raw(String.valueOf(skipped.get())).color("yellow"));
                    }
                    if (failed.get() > 0) {
                        summary = summary.insert(Message.raw(" | Failed: ").color("gray"))
                                .insert(Message.raw(String.valueOf(failed.get())).color("red"));
                    }

                    playerRef.sendMessage(summary);

                    if (upgraded.get() > 0) {
                        playerRef.sendMessage(Message.raw("Server restart required to load updated mods.").color("gold"));
                    }
                    if (failed.get() > 0) {
                        playerRef.sendMessage(Message.raw("There were errors while deleting old mods. ").color("red")
                                .insert("This can encounter on windows based servers because of aggresive file locking. ").color("gray")
                                .insert("Please check pending_deletions.json in the mod folder.").color("gold"));
                    }
                });
    }

    private CompletableFuture<UpgradeResult> upgradeMod(ManagedMod mod) {
        if (!mod.isInstalled()) {
            return CompletableFuture.completedFuture(UpgradeResult.SKIPPED);
        }

        InstalledState currentState = mod.getInstalledState().orElseThrow();

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
                    ModVersion latestVersion = modEntry.getLatestVersion();

                    if (latestVersion == null) {
                        return CompletableFuture.failedFuture(
                                new IllegalStateException("No version available for " + mod.getName())
                        );
                    }

                    // Check if update is needed
                    if (latestVersion.getVersionId().equals(currentState.getInstalledVersionId())) {
                        return CompletableFuture.completedFuture(UpgradeResult.UP_TO_DATE);
                    }

                    // Check download URL
                    if (latestVersion.getDownloadUrl() == null || latestVersion.getDownloadUrl().isBlank()) {
                        return CompletableFuture.completedFuture(UpgradeResult.SKIPPED);
                    }

                    // Delete old version and install new
                    return plugin.getDownloadService().deleteMod(mod)
                            .thenCompose(v -> plugin.getDownloadService().downloadAndInstall(mod, latestVersion))
                            .thenApply(newInstalledState -> {
                                // Update the mod with the new installed state
                                ManagedMod updatedMod = mod.toBuilder()
                                        .installedState(newInstalledState)
                                        .build();
                                plugin.getManagedModStorage().updateMod(updatedMod);
                                return UpgradeResult.UPGRADED;
                            });
                });
    }

    private String extractErrorMessage(Throwable ex) {
        if (ex == null) {
            return "Unknown error";
        }
        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
        String msg = cause.getMessage();
        return msg != null ? msg : cause.getClass().getSimpleName();
    }

    private enum UpgradeResult {
        UPGRADED,
        UP_TO_DATE,
        SKIPPED
    }
}
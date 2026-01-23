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
import de.onyxmoon.modsync.util.VersionSelector;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Command: /modsync upgrade <target>
 * Upgrades installed mods to their latest versions.
 *
 * Usage:
 * - /modsync upgrade --all          - Upgrades all installed mods
 * - /modsync upgrade [name]         - Upgrades mod by name
 * - /modsync upgrade [slug]         - Upgrades mod by slug
 * - /modsync upgrade [group:name]   - Upgrades mod by identifier
 */
public class UpgradeCommand extends AbstractPlayerCommand {
    private final ModSync modSync;
    private final OptionalArg<String> targetArg = this.withOptionalArg(
            "target",
            "name | slug | identifier",
            ArgTypes.STRING
    );

    public UpgradeCommand(ModSync modSync) {
        super("upgrade", "Upgrade installed mods to latest version");
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

        ManagedModRegistry registry = modSync.getManagedModStorage().getRegistry();

        if (registry.isEmpty()) {
            playerRef.sendMessage(Message.raw("No mods in list.").color("red"));
            return;
        }

        String target = CommandUtils.stripQuotes(commandContext.get(targetArg));

        if (target == null || target.isEmpty()) {
            upgradeAllMods(playerRef, registry);
        } else {
            upgradeSpecificMod(playerRef, registry, target);
        }
    }

    private void showHelp(PlayerRef playerRef, ManagedModRegistry registry) {
        playerRef.sendMessage(Message.raw("Usage: ").color(Color.CYAN)
                .insert(Message.raw("/modsync upgrade <name|slug|identifier>").color("white")));
        playerRef.sendMessage(Message.raw("       ").color(Color.CYAN)
                .insert(Message.raw("/modsync upgrade all").color("white"))
                .insert(Message.raw(" to upgrade all").color("gray")));
        playerRef.sendMessage(Message.raw("Tip: ").color("gray")
                .insert(Message.raw("Use quotes for names with spaces: ").color("gray"))
                .insert(Message.raw("\"My Mod\"").color(Color.YELLOW)));
        playerRef.sendMessage(Message.raw(""));

        // Show installed mods
        List<ManagedMod> installed = registry.getInstalled();
        if (installed.isEmpty()) {
            playerRef.sendMessage(Message.raw("No installed mods to upgrade.").color(Color.YELLOW));
        } else {
            playerRef.sendMessage(Message.raw("=== Installed (" + installed.size() + ") ===").color(Color.CYAN));
            for (ManagedMod mod : installed) {
                playerRef.sendMessage(CommandUtils.formatModLine(mod));
            }
        }
    }

    private void upgradeSpecificMod(PlayerRef playerRef, ManagedModRegistry registry, String target) {
        SelectionResult result = ModSelector.findByNameOrSlugOrIdentifier(registry, target);

        switch (result) {
            case SelectionResult.Found found -> {
                ManagedMod mod = found.mod();
                if (!mod.isInstalled()) {
                    playerRef.sendMessage(Message.raw("Mod is not installed: " + mod.getName()).color("red"));
                    playerRef.sendMessage(Message.raw("Use ").color("gray")
                            .insert(Message.raw("/modsync install " + target).color("white"))
                            .insert(Message.raw(" to install it first.").color("gray")));
                    return;
                }
                playerRef.sendMessage(Message.raw("Checking for update: " + mod.getName() + "...").color(Color.YELLOW));
                upgradeMod(mod)
                        .thenAccept(upgradeResult -> {
                            switch (upgradeResult) {
                                case UpgradeResult.Upgraded u -> {
                                    playerRef.sendMessage(Message.raw("Upgraded: ").color("green")
                                            .insert(Message.raw(mod.getName()).color(Color.YELLOW))
                                            .insert(Message.raw(" -> ").color("gray"))
                                            .insert(Message.raw(u.newVersion()).color("white")));
                                    playerRef.sendMessage(Message.raw("Server restart required to load the new version.").color(Color.CYAN));
                                }
                                case UpgradeResult.UpToDate ignored ->
                                    playerRef.sendMessage(Message.raw("Already up to date: ").color("green")
                                            .insert(Message.raw(mod.getName()).color(Color.YELLOW)));
                                case UpgradeResult.Skipped ignored ->
                                    playerRef.sendMessage(Message.raw("Skipped: ").color(Color.YELLOW)
                                            .insert(Message.raw(mod.getName()).color(Color.YELLOW))
                                            .insert(Message.raw(" - Download URL not available").color("gray")));
                            }
                        })
                        .exceptionally(ex -> {
                            String errorMsg = CommandUtils.extractErrorMessage(ex);
                            playerRef.sendMessage(Message.raw("Failed to upgrade: ").color("red")
                                    .insert(Message.raw(mod.getName()).color(Color.YELLOW))
                                    .insert(Message.raw(" - " + errorMsg).color("gray")));
                            return null;
                        });
            }
            case SelectionResult.NotFound notFound -> {
                playerRef.sendMessage(Message.raw("Mod not found in list: " + notFound.query()).color("red"));
                playerRef.sendMessage(Message.raw("     "));
                showHelp(playerRef, registry);
            }
            case SelectionResult.InvalidIndex ignored ->
                playerRef.sendMessage(Message.raw("Use name, slug, or identifier to upgrade mods.").color("red"));
            case SelectionResult.EmptyRegistry ignored ->
                playerRef.sendMessage(Message.raw("No mods in list.").color("red"));
        }
    }

    private void upgradeAllMods(PlayerRef playerRef, ManagedModRegistry registry) {
        List<ManagedMod> installedMods = registry.getInstalled();

        if (installedMods.isEmpty()) {
            playerRef.sendMessage(Message.raw("No installed mods to upgrade.").color(Color.YELLOW));
            return;
        }

        playerRef.sendMessage(Message.raw("Checking " + installedMods.size() + " mod(s) for updates...").color(Color.YELLOW));

        AtomicInteger upgraded = new AtomicInteger(0);
        AtomicInteger upToDate = new AtomicInteger(0);
        AtomicInteger skipped = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);

        CompletableFuture<?>[] futures = installedMods.stream()
                .map(mod -> upgradeMod(mod)
                        .thenAccept(result -> {
                            switch (result) {
                                case UpgradeResult.Upgraded u -> {
                                    upgraded.incrementAndGet();
                                    playerRef.sendMessage(Message.raw("  Upgraded: ").color("green")
                                            .insert(Message.raw(mod.getName()).color(Color.YELLOW))
                                            .insert(Message.raw(" -> ").color("gray"))
                                            .insert(Message.raw(u.newVersion()).color("white")));
                                }
                                case UpgradeResult.UpToDate ignored -> upToDate.incrementAndGet();
                                case UpgradeResult.Skipped ignored -> {
                                    skipped.incrementAndGet();
                                    playerRef.sendMessage(Message.raw("  Skipped: ").color(Color.YELLOW)
                                            .insert(Message.raw(mod.getName()).color(Color.YELLOW))
                                            .insert(Message.raw(" - Download URL not available").color("gray")));
                                }
                            }
                        })
                        .exceptionally(ex -> {
                            failed.incrementAndGet();
                            String errorMsg = CommandUtils.extractErrorMessage(ex);
                            playerRef.sendMessage(Message.raw("  Failed: ").color("red")
                                    .insert(Message.raw(mod.getName()).color(Color.YELLOW))
                                    .insert(Message.raw(" - " + errorMsg).color("gray")));
                            return null;
                        }))
                .toArray(CompletableFuture[]::new);

        CompletableFuture.allOf(futures)
                .thenRun(() -> {
                    playerRef.sendMessage(Message.raw("=== Upgrade Complete ===").color(Color.CYAN));

                    Message summary = Message.raw("Upgraded: ").color("gray")
                            .insert(Message.raw(String.valueOf(upgraded.get())).color("green"))
                            .insert(Message.raw(" | Up to date: ").color("gray"))
                            .insert(Message.raw(String.valueOf(upToDate.get())).color("white"));

                    if (skipped.get() > 0) {
                        summary = summary.insert(Message.raw(" | Skipped: ").color("gray"))
                                .insert(Message.raw(String.valueOf(skipped.get())).color(Color.YELLOW));
                    }
                    if (failed.get() > 0) {
                        summary = summary.insert(Message.raw(" | Failed: ").color("gray"))
                                .insert(Message.raw(String.valueOf(failed.get())).color("red"));
                    }

                    playerRef.sendMessage(summary);

                    if (upgraded.get() > 0) {
                        playerRef.sendMessage(Message.raw("Server restart required to load updated mods.").color(Color.CYAN));
                    }
                    if (failed.get() > 0) {
                        playerRef.sendMessage(Message.raw("There were errors while deleting old mods. ").color("red")
                                .insert("This can encounter on windows based servers because of aggresive file locking. ").color("gray")
                                .insert("Please check pending_deletions.json in the mod folder.").color(Color.CYAN));
                    }
                });
    }

    private CompletableFuture<UpgradeResult> upgradeMod(ManagedMod mod) {
        if (!mod.isInstalled()) {
            return CompletableFuture.completedFuture(new UpgradeResult.Skipped());
        }

        InstalledState currentState = mod.getInstalledState().orElseThrow();

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
                    ModVersion latestVersion = VersionSelector.selectVersion(
                            mod, modEntry, modSync.getConfigStorage().getConfig());

                    if (latestVersion == null) {
                        return CompletableFuture.failedFuture(
                                new IllegalStateException("No version available for " + mod.getName())
                        );
                    }

                    // Check if update is needed
                    if (latestVersion.getVersionId().equals(currentState.getInstalledVersionId())) {
                        return CompletableFuture.completedFuture(new UpgradeResult.UpToDate());
                    }

                    // Check download URL
                    if (latestVersion.getDownloadUrl() == null || latestVersion.getDownloadUrl().isBlank()) {
                        return CompletableFuture.completedFuture(new UpgradeResult.Skipped());
                    }

                    String newVersionNumber = latestVersion.getVersionNumber();

                    // Delete old version and install new
                    return modSync.getDownloadService().deleteMod(mod)
                            .thenCompose(v -> modSync.getDownloadService().downloadAndInstall(mod, latestVersion))
                            .thenApply(newInstalledState -> {
                                ManagedMod updatedMod = mod.toBuilder()
                                        .installedState(newInstalledState)
                                        .build();
                                modSync.getManagedModStorage().updateMod(updatedMod);
                                return new UpgradeResult.Upgraded(newVersionNumber);
                            });
                });
    }

    private sealed interface UpgradeResult {
        record Upgraded(String newVersion) implements UpgradeResult {}
        record UpToDate() implements UpgradeResult {}
        record Skipped() implements UpgradeResult {}
    }
}
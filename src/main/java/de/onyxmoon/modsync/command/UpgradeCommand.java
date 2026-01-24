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
            playerRef.sendMessage(Message.raw("No mods in list.").color(Color.RED));
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
                .insert(Message.raw("/modsync upgrade <name|slug|identifier>").color(Color.WHITE)));
        playerRef.sendMessage(Message.raw("       ").color(Color.CYAN)
                .insert(Message.raw("/modsync upgrade all").color(Color.WHITE))
                .insert(Message.raw(" to upgrade all").color(Color.GRAY)));
        playerRef.sendMessage(Message.raw("Tip: ").color(Color.GRAY)
                .insert(Message.raw("Use quotes for names with spaces: ").color(Color.GRAY))
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
                    playerRef.sendMessage(Message.raw("Mod is not installed: " + mod.getName()).color(Color.RED));
                    playerRef.sendMessage(Message.raw("Use ").color(Color.GRAY)
                            .insert(Message.raw("/modsync install " + target).color(Color.WHITE))
                            .insert(Message.raw(" to install it first.").color(Color.GRAY)));
                    return;
                }
                playerRef.sendMessage(Message.raw("Checking for update: " + mod.getName() + "...").color(Color.YELLOW));
                upgradeMod(mod)
                        .thenAccept(upgradeResult -> {
                            switch (upgradeResult) {
                                case UpgradeResult.Upgraded u -> {
                                    sendModStatusWithVersion(playerRef, mod, u.oldVersion(), u.newVersion(), "UPGRADED", Color.GREEN);
                                    playerRef.sendMessage(Message.raw("Server restart required to load the new version.").color(Color.CYAN));
                                }
                                case UpgradeResult.UpToDate ignored ->
                                    sendModStatus(playerRef, mod, "UP TO DATE", Color.GREEN);
                                case UpgradeResult.Skipped ignored -> {
                                    sendModStatus(playerRef, mod, "SKIPPED", Color.YELLOW);
                                    sendDetailLine(playerRef, "Download URL not available", Color.GRAY);
                                }
                            }
                        })
                        .exceptionally(ex -> {
                            sendModStatus(playerRef, mod, "FAILED", Color.RED);
                            sendDetailLine(playerRef, CommandUtils.extractErrorMessage(ex), Color.RED);
                            return null;
                        });
            }
            case SelectionResult.NotFound notFound -> {
                playerRef.sendMessage(Message.raw("Mod not found in list: " + notFound.query()).color(Color.RED));
                playerRef.sendMessage(Message.raw("     "));
                showHelp(playerRef, registry);
            }
            case SelectionResult.InvalidIndex ignored ->
                playerRef.sendMessage(Message.raw("Use name, slug, or identifier to upgrade mods.").color(Color.RED));
            case SelectionResult.EmptyRegistry ignored ->
                playerRef.sendMessage(Message.raw("No mods in list.").color(Color.RED));
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
                                    sendModStatusWithVersion(playerRef, mod, u.oldVersion(), u.newVersion(), "UPGRADED", Color.GREEN);
                                }
                                case UpgradeResult.UpToDate ignored -> upToDate.incrementAndGet();
                                case UpgradeResult.Skipped ignored -> {
                                    skipped.incrementAndGet();
                                    sendModStatus(playerRef, mod, "SKIPPED", Color.YELLOW);
                                    sendDetailLine(playerRef, "Download URL not available", Color.GRAY);
                                }
                            }
                        })
                        .exceptionally(ex -> {
                            failed.incrementAndGet();
                            sendModStatus(playerRef, mod, "FAILED", Color.RED);
                            sendDetailLine(playerRef, CommandUtils.extractErrorMessage(ex), Color.RED);
                            return null;
                        }))
                .toArray(CompletableFuture[]::new);

        CompletableFuture.allOf(futures)
                .thenRun(() -> {
                    playerRef.sendMessage(Message.raw("=== Upgrade Complete ===").color(Color.CYAN));

                    Message summary = Message.raw("Upgraded: ").color(Color.GRAY)
                            .insert(Message.raw(String.valueOf(upgraded.get())).color(Color.GREEN))
                            .insert(Message.raw(" | Up to date: ").color(Color.GRAY))
                            .insert(Message.raw(String.valueOf(upToDate.get())).color(Color.WHITE));

                    if (skipped.get() > 0) {
                        summary = summary.insert(Message.raw(" | Skipped: ").color(Color.GRAY))
                                .insert(Message.raw(String.valueOf(skipped.get())).color(Color.YELLOW));
                    }
                    if (failed.get() > 0) {
                        summary = summary.insert(Message.raw(" | Failed: ").color(Color.GRAY))
                                .insert(Message.raw(String.valueOf(failed.get())).color(Color.RED));
                    }

                    playerRef.sendMessage(summary);

                    if (upgraded.get() > 0) {
                        playerRef.sendMessage(Message.raw("Server restart required to load updated mods.").color(Color.CYAN));
                    }
                    if (failed.get() > 0) {
                        playerRef.sendMessage(Message.raw("There were errors while deleting old mods. ").color(Color.RED)
                                .insert("This can encounter on windows based servers because of aggresive file locking. ").color(Color.GRAY)
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

                    String oldVersionNumber = currentState.getInstalledVersionNumber();
                    String newVersionNumber = latestVersion.getVersionNumber();

                    // Delete old version and install new
                    return modSync.getDownloadService().deleteMod(mod)
                            .thenCompose(v -> modSync.getDownloadService().downloadAndInstall(mod, latestVersion))
                            .thenApply(newInstalledState -> {
                                ManagedMod updatedMod = mod.toBuilder()
                                        .installedState(newInstalledState)
                                        .build();
                                modSync.getManagedModStorage().updateMod(updatedMod);
                                return new UpgradeResult.Upgraded(oldVersionNumber, newVersionNumber);
                            });
                });
    }

    private sealed interface UpgradeResult {
        record Upgraded(String oldVersion, String newVersion) implements UpgradeResult {}
        record UpToDate() implements UpgradeResult {}
        record Skipped() implements UpgradeResult {}
    }

    // ===== Formatting Helpers =====

    private void sendModStatus(PlayerRef playerRef, ManagedMod mod, String status, Color statusColor) {
        Message firstLine = Message.raw("> ").color(Color.ORANGE)
                .insert(Message.raw(mod.getName()).color(Color.WHITE))
                .insert(Message.raw(" [" + status + "]").color(statusColor));
        playerRef.sendMessage(firstLine);
        sendIdentifierLine(playerRef, mod);
    }

    private void sendModStatusWithVersion(PlayerRef playerRef, ManagedMod mod, String oldVersion, String newVersion, String status, Color statusColor) {
        Message firstLine = Message.raw("> ").color(Color.ORANGE)
                .insert(Message.raw(mod.getName()).color(Color.WHITE))
                .insert(Message.raw(" [" + status + "]").color(statusColor));
        playerRef.sendMessage(firstLine);
        sendIdentifierLine(playerRef, mod);
        sendVersionLine(playerRef, oldVersion, newVersion);
    }

    private void sendIdentifierLine(PlayerRef playerRef, ManagedMod mod) {
        String identifier = mod.getIdentifierString().orElse("-");
        playerRef.sendMessage(Message.raw("    ").color(Color.GRAY)
                .insert(Message.raw(identifier).color(Color.CYAN)));
    }

    private void sendDetailLine(PlayerRef playerRef, String text, Color color) {
        playerRef.sendMessage(Message.raw("    ").color(Color.GRAY)
                .insert(Message.raw(text).color(color)));
    }

    private void sendVersionLine(PlayerRef playerRef, String oldVersion, String newVersion) {
        CommandUtils.formatVersionLine(oldVersion, newVersion)
                .ifPresent(line -> playerRef.sendMessage(Message.raw("    ").color(Color.GRAY)
                        .insert(Message.raw(line.oldDisplay()).color(Color.RED))
                        .insert(Message.raw(" -> ").color(Color.GRAY))
                        .insert(Message.raw(line.newDisplay()).color(Color.GREEN))));
    }
}

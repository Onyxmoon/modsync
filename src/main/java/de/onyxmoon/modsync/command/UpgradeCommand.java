package de.onyxmoon.modsync.command;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import de.onyxmoon.modsync.ModSync;
import de.onyxmoon.modsync.api.ModProvider;
import de.onyxmoon.modsync.api.model.InstalledState;
import de.onyxmoon.modsync.api.model.ManagedMod;
import de.onyxmoon.modsync.api.model.ManagedModRegistry;
import de.onyxmoon.modsync.api.model.provider.ModVersion;
import de.onyxmoon.modsync.util.CommandMessageFormatter;
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
public class UpgradeCommand extends CommandBase {
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
    protected void executeSync(@Nonnull CommandContext commandContext) {
        if (!PermissionHelper.checkAdminPermission(commandContext)) {
            return;
        }

        CommandSender sender = commandContext.sender();
        ManagedModRegistry registry = modSync.getManagedModStorage().getRegistry();

        if (registry.isEmpty()) {
            sender.sendMessage(Message.raw("No mods in list.").color(Color.RED));
            return;
        }

        String target = CommandUtils.stripQuotes(commandContext.get(targetArg));

        if (target == null || target.isEmpty()) {
            upgradeAllMods(sender, registry);
        } else {
            upgradeSpecificMod(sender, registry, target);
        }
    }

    private void showHelp(CommandSender sender, ManagedModRegistry registry) {
        sender.sendMessage(Message.raw("Usage: ").color(Color.CYAN)
                .insert(Message.raw("/modsync upgrade <name|slug|identifier>").color(Color.WHITE)));
        sender.sendMessage(Message.raw("       ").color(Color.CYAN)
                .insert(Message.raw("/modsync upgrade all").color(Color.WHITE))
                .insert(Message.raw(" to upgrade all").color(Color.GRAY)));
        sender.sendMessage(Message.raw("Tip: ").color(Color.GRAY)
                .insert(Message.raw("Use quotes for names with spaces: ").color(Color.GRAY))
                .insert(Message.raw("\"My Mod\"").color(Color.YELLOW)));
        sender.sendMessage(Message.raw(""));

        // Show installed mods
        List<ManagedMod> installed = registry.getInstalled();
        if (installed.isEmpty()) {
            sender.sendMessage(Message.raw("No installed mods to upgrade.").color(Color.YELLOW));
        } else {
            sender.sendMessage(Message.raw("=== Installed (" + installed.size() + ") ===").color(Color.CYAN));
            for (ManagedMod mod : installed) {
                sender.sendMessage(CommandUtils.formatModLine(mod));
            }
        }
    }

    private void upgradeSpecificMod(CommandSender sender, ManagedModRegistry registry, String target) {
        SelectionResult result = ModSelector.findByNameOrSlugOrIdentifier(registry, target);

        switch (result) {
            case SelectionResult.Found found -> {
                ManagedMod mod = found.mod();
                if (!mod.isInstalled()) {
                    sender.sendMessage(Message.raw("Mod is not installed: " + mod.getName()).color(Color.RED));
                    sender.sendMessage(Message.raw("Use ").color(Color.GRAY)
                            .insert(Message.raw("/modsync install " + target).color(Color.WHITE))
                            .insert(Message.raw(" to install it first.").color(Color.GRAY)));
                    return;
                }
                sender.sendMessage(Message.raw("Checking for update: " + mod.getName() + "...").color(Color.YELLOW));
                upgradeMod(mod)
                        .thenAccept(upgradeResult -> {
                            switch (upgradeResult) {
                                case UpgradeResult.Upgraded u -> {
                                    CommandMessageFormatter.sendModStatusWithVersion(sender, mod, u.oldVersion(), u.newVersion(), "UPGRADED", Color.GREEN);
                                    sender.sendMessage(Message.raw("Server restart required to load the new version.").color(Color.CYAN));
                                }
                                case UpgradeResult.UpToDate ignored ->
                                    CommandMessageFormatter.sendModStatus(sender, mod, "UP TO DATE", Color.GREEN);
                                case UpgradeResult.Skipped ignored -> {
                                    CommandMessageFormatter.sendModStatus(sender, mod, "SKIPPED", Color.YELLOW);
                                    CommandMessageFormatter.sendDetailLine(sender, "Download URL not available", Color.GRAY);
                                }
                            }
                        })
                        .exceptionally(ex -> {
                            CommandMessageFormatter.sendModStatus(sender, mod, "FAILED", Color.RED);
                            CommandMessageFormatter.sendDetailLine(sender, CommandUtils.extractErrorMessage(ex), Color.RED);
                            return null;
                        });
            }
            case SelectionResult.NotFound notFound -> {
                sender.sendMessage(Message.raw("Mod not found in list: " + notFound.query()).color(Color.RED));
                sender.sendMessage(Message.raw("     "));
                showHelp(sender, registry);
            }
            case SelectionResult.InvalidIndex ignored ->
                sender.sendMessage(Message.raw("Use name, slug, or identifier to upgrade mods.").color(Color.RED));
            case SelectionResult.EmptyRegistry ignored ->
                sender.sendMessage(Message.raw("No mods in list.").color(Color.RED));
        }
    }

    private void upgradeAllMods(CommandSender sender, ManagedModRegistry registry) {
        List<ManagedMod> installedMods = registry.getInstalled();

        if (installedMods.isEmpty()) {
            sender.sendMessage(Message.raw("No installed mods to upgrade.").color(Color.YELLOW));
            return;
        }

        sender.sendMessage(Message.raw("Checking " + installedMods.size() + " mod(s) for updates...").color(Color.YELLOW));

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
                                    CommandMessageFormatter.sendModStatusWithVersion(sender, mod, u.oldVersion(), u.newVersion(), "UPGRADED", Color.GREEN);
                                }
                                case UpgradeResult.UpToDate ignored -> upToDate.incrementAndGet();
                                case UpgradeResult.Skipped ignored -> {
                                    skipped.incrementAndGet();
                                    CommandMessageFormatter.sendModStatus(sender, mod, "SKIPPED", Color.YELLOW);
                                    CommandMessageFormatter.sendDetailLine(sender, "Download URL not available", Color.GRAY);
                                }
                            }
                        })
                        .exceptionally(ex -> {
                            failed.incrementAndGet();
                            CommandMessageFormatter.sendModStatus(sender, mod, "FAILED", Color.RED);
                            CommandMessageFormatter.sendDetailLine(sender, CommandUtils.extractErrorMessage(ex), Color.RED);
                            return null;
                        }))
                .toArray(CompletableFuture[]::new);

        CompletableFuture.allOf(futures)
                .thenRun(() -> {
                    sender.sendMessage(Message.raw("=== Upgrade Complete ===").color(Color.CYAN));

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

                    sender.sendMessage(summary);

                    if (upgraded.get() > 0) {
                        sender.sendMessage(Message.raw("Server restart required to load updated mods.").color(Color.CYAN));
                    }
                    if (failed.get() > 0) {
                        sender.sendMessage(Message.raw("There were errors while deleting old mods. ").color(Color.RED)
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

        ModProvider provider = modSync.getProviderRegistry().getProvider(mod.getSource());
        String apiKey = modSync.getConfigStorage().getConfig().getApiKey(mod.getSource());

        if (provider.requiresApiKey() && apiKey == null) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException("No API key set for " + provider.getDisplayName())
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
}

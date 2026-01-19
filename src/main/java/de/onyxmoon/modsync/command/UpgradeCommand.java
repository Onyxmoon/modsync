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
import de.onyxmoon.modsync.api.model.InstalledMod;
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

        ManagedModList modList = plugin.getManagedModListStorage().getModList();

        if (modList.isEmpty()) {
            playerRef.sendMessage(Message.raw("No mods in list.").color("red"));
            return;
        }

        if (!name.isEmpty()) {
            upgradeSpecificMod(playerRef, modList, name);
        } else {
            upgradeAllMods(playerRef, modList);
        }
    }

    private void upgradeSpecificMod(PlayerRef playerRef, ManagedModList modList, String nameOrSlug) {
        Optional<ManagedModEntry> entryOpt = modList.findByName(nameOrSlug);
        if (entryOpt.isEmpty()) {
            entryOpt = modList.findBySlug(nameOrSlug);
        }

        // Try by identifier (group:name) if contains colon
        if (entryOpt.isEmpty() && nameOrSlug.contains(":")) {
            Optional<InstalledMod> installedOpt = plugin.getInstalledModStorage().findByIdentifier(nameOrSlug);
            if (installedOpt.isPresent()) {
                entryOpt = modList.findBySourceId(installedOpt.get().getSourceId());
            }
        }

        if (entryOpt.isEmpty()) {
            playerRef.sendMessage(Message.raw("Mod not found in list: " + nameOrSlug).color("red"));
            return;
        }

        ManagedModEntry entry = entryOpt.get();

        Optional<InstalledMod> installedOpt = plugin.getInstalledModStorage()
                .getRegistry()
                .findBySourceId(entry.getSource(), entry.getModId());

        if (installedOpt.isEmpty()) {
            playerRef.sendMessage(Message.raw("Mod is not installed: " + entry.getName()).color("red"));
            playerRef.sendMessage(Message.raw("Use ").color("gray")
                    .insert(Message.raw("/modsync install " + nameOrSlug).color("white"))
                    .insert(Message.raw(" to install it first.").color("gray")));
            return;
        }

        playerRef.sendMessage(Message.raw("Checking for update: " + entry.getName() + "...").color("yellow"));

        upgradeMod(entry, installedOpt.get())
                .thenAccept(result -> {
                    if (result == UpgradeResult.UPGRADED) {
                        playerRef.sendMessage(Message.raw("Upgraded: " + entry.getName()).color("green"));
                        playerRef.sendMessage(Message.raw("Server restart required to load the new version.").color("gold"));
                    } else if (result == UpgradeResult.UP_TO_DATE) {
                        playerRef.sendMessage(Message.raw("Already up to date: " + entry.getName()).color("green"));
                    } else {
                        playerRef.sendMessage(Message.raw("Skipped: " + entry.getName() + " - Download URL not available").color("yellow"));
                    }
                })
                .exceptionally(ex -> {
                    String errorMsg = extractErrorMessage(ex);
                    playerRef.sendMessage(Message.raw("Failed to upgrade " + entry.getName() + ": " + errorMsg).color("red"));
                    return null;
                });
    }

    private void upgradeAllMods(PlayerRef playerRef, ManagedModList modList) {
        // Get all installed mods
        List<ManagedModEntry> installedEntries = modList.getMods().stream()
                .filter(entry -> plugin.getInstalledModStorage()
                        .getRegistry()
                        .findBySourceId(entry.getSource(), entry.getModId())
                        .isPresent())
                .toList();

        if (installedEntries.isEmpty()) {
            playerRef.sendMessage(Message.raw("No installed mods to upgrade.").color("yellow"));
            return;
        }

        playerRef.sendMessage(Message.raw("Checking " + installedEntries.size() + " mod(s) for updates...").color("yellow"));

        AtomicInteger upgraded = new AtomicInteger(0);
        AtomicInteger upToDate = new AtomicInteger(0);
        AtomicInteger skipped = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);

        CompletableFuture<?>[] futures = installedEntries.stream()
                .map(entry -> {
                    Optional<InstalledMod> installedOpt = plugin.getInstalledModStorage()
                            .getRegistry()
                            .findBySourceId(entry.getSource(), entry.getModId());

                    return upgradeMod(entry, installedOpt.get())
                            .thenAccept(result -> {
                                switch (result) {
                                    case UPGRADED:
                                        upgraded.incrementAndGet();
                                        playerRef.sendMessage(Message.raw("  Upgraded: " + entry.getName()).color("green"));
                                        break;
                                    case UP_TO_DATE:
                                        upToDate.incrementAndGet();
                                        break;
                                    case SKIPPED:
                                        skipped.incrementAndGet();
                                        playerRef.sendMessage(Message.raw("  Skipped: " + entry.getName() + " - Download URL not available").color("yellow"));
                                        break;
                                }
                            })
                            .exceptionally(ex -> {
                                failed.incrementAndGet();
                                String errorMsg = extractErrorMessage(ex);
                                playerRef.sendMessage(Message.raw("  Failed: " + entry.getName() + " - " + errorMsg).color("red"));
                                return null;
                            });
                })
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

    private CompletableFuture<UpgradeResult> upgradeMod(ManagedModEntry entry, InstalledMod installed) {
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
                    ModVersion latestVersion = modEntry.getLatestVersion();

                    if (latestVersion == null) {
                        return CompletableFuture.failedFuture(
                                new IllegalStateException("No version available for " + entry.getName())
                        );
                    }

                    // Check if update is needed
                    if (latestVersion.getVersionId().equals(installed.getInstalledVersionId())) {
                        return CompletableFuture.completedFuture(UpgradeResult.UP_TO_DATE);
                    }

                    // Check download URL
                    if (latestVersion.getDownloadUrl() == null || latestVersion.getDownloadUrl().isBlank()) {
                        return CompletableFuture.completedFuture(UpgradeResult.SKIPPED);
                    }

                    // Delete old version and install new
                    return plugin.getDownloadService().deleteMod(installed)
                            .thenCompose(v -> plugin.getDownloadService().downloadAndInstall(entry, latestVersion))
                            .thenApply(newInstalled -> UpgradeResult.UPGRADED);
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
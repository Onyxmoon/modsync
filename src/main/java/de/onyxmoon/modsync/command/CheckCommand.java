package de.onyxmoon.modsync.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
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
import de.onyxmoon.modsync.util.PermissionHelper;
import de.onyxmoon.modsync.util.VersionSelector;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Command: /modsync check
 * Checks all managed mods for available updates.
 */
public class CheckCommand extends AbstractPlayerCommand {
    private final ModSync modSync;

    public CheckCommand(ModSync modSync) {
        super("check", "Check for mod updates");
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
            playerRef.sendMessage(Message.raw("No mods in list to check.").color(Color.red));
            return;
        }

        // Only check installed mods
        List<ManagedMod> installedMods = registry.getInstalled();

        if (installedMods.isEmpty()) {
            playerRef.sendMessage(Message.raw("No installed mods to check.").color(Color.YELLOW));
            return;
        }

        playerRef.sendMessage(Message.raw("Checking " + installedMods.size() + " mod(s) for updates...").color(Color.YELLOW));

        AtomicInteger updatesAvailable = new AtomicInteger(0);
        AtomicInteger upToDate = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);

        CompletableFuture<?>[] futures = installedMods.stream()
                .map(mod -> checkForUpdate(mod)
                        .thenAccept(result -> {
                            if (result.hasUpdate()) {
                                updatesAvailable.incrementAndGet();
                                playerRef.sendMessage(Message.raw("  ").insert(CommandUtils.formatModLine(mod)).color(Color.YELLOW)
                                        .insert(Message.raw(": ").color(Color.gray))
                                        .insert(Message.raw(result.installedVersion()).color(Color.red))
                                        .insert(Message.raw(" -> ").color(Color.gray))
                                        .insert(Message.raw(result.latestVersion()).color(Color.green)));
                            } else {
                                upToDate.incrementAndGet();
                            }
                        })
                        .exceptionally(ex -> {
                            failed.incrementAndGet();
                            return null;
                        }))
                .toArray(CompletableFuture[]::new);

        CompletableFuture.allOf(futures)
                .thenRun(() -> {
                    playerRef.sendMessage(Message.raw("=== Update Check Complete ===").color(Color.CYAN));
                    playerRef.sendMessage(Message.raw("Updates available: ").color(Color.gray)
                            .insert(Message.raw(String.valueOf(updatesAvailable.get())).color(Color.YELLOW))
                            .insert(Message.raw(" | Up to date: ").color(Color.gray))
                            .insert(Message.raw(String.valueOf(upToDate.get())).color(Color.green))
                            .insert(Message.raw(" | Failed: ").color(Color.gray))
                            .insert(Message.raw(String.valueOf(failed.get())).color(Color.red)));

                    if (updatesAvailable.get() > 0) {
                        playerRef.sendMessage(Message.raw("Use ").color(Color.gray)
                                .insert(Message.raw("/modsync upgrade").color(Color.white))
                                .insert(Message.raw(" to update.").color(Color.gray)));
                    }
                });
    }

    private CompletableFuture<CheckResult> checkForUpdate(ManagedMod mod) {
        if (!mod.isInstalled()) {
            return CompletableFuture.completedFuture(CheckResult.upToDate("", ""));
        }

        InstalledState installedState = mod.getInstalledState().orElseThrow();

        if (!modSync.getProviderRegistry().hasProvider(mod.getSource())) {
            return CompletableFuture.completedFuture(CheckResult.upToDate(installedState.getInstalledVersionNumber(), ""));
        }

        ModListProvider provider = modSync.getProviderRegistry().getProvider(mod.getSource());
        String apiKey = modSync.getConfigStorage().getConfig().getApiKey(mod.getSource());

        if (provider.requiresApiKey() && apiKey == null) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException("No API key for " + mod.getSource().getDisplayName())
            );
        }

        return provider.fetchMod(apiKey, mod.getModId())
                .thenApply(modEntry -> {
                    ModVersion latestVersion = VersionSelector.selectVersion(
                            mod, modEntry, modSync.getConfigStorage().getConfig());
                    if (latestVersion == null) {
                        return CheckResult.upToDate(installedState.getInstalledVersionNumber(), "");
                    }

                    String latestVersionId = latestVersion.getVersionId();
                    String installedVersionId = installedState.getInstalledVersionId();

                    boolean hasUpdate = !latestVersionId.equals(installedVersionId);
                    return new CheckResult(
                            hasUpdate,
                            installedState.getInstalledVersionNumber(),
                            latestVersion.getVersionNumber()
                    );
                });
    }

    private record CheckResult(boolean hasUpdate, String installedVersion, String latestVersion) {
        static CheckResult upToDate(String installedVersion, String latestVersion) {
            return new CheckResult(false, installedVersion, latestVersion);
        }
    }
}
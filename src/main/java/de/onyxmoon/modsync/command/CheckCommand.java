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
import de.onyxmoon.modsync.util.PermissionHelper;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Command: /modsync check
 * Checks all managed mods for available updates.
 */
public class CheckCommand extends AbstractPlayerCommand {
    private final ModSync plugin;

    public CheckCommand(ModSync plugin) {
        super("check", "Check for mod updates");
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
            playerRef.sendMessage(Message.raw("No mods in list to check.").color("red"));
            return;
        }

        // Only check installed mods
        List<ManagedMod> installedMods = registry.getInstalled();

        if (installedMods.isEmpty()) {
            playerRef.sendMessage(Message.raw("No installed mods to check.").color("yellow"));
            return;
        }

        playerRef.sendMessage(Message.raw("Checking " + installedMods.size() + " mod(s) for updates...").color("yellow"));

        AtomicInteger updatesAvailable = new AtomicInteger(0);
        AtomicInteger upToDate = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);

        CompletableFuture<?>[] futures = installedMods.stream()
                .map(mod -> checkForUpdate(mod)
                        .thenAccept(result -> {
                            if (result.hasUpdate()) {
                                updatesAvailable.incrementAndGet();
                                playerRef.sendMessage(Message.raw("  " + mod.getName()).color("yellow")
                                        .insert(Message.raw(": ").color("gray"))
                                        .insert(Message.raw(result.installedVersion()).color("red"))
                                        .insert(Message.raw(" -> ").color("gray"))
                                        .insert(Message.raw(result.latestVersion()).color("green")));
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
                    playerRef.sendMessage(Message.raw("=== Update Check Complete ===").color("gold"));
                    playerRef.sendMessage(Message.raw("Updates available: ").color("gray")
                            .insert(Message.raw(String.valueOf(updatesAvailable.get())).color("yellow"))
                            .insert(Message.raw(" | Up to date: ").color("gray"))
                            .insert(Message.raw(String.valueOf(upToDate.get())).color("green"))
                            .insert(Message.raw(" | Failed: ").color("gray"))
                            .insert(Message.raw(String.valueOf(failed.get())).color("red")));

                    if (updatesAvailable.get() > 0) {
                        playerRef.sendMessage(Message.raw("Use ").color("gray")
                                .insert(Message.raw("/modsync upgrade").color("white"))
                                .insert(Message.raw(" to update.").color("gray")));
                    }
                });
    }

    private CompletableFuture<CheckResult> checkForUpdate(ManagedMod mod) {
        if (!mod.isInstalled()) {
            return CompletableFuture.completedFuture(CheckResult.upToDate("", ""));
        }

        InstalledState installedState = mod.getInstalledState().orElseThrow();

        if (!plugin.getProviderRegistry().hasProvider(mod.getSource())) {
            return CompletableFuture.completedFuture(CheckResult.upToDate(installedState.getInstalledVersionNumber(), ""));
        }

        ModListProvider provider = plugin.getProviderRegistry().getProvider(mod.getSource());
        String apiKey = plugin.getConfigStorage().getConfig().getApiKey(mod.getSource());

        if (provider.requiresApiKey() && apiKey == null) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException("No API key for " + mod.getSource().getDisplayName())
            );
        }

        return provider.fetchMod(apiKey, mod.getModId())
                .thenApply(modEntry -> {
                    ModVersion latestVersion = modEntry.getLatestVersion();
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
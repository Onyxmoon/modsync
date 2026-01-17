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
import de.onyxmoon.modsync.api.model.InstalledMod;
import de.onyxmoon.modsync.api.model.ManagedModEntry;
import de.onyxmoon.modsync.api.model.ManagedModList;
import de.onyxmoon.modsync.util.PermissionHelper;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;
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

        ManagedModList modList = plugin.getManagedModListStorage().getModList();

        if (modList.isEmpty()) {
            playerRef.sendMessage(Message.raw("No mods in list to check.").color("red"));
            return;
        }

        // Only check installed mods
        List<ManagedModEntry> installedEntries = modList.getMods().stream()
                .filter(entry -> plugin.getInstalledModStorage()
                        .getRegistry()
                        .findBySourceId(entry.getSource(), entry.getModId())
                        .isPresent())
                .toList();

        if (installedEntries.isEmpty()) {
            playerRef.sendMessage(Message.raw("No installed mods to check.").color("yellow"));
            return;
        }

        playerRef.sendMessage(Message.raw("Checking " + installedEntries.size() + " mod(s) for updates...").color("yellow"));

        AtomicInteger updatesAvailable = new AtomicInteger(0);
        AtomicInteger upToDate = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);

        CompletableFuture<?>[] futures = installedEntries.stream()
                .map(entry -> checkForUpdate(entry)
                        .thenAccept(hasUpdate -> {
                            if (hasUpdate) {
                                updatesAvailable.incrementAndGet();
                                playerRef.sendMessage(Message.raw("  Update available: " + entry.getName()).color("yellow"));
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

    private CompletableFuture<Boolean> checkForUpdate(ManagedModEntry entry) {
        Optional<InstalledMod> installedOpt = plugin.getInstalledModStorage()
                .getRegistry()
                .findBySourceId(entry.getSource(), entry.getModId());

        if (installedOpt.isEmpty()) {
            return CompletableFuture.completedFuture(false);
        }

        InstalledMod installed = installedOpt.get();

        if (!plugin.getProviderRegistry().hasProvider(entry.getSource())) {
            return CompletableFuture.completedFuture(false);
        }

        ModListProvider provider = plugin.getProviderRegistry().getProvider(entry.getSource());
        String apiKey = plugin.getConfigStorage().getConfig().getApiKey(entry.getSource());

        if (provider.requiresApiKey() && apiKey == null) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException("No API key for " + entry.getSource().getDisplayName())
            );
        }

        return provider.fetchMod(apiKey, entry.getModId())
                .thenApply(modEntry -> {
                    if (modEntry.getLatestVersion() == null) {
                        return false;
                    }

                    String latestVersionId = modEntry.getLatestVersion().getVersionId();
                    String installedVersionId = installed.getInstalledVersionId();

                    // Compare version IDs
                    return !latestVersionId.equals(installedVersionId);
                });
    }
}

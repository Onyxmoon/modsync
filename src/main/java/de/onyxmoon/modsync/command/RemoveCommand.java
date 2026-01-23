package de.onyxmoon.modsync.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.onyxmoon.modsync.ModSync;
import de.onyxmoon.modsync.api.model.ManagedMod;
import de.onyxmoon.modsync.api.model.ManagedModRegistry;
import de.onyxmoon.modsync.util.CommandUtils;
import de.onyxmoon.modsync.util.ModSelector;
import de.onyxmoon.modsync.util.ModSelector.SelectionResult;
import de.onyxmoon.modsync.util.PermissionHelper;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Command: /modsync remove <target>
 * Removes mods from the managed list.
 *
 * Usage:
 * - /modsync remove --all           - Removes all mods
 * - /modsync remove [name]          - Removes mod by name
 * - /modsync remove [slug]          - Removes mod by slug
 * - /modsync remove [group:name]    - Removes mod by identifier
 */
public class RemoveCommand extends AbstractPlayerCommand {
    private final ModSync modSync;
    private final RequiredArg<String> targetArg = this.withRequiredArg(
            "target",
            "all | name | slug | identifier",
            ArgTypes.STRING
    );

    public RemoveCommand(ModSync modSync) {
        super("remove", "Remove mod(s) from list");
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
            playerRef.sendMessage(Message.raw("No mods in list.").color(Color.YELLOW));
            return;
        }

        String target = CommandUtils.stripQuotes(commandContext.get(targetArg));

        if (target == null || target.isEmpty()) {
            showHelp(playerRef, registry);
            return;
        }

        if (target.equals("all")) {
            removeAllMods(playerRef, registry);
            return;
        }

        // Use ModSelector for unified lookup (name, slug, identifier)
        SelectionResult result = ModSelector.findByNameOrSlugOrIdentifier(registry, target);
        handleSelectionResult(playerRef, result);
    }

    private void handleSelectionResult(PlayerRef playerRef, SelectionResult result) {
        switch (result) {
            case SelectionResult.Found found -> removeMod(playerRef, found.mod());
            case SelectionResult.NotFound notFound ->
                playerRef.sendMessage(Message.raw("Mod not found: " + notFound.query()).color(Color.RED));
            case SelectionResult.InvalidIndex invalid ->
                playerRef.sendMessage(Message.raw("Use name, slug, or identifier to remove mods.").color(Color.RED));
            case SelectionResult.EmptyRegistry ignored ->
                playerRef.sendMessage(Message.raw("No mods in list.").color(Color.YELLOW));
        }
    }

    private void showHelp(PlayerRef playerRef, ManagedModRegistry registry) {
        playerRef.sendMessage(Message.raw("Usage: ").color(Color.CYAN)
                .insert(Message.raw("/modsync remove <name|slug|identifier>").color(Color.WHITE)));
        playerRef.sendMessage(Message.raw("       ").color(Color.CYAN)
                .insert(Message.raw("/modsync remove all").color(Color.WHITE))
                .insert(Message.raw(" to remove all").color(Color.GRAY)));
        playerRef.sendMessage(Message.raw("Tip: ").color(Color.GRAY)
                .insert(Message.raw("Use quotes for names with spaces: ").color(Color.GRAY))
                .insert(Message.raw("\"My Mod\"").color(Color.YELLOW)));
        playerRef.sendMessage(Message.raw(""));

        // Show current mods (like ListCommand)
        List<ManagedMod> mods = registry.getAll();
        playerRef.sendMessage(Message.raw("=== Managed Mods (" + mods.size() + ") ===").color(Color.CYAN));

        for (ManagedMod mod : mods) {
            playerRef.sendMessage(CommandUtils.formatModLineWithStatus(mod));
        }
    }

    private void removeAllMods(PlayerRef playerRef, ManagedModRegistry registry) {
        List<ManagedMod> mods = registry.getAll();
        int total = mods.size();

        playerRef.sendMessage(Message.raw("Removing " + total + " mod(s)...").color(Color.YELLOW));

        AtomicInteger removed = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);
        AtomicInteger pendingRestart = new AtomicInteger(0);

        CompletableFuture<?>[] futures = mods.stream()
                .map(mod -> removeModAsync(mod)
                        .thenAccept(deletedImmediately -> {
                            removed.incrementAndGet();
                            if (!deletedImmediately) {
                                pendingRestart.incrementAndGet();
                            }
                        })
                        .exceptionally(ex -> {
                            failed.incrementAndGet();
                            return null;
                        }))
                .toArray(CompletableFuture[]::new);

        CompletableFuture.allOf(futures)
                .thenRun(() -> {
                    playerRef.sendMessage(Message.raw("Removed: " + removed.get()).color(Color.GREEN));
                    playerRef.sendMessage(Message.raw("Removed: " + removed.get()).color(Color.GREEN)
                            .insert(Message.raw(" | Failed: " + failed.get()).color(failed.get() > 0 ? Color.RED : Color.GRAY)));
                    if (pendingRestart.get() > 0) {
                        playerRef.sendMessage(Message.raw(pendingRestart.get() + " file(s) locked. Restart server to complete deletion.").color(Color.YELLOW));
                    }
                    if (removed.get() > 0) {
                        playerRef.sendMessage(Message.raw("Server restart required to fully unload mods.").color(Color.CYAN));
                    }
                });
    }

    private void removeMod(PlayerRef playerRef, ManagedMod mod) {
        if (mod.isInstalled()) {
            playerRef.sendMessage(Message.raw("Removing installed file...").color(Color.YELLOW));

            modSync.getDownloadService().deleteMod(mod)
                    .thenAccept(deletedImmediately -> {
                        modSync.getManagedModStorage().removeMod(mod.getSourceId());
                        if (deletedImmediately) {
                            playerRef.sendMessage(Message.raw("Removed: ").insert(CommandUtils.formatModLine(mod)).insert(" (file deleted)").color(Color.GREEN));
                            playerRef.sendMessage(Message.raw("Server restart required to fully unload the mod.").color(Color.CYAN));
                        } else {
                            playerRef.sendMessage(Message.raw("Removed: ").insert(CommandUtils.formatModLine(mod)).insert(" (file deleted)").color(Color.GREEN));
                            playerRef.sendMessage(Message.raw("File is locked. Restart server to complete deletion.").color(Color.YELLOW));
                        }
                    })
                    .exceptionally(ex -> {
                        playerRef.sendMessage(Message.raw("Failed to delete file: " + ex.getMessage()).color(Color.RED));
                        modSync.getManagedModStorage().removeMod(mod.getSourceId());
                        playerRef.sendMessage(Message.raw("Removed from list (file may remain)").color(Color.YELLOW));
                        return null;
                    });
        } else {
            modSync.getManagedModStorage().removeMod(mod.getSourceId());
            playerRef.sendMessage(Message.raw("Removed: ").insert(CommandUtils.formatModLine(mod)).color(Color.GREEN));
        }
    }

    private CompletableFuture<Boolean> removeModAsync(ManagedMod mod) {
        if (mod.isInstalled()) {
            return modSync.getDownloadService().deleteMod(mod)
                    .thenApply(deletedImmediately -> {
                        modSync.getManagedModStorage().removeMod(mod.getSourceId());
                        return deletedImmediately;
                    });
        } else {
            modSync.getManagedModStorage().removeMod(mod.getSourceId());
            return CompletableFuture.completedFuture(true);
        }
    }
}
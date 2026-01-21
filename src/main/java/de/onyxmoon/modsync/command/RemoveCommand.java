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
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Command: /modsync remove <target>
 * Removes mods from the managed list.
 *
 * Usage:
 * - /modsync remove --all           - Removes all mods
 * - /modsync remove <name>          - Removes mod by name
 * - /modsync remove <slug>          - Removes mod by slug
 * - /modsync remove <group:name>    - Removes mod by identifier
 */
public class RemoveCommand extends AbstractPlayerCommand {
    private final ModSync plugin;
    private final RequiredArg<String> targetArg = this.withRequiredArg(
            "target",
            "all | name | slug | identifier",
            ArgTypes.STRING
    );

    public RemoveCommand(ModSync plugin) {
        super("remove", "Remove mod(s) from list");
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
            playerRef.sendMessage(Message.raw("No mods in list.").color("yellow"));
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
                playerRef.sendMessage(Message.raw("Mod not found: " + notFound.query()).color("red"));
            case SelectionResult.InvalidIndex invalid ->
                playerRef.sendMessage(Message.raw("Use name, slug, or identifier to remove mods.").color("red"));
            case SelectionResult.EmptyRegistry ignored ->
                playerRef.sendMessage(Message.raw("No mods in list.").color("yellow"));
        }
    }

    private void showHelp(PlayerRef playerRef, ManagedModRegistry registry) {
        playerRef.sendMessage(Message.raw("Usage: ").color("gold")
                .insert(Message.raw("/modsync remove <name|slug|identifier>").color("white")));
        playerRef.sendMessage(Message.raw("       ").color("gold")
                .insert(Message.raw("/modsync remove all").color("white"))
                .insert(Message.raw(" to remove all").color("gray")));
        playerRef.sendMessage(Message.raw("Tip: ").color("gray")
                .insert(Message.raw("Use quotes for names with spaces: ").color("gray"))
                .insert(Message.raw("\"My Mod\"").color("yellow")));
        playerRef.sendMessage(Message.raw(""));

        // Show current mods (like ListCommand)
        List<ManagedMod> mods = registry.getAll();
        playerRef.sendMessage(Message.raw("=== Managed Mods (" + mods.size() + ") ===").color("gold"));

        for (ManagedMod mod : mods) {
            playerRef.sendMessage(CommandUtils.formatModLineWithStatus(mod));
        }
    }

    private void removeAllMods(PlayerRef playerRef, ManagedModRegistry registry) {
        List<ManagedMod> mods = registry.getAll();
        int total = mods.size();

        playerRef.sendMessage(Message.raw("Removing " + total + " mod(s)...").color("yellow"));

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
                    playerRef.sendMessage(Message.raw("Removed: " + removed.get()).color("green")
                            .insert(Message.raw(" | Failed: " + failed.get()).color(failed.get() > 0 ? "red" : "gray")));
                    if (pendingRestart.get() > 0) {
                        playerRef.sendMessage(Message.raw(pendingRestart.get() + " file(s) locked. Restart server to complete deletion.").color("yellow"));
                    }
                    if (removed.get() > 0) {
                        playerRef.sendMessage(Message.raw("Server restart required to fully unload mods.").color("gold"));
                    }
                });
    }

    private void removeMod(PlayerRef playerRef, ManagedMod mod) {
        if (mod.isInstalled()) {
            playerRef.sendMessage(Message.raw("Removing installed file...").color("yellow"));

            plugin.getDownloadService().deleteMod(mod)
                    .thenAccept(deletedImmediately -> {
                        plugin.getManagedModStorage().removeMod(mod.getSourceId());
                        if (deletedImmediately) {
                            playerRef.sendMessage(Message.raw("Removed: ").insert(CommandUtils.formatModLine(mod)).insert(" (file deleted)").color("green"));
                            playerRef.sendMessage(Message.raw("Server restart required to fully unload the mod.").color("gold"));
                        } else {
                            playerRef.sendMessage(Message.raw("Removed: ").insert(CommandUtils.formatModLine(mod)).insert(" (file deleted)").color("green"));
                            playerRef.sendMessage(Message.raw("File is locked. Restart server to complete deletion.").color("yellow"));
                        }
                    })
                    .exceptionally(ex -> {
                        playerRef.sendMessage(Message.raw("Failed to delete file: " + ex.getMessage()).color("red"));
                        plugin.getManagedModStorage().removeMod(mod.getSourceId());
                        playerRef.sendMessage(Message.raw("Removed from list (file may remain)").color("yellow"));
                        return null;
                    });
        } else {
            plugin.getManagedModStorage().removeMod(mod.getSourceId());
            playerRef.sendMessage(Message.raw("Removed: ").insert(CommandUtils.formatModLine(mod)).color("green"));
        }
    }

    private CompletableFuture<Boolean> removeModAsync(ManagedMod mod) {
        if (mod.isInstalled()) {
            return plugin.getDownloadService().deleteMod(mod)
                    .thenApply(deletedImmediately -> {
                        plugin.getManagedModStorage().removeMod(mod.getSourceId());
                        return deletedImmediately;
                    });
        } else {
            plugin.getManagedModStorage().removeMod(mod.getSourceId());
            return CompletableFuture.completedFuture(true);
        }
    }
}
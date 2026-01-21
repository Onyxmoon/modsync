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
import de.onyxmoon.modsync.api.model.ManagedMod;
import de.onyxmoon.modsync.api.model.ManagedModRegistry;
import de.onyxmoon.modsync.util.PermissionHelper;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Command: /modsync remove [index|--all|name]
 * Removes mods from the managed list.
 *
 * Usage:
 * - /modsync remove        - Shows numbered list
 * - /modsync remove 1      - Removes mod at index 1
 * - /modsync remove --all  - Removes all mods
 * - /modsync remove name   - Removes mod by name/slug/identifier
 */
public class RemoveCommand extends AbstractPlayerCommand {
    private final ModSync plugin;
    private final OptionalArg<String> argArg = this.withOptionalArg(
            "index|--all|name",
            "Index number, --all, or mod name",
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

        String arg = commandContext.provided(argArg) ? commandContext.get(argArg) : "";

        if (arg.isEmpty()) {
            // Interactive: show numbered list
            showNumberedList(playerRef, registry);
            return;
        }

        if (arg.equals("--all")) {
            // Remove all mods
            removeAllMods(playerRef, registry);
            return;
        }

        // Try to parse as index
        try {
            int index = Integer.parseInt(arg);
            removeByIndex(playerRef, registry, index);
            return;
        } catch (NumberFormatException ignored) {
            // Not a number, try by name
        }

        // Remove by name/slug/identifier
        removeByName(playerRef, registry, arg);
    }

    private void showNumberedList(PlayerRef playerRef, ManagedModRegistry registry) {
        List<ManagedMod> mods = registry.getAll();

        playerRef.sendMessage(Message.raw("=== Select mod to remove ===").color("gold"));

        for (int i = 0; i < mods.size(); i++) {
            ManagedMod mod = mods.get(i);
            String status = mod.isInstalled() ? "[installed]" : "[not installed]";
            String statusColor = mod.isInstalled() ? "green" : "gray";

            playerRef.sendMessage(Message.raw((i + 1) + ". ").color("white")
                    .insert(Message.raw(mod.getName()).color("yellow"))
                    .insert(Message.raw(" " + status).color(statusColor)));
        }

        playerRef.sendMessage(Message.raw(""));
        playerRef.sendMessage(Message.raw("Use ").color("gray")
                .insert(Message.raw("/modsync remove <number>").color("white"))
                .insert(Message.raw(" to remove").color("gray")));
        playerRef.sendMessage(Message.raw("Use ").color("gray")
                .insert(Message.raw("/modsync remove --all").color("white"))
                .insert(Message.raw(" to remove all").color("gray")));
    }

    private void removeByIndex(PlayerRef playerRef, ManagedModRegistry registry, int index) {
        List<ManagedMod> mods = registry.getAll();

        if (index < 1 || index > mods.size()) {
            playerRef.sendMessage(Message.raw("Invalid index. Use 1-" + mods.size()).color("red"));
            return;
        }

        ManagedMod mod = mods.get(index - 1);
        removeMod(playerRef, mod);
    }

    private void removeByName(PlayerRef playerRef, ManagedModRegistry registry, String nameOrSlug) {
        // Simplified lookup - directly on registry
        Optional<ManagedMod> modOpt = registry.findByName(nameOrSlug);
        if (modOpt.isEmpty()) {
            modOpt = registry.findBySlug(nameOrSlug);
        }
        // Try by identifier (group:name) if contains colon
        if (modOpt.isEmpty() && nameOrSlug.contains(":")) {
            modOpt = registry.findByIdentifier(nameOrSlug);
        }

        if (modOpt.isEmpty()) {
            playerRef.sendMessage(Message.raw("Mod not found: " + nameOrSlug).color("red"));
            return;
        }

        removeMod(playerRef, modOpt.get());
    }

    private void removeAllMods(PlayerRef playerRef, ManagedModRegistry registry) {
        List<ManagedMod> mods = registry.getAll();
        int total = mods.size();

        playerRef.sendMessage(Message.raw("Removing " + total + " mod(s)...").color("yellow"));

        AtomicInteger removed = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);
        AtomicInteger pendingRestart = new AtomicInteger(0);

        CompletableFuture<?>[] futures = mods.stream()
                .map(mod -> removeModAsyncWithStatus(mod)
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
                });
    }

    private void removeMod(PlayerRef playerRef, ManagedMod mod) {
        if (mod.isInstalled()) {
            playerRef.sendMessage(Message.raw("Removing installed file...").color("yellow"));

            plugin.getDownloadService().deleteMod(mod)
                    .thenAccept(deletedImmediately -> {
                        plugin.getManagedModStorage().removeMod(mod.getSourceId());
                        if (deletedImmediately) {
                            playerRef.sendMessage(Message.raw("Removed: " + mod.getName() + " (file deleted)").color("green"));
                        } else {
                            playerRef.sendMessage(Message.raw("Removed: " + mod.getName()).color("green"));
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
            playerRef.sendMessage(Message.raw("Removed: " + mod.getName()).color("green"));
        }
    }

    private CompletableFuture<Boolean> removeModAsyncWithStatus(ManagedMod mod) {
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
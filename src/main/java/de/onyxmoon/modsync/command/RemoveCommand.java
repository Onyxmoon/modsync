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
 * Command: /modsync remove [index|--all|name]
 * Removes mods from the managed list.
 *
 * Usage:
 * - /modsync remove        - Shows numbered list
 * - /modsync remove 1      - Removes mod at index 1
 * - /modsync remove --all  - Removes all mods
 * - /modsync remove name   - Removes mod by name/slug
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

        ManagedModList modList = plugin.getManagedModListStorage().getModList();

        if (modList.isEmpty()) {
            playerRef.sendMessage(Message.raw("No mods in list.").color("yellow"));
            return;
        }

        String arg = commandContext.provided(argArg) ? commandContext.get(argArg) : "";

        if (arg.isEmpty()) {
            // Interactive: show numbered list
            showNumberedList(playerRef, modList);
            return;
        }

        if (arg.equals("--all")) {
            // Remove all mods
            removeAllMods(playerRef, modList);
            return;
        }

        // Try to parse as index
        try {
            int index = Integer.parseInt(arg);
            removeByIndex(playerRef, modList, index);
            return;
        } catch (NumberFormatException ignored) {
            // Not a number, try by name
        }

        // Remove by name/slug
        removeByName(playerRef, modList, arg);
    }

    private void showNumberedList(PlayerRef playerRef, ManagedModList modList) {
        List<ManagedModEntry> mods = modList.getMods();

        playerRef.sendMessage(Message.raw("=== Select mod to remove ===").color("gold"));

        for (int i = 0; i < mods.size(); i++) {
            ManagedModEntry entry = mods.get(i);
            boolean installed = plugin.getInstalledModStorage().getRegistry()
                    .findBySourceId(entry.getSource(), entry.getModId()).isPresent();

            String status = installed ? "[installed]" : "[not installed]";
            String statusColor = installed ? "green" : "gray";

            playerRef.sendMessage(Message.raw((i + 1) + ". ").color("white")
                    .insert(Message.raw(entry.getName()).color("yellow"))
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

    private void removeByIndex(PlayerRef playerRef, ManagedModList modList, int index) {
        List<ManagedModEntry> mods = modList.getMods();

        if (index < 1 || index > mods.size()) {
            playerRef.sendMessage(Message.raw("Invalid index. Use 1-" + mods.size()).color("red"));
            return;
        }

        ManagedModEntry entry = mods.get(index - 1);
        removeMod(playerRef, entry);
    }

    private void removeByName(PlayerRef playerRef, ManagedModList modList, String nameOrSlug) {
        Optional<ManagedModEntry> entryOpt = modList.findByName(nameOrSlug);
        if (entryOpt.isEmpty()) {
            entryOpt = modList.findBySlug(nameOrSlug);
        }

        if (entryOpt.isEmpty()) {
            playerRef.sendMessage(Message.raw("Mod not found: " + nameOrSlug).color("red"));
            return;
        }

        removeMod(playerRef, entryOpt.get());
    }

    private void removeAllMods(PlayerRef playerRef, ManagedModList modList) {
        List<ManagedModEntry> mods = modList.getMods();
        int total = mods.size();

        playerRef.sendMessage(Message.raw("Removing " + total + " mod(s)...").color("yellow"));

        AtomicInteger removed = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);

        CompletableFuture<?>[] futures = mods.stream()
                .map(entry -> removeModAsync(entry)
                        .thenRun(() -> removed.incrementAndGet())
                        .exceptionally(ex -> {
                            failed.incrementAndGet();
                            return null;
                        }))
                .toArray(CompletableFuture[]::new);

        CompletableFuture.allOf(futures)
                .thenRun(() -> {
                    playerRef.sendMessage(Message.raw("Removed: " + removed.get()).color("green")
                            .insert(Message.raw(" | Failed: " + failed.get()).color(failed.get() > 0 ? "red" : "gray")));
                });
    }

    private void removeMod(PlayerRef playerRef, ManagedModEntry entry) {
        Optional<InstalledMod> installedOpt = plugin.getInstalledModStorage()
                .getRegistry()
                .findBySourceId(entry.getSource(), entry.getModId());

        if (installedOpt.isPresent()) {
            InstalledMod installed = installedOpt.get();
            playerRef.sendMessage(Message.raw("Removing installed file...").color("yellow"));

            plugin.getDownloadService().deleteMod(installed)
                    .thenRun(() -> {
                        plugin.getManagedModListStorage().removeMod(entry.getSourceId());
                        playerRef.sendMessage(Message.raw("Removed: " + entry.getName() + " (file deleted)").color("green"));
                    })
                    .exceptionally(ex -> {
                        playerRef.sendMessage(Message.raw("Failed to delete file: " + ex.getMessage()).color("red"));
                        plugin.getManagedModListStorage().removeMod(entry.getSourceId());
                        playerRef.sendMessage(Message.raw("Removed from list (file may remain)").color("yellow"));
                        return null;
                    });
        } else {
            plugin.getManagedModListStorage().removeMod(entry.getSourceId());
            playerRef.sendMessage(Message.raw("Removed: " + entry.getName()).color("green"));
        }
    }

    private CompletableFuture<Void> removeModAsync(ManagedModEntry entry) {
        Optional<InstalledMod> installedOpt = plugin.getInstalledModStorage()
                .getRegistry()
                .findBySourceId(entry.getSource(), entry.getModId());

        if (installedOpt.isPresent()) {
            return plugin.getDownloadService().deleteMod(installedOpt.get())
                    .thenRun(() -> plugin.getManagedModListStorage().removeMod(entry.getSourceId()));
        } else {
            plugin.getManagedModListStorage().removeMod(entry.getSourceId());
            return CompletableFuture.completedFuture(null);
        }
    }
}
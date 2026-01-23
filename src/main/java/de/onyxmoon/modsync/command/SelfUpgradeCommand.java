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
import de.onyxmoon.modsync.service.SelfUpgradeService;
import de.onyxmoon.modsync.service.selfupgrade.model.UpgradeStatus;
import de.onyxmoon.modsync.util.PermissionHelper;

import javax.annotation.Nonnull;
import java.awt.*;

/**
 * Command: /modsync selfupgrade [check|apply]
 * Manages self-updating of the ModSync plugin.
 */
public class SelfUpgradeCommand extends AbstractPlayerCommand {
    private final ModSync modSync;
    private final RequiredArg<String> actionArg = this.withRequiredArg(
            "action",
            "check | apply",
            ArgTypes.STRING
    );

    public SelfUpgradeCommand(ModSync modSync) {
        super("selfupgrade", "Check for and apply ModSync plugin updates");
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

        String action = commandContext.get(actionArg);
        SelfUpgradeService updateService = modSync.getSelfUpdateService();

        if (action == null || action.equalsIgnoreCase("check")) {
            performCheck(playerRef, updateService);
        } else if (action.equalsIgnoreCase("apply")) {
            performUpdate(playerRef, updateService);
        } else {
            showHelp(playerRef);
        }
    }

    private void showHelp(PlayerRef playerRef) {
        playerRef.sendMessage(Message.raw("Usage: ").color(Color.CYAN)
                .insert(Message.raw("/modsync selfupdate [check|apply]").color(Color.WHITE)));
        playerRef.sendMessage(Message.raw("  check").color(Color.YELLOW)
                .insert(Message.raw(" - Check for available updates").color(Color.GRAY)));
        playerRef.sendMessage(Message.raw("  apply").color(Color.YELLOW)
                .insert(Message.raw(" - Download and install the latest version").color(Color.GRAY)));
    }

    private void performCheck(PlayerRef playerRef, SelfUpgradeService updateService) {
        playerRef.sendMessage(Message.raw("Checking for ModSync updates...").color(Color.YELLOW));

        updateService.checkForUpgrade()
                .thenAccept(result -> {
                    switch (result.status()) {
                        case UP_TO_DATE -> playerRef.sendMessage(
                                Message.raw("ModSync is up to date! ").color(Color.GREEN)
                                        .insert(Message.raw("(" + result.currentVersion() + ")").color(Color.GRAY)));

                        case UPGRADE_AVAILABLE -> {
                            playerRef.sendMessage(Message.raw("=== Update Available ===").color(Color.CYAN));
                            playerRef.sendMessage(Message.raw("Current: ").color(Color.GRAY)
                                    .insert(Message.raw(result.currentVersion().toString()).color(Color.RED)));
                            playerRef.sendMessage(Message.raw("Latest:  ").color(Color.GRAY)
                                    .insert(Message.raw(result.latestVersion().toString()).color(Color.GREEN)));

                            if (result.release() != null && result.release().getBody() != null) {
                                String notes = result.release().getBody();
                                // Truncate long release notes
                                if (notes.length() > 200) {
                                    notes = notes.substring(0, 197) + "...";
                                }
                                // Remove markdown formatting for cleaner display
                                notes = notes.replaceAll("\\*\\*", "")
                                            .replaceAll("\\*", "")
                                            .replaceAll("#+ ", "");
                                playerRef.sendMessage(Message.raw("Release Notes: ").color(Color.GRAY)
                                        .insert(Message.raw(notes).color(Color.WHITE)));
                            }

                            playerRef.sendMessage(Message.raw("Use ").color(Color.GRAY)
                                    .insert(Message.raw("/modsync selfupdate apply").color(Color.YELLOW))
                                    .insert(Message.raw(" to install.").color(Color.GRAY)));
                        }

                        case ERROR -> playerRef.sendMessage(
                                Message.raw("Update check failed: ").color(Color.RED)
                                        .insert(Message.raw(result.message()).color(Color.GRAY)));
                    }
                })
                .exceptionally(ex -> {
                    playerRef.sendMessage(Message.raw("Update check failed: " + ex.getMessage()).color(Color.RED));
                    return null;
                });
    }

    private void performUpdate(PlayerRef playerRef, SelfUpgradeService updateService) {
        playerRef.sendMessage(Message.raw("Checking for updates...").color(Color.YELLOW));

        updateService.checkForUpgrade()
                .thenCompose(checkResult -> {
                    if (checkResult.status() != UpgradeStatus.UPGRADE_AVAILABLE) {
                        if (checkResult.status() == UpgradeStatus.ERROR) {
                            playerRef.sendMessage(Message.raw("Update check failed: ").color(Color.RED)
                                    .insert(Message.raw(checkResult.message()).color(Color.GRAY)));
                        } else {
                            playerRef.sendMessage(Message.raw("ModSync is already up to date! ").color(Color.GREEN)
                                    .insert(Message.raw("(" + checkResult.currentVersion() + ")").color(Color.GRAY)));
                        }
                        return java.util.concurrent.CompletableFuture.completedFuture(null);
                    }

                    playerRef.sendMessage(Message.raw("Downloading v" + checkResult.latestVersion() + "...").color(Color.YELLOW));
                    return updateService.performUpgrade(checkResult.release());
                })
                .thenAccept(updateResult -> {
                    if (updateResult == null) return;

                    if (updateResult.success()) {
                        playerRef.sendMessage(Message.raw("Update successful!").color(Color.GREEN));
                        playerRef.sendMessage(Message.raw(updateResult.message()).color(Color.GRAY));

                        if (updateResult.restartRequired()) {
                            playerRef.sendMessage(Message.raw("Server restart required to apply the update.").color(Color.CYAN));
                        }
                    } else {
                        playerRef.sendMessage(Message.raw("Update failed: ").color(Color.RED)
                                .insert(Message.raw(updateResult.message()).color(Color.GRAY)));
                    }
                })
                .exceptionally(ex -> {
                    playerRef.sendMessage(Message.raw("Update failed: " + ex.getMessage()).color(Color.RED));
                    return null;
                });
    }
}
package de.onyxmoon.modsync.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
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

/**
 * Command: /modsync selfupgrade [check|apply]
 * Manages self-updating of the ModSync plugin.
 */
public class SelfUpgradeCommand extends AbstractPlayerCommand {
    private final ModSync plugin;
    private final RequiredArg<String> actionArg = this.withRequiredArg(
            "action",
            "check | apply",
            ArgTypes.STRING
    );

    public SelfUpgradeCommand(ModSync plugin) {
        super("selfupgrade", "Check for and apply ModSync plugin updates");
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

        String action = commandContext.get(actionArg);
        SelfUpgradeService updateService = plugin.getSelfUpdateService();

        if (action == null || action.equalsIgnoreCase("check")) {
            performCheck(playerRef, updateService);
        } else if (action.equalsIgnoreCase("apply")) {
            performUpdate(playerRef, updateService);
        } else {
            showHelp(playerRef);
        }
    }

    private void showHelp(PlayerRef playerRef) {
        playerRef.sendMessage(Message.raw("Usage: ").color("gold")
                .insert(Message.raw("/modsync selfupdate [check|apply]").color("white")));
        playerRef.sendMessage(Message.raw("  check").color("yellow")
                .insert(Message.raw(" - Check for available updates").color("gray")));
        playerRef.sendMessage(Message.raw("  apply").color("yellow")
                .insert(Message.raw(" - Download and install the latest version").color("gray")));
    }

    private void performCheck(PlayerRef playerRef, SelfUpgradeService updateService) {
        playerRef.sendMessage(Message.raw("Checking for ModSync updates...").color("yellow"));

        updateService.checkForUpgrade()
                .thenAccept(result -> {
                    switch (result.status()) {
                        case UP_TO_DATE -> playerRef.sendMessage(
                                Message.raw("ModSync is up to date! ").color("green")
                                        .insert(Message.raw("(" + result.currentVersion() + ")").color("gray")));

                        case UPGRADE_AVAILABLE -> {
                            playerRef.sendMessage(Message.raw("=== Update Available ===").color("gold"));
                            playerRef.sendMessage(Message.raw("Current: ").color("gray")
                                    .insert(Message.raw(result.currentVersion().toString()).color("red")));
                            playerRef.sendMessage(Message.raw("Latest:  ").color("gray")
                                    .insert(Message.raw(result.latestVersion().toString()).color("green")));

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
                                playerRef.sendMessage(Message.raw("Release Notes: ").color("gray")
                                        .insert(Message.raw(notes).color("white")));
                            }

                            playerRef.sendMessage(Message.raw("Use ").color("gray")
                                    .insert(Message.raw("/modsync selfupdate apply").color("yellow"))
                                    .insert(Message.raw(" to install.").color("gray")));
                        }

                        case ERROR -> playerRef.sendMessage(
                                Message.raw("Update check failed: ").color("red")
                                        .insert(Message.raw(result.message()).color("gray")));
                    }
                })
                .exceptionally(ex -> {
                    playerRef.sendMessage(Message.raw("Update check failed: " + ex.getMessage()).color("red"));
                    return null;
                });
    }

    private void performUpdate(PlayerRef playerRef, SelfUpgradeService updateService) {
        playerRef.sendMessage(Message.raw("Checking for updates...").color("yellow"));

        updateService.checkForUpgrade()
                .thenCompose(checkResult -> {
                    if (checkResult.status() != UpgradeStatus.UPGRADE_AVAILABLE) {
                        if (checkResult.status() == UpgradeStatus.ERROR) {
                            playerRef.sendMessage(Message.raw("Update check failed: ").color("red")
                                    .insert(Message.raw(checkResult.message()).color("gray")));
                        } else {
                            playerRef.sendMessage(Message.raw("ModSync is already up to date! ").color("green")
                                    .insert(Message.raw("(" + checkResult.currentVersion() + ")").color("gray")));
                        }
                        return java.util.concurrent.CompletableFuture.completedFuture(null);
                    }

                    playerRef.sendMessage(Message.raw("Downloading v" + checkResult.latestVersion() + "...").color("yellow"));
                    return updateService.performUpgrade(checkResult.release());
                })
                .thenAccept(updateResult -> {
                    if (updateResult == null) return;

                    if (updateResult.success()) {
                        playerRef.sendMessage(Message.raw("Update successful!").color("green"));
                        playerRef.sendMessage(Message.raw(updateResult.message()).color("gray"));

                        if (updateResult.restartRequired()) {
                            playerRef.sendMessage(Message.raw("Server restart required to apply the update.").color("gold"));
                        }
                    } else {
                        playerRef.sendMessage(Message.raw("Update failed: ").color("red")
                                .insert(Message.raw(updateResult.message()).color("gray")));
                    }
                })
                .exceptionally(ex -> {
                    playerRef.sendMessage(Message.raw("Update failed: " + ex.getMessage()).color("red"));
                    return null;
                });
    }
}
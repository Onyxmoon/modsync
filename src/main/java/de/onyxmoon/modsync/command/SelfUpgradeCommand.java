package de.onyxmoon.modsync.command;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
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
public class SelfUpgradeCommand extends CommandBase {
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
    protected void executeSync(@Nonnull CommandContext commandContext) {
        if (!PermissionHelper.checkAdminPermission(commandContext)) {
            return;
        }

        String action = commandContext.get(actionArg);
        SelfUpgradeService updateService = modSync.getSelfUpdateService();
        CommandSender sender = commandContext.sender();

        if (action == null || action.equalsIgnoreCase("check")) {
            performCheck(sender, updateService);
        } else if (action.equalsIgnoreCase("apply")) {
            performUpdate(sender, updateService);
        } else {
            showHelp(sender);
        }
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage(Message.raw("Usage: ").color(Color.CYAN)
                .insert(Message.raw("/modsync selfupdate [check|apply]").color(Color.WHITE)));
        sender.sendMessage(Message.raw("  check").color(Color.YELLOW)
                .insert(Message.raw(" - Check for available updates").color(Color.GRAY)));
        sender.sendMessage(Message.raw("  apply").color(Color.YELLOW)
                .insert(Message.raw(" - Download and install the latest version").color(Color.GRAY)));
    }

    private void performCheck(CommandSender sender, SelfUpgradeService updateService) {
        sender.sendMessage(Message.raw("Checking for ModSync updates...").color(Color.YELLOW));

        updateService.checkForUpgrade()
                .thenAccept(result -> {
                    switch (result.status()) {
                        case UP_TO_DATE -> sender.sendMessage(
                                Message.raw("ModSync is up to date! ").color(Color.GREEN)
                                        .insert(Message.raw("(" + result.currentVersion() + ")").color(Color.GRAY)));

                        case UPGRADE_AVAILABLE -> {
                            sender.sendMessage(Message.raw("=== Update Available ===").color(Color.CYAN));
                            sender.sendMessage(Message.raw("Current: ").color(Color.GRAY)
                                    .insert(Message.raw(result.currentVersion().toString()).color(Color.RED)));
                            sender.sendMessage(Message.raw("Latest:  ").color(Color.GRAY)
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
                                sender.sendMessage(Message.raw("Release Notes: ").color(Color.GRAY)
                                        .insert(Message.raw(notes).color(Color.WHITE)));
                            }

                            sender.sendMessage(Message.raw("Use ").color(Color.GRAY)
                                    .insert(Message.raw("/modsync selfupdate apply").color(Color.YELLOW))
                                    .insert(Message.raw(" to install.").color(Color.GRAY)));
                        }

                        case ERROR -> sender.sendMessage(
                                Message.raw("Update check failed: ").color(Color.RED)
                                        .insert(Message.raw(result.message()).color(Color.GRAY)));
                    }
                })
                .exceptionally(ex -> {
                    sender.sendMessage(Message.raw("Update check failed: " + ex.getMessage()).color(Color.RED));
                    return null;
                });
    }

    private void performUpdate(CommandSender sender, SelfUpgradeService updateService) {
        sender.sendMessage(Message.raw("Checking for updates...").color(Color.YELLOW));

        updateService.checkForUpgrade()
                .thenCompose(checkResult -> {
                    if (checkResult.status() != UpgradeStatus.UPGRADE_AVAILABLE) {
                        if (checkResult.status() == UpgradeStatus.ERROR) {
                            sender.sendMessage(Message.raw("Update check failed: ").color(Color.RED)
                                    .insert(Message.raw(checkResult.message()).color(Color.GRAY)));
                        } else {
                            sender.sendMessage(Message.raw("ModSync is already up to date! ").color(Color.GREEN)
                                    .insert(Message.raw("(" + checkResult.currentVersion() + ")").color(Color.GRAY)));
                        }
                        return java.util.concurrent.CompletableFuture.completedFuture(null);
                    }

                    sender.sendMessage(Message.raw("Downloading v" + checkResult.latestVersion() + "...").color(Color.YELLOW));
                    return updateService.performUpgrade(checkResult.release());
                })
                .thenAccept(updateResult -> {
                    if (updateResult == null) return;

                    if (updateResult.success()) {
                        sender.sendMessage(Message.raw("Update successful!").color(Color.GREEN));
                        sender.sendMessage(Message.raw(updateResult.message()).color(Color.GRAY));

                        if (updateResult.restartRequired()) {
                            sender.sendMessage(Message.raw("Server restart required to apply the update.").color(Color.CYAN));
                        }
                    } else {
                        sender.sendMessage(Message.raw("Update failed: ").color(Color.RED)
                                .insert(Message.raw(updateResult.message()).color(Color.GRAY)));
                    }
                })
                .exceptionally(ex -> {
                    sender.sendMessage(Message.raw("Update failed: " + ex.getMessage()).color(Color.RED));
                    return null;
                });
    }
}

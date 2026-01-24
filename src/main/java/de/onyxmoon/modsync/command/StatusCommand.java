package de.onyxmoon.modsync.command;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import de.onyxmoon.modsync.BuildInfo;
import de.onyxmoon.modsync.ModSync;
import de.onyxmoon.modsync.api.model.provider.ModList;
import de.onyxmoon.modsync.storage.model.PluginConfig;
import de.onyxmoon.modsync.util.PermissionHelper;

import javax.annotation.Nonnull;
import java.awt.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Command: /modsync status
 * Shows current status and configuration.
 */
public class StatusCommand extends CommandBase {
    private static final DateTimeFormatter FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private final ModSync modSync;

    public StatusCommand(ModSync modSync) {
        super("status", "View ModSync status");
        this.modSync = modSync;
    }

    @Override
    protected void executeSync(@Nonnull CommandContext commandContext) {
        if (!PermissionHelper.checkAdminPermission(commandContext)) {
            return;
        }

        CommandSender sender = commandContext.sender();
        PluginConfig config = modSync.getConfigStorage().getConfig();

        sender.sendMessage(Message.raw("=== ModSync Status ===").color(Color.CYAN));
        sender.sendMessage(Message.raw("Version: ").color(Color.GRAY)
                .insert(Message.raw("v" + BuildInfo.VERSION).color(Color.WHITE)));
        sender.sendMessage(Message.raw("Current Source: ").color(Color.GRAY)
                .insert(Message.raw(config.getCurrentSource().getDisplayName()).color(Color.WHITE)));
        sender.sendMessage(Message.raw("Update Mode: ").color(Color.GRAY)
                .insert(Message.raw(config.getUpdateMode().toString()).color(Color.WHITE)));

        if (config.getCurrentProjectId() != null) {
            sender.sendMessage(Message.raw("Project ID: ").color(Color.GRAY)
                    .insert(Message.raw(config.getCurrentProjectId()).color(Color.WHITE)));
        }

        Optional<Instant> lastUpdate = modSync.getModListStorage().getLastUpdateTime();
        if (lastUpdate.isPresent()) {
            sender.sendMessage(Message.raw("Last Update: ").color(Color.GRAY)
                    .insert(Message.raw(FORMATTER.format(lastUpdate.get())).color(Color.WHITE)));

            Optional<ModList> modList = modSync.getModListStorage().load();
            modList.ifPresent(list ->
                sender.sendMessage(Message.raw("Mods Loaded: ").color(Color.GRAY)
                        .insert(Message.raw(String.valueOf(list.getMods().size())).color(Color.WHITE)))
            );
        } else {
            sender.sendMessage(Message.raw("Last Update: ").color(Color.GRAY)
                    .insert(Message.raw("Never").color(Color.RED)));
        }

        boolean hasApiKey = config.getApiKey(config.getCurrentSource()) != null;
        sender.sendMessage(Message.raw("API Key Configured: ").color(Color.GRAY)
                .insert(Message.raw(hasApiKey ? "Yes" : "No").color(Color.WHITE)));
    }
}

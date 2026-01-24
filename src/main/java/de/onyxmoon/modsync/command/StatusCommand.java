package de.onyxmoon.modsync.command;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import de.onyxmoon.modsync.BuildInfo;
import de.onyxmoon.modsync.ModSync;
import de.onyxmoon.modsync.api.ModListProvider;
import de.onyxmoon.modsync.api.model.provider.ModList;
import de.onyxmoon.modsync.storage.model.PluginConfig;
import de.onyxmoon.modsync.util.PermissionHelper;

import javax.annotation.Nonnull;
import java.awt.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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
        sender.sendMessage(Message.raw("Update Mode: ").color(Color.GRAY)
                .insert(Message.raw(config.getUpdateMode().toString()).color(Color.WHITE)));

        Optional<Instant> lastUpdate = modSync.getModListStorage().getLastUpdateTime();
        Optional<ModList> modList = lastUpdate.isPresent()
                ? modSync.getModListStorage().load()
                : Optional.empty();

        if (modList.isPresent()) {
            sender.sendMessage(Message.raw("Mod List Source: ").color(Color.GRAY)
                    .insert(Message.raw(modList.get().getSource().getDisplayName()).color(Color.WHITE)));
            sender.sendMessage(Message.raw("Project ID: ").color(Color.GRAY)
                    .insert(Message.raw(modList.get().getProjectId()).color(Color.WHITE)));
        } else if (config.getCurrentProjectId() != null) {
            sender.sendMessage(Message.raw("Project ID: ").color(Color.GRAY)
                    .insert(Message.raw(config.getCurrentProjectId()).color(Color.WHITE)));
        }

        if (lastUpdate.isPresent()) {
            sender.sendMessage(Message.raw("Last Update: ").color(Color.GRAY)
                    .insert(Message.raw(FORMATTER.format(lastUpdate.get())).color(Color.WHITE)));

            modList.ifPresent(list ->
                sender.sendMessage(Message.raw("Mods Loaded: ").color(Color.GRAY)
                        .insert(Message.raw(String.valueOf(list.getMods().size())).color(Color.WHITE)))
            );
        } else {
            sender.sendMessage(Message.raw("Last Update: ").color(Color.GRAY)
                    .insert(Message.raw("Never").color(Color.RED)));
        }

        List<ModListProvider> providers = new ArrayList<>(modSync.getProviderRegistry().getProviders());
        providers.sort(Comparator.comparing(ModListProvider::getDisplayName, String.CASE_INSENSITIVE_ORDER));
        providers.forEach(provider -> {
            if (!provider.requiresApiKey()) {
                sender.sendMessage(Message.raw(provider.getDisplayName() + " API Key: ").color(Color.GRAY)
                        .insert(Message.raw("Not required").color(Color.WHITE)));
                return;
            }
            boolean hasApiKey = config.getApiKey(provider.getSource()) != null;
            sender.sendMessage(Message.raw(provider.getDisplayName() + " API Key: ").color(Color.GRAY)
                    .insert(Message.raw(hasApiKey ? "Set" : "Not set").color(Color.WHITE)));
        });
    }
}

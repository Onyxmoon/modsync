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
import de.onyxmoon.modsync.api.model.provider.ModList;
import de.onyxmoon.modsync.storage.model.PluginConfig;
import de.onyxmoon.modsync.util.PermissionHelper;

import javax.annotation.Nonnull;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Command: /modsync status
 * Shows current status and configuration.
 */
public class StatusCommand extends AbstractPlayerCommand {
    private static final DateTimeFormatter FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private final ModSync plugin;

    public StatusCommand(ModSync plugin) {
        super("status", "View ModSync status");
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

        PluginConfig config = plugin.getConfigStorage().getConfig();

        playerRef.sendMessage(Message.raw("=== ModSync Status ===").color("gold"));
        playerRef.sendMessage(Message.raw("Current Source: ").color("gray")
                .insert(Message.raw(config.getCurrentSource().getDisplayName()).color("white")));
        playerRef.sendMessage(Message.raw("Update Mode: ").color("gray")
                .insert(Message.raw(config.getUpdateMode().toString()).color("white")));

        if (config.getCurrentProjectId() != null) {
            playerRef.sendMessage(Message.raw("Project ID: ").color("gray")
                    .insert(Message.raw(config.getCurrentProjectId()).color("white")));
        }

        Optional<Instant> lastUpdate = plugin.getModListStorage().getLastUpdateTime();
        if (lastUpdate.isPresent()) {
            playerRef.sendMessage(Message.raw("Last Update: ").color("gray")
                    .insert(Message.raw(FORMATTER.format(lastUpdate.get())).color("white")));

            Optional<ModList> modList = plugin.getModListStorage().load();
            modList.ifPresent(list ->
                playerRef.sendMessage(Message.raw("Mods Loaded: ").color("gray")
                        .insert(Message.raw(String.valueOf(list.getMods().size())).color("white")))
            );
        } else {
            playerRef.sendMessage(Message.raw("Last Update: ").color("gray")
                    .insert(Message.raw("Never").color("red")));
        }

        boolean hasApiKey = config.getApiKey(config.getCurrentSource()) != null;
        playerRef.sendMessage(Message.raw("API Key Configured: ").color("gray")
                .insert(Message.raw(hasApiKey ? "Yes" : "No").color("white")));
    }
}

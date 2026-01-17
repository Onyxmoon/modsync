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
import de.onyxmoon.modsync.api.ModListSource;
import de.onyxmoon.modsync.util.PermissionHelper;

import javax.annotation.Nonnull;

/**
 * Command: /modsync setkey <key>
 * Sets the API key for the current source.
 */
public class SetKeyCommand extends AbstractPlayerCommand {
    private final ModSync plugin;
    private final RequiredArg<String> keyArg = this.withRequiredArg(
            "key",
            "API key for mod source",
            ArgTypes.STRING
    );

    public SetKeyCommand(ModSync plugin) {
        super("setkey", "Set API key for mod source");
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

        String key = commandContext.get(keyArg);

        if (key.isEmpty()) {
            playerRef.sendMessage(Message.raw("Usage: /modsync setkey <key>").color("red"));
            return;
        }

        ModListSource currentSource = plugin.getConfigStorage()
            .getConfig()
            .getCurrentSource();

        // Validate key asynchronously
        playerRef.sendMessage(Message.raw("Validating API key...").color("yellow"));

        plugin.getProviderRegistry()
            .getProvider(currentSource)
            .validateApiKey(key)
            .thenAccept(valid -> {
                if (valid) {
                    plugin.getConfigStorage().getConfig()
                        .setApiKey(currentSource, key);
                    plugin.getConfigStorage().save();
                    playerRef.sendMessage(Message.raw("API key set successfully for " + currentSource.getDisplayName()).color("green"));
                } else {
                    playerRef.sendMessage(Message.raw("Invalid API key for " + currentSource.getDisplayName()).color("red"));
                }
            })
            .exceptionally(ex -> {
                playerRef.sendMessage(Message.raw("Error validating API key: " + ex.getMessage()).color("red"));
                return null;
            });
    }
}

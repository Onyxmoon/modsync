package de.onyxmoon.modsync.command;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import de.onyxmoon.modsync.ModSync;
import de.onyxmoon.modsync.api.ModListSource;
import de.onyxmoon.modsync.util.PermissionHelper;

import javax.annotation.Nonnull;
import java.awt.*;

/**
 * Command: /modsync setkey <key>
 * Sets the API key for the current source.
 */
public class SetKeyCommand extends CommandBase {
    private final ModSync modSync;
    private final RequiredArg<String> keyArg = this.withRequiredArg(
            "key",
            "API key for mod source",
            ArgTypes.STRING
    );

    public SetKeyCommand(ModSync modSync) {
        super("setkey", "Set API key for mod source");
        this.modSync = modSync;
    }

    @Override
    protected void executeSync(@Nonnull CommandContext commandContext) {
        if (!PermissionHelper.checkAdminPermission(commandContext)) {
            return;
        }

        CommandSender sender = commandContext.sender();
        String key = commandContext.get(keyArg);

        if (key.isEmpty()) {
            sender.sendMessage(Message.raw("Usage: /modsync setkey <key>").color(Color.RED));
            return;
        }

        ModListSource currentSource = modSync.getConfigStorage()
            .getConfig()
            .getCurrentSource();

        // Validate key asynchronously
        sender.sendMessage(Message.raw("Validating API key...").color(Color.YELLOW));

        modSync.getProviderRegistry()
            .getProvider(currentSource)
            .validateApiKey(key)
            .thenAccept(valid -> {
                if (valid) {
                    modSync.getConfigStorage().getConfig()
                        .setApiKey(currentSource, key);
                    modSync.getConfigStorage().save();
                    sender.sendMessage(Message.raw("API key set successfully for " + currentSource.getDisplayName()).color(Color.GREEN));
                } else {
                    sender.sendMessage(Message.raw("Invalid API key for " + currentSource.getDisplayName()).color(Color.RED));
                }
            })
            .exceptionally(ex -> {
                sender.sendMessage(Message.raw("Error validating API key: " + ex.getMessage()).color(Color.RED));
                return null;
            });
    }
}

package de.onyxmoon.modsync.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.onyxmoon.modsync.ModSync;
import de.onyxmoon.modsync.ui.page.ModSyncMainPage;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nonnull;

/**
 * Command: /modsync ui
 * Opens the ModSync graphical user interface.
 */
public class UICommand extends AbstractPlayerCommand {
    private final ModSync modSync;

    public UICommand(ModSync modSync) {
        super("ui", "Open ModSync UI");
        this.modSync = modSync;
    }

    @Override
    protected void execute(@NonNullDecl CommandContext commandContext,
                           @NonNullDecl Store<EntityStore> store,
                           @NonNullDecl Ref<EntityStore> ref, @NonNullDecl PlayerRef playerRef,
                           @NonNullDecl World world) {
        // Get Player component for PageManager
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            playerRef.sendMessage(Message.raw("Could not get player component."));
            return;
        }

        // Open the main page directly
        ModSyncMainPage page = new ModSyncMainPage(modSync.getUIManager(), playerRef, store);
        player.getPageManager().openCustomPage(ref, store, page);
    }
}

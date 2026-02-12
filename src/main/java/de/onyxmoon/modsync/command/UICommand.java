package de.onyxmoon.modsync.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.onyxmoon.modsync.ModSync;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

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
        // Open the main UI page via UIManager (handles state management and null checks)
        modSync.getUIManager().openMainPage(playerRef, store);
    }
}

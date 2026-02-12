package de.onyxmoon.modsync.ui.page;

import au.ellie.hyui.builders.PageBuilder;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.onyxmoon.modsync.ModSync;
import de.onyxmoon.modsync.ui.UIManager;
import de.onyxmoon.modsync.ui.state.UIState;

/**
 * Base class for HyUI-based pages.
 */
public abstract class HyUIBasePage {

    protected final UIManager uiManager;
    protected final PlayerRef playerRef;
    protected final Store<EntityStore> store;

    protected HyUIBasePage(UIManager uiManager, PlayerRef playerRef, Store<EntityStore> store) {
        this.uiManager = uiManager;
        this.playerRef = playerRef;
        this.store = store;
    }

    /**
     * Opens this page for the player.
     */
    public abstract void open();

    /**
     * Creates a new PageBuilder for this player.
     */
    protected PageBuilder pageBuilder() {
        return PageBuilder.pageForPlayer(playerRef);
    }

    /**
     * Gets the ModSync plugin instance.
     */
    protected ModSync getModSync() {
        return uiManager.getModSync();
    }

    /**
     * Gets the UI state for the current player.
     */
    protected UIState getUIState() {
        return uiManager.getOrCreateState(playerRef);
    }

    /**
     * Navigates back to the previous page.
     */
    protected void navigateBack() {
        uiManager.navigateBack(playerRef, store);
    }

    /**
     * Opens the main page.
     */
    protected void openMain() {
        uiManager.openMainPage(playerRef, store);
    }

    /**
     * Escapes HTML special characters.
     */
    protected String escapeHtml(String text) {
        if (text == null) return "";
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}

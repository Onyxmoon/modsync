package de.onyxmoon.modsync.ui.page;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.onyxmoon.modsync.ModSync;
import de.onyxmoon.modsync.ui.ModSyncUIManager;
import de.onyxmoon.modsync.ui.state.UIState;

import javax.annotation.Nonnull;

/**
 * Base class for all ModSync UI pages.
 * Extends InteractiveCustomUIPage for event handling support.
 * Provides common functionality and access to the UI manager.
 */
public abstract class ModSyncBasePage extends InteractiveCustomUIPage<ModSyncBasePage.ModSyncEventData> {

    /**
     * Event data structure for ModSync pages.
     * Contains the action and optional string parameters.
     */
    public static class ModSyncEventData {
        public String action;
        public String param1;
        public String param2;

        public static final BuilderCodec<ModSyncEventData> CODEC =
                BuilderCodec.builder(ModSyncEventData.class, ModSyncEventData::new)
                        .append(new KeyedCodec<>("Action", Codec.STRING),
                                (ModSyncEventData o, String v) -> o.action = v,
                                (ModSyncEventData o) -> o.action)
                        .add()
                        .append(new KeyedCodec<>("Param1", Codec.STRING),
                                (ModSyncEventData o, String v) -> o.param1 = v,
                                (ModSyncEventData o) -> o.param1)
                        .add()
                        .append(new KeyedCodec<>("Param2", Codec.STRING),
                                (ModSyncEventData o, String v) -> o.param2 = v,
                                (ModSyncEventData o) -> o.param2)
                        .add()
                        .build();
    }

    protected final ModSyncUIManager uiManager;
    protected final PlayerRef playerRef;
    protected final Store<EntityStore> store;

    protected ModSyncBasePage(ModSyncUIManager uiManager, PlayerRef playerRef, Store<EntityStore> store) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, ModSyncEventData.CODEC);
        this.uiManager = uiManager;
        this.playerRef = playerRef;
        this.store = store;
    }

    /**
     * Build method called by Hytale to construct the UI.
     */
    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder commands,
                      @Nonnull UIEventBuilder events, @Nonnull Store<EntityStore> store) {
        buildPage(commands);
        bindEvents(events);
    }

    /**
     * Builds the page content. Override in subclasses.
     */
    protected abstract void buildPage(UICommandBuilder commands);

    /**
     * Binds events for the page. Override in subclasses if needed.
     */
    protected void bindEvents(UIEventBuilder events) {
        // Default implementation - subclasses should override
    }

    /**
     * Handles an event from the UI.
     */
    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store,
                                @Nonnull ModSyncEventData data) {
        if (data.action != null) {
            handleAction(data.action, data);
        }
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
     * Refreshes the current page.
     */
    protected void refresh() {
        uiManager.refreshCurrentPage(playerRef, store);
    }

    /**
     * Opens the main page.
     */
    protected void openMain() {
        uiManager.openMainPage(playerRef, store);
    }

    /**
     * Closes the current page.
     */
    protected void closePage(Ref<EntityStore> ref, Store<EntityStore> store) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player != null) {
            player.getPageManager().setPage(ref, store, Page.None);
        }
    }

    /**
     * Handles a specific action from an event.
     * Override in subclasses to handle page-specific actions.
     */
    protected void handleAction(String action, ModSyncEventData eventData) {
        // Default: handle common actions
        switch (action) {
            case "back" -> navigateBack();
            case "close" -> uiManager.closePage(playerRef, store);
            case "refresh" -> refresh();
        }
    }
}

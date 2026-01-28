package de.onyxmoon.modsync.ui;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.PageManager;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.onyxmoon.modsync.ModSync;
import de.onyxmoon.modsync.api.model.ManagedMod;
import de.onyxmoon.modsync.api.model.UnmanagedMod;
import de.onyxmoon.modsync.ui.page.*;
import de.onyxmoon.modsync.ui.state.UIState;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central UI manager for ModSync.
 * Manages page navigation and per-player UI state.
 */
public class ModSyncUIManager {

    private final ModSync modSync;
    private final Map<PlayerRef, UIState> playerStates = new ConcurrentHashMap<>();

    public ModSyncUIManager(ModSync modSync) {
        this.modSync = modSync;
    }

    /**
     * Gets the ModSync plugin instance.
     */
    public ModSync getModSync() {
        return modSync;
    }

    /**
     * Gets or creates the UI state for a player.
     */
    @Nonnull
    public UIState getOrCreateState(PlayerRef player) {
        return playerStates.computeIfAbsent(player, k -> new UIState());
    }

    /**
     * Removes the UI state for a player (call on disconnect).
     */
    public void removeState(PlayerRef player) {
        UIState state = playerStates.remove(player);
        if (state != null) {
            state.cancelPendingOperation();
        }
    }

    /**
     * Opens the main page showing the mod list.
     */
    public void openMainPage(PlayerRef playerRef, Store<EntityStore> store) {
        UIState state = getOrCreateState(playerRef);
        state.setCurrentPage(UIState.PageType.MAIN);
        state.setSelectedMod(null);

        openPage(playerRef, store, new ModSyncMainPage(this, playerRef, store));
    }

    /**
     * Opens the add mod page for entering a URL.
     */
    public void openAddModPage(PlayerRef playerRef, Store<EntityStore> store) {
        UIState state = getOrCreateState(playerRef);
        state.setCurrentPage(UIState.PageType.ADD_MOD);

        openPage(playerRef, store, new ModSyncAddModPage(this, playerRef, store));
    }

    /**
     * Opens the mod detail page for a specific mod.
     */
    public void openModDetailPage(PlayerRef playerRef, Store<EntityStore> store, ManagedMod mod) {
        UIState state = getOrCreateState(playerRef);
        state.setCurrentPage(UIState.PageType.MOD_DETAIL);
        state.setSelectedMod(mod);

        openPage(playerRef, store, new ModSyncModDetailPage(this, playerRef, store, mod));
    }

    /**
     * Opens the scan page for unmanaged mods.
     */
    public void openScanPage(PlayerRef playerRef, Store<EntityStore> store) {
        UIState state = getOrCreateState(playerRef);
        state.setCurrentPage(UIState.PageType.SCAN);

        openPage(playerRef, store, new ModSyncScanPage(this, playerRef, store));
    }

    /**
     * Opens the scan page with pre-scanned mods.
     */
    public void openScanPage(PlayerRef playerRef, Store<EntityStore> store, List<UnmanagedMod> unmanagedMods) {
        UIState state = getOrCreateState(playerRef);
        state.setCurrentPage(UIState.PageType.SCAN);

        openPage(playerRef, store, new ModSyncScanPage(this, playerRef, store, unmanagedMods));
    }

    /**
     * Opens the configuration page.
     */
    public void openConfigPage(PlayerRef playerRef, Store<EntityStore> store) {
        UIState state = getOrCreateState(playerRef);
        state.setCurrentPage(UIState.PageType.CONFIG);

        openPage(playerRef, store, new ModSyncConfigPage(this, playerRef, store));
    }

    /**
     * Closes the current page and returns to main or closes completely.
     */
    public void closePage(PlayerRef playerRef, Store<EntityStore> store) {
        UIState state = getOrCreateState(playerRef);
        UIState.PageType currentPage = state.getCurrentPage();

        // Close the page
        Ref<EntityStore> ref = playerRef.getReference();
        if (ref != null) {
            Player player = store.getComponent(ref, Player.getComponentType());
            if (player != null) {
                player.getPageManager().setPage(ref, store, Page.None);
            }
        }

        // Navigate back based on current page
        if (currentPage != UIState.PageType.MAIN) {
            // Return to main page
            openMainPage(playerRef, store);
        } else {
            // Closing main page - reset state
            state.reset();
        }
    }

    /**
     * Navigates back to the previous page.
     */
    public void navigateBack(PlayerRef playerRef, Store<EntityStore> store) {
        UIState state = getOrCreateState(playerRef);
        UIState.PageType currentPage = state.getCurrentPage();

        // Navigate to appropriate page
        switch (currentPage) {
            case ADD_MOD, MOD_DETAIL, SCAN, CONFIG -> openMainPage(playerRef, store);
            case MAIN -> {
                // Close completely
                Ref<EntityStore> ref = playerRef.getReference();
                if (ref != null) {
                    Player player = store.getComponent(ref, Player.getComponentType());
                    if (player != null) {
                        player.getPageManager().setPage(ref, store, Page.None);
                    }
                }
                state.reset();
            }
        }
    }

    /**
     * Refreshes the current page with updated data.
     */
    public void refreshCurrentPage(PlayerRef playerRef, Store<EntityStore> store) {
        UIState state = getOrCreateState(playerRef);

        switch (state.getCurrentPage()) {
            case MAIN -> openMainPage(playerRef, store);
            case ADD_MOD -> openAddModPage(playerRef, store);
            case MOD_DETAIL -> {
                ManagedMod mod = state.getSelectedMod();
                if (mod != null) {
                    // Refresh mod data from registry
                    ManagedMod refreshedMod = modSync.getManagedModStorage()
                            .getRegistry()
                            .findBySourceId(mod.getSourceId())
                            .orElse(mod);
                    openModDetailPage(playerRef, store, refreshedMod);
                } else {
                    openMainPage(playerRef, store);
                }
            }
            case SCAN -> openScanPage(playerRef, store);
            case CONFIG -> openConfigPage(playerRef, store);
        }
    }

    /**
     * Opens a page for the player.
     */
    private void openPage(PlayerRef playerRef, Store<EntityStore> store, ModSyncBasePage page) {
        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null) {
            ModSync.LOGGER.atWarning().log("Could not get reference for player UI");
            return;
        }

        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            ModSync.LOGGER.atWarning().log("Could not get Player component for UI");
            return;
        }

        PageManager pageManager = player.getPageManager();
        pageManager.openCustomPage(ref, store, page);
    }
}

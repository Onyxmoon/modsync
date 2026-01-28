package de.onyxmoon.modsync.ui.page;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.onyxmoon.modsync.BuildInfo;
import de.onyxmoon.modsync.api.model.ManagedMod;
import de.onyxmoon.modsync.api.model.ManagedModRegistry;
import de.onyxmoon.modsync.ui.ModSyncUIManager;
import de.onyxmoon.modsync.ui.state.UIState;

import java.util.List;

/**
 * Main page showing the list of managed mods.
 * Provides actions for adding, installing, and managing mods.
 */
public class ModSyncMainPage extends ModSyncBasePage {

    public ModSyncMainPage(ModSyncUIManager uiManager, PlayerRef playerRef, Store<EntityStore> store) {
        super(uiManager, playerRef, store);
    }

    @Override
    protected void buildPage(UICommandBuilder commands) {
        commands.append("Custom/Pages/ModSyncMain.ui");

        ManagedModRegistry registry = getModSync().getManagedModStorage().getRegistry();
        List<ManagedMod> mods = registry.getAll();

        // Set header info
        commands.set("#title", "ModSync");
        commands.set("#version", BuildInfo.VERSION);

        // Populate mod list
        populateModList(commands, mods);

        // Set summary
        int installed = registry.getInstalled().size();
        int total = mods.size();
        commands.set("#summary", installed + " / " + total + " installed");

        // Show loading state if active
        if (getUIState().isLoading()) {
            commands.set("#loading", "true");
        }

        // Show status message if any
        String statusMessage = getUIState().getStatusMessage();
        if (statusMessage != null) {
            commands.set("#status_message", statusMessage);
            commands.set("#status_type", getUIState().getStatusType().name().toLowerCase());
        }
    }

    private void populateModList(UICommandBuilder commands, List<ManagedMod> mods) {
        StringBuilder modListHtml = new StringBuilder();

        for (ManagedMod mod : mods) {
            String status = mod.isInstalled() ? "installed" : "not_installed";
            String version = mod.getInstalledState()
                    .map(state -> state.getInstalledVersionNumber())
                    .orElse("-");

            // Build mod entry (format depends on .ui file structure)
            modListHtml.append(String.format(
                    "<mod id=\"%s\" name=\"%s\" status=\"%s\" version=\"%s\" source=\"%s\"/>",
                    mod.getSourceId(),
                    escapeXml(mod.getName()),
                    status,
                    escapeXml(version),
                    mod.getSource()
            ));
        }

        commands.set("#mod_list", modListHtml.toString());
    }

    private String escapeXml(String text) {
        if (text == null) return "";
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    @Override
    protected void bindEvents(UIEventBuilder events) {
        events.addEventBinding(CustomUIEventBindingType.Activating, "#add_btn");
        events.addEventBinding(CustomUIEventBindingType.Activating, "#scan_btn");
        events.addEventBinding(CustomUIEventBindingType.Activating, "#settings_btn");
    }

    @Override
    protected void handleAction(String action, ModSyncEventData eventData) {
        switch (action) {
            case "add_mod" -> uiManager.openAddModPage(playerRef, store);
            case "scan" -> uiManager.openScanPage(playerRef, store);
            case "settings" -> uiManager.openConfigPage(playerRef, store);
            case "mod_click" -> {
                if (eventData.param1 != null) {
                    getModSync().getManagedModStorage().getRegistry()
                            .findBySourceId(eventData.param1)
                            .ifPresent(mod -> uiManager.openModDetailPage(playerRef, store, mod));
                }
            }
            case "install_all" -> installAll();
            case "check_updates" -> checkUpdates();
            case "upgrade_all" -> upgradeAll();
            default -> super.handleAction(action, eventData);
        }
    }

    private void installAll() {
        getUIState().startLoading();
        getUIState().setStatus("Installing all mods...", UIState.StatusType.INFO);
        refresh();

        // TODO: Implement install all via service
        getUIState().stopLoading();
        getUIState().setStatus("Install all not yet implemented", UIState.StatusType.WARNING);
        refresh();
    }

    private void checkUpdates() {
        getUIState().startLoading();
        getUIState().setStatus("Checking for updates...", UIState.StatusType.INFO);
        refresh();

        // TODO: Implement update check via service
        getUIState().stopLoading();
        getUIState().setStatus("Update check not yet implemented", UIState.StatusType.WARNING);
        refresh();
    }

    private void upgradeAll() {
        getUIState().startLoading();
        getUIState().setStatus("Upgrading all mods...", UIState.StatusType.INFO);
        refresh();

        // TODO: Implement upgrade all via service
        getUIState().stopLoading();
        getUIState().setStatus("Upgrade all not yet implemented", UIState.StatusType.WARNING);
        refresh();
    }
}

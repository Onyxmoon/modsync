package de.onyxmoon.modsync.ui.page;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.onyxmoon.modsync.BuildInfo;
import de.onyxmoon.modsync.api.model.InstalledState;
import de.onyxmoon.modsync.api.model.ManagedMod;
import de.onyxmoon.modsync.api.model.ManagedModRegistry;
import de.onyxmoon.modsync.ui.UIManager;
import de.onyxmoon.modsync.ui.state.UIState;

import java.util.List;

/**
 * Main page showing the list of managed mods.
 * Provides actions for adding, installing, and managing mods.
 */
public class ModSyncMainPage extends ModSyncBasePage {

    public ModSyncMainPage(UIManager uiManager, PlayerRef playerRef, Store<EntityStore> store) {
        super(uiManager, playerRef, store);
    }

    @Override
    protected void buildPage(UICommandBuilder commands) {
        commands.append("Pages/ModSyncMain.ui");

        ManagedModRegistry registry = getModSync().getManagedModStorage().getRegistry();
        List<ManagedMod> mods = registry.getAll();

        // Set header info
        commands.set("#version.Text", BuildInfo.VERSION);

        // Populate mod list
        populateModList(commands, mods);

        // Set summary
        int installed = registry.getInstalled().size();
        int total = mods.size();
        commands.set("#summary.Text", installed + " / " + total + " installed");

        // Show loading state if active
        if (getUIState().isLoading()) {
            commands.set("#loading", "true");
        }

        // Show status message if any
        String statusMessage = getUIState().getStatusMessage();
        if (statusMessage != null) {
            commands.set("#statusMessage.Text", statusMessage);
            commands.set("#statusType", getUIState().getStatusType().name().toLowerCase());
        }
    }

    private void populateModList(UICommandBuilder commands, List<ManagedMod> mods) {
        if (mods.isEmpty()) {
            // Empty message is shown by default in the UI
            return;
        }

        // Hide empty message
        commands.set("#emptyMessage.Text", "");

        // Use appendInline to create mod items with embedded values
        for (ManagedMod mod : mods) {
            String name = escapeUiString(mod.getName());
            String identifier = escapeUiString(mod.getIdentifierString().orElse(mod.getSource() + ":" + mod.getSourceId()));
            String version = "v" + mod.getInstalledState()
                    .map(InstalledState::getInstalledVersionNumber)
                    .orElse("-");
            String status = mod.isInstalled() ? "Installed" : "Not installed";

            String inlineUi = String.format("""
                Group {
                  Anchor: (Height: 56, Bottom: 4);
                  Padding: (Full: 8);
                  LayoutMode: Left;
                  Background: (Color: #1e2a3a);

                  Group { Anchor: (Width: 40, Height: 40); Background: (Color: #2a3a4a); }
                  Group { Anchor: (Width: 10); }

                  Group {
                    FlexWeight: 1;
                    LayoutMode: Top;
                    Label { Text: "%s"; Style: (FontSize: 14, TextColor: #ffffff, RenderBold: true); Anchor: (Height: 18); }
                    Label { Text: "%s"; Style: (FontSize: 11, TextColor: #4a5568); Anchor: (Height: 14); }
                    Label { Text: "%s"; Style: (FontSize: 11, TextColor: #96a9be); }
                  }

                  Label { Text: "%s"; Anchor: (Width: 80); Style: (FontSize: 11, TextColor: #96a9be, HorizontalAlignment: End, VerticalAlignment: Center); }
                }
                """, name, identifier, version, status);

            commands.appendInline("#modListContainer", inlineUi);
        }
    }

    private String escapeUiString(String text) {
        if (text == null) return "";
        return text.replace("\"", "\\\"").replace("\n", " ");
    }

    @Override
    protected void bindEvents(UIEventBuilder events) {
        events.addEventBinding(CustomUIEventBindingType.Activating, "#addBtn");
        events.addEventBinding(CustomUIEventBindingType.Activating, "#scanBtn");
        events.addEventBinding(CustomUIEventBindingType.Activating, "#settingsBtn");
    }

    @Override
    protected void handleAction(String action, ModSyncEventData eventData) {
        // Action may come as button ID (with or without #) or as custom action name
        String normalizedAction = action.startsWith("#") ? action.substring(1) : action;

        switch (normalizedAction) {
            case "addBtn", "add_mod" -> uiManager.openAddModPage(playerRef, store);
            case "scanBtn", "scan" -> uiManager.openScanPage(playerRef, store);
            case "settingsBtn", "settings" -> uiManager.openConfigPage(playerRef, store);
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

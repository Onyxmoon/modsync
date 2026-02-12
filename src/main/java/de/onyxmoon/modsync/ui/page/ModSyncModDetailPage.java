package de.onyxmoon.modsync.ui.page;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.onyxmoon.modsync.api.model.InstalledState;
import de.onyxmoon.modsync.api.model.ManagedMod;
import de.onyxmoon.modsync.ui.UIManager;
import de.onyxmoon.modsync.ui.state.UIState;

/**
 * Page showing details of a specific mod.
 * Allows installing, upgrading, and removing the mod.
 */
public class ModSyncModDetailPage extends ModSyncBasePage {

    private final ManagedMod mod;

    public ModSyncModDetailPage(UIManager uiManager, PlayerRef playerRef,
                                Store<EntityStore> store, ManagedMod mod) {
        super(uiManager, playerRef, store);
        this.mod = mod;
    }

    @Override
    protected void buildPage(UICommandBuilder commands) {
        commands.append("Pages/ModSyncModDetail.ui");

        // Basic info
        commands.set("#modName.Text", mod.getName());
        commands.set("#modSlug.Text", mod.getSlug() != null ? mod.getSlug() : "-");
        commands.set("#modIdentifier.Text", mod.getIdentifierString().orElse("-"));
        commands.set("#modSource.Text", mod.getSource());
        commands.set("#modId.Text", mod.getModId());
        commands.set("#modType.Text", mod.getPluginType() != null ? mod.getPluginType().name() : "Unknown");

        // Installation state
        if (mod.isInstalled()) {
            InstalledState state = mod.getInstalledState().orElseThrow();
            commands.set("#isInstalled.Text", "Installed");
            commands.set("#installedVersion.Text", state.getInstalledVersionNumber());
            commands.set("#installedFile.Text", state.getFileName());
        } else {
            commands.set("#isInstalled.Text", "Not installed");
            commands.set("#installedVersion.Text", "-");
            commands.set("#installedFile.Text", "-");
        }

        // Status message
        String statusMessage = getUIState().getStatusMessage();
        if (statusMessage != null) {
            commands.set("#statusMessage.Text", statusMessage);
        }
    }

    @Override
    protected void bindEvents(UIEventBuilder events) {
        events.addEventBinding(CustomUIEventBindingType.Activating, "#installBtn");
        events.addEventBinding(CustomUIEventBindingType.Activating, "#uninstallBtn");
        events.addEventBinding(CustomUIEventBindingType.Activating, "#upgradeBtn");
        events.addEventBinding(CustomUIEventBindingType.Activating, "#removeBtn");
        events.addEventBinding(CustomUIEventBindingType.Activating, "#backBtn");
    }

    @Override
    protected void handleAction(String action, ModSyncEventData eventData) {
        String normalizedAction = action.startsWith("#") ? action.substring(1) : action;

        switch (normalizedAction) {
            case "installBtn", "install" -> installMod();
            case "uninstallBtn", "uninstall" -> uninstallMod();
            case "upgradeBtn", "upgrade" -> upgradeMod();
            case "removeBtn", "remove" -> removeFromList();
            case "backBtn", "back" -> navigateBack();
            default -> super.handleAction(action, eventData);
        }
    }

    private void installMod() {
        if (mod.isInstalled()) {
            getUIState().setStatus("Mod is already installed", UIState.StatusType.WARNING);
            refresh();
            return;
        }

        getUIState().startLoading();
        refresh();

        // TODO: Use extracted service for installation
        // For now, this is a placeholder
        getUIState().setStatus("Installation not yet implemented in UI", UIState.StatusType.WARNING);
        getUIState().stopLoading();
        refresh();
    }

    private void uninstallMod() {
        if (!mod.isInstalled()) {
            getUIState().setStatus("Mod is not installed", UIState.StatusType.WARNING);
            refresh();
            return;
        }

        getUIState().startLoading();
        refresh();

        getModSync().getDownloadService().deleteMod(mod)
                .thenAccept(deleted -> {
                    if (deleted) {
                        // Update mod in registry (remove installed state)
                        ManagedMod updatedMod = mod.toBuilder()
                                .installedState(null)
                                .build();
                        getModSync().getManagedModStorage().updateMod(updatedMod);

                        getUIState().stopLoading();
                        getUIState().setStatus("Mod uninstalled", UIState.StatusType.SUCCESS);
                        // Refresh with updated mod
                        uiManager.openModDetailPage(playerRef, store, updatedMod);
                    } else {
                        getUIState().stopLoading();
                        getUIState().setStatus("Could not delete mod file", UIState.StatusType.ERROR);
                        refresh();
                    }
                })
                .exceptionally(ex -> {
                    getUIState().stopLoading();
                    getUIState().setStatus("Error: " + ex.getMessage(), UIState.StatusType.ERROR);
                    refresh();
                    return null;
                });
    }

    private void upgradeMod() {
        if (!mod.isInstalled()) {
            getUIState().setStatus("Mod must be installed first", UIState.StatusType.WARNING);
            refresh();
            return;
        }

        getUIState().startLoading();
        refresh();

        // TODO: Use extracted service for upgrade
        getUIState().setStatus("Upgrade not yet implemented in UI", UIState.StatusType.WARNING);
        getUIState().stopLoading();
        refresh();
    }

    private void removeFromList() {
        // First uninstall if installed
        if (mod.isInstalled()) {
            getUIState().startLoading();
            refresh();

            getModSync().getDownloadService().deleteMod(mod)
                    .thenAccept(deleted -> {
                        getModSync().getManagedModStorage().removeMod(mod.getSourceId());
                        getUIState().stopLoading();
                        getUIState().setStatus("Removed: " + mod.getName(), UIState.StatusType.SUCCESS);
                        openMain();
                    })
                    .exceptionally(ex -> {
                        getUIState().stopLoading();
                        getUIState().setStatus("Error removing: " + ex.getMessage(), UIState.StatusType.ERROR);
                        refresh();
                        return null;
                    });
        } else {
            getModSync().getManagedModStorage().removeMod(mod.getSourceId());
            getUIState().setStatus("Removed: " + mod.getName(), UIState.StatusType.SUCCESS);
            openMain();
        }
    }
}

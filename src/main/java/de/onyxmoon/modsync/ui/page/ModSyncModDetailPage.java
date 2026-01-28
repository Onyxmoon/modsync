package de.onyxmoon.modsync.ui.page;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.onyxmoon.modsync.api.model.InstalledState;
import de.onyxmoon.modsync.api.model.ManagedMod;
import de.onyxmoon.modsync.ui.ModSyncUIManager;
import de.onyxmoon.modsync.ui.state.UIState;

/**
 * Page showing details of a specific mod.
 * Allows installing, upgrading, and removing the mod.
 */
public class ModSyncModDetailPage extends ModSyncBasePage {

    private final ManagedMod mod;

    public ModSyncModDetailPage(ModSyncUIManager uiManager, PlayerRef playerRef,
                                 Store<EntityStore> store, ManagedMod mod) {
        super(uiManager, playerRef, store);
        this.mod = mod;
    }

    @Override
    protected void buildPage(UICommandBuilder commands) {
        commands.append("Custom/Pages/ModSyncModDetail.ui");

        // Basic info
        commands.set("#mod_name", mod.getName());
        commands.set("#mod_slug", mod.getSlug() != null ? mod.getSlug() : "-");
        commands.set("#mod_identifier", mod.getIdentifierString().orElse("-"));
        commands.set("#mod_source", mod.getSource());
        commands.set("#mod_id", mod.getModId());
        commands.set("#mod_type", mod.getPluginType() != null ? mod.getPluginType().name() : "Unknown");

        // Added info
        if (mod.getAddedAt() != null) {
            commands.set("#added_at", mod.getAddedAt().toString());
        }
        if (mod.getAddedViaUrl() != null) {
            commands.set("#added_url", mod.getAddedViaUrl());
        }

        // Installation state
        if (mod.isInstalled()) {
            InstalledState state = mod.getInstalledState().orElseThrow();
            commands.set("#is_installed", "true");
            commands.set("#installed_version", state.getInstalledVersionNumber());
            commands.set("#installed_file", state.getFileName());
            commands.set("#installed_hash", state.getFileHash() != null ? state.getFileHash() : "-");
            if (state.getInstalledAt() != null) {
                commands.set("#installed_at", state.getInstalledAt().toString());
            }
        } else {
            commands.set("#is_installed", "false");
        }

        // Release channel override
        if (mod.getReleaseChannelOverride() != null) {
            commands.set("#channel_override", mod.getReleaseChannelOverride().name());
        }

        // Loading state
        if (getUIState().isLoading()) {
            commands.set("#loading", "true");
        }

        // Status message
        String statusMessage = getUIState().getStatusMessage();
        if (statusMessage != null) {
            commands.set("#status_message", statusMessage);
            commands.set("#status_type", getUIState().getStatusType().name().toLowerCase());
        }
    }

    @Override
    protected void bindEvents(UIEventBuilder events) {
        events.addEventBinding(CustomUIEventBindingType.Activating, "#install_btn");
        events.addEventBinding(CustomUIEventBindingType.Activating, "#uninstall_btn");
        events.addEventBinding(CustomUIEventBindingType.Activating, "#upgrade_btn");
        events.addEventBinding(CustomUIEventBindingType.Activating, "#remove_btn");
        events.addEventBinding(CustomUIEventBindingType.Activating, "#back_btn");
    }

    @Override
    protected void handleAction(String action, ModSyncEventData eventData) {
        switch (action) {
            case "install" -> installMod();
            case "uninstall" -> uninstallMod();
            case "upgrade" -> upgradeMod();
            case "remove" -> removeFromList();
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

package de.onyxmoon.modsync.ui.page;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.onyxmoon.modsync.BuildInfo;
import de.onyxmoon.modsync.service.selfupgrade.model.UpgradeCheckResult;
import de.onyxmoon.modsync.storage.model.PluginConfig;
import de.onyxmoon.modsync.ui.UIManager;
import de.onyxmoon.modsync.ui.state.UIState;

/**
 * Configuration page for ModSync settings and API keys.
 */
public class ModSyncConfigPage extends ModSyncBasePage {

    private UpgradeCheckResult upgradeCheck;

    public ModSyncConfigPage(UIManager uiManager, PlayerRef playerRef, Store<EntityStore> store) {
        super(uiManager, playerRef, store);
    }

    @Override
    protected void buildPage(UICommandBuilder commands) {
        commands.append("Pages/ModSyncConfig.ui");

        // Version info
        commands.set("#version.Text", BuildInfo.VERSION);

        // Upgrade status
        if (upgradeCheck != null) {
            if (upgradeCheck.hasUpdate()) {
                commands.set("#updateStatus.Text", "Update available: " + upgradeCheck.latestVersion());
            } else {
                commands.set("#updateStatus.Text", "Up to date");
            }
        } else {
            commands.set("#updateStatus.Text", "Not checked");
        }

        // Config settings
        PluginConfig config = getModSync().getConfigStorage().getConfig();
        commands.set("#updateMode.Text", config.getUpdateMode().name());
        commands.set("#defaultChannel.Text", config.getDefaultReleaseChannel().getDisplayName());

        // Status message
        String statusMessage = getUIState().getStatusMessage();
        if (statusMessage != null) {
            commands.set("#statusMessage.Text", statusMessage);
        }
    }

    @Override
    protected void bindEvents(UIEventBuilder events) {
        events.addEventBinding(CustomUIEventBindingType.Activating, "#checkUpdateBtn");
        events.addEventBinding(CustomUIEventBindingType.Activating, "#upgradeBtn");
        events.addEventBinding(CustomUIEventBindingType.Activating, "#reloadBtn");
        events.addEventBinding(CustomUIEventBindingType.Activating, "#backBtn");
    }

    @Override
    protected void handleAction(String action, ModSyncEventData eventData) {
        String normalizedAction = action.startsWith("#") ? action.substring(1) : action;

        switch (normalizedAction) {
            case "checkUpdateBtn", "check_update" -> checkForUpdate();
            case "upgradeBtn", "self_upgrade" -> performSelfUpgrade();
            case "save_key" -> saveApiKey(eventData.param1, eventData.param2);
            case "reloadBtn", "reload" -> reloadConfig();
            case "backBtn", "back" -> navigateBack();
            default -> super.handleAction(action, eventData);
        }
    }

    private void checkForUpdate() {
        getUIState().startLoading();
        refresh();

        getModSync().getSelfUpdateService().checkForUpgrade()
                .thenAccept(result -> {
                    this.upgradeCheck = result;
                    getUIState().stopLoading();
                    if (result.hasUpdate()) {
                        getUIState().setStatus("Update available: " + result.latestVersion(),
                                UIState.StatusType.INFO);
                    } else {
                        getUIState().setStatus("ModSync is up to date", UIState.StatusType.SUCCESS);
                    }
                    refresh();
                })
                .exceptionally(ex -> {
                    getUIState().stopLoading();
                    getUIState().setStatus("Error checking for updates: " + ex.getMessage(),
                            UIState.StatusType.ERROR);
                    refresh();
                    return null;
                });
    }

    private void performSelfUpgrade() {
        if (upgradeCheck == null || !upgradeCheck.hasUpdate()) {
            getUIState().setStatus("No update available. Check for updates first.",
                    UIState.StatusType.WARNING);
            refresh();
            return;
        }

        getUIState().startLoading();
        refresh();

        getModSync().getSelfUpdateService().performUpgrade(upgradeCheck.release())
                .thenAccept(result -> {
                    getUIState().stopLoading();
                    if (result.success()) {
                        if (result.restartRequired()) {
                            getUIState().setStatus(
                                    "Upgrade complete. Restart server to apply.",
                                    UIState.StatusType.SUCCESS);
                        } else {
                            getUIState().setStatus(result.message(), UIState.StatusType.SUCCESS);
                        }
                    } else {
                        getUIState().setStatus(
                                "Upgrade failed: " + result.message(),
                                UIState.StatusType.ERROR);
                    }
                    refresh();
                })
                .exceptionally(ex -> {
                    getUIState().stopLoading();
                    getUIState().setStatus("Upgrade error: " + ex.getMessage(),
                            UIState.StatusType.ERROR);
                    refresh();
                    return null;
                });
    }

    private void saveApiKey(String providerId, String key) {
        if (providerId == null || providerId.isEmpty()) {
            getUIState().setStatus("Invalid provider", UIState.StatusType.ERROR);
            refresh();
            return;
        }

        PluginConfig config = getModSync().getConfigStorage().getConfig();

        if (key == null || key.isEmpty()) {
            config.getApiKeys().remove(providerId.toLowerCase());
            getUIState().setStatus("API key removed for " + providerId, UIState.StatusType.INFO);
        } else {
            config.setApiKey(providerId, key);
            getUIState().setStatus("API key saved for " + providerId, UIState.StatusType.SUCCESS);
        }

        getModSync().getConfigStorage().save();
        refresh();
    }

    private void reloadConfig() {
        getModSync().getConfigStorage().reload();
        getModSync().getManagedModStorage().reload();
        getUIState().setStatus("Configuration reloaded", UIState.StatusType.SUCCESS);
        refresh();
    }
}

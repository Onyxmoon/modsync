package de.onyxmoon.modsync.ui.page;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.onyxmoon.modsync.BuildInfo;
import de.onyxmoon.modsync.api.ModProvider;
import de.onyxmoon.modsync.service.selfupgrade.model.UpgradeCheckResult;
import de.onyxmoon.modsync.storage.model.PluginConfig;
import de.onyxmoon.modsync.ui.ModSyncUIManager;
import de.onyxmoon.modsync.ui.state.UIState;

import java.util.Collection;

/**
 * Configuration page for ModSync settings and API keys.
 */
public class ModSyncConfigPage extends ModSyncBasePage {

    private UpgradeCheckResult upgradeCheck;

    public ModSyncConfigPage(ModSyncUIManager uiManager, PlayerRef playerRef, Store<EntityStore> store) {
        super(uiManager, playerRef, store);
    }

    @Override
    protected void buildPage(UICommandBuilder commands) {
        commands.append("Custom/Pages/ModSyncConfig.ui");

        commands.set("#title", "ModSync Settings");

        // Version info
        commands.set("#version", BuildInfo.VERSION);

        // Upgrade status
        if (upgradeCheck != null) {
            if (upgradeCheck.hasUpdate()) {
                commands.set("#update_available", "true");
                commands.set("#latest_version", upgradeCheck.latestVersion().toString());
            } else {
                commands.set("#update_available", "false");
                commands.set("#update_status", "Up to date");
            }
        } else {
            commands.set("#update_status", "Not checked");
        }

        // Provider API keys
        populateProviderKeys(commands);

        // Config settings
        PluginConfig config = getModSync().getConfigStorage().getConfig();
        commands.set("#update_mode", config.getUpdateMode().name());
        commands.set("#default_channel", config.getDefaultReleaseChannel().getDisplayName());

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

    private void populateProviderKeys(UICommandBuilder commands) {
        Collection<ModProvider> providers = getModSync().getProviderRegistry().getProviders();
        PluginConfig config = getModSync().getConfigStorage().getConfig();

        StringBuilder providersHtml = new StringBuilder();

        for (ModProvider provider : providers) {
            String name = provider.getDisplayName();
            boolean requiresKey = provider.requiresApiKey();
            String keyStatus;

            if (!requiresKey) {
                keyStatus = "not_required";
            } else {
                String key = config.getApiKey(provider.getSource());
                keyStatus = (key != null && !key.isEmpty()) ? "set" : "missing";
            }

            providersHtml.append(String.format(
                    "<provider name=\"%s\" id=\"%s\" requires_key=\"%s\" key_status=\"%s\"/>",
                    escapeXml(name),
                    escapeXml(provider.getSource()),
                    requiresKey,
                    keyStatus
            ));
        }

        commands.set("#providers", providersHtml.toString());
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
        events.addEventBinding(CustomUIEventBindingType.Activating, "#check_update_btn");
        events.addEventBinding(CustomUIEventBindingType.Activating, "#upgrade_btn");
        events.addEventBinding(CustomUIEventBindingType.Activating, "#reload_btn");
        events.addEventBinding(CustomUIEventBindingType.Activating, "#back_btn");
    }

    @Override
    protected void handleAction(String action, ModSyncEventData eventData) {
        switch (action) {
            case "check_update" -> checkForUpdate();
            case "self_upgrade" -> performSelfUpgrade();
            case "save_key" -> saveApiKey(eventData.param1, eventData.param2);
            case "reload" -> reloadConfig();
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

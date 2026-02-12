package de.onyxmoon.modsync.ui.page;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.onyxmoon.modsync.api.model.ManagedMod;
import de.onyxmoon.modsync.api.model.provider.ModEntry;
import de.onyxmoon.modsync.service.ProviderFetchService;
import de.onyxmoon.modsync.ui.UIManager;
import de.onyxmoon.modsync.ui.state.UIState;
import de.onyxmoon.modsync.util.CommandUtils;

import java.time.Instant;
import java.util.List;

/**
 * Page for adding a new mod via URL.
 */
public class ModSyncAddModPage extends ModSyncBasePage {

    private String currentUrl = "";
    private String errorMessage;
    private ModEntry fetchedMod;
    private String fetchedProvider;

    public ModSyncAddModPage(UIManager uiManager, PlayerRef playerRef, Store<EntityStore> store) {
        super(uiManager, playerRef, store);
    }

    @Override
    protected void buildPage(UICommandBuilder commands) {
        commands.append("Pages/ModSyncAddMod.ui");

        // Show detected providers
        if (!currentUrl.isEmpty()) {
            List<String> providers = getModSync().getFetchService().getProviderNamesForUrl(currentUrl);
            if (!providers.isEmpty()) {
                commands.set("#providerInfo.Text", "Provider: " + String.join(", ", providers));
            } else {
                commands.set("#providerInfo.Text", "No provider found for this URL");
            }
        }

        // Show error if any
        if (errorMessage != null) {
            commands.set("#errorMessage.Text", errorMessage);
        }

        // Show fetched mod info
        if (fetchedMod != null) {
            commands.set("#modName.Text", fetchedMod.getName());
            commands.set("#modSlug.Text", fetchedMod.getSlug());
            commands.set("#modProvider.Text", fetchedProvider);
        }
    }

    @Override
    protected void bindEvents(UIEventBuilder events) {
        events.addEventBinding(CustomUIEventBindingType.Activating, "#fetchBtn");
        events.addEventBinding(CustomUIEventBindingType.Activating, "#addBtn");
        events.addEventBinding(CustomUIEventBindingType.Activating, "#backBtn");
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#urlInput");
    }

    @Override
    protected void handleAction(String action, ModSyncEventData eventData) {
        String normalizedAction = action.startsWith("#") ? action.substring(1) : action;

        switch (normalizedAction) {
            case "fetchBtn", "fetch" -> onFetchClicked();
            case "addBtn", "add" -> onAddClicked();
            case "backBtn", "back" -> navigateBack();
            case "urlInput", "url_changed" -> {
                if (eventData.param1 != null) {
                    onUrlChanged(eventData.param1);
                }
            }
            default -> super.handleAction(action, eventData);
        }
    }

    private void onUrlChanged(String url) {
        this.currentUrl = url;
        this.errorMessage = null;
        this.fetchedMod = null;
        refresh();
    }

    private void onFetchClicked() {
        if (currentUrl == null || currentUrl.isBlank()) {
            errorMessage = "Please enter a URL";
            refresh();
            return;
        }

        UIState state = getUIState();
        state.startLoading();
        refresh();

        ProviderFetchService fetchService = getModSync().getFetchService();

        fetchService.fetchFromUrl(currentUrl, providerName -> {
            // API key missing callback - just log it
        }).thenAccept(result -> {
            state.stopLoading();
            this.fetchedMod = result.modEntry();
            this.fetchedProvider = result.provider().getDisplayName();
            this.errorMessage = null;
            refresh();
        }).exceptionally(ex -> {
            state.stopLoading();
            this.errorMessage = CommandUtils.extractErrorMessage(ex);
            this.fetchedMod = null;
            refresh();
            return null;
        });
    }

    public void onAddClicked() {
        if (fetchedMod == null || fetchedProvider == null) {
            errorMessage = "Please fetch mod info first";
            refresh();
            return;
        }

        // Check if mod already exists
        if (getModSync().getManagedModStorage().getRegistry()
                .findBySourceId(fetchedMod.getModId()).isPresent()) {
            errorMessage = "This mod is already in your list";
            refresh();
            return;
        }

        // Create and add the managed mod
        ManagedMod newMod = ManagedMod.builder()
                .name(fetchedMod.getName())
                .slug(fetchedMod.getSlug())
                .source(fetchedProvider)
                .modId(fetchedMod.getModId())
                .addedAt(Instant.now())
                .addedViaUrl(currentUrl)
                .pluginType(fetchedMod.getPluginType())
                .build();

        getModSync().getManagedModStorage().addMod(newMod);

        // Success - go back to main page
        getUIState().setStatus("Added: " + newMod.getName(), UIState.StatusType.SUCCESS);
        openMain();
    }

    public void onBackClicked() {
        navigateBack();
    }
}

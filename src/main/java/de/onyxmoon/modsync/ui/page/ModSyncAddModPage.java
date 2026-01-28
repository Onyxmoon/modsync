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
import de.onyxmoon.modsync.ui.ModSyncUIManager;
import de.onyxmoon.modsync.ui.state.UIState;
import de.onyxmoon.modsync.util.CommandUtils;

import javax.annotation.Nullable;
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

    public ModSyncAddModPage(ModSyncUIManager uiManager, PlayerRef playerRef, Store<EntityStore> store) {
        super(uiManager, playerRef, store);
    }

    @Override
    protected void buildPage(UICommandBuilder commands) {
        commands.append("Custom/Pages/ModSyncAddMod.ui");

        commands.set("#title", "Add Mod");
        commands.set("#url_value", currentUrl);

        // Show detected providers
        if (!currentUrl.isEmpty()) {
            List<String> providers = getModSync().getFetchService().getProviderNamesForUrl(currentUrl);
            if (!providers.isEmpty()) {
                commands.set("#provider_info", "Provider: " + String.join(", ", providers));
            } else {
                commands.set("#provider_info", "No provider found for this URL");
            }
        }

        // Show loading state
        if (getUIState().isLoading()) {
            commands.set("#loading", "true");
            commands.set("#status_message", "Fetching mod info...");
        }

        // Show error if any
        if (errorMessage != null) {
            commands.set("#error_message", errorMessage);
        }

        // Show fetched mod info
        if (fetchedMod != null) {
            commands.set("#mod_name", fetchedMod.getName());
            commands.set("#mod_slug", fetchedMod.getSlug());
            commands.set("#mod_provider", fetchedProvider);
            commands.set("#mod_found", "true");
        }
    }

    @Override
    protected void bindEvents(UIEventBuilder events) {
        events.addEventBinding(CustomUIEventBindingType.Activating, "#fetch_btn");
        events.addEventBinding(CustomUIEventBindingType.Activating, "#add_btn");
        events.addEventBinding(CustomUIEventBindingType.Activating, "#back_btn");
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#url_input");
    }

    @Override
    protected void handleAction(String action, ModSyncEventData eventData) {
        switch (action) {
            case "fetch" -> onFetchClicked();
            case "add" -> onAddClicked();
            case "url_changed" -> {
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

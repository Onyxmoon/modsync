package de.onyxmoon.modsync.ui.page;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.onyxmoon.modsync.api.model.ImportMatch;
import de.onyxmoon.modsync.api.model.ImportMatchConfidence;
import de.onyxmoon.modsync.api.model.UnmanagedMod;
import de.onyxmoon.modsync.service.ModScanService;
import de.onyxmoon.modsync.ui.UIManager;
import de.onyxmoon.modsync.ui.state.UIState;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Page for scanning and importing unmanaged mods.
 */
public class ModSyncScanPage extends ModSyncBasePage {

    private static final String DEFAULT_IMPORT_SOURCE = "curseforge";

    private List<UnmanagedMod> unmanagedMods;
    private final Map<String, ImportMatch> matches = new HashMap<>();
    private boolean isScanning = false;

    public ModSyncScanPage(UIManager uiManager, PlayerRef playerRef, Store<EntityStore> store) {
        super(uiManager, playerRef, store);
        this.unmanagedMods = null; // Will trigger scan on build
    }

    public ModSyncScanPage(UIManager uiManager, PlayerRef playerRef, Store<EntityStore> store,
                           List<UnmanagedMod> unmanagedMods) {
        super(uiManager, playerRef, store);
        this.unmanagedMods = unmanagedMods;
    }

    @Override
    protected void buildPage(UICommandBuilder commands) {
        commands.append("Pages/ModSyncScan.ui");

        // If no mods scanned yet, trigger scan
        if (unmanagedMods == null && !isScanning) {
            performScan();
        }

        // Show scanning state
        if (isScanning || getUIState().isLoading()) {
            commands.set("#statusMessage.Text", "Scanning...");
            return;
        }

        // Show results
        if (unmanagedMods != null) {
            if (unmanagedMods.isEmpty()) {
                commands.set("#statusMessage.Text", "No unmanaged mods found");
            } else {
                commands.set("#summary.Text", unmanagedMods.size() + " unmanaged mod(s) found");
            }
        }

        // Status message
        String statusMessage = getUIState().getStatusMessage();
        if (statusMessage != null) {
            commands.set("#statusMessage.Text", statusMessage);
        }
    }

    private String getMatchStatusText(UnmanagedMod mod) {
        ImportMatch match = matches.get(mod.filePath().toString());
        if (match == null) {
            return "Not searched";
        }
        return switch (match.confidence()) {
            case EXACT -> "Exact match: " + (match.matchedEntry() != null ? match.matchedEntry().getName() : "?");
            case HIGH -> "High match: " + (match.matchedEntry() != null ? match.matchedEntry().getName() : "?");
            case LOW -> "Low match: " + (match.matchedEntry() != null ? match.matchedEntry().getName() : "?");
            case NONE -> "No match found";
        };
    }

    @Override
    protected void bindEvents(UIEventBuilder events) {
        events.addEventBinding(CustomUIEventBindingType.Activating, "#rescanBtn");
        events.addEventBinding(CustomUIEventBindingType.Activating, "#importAllBtn");
        events.addEventBinding(CustomUIEventBindingType.Activating, "#backBtn");
    }

    @Override
    protected void handleAction(String action, ModSyncEventData eventData) {
        String normalizedAction = action.startsWith("#") ? action.substring(1) : action;

        switch (normalizedAction) {
            case "rescanBtn", "rescan" -> performScan();
            case "find_match" -> findMatch(eventData.param1);
            case "import" -> importMod(eventData.param1);
            case "importAllBtn", "import_all" -> importAllWithHighConfidence();
            case "backBtn", "back" -> navigateBack();
            default -> super.handleAction(action, eventData);
        }
    }

    private void performScan() {
        isScanning = true;
        matches.clear();

        // Scan synchronously without refresh during build
        ModScanService scanService = getModSync().getScanService();
        this.unmanagedMods = scanService.scanForUnmanagedMods();
        isScanning = false;
        // Don't call refresh() here - buildPage() will handle the display
    }

    private void findMatch(String filename) {
        if (filename == null || unmanagedMods == null) return;

        UnmanagedMod mod = findModByFilename(filename);
        if (mod == null) return;

        getUIState().startLoading();
        refresh();

        getModSync().getScanService().findMatch(mod)
                .thenAccept(match -> {
                    matches.put(mod.filePath().toString(), match);
                    getUIState().stopLoading();
                    refresh();
                })
                .exceptionally(ex -> {
                    getUIState().stopLoading();
                    getUIState().setStatus("Error finding match: " + ex.getMessage(), UIState.StatusType.ERROR);
                    refresh();
                    return null;
                });
    }

    private void importMod(String filename) {
        if (filename == null || unmanagedMods == null) return;

        UnmanagedMod mod = findModByFilename(filename);
        if (mod == null) return;

        ImportMatch match = matches.get(mod.filePath().toString());
        if (match == null || match.confidence() == ImportMatchConfidence.NONE || match.matchedEntry() == null) {
            getUIState().setStatus("Find a match first before importing", UIState.StatusType.WARNING);
            refresh();
            return;
        }

        getUIState().startLoading();
        refresh();

        try {
            getModSync().getScanService().importWithEntry(mod, match.matchedEntry(), DEFAULT_IMPORT_SOURCE);
            unmanagedMods = new ArrayList<>(unmanagedMods);
            unmanagedMods.removeIf(m -> m.filePath().toString().equals(mod.filePath().toString()));
            matches.remove(mod.filePath().toString());
            getUIState().stopLoading();
            getUIState().setStatus("Imported: " + match.matchedEntry().getName(), UIState.StatusType.SUCCESS);
            refresh();
        } catch (Exception e) {
            getUIState().stopLoading();
            getUIState().setStatus("Error importing: " + e.getMessage(), UIState.StatusType.ERROR);
            refresh();
        }
    }

    private void importAllWithHighConfidence() {
        if (unmanagedMods == null || unmanagedMods.isEmpty()) return;

        List<UnmanagedMod> toImport = new ArrayList<>();
        for (UnmanagedMod mod : unmanagedMods) {
            ImportMatch match = matches.get(mod.filePath().toString());
            if (match != null && match.matchedEntry() != null
                    && (match.confidence() == ImportMatchConfidence.EXACT
                    || match.confidence() == ImportMatchConfidence.HIGH)) {
                toImport.add(mod);
            }
        }

        if (toImport.isEmpty()) {
            getUIState().setStatus("No mods with high confidence matches to import", UIState.StatusType.WARNING);
            refresh();
            return;
        }

        getUIState().startLoading();
        refresh();

        int imported = 0;
        for (UnmanagedMod mod : toImport) {
            ImportMatch match = matches.get(mod.filePath().toString());
            try {
                getModSync().getScanService().importWithEntry(mod, match.matchedEntry(), DEFAULT_IMPORT_SOURCE);
                imported++;
            } catch (Exception e) {
                // Continue with others
            }
        }

        // Refresh the list
        performScan();
        getUIState().setStatus("Imported " + imported + " mod(s)", UIState.StatusType.SUCCESS);
    }

    @Nullable
    private UnmanagedMod findModByFilename(String filename) {
        if (unmanagedMods == null) return null;
        return unmanagedMods.stream()
                .filter(m -> m.filePath().getFileName().toString().equals(filename))
                .findFirst()
                .orElse(null);
    }
}

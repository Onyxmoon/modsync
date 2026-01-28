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
import de.onyxmoon.modsync.ui.ModSyncUIManager;
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

    public ModSyncScanPage(ModSyncUIManager uiManager, PlayerRef playerRef, Store<EntityStore> store) {
        super(uiManager, playerRef, store);
        this.unmanagedMods = null; // Will trigger scan on build
    }

    public ModSyncScanPage(ModSyncUIManager uiManager, PlayerRef playerRef, Store<EntityStore> store,
                           List<UnmanagedMod> unmanagedMods) {
        super(uiManager, playerRef, store);
        this.unmanagedMods = unmanagedMods;
    }

    @Override
    protected void buildPage(UICommandBuilder commands) {
        commands.append("Custom/Pages/ModSyncScan.ui");

        commands.set("#title", "Scan Unmanaged Mods");

        // If no mods scanned yet, trigger scan
        if (unmanagedMods == null && !isScanning) {
            performScan();
        }

        // Show scanning state
        if (isScanning || getUIState().isLoading()) {
            commands.set("#loading", "true");
            commands.set("#status_message", "Scanning...");
            return;
        }

        // Show results
        if (unmanagedMods != null) {
            if (unmanagedMods.isEmpty()) {
                commands.set("#no_results", "true");
                commands.set("#status_message", "No unmanaged mods found");
            } else {
                populateModList(commands);
                commands.set("#summary", unmanagedMods.size() + " unmanaged mod(s) found");
            }
        }

        // Status message
        String statusMessage = getUIState().getStatusMessage();
        if (statusMessage != null) {
            commands.set("#status_message", statusMessage);
            commands.set("#status_type", getUIState().getStatusType().name().toLowerCase());
        }
    }

    private void populateModList(UICommandBuilder commands) {
        StringBuilder listHtml = new StringBuilder();

        for (UnmanagedMod mod : unmanagedMods) {
            String identifier = mod.getIdentifierString() != null ? mod.getIdentifierString() : "Unknown";
            String matchStatus = getMatchStatusText(mod);

            listHtml.append(String.format(
                    "<mod id=\"%s\" filename=\"%s\" identifier=\"%s\" size=\"%d\" match=\"%s\"/>",
                    escapeXml(mod.filePath().getFileName().toString()),
                    escapeXml(mod.filePath().getFileName().toString()),
                    escapeXml(identifier),
                    mod.fileSize(),
                    escapeXml(matchStatus)
            ));
        }

        commands.set("#mod_list", listHtml.toString());
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
        events.addEventBinding(CustomUIEventBindingType.Activating, "#rescan_btn");
        events.addEventBinding(CustomUIEventBindingType.Activating, "#import_all_btn");
        events.addEventBinding(CustomUIEventBindingType.Activating, "#back_btn");
    }

    @Override
    protected void handleAction(String action, ModSyncEventData eventData) {
        switch (action) {
            case "rescan" -> performScan();
            case "find_match" -> findMatch(eventData.param1);
            case "import" -> importMod(eventData.param1);
            case "import_all" -> importAllWithHighConfidence();
            default -> super.handleAction(action, eventData);
        }
    }

    private void performScan() {
        isScanning = true;
        unmanagedMods = null;
        matches.clear();
        refresh();

        ModScanService scanService = getModSync().getScanService();
        this.unmanagedMods = scanService.scanForUnmanagedMods();
        isScanning = false;
        refresh();
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

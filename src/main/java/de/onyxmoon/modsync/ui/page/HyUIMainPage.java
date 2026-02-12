package de.onyxmoon.modsync.ui.page;

import au.ellie.hyui.builders.GroupBuilder;
import au.ellie.hyui.builders.LabelBuilder;
import au.ellie.hyui.builders.PageBuilder;
import au.ellie.hyui.html.TemplateProcessor;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.onyxmoon.modsync.BuildInfo;
import de.onyxmoon.modsync.api.model.InstalledState;
import de.onyxmoon.modsync.api.model.ManagedMod;
import de.onyxmoon.modsync.api.model.ManagedModRegistry;
import de.onyxmoon.modsync.ui.UIManager;
import de.onyxmoon.modsync.ui.state.UIState;

import java.util.ArrayList;
import java.util.List;

/**
 * Main page showing the list of managed mods using HyUI with Template Processor.
 */
public class HyUIMainPage extends HyUIBasePage {

    private List<ManagedMod> allMods = new ArrayList<>();

    public HyUIMainPage(UIManager uiManager, PlayerRef playerRef, Store<EntityStore> store) {
        super(uiManager, playerRef, store);
    }

    /**
     * Wrapper class for template processor to expose mod data with index.
     */
    public record ModItemData(
            int index,
            String name,
            String identifier,
            String version,
            boolean installed,
            String statusColor,
            String statusText
    ) {}

    @Override
    public void open() {
        ManagedModRegistry registry = getModSync().getManagedModStorage().getRegistry();
        allMods = registry.getAll();

        int installed = registry.getInstalled().size();
        int total = allMods.size();

        // Create mod items for template
        List<ModItemData> modItems = new ArrayList<>();
        for (int i = 0; i < allMods.size(); i++) {
            ManagedMod mod = allMods.get(i);
            boolean isInstalled = mod.isInstalled();
            modItems.add(new ModItemData(
                    i,
                    escapeHtml(mod.getName()),
                    escapeHtml(mod.getIdentifierString().orElse("-")),
                    mod.getInstalledState().map(InstalledState::getInstalledVersionNumber).orElse("-"),
                    isInstalled,
                    isInstalled ? "#55aa55" : "#aa5555",
                    isInstalled ? "Installed" : "Not Installed"
            ));
        }

        // Process template
        TemplateProcessor template = new TemplateProcessor()
                .setVariable("version", BuildInfo.VERSION)
                .setVariable("installed", installed)
                .setVariable("total", total)
                .setVariable("modCount", allMods.size())
                .setVariable("mods", modItems)
                .setVariable("hasMods", !allMods.isEmpty());

        String html = template.process(TEMPLATE);

        PageBuilder builder = pageBuilder()
                .fromHtml(html)
                // Toolbar buttons
                .addEventListener("addBtn", CustomUIEventBindingType.Activating,
                        (data, ctx) -> uiManager.openAddModPage(playerRef, store))
                .addEventListener("scanBtn", CustomUIEventBindingType.Activating,
                        (data, ctx) -> uiManager.openScanPage(playerRef, store))
                .addEventListener("settingsBtn", CustomUIEventBindingType.Activating,
                        (data, ctx) -> uiManager.openConfigPage(playerRef, store))
                // Search input - filter visibility without rebuilding
                .addEventListener("searchInput", CustomUIEventBindingType.ValueChanged,
                        (data, ctx) -> {
                            String filter = ctx.getValueAs("searchInput", String.class).orElse("").toLowerCase();

                            int visibleCount = 0;
                            for (int i = 0; i < allMods.size(); i++) {
                                ManagedMod mod = allMods.get(i);
                                boolean matches = filter.isEmpty() ||
                                        mod.getName().toLowerCase().contains(filter) ||
                                        mod.getIdentifierString().orElse("").toLowerCase().contains(filter);

                                final int index = i;
                                ctx.getById("modItem" + index, GroupBuilder.class)
                                        .ifPresent(g -> g.withVisible(matches));

                                if (matches) visibleCount++;
                            }

                            // Update count label
                            final int count = visibleCount;
                            ctx.getById("modsHeader", LabelBuilder.class)
                                    .ifPresent(l -> l.withText("MODS (" + count + ")"));

                            ctx.updatePage(false);
                        });

        // Add listeners for each mod item
        for (int i = 0; i < allMods.size(); i++) {
            final ManagedMod mod = allMods.get(i);

            // Detail button
            builder.addEventListener("detail" + i, CustomUIEventBindingType.Activating,
                    (data, ctx) -> uiManager.openModDetailPage(playerRef, store, mod));

            // Install/Uninstall button
            builder.addEventListener("toggle" + i, CustomUIEventBindingType.Activating,
                    (data, ctx) -> {
                        if (mod.isInstalled()) {
                            uninstallMod(mod);
                        } else {
                            installMod(mod);
                        }
                    });

            // Remove button
            builder.addEventListener("remove" + i, CustomUIEventBindingType.Activating,
                    (data, ctx) -> removeMod(mod));
        }

        builder.open(store);
    }

    private void installMod(ManagedMod mod) {
        getUIState().setStatus("Installing " + mod.getName() + "...", UIState.StatusType.INFO);
        // TODO: Implement async install
        open();
    }

    private void uninstallMod(ManagedMod mod) {
        getModSync().getDownloadService().deleteMod(mod)
                .thenAccept(deleted -> {
                    if (deleted) {
                        ManagedMod updatedMod = mod.toBuilder().installedState(null).build();
                        getModSync().getManagedModStorage().updateMod(updatedMod);
                        getUIState().setStatus("Uninstalled: " + mod.getName(), UIState.StatusType.SUCCESS);
                    } else {
                        getUIState().setStatus("Could not uninstall", UIState.StatusType.ERROR);
                    }
                    open();
                });
    }

    private void removeMod(ManagedMod mod) {
        if (mod.isInstalled()) {
            getModSync().getDownloadService().deleteMod(mod)
                    .thenAccept(deleted -> {
                        getModSync().getManagedModStorage().removeMod(mod.getSourceId());
                        getUIState().setStatus("Removed: " + mod.getName(), UIState.StatusType.SUCCESS);
                        open();
                    });
        } else {
            getModSync().getManagedModStorage().removeMod(mod.getSourceId());
            getUIState().setStatus("Removed: " + mod.getName(), UIState.StatusType.SUCCESS);
            open();
        }
    }

    private static final String TEMPLATE = """
        <div class="page-overlay">
            <div class="decorated-container" data-hyui-title="ModSync" style="anchor-width: 800; anchor-height: 600;">
                <div class="container-contents" style="layout: top; padding: 16;">
                    <!-- Toolbar -->
                    <div style="layout: left; anchor-height: 36; anchor-bottom: 12;">
                        <button id="addBtn" style="anchor-width: 120; anchor-height: 32;">Add Mod</button>
                        <button id="scanBtn" style="anchor-width: 100; anchor-height: 32; anchor-left: 8;">Scan</button>
                        <button id="settingsBtn" style="anchor-width: 120; anchor-height: 32; anchor-left: 8;">Settings</button>
                        <div style="flex-weight: 1;"></div>
                        <p style="font-size: 13; color: #aaaaaa;">{{$installed}} / {{$total}} installed</p>
                    </div>

                    <!-- Search -->
                    <div style="anchor-height: 36; anchor-bottom: 12;">
                        <input type="text" id="searchInput" placeholder="Filter by name or identifier..." style="anchor-height: 32;"/>
                    </div>

                    <!-- Mods Header -->
                    <p id="modsHeader" style="font-size: 11; color: #888888; anchor-height: 20;">
                        MODS ({{$modCount}})
                    </p>

                    <!-- Mod List -->
                    <div style="flex-weight: 1; layout: top-scrolling;">
                        {{#if hasMods}}
                        {{#each mods}}
                        <div id="modItem{{$index}}" style="layout: left; anchor-height: 70; anchor-bottom: 6; padding: 8;">
                            <!-- Icon placeholder -->
                            <div style="anchor-width: 54; anchor-height: 54; background-color: #2a3a4a;"></div>

                            <!-- Info -->
                            <div style="flex-weight: 1; layout: top; padding-left: 16; padding-top: 2;">
                                <p style="font-weight: bold; font-size: 14;">{{$name}}</p>
                                <p style="font-size: 11; color: #aaaaaa;">{{$identifier}}</p>
                                <div style="layout: left; anchor-height: 16;">
                                    <p style="font-size: 11; color: #888888;">v{{$version}}</p>
                                    <p style="font-size: 11; color: {{$statusColor}}; padding-left: 12;">{{$statusText}}</p>
                                </div>
                            </div>

                            <!-- Buttons (horizontal) -->
                            <div style="layout: left; anchor-height: 26; padding-right: 8;">
                                <button id="detail{{$index}}" style="anchor-width: 80; anchor-height: 26;">Details</button>
                                {{#if installed}}
                                <button id="toggle{{$index}}" style="anchor-width: 100; anchor-height: 26; anchor-left: 8;">Uninstall</button>
                                {{else}}
                                <button id="toggle{{$index}}" style="anchor-width: 80; anchor-height: 26; anchor-left: 8;">Install</button>
                                {{/if}}
                                <button id="remove{{$index}}" style="anchor-width: 80; anchor-height: 26; anchor-left: 8;">Remove</button>
                            </div>
                        </div>
                        {{/each}}
                        {{else}}
                        <p style="padding-top: 40; color: #888888;">
                            No mods found. Use 'Add' to add your first mod.
                        </p>
                        {{/if}}
                    </div>

                    <!-- Footer with version -->
                    <div style="layout: left; anchor-height: 20; padding-top: 8;">
                        <div style="flex-weight: 1;"></div>
                        <p style="font-size: 10; color: #666666;">v{{$version}}</p>
                    </div>
                </div>
            </div>
        </div>
        """;
}

package de.onyxmoon.modsync.ui.page;

import au.ellie.hyui.builders.GroupBuilder;
import au.ellie.hyui.builders.LabelBuilder;
import au.ellie.hyui.builders.PageBuilder;
import au.ellie.hyui.builders.TextFieldBuilder;
import au.ellie.hyui.events.UIContext;
import au.ellie.hyui.html.TemplateProcessor;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.onyxmoon.modsync.BuildInfo;
import de.onyxmoon.modsync.api.model.InstalledState;
import de.onyxmoon.modsync.api.model.ManagedMod;
import de.onyxmoon.modsync.api.model.ManagedModRegistry;
import de.onyxmoon.modsync.api.model.provider.ModEntry;
import de.onyxmoon.modsync.service.ProviderFetchService;
import de.onyxmoon.modsync.storage.model.PluginConfig;
import de.onyxmoon.modsync.ui.UIManager;
import de.onyxmoon.modsync.ui.state.UIState;
import de.onyxmoon.modsync.util.CommandUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Main ModSync page with tabs (Mods, Scan, Settings) and Add Mod overlay.
 */
public class HyUIModSyncPage extends HyUIBasePage {

    private List<ManagedMod> allMods = new ArrayList<>();

    // Add Mod overlay state
    private boolean addModOverlayVisible = false;
    private String addModUrl = "";
    private ModEntry fetchedMod = null;
    private String fetchedProvider = null;
    private String addModError = null;

    public HyUIModSyncPage(UIManager uiManager, PlayerRef playerRef, Store<EntityStore> store) {
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

        // Config data
        PluginConfig config = getModSync().getConfigStorage().getConfig();

        // Process template
        TemplateProcessor template = new TemplateProcessor()
                .setVariable("version", BuildInfo.VERSION)
                .setVariable("installed", installed)
                .setVariable("total", total)
                .setVariable("modCount", allMods.size())
                .setVariable("mods", modItems)
                .setVariable("hasMods", !allMods.isEmpty())
                // Add Mod overlay state
                .setVariable("addModOverlayVisible", addModOverlayVisible)
                .setVariable("addModUrl", addModUrl)
                .setVariable("hasFetchedMod", fetchedMod != null)
                .setVariable("fetchedModName", fetchedMod != null ? fetchedMod.getName() : "")
                .setVariable("fetchedModSlug", fetchedMod != null ? fetchedMod.getSlug() : "")
                .setVariable("fetchedProvider", fetchedProvider != null ? fetchedProvider : "")
                .setVariable("addModError", addModError != null ? addModError : "")
                .setVariable("hasAddModError", addModError != null)
                // Settings
                .setVariable("updateMode", config.getUpdateMode().name())
                .setVariable("releaseChannel", config.getDefaultReleaseChannel().getDisplayName());

        String html = template.process(TEMPLATE);

        PageBuilder builder = pageBuilder()
                .fromHtml(html)
                // === MODS TAB ===
                .addEventListener("addBtn", CustomUIEventBindingType.Activating,
                        (data, ctx) -> {
                            addModOverlayVisible = true;
                            addModUrl = "";
                            fetchedMod = null;
                            fetchedProvider = null;
                            addModError = null;
                            open();
                        })
                // Search input
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

                            final int count = visibleCount;
                            ctx.getById("modsCount", LabelBuilder.class)
                                    .ifPresent(l -> l.withText("(" + count + ")"));

                            ctx.updatePage(false);
                        })
                // === SETTINGS TAB ===
                .addEventListener("reloadBtn", CustomUIEventBindingType.Activating,
                        (data, ctx) -> {
                            getModSync().getConfigStorage().reload();
                            getModSync().getManagedModStorage().reload();
                            getUIState().setStatus("Configuration reloaded", UIState.StatusType.SUCCESS);
                            open();
                        })
                .addEventListener("checkUpdateBtn", CustomUIEventBindingType.Activating,
                        (data, ctx) -> checkForUpdate());

        // === ADD MOD OVERLAY (only when visible) ===
        if (addModOverlayVisible) {
            builder.addEventListener("closeAddModBtn", CustomUIEventBindingType.Activating,
                            (data, ctx) -> {
                                addModOverlayVisible = false;
                                open();
                            })
                    .addEventListener("fetchBtn", CustomUIEventBindingType.Activating,
                            (data, ctx) -> onFetchClicked(ctx));

            // confirmAddBtn only exists when a mod has been fetched
            if (fetchedMod != null) {
                builder.addEventListener("confirmAddBtn", CustomUIEventBindingType.Activating,
                        (data, ctx) -> onConfirmAdd());
            }
        }

        // Add listeners for each mod item
        for (int i = 0; i < allMods.size(); i++) {
            final ManagedMod mod = allMods.get(i);

            builder.addEventListener("detail" + i, CustomUIEventBindingType.Activating,
                    (data, ctx) -> uiManager.openModDetailPage(playerRef, store, mod));

            builder.addEventListener("toggle" + i, CustomUIEventBindingType.Activating,
                    (data, ctx) -> {
                        if (mod.isInstalled()) {
                            uninstallMod(mod);
                        } else {
                            installMod(mod);
                        }
                    });

            builder.addEventListener("remove" + i, CustomUIEventBindingType.Activating,
                    (data, ctx) -> removeMod(mod));
        }

        builder.open(store);
    }

    private void onFetchClicked(UIContext ctx) {
        String url = ctx.getValueAs("addModUrlInput", String.class).orElse("");
        if (url.isBlank()) {
            addModError = "Please enter a URL";
            open();
            return;
        }

        addModUrl = url;
        addModError = null;

        ProviderFetchService fetchService = getModSync().getFetchService();
        fetchService.fetchFromUrl(url, providerName -> {})
                .thenAccept(result -> {
                    this.fetchedMod = result.modEntry();
                    this.fetchedProvider = result.provider().getDisplayName();
                    this.addModError = null;
                    open();
                })
                .exceptionally(ex -> {
                    this.addModError = CommandUtils.extractErrorMessage(ex);
                    this.fetchedMod = null;
                    open();
                    return null;
                });
    }

    private void onConfirmAdd() {
        if (fetchedMod == null || fetchedProvider == null) {
            addModError = "Please fetch mod info first";
            open();
            return;
        }

        if (getModSync().getManagedModStorage().getRegistry()
                .findBySourceId(fetchedMod.getModId()).isPresent()) {
            addModError = "This mod is already in your list";
            open();
            return;
        }

        ManagedMod newMod = ManagedMod.builder()
                .name(fetchedMod.getName())
                .slug(fetchedMod.getSlug())
                .source(fetchedProvider)
                .modId(fetchedMod.getModId())
                .addedAt(Instant.now())
                .addedViaUrl(addModUrl)
                .pluginType(fetchedMod.getPluginType())
                .build();

        getModSync().getManagedModStorage().addMod(newMod);

        addModOverlayVisible = false;
        fetchedMod = null;
        fetchedProvider = null;
        addModUrl = "";
        getUIState().setStatus("Added: " + newMod.getName(), UIState.StatusType.SUCCESS);
        open();
    }

    private void checkForUpdate() {
        getModSync().getSelfUpdateService().checkForUpgrade()
                .thenAccept(result -> {
                    if (result.hasUpdate()) {
                        getUIState().setStatus("Update available: " + result.latestVersion(), UIState.StatusType.INFO);
                    } else {
                        getUIState().setStatus("ModSync is up to date", UIState.StatusType.SUCCESS);
                    }
                    open();
                })
                .exceptionally(ex -> {
                    getUIState().setStatus("Error checking updates", UIState.StatusType.ERROR);
                    open();
                    return null;
                });
    }

    private void installMod(ManagedMod mod) {
        getUIState().setStatus("Installing " + mod.getName() + "...", UIState.StatusType.INFO);
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
        <div class="page-overlay" style="layout: Full;">
            <!-- Main Panel -->
            <div class="decorated-container" data-hyui-title="ModSync" style="anchor-width: 850; anchor-height: 600;">
                <div class="container-contents" style="layout: top; padding: 12;">
                    <!-- Tabs -->
                    <nav id="mainTabs" class="tabs"
                         data-tabs="mods:Mods:modsTab,settings:Settings:settingsTab"
                         data-selected="mods">
                    </nav>

                    <!-- MODS TAB -->
                    <div id="modsTab" class="tab-content" data-hyui-tab-id="mods" style="layout: top; flex-weight: 1;">
                        <!-- Toolbar -->
                        <div style="layout: left; anchor-height: 36; anchor-bottom: 8; padding-top: 8;">
                            <p style="font-size: 13; color: #aaaaaa;">{{$installed}} / {{$total}} installed</p>
                        </div>

                        <!-- Search -->
                        <div style="anchor-height: 36; anchor-bottom: 8;">
                            <input type="text" id="searchInput" placeholder="Filter by name or identifier..." style="anchor-height: 32;"/>
                        </div>

                        <!-- Mods Header -->
                        <div style="layout: left; anchor-height: 20;">
                            <p style="font-size: 11; color: #888888;">MODS </p>
                            <p id="modsCount" style="font-size: 11; color: #888888;">({{$modCount}})</p>
                        </div>

                        <!-- Mod List -->
                        <div style="flex-weight: 1; layout: top-scrolling;">
                            {{#if hasMods}}
                            {{#each mods}}
                            <div id="modItem{{$index}}" style="layout: left; anchor-height: 70; anchor-bottom: 6; padding: 8;">
                                <div style="anchor-width: 54; anchor-height: 54; background-color: #2a3a4a;"></div>
                                <div style="flex-weight: 1; layout: top; padding-left: 16; padding-top: 2;">
                                    <p style="font-weight: bold; font-size: 14;">{{$name}}</p>
                                    <p style="font-size: 11; color: #aaaaaa;">{{$identifier}}</p>
                                    <div style="layout: left; anchor-height: 16;">
                                        <p style="font-size: 11; color: #888888;">v{{$version}}</p>
                                        <p style="font-size: 11; color: {{$statusColor}}; padding-left: 12;">{{$statusText}}</p>
                                    </div>
                                </div>
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
                            <p style="padding-top: 40; color: #888888;">No mods found. Click 'Add Mod' to add your first mod.</p>
                            {{/if}}
                        </div>

                        <!-- Add Mod Button -->
                        <div style="anchor-height: 44; padding-top: 8;">
                            <button id="addBtn" style="anchor-height: 36;">Add Mod</button>
                        </div>
                    </div>

                    <!-- SETTINGS TAB -->
                    <div id="settingsTab" class="tab-content" data-hyui-tab-id="settings" style="layout: top; flex-weight: 1; padding-top: 12;">
                        <p style="font-size: 11; color: #888888; anchor-height: 20;">VERSION</p>
                        <p style="font-size: 14; anchor-height: 24;">{{$version}}</p>

                        <p style="font-size: 11; color: #888888; anchor-height: 20; padding-top: 16;">CONFIGURATION</p>
                        <div style="layout: left; anchor-height: 24;">
                            <p style="font-size: 12; anchor-width: 150;">Update Mode:</p>
                            <p style="font-size: 12; color: #aaaaaa;">{{$updateMode}}</p>
                        </div>
                        <div style="layout: left; anchor-height: 24;">
                            <p style="font-size: 12; anchor-width: 150;">Release Channel:</p>
                            <p style="font-size: 12; color: #aaaaaa;">{{$releaseChannel}}</p>
                        </div>

                        <p style="font-size: 11; color: #888888; anchor-height: 20; padding-top: 16;">ACTIONS</p>
                        <div style="layout: left; anchor-height: 40;">
                            <button id="reloadBtn" style="anchor-width: 150; anchor-height: 32;">Reload Config</button>
                            <button id="checkUpdateBtn" style="anchor-width: 150; anchor-height: 32; anchor-left: 8;">Check for Updates</button>
                        </div>
                    </div>

                    <!-- Footer -->
                    <div style="layout: left; anchor-height: 10; margin-top: 8;">
                        <div style="flex-weight: 1;"></div>
                        <p style="font-size: 10; color: #666666;">v{{$version}}</p>
                    </div>
                </div>
            </div>

            <!-- ADD MOD OVERLAY -->
            {{#if addModOverlayVisible}}
            <div style="layout: Full; background-color: #00000099;">
                <div class="container" style="anchor-width: 500; anchor-height: 350;">
                    <div class="container-contents" style="layout: top; padding: 16;">
                        <div style="layout: left; anchor-height: 30; anchor-bottom: 16;">
                            <p style="font-size: 16; font-weight: bold; flex-weight: 1;">Add Mod</p>
                            <button id="closeAddModBtn" style="anchor-width: 30; anchor-height: 26;">X</button>
                        </div>

                        <p style="font-size: 11; color: #888888; anchor-height: 20;">MOD URL</p>
                        <div style="layout: left; anchor-height: 36; anchor-bottom: 12;">
                            <input type="text" id="addModUrlInput" placeholder="https://curseforge.com/hytale/mods/..." style="flex-weight: 1; anchor-height: 32;"/>
                            <button id="fetchBtn" style="anchor-width: 80; anchor-height: 32; anchor-left: 8;">Fetch</button>
                        </div>

                        {{#if hasAddModError}}
                        <p style="font-size: 12; color: #aa5555; anchor-height: 24;">{{$addModError}}</p>
                        {{/if}}

                        {{#if hasFetchedMod}}
                        <p style="font-size: 11; color: #888888; anchor-height: 20; padding-top: 8;">FETCHED MOD</p>
                        <div style="layout: left; anchor-height: 20;">
                            <p style="font-size: 12; anchor-width: 80;">Name:</p>
                            <p style="font-size: 12; color: #55aa55;">{{$fetchedModName}}</p>
                        </div>
                        <div style="layout: left; anchor-height: 20;">
                            <p style="font-size: 12; anchor-width: 80;">Slug:</p>
                            <p style="font-size: 12; color: #aaaaaa;">{{$fetchedModSlug}}</p>
                        </div>
                        <div style="layout: left; anchor-height: 20;">
                            <p style="font-size: 12; anchor-width: 80;">Provider:</p>
                            <p style="font-size: 12; color: #aaaaaa;">{{$fetchedProvider}}</p>
                        </div>

                        <div style="flex-weight: 1;"></div>
                        <button id="confirmAddBtn" style="anchor-width: 150; anchor-height: 32;">Add to List</button>
                        {{/if}}
                    </div>
                </div>
            </div>
            {{/if}}
        </div>
        """;
}

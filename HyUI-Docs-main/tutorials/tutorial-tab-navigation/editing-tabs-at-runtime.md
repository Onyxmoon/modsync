# Editing Tabs at Runtime

This tutorial shows how to build tabs in HYUIML, fetch the navigation builder with `getById(...)`, and update a tab (including swapping the button builder) at runtime.

We will:

* Define tabs in HYUIML.
* Use `TabNavigationBuilder.getTab(...)` and `updateTab(...)`.
* Replace the button builder for a single tab.

{% stepper %}
{% step %}
### Base HYUIML

{% code title="base.html" %}
```html
<div class="page-overlay">
    <div class="decorated-container" data-hyui-title="Workshop">
        <div class="container-contents" style="layout-mode: Top; padding: 6;">
            <nav id="workshop-tabs" class="tabs"
                 data-tabs="blueprints:Blueprints:blueprints-content,materials:Materials:materials-content,tools:Tools:tools-content"
                 data-selected="blueprints">
            </nav>

            <div id="blueprints-content" class="tab-content" data-hyui-tab-id="blueprints">
                <p>Blueprint drafts live here.</p>
            </div>

            <div id="materials-content" class="tab-content" data-hyui-tab-id="materials">
                <p>Material stacks and salvage.</p>
            </div>

            <div id="tools-content" class="tab-content" data-hyui-tab-id="tools">
                <p>Workbench tools and kits.</p>
            </div>

            <button id="upgrade-tabs" class="secondary-button">Upgrade Materials Tab</button>
        </div>
    </div>
</div>
```
{% endcode %}
{% endstep %}

{% step %}
### Update a Tab (Label + Button Builder)

<pre class="language-java" data-title="update-tab.java"><code class="lang-java">PageBuilder page = PageBuilder.pageForPlayer(playerRef)
    .fromHtml(html);

page.addEventListener("upgrade-tabs", CustomUIEventBindingType.Activating, (data, ctx) -> {
    ctx.getById("workshop-tabs", TabNavigationBuilder.class).ifPresent(nav -> {
        TabNavigationBuilder.Tab existing = nav.getTab("materials");
        if (existing == null) {
            return;
        }

        <a data-footnote-ref href="#user-content-fn-1">ButtonBuilder customButton = ButtonBuilder.smallTertiaryTextButton();</a>

        TabNavigationBuilder.Tab updated = new TabNavigationBuilder.Tab(
                existing.id(),
                "Materials+",
                existing.contentId(),
                existing.selected(),
                customButton
        );

        nav.updateTab("materials", updated);
    });

    ctx.updatePage(true);
});

page.open(store);
</code></pre>

{% hint style="info" %}
Notes:

* `updateTab(...)` replaces the tab entry in the navigation list.
* `getTab(...)` returns a snapshot of the tab metadata you can reuse.
* `getAllTabs()` (or `getTabs()`) returns a copy of the current tab list if you need to inspect everything.
{% endhint %}
{% endstep %}
{% endstepper %}

[^1]: This could alternatively be a CustomButtonBuilder!

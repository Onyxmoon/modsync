# Using Tabs

This tutorial introduces tab navigation using both HYUIML and the builder API. We will build a simple **Workshop** page with tabs that swap content blocks.

We will:

* Define tabs in HYUIML with `data-tabs`.
* Link content with `data-hyui-tab-id`.

{% stepper %}
{% step %}
### HYUIML Tabs

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
        </div>
    </div>
</div>
```

This creates a tab bar and three content blocks. Only the selected tab content is visible.
{% endstep %}

{% step %}
### Builder Tabs

```java
TabNavigationBuilder tabs = TabNavigationBuilder.tabNavigation()
    .withId("workshop-tabs")
    .addTab("blueprints", "Blueprints", "blueprints-content")
    .addTab("materials", "Materials", "materials-content")
    .addTab("tools", "Tools", "tools-content")
    .withSelectedTab("blueprints");

TabContentBuilder blueprints = TabContentBuilder.tabContent()
    .withId("blueprints-content")
    .withTabId("blueprints")
    .addChild(LabelBuilder.label().withText("Blueprint drafts live here."));

TabContentBuilder materials = TabContentBuilder.tabContent()
    .withId("materials-content")
    .withTabId("materials")
    .addChild(LabelBuilder.label().withText("Material stacks and salvage."));

TabContentBuilder tools = TabContentBuilder.tabContent()
    .withId("tools-content")
    .withTabId("tools")
    .addChild(LabelBuilder.label().withText("Workbench tools and kits."));

PageBuilder.pageForPlayer(playerRef)
    .addElement(tabs)
    .addElement(blueprints)
    .addElement(materials)
    .addElement(tools)
    .open(store);
```
{% endstep %}
{% endstepper %}

{% hint style="info" %}
Notes:

* Tabs are matched by tab ID, so keep `data-hyui-tab-id` / `withTabId(...)` consistent.
* If you omit `data-selected` / `withSelectedTab(...)`, the first tab becomes selected.
{% endhint %}

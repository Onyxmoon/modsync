# Multiple Tab Navigators

This tutorial shows how to run more than one tab navigation on the same page, and how to make sure each `TabContentBuilder` is linked to the correct tab bar.

We will:

* Create two independent tab navigations.
* Link content to a specific navigation ID.
* Avoid tab ID collisions across navs.

## HYUIML with Multiple Navigations

{% tabs %}
{% tab title="HYUIML (HTML)" %}
{% code title="multiple-navs.html" %}
```html
<div class="page-overlay">
    <div class="decorated-container" data-hyui-title="Workshop + Map Room">
        <div class="container-contents" style="layout-mode: Top; padding: 6;">
            <nav id="workshop-tabs" class="tabs"
                 data-tabs="blueprints:Blueprints:blueprints-content,materials:Materials:materials-content"
                 data-selected="blueprints">
            </nav>

            <div id="blueprints-content" class="tab-content"
                 data-hyui-tab-id="blueprints"
                 data-hyui-tab-nav="workshop-tabs">
                <p>Blueprint drafts live here.</p>
            </div>

            <div id="materials-content" class="tab-content"
                 data-hyui-tab-id="materials"
                 data-hyui-tab-nav="workshop-tabs">
                <p>Material stacks and salvage.</p>
            </div>

            <nav id="map-tabs" class="tabs"
                 data-tabs="overview:Overview:overview-content,zones:Zones:zones-content"
                 data-selected="overview">
            </nav>

            <div id="overview-content" class="tab-content"
                 data-hyui-tab-id="overview"
                 data-hyui-tab-nav="map-tabs">
                <p>Map overview data.</p>
            </div>

            <div id="zones-content" class="tab-content"
                 data-hyui-tab-id="zones"
                 data-hyui-tab-nav="map-tabs">
                <p>Discovered zones.</p>
            </div>
        </div>
    </div>
</div>
```
{% endcode %}
{% endtab %}

{% tab title="Builder (Java)" %}
{% code title="MultipleTabNavigators.java" %}
```java
TabNavigationBuilder workshopTabs = TabNavigationBuilder.tabNavigation()
    .withId("workshop-tabs")
    .addTab("blueprints", "Blueprints", "blueprints-content")
    .addTab("materials", "Materials", "materials-content")
    .withSelectedTab("blueprints");

TabNavigationBuilder mapTabs = TabNavigationBuilder.tabNavigation()
    .withId("map-tabs")
    .addTab("overview", "Overview", "overview-content")
    .addTab("zones", "Zones", "zones-content")
    .withSelectedTab("overview");

TabContentBuilder blueprints = TabContentBuilder.tabContent()
    .withId("blueprints-content")
    .withTabId("blueprints")
    .withTabNavigationId("workshop-tabs")
    .addChild(LabelBuilder.label().withText("Blueprint drafts live here."));

TabContentBuilder materials = TabContentBuilder.tabContent()
    .withId("materials-content")
    .withTabId("materials")
    .withTabNavigationId("workshop-tabs")
    .addChild(LabelBuilder.label().withText("Material stacks and salvage."));

TabContentBuilder overview = TabContentBuilder.tabContent()
    .withId("overview-content")
    .withTabId("overview")
    .withTabNavigationId("map-tabs")
    .addChild(LabelBuilder.label().withText("Map overview data."));

TabContentBuilder zones = TabContentBuilder.tabContent()
    .withId("zones-content")
    .withTabId("zones")
    .withTabNavigationId("map-tabs")
    .addChild(LabelBuilder.label().withText("Discovered zones."));

PageBuilder.pageForPlayer(playerRef)
    .addElement(workshopTabs)
    .addElement(mapTabs)
    .addElement(blueprints)
    .addElement(materials)
    .addElement(overview)
    .addElement(zones)
    .open(store);
```
{% endcode %}
{% endtab %}
{% endtabs %}

{% hint style="info" %}
The `data-hyui-tab-nav` attribute (or `withTabNavigationId(...)` in the builder) ensures each content block listens to the correct tab navigation. You can reuse tab IDs across different navigators safely as long as each content block is bound to the appropriate nav.
{% endhint %}

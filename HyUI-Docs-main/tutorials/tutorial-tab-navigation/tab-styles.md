# Tab Styles

This tutorial focuses on styling tabs without swapping button builders. You will keep the default tab buttons and apply different styles for selected and unselected states.

We will:

* Set `selectedTabStyle` and `unselectedTabStyle`.
* Keep the default button builders intact.
* Use style references from UI documents.





{% code title="TabsStyling.java" %}
```java
HyUIStyle selectedStyle = new HyUIStyle()
    .withStyleReference("Common.ui", "DefaultTextButtonStyle");

HyUIStyle unselectedStyle = new HyUIStyle()
    .withStyleReference("Common.ui", "SecondaryTextButtonStyle");

TabNavigationBuilder tabs = TabNavigationBuilder.tabNavigation()
    .withId("workshop-tabs")
    .addTab("blueprints", "Blueprints", "blueprints-content")
    .addTab("materials", "Materials", "materials-content")
    .addTab("tools", "Tools", "tools-content")
    .withSelectedTab("blueprints")
    .withSelectedTabStyle(selectedStyle)
    .withUnselectedTabStyle(unselectedStyle);

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
{% endcode %}

{% hint style="info" %}
This keeps the default button builder intact and only swaps styles.
{% endhint %}

{% hint style="info" %}
You can point the style references to any styles found within `Common.ui` on the server.
{% endhint %}

{% hint style="warning" %}
You can optionally change the style manually, but be careful - these are not custom buttons (you can use these for tab navigation!). Swapping builders is different from applying style references to the default button builder.
{% endhint %}

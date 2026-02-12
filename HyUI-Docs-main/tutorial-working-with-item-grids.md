# Tutorial: Working with Item Grids

This tutorial builds a **Salvage Board** page that shows loot slots and reacts to slot interactions. It is similar in structure to the Bounty Board examples, but focused on `ItemGrid` events.

{% stepper %}
{% step %}
### Build a Salvage Board ItemGrid

We will build a simple page with a grid of salvaged items and a short summary label using HYUIML. The item grid and its slots are declared in markup so the structure is clear at a glance.

<pre class="language-java"><code class="lang-java">String html = """
    &#x3C;div class="page-overlay">
        &#x3C;div class="decorated-container" data-hyui-title="Salvage Board">
            &#x3C;div class="container-contents" style="layout-mode: Top; padding: 6;">
                &#x3C;p id="summary">Drop items to salvage. Click a slot for details.&#x3C;/p>

                &#x3C;div id="salvage-grid"
                     class="item-grid"
                     data-hyui-slots-per-row="4"
                     data-hyui-are-items-draggable="true">
                    &#x3C;div class="item-grid-slot"
                         data-hyui-item-id="Ore_Mithril"
                         data-hyui-quantity="12">&#x3C;/div>
                    &#x3C;div class="item-grid-slot"
                         data-hyui-item-id="Ore_Mithril"
                         data-hyui-quantity="1">&#x3C;/div>
                    <a data-footnote-ref href="#user-content-fn-1">&#x3C;div class="item-grid-slot">&#x3C;/div></a>
                &#x3C;/div>
            &#x3C;/div>
        &#x3C;/div>
    &#x3C;/div>
    """;

PageBuilder page = PageBuilder.pageForPlayer(playerRef)
    .fromHtml(html);

page.open(store);
</code></pre>

This gives you a clean grid with a mix of filled and empty slots. Now we can listen for slot interactions.
{% endstep %}

{% step %}
### Listen to Slot Events and Use Payloads

Override the grid contents with `getById(...)` and `addSlot(...)`, then listen for a click and a drop event. The listeners receive strongly-typed payloads that match the event.

<pre class="language-java"><code class="lang-java">PageBuilder page = PageBuilder.pageForPlayer(playerRef)
    .fromHtml(html);

<a data-footnote-ref href="#user-content-fn-2">page.getById(</a>"salvage-grid", ItemGridBuilder.class).ifPresent(grid -> {
    grid.addSlot(new ItemGridSlot(new ItemStack("Ore_Gold", 25)));
    grid.addSlot(new ItemGridSlot(new ItemStack("Ore_Iron", 25)));
    grid.addSlot(new ItemGridSlot(new ItemStack("Tool_Pickaxe_Crude", 1)));
});

page.open(store);
</code></pre>

```java
PageBuilder page = PageBuilder.pageForPlayer(playerRef)
    .fromHtml(html);

page.addEventListener("salvage-grid", CustomUIEventBindingType.SlotClicking, SlotClickingEventData.class, (slot, ctx) -> {
        Integer index = slot.getSlotIndex();
        ctx.getById("summary", LabelBuilder.class).ifPresent(label -> {
            label.withText("Clicked slot index: " + index);
        });
        ctx.updatePage(false);
    });
page.addEventListener("salvage-grid", CustomUIEventBindingType.Dropped, DroppedEventData.class, (drop, ctx) -> {
        ctx.getById("summary", LabelBuilder.class).ifPresent(label -> {
            label.withText("Dropped " + drop.getItemStackId() + " x" + drop.getItemStackQuantity());
        });
        ctx.updatePage(false);
    });

page.open(store);
```
{% endstep %}

{% step %}
### Update, Remove and Read Slots

Once the grid has slots, you can update, remove, or query them by index (zero-based). `getSlots()` returns an unmodifiable snapshot, so use `updateSlot(...)` or `removeSlot(...)` to mutate the grid.

```java
page.getById("salvage-grid", ItemGridBuilder.class).ifPresent(grid -> {
    ItemGridSlot replacement = new ItemGridSlot(new ItemStack("Soil_Dirt_Tilled", 8));
    grid.updateSlot(replacement, 1); // update slot at index 1

    grid.removeSlot(2); // remove slot at index 2

    ItemGridSlot firstSlot = grid.getSlot(0);
    List<ItemGridSlot> currentSlots = grid.getSlots();
});
```

```java
PageBuilder page = PageBuilder.pageForPlayer(playerRef)
    .fromHtml(html);

page.addEventListener("salvage-grid", CustomUIEventBindingType.SlotClicking, SlotClickingEventData.class, (slot, ctx) -> {
        Integer index = slot.getSlotIndex();
        ctx.getById("summary", LabelBuilder.class).ifPresent(label -> {
            label.withText("Clicked slot index: " + index);
        });
        ctx.updatePage(false);
    });

page.open(store);
```


{% endstep %}

{% step %}
### Drag and Drop - Properly Implemented

When a drop happens, you can grab the `ItemGridBuilder` and update slots in-place. The example below moves the source slot to the destination and clears the source. It also shows how to update a slot's quantity.

```java
page.addEventListener("salvage-grid", CustomUIEventBindingType.Dropped, DroppedEventData.class, (drop, ctx) -> {
    ctx.getById("salvage-grid", ItemGridBuilder.class).ifPresent(grid -> {
        Integer sourceIndex = drop.SourceSlotId();
        Integer targetIndex = drop.getSlotIndex();

        if (sourceIndex == null || targetIndex == null) {
            return;
        }

        ItemGridSlot sourceSlot = grid.getSlot(sourceIndex);
        if (sourceSlot == null) {
            return;
        }

        // Example: move the dragged slot to the drop target.
        grid.updateSlot(sourceSlot, targetIndex);
        grid.updateSlot(new ItemGridSlot(), sourceIndex);

        // Example: adjust quantity on the target slot after the move.
        ItemGridSlot updatedTarget = new ItemGridSlot(new ItemStack(
                drop.getItemStackId(),
                Math.max(1, drop.getItemStackQuantity() - 1)
        ));
        grid.updateSlot(updatedTarget, targetIndex);
    });

    ctx.updatePage(false);
});
```


{% endstep %}
{% endstepper %}

{% hint style="info" %}
Notes:

* `SlotClickingEventData` is only passed to `CustomUIEventBindingType.SlotClicking` listeners.
* `DroppedEventData` is only passed to `CustomUIEventBindingType.Dropped` listeners.
* The grid uses `data-hyui-are-items-draggable="true"` so drag events will fire.
* See a complete list of concrete event data types and their events [here](item-grid-event-data.md).
{% endhint %}

[^1]: This creates an empty slot.

[^2]: Optionally, we can add content to the itemgrid when we build the page.

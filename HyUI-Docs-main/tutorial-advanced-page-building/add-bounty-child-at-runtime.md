# Add Bounty (Child) at Runtime

Now we are going to mutate the page while it is open by adding a new bounty card. This is the step where your UI starts feeling alive instead of static.

We will:

* Add an "Add Bounty" button.
* Create a `GroupBuilder` card on click.
* Append it with `.addChild(...)`.
* Rebuild with `ctx.updatePage(true)`.

{% stepper %}
{% step %}
### Add the Button in HYUIML

Add a button next to the filters. This makes the runtime change feel intentional (and chaotic).

Edit your filters layout, add some horizontal padding (the second number). Or you could just make a style class for this... (if you know how to!)

```html
<div id="filters" style="layout-mode: Left;">
    <select id="region" data-hyui-showlabel="true" value="Forest" style="padding: 0 10;">
        <option value="Forest">Forest</option>
        <option value="Desert">Desert</option>
        <option value="Tundra">Tundra</option>
    </select>

    <input id="minLevel" type="number" value="1" style="anchor-width: 60; padding: 0 10;" />

    <button id="toggle-mode" style="padding: 0 10;" class="secondary-button">Compact View</button>
    <button id="add-bounty" style="padding: 0 10;" class="secondary-button">Add Bounty</button>
</div>
```
{% endstep %}

{% step %}
### Build a Card Programmatically

We will create a helper method so each new bounty card looks the same.

```java
private static GroupBuilder buildBountyCard(String title, int level) {
    return GroupBuilder.group()
        .withLayoutMode("Left")
        .addChild(LabelBuilder.label()
            .withText(title)
            .withFlexWeight(2)
        )
        .addChild(LabelBuilder.label()
            .withText("Lvl " + level)
            .withFlexWeight(1)
        )
        .addChild(ButtonBuilder.smallTertiaryTextButton()
            .withText("Track")
        );
}
```
{% endstep %}

{% step %}
### Add the Child and Update the Page

```java
AtomicInteger counter = new AtomicInteger(3);

PageBuilder.pageForPlayer(playerRef)
    .fromHtml(html)
    .addEventListener("add-bounty", CustomUIEventBindingType.Activating, (ignored, ctx) -> {
        int next = counter.incrementAndGet();
        String title = "Urgent Bounty #" + next;
        int level = 2 + (next % 6);

        ctx.getById("list", GroupBuilder.class).ifPresent(list -> {
            list.addChild(buildBountyCard(title, level));
        });

        ctx.getById("summary", LabelBuilder.class).ifPresent(label -> {
            label.withText("Showing " + next + " bounties");
        });

        ctx.updatePage(true);
    })
    .open(store);
```

Yes, we just changed the structure of the page after it opened. No, you are not dreaming.
{% endstep %}

{% step %}
### A Couple of Gotchas

* If you add a lot of elements, the page rebuild can get heavier. Keep the list in a scrollable container (`TopScrolling`) if it grows big.
* If you need to _remove_ elements, you should... not, because HyUI doesn't have that quite yet. Use the template processor (more on that later!).
{% endstep %}
{% endstepper %}

Next: we will switch gears and learn how to use the **Template Processor** to generate the same page from data. No runtime editing in that one; pure setup basics.

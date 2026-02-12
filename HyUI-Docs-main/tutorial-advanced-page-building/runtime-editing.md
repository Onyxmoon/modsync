# Runtime Editing

{% stepper %}
{% step %}
### Base HYUIML for the Bounty Board

We will load this as a string for now. Use the standard `page-overlay` and `container` structure so the page looks like a proper HyUI page. Optionally, load it from file!

```java
String html = """
    <div class="page-overlay">
        <div class="decorated-container" data-hyui-title="Bounty Board" style="anchor-width: 900; anchor-height: 600;">
            <div class="container-contents" style="layout-mode: Top;">
                <div id="filters" style="layout-mode: Left; padding: 6;">
                    <select id="region" data-hyui-showlabel="true" value="Forest">
                        <option value="Forest">Forest</option>
                        <option value="Desert">Desert</option>
                        <option value="Tundra">Tundra</option>
                    </select>

                    <input id="minLevel" type="number" value="1" style="anchor-width: 60;" />

                    <button id="toggle-mode" class="secondary-button">Compact View</button>
                </div>

                <p id="summary" style="padding: 4;">Showing 3 bounties</p>

                <div id="list" style="layout-mode: Top; padding: 6;">
                    <div class="bounty-card" style="layout-mode: Left; padding: 4;">
                        <p style="flex-weight: 2;">Slime Cleanup</p>
                        <p style="flex-weight: 1;">Lvl 1</p>
                        <button class="small-tertiary-button">Track</button>
                    </div>

                    <div class="bounty-card" style="layout-mode: Left; padding: 4;">
                        <p style="flex-weight: 2;">Bandit Camp</p>
                        <p style="flex-weight: 1;">Lvl 4</p>
                        <button class="small-tertiary-button">Track</button>
                    </div>

                    <div class="bounty-card" style="layout-mode: Left; padding: 4;">
                        <p style="flex-weight: 2;">Wisp Hunt</p>
                        <p style="flex-weight: 1;">Lvl 7</p>
                        <button class="small-tertiary-button">Track</button>
                    </div>
                </div>
            </div>
        </div>
    </div>
    """;
```
{% endstep %}

{% step %}
### Runtime Editing: Toggle Layout + Update Summary

We want the **Compact View** button to flip the list between `Top` and `LeftCenterWrap` layouts, and update the summary text. We also want the number field to change the summary as you type.

The trick is:

* Use `ctx.getById("list", GroupBuilder.class)` to grab the list container.
* Change its layout mode.
* Update the summary label.
* Call `ctx.updatePage(true)`.

```java
AtomicBoolean compact = new AtomicBoolean(false);

PageBuilder.pageForPlayer(playerRef)
    .fromHtml(html)
    .addEventListener("toggle-mode", CustomUIEventBindingType.Activating, (ignored, ctx) -> {
        boolean newState = !compact.get();
        compact.set(newState);

        ctx.getById("list", GroupBuilder.class).ifPresent(list -> {
            list.withLayoutMode(newState ? "LeftCenterWrap" : "Top");
        });

        ctx.getById("summary", LabelBuilder.class).ifPresent(label -> {
            label.withText(newState ? "Compact view: 3 bounties" : "Showing 3 bounties");
        });

        ctx.getById("toggle-mode", ButtonBuilder.class).ifPresent(btn -> {
            btn.withText(newState ? "Comfy View" : "Compact View");
        });

        ctx.updatePage(true);
    })
    .addEventListener("minLevel", CustomUIEventBindingType.ValueChanged, (data, ctx) -> {
        String level = String.valueOf(data);
        ctx.getById("summary", LabelBuilder.class).ifPresent(label -> {
            label.withText("Min level: " + level + " (still 3 bounties)");
        });
        ctx.updatePage(true);
    })
    .open(store);
```

Yes, we edited three builders in one click. We are unstoppable.
{% endstep %}

{% step %}
### A Few Runtime Editing Tips

{% hint style="info" %}
* `ctx.updatePage(true)` rebuilds the whole page client-side. It is the simplest approach for runtime edits, but it is a rebuild, not a surgical patch.
* For text inputs, a `ValueChanged` event fires on every change. That is great for sliders, but can be noisy for text fields. Prefer `FocusLost` for those.
* If you are editing layout or visibility, expect the whole layout to reflow.
{% endhint %}



Next: we dive into layout modes so you can deliberately control how this page flows instead of hoping the UI engine feels merciful.
{% endstep %}
{% endstepper %}

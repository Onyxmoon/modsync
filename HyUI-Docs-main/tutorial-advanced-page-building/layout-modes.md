# Layout Modes

In the previous page, we toggled layouts at runtime. Now we will slow down and actually understand what those layouts _do_.

Our Bounty Board example continues. If you skipped the previous page, no judgment. But some side-eye.

## Layout Modes

Layout modes are how Hytale arranges children inside a group/container. In HYUIML you set them with `layout-mode`, `layout`, or `text-align` on a `<div>`.

Supported `LayoutMode` values (case-insensitive):

* `Top`, `Bottom`, `Left`, `Right`, `Center`, `Full`, `Middle`
* `TopScrolling`, `BottomScrolling`
* `MiddleCenter` or `CenterMiddle` (same idea, different order)
* `LeftCenterWrap`

Think of them like pre-baked layout rules rather than full CSS.

{% hint style="info" %}
This is a mental model, not exact CSS behavior. Use it to reason about how children will be placed inside groups.
{% endhint %}

## But... What are they?

Here is the mental model (rough, but useful):

* `Top` / `Bottom` / `Left` / `Right`: stack children in that direction.
* `Center`: center each child, still stacked.
* `Full`: stretch children to fill the container as much as possible.
* `Middle` / `MiddleCenter`: center content vertically, and often horizontally.
* `TopScrolling` / `BottomScrolling`: like `Top` / `Bottom` but allows scroll.
* `LeftCenterWrap`: wraps children into rows, centered on each row. Great for grids.

## HYUIML Layout Modes

Let us use the Bounty Board list container and swap layout modes.

```html
<div id="list" style="layout-mode: Top; padding: 6;">
    <!-- vertical list -->
</div>

<div id="list" style="layout-mode: LeftCenterWrap; padding: 6;">
    <!-- grid-like list -->
</div>

<div id="list" style="layout-mode: TopScrolling; padding: 6; anchor-height: 200;">
    <!-- scrollable list -->
</div>
```

In practice:

* `Top` gives the classic bounty list.
* `LeftCenterWrap` gives you a tile grid (useful for cards).
* `TopScrolling` keeps your layout sane when the list grows past the container height.

## Using Flex Weights (Because We Are Not Savages)

When you stack items, you usually want predictable widths. Use `flex-weight` to do it. It is not full flexbox, but it is good enough to keep text from colliding.

```html
<div class="bounty-card" style="layout-mode: Left; padding: 4;">
    <p style="flex-weight: 2;">Slime Cleanup</p>
    <p style="flex-weight: 1;">Lvl 1</p>
    <button class="small-tertiary-button">Track</button>
</div>
```

If the title gets longer, it will take more space, the level stays readable, and the button keeps its size. This is the kind of tiny detail that makes UIs feel deliberate.

{% hint style="warning" %}
`flex-weight` is a lightweight sizing tool - it behaves like a simple proportional allocator, not a full flexbox implementation.
{% endhint %}

## Layout Modes for the Outer Shell

You can also set layout modes on:

* `page-overlay`
* `container` / `decorated-container`
* any `<div>` that compiles to a `GroupBuilder`

A common pattern for pages:

```html
<div class="page-overlay" style="layout-mode: Full;">
    <div class="decorated-container" data-hyui-title="Bounty Board" style="layout-mode: Top;">
        <div class="container-contents" style="layout-mode: Top;">
            <!-- content -->
        </div>
    </div>
</div>
```



Next: we will **add new bounty cards at runtime**, because the board should evolve while the page is open.

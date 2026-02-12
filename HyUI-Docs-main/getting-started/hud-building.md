# HUD Building

HUDs (Heads-Up Displays) in HyUI are persistent UI elements that stay on the player's screen. They are managed via the `HudBuilder`.

## Content Sources

There are multiple ways to define the content of your HUD:

{% stepper %}
{% step %}
### Loading from UI Files

You can use an existing Hytale `.ui` file as the base for your HUD.

{% code title="Java" %}
```java
HudBuilder.hudForPlayer(playerRef)
    .fromFile("Pages/MyHud.ui")
    .show(store);
```
{% endcode %}
{% endstep %}

{% step %}
### Loading from HYUIML (HTML)

You can define your HUD using HTML-like syntax.

{% code title="Java" %}
```java
HudBuilder.hudForPlayer(playerRef)
    .fromHtml("<div style='anchor-top: 10; anchor-left: 10;'><p>Hello World!</p></div>")
    .show(store);
```
{% endcode %}
{% endstep %}

{% step %}
### Manual Building

You can manually add elements using builders.

Note: `addElement(...)` only attaches elements to the root. To nest elements, use `.addChild(...)` or element-specific child helpers.

{% code title="Java" %}
```java
HudBuilder.hudForPlayer(playerRef)
    .addElement(LabelBuilder.label()
        .withText("Manual HUD")
        .withAnchor(new HyUIAnchor().setTop(10).setLeft(10)))
    .show(store);
```
{% endcode %}
{% endstep %}
{% endstepper %}

## Detached HUDs

Sometimes you may want to prepare a HUD configuration before you have a player reference, or reuse the same configuration for multiple players. You can use `.detachedHud()` for this.

{% code title="Java" %}
```java
// Pre-make the HUD configuration
HudBuilder builder = HudBuilder.detachedHud()
    .fromHtml("<p>Shared HUD</p>");

// Later, show it for a specific player
builder.show(playerRef, store);
```
{% endcode %}

## Multi-HUD System

One of the most powerful features of HyUI is the **Multi-HUD system**.

By default, Hytale only allows a single "Custom UI HUD" to be active for a player at any given time. This usually means that if two different mods try to show a HUD, they will overwrite each other.

HyUI takes care of this for you. When you call `.show()`, HyUI automatically checks if the player already has a HUD. If they do, and it's managed by HyUI (or a compatible "Multiple HUD" mod), it simply adds your new HUD to the existing stack. If not, it creates a `MultiHud` container that can host many independent HUD instances.

This means:

* You can have multiple independent HUD elements (e.g., a minimap, a quest tracker, and a notification bar) all running at once.
* Each HUD has its own refresh rate, its own elements, and its own event listeners.
* They won't interfere with each other or with other HyUI-based mods.
* It's completely transparent — you just build your HUD and call `.show()`, and HyUI handles the composition.

## Running on the World Thread

{% hint style="danger" %}
It is critical that calls to `.show()` are made on the world thread. If you are inside an async command or another thread, schedule the HUD opening with `world.execute()`. Failing to run `.show()` on the world thread will lead to an exception and the client disconnecting.
{% endhint %}

{% code title="Java" %}
```java
world.execute(() -> {
    HudBuilder.hudForPlayer(playerRef)
        .fromHtml("<div>Welcome!</div>")
        .show(store);
});
```
{% endcode %}

## Periodic Refreshing

If your HUD needs to update regularly (e.g., a timer or player stats), you can set a refresh rate.

{% code title="Java" %}
```java
HudBuilder.hudForPlayer(playerRef)
    // Refresh every 1 second
    .withRefreshRate(1000)
    .onRefresh(hud -> {
        hud.getById("timer", LabelBuilder.class).ifPresent(label -> {
            label.withText("Time: " + System.currentTimeMillis());
        });
    })
    .show(store);
```
{% endcode %}

HyUI optimizes these refreshes by batching updates for all HUDs belonging to the same player.

## Toggling Visibility

You can hide or show specific HUD instances within the multi-hud system:

{% code title="Java" %}
```java
// Hides the root element of this specific HUD
hud.hide();
// Shows it again
hud.unhide();
```
{% endcode %}

## Removing and Re-adding

If you want to completely remove a HUD from the screen (and stop its periodic refreshes), you can use `.remove()`. You can later re-add it using `.readd()`.

{% code title="Java" %}
```java
// Removes the HUD from the multi-hud manager
hud.remove();

// Re-adds it to the same multi-hud manager later
hud.readd();
```
{% endcode %}

## Showing a HUD on Player Join

A common requirement is to show a HUD (like a scoreboard or player info) as soon as a player joins the world. In Hytale, this is best handled using the `PlayerReadyEvent`.

When the player is ready, obtain their `PlayerRef` and `Store` to show the HUD. Remember that `.show()` must be called on the world thread.

{% code title="Java" %}
```java
public void onPlayerReady(PlayerReadyEvent event) {
    var player = event.getPlayer();
    if (player == null) return;

    Ref<EntityStore> ref = player.getReference();
    if (ref == null || !ref.isValid()) return;
    
    Store<EntityStore> store = ref.getStore();
    World world = store.getExternalData().getWorld();

    // Ensure we are on the world thread
    world.execute(() -> {
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        
        HudBuilder.detachedHud()
            .fromHtml("<div style='anchor-top: 10; anchor-right: 10;'><p>Welcome!</p></div>")
            .show(playerRef, store);
    });
}
```
{% endcode %}

For a complete implementation of registering this event in a plugin, see `HyUIPlugin.java`.

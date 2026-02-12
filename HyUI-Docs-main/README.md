# Home

**HyUI** is a powerful, developer-friendly Java library designed to simplify the creation and management of custom User Interfaces for Hytale servers. By bridging Hytale's raw UI protocol with high-level abstractions, HyUI allows you to build complex, interactive, and high-performance UIs using either a fluent **Java Builder API** or **HYUIML**, a declarative HTML/CSS-like syntax.

Whether you are building a simple admin panel, a persistent HUD, or a full-scale RPG menu system, HyUI provides the tools and "escape hatches" needed to get the job done efficiently.

Features

* **HYUIML (HTML/CSS):** Build interfaces using a familiar, declarative HTML-like syntax with CSS styling, plus templating and reusable components.
* **Fluent Builder API:** Construct nested UI hierarchies (Groups, Buttons, Labels, etc.) using a clean, readable chain of methods.
* **Multi-HUD System:** Coexist with other mods effortlessly. HyUI automatically manages Hytale's single HUD slot to allow multiple independent HUD elements to be displayed simultaneously.
* **Dynamic Element Injection:** Load base `.ui` files and inject dynamic elements into specific selectors at runtime.
* **Event Handling Simplified:** Bind server-side logic directly to UI events using simple lambda expressions and access UI state via `UIContext`.
* **Periodic UI Refresh:** Built-in support for batched, periodic HUD updates with low performance overhead.
* **Specialized Builders:** Includes ready-to-use builders for:
  * **Buttons:** Standardized game-themed text buttons and back buttons.
  * **Input Fields:** Text, Numbers, Sliders, Checkboxes, and Color Pickers.
  * **Progress Bars:** Dynamic progress indicators with customizable textures and effects.
  * **Item Icons:** Display item icons with asset-backed textures.
  * **Containers:** Flexible Group builders with various layout modes and window frames.
* **Images:** Easy asset-backed images, plus runtime-downloaded dynamic images.
* **Advanced Logic (Escape Hatches):** Access raw `UICommandBuilder` instances at any point for properties not natively covered by the API.

Quick Start

{% stepper %}
{% step %}
### Installation (Gradle)

To use HyUI in your Hytale project, you can get started quickly by using the example project: https://github.com/Elliesaur/Hytale-Example-UI-Project

Otherwise, add HyUI to your project via Cursemaven:

```groovy
repositories {
    maven { url "https://www.cursemaven.com" }
}

dependencies {
    // Project ID: 1431415
    implementation "curse.maven:hyui-1431415:<file-id>"
}
```
{% endstep %}

{% step %}
### Creating a Page with HYUIML (HTML)

For most use cases, HYUIML is the fastest way to build layouts:

```java
String html = """
    <div class="page-overlay">
        <div class="container" data-hyui-title="My Menu">
            <div class="container-contents">
                <p>Welcome to the menu!</p>
                <button id="myBtn">Click Me</button>
                <img class="dynamic-image" src="https://example.invalid/render/PlayerName" />
            </div>
        </div>
    </div>
    """;

PageBuilder.pageForPlayer(playerRef)
    .fromHtml(html)
    .addEventListener("myBtn", CustomUIEventBindingType.Activating, (ctx) -> {
        playerRef.sendMessage(Message.raw("Clicked!"));
    })
    .open(store);
```
{% endstep %}

{% step %}
### Creating a HUD

HUDs are persistent on-screen elements:

```java
HudBuilder.hudForPlayer(playerRef)
    .fromHtml("<div style='anchor-top: 10; anchor-left: 10;'><p>Health: 100</p></div>")
    .show(store);
```
{% endstep %}
{% endstepper %}

Components

| Builder                | Purpose                                                                    |
| ---------------------- | -------------------------------------------------------------------------- |
| `PageBuilder`          | Entry point for full-screen UIs; manages file loading and lifecycle.       |
| `HudBuilder`           | Entry point for HUD creation; manages multi-HUD coexistence and refreshes. |
| `GroupBuilder`         | A container used to organize and layout child elements.                    |
| `ContainerBuilder`     | Provides the standard Hytale window frame.                                 |
| `PageOverlayBuilder`   | Full-screen overlay container.                                             |
| `TabNavigationBuilder` | Tabbed navigation bar.                                                     |
| `TabContentBuilder`    | Tab content container.                                                     |
| `ButtonBuilder`        | For interactive buttons; supports standard Hytale aesthetics.              |
| `LabelBuilder`         | For displaying dynamic text with style and anchor support.                 |
| `ImageBuilder`         | For displaying asset-backed images (`.png`).                               |
| `DynamicImageBuilder`  | Runtime-downloaded PNG images.                                             |
| `TextFieldBuilder`     | Captures string input from the player.                                     |
| `NumberFieldBuilder`   | Captures numeric input from the player.                                    |
| `DropdownBoxBuilder`   | Dropdown selection control.                                                |
| `CheckBoxBuilder`      | Checkbox input control.                                                    |
| `ColorPickerBuilder`   | Provides a Hex color selection interface.                                  |
| `SliderBuilder`        | Provides support for number sliders.                                       |
| `ProgressBarBuilder`   | Provides support for progress bars.                                        |
| `TimerLabelBuilder`    | Timer labels with formatting.                                              |
| `SpriteBuilder`        | Animated sprite rendering.                                                 |
| `ItemIconBuilder`      | Provides support for item icons.                                           |
| `ItemSlotBuilder`      | Item slot UI elements.                                                     |
| `ItemGridBuilder`      | Scrollable item grid.                                                      |

Documentation & Examples

Detailed documentation for installation, page building, HUD building, and HYUIML can be found in the `docs` folder.

Requirements:

* Hytale Server added as a dependency
* Java 25 (or current Hytale-compatible version)
* jsoup (embedded in the JAR under MIT license)
* MultipleHUD - optional requirement - included source in JAR (not embedded) under MIT license.

Licenses

All open source licenses can be found within the JAR and GitHub repo.

Help and Support

Feel free to join the Discord: https://discord.gg/NYeK9JqmNB - happy to help!

# Getting Started

HyUI is a fluent, builder-based library for creating and managing custom user interfaces in Hytale. It simplifies the process of building pages and HUD elements by providing a high-level Java API and a lightweight markup language.

What would you like to do?

* [**Install HyUI**](installation.md): Set up HyUI in your Hytale project.
* [**Build a HUD**](hud-building.md): Create persistent on-screen elements like maps, health bars, or notifications.
* [**Build a Page**](../tutorial-page-building/): Create full-screen interactive menus and settings pages.
* [**Use HYUIML (HTML/CSS)**](../hyuiml-htmlish-in-hytale.md): Learn how to define your UIs using a familiar markup syntax.
* [**Template Processor**](../template-processor.md): Reusable HYUIML components and variables.
* [**UI Elements**](../element-examples.md): Common UI element examples and patterns.
* [**Element Validation**](../element-validation.md): How we validate your UI.
* [**Changelog**](../changelog.md): Version updates and breaking changes.

***

Quick Start: Opening your first Page

This example uses the PageBuilder to load a UI from HTML.

{% code title="Example.java" %}
```java
String html = "<div><p>Hello Hytale!</p></div>";

PageBuilder.pageForPlayer(playerRef)
    .fromHtml(html)
    .open(store);
```
{% endcode %}

Concepts

{% stepper %}
{% step %}
### Builders

Everything in HyUI is built using fluent builders (e.g., `ButtonBuilder`, `LabelBuilder`).
{% endstep %}

{% step %}
### Detached Builders

You can prepare your UI configurations (using `detachedPage()` or `detachedHud()`) before you even have a player reference.
{% endstep %}

{% step %}
### Selectors and IDs

Use `.withId("my-id")` to reference elements later for event listeners or updates.
{% endstep %}

{% step %}
### Automatic Management

HyUI handles Hytale's single HUD slot limitation and complex event binding for you.
{% endstep %}
{% endstepper %}


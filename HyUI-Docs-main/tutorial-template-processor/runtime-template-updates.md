# Runtime Template Updates

This is the advanced mode used in `HyUITemplateRuntimeCommand`. It lets template variables resolve from live UI values, like dropdown selections or inputs, **when the template is reprocessed**. It is powerful and experimental. Use responsibly and test thoroughly.

We will cover:

* Enabling runtime template updates.
* Using live UI IDs in template variables and conditionals.
* Rebuilding the page when inputs change.

{% stepper %}
{% step %}
### Enable Runtime Template Updates

```java
PageBuilder builder = PageBuilder.pageForPlayer(playerRef)
    .loadHtml("Pages/BountyRuntime.html", template)
    .enableRuntimeTemplateUpdates(true)
    .withLifetime(CustomPageLifetime.CanDismiss);
```

When runtime updates are enabled, the template processor can resolve variables from element IDs at runtime. This means `{{$region}}` can pull the value from `<select id="region">`.
{% endstep %}

{% step %}
### Template With Live Inputs

Example template file: `Common/UI/Custom/Pages/BountyRuntime.html`

```html
<div class="page-overlay">
    <div class="decorated-container" data-hyui-title="Bounty Board">
        <div class="container-contents" style="layout-mode: Top;" style="anchor-width: 900; anchor-height: 600;">
            <div style="layout-mode: Left; padding: 6;">
                <label style="anchor-width: 80;">Region</label>
                <select id="region" data-hyui-showlabel="true" value="Forest">
                    <option value="Forest" {{#if region == Forest}}selected{{/if}}>Forest</option>
                    <option value="Desert" {{#if region == Desert}}selected{{/if}}>Desert</option>
                    <option value="Tundra" {{#if region == Tundra}}selected{{/if}}>Tundra</option>
                </select>

                <label style="anchor-width: 80; padding-left: 10;">Min</label>
                <input id="minLevel" type="number" value="1" style="anchor-width: 60;" />
            </div>

            <p style="padding: 4;">Filters: {{$region|Unknown}} / Min {{$minLevel|1}}</p>

            {{#if minLevel >= 6}}
            <p style="color: #4CAF50;">Elite filters on. Good luck.</p>
            {{else}}
            <p style="color: #888888;">Standard filters.</p>
            {{/if}}

            <div id="list" style="layout-mode: Top; padding: 6;">
                {{#each bounties}}
                <div class="bounty-card" style="layout-mode: Left; padding: 4;">
                    <p style="flex-weight: 2;">{{$title}}</p>
                    <p style="flex-weight: 1;">Lvl {{$level}}</p>
                    <button class="small-tertiary-button">Track</button>
                </div>
                {{/each}}
            </div>
        </div>
    </div>
</div>
```

The variables `{{$region}}` and `{{$minLevel}}` resolve from UI element IDs because runtime template updates are enabled.
{% endstep %}

{% step %}
### Add Event Listeners to Rebuild on Runtime

```java
builder.addEventListener("region", CustomUIEventBindingType.ValueChanged, (value, ctx) -> {
    ctx.updatePage(false);
});

builder.addEventListener("minLevel", CustomUIEventBindingType.ValueChanged, (value, ctx) -> {
    ctx.updatePage(false);
});
```

`updatePage(false)` rebuilds the page without clearing it completely. This is what makes the runtime template values re-evaluate.&#x20;

{% hint style="danger" %}
Be careful, false means you need to make sure all your elements exist on the FIRST page load - the initial template **MUST** have all elements you want to have on the page (you can hide them!), you cannot **ADD** or **REMOVE** elements. **Use true if you want to add or remove elements on rebuild.**
{% endhint %}

You will need to add event listeners to **every input** you want to update the page and rebuild the page using the template processor. If you want them to enter text into a text input, opt for `FocusLost` event and check for changes before updating the page (otherwise it will get funky!).
{% endstep %}

{% step %}
### Runtime Values vs Template Variables

* Static variables from `TemplateProcessor.setVariable(...)` still work.
* Runtime values are pulled from the UI by matching ID names. Conflicts preference the runtime value.
* If a runtime value exists, it can be used in `{{$var}}` and `{{#if}}` just like a normal variable.

Example: in the template, `{{$region}}` uses the dropdown value because the ID is `region`.
{% endstep %}

{% step %}
### Keep It Sane

* Use `ValueChanged` on dropdowns and sliders.
* For text inputs, prefer `FocusLost` or you will rebuild on every keystroke.
* This is experimental. Be cautious in production UIs.

{% hint style="warning" %}
This feature is experimental and powerful. Test thoroughly and use responsibly.
{% endhint %}

You now have a complete template-driven Bounty Board with live runtime updates. That is scary power. Use it for good.
{% endstep %}
{% endstepper %}

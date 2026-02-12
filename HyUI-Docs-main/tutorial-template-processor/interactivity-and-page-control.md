# Interactivity and Page Control

Templates build the initial UI, but your users will still click buttons. This part shows how templates and runtime events work together.

We will cover:

* Using event listeners with template-built elements.
* Editing builders at runtime.
* Getting the page reference from `UIContext` and closing the page.

{% stepper %}
{% step %}
### Add Interactive IDs in the Template

Include IDs on elements you want to interact with at runtime.

{% code title="Pages/BountyBoard.html" %}
```html
<div class="page-overlay">
    <div class="decorated-container" data-hyui-title="{{$title}}">
        <div class="container-contents" style="layout-mode: Top;">
            <p id="summary" style="padding: 4;">{{$summary}}</p>

            <div style="layout-mode: Left; padding: 6;">
                <button id="refresh-summary" class="secondary-button">Refresh Summary</button>
                <button id="close-board" class="secondary-button">Close</button>
            </div>

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
{% endcode %}
{% endstep %}

{% step %}
### Listen for Events

Register event listeners on the PageBuilder that target the element ids from the template and update the runtime builders accordingly.

{% code title="Java (PageBuilder usage)" %}
```java
PageBuilder builder = PageBuilder.pageForPlayer(playerRef)
    .loadHtml("Pages/BountyBoard.html", template)
    .withLifetime(CustomPageLifetime.CanDismiss);

builder.addEventListener("refresh-summary", CustomUIEventBindingType.Activating, (ignored, ctx) -> {
    ctx.getById("summary", LabelBuilder.class).ifPresent(label -> {
        label.withText("Still showing " + bounties.size() + " bounties");
    });
    ctx.updatePage(true);
});

builder.addEventListener("close-board", CustomUIEventBindingType.Activating, (ignored, ctx) -> {
    ctx.getPage().ifPresent(page -> page.close());
});

builder.open(store);
```
{% endcode %}
{% endstep %}

{% step %}
### Important Mental Model

* Templates are processed when the page is built.
* Once the page is open, editing builders is normal runtime UI behavior.
* If you need template values to update based on live inputs, you must use **runtime template updates** (next part).
{% endstep %}
{% endstepper %}

\
Next: advanced runtime template updates. This is the spicy, experimental part.

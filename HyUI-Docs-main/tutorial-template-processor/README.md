# Tutorial: Template Processor

We are continuing the Bounty Board story, but now we are doing it with the Template Processor. This is the "build UI from data" mode, and it is a great fit when you have lists, conditional blocks, or repeated UI.

In this first part we will:

* Load HYUIML from a resource file.
* Use a `TemplateProcessor` while loading.
* Keep the Bounty Board layout familiar.

{% stepper %}
{% step %}
### Create the HTML template file

Create a file under `Common/UI/Custom/Pages/` (this is where `loadHtml(...)` looks).

Example: `Common/UI/Custom/Pages/BountyBoard.html`

{% code title="BountyBoard.html" %}
```html
<div class="page-overlay">
    <div class="decorated-container" data-hyui-title="{{$title}}">
        <div class="container-contents" style="layout-mode: Top;" style="anchor-width: 900; anchor-height: 600;">
            <p id="summary" style="padding: 4;">{{$summary}}</p>

            <div id="list" style="layout-mode: Top; padding: 0 6;">
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

We are already using variables (`{{$title}}`, `{{$summary}}`) and a loop (`{{#each bounties}}`). We will break those down in later parts.
{% endstep %}

{% step %}
### Load the file with a TemplateProcessor

The `PageBuilder` supports loading HTML templates directly from resources with a `TemplateProcessor`:

{% code title="Java" %}
```java
List<Bounty> bounties = List.of(
    new Bounty("Slime Cleanup", 1),
    new Bounty("Bandit Camp", 4),
    new Bounty("Wisp Hunt", 7)
);

TemplateProcessor template = new TemplateProcessor()
    .setVariable("title", "Bounty Board")
    .setVariable("summary", "Showing " + bounties.size() + " bounties")
    .setVariable("bounties", bounties);

PageBuilder.pageForPlayer(playerRef)
    .loadHtml("Pages/BountyBoard.html", template)
    .open(store);
```
{% endcode %}

Yes, we just connected a HTML file to real Java data. We have left tutorial land and entered "this could ship" territory.

Next up: we deep dive into variables and how you can shape them with filters and defaults.
{% endstep %}
{% endstepper %}

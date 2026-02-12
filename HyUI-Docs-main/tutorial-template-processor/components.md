# Components

Components are like reusable mini-templates. Perfect for repeated UI cards, stat blocks, and the entire Bounty Board list.

We will:

* Define a component in Java.
* Pass parameters into it.
* Use it inside loops.

{% stepper %}
{% step %}
### Define a Component

```java
TemplateProcessor template = new TemplateProcessor()
    .registerComponent("bountyCard", """
        <div class=\"bounty-card\" style=\"layout-mode: Left; padding: 4;\">
            <p style=\"flex-weight: 2;\">{{$title}}</p>
            <p style=\"flex-weight: 1;\">Lvl {{$level}}</p>
            {{#if level >= 6}}
            <p style=\"color: #4CAF50; flex-weight: 1;\">Elite</p>
            {{else}}
            <p style=\"color: #888888; flex-weight: 1;\">Standard</p>
            {{/if}}
            <button class=\"small-tertiary-button\">Track</button>
        </div>
        """);
```

Component parameters are just variables used inside the component template.
{% endstep %}

{% step %}
### Use the Component in HYUIML

```html
<div id="list" style="layout-mode: Top; padding: 6;">
    {{#each bounties}}
    {{@bountyCard:title={{$title}},level={{$level}}}}
    {{/each}}
</div>
```

You can pass any variable name, including nested values:

```html
{{@bountyCard:title={{$title}},level={{$meta.level}}}}
```
{% endstep %}

{% step %}
### Why Components Matter

* You keep the template readable.
* You can adjust one card design and update all instances.
* Your teammates stop asking you why the cards are 90% copy-paste.

Next: runtime interactivity and how templates behave when you start clicking things.
{% endstep %}
{% endstepper %}

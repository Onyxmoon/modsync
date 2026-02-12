# Loops and Conditionals

Time to teach the Bounty Board to repeat itself, and also to judge its own bounties.

We will cover:

* `{{#each}}` loops.
* `{{#if}}` conditionals with logic.
* Working with lists and model fields.

{% stepper %}
{% step %}
### Looping Through Bounties

Create a data list:

```java
public record Bounty(String title, int level, String rarity) {}

List<Bounty> bounties = List.of(
    new Bounty("Slime Cleanup", 1, "Common"),
    new Bounty("Bandit Camp", 4, "Uncommon"),
    new Bounty("Wisp Hunt", 7, "Rare")
);

TemplateProcessor template = new TemplateProcessor()
    .setVariable("bounties", bounties);
```

Loop in HYUIML:

```html
<div id="list" style="layout-mode: Top; padding: 6;">
    {{#each bounties}}
    <div class="bounty-card" style="layout-mode: Left; padding: 4;">
        <p style="flex-weight: 2;">{{$title}}</p>
        <p style="flex-weight: 1;">Lvl {{$level}}</p>
        <p style="flex-weight: 1;">{{$rarity}}</p>
        <button class="small-tertiary-button">Track</button>
    </div>
    {{/each}}
</div>
```

{% hint style="info" %}
Inside `{{#each}}`, fields and getters are exposed directly as `{{$title}}`, `{{$level}}`, `{{$rarity}}`.
{% endhint %}
{% endstep %}

{% step %}
### Conditionals

Add a little logic so high-level bounties look special:

```html
{{#each bounties}}
<div class="bounty-card" style="layout-mode: Left; padding: 4;">
    <p style="flex-weight: 2;">{{$title}}</p>
    <p style="flex-weight: 1;">Lvl {{$level}}</p>
    {{#if level >= 6}}
    <p style="color: #4CAF50; flex-weight: 1;">Elite</p>
    {{else}}
    <p style="color: #888888; flex-weight: 1;">Standard</p>
    {{/if}}
</div>
{{/each}}
```

Supported operators:

* Equality: `==`, `!=`
* Numeric: `>`, `<`, `>=`, `<=`
* Logical: `&&`, `||`, `!`
* Contains: `contains`

Example with logic:

```html
{{#if rarity == Rare || level >= 7}}
<p style="color: #4CAF50;">Priority</p>
{{/if}}
```
{% endstep %}
{% endstepper %}

Next: components so you can avoid copy-paste UI blocks. Your wrists will thank you.

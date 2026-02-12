# Template Processor Basics

{% stepper %}
{% step %}
### Define a Simple Data Model

Keep it boring. Boring is good when it is 2 a.m.

{% code title="Bounty.java" %}
```java
public record Bounty(String name, int level) {}
```
{% endcode %}
{% endstep %}

{% step %}
### Build the Template HTML

We will use `{{#each}}` to render the list. No runtime editing yet... (shh).

<pre class="language-vue-html" data-title="template.html (string)"><code class="lang-vue-html">
&#x3C;div class="page-overlay">
    &#x3C;div class="decorated-container" data-hyui-title="<a data-footnote-ref href="#user-content-fn-1">{{$title}}</a>">
        &#x3C;div class="container-contents" style="layout-mode: Top;">
            &#x3C;p id="summary" style="padding: 4;">{{$summary}}&#x3C;/p>

            &#x3C;div id="list" style="layout-mode: Top; padding: 6;">
                <a data-footnote-ref href="#user-content-fn-2">{{#each bounties}}</a>
                &#x3C;div class="bounty-card" style="layout-mode: Left; padding: 4;">
                    &#x3C;p style="flex-weight: 2;">{{$name}}&#x3C;/p>
                    &#x3C;p style="flex-weight: 1;">Lvl {{$level}}&#x3C;/p>
                    &#x3C;button class="small-tertiary-button">Track&#x3C;/button>
                &#x3C;/div>
                {{/each}}
            &#x3C;/div>
        &#x3C;/div>
    &#x3C;/div>
&#x3C;/div>
</code></pre>
{% endstep %}

{% step %}
### Bind Variables and Render

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
    .fromTemplate(html, template)
    .open(store);
```
{% endstep %}

{% step %}
### Advantages

{% hint style="info" %}
* Your HYUIML stays clean, even with repeated UI blocks.
* You can centralize the data shape in Java, where you already live.
* You avoid copy-paste UI cards. Nobody wants that job.
{% endhint %}

That is the end of the four-part arc. Feel free to go back and have a look at the previous pages. They can help!
{% endstep %}
{% endstepper %}

[^1]: This works, it'll be replaced with the title variable.

[^2]: For each instance of the model in the list...

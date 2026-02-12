# Variables and Filters

Variables are the fuel. Without them, your template is a static napkin. With them, it becomes a Bounty Board with opinions.

We will cover:

* Defining variables in Java.
* Using them in HYUIML.
* Defaults and filters.
* Dot paths for nested data.

{% stepper %}
{% step %}
### Defining Variables

You can set variables one at a time:

{% code title="Java" %}
```java
TemplateProcessor template = new TemplateProcessor()
    .setVariable("title", "Bounty Board")
    .setVariable("summary", "Showing 3 bounties")
    .setVariable("region", "Forest");
```
{% endcode %}

Or set them in a map:

{% code title="Java" %}
```java
Map<String, Object> vars = Map.of(
    "title", "Bounty Board",
    "summary", "Showing 3 bounties",
    "region", "Forest"
);

TemplateProcessor template = new TemplateProcessor().setVariables(vars);
```
{% endcode %}
{% endstep %}

{% step %}
### Using Variables in HYUIML

```html
<p>{{$summary}}</p>
<p>Region: {{$region}}</p>
```

If the variable exists, it is replaced. If it does not exist, you can provide a default:

```html
<p>Region: {{$region|Unknown}}</p>
```
{% endstep %}

{% step %}
### Filters

Filters are tiny helpers you can apply after the variable:

```html
<p>Shouting: {{$title|upper}}</p>
<p>Gold: {{$reward|number}}</p>
```

If you hand the template a number, `number` will format it. If you hand it a string, it will do its best not to cry.
{% endstep %}

{% step %}
### Dot Paths

If your data has nested fields, you can access them with dot paths.

{% code title="Java" %}
```java
public record BountyMeta(String region, String giver) {}
public record Bounty(String title, int level, BountyMeta meta) {}

Bounty bounty = new Bounty("Wisp Hunt", 7, new BountyMeta("Tundra", "Warden"));
```
{% endcode %}

```html
<p>Region: {{$meta.region}}</p>
<p>Given by: {{$meta.giver}}</p>
```

You can also do array or list indexing like `{{$bounties.0.title}}`, but do that only when you really, really mean it.
{% endstep %}
{% endstepper %}

Next: loops and conditionals, where the Bounty Board starts to feel alive.

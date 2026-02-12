# Page Interactions

We've got this far, our HTML is looking like this:

```html
<div class="page-overlay">
    <div class="container">
        <div class="container-contents">
            <p id="my-label">Hello, World!</p>
        </div>
    </div>
</div>
```

Let's now listen for events in our code. If you remember, our code is split between the constructor, and the `executeCommand` method.

Let's focus on the `executeCommand` Method:

```java
PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
if (playerRef != null) {
    page.open(playerRef, store);
}
```

We're going to add in some events before we open the page.

<pre class="language-java"><code class="lang-java">page.addEventListener("my-button", CustomUIEventBindingType.Activating, 
    (data, ctx) -> {
        <a data-footnote-ref href="#user-content-fn-1">playerRef.sendMessage</a>(Message.raw("Button was pressed!"));
        var labelText = <a data-footnote-ref href="#user-content-fn-2">ctx.getValueAs</a>("my-label", String.class)<a data-footnote-ref href="#user-content-fn-3">.orElse("N/A");</a>
        <a data-footnote-ref href="#user-content-fn-4">ctx.getById</a>("my-label", LabelBuilder.class).ifPresent(labelBuilder -> {
            <a data-footnote-ref href="#user-content-fn-5">labelBuilder.withText</a>("Woah, I changed!");
            <a data-footnote-ref href="#user-content-fn-6">ctx.updatePage(true)</a>;
        });
    });
</code></pre>

Read the annotations (hover your mouse) and... try the code!



Well, if you tried it (did you??? Go back!), it would fail and spit an error out at you at runtime. It can't find the "my-button" to add an event for! Duh, we forgot to add the button.

```html
<div class="page-overlay">
    <div class="container">
        <div class="container-contents">
            <p id="my-label">Hello, World!</p>
            <button id="my-button">Click Me</button>
        </div>
    </div>
</div>
```



Now try the code. See what happens!

It should have updated the label contents and the page refreshed in front of your eyes. You probably didn't see it refresh at all.

This is how we add a basic button interaction. There are different events, most likely you'll want to use ValueChanged when dealing with elements such as NumberFields, and so on.



### Builder Mappings

One last thing, how did we know it would be a label builder? Check out the complete reference for HYUIML [here](../hyuiml-htmlish-in-hytale.md#supported-tags-and-mappings) and read how each tag is converted to builders.&#x20;





[^1]: Sends a message to the player.

[^2]: Attempts to get a value of a value-specific element. Label is not a value-specific element!

[^3]: The value returned in this instance would be "N/A" because it is not an element that supports values.

[^4]: This method gets a specific element's builder - the thing that was constructed to make the element appear.

[^5]: We change the text at runtime.

[^6]: We make sure to let the context know that we are updating the page and it should rebuild/reload the page. True means "clear the page and rebuild it entirely".

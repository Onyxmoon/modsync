# Identifying Elements

Selectors in UI files are how we can identify elements. In HYUIML these are called "IDs" and are treated as such in the HTML we write. Any element with an ID attribute is able to be called, or changed during runtime of the plugin.

```html
<div class="page-overlay">
    <div class="container">
        <div class="container-contents">
            <p>Hello, World!</p>
        </div>
    </div>
</div>
```

Let's edit our HTML a little bit. Let's add an ID to our paragraph tag:

```html
<p id="my-label">Hello, World!</p>
```

Now we can change this before the page is built. What does it mean to be "built"? Built is a part of the lifecycle of Hytale's UI and HyUI itself.



PageBuilder -> Open for Player -> Build to CommandBuilder steps -> Send to CommandBuilder -> Sent to Client -> Rendered on Client



Once we have built the page, the PageBuilder has done its job it is finished! It hands control over to the HyUIInterface class, and events begin to trigger for interactions. Events in HyUI are handled internally, and you receive events through the event listeners added to the builder.



In the next page, we'll look at how to listen for events.

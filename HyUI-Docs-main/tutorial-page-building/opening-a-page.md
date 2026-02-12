# Opening a Page

Let's continue with our HYUIML from last time:

```html
<div class="page-overlay">
    <div class="container">
        <div class="container-contents">
            <p>Hello, World!</p>
        </div>
    </div>
</div>
```

This is a cool page, but it ... does nothing? How do we get this to show? We know we "load" it into a PageBuilder, but how do we make that show for players when they do something?

If you're in the example project, you'll already see an example command made for you. We'll use that for this tutorial, if you can't see it, check out the example project: [https://github.com/Elliesaur/Hytale-Example-UI-Project](https://github.com/Elliesaur/Hytale-Example-UI-Project) and find the Commands package with the ExampleCommand within it.



The first thing to do is to adjust the code in the constructor, let's change it to `.loadHtml(filePath)`, and provide it the location of your example: `Pages/MyPage.html`

```java
page = PageBuilder.detachedPage()
       .withLifetime(CustomPageLifetime.CanDismiss)
       .loadHtml("Pages/MyPage.html");
```

Now, to open that page, look inside the executeCommand method which already opens our page for us!

```java
PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
if (playerRef != null) {
    // Set up our button for this player specifically.
    page.getById("exampleBtn", ButtonBuilder.class).ifPresent(button -> {
        button.addEventListener(CustomUIEventBindingType.Activating, event -> {
            commandContext.sendMessage(Message.raw("Button clicked!"));
        });
    });
    page.open(playerRef, store);
}
```

We will need to adjust this, let's remove the `getById` method entirely.

```java
PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
if (playerRef != null) {
    page.open(playerRef, store);
}
```

All finished, the open method takes in a reference to a player, and the entity store and opens the page when they type the /test command in chat.



### Challenge

Edit the HYUIML page in resources and add in a button, which once clicked tells the player `Hello, World!`

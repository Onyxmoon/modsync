# Loading HYUIML From File

It is possible and encouraged for you to load your HTML from a file stored in your resources directory. As your page gets larger, you will want to have more control over the contents of it, and IDEs often support HTML syntax highlighting. A bonus!

```java
PageBuilder builder = PageBuilder.detachedPage()
        .loadHtml("Pages/HyUIHtmlTest.html")
```

We can use the `loadHtml(filePath)` to load HTML from the resources of your project. Your HTML file should be formatted for HYUIML to parse correctly.

The code above loads from the `Common/UI/Custom/Pages` directory, the file `HyUIHtmlTest.html` If it does not find the file, it will raise an IllegalArgumentException, or a RuntimeException if something else fails.

Once you have loaded your HYUIML from file, you can use the various builder methods to load your page.

### Tutorial

Create a "Pages" folder within your Common/UI/Custom directory of your plugin.&#x20;

Within that folder, create a new HTML file, give it the name "MyPage.html"

Add in the following content to it:

{% code lineNumbers="true" %}
```html
<div class="page-overlay">
    <div class="container">
        <div class="container-contents">
            <p>Hello, World!</p>
        </div>
    </div>
</div>

```
{% endcode %}

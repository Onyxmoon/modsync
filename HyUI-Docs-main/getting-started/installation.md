# Installation

HyUI is designed to be easily integrated into your Hytale modding project.

Quick Start: Example Project

To use HyUI in your Hytale project, you can get started quickly by using the example project: https://github.com/Elliesaur/Hytale-Example-UI-Project

{% stepper %}
{% step %}
### Add the Dependency

Otherwise, you can either include the JAR file directly or use Cursemaven if you are using Gradle.

#### Using the JAR file

{% stepper %}
{% step %}
Download the latest `HyUI-0.X.0.jar` (replace `X` with version) from the CurseForge page (see README).
{% endstep %}

{% step %}
Place the JAR in your project's `libs` folder.
{% endstep %}

{% step %}
Add the following to your `build.gradle` (replace `X` with version):

{% code title="build.gradle" %}
```gradle
dependencies {
    implementation files('libs/HyUI-0.X.0.jar')
}
```
{% endcode %}
{% endstep %}
{% endstepper %}

#### Using Cursemaven (Gradle)

You can use Cursemaven to easily add it as a dependency:

{% code title="build.gradle" %}
```gradle
repositories {
    maven {
        url "https://www.cursemaven.com"
    }
}

dependencies {
    // Replace <file-id> with the actual ID from CurseForge
    // Link to CurseForge Files: https://www.curseforge.com/hytale/mods/hyui/files/all?page=1&pageSize=20&showAlphaFiles=hide
    // Project ID: 1431415
    implementation "curse.maven:hyui-1431415:<file-id>"
}
```
{% endcode %}
{% endstep %}

{% step %}
### Resource Setup

HyUI requires certain folders in your project. Ensure your `src/main/resources` folder contains the `Common/UI/Custom` directory.

The basic directory structure should look like this:

* `src/main/resources/Common/UI/Custom/Pages/`
* `src/main/resources/Common/UI/Custom/Huds/`
* `src/main/resources/Common/UI/Custom/`

These folders are good practice for pages you load from `.ui` files.

These folders are also where you will store images, which are relative to the `Common/UI/Custom` folder itself.
{% endstep %}

{% step %}
### Verification

To verify that HyUI is correctly installed, try opening a simple page in your mod:

{% code title="Example Java" %}
```java
PageBuilder.pageForPlayer(playerRef)
    .fromHtml("<div class='page-overlay'><p>HyUI is working!</p></div>")
    .open(store);
```
{% endcode %}
{% endstep %}
{% endstepper %}

# ModSync

> **Early Development Stage** - This plugin is in very early development. Use at your own risk. Development is ongoing - please be kind with feedback and bug reports!

A server-side mod management plugin for Hytale that lets you easily add, install, update, and remove mods directly from in-game commands.

## Features

*   **Add mods via URL** - Simply paste a CurseForge mod URL to add it to your server's mod list
*   **Easy installation** - Install all managed mods with a single command
*   **Update checking** - Check for available updates with version comparison display
*   **One-click upgrades** - Upgrade mods to their latest versions
*   **Release channels** - Choose between Release, Beta, or Alpha versions (global default + per-mod override)
*   **Import existing mods** - Import unmanaged mods into ModSync with auto-matching
*   **Flexible removal** - Remove mods by name, slug, or plugin identifier
*   **Persistent tracking** - Your mod list is saved and persists across server restarts
*   **Smart file handling** - Locked files are automatically queued for deletion on next server restart (with the optional bootstrap plugin)
*   **Self-upgrade** - Update ModSync itself directly from in-game commands

## Commands

All commands use the `/modsync` prefix:

| Command                                    | Description                                                        |
|--------------------------------------------|--------------------------------------------------------------------|
| <code>/modsync add [url]</code>            | Add a mod from a CurseForge URL                                    |
| <code>/modsync list</code>                 | Show all managed mods with install status, version, and identifier |
| <code>/modsync install</code>              | Install all mods from your list                                    |
| <code>/modsync install [name]</code>       | Install a specific mod by name, slug, or identifier                |
| <code>/modsync remove all</code>           | Remove all mods                                                    |
| <code>/modsync remove [name]</code>        | Remove mod by name, slug, or identifier                            |
| <code>/modsync check</code>                | Check for available updates (shows installed vs. latest version)   |
| <code>/modsync upgrade</code>              | Upgrade all installed mods to latest version                       |
| <code>/modsync upgrade [name]</code>       | Upgrade a specific mod by name, slug, or identifier                |
| <code>/modsync scan</code>                 | List unmanaged mods in the mods folder                             |
| <code>/modsync import [target]</code>      | Auto-match and import an unmanaged mod                             |
| <code>/modsync import [target] [url]</code> | Import an unmanaged mod with a CurseForge URL                     |
| <code>/modsync config</code>               | Show all configuration settings                                    |
| <code>/modsync config channel [value]</code> | View or set default release channel (release/beta/alpha)         |
| <code>/modsync setchannel [mod] [channel]</code> | Set per-mod release channel override                           |
| <code>/modsync status</code>               | Show current configuration and version                             |
| <code>/modsync setkey [key]</code>         | Set your CurseForge API key                                        |
| <code>/modsync selfupgrade</code>          | Check for ModSync plugin updates                                   |
| <code>/modsync selfupgrade apply</code>    | Download and install the latest ModSync version                    |
| <code>/modsync reload</code>               | Reload configuration                                               |

> **Tip:** Use quotes for names with spaces: `/modsync install "My Mod"`

### Examples

```
# Adding mods
/modsync add https://www.curseforge.com/hytale/mods/example


# Managing mods
/modsync install
/modsync install simply-trash
/modsync install BlameJared:SimplyTrash
/modsync check
/modsync upgrade
/modsync upgrade BlameJared:SimplyTrash
/modsync remove BlameJared:SimplyTrash
/modsync remove all
```

## Setup

1.  Install the plugin on your Hytale server
2.  (Optional but recommended) Install the bootstrap plugin in the `earlyplugins` folder (included as dependency on curseforge), especially if you get deletion errors.

*   Server must be started with `--early-plugins` and `--accept-early-plugins` flags

1.  Get a CurseForge API key from [CurseForge for Studios](https://console.curseforge.com/)
2.  Set your API key: `/modsync setkey YOUR_API_KEY`
3.  Start adding mods: `/modsync add https://www.curseforge.com/hytale/mods/your-mod`

## Requirements

*   Hytale Server (release patchline)
*   CurseForge API key
*   (Optional) Server must be started with `--early-plugins` and `--accept-early-plugins` flags
*   (Optional) Bootstrap plugin for proper file deletion

## How It Works

### Installing Mods

When you install a mod, the plugin:

1.  Downloads the JAR file from CurseForge
2.  Detects the plugin type (regular plugin or early plugin) from the CurseForge category
3.  Saves it to the appropriate folder:
    *   Regular plugins → `mods/`
    *   Early/Bootstrap plugins → `earlyplugins/` (configurable)
4.  Registers the mod in the installed mods registry

**Note:** A server restart is required to load newly installed mods.

### Bootstrap/Early Plugins (ALPHA)

ModSync automatically detects Bootstrap/Early Plugins from CurseForge and installs them to the correct folder. When you add a mod:

*   URLs like `https://www.curseforge.com/hytale/mods/...` are regular plugins
*   URLs like `https://www.curseforge.com/hytale/bootstrap/...` are early plugins

The plugin type is displayed in commands with `[Plugin]` or `[Early Plugin]`.

### Removing Mods

When you remove a mod, the plugin:

1.  Unloads the mod if currently loaded
2.  Attempts to delete the JAR file
3.  If the file is locked (Windows), it queues the file for deletion on next restart
4.  (Optional) The bootstrap plugin handles queued deletions before the server loads mods

**Note:** If a file is locked, you will see a message indicating a restart is required.

### Checking for Updates

The `/modsync check` command shows:

*   Which mods have updates available
*   Current installed version vs. latest available version
*   Example output: `Simply Trash: Simply-Trash-0.0.1.jar -> Simply-Trash-0.0.2.jar`

### Upgrading Mods

The `/modsync upgrade` command:

1.  Checks each mod for available updates
2.  Downloads and installs the new version
3.  Queues the old version for deletion

**Note:** A server restart is required to load the upgraded mods.

### Release Channels

Control which release types are considered for installation and updates:

| Channel   | Installs                       |
|-----------|--------------------------------|
| `release` | Only stable releases (default) |
| `beta`    | Beta and release versions      |
| `alpha`   | All versions including alpha   |

```
# Set global default
/modsync config channel beta

# Override for a specific mod
/modsync setchannel MyMod alpha

# Remove override (use global default)
/modsync setchannel MyMod default
```

## Important Notes

*   **Mod Distribution Setting**: Mods can only be downloaded if the mod author has enabled "Allow Mod Distribution" in their CurseForge project settings. If a mod doesn't allow API distribution, it will be skipped during installation.
*   **Server Restart**: Installing, upgrading, or removing mods requires a server restart to take effect.
*   **No Automatic Restart**: Currently, there is no way to trigger a server restart via the Mod API. After installing, upgrading, or removing mods, you must manually restart the server.
*   **File Locking (Windows)**: JAR files are locked while loaded. The bootstrap plugin handles deletion on restart.
*   **Supported Sources**: Currently CurseForge is the only supported mod source. The plugin architecture allows for additional sources to be added in the future.

## Configuration

The `config.json` file in `mods/Onyxmoon_ModSync/` contains:

```json
{
  "apiKeys": {
    "CURSEFORGE": "your-api-key"
  },
  "earlyPluginsPath": "earlyplugins"
}
```

*   `earlyPluginsPath` - Path for early plugins folder (default: `earlyplugins`). Can be absolute or relative to server root.

## File Locations

All data is stored in `mods/Onyxmoon_ModSync/`:

*   `config.json` - API keys and settings
*   `mods.json` - Your curated mod list (shareable between servers)
*   `mods.lock.json` - Installation state (machine-specific, not shareable)
*   `pending_deletions.json` - Files queued for deletion on restart

## Support

Report issues on the [GitHub repository](https://github.com/onyxmoon/modsync).
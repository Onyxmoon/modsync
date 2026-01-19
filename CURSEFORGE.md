# ModSync

> **Early Development Stage** - This plugin is in very early development. Use at your own risk. Development is ongoing - please be kind with feedback and bug reports!

A server-side mod management plugin for Hytale that lets you easily add, install, update, and remove mods directly from in-game commands.

> Required server flags (--early-plugins --accept-early-plugins)

## Features

- **Add mods via URL** - Simply paste a CurseForge mod URL to add it to your server's mod list
- **Easy installation** - Install all managed mods with a single command
- **Update checking** - Check for available updates with version comparison display
- **One-click upgrades** - Upgrade mods to their latest versions
- **Flexible removal** - Remove mods by index number, name, slug, or plugin identifier
- **Persistent tracking** - Your mod list is saved and persists across server restarts
- **Smart file handling** - Locked files are automatically queued for deletion on next server restart

## Commands

All commands use the `/modsync` prefix:

| Command                               | Description                                                        |
|---------------------------------------|--------------------------------------------------------------------|
| `/modsync add <url>`                  | Add a mod from a CurseForge URL                                    |
| `/modsync list`                       | Show all managed mods with install status, version, and identifier |
| `/modsync install`                    | Install all mods from your list                                    |
| `/modsync install <name\|identifier>` | Install a specific mod                                             |
| `/modsync remove`                     | Show numbered list for removal                                     |
| `/modsync remove <number>`            | Remove mod by list number                                          |
| `/modsync remove <name\|identifier>`  | Remove mod by name or identifier                                   |
| `/modsync remove --all`               | Remove all mods                                                    |
| `/modsync check`                      | Check for available updates (shows installed vs. latest version)   |
| `/modsync upgrade`                    | Upgrade all installed mods to latest version                       |
| `/modsync upgrade <name\|identifier>` | Upgrade a specific mod                                             |
| `/modsync setkey <key>`               | Set your CurseForge API key                                        |
| `/modsync status`                     | Show current configuration                                         |
| `/modsync reload`                     | Reload configuration                                               |

### Examples

```
/modsync add https://www.curseforge.com/hytale/mods/simply-trash
/modsync install simply-trash
/modsync install Onyxmoon:SimplyTrash
/modsync check
/modsync upgrade Onyxmoon:SimplyTrash
/modsync remove Onyxmoon:SimplyTrash
```

## Setup

1. Install the plugin on your Hytale server
2. Install the bootstrap plugin in the `earlyplugins` folder (included in release)
3. Required server flags (--early-plugins --accept-early-plugins)
5. Get a CurseForge API key from [CurseForge for Studios](https://console.curseforge.com/)
6. Set your API key: `/modsync setkey YOUR_API_KEY`
7. Start adding mods: `/modsync add https://www.curseforge.com/hytale/mods/your-mod`

## Requirements

- Hytale Server (release patchline)
- CurseForge API key
- Bootstrap plugin for proper file deletion (included)

## How It Works

### Installing Mods
When you install a mod, the plugin:
1. Downloads the JAR file from CurseForge
2. Saves it to the `mods` folder
3. Registers the mod in the installed mods registry

**Note:** A server restart is required to load newly installed mods.

### Removing Mods
When you remove a mod, the plugin:
1. Unloads the mod if currently loaded
2. Attempts to delete the JAR file
3. If the file is locked (Windows), it queues the file for deletion on next restart
4. The bootstrap plugin handles queued deletions before the server loads mods

**Note:** If a file is locked, you will see a message indicating a restart is required.

### Checking for Updates
The `/modsync check` command shows:
- Which mods have updates available
- Current installed version vs. latest available version
- Example output: `Simply Trash: Simply-Trash-0.0.1.jar -> Simply-Trash-0.0.2.jar`

### Upgrading Mods
The `/modsync upgrade` command:
1. Checks each mod for available updates
2. Downloads and installs the new version
3. Queues the old version for deletion

**Note:** A server restart is required to load the upgraded mods.

## Important Notes

- **Mod Distribution Setting**: Mods can only be downloaded if the mod author has enabled "Allow Mod Distribution" in their CurseForge project settings. If a mod doesn't allow API distribution, it will be skipped during installation.
- **Server Restart**: Installing, upgrading, or removing mods requires a server restart to take effect.
- **No Automatic Restart**: Currently, there is no way to trigger a server restart via the Mod API. After installing, upgrading, or removing mods, you must manually restart the server.
- **File Locking (Windows)**: JAR files are locked while loaded. The bootstrap plugin handles deletion on restart.
- **Supported Sources**: Currently CurseForge is the only supported mod source. The plugin architecture allows for additional sources to be added in the future.

## File Locations

All data is stored in `mods/Onyxmoon_ModSync/`:
- `config.json` - API keys and settings
- `managed_mods.json` - Your curated mod list
- `installed_mods.json` - Registry of installed mods with versions
- `pending_deletions.json` - Files queued for deletion on restart

## Support

Report issues on the [GitHub repository](https://github.com/onyxmoon/modsync).
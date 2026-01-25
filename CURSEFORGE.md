# ModSync

> **Early Development Stage** - This plugin is in very early development. Use at your own risk. Development is ongoing - please be kind with feedback and bug reports!

A server-side mod management plugin for Hytale dedicated servers that lets you easily add, install, update, and remove mods directly from in-game or console commands.

## Features

- **Multiple Sources** - CurseForge (API key required), CFWidget (no API key) and Modtale (alpha, API key required). More to come.
- **One-command install** - Install all managed mods at once
- **Full Mod Lifecycle** - Add, install, update, remove mods via commands
- **Update Management** - Check for updates, upgrade individual mods or all at once
- **Release Channels** - Choose Release, Beta, or Alpha versions (global + per-mod)
- **Import Existing Mods** - Scan and import unmanaged mods with auto-matching
- **Persistent tracking** - Your mod list is saved and persists across server restarts. Share your list with friends like a modpack!
- **Self-Upgrade** - Update ModSync itself from GitHub Releases
- **Console Support** - All commands work from in-game and server console

## Quick Start

1. Download `modsync-<version>.jar` from here or [GitHub-Releases](https://github.com/Onyxmoon/modsync/releases)
2. Place in your server's `mods/` folder
3. Start server
4. (Optional) Set your CurseForge API key: `/modsync config key curseforge <key>`
5. Add mods: `/modsync add https://curseforge.com/hytale/mods/example`

> Get your API key from [CurseForge Console](https://console.curseforge.com/) or [Modtale API Docs](https://modtale.net/api-docs)

> **Note:** Without an API key, ModSync uses CFWidget as fallback for CurseForge URLs. CFWidget supports URL-based lookups but not search-based import matching.
> **Note:** Modtale support is experimental/alpha and requires an API key.

## Commands

| Command                                | Description                               |
|----------------------------------------|-------------------------------------------|
| `/modsync add <url>`                   | Add mod from CurseForge URL               |
| `/modsync list`                        | Show all managed mods with install status |
| `/modsync install`                     | Install all mods from your list           |
| `/modsync install <name>`              | Install a specific mod                    |
| `/modsync remove <name>`               | Remove mod by name, slug, or identifier   |
| `/modsync remove all`                  | Remove all mods                           |
| `/modsync check`                       | Check for available updates               |
| `/modsync upgrade`                     | Upgrade all installed mods                |
| `/modsync upgrade <name>`              | Upgrade a specific mod                    |
| `/modsync scan`                        | List unmanaged mods in the mods folder    |
| `/modsync import`                      | Auto-match and import all unmanaged mods  |
| `/modsync import <target>`             | Auto-match and import a specific mod      |
| `/modsync import <target> --url=<url>` | Import with a specific CurseForge URL     |
| `/modsync config`                      | Show all configuration settings           |
| `/modsync config channel <value>`      | Set default release channel               |
| `/modsync config key <provider> <key>` | Set API key for a provider                |
| `/modsync config welcome <on\|off>`    | Enable or disable the admin welcome message |
| `/modsync setchannel <mod> <channel>`  | Set per-mod release channel               |
| `/modsync selfupgrade`                 | Check for ModSync plugin updates          |
| `/modsync selfupgrade apply`           | Download and install latest ModSync       |
| `/modsync status`                      | Show current configuration and version    |
| `/modsync reload`                      | Reload configuration from disk            |

**Tip:** Use quotes for names with spaces: `/modsync install "My Mod"`

## Release Channels

Control which release types are considered for installation and updates:

| Channel   | Versions Included              |
|-----------|--------------------------------|
| `release` | Stable releases only (default) |
| `beta`    | Beta and stable releases       |
| `alpha`   | All versions including alpha   |

```
/modsync config channel beta       # Set global default
/modsync setchannel MyMod alpha    # Override for specific mod
/modsync setchannel MyMod default  # Remove override, use global
```

## How It Works

### Installing Mods

The `/modsync add` and `/modsync install` command

When you install a mod, the plugin:

1. Add the mod to the managed list via an URL
2. Downloads the mod from the mod source and validates the manifest
3. Detects the plugin type (regular plugin or early plugin) from the CurseForge category
4. Saves it to the appropriate folder:
    *   Regular plugins → `mods/`
    *   Early/Bootstrap plugins → `earlyplugins/` (configurable)
5. Registers the mod in the installed mods registry

**Note:** A server restart is required to load newly installed mods.

### Removing Mods

The `/modsync remove` command

When you remove a mod, the plugin:

1.  Unloads the mod if currently loaded
2.  Attempts to delete the mod file 
3. If the file is locked (Windows), it queues the file for deletion on next restart. Install the bootstrap plugin to handle deletion on restart.

**Note:** If the file is locked (Windows), it queues the file for deletion on next restart. The bootstrap plugin handles queued deletions before the server loads mods.

### Checking and installing upgrades

The `/modsync check` command:

Shows which mods have updates available with version comparison (e.g., `1.0.1 -> 1.0.2`).

The `/modsync upgrade` command:

Upgrades all installed mods or a specific mod.

**Note:** A server restart is required to load upgraded mods.

### Importing Existing Mods

Use `/modsync scan` to find unmanaged mods, then `/modsync import` to bring them under ModSync control with auto-matching.

**Note:** This works only when a mod source is configured which supports search lookups (e.g., CurseForge).

## Bootstrap Plugin (Optional)

On Windows, JAR files are locked while loaded by the JVM. This prevents deletion of old mod versions during upgrades.

The bootstrap plugin is an **early plugin** that runs before normal plugins load:
1. Reads `pending_deletions.json`
2. Deletes queued files before they get loaded
3. Normal plugin loading proceeds

**Installation (only if you have deletion issues):**
1. Download from [modsync-bootstrap](https://curseforge.com/hytale/bootstrap/modsync-bootstrap)
2. Place in `earlyplugins/` folder
3. Start server with `--early-plugins --accept-early-plugins` flags

> **Note:** On Linux dedicated servers, file locking is typically not an issue and the bootstrap plugin is usually not needed.

## File Locations

All data is stored in `mods/Onyxmoon_ModSync/`:

*   `config.json` - API keys and settings
*   `mods.json` - Your curated mod list (shareable between servers)
*   `mods.lock.json` - Installation state (machine-specific, not shareable)
*   `pending_deletions.json` - Files queued for deletion on restart (only when there are locked files and the bootstrap plugin is required)

## Important Notes

- **Server restart required** - Currently, there is no way to trigger a server restart via the Mod API. After installing, upgrading, or removing mods, you must manually restart the server. If you server is auto-restarting, you may use the /stop command to trigger a restart.
- **Mod distribution** - Mods can only be downloaded if the author has enabled "Allow Mod Distribution" in their CurseForge project settings
- **Provider fallback** - Without a CurseForge API key, ModSync uses CFWidget which supports URL lookups but not search
- **File Locking (Windows)**: JAR files are locked while loaded. The bootstrap plugin handles deletion on restart.
- **Supported Sources**: CurseForge, Modtale (alpha), and CFWidget are supported. CFWidget uses the public widget API and does not require an API key, but does not support search-based imports.
- **Admin welcome message** - Disable the admin welcome message via `config.json` with `disableAdminWelcomeMessage: true`

## Support

- [GitHub Issues](https://github.com/onyxmoon/modsync/issues)
- [Changelog](https://github.com/Onyxmoon/modsync/blob/main/CHANGELOG.md)

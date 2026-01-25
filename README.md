# ModSync

[![Build](https://github.com/Onyxmoon/modsync/actions/workflows/build.yml/badge.svg)](https://github.com/Onyxmoon/modsync/actions/workflows/build.yml)
![Java 25](https://img.shields.io/badge/Java-25-orange)
![Hytale Plugin](https://img.shields.io/badge/Hytale-Plugin-purple)
![License](https://img.shields.io/badge/License-Polyform%20NC-blue)

Server-side mod management for Hytale dedicated servers. Add, install, update, and remove mods directly from in-game or console commands.

## Features

- **Multiple Sources** - CurseForge plus CFWidget (fallback for no API key) and Modtale (alpha, API key required). More to come.
- **Full Mod Lifecycle** - Add, install, update, remove mods via commands
- **Update Management** - Check for updates, upgrade individual mods or all at once
- **Release Channels** - Choose Release, Beta, or Alpha versions (global + per-mod)
- **Import Existing Mods** - Scan and import unmanaged mods with auto-matching
- **Self-Upgrade** - Update ModSync itself from GitHub Releases
- **Resilient Downloads** - Automatic retries with exponential backoff
- (optional) **Bootstrap Plugin** - Handles Windows file locking for seamless updates

## Requirements

- Java 25
- Hytale Server (release patchline)
- CurseForge API key (optional, but recommended for full functionality)
- Modtale API key (optional, required for Modtale)

## Installation

1. Download `modsync-<version>.jar` from [Releases](https://github.com/Onyxmoon/modsync/releases)
2. Place in your server's `mods/` folder
3. Start server
4. (Optional) Set your API key: `/modsync config key <provider> <key>`

> Get your API key from [CurseForge Console](https://console.curseforge.com/) or [Modtale API](https://modtale.net/api-docs)

> **Note:** Without an API key, ModSync uses CFWidget as fallback. CFWidget supports URL-based lookups but not search-based import matching.

## Commands

| Command                                 | Description |
|-----------------------------------------|-------------|
| `/modsync add <url>`                    | Add mod from CurseForge URL |
| `/modsync list`                         | Show all managed mods with install status, version, and identifier |
| `/modsync install`                      | Install all mods from your list |
| `/modsync install <name>`               | Install a specific mod by name, slug, or identifier |
| `/modsync remove <name>`                | Remove mod by name, slug, or identifier |
| `/modsync remove all`                   | Remove all mods |
| `/modsync check`                        | Check for available updates (shows installed vs. latest version) |
| `/modsync upgrade`                      | Upgrade all installed mods to latest version |
| `/modsync upgrade <name>`               | Upgrade a specific mod by name, slug, or identifier |
| `/modsync scan`                         | List unmanaged mods in the mods folder |
| `/modsync import`                       | Auto-match and import all unmanaged mods |
| `/modsync import <target>`              | Auto-match and import a specific unmanaged mod |
| `/modsync import <target> --url= <url>` | Import an unmanaged mod with a specific CurseForge URL |
| `/modsync config`                       | Show all configuration settings |
| `/modsync config channel <value>`       | Set default release channel (release/beta/alpha) |
| `/modsync config key <provider> <key>`  | Set API key for a provider |
| `/modsync config welcome <on\|off>`      | Enable or disable the admin welcome message |
| `/modsync setchannel <mod> <channel>`   | Set per-mod release channel override |
| `/modsync selfupgrade`                  | Check for ModSync plugin updates |
| `/modsync selfupgrade apply`            | Download and install the latest ModSync version |
| `/modsync status`                       | Show current configuration and version |
| `/modsync reload`                       | Reload configuration from disk |

**Target formats:** mod name, slug, or identifier (`Group:Name`)

> **Tip:** Use quotes for names with spaces: `/modsync install "My Mod"`

## How It Works

### Installing Mods

When you install a mod, ModSync:

1. Downloads the JAR file from CurseForge
2. Validates the plugin manifest and calculates file hash
3. Detects the plugin type (regular or early plugin) from CurseForge category
4. Saves to the appropriate folder (`mods/` or `earlyplugins/`)
5. Registers the mod in the installed mods registry

**Note:** A server restart is required to load newly installed mods.

### Removing Mods

When you remove a mod, ModSync:

1. Unloads the mod if currently loaded
2. Attempts to delete the JAR file
3. If the file is locked (common on Windows), queues it for deletion on restart

### Checking for Updates

The `/modsync check` command shows:

- Which mods have updates available
- Current installed version vs. latest available version
- Example: `1.0.1 -> 1.0.2`

### Upgrading Mods

The `/modsync upgrade` command:

1. Checks each mod for available updates
2. Downloads and installs the new version
3. Queues the old version for deletion

**Note:** A server restart is required to load upgraded mods.

### Importing Existing Mods

Use `/modsync scan` to find unmanaged mods, then `/modsync import` to bring them under ModSync control:

- **Auto-matching** tries slug lookup and name search on CurseForge
- **Manual import** with URL: `/modsync import mymod.jar https://curseforge.com/hytale/mods/example`

> **Note:** Import and scan currently require a CurseForge API key for auto-matching. CFWidget and Modtale do not support search-based imports.

## Release Channels

Control which release types are considered for installation and updates:

| Channel | Versions Included |
|---------|-------------------|
| `release` | Stable releases only (default) |
| `beta` | Beta and stable releases |
| `alpha` | All versions including alpha |

```
/modsync config channel beta       # Set global default
/modsync setchannel MyMod alpha    # Override for specific mod
/modsync setchannel MyMod default  # Remove override, use global
```

**Automatic Fallback:** If a mod has no releases for your configured channel (e.g., only Beta versions but you're on Release), ModSync automatically falls back to Beta, then Alpha. A warning is shown so you know which version type was used.

## Configuration

The `config.json` file in `mods/Onyxmoon_ModSync/` contains:

```json
{
  "apiKeys": {
    "CURSEFORGE": "your-api-key"
  },
  "defaultReleaseChannel": "RELEASE",
  "earlyPluginsPath": "earlyplugins",
  "checkForPluginUpdates": true,
  "includePrereleases": false,
  "disableAdminWelcomeMessage": false
}
```

| Setting | Description |
|---------|-------------|
| `apiKeys` | API keys per provider (currently CurseForge and Modtale) |
| `defaultReleaseChannel` | Global release channel (RELEASE, BETA, ALPHA) |
| `earlyPluginsPath` | Path for early plugins folder (default: `earlyplugins`) |
| `checkForPluginUpdates` | Check for ModSync updates on startup |
| `includePrereleases` | Include prerelease versions in self-upgrade checks |
| `disableAdminWelcomeMessage` | Disable the admin welcome message on join |

## File Locations

All data is stored in `mods/Onyxmoon_ModSync/`:

| File | Purpose |
|------|---------|
| `config.json` | API keys and plugin settings |
| `mods.json` | Your mod list (shareable between servers) |
| `mods.lock.json` | Installation state (machine-specific) |
| `pending_deletions.json` | Files queued for deletion on restart |

## Bootstrap Plugin (Windows)

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

## Important Notes

- **Server restart required** - Installing, upgrading, or removing mods requires a server restart
- **Mod distribution** - Mods can only be downloaded if the author has enabled "Allow Mod Distribution" in their CurseForge project settings
- **No automatic restart** - There is no way to trigger a server restart via the Mod API; you must restart manually
- **Provider fallback** - Without a CurseForge API key, ModSync uses CFWidget which supports URL lookups but not search
- **Modtale (alpha)** - Modtale support is experimental and requires an API key

## Building

```bash
gradlew.bat build    # Windows
./gradlew build      # Unix
```

Output: `build/libs/modsync-<version>.jar`

## Architecture

```
modsync/
├── src/main/java/           # Main plugin
│   └── de/onyxmoon/modsync/
│       ├── api/             # Provider interfaces, models
│       ├── command/         # All commands
│       ├── provider/        # CurseForge, CFWidget providers
│       ├── service/         # Download, scan, upgrade services
│       ├── storage/         # Config, mod registry storage
│       └── util/            # Helpers and utilities
└── bootstrap/               # Early plugin for file deletion
```

**Provider System:** SPI-based via `ServiceLoader`. Implement `ModListProvider` and register in `META-INF/services` to add new mod sources.

## License

[Polyform NonCommercial 1.0.0](LICENSE)

- Use for personal/non-commercial purposes
- Commercial use requires author permission

## Links

- [CurseForge](https://curseforge.com/hytale/mods/modsync)
- [Issues](https://github.com/Onyxmoon/modsync/issues)
- [Changelog](CHANGELOG.md)

# ModSync

![Java](https://img.shields.io/badge/Java-25-orange)
![Status](https://img.shields.io/badge/Status-Alpha-yellow)
[![Build Plugin](https://github.com/Onyxmoon/modsync/actions/workflows/build.yml/badge.svg)](https://github.com/Onyxmoon/modsync/actions/workflows/build.yml)
![License](https://img.shields.io/badge/License-Polyform%20NC-blue)
![Hytale](https://img.shields.io/badge/Hytale-Plugin-purple)

A Hytale server plugin for managing server-side mods. ModSync allows server administrators to add mods via CurseForge URLs, track them in a managed list, and install/update them via in-game commands.

## Features

- **CurseForge Integration** - Add mods directly from CurseForge URLs
- **Mod Management** - Track, install, update, and remove mods via commands
- **Version Tracking** - Check for updates and upgrade mods to latest versions
- **Release Channels** - Choose between Release, Beta, or Alpha versions (global default + per-mod override)
- **Import Existing Mods** - Import unmanaged mods into ModSync with auto-matching
- **Identifier Support** - Reference mods by `group:name` format (e.g., `Onyxmoon:SimplyTrash`)
- **Self-Upgrade** - Update ModSync itself via `/modsync selfupgrade`
- **Bootstrap Plugin** - Handles Windows file locking for seamless mod updates
- **Resilient Downloads** - Automatic retries with exponential backoff

## Quick Start (Using the mod)

### Prerequisites

- Java 25
- Hytale Server (release patchline)
- CurseForge API key

### Installation

1. Download the latest release from [CurseForge](https://curseforge.com/hytale/mods/modsync)
2. Download the bootstrap plugin from [CurseForge](https://curseforge.com/hytale/bootstrap/modsync-bootstrap)
3. Place the main plugin JAR in your server's `mods/` directory
4. Place the bootstrap JAR in your server's `earlyplugins/` directory
5. Start the server with `--early-plugins` and `--accept-early-plugins` flags
6. Set your CurseForge API key: `/modsync setkey <your-api-key>`

## Commands

All commands are subcommands of `/modsync`:

| Command                      | Description                                                     |
|------------------------------|-----------------------------------------------------------------|
| `add <url>`                  | Add a mod from a CurseForge URL                                 |
| `list`                       | Show all managed mods with install status and version           |
| `install [target]`           | Install mod by name/slug/identifier (no argument = install all) |
| `remove <target>`            | Remove mod by name/slug/identifier, or `all` for all            |
| `check`                      | Check for available updates (shows version comparison)          |
| `upgrade [target]`           | Upgrade mod by name/slug/identifier (no argument = upgrade all) |
| `scan`                       | List unmanaged mods in the mods folder                          |
| `import <target> [url]`      | Import an unmanaged mod (auto-match or manual URL)              |
| `config`                     | Show all configuration settings                                 |
| `config channel [value]`     | View or set default release channel (release/beta/alpha)        |
| `setchannel <mod> <channel>` | Set per-mod release channel override                            |
| `setkey <key>`               | Set your CurseForge API key                                     |
| `status`                     | Show current configuration                                      |
| `reload`                     | Reload configuration from disk                                  |

> **Tip:** Use quotes for names with spaces: `/modsync install "My Mod"`

### Examples

```
/modsync add https://curseforge.com/hytale/mods/example-mod
/modsync list
/modsync install
/modsync install ExampleMod
/modsync install "Example Mod"
/modsync check
/modsync upgrade
/modsync upgrade Onyxmoon:ExampleMod
/modsync remove example-mod
/modsync remove all
```

## Configuration

### API Key Setup

ModSync requires a CurseForge API key to fetch mod information. Get your key from the [CurseForge Console](https://console.curseforge.com/).

```
/modsync setkey <your-api-key>
```

### Release Channels

Control which release types are considered for installation and updates:

| Channel | Installs |
|---------|----------|
| `release` | Only stable releases (default) |
| `beta` | Beta and release versions |
| `alpha` | All versions including alpha |

```
# Set global default
/modsync config channel beta

# Override for a specific mod
/modsync setchannel MyMod alpha

# Remove override (use global default)
/modsync setchannel MyMod default
```

### Data Files

All data files are stored in `mods/Onyxmoon_ModSync/`:

| File | Purpose |
|------|---------|
| `config.json` | API keys and plugin settings |
| `mods.json` | Your tracked mod list (shareable) |
| `mods.lock.json` | Installation state (machine-specific) |
| `pending_deletions.json` | Files queued for deletion on next startup |

## Building from Source

### Prerequisites

- Java 25 JDK
- Hytale installed (for HytaleServer.jar dependency)

### Build Commands

```bash
# Windows
gradlew.bat build

# Unix
./gradlew build

# Clean build
gradlew.bat clean build
```

The build output will be in `build/libs/`.

### Project Structure

```
modsync/
├── src/main/java/          # Main plugin source
├── bootstrap/              # Bootstrap early plugin
│   └── src/main/java/      # Bootstrap source
├── build.gradle            # Main build configuration
└── settings.gradle         # Multi-project settings
```

## Architecture

### Plugin Lifecycle

1. `ModSyncPlugin.setup()` - Initializes storage and provider registry
2. `ModSyncPlugin.start()` - Registers commands
3. `ModSyncPlugin.shutdown()` - Saves configuration

### Provider System

ModSync uses a Service Provider Interface (SPI) for mod sources:

- `ModListProvider` - Interface for mod sources
- `ProviderRegistry` - Loads providers via `ServiceLoader`
- `CurseForgeProvider` - Current implementation

To add new mod sources, implement `ModListProvider` and register in `META-INF/services`.

### Bootstrap Plugin

The bootstrap plugin is an "early plugin" that runs before normal plugins load. It handles file deletion for mods that were locked during runtime (common on Windows when JAR files are loaded as plugins).

**How it works:**
1. When a mod file cannot be deleted, it's queued in `pending_deletions.json`
2. On next server start, the bootstrap plugin runs first
3. It deletes the queued files before they get loaded
4. Normal plugin loading proceeds

## Roadmap

- Additional mod source providers (Modrinth, etc.)
- Mod dependency resolution
- Automatic update scheduling
- Web dashboard for remote management

## Contributing

Contributions are welcome! Please:

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Submit a pull request

For major changes, please open an issue first to discuss what you would like to change.

## License

This project is licensed under the [Polyform NonCommercial 1.0.0](LICENSE) license.

**You may:**
- View, copy, modify, and distribute for non-commercial purposes
- Use for personal servers and private use

**You may not:**
- Use commercially without explicit permission from the author

## Acknowledgements

- [Hypixel Studios](https://hypixelstudios.com/) for Hytale
- [CurseForge](https://curseforge.com/) for their mod hosting platform and API

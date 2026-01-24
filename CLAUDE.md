# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

ModSync is a Hytale server plugin for managing server-side mods. It allows server administrators to add mods via CurseForge URLs, track them in a managed list, and install/update them via in-game commands.

## Project Structure

This is a **multi-project Gradle build** with two subprojects:
- **Root project** (`modsync`) - The main plugin
- **Bootstrap subproject** (`bootstrap/`) - Early plugin for file deletion on startup

## Build Commands

```bash
# Build both projects (Windows)
gradlew.bat build

# Build both projects (Unix)
./gradlew build

# Clean build
gradlew.bat clean

# Generate VSCode launch configuration
gradlew.bat generateVSCodeLaunch
```

The build system automatically:
- Detects Hytale installation path from `%appdata%/Hytale` (Windows)
- Updates `manifest.json` version from `gradle.properties`
- Creates IntelliJ run configuration for HytaleServer
- Builds the bootstrap plugin and copies it to `run/earlyplugins/`
- Adds `--early-plugins` and `--accept-early-plugins` flags to run configuration

## Running the Server

Use the **HytaleServer** run configuration in IntelliJ IDEA. First-time setup requires authentication:
1. Run the HytaleServer configuration
2. Execute `auth login device` in the terminal
3. Complete login via the URL provided
4. Run `auth persistence Encrypted`

Connect via Hytale client to `127.0.0.1`.

## Architecture

### Plugin Lifecycle
`ModSyncPlugin` (entry point) → `setup()` initializes storage/registry → `start()` registers commands → `shutdown()` saves config

### Bootstrap Plugin (Early Plugin)
The bootstrap plugin (`bootstrap/`) runs **before** normal plugins load:
- Implements `ClassTransformer` interface (no-op transformer)
- Reads `pending_deletions.json` from the ModSync data directory
- Deletes files that were locked during runtime (because they were loaded as plugins)
- Located in `run/earlyplugins/modsync-bootstrap-<version>.jar`

**Why needed:** On Windows, JAR files loaded as plugins are locked and cannot be deleted while the server is running. The bootstrap plugin deletes them on the next server start, before the PluginManager loads mods.

### Pending Deletions System
When a mod file cannot be deleted (locked):
1. `ModDownloadService.deleteMod()` catches the IOException
2. Calls `ModSync.addPendingDeletion(path)` to queue the file
3. Path is written to `mods/Onyxmoon_ModSync/pending_deletions.json`
4. On next server start, bootstrap plugin deletes the files

### Provider System (SPI-based)
- `ModListProvider` interface defines the contract for mod sources (fetchMod, fetchModBySlug, validateApiKey)
- `ProviderRegistry` loads providers via `ServiceLoader` (META-INF/services)
- `CurseForgeProvider` is the current implementation using `CurseForgeClient` for API calls
- To add new sources: implement `ModListProvider`, register in META-INF/services

### Storage Layer
- `ConfigurationStorage` - plugin config (API keys per source)
- `ManagedModStorage` - unified storage using two files:
  - `mods.json` - mod list (versionable, shareable)
  - `mods.lock.json` - installation state (machine-specific)
- All use Gson with custom `InstantTypeAdapter`

### Data Models
- `ManagedMod` - immutable, unified model with optional `InstalledState` (use `toBuilder()` for updates)
- `InstalledState` - embedded state when mod is installed (identifier, version, file path, hash)
- `ManagedModRegistry` - immutable collection with lookup methods (findByName, findByIdentifier, etc.)
- `ModEntry` / `ModVersion` / `ModAuthor` / `ModList` - API response models from providers (in `api.model.provider` package)

### Commands
All commands are subcommands of `/modsync`:
- `add <url>` - Add mod from CurseForge URL
- `remove <target>` - Remove mod by name/slug/identifier, or `--all` for all
- `list` - Show all managed mods with install status, version, and identifier
- `install <target>` - Install mod by name/slug/identifier, or `--all` for all
- `check` - Check for available updates (shows version comparison)
- `upgrade <target>` - Upgrade mod by name/slug/identifier, or `--all` for all
- `config key <provider> <key>` - Set API key for a provider (e.g., CurseForge)
- `status` - Show current configuration
- `reload` - Reload configuration from disk

**Identifier format:** `group:name` (e.g., `Onyxmoon:SimplyTrash`) - can be used in remove/install/upgrade commands.

**Command argument pattern:** Commands use `RequiredArg` for positional arguments. Without argument, Hytale shows usage help.

### Version Comparison
- CurseForge File-ID is used as `versionId`
- `InstalledState.installedVersionId` stores the installed file ID
- `ModVersion.versionId` contains the latest file ID from CurseForge
- Comparison: `latestVersion.getVersionId().equals(installedState.getInstalledVersionId())`

### Message Formatting
Use Hytale's Message API with `.color("colorname")`:
```java
playerRef.sendMessage(Message.raw("Text").color("red"));
playerRef.sendMessage(Message.raw("Part 1").color("gray")
    .insert(Message.raw("Part 2").color("green")));
```
Available colors: black, dark_blue, dark_green, dark_aqua, dark_red, dark_purple, gold, gray, dark_gray, blue, green, aqua, red, light_purple, yellow, white

## Key Configuration

`gradle.properties`:
- `version` - Plugin version (updates manifest.json)
- `java_version=25` - Required Java version
- `includes_pack=true` - Include as asset pack
- `patchline=release` - Hytale release channel

`manifest.json`:
- `Main` must match entry point class: `de.onyxmoon.modsync.ModSync`

## Data Files

Located in `mods/Onyxmoon_ModSync/`:
- `config.json` - API keys and settings
- `mods.json` - Mod list (versionable, shareable, schema version in file)
- `mods.lock.json` - Installation state (machine-specific, schema version in file)
- `pending_deletions.json` - Files to delete on next startup

## Dependencies

HytaleServer.jar is loaded from the local Hytale installation (compileOnly scope).

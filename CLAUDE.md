# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

ModSync is a Hytale server plugin for managing server-side mods. It allows server administrators to add mods via CurseForge URLs, track them in a managed list, and install/update them via in-game commands.

## Build Commands

```bash
# Build the project (Windows)
gradlew.bat build

# Build the project (Unix)
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

### Provider System (SPI-based)
- `ModListProvider` interface defines the contract for mod sources (fetchMod, fetchModBySlug, validateApiKey)
- `ProviderRegistry` loads providers via `ServiceLoader` (META-INF/services)
- `CurseForgeProvider` is the current implementation using `CurseForgeClient` for API calls
- To add new sources: implement `ModListProvider`, register in META-INF/services

### Storage Layer
- `ConfigurationStorage` - plugin config (API keys per source)
- `ManagedModListStorage` - user's mod list (mods added via URLs)
- `InstalledModStorage` - tracks installed mods with file paths
- All use Gson with custom `InstantTypeAdapter`

### Data Models
- `ManagedModEntry` - immutable, represents a mod the user wants to track (use `toBuilder()` for updates)
- `ManagedModList` - immutable collection of managed mods
- `InstalledMod` - tracks an installed mod file
- `ModEntry` / `ModVersion` - API response models from providers

### Commands
All commands are subcommands of `/modsync`:
- `add <url>` - Add mod from CurseForge URL
- `remove [index|--all|name]` - Remove mod(s) interactively or by index/name
- `list` - Show all managed mods with install status
- `install [name]` - Install all or specific mod
- `check` - Check for available updates
- `update` - Refresh metadata for all mods
- `setkey <key>` - Set CurseForge API key
- `status` - Show current configuration
- `reload` - Reload configuration from disk

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

## Dependencies

HytaleServer.jar is loaded from the local Hytale installation (compileOnly scope).
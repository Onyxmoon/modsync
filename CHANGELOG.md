# Changelog

All notable changes to ModSync will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.2.0] - 2026-01-21

### Added
- **Unified data model**: `ManagedMod` with embedded `InstalledState` replaces separate `ManagedModEntry` and `InstalledMod`
- **Split storage files**: `mods.json` (shareable mod list) and `mods.lock.json` (machine-specific installation state)
- **ModSelector utility**: Centralized mod lookup by name, slug, or identifier
- **CommandUtils utility**: Shared error message extraction for commands
- **Automatic migration**: Old `managed_mods.json` and `installed_mods.json` files are automatically migrated to the new format

### Changed
- **Command syntax**: `install`, `remove`, and `upgrade` now use positional arguments instead of optional named arguments
  - `/modsync install --all` or `/modsync install <name>`
  - `/modsync remove --all` or `/modsync remove <name>`
  - `/modsync upgrade --all` or `/modsync upgrade <name>`
- **Removed index-based removal**: Use name, slug, or identifier instead of index numbers
- **Logger format**: Fixed Flogger placeholder format (`%s`/`%d` instead of `{}`)

### Fixed
- Commands now work correctly with positional arguments (was using `OptionalArg` which required `--target=value` syntax)
- Exception logging now uses `.withCause(ex)` instead of passing exception as parameter

## [0.1.0] - 2026-01-21

### Added
- **Bootstrap/Early Plugin support**: Automatically detects and installs early plugins to the `earlyplugins/` folder
- **CurseForge Bootstrap URLs**: Support for `https://www.curseforge.com/hytale/bootstrap/...` URLs
- **Plugin type display**: Commands now show `[Plugin]` or `[Early Plugin]` for each mod
- **Configurable early plugins path**: New `earlyPluginsPath` setting in `config.json` (default: `earlyplugins`)
- Config is now saved immediately on startup to persist new default fields

### Changed
- `ModDownloadService` now supports both `mods/` and `earlyplugins/` folders
- URL parser updated to support both `/mods/` and `/bootstrap/` URL patterns

### Fixed
- Null safety for `pluginType` field when loading old config files without this field

## [0.0.2] - 2026-01-19

### Added
- Initial release
- Add mods via CurseForge URL (`/modsync add`)
- List managed mods with install status (`/modsync list`)
- Install mods from managed list (`/modsync install`)
- Remove mods by index, name, or identifier (`/modsync remove`)
- Check for available updates (`/modsync check`)
- Upgrade mods to latest version (`/modsync upgrade`)
- Set CurseForge API key (`/modsync setkey`)
- View configuration status (`/modsync status`)
- Reload configuration (`/modsync reload`)
- Persistent mod tracking across server restarts
- Smart file handling with pending deletions for locked files
- Bootstrap plugin for file deletion on Windows
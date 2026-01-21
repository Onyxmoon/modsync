# Changelog

All notable changes to ModSync will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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
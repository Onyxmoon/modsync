# Changelog

All notable changes to ModSync will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.7.0] - 2026-01-25

### Added
- **CFWidget provider**: URL-based mod lookups via the CFWidget API (no API key required)
- **Modtale provider (alpha)**: Experimental Modtale support behind API keys
- **StringUtils**: New utility class for consistent null/blank string handling
- **Config**: Option to disable the admin welcome message
- **Release channel fallback**: If no version is found for the configured channel (e.g., Release), ModSync now automatically falls back to Beta, then Alpha. A warning is shown when a fallback is used.
- **DownloadHandler interface**: Providers can now implement custom download logic (e.g., authenticated downloads with API key headers, filename extraction from Content-Disposition)
- **Extensible provider system**: Sources are now string-based, allowing external providers in separate JARs to define custom source identifiers

### Changed
- **URL handling**: Providers now parse URLs directly and are tried in priority order (CurseForge first)
- **Config keys**: API keys are now set per provider via `/modsync config key <provider> <key>` (no global current source)
- **Provider fetching**: URL resolution for add/import now uses a shared fetch service (consistent API key handling and fallbacks)
- **Command output**: Status/formatting helpers consolidated for check/upgrade commands
- **Download/scan utilities**: Hashing and manifest parsing centralized in shared helpers
- **Storage models**: `mods.json`/`mods.lock.json` now built from immutable model objects
- **HTTP clients**: CurseForge/CFWidget reuse shared `HttpClient` and `Gson` instances with longer timeouts
- **Thread-safety**: `ProviderRegistry` now uses `ConcurrentHashMap`, `ManagedModStorage` uses `volatile` for registry field
- **Magic numbers**: Extracted constants for timeouts, retry attempts, and intervals (`CONNECT_TIMEOUT_SECONDS`, `MAX_RETRY_ATTEMPTS`, `STARTUP_DELAY_SECONDS`, etc.)
- **Provider structure**: URL parsers and download handlers are now internal helpers, keeping provider classes focused
- **Modtale downloads**: Now uses authenticated downloads with `X-MODTALE-KEY` header and extracts correct filename from `Content-Disposition`

### Fixed
- **Import messaging**: Clearer errors when no provider can resolve a URL or when search is unsupported
- **Beta/Alpha-only mods**: Mods with only Beta or Alpha releases can now be installed (automatic fallback from Release channel)

## [0.6.3] - 2026-01-24

### Fixed
- **Console list crash**: Removed invalid formatted header parsing that caused `/modsync list` to throw a JSON parse error

### tl;dr
> Fixed console list crash;

## [0.6.2] - 2026-01-24

### Changed
- **Console support**: ModSync commands can now be executed from the server console (player permissions still required in-game)

### tl;dr
> ModSync commands now work from the server console; in-game usage still requires permissions.

## [0.6.1] - 2026-01-24

### Changed
- **Console output layout**: Output formatting refined across commands for consistent, terminal-friendly lines
- **Check/upgrade output layout**: Status line now keeps only mod name + status, with identifier and versions on their own lines
- **Version display extraction**: Prefer semver-like values for compact `old -> new` output, with raw fallback when unavailable

### tl;dr
> Console output is cleaner and more consistent across commands; update versions show as `1.0.1 -> 1.0.19` when detectable, otherwise raw filenames.

## [0.6.0] - 2026-01-23

### Added
- **Import Feature**: Import existing unmanaged mods into ModSync management
  - `/modsync scan` - List all unmanaged JAR/ZIP files in mods folder
  - `/modsync import <target>` - Auto-match and import a mod
  - `/modsync import <target> --url=<url>` - Manual import with CurseForge URL
  - Auto-matching tries slug lookup and name search
- **Search API**: Provider interface now supports mod search for import matching

### Changed
- **File scanning**: Now supports both JAR and ZIP files

### tl;dr
> Import existing mods with `/modsync scan` and `/modsync import`. Auto-matching tries to find your mod on CurseForge automatically.

## [0.5.0] - 2026-01-23

### Added
- **Release Channel System**: Control which release types (Release, Beta, Alpha) are considered for installation and updates
  - Global default channel via `/modsync config channel <release|beta|alpha>`
  - Per-mod override via `/modsync setchannel <mod> <release|beta|alpha|default>`
- **Config command**: New `/modsync config` shows all configuration at a glance
- **Version selection**: Install and upgrade commands now respect release channel settings
- **Channel display**: `list` and `check` commands show effective channel when not using default

### Changed
- **Provider architecture**: CurseForge adapter now fetches all available versions instead of just the latest, enabling channel-based filtering
- **ModEntry model**: Now includes `availableVersions` list for version selection
- **Storage format**: `mods.json` now stores per-mod `releaseChannelOverride` (backwards compatible)

### tl;dr
> New release channel system lets you choose between stable releases, beta, or alpha versions. Set a global default with `/modsync config channel beta` or override per-mod with `/modsync setchannel MyMod alpha`.

## [0.4.1] - 2026-01-22

### Fixed
- **Self-upgrade crash**: Fixed crash when trying to upgrade to the same version (now skips with "already at target version")
- **Bootstrap auto-update disabled**: Bootstrap plugin can no longer be auto-updated due to file locking issues (both old and new would be loaded). If a bootstrap update is available, a hint is shown for manual update.

### tl;dr
> Fixed self-upgrade crashes when target files are locked. Bootstrap updates now require manual installation.

## [0.4.0] - 2026-01-22

### Added
- **Self-upgrade command**: `/modsync selfupgrade [check|apply]` to check for and install ModSync updates directly from GitHub Releases
- **Version display**: Plugin version is now shown in logs on startup and via `/modsync status`
- **Build-time version**: Version is now embedded at build time via generated `BuildInfo.java`
- **Semantic versioning**: Full support for comparing versions including prereleases (e.g., `1.0.0-alpha < 1.0.0`)
- **Config options**: New settings `checkForPluginUpdates` and `includePrereleases` for self-upgrade behavior

### Changed
- **Atomic mod installation**: Mods are now validated (manifest, hash) before being moved to their final location - no more orphan files if validation fails
- **Resilient downloads**: Both mod downloads and self-upgrades now use retry logic (3 attempts with exponential backoff) and fallback file operations

### Fixed
- **Orphan mod files**: Fixed issue where downloaded mods could remain in the mods folder without being tracked if manifest reading failed
- **File move crashes**: Download operations now gracefully handle locked files and filesystem errors

### tl;dr
> ModSync can now update itself! Use `/modsync selfupgrade check` to see if a new version is available, and `/modsync selfupgrade apply` to install it. Downloads are now more reliable with automatic retries and proper cleanup on failure.

## [0.3.2] - 2026-01-22

### Fixed
- **CurseForge download reliability**: Improved handling of temporary download errors by adding a fallback strategy to increase robustness

### tl;dr
> ModSync handles failed CurseForge downloads more gracefully, resulting in fewer missing mods. More mods can now be downloaded successfully with ModSync.

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

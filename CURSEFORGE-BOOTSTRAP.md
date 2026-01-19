# ModSync Bootstrap

> **Early Development Stage** - This plugin is in very early development. Use at your own risk. Development is ongoing - please be kind with feedback and bug reports!

**Summary:** Required dependency for ModSync. Handles file deletions before mods are loaded to work around Windows file locking.

A bootstrap/early plugin for Hytale that handles file deletions before the server loads mods. This is a **required dependency** for the [ModSync](https://www.curseforge.com/hytale/mods/modsync) plugin.

## Why Is This Needed?

On Windows, JAR files are locked while the server is running. This means mods cannot be deleted or replaced while they are loaded. The bootstrap plugin runs **before** mods are loaded and handles any pending file deletions queued by ModSync.

## Installation

1. Download the `modsync-bootstrap.jar` file
2. Place it in your server's `earlyplugins` folder (create if it doesn't exist)
3. Start the server with the `--early-plugins` and `--accept-early-plugins` flags

## How It Works

1. ModSync queues files for deletion in `mods/Onyxmoon_ModSync/pending_deletions.json`
2. On server startup, the bootstrap plugin runs before any mods are loaded
3. It reads the pending deletions file and deletes the queued files
4. The pending deletions file is cleared after processing
5. The server continues to load mods normally

## Requirements

- Hytale Server (release patchline)
- Server must be started with `--early-plugins` and `--accept-early-plugins` flags

## Server Start Flags

Add these flags when starting your Hytale server:

```
--early-plugins --accept-early-plugins
```

## Important Notes

- **Library Only**: This plugin does nothing on its own. It is a required dependency for ModSync.
- **No Configuration**: There are no settings to configure. The plugin automatically processes pending deletions.
- **Safe Operation**: If no pending deletions exist, the plugin does nothing.
- **Windows Compatibility**: Primarily needed for Windows servers where file locking is an issue.

## Support

Report issues on the [GitHub repository](https://github.com/onyxmoon/modsync).
# ModSync

A server-side mod management plugin for Hytale that lets you easily add, install, and update mods directly from in-game commands.

## Features

- **Add mods via URL** - Simply paste a CurseForge mod URL to add it to your server's mod list
- **Easy installation** - Install all managed mods with a single command
- **Update checking** - Check for available updates across all your mods
- **Flexible removal** - Remove mods by index number, name, or clear all at once
- **Persistent tracking** - Your mod list is saved and persists across server restarts

## Commands

All commands use the `/modsync` prefix:

| Command                    | Description                               |
|----------------------------|-------------------------------------------|
| `/modsync add <url>`       | Add a mod from a CurseForge URL           |
| `/modsync list`            | Show all managed mods with install status |
| `/modsync install`         | Install all mods from your list           |
| `/modsync install <name>`  | Install a specific mod                    |
| `/modsync remove`          | Show numbered list for removal            |
| `/modsync remove <number>` | Remove mod by list number                 |
| `/modsync remove --all`    | Remove all mods                           |
| `/modsync check`           | Check for available updates               |
| `/modsync update`          | Refresh mod metadata                      |
| `/modsync setkey <key>`    | Set your CurseForge API key               |
| `/modsync status`          | Show current configuration                |
| `/modsync reload`          | Reload configuration                      |

## Setup

1. Install the plugin on your Hytale server
2. Get a CurseForge API key from [CurseForge for Studios](https://console.curseforge.com/)
3. Set your API key: `/modsync setkey YOUR_API_KEY`
4. Start adding mods: `/modsync add https://www.curseforge.com/hytale/mods/your-mod`

## Requirements

- Hytale Server (release patchline)
- CurseForge API key

## Important Notes

- **Mod Distribution Setting**: Mods can only be downloaded if the mod author has enabled "Allow Mod Distribution" in their CurseForge project settings. If a mod doesn't allow API distribution, it will be skipped during installation.
- **Supported Sources**: Currently CurseForge is the only supported mod source. The plugin architecture allows for additional sources to be added in the future.

## Support

Report issues on the [GitHub repository](https://github.com/your-repo/modsync).
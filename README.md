# BackroomsGen

A Paper/Spigot plugin that adds Backrooms dimension generation to Minecraft servers. Features multi-level world generation with custom chunk generators and integrations with BlueMap, MultiWorld, WorldEdit, and WorldGuard.

## Features

- **Multi-Level Generation**: Three distinct Backrooms levels with unique themes
  - Level 0 - The Lobby (classic yellow rooms)
  - Level 1 - Habitable Zone (darker, larger rooms)
  - Level 2 - Pipe Dreams (deepslate industrial theme)
- **Custom Chunk Generator**: Procedural room generation with configurable sizes, materials, and lighting
- **Plugin Integrations**: BlueMap, MultiWorld, WorldEdit, WorldGuard support
- **Configurable**: Customize room dimensions, materials, lighting, mob spawning, and more
- **Admin Commands**: Create worlds, reload config, view level info in-game

## Requirements

- Minecraft 1.21+
- Paper/Spigot server
- Java 21+

## Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/backrooms` | Teleport to the Backrooms world | `backrooms.use` |
| `/backroomsadmin create` | Create the Backrooms world | `backrooms.admin` |
| `/backroomsadmin reload` | Reload configuration | `backrooms.admin` |
| `/backroomsadmin setlevel <id>` | View level info | `backrooms.admin` |
| `/backroomsadmin info` | Show plugin info | `backrooms.admin` |

## Building

```bash
cd BackroomsPlugin
./gradlew build
```

The compiled jar will be in `BackroomsPlugin/build/libs/`.

## Installation

1. Place the compiled jar in your server's `plugins/` folder
2. Start/restart the server
3. Run `/backroomsadmin create` to generate the Backrooms world
4. Players can use `/backrooms` to enter

## Configuration

Edit `plugins/BackroomsGen/config.yml` to customize:
- World name
- Level definitions (Y ranges, room sizes, materials, lighting)
- Gameplay settings (mob spawning, difficulty, PvP)
- BlueMap integration options
- Generation parameters (seed, stairwell/loot/hazard chances)

## Project Structure

```
backrooms-plugin/
├── BackroomsPlugin/     # Paper/Spigot plugin (BackroomsGen)
└── sources/
    ├── BackroomsMod/    # Fabric mod companion
    └── Paper/           # Paper server source
```

## License

MIT

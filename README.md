# LiminalGen

A Paper/Spigot plugin that adds Liminal dimension generation to Minecraft servers. Features multi-level world generation with custom chunk generators and integrations with BlueMap, MultiWorld, WorldEdit, and WorldGuard.

## Features

- **Multi-Level Generation**: Three distinct Liminal levels with unique themes
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
| `/liminal` | Teleport to the Liminal world | `liminal.use` |
| `/liminaladmin create` | Create the Liminal world | `liminal.admin` |
| `/liminaladmin reload` | Reload configuration | `liminal.admin` |
| `/liminaladmin setlevel <id>` | View level info | `liminal.admin` |
| `/liminaladmin info` | Show plugin info | `liminal.admin` |

## Building

```bash
cd LiminalPlugin
./gradlew build
```

The compiled jar will be in `LiminalPlugin/build/libs/`.

## Installation

1. Place the compiled jar in your server's `plugins/` folder
2. Start/restart the server
3. Run `/liminaladmin create` to generate the Liminal world
4. Players can use `/liminal` to enter

## Configuration

Edit `plugins/LiminalGen/config.yml` to customize:
- World name
- Level definitions (Y ranges, room sizes, materials, lighting)
- Gameplay settings (mob spawning, difficulty, PvP)
- BlueMap integration options
- Generation parameters (seed, stairwell/loot/hazard chances)

## Project Structure

```
liminal-plugin/
├── LiminalPlugin/     # Paper/Spigot plugin (LiminalGen)
└── sources/
    ├── LiminalMod/    # Fabric mod companion
    └── Paper/           # Paper server source
```

## License

MIT

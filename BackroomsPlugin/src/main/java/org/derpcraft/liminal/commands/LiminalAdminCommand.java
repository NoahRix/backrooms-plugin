package org.derpcraft.liminal.commands;

import org.derpcraft.liminal.LiminalPlugin;
import org.derpcraft.liminal.config.LevelConfig;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Difficulty;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

/**
 * Handles the {@code /liminaladmin} command for server administrators.
 *
 * <p>Provides administrative commands for managing the Liminal world:</p>
 * <ul>
 *   <li>{@code /liminaladmin create} &ndash; creates the Liminal world</li>
 *   <li>{@code /liminaladmin reload} &ndash; reloads the plugin configuration</li>
 *   <li>{@code /liminaladmin setlevel <id>} &ndash; shows detailed info about a level</li>
 *   <li>{@code /liminaladmin info} &ndash; shows general plugin information</li>
 * </ul>
 *
 * <p>All subcommands require the {@code liminal.admin} permission.</p>
 *
 * @see LiminalCommand
 */
public class LiminalAdminCommand implements CommandExecutor {

    /** Reference to the owning plugin instance. */
    private final LiminalPlugin plugin;

    /**
     * Constructs a new admin command executor.
     *
     * @param plugin the owning plugin instance
     */
    public LiminalAdminCommand(LiminalPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Executes the {@code /liminaladmin} command and dispatches to the appropriate subcommand.
     *
     * @param sender  the command sender
     * @param command the command
     * @param label   the command label
     * @param args    the command arguments
     * @return always {@code true}
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("liminal.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create" -> handleCreate(sender);
            case "reload" -> handleReload(sender);
            case "setlevel" -> handleSetLevel(sender, args);
            case "info" -> handleInfo(sender);
            default -> sendHelp(sender);
        }
        return true;
    }

    /**
     * Creates the Liminal world using the configured generator.
     *
     * @param sender the command sender
     */
    private void handleCreate(@NotNull CommandSender sender) {
        String worldName = plugin.getLiminalConfig().getLiminalWorldName();

        if (Bukkit.getWorld(worldName) != null) {
            sender.sendMessage(ChatColor.RED + "World '" + worldName + "' already exists!");
            return;
        }

        sender.sendMessage(ChatColor.GOLD + "Creating Liminal world '" + worldName + "'...");

        WorldCreator creator = new WorldCreator(worldName);
        creator.generator(plugin.getChunkGenerator());
        creator.environment(World.Environment.NORMAL);
        creator.generateStructures(false);

        long seed = plugin.getLiminalConfig().getGenerationSeed();
        if (seed != 0) {
            creator.seed(seed);
        }

        World world = creator.createWorld();
        if (world != null) {
            applyWorldSettings(world);
            sender.sendMessage(ChatColor.GREEN + "Liminal world '" + worldName + "' created successfully!");
            sender.sendMessage(ChatColor.GRAY + "Use '/mv import " + worldName + " normal -g LiminalGen' if using MultiWorld");
        } else {
            sender.sendMessage(ChatColor.RED + "Failed to create the Liminal world!");
        }
    }

    /**
     * Applies configured game settings to the Liminal world.
     *
     * @param world the Liminal world
     */
    private void applyWorldSettings(@NotNull World world) {
        var cfg = plugin.getLiminalConfig();
        try {
            world.setDifficulty(Difficulty.valueOf(cfg.getDifficulty()));
        } catch (IllegalArgumentException e) {
            world.setDifficulty(Difficulty.NORMAL);
        }
        world.setSpawnFlags(cfg.isSpawnMonsters(), cfg.isSpawnAnimals());
        world.setGameRuleValue("doMobSpawning", String.valueOf(cfg.isMobSpawning()));
        world.setGameRuleValue("doWeatherCycle", String.valueOf(cfg.isWeatherEnabled()));
    }

    /**
     * Reloads the plugin configuration.
     *
     * @param sender the command sender
     */
    private void handleReload(@NotNull CommandSender sender) {
        plugin.reload();
        sender.sendMessage(ChatColor.GREEN + "LiminalGen configuration reloaded!");
    }

    /**
     * Shows detailed information about a specific level.
     *
     * @param sender the command sender
     * @param args   the command arguments (args[1] is the level ID)
     */
    private void handleSetLevel(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /liminaladmin setlevel <levelId>");
            sender.sendMessage(ChatColor.GRAY + "Available levels:");
            for (LevelConfig level : plugin.getLiminalConfig().getEnabledLevels()) {
                sender.sendMessage(ChatColor.GRAY + "  - " + level.getId() + ": " + level.getName());
            }
            return;
        }

        String levelId = args[1];
        LevelConfig level = plugin.getLiminalConfig().getLevels().get(levelId);
        if (level == null) {
            sender.sendMessage(ChatColor.RED + "Level '" + levelId + "' not found!");
            return;
        }

        sender.sendMessage(ChatColor.GREEN + "Level '" + level.getName() + "' info:");
        sender.sendMessage(ChatColor.GRAY + "  Y Range: " + level.getMinY() + " to " + level.getMaxY());
        sender.sendMessage(ChatColor.GRAY + "  Room Size: " + level.getRoomMinWidth() + "-" + level.getRoomMaxWidth() + " x " + level.getRoomMinLength() + "-" + level.getRoomMaxLength());
        sender.sendMessage(ChatColor.GRAY + "  Wall: " + level.getWallMaterial());
        sender.sendMessage(ChatColor.GRAY + "  Floor: " + level.getFloorMaterial());
        sender.sendMessage(ChatColor.GRAY + "  Light: " + level.getLightMaterial());
    }

    /**
     * Shows general plugin information.
     *
     * @param sender the command sender
     */
    private void handleInfo(@NotNull CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== LiminalGen Info ===");
        sender.sendMessage(ChatColor.GRAY + "Version: " + ChatColor.WHITE + plugin.getDescription().getVersion());
        sender.sendMessage(ChatColor.GRAY + "World Name: " + ChatColor.WHITE + plugin.getLiminalConfig().getLiminalWorldName());
        sender.sendMessage(ChatColor.GRAY + "Levels: " + ChatColor.WHITE + plugin.getLiminalConfig().getEnabledLevels().size());
        for (LevelConfig level : plugin.getLiminalConfig().getEnabledLevels()) {
            sender.sendMessage(ChatColor.GRAY + "  - " + level.getName() + " (Y: " + level.getMinY() + " to " + level.getMaxY() + ")");
        }
        sender.sendMessage(ChatColor.GRAY + "BlueMap: " + ChatColor.WHITE + (plugin.getBlueMapHook() != null ? "Connected" : "Not connected"));
        sender.sendMessage(ChatColor.GRAY + "MultiWorld: " + ChatColor.WHITE + (plugin.getMultiWorldHook() != null ? "Connected" : "Not connected"));

        World world = Bukkit.getWorld(plugin.getLiminalConfig().getLiminalWorldName());
        sender.sendMessage(ChatColor.GRAY + "World Loaded: " + ChatColor.WHITE + (world != null ? "Yes" : "No"));
    }

    /**
     * Sends the help message listing all available subcommands.
     *
     * @param sender the command sender
     */
    private void sendHelp(@NotNull CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== LiminalGen Admin Commands ===");
        sender.sendMessage(ChatColor.YELLOW + "/liminaladmin create" + ChatColor.GRAY + " - Create the Liminal world");
        sender.sendMessage(ChatColor.YELLOW + "/liminaladmin reload" + ChatColor.GRAY + " - Reload configuration");
        sender.sendMessage(ChatColor.YELLOW + "/liminaladmin setlevel <id>" + ChatColor.GRAY + " - View level info");
        sender.sendMessage(ChatColor.YELLOW + "/liminaladmin info" + ChatColor.GRAY + " - Show plugin info");
    }
}

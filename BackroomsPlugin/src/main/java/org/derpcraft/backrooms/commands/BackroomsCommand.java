package org.derpcraft.backrooms.commands;

import org.derpcraft.backrooms.BackroomsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Handles the {@code /backrooms} command for players.
 *
 * <p>Teleports the executing player to the Backrooms world spawn. Requires the
 * {@code backrooms.use} permission.</p>
 *
 * <h2>Usage</h2>
 * <pre>/backrooms</pre>
 *
 * @see BackroomsAdminCommand
 */
public class BackroomsCommand implements CommandExecutor {

    /** Reference to the owning plugin instance. */
    private final BackroomsPlugin plugin;

    /**
     * Constructs a new command executor.
     *
     * @param plugin the owning plugin instance
     */
    public BackroomsCommand(BackroomsPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Executes the {@code /backrooms} command.
     *
     * <p>Teleports the player to the Backrooms world spawn and sends a thematic
     * message. Fails gracefully if the player lacks permission or the world
     * does not exist.</p>
     *
     * @param sender  the command sender (must be a player)
     * @param command the command
     * @param label   the command label
     * @param args    the command arguments (unused)
     * @return always {@code true}
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players");
            return true;
        }

        if (!player.hasPermission("backrooms.use")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use this command");
            return true;
        }

        String worldName = plugin.getBackroomsConfig().getBackroomsWorldName();
        World backroomsWorld = Bukkit.getWorld(worldName);

        if (backroomsWorld == null) {
            player.sendMessage(ChatColor.RED + "The Backrooms world does not exist. Ask an admin to create it.");
            return true;
        }

        Location spawn = backroomsWorld.getSpawnLocation();
        player.teleport(spawn);
        player.sendMessage(ChatColor.GOLD + "You have entered " + ChatColor.YELLOW + "The Backrooms" + ChatColor.GOLD + "...");
        player.sendMessage(ChatColor.GRAY + "The fluorescent lights hum endlessly above you.");
        return true;
    }
}

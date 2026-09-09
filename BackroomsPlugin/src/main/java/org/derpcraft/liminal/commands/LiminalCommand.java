package org.derpcraft.liminal.commands;

import org.derpcraft.liminal.LiminalPlugin;
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
 * Handles the {@code /liminal} command for players.
 *
 * <p>Teleports the executing player to the Liminal world spawn. Requires the
 * {@code liminal.use} permission.</p>
 *
 * <h2>Usage</h2>
 * <pre>/liminal</pre>
 *
 * @see LiminalAdminCommand
 */
public class LiminalCommand implements CommandExecutor {

    /** Reference to the owning plugin instance. */
    private final LiminalPlugin plugin;

    /**
     * Constructs a new command executor.
     *
     * @param plugin the owning plugin instance
     */
    public LiminalCommand(LiminalPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Executes the {@code /liminal} command.
     *
     * <p>Teleports the player to the Liminal world spawn and sends a thematic
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

        if (!player.hasPermission("liminal.use")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use this command");
            return true;
        }

        String worldName = plugin.getLiminalConfig().getLiminalWorldName();
        World liminalWorld = Bukkit.getWorld(worldName);

        if (liminalWorld == null) {
            player.sendMessage(ChatColor.RED + "The Liminal world does not exist. Ask an admin to create it.");
            return true;
        }

        Location spawn = liminalWorld.getSpawnLocation();
        player.teleport(spawn);
        player.sendMessage(ChatColor.GOLD + "You have entered " + ChatColor.YELLOW + "The Liminal" + ChatColor.GOLD + "...");
        player.sendMessage(ChatColor.GRAY + "The fluorescent lights hum endlessly above you.");
        return true;
    }
}

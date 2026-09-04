package com.derpcraft.backrooms.commands;

import com.derpcraft.backrooms.BackroomsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class BackroomsCommand implements CommandExecutor {

    private final BackroomsPlugin plugin;

    public BackroomsCommand(BackroomsPlugin plugin) {
        this.plugin = plugin;
    }

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

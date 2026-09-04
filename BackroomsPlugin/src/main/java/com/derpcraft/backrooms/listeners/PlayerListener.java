package com.derpcraft.backrooms.listeners;

import com.derpcraft.backrooms.BackroomsPlugin;
import com.derpcraft.backrooms.config.LevelConfig;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class PlayerListener implements Listener {

    private final BackroomsPlugin plugin;

    public PlayerListener(BackroomsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        World backroomsWorld = plugin.getServer().getWorld(plugin.getBackroomsConfig().getBackroomsWorldName());
        if (backroomsWorld != null && player.getWorld() == backroomsWorld) {
            sendLevelMessage(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        World backroomsWorld = plugin.getServer().getWorld(plugin.getBackroomsConfig().getBackroomsWorldName());
        if (backroomsWorld != null && player.getWorld() == backroomsWorld) {
            event.setRespawnLocation(backroomsWorld.getSpawnLocation());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getFrom().getBlockX() >> 4 == event.getTo().getBlockX() >> 4
                && event.getFrom().getBlockZ() >> 4 == event.getTo().getBlockZ() >> 4) {
            return;
        }

        Player player = event.getPlayer();
        String worldName = plugin.getBackroomsConfig().getBackroomsWorldName();
        if (!player.getWorld().getName().equals(worldName)) return;

        int y = player.getLocation().getBlockY();
        LevelConfig level = plugin.getBackroomsConfig().getLevelForY(y);
        if (level != null) {
            player.sendActionBar(ChatColor.GOLD + level.getName());
        }
    }

    private void sendLevelMessage(Player player) {
        int y = player.getLocation().getBlockY();
        LevelConfig level = plugin.getBackroomsConfig().getLevelForY(y);
        if (level != null) {
            player.sendMessage(ChatColor.GOLD + "You are in " + ChatColor.YELLOW + level.getName());
        }
    }
}

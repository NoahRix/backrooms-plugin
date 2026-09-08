package org.derpcraft.backrooms.listeners;

import org.derpcraft.backrooms.BackroomsPlugin;
import org.derpcraft.backrooms.config.LevelConfig;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.derpcraft.backrooms.effects.FlickerManager;

/**
 * Handles player-related events in the Backrooms world.
 *
 * <p>Provides the following functionality:</p>
 * <ul>
 *   <li><b>Level announcements</b> &ndash; when a player enters the Backrooms or crosses
 *       into a different level's Y range, they receive a chat message or action bar
 *       notification with the level name.</li>
 *   <li><b>Respawn handling</b> &ndash; players who die in the Backrooms respawn at the
 *       world's spawn location rather than the server's global spawn.</li>
 * </ul>
 *
 * @see BackroomsPlugin
 */
public class PlayerListener implements Listener {

    /** Reference to the owning plugin instance. */
    private final BackroomsPlugin plugin;

    /** Name of the Backrooms world. */
    private final String backroomsWorldName;

    /**
     * Constructs a new player event listener.
     *
     * @param plugin the owning plugin instance
     */
    public PlayerListener(BackroomsPlugin plugin) {
        this.plugin = plugin;
        this.backroomsWorldName = plugin.getBackroomsConfig().getBackroomsWorldName();
    }

    /**
     * Sends a level announcement when a player joins while in the Backrooms world.
     *
     * @param event the player join event
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        World backroomsWorld = plugin.getServer().getWorld(backroomsWorldName);
        if (backroomsWorld != null && player.getWorld() == backroomsWorld) {
            sendLevelMessage(player);
            startFlickerEffect(player);
        }
    }

    /**
     * Handles player quit to stop flicker effects.
     *
     * @param event the player quit event
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerQuit(PlayerQuitEvent event) {
        stopFlickerEffect(event.getPlayer());
    }

    /**
     * Handles player teleport to start/stop flicker effects based on destination world.
     *
     * @param event the player teleport event
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        World toWorld = event.getTo() != null ? event.getTo().getWorld() : null;
        World backroomsWorld = plugin.getServer().getWorld(backroomsWorldName);

        if (toWorld == backroomsWorld) {
            startFlickerEffect(player);
        } else {
            stopFlickerEffect(player);
        }
    }

    /**
     * Starts the flicker effect for a player if the flicker manager is available.
     *
     * @param player the player
     */
    private void startFlickerEffect(Player player) {
        FlickerManager flickerManager = BackroomsPlugin.getFlickerManager();
        if (flickerManager != null) {
            flickerManager.startFlickering(player);
        }
    }

    /**
     * Stops the flicker effect for a player if the flicker manager is available.
     *
     * @param player the player
     */
    private void stopFlickerEffect(Player player) {
        FlickerManager flickerManager = BackroomsPlugin.getFlickerManager();
        if (flickerManager != null) {
            flickerManager.stopFlickering(player);
        }
    }

    /**
     * Redirects respawn to the Backrooms spawn when a player dies in the Backrooms world.
     *
     * @param event the player respawn event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        World backroomsWorld = plugin.getServer().getWorld(backroomsWorldName);
        if (backroomsWorld != null && player.getWorld() == backroomsWorld) {
            event.setRespawnLocation(backroomsWorld.getSpawnLocation());
        }
    }

    /**
     * Shows the current level name on the action bar when a player crosses a chunk border
     * in the Backrooms world.
     *
     * @param event the player move event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getFrom().getBlockX() >> 4 == event.getTo().getBlockX() >> 4
                && event.getFrom().getBlockZ() >> 4 == event.getTo().getBlockZ() >> 4) {
            return;
        }

        Player player = event.getPlayer();
        if (!player.getWorld().getName().equals(backroomsWorldName)) return;

        int y = player.getLocation().getBlockY();
        LevelConfig level = plugin.getBackroomsConfig().getLevelForY(y);
        if (level != null) {
            player.sendActionBar(ChatColor.GOLD + level.getName());
        }
    }

    /**
     * Sends a chat message to the player announcing which level they are in.
     *
     * @param player the player to message
     */
    private void sendLevelMessage(Player player) {
        int y = player.getLocation().getBlockY();
        LevelConfig level = plugin.getBackroomsConfig().getLevelForY(y);
        if (level != null) {
            player.sendMessage(ChatColor.GOLD + "You are in " + ChatColor.YELLOW + level.getName());
        }
    }
}

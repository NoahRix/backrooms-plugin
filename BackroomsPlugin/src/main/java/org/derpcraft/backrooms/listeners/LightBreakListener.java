package org.derpcraft.backrooms.listeners;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.derpcraft.backrooms.BackroomsPlugin;
import org.derpcraft.backrooms.effects.FlickerManager;

/**
 * Handles block break events for flickering lights in the Backrooms world.
 *
 * <p>When a player breaks a flickering light (sea lantern), this listener removes
 * the location from the flickering set so the flicker effect stops being applied.</p>
 */
public class LightBreakListener implements Listener {

    private final BackroomsPlugin plugin;

    public LightBreakListener(BackroomsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        // Check if we're in the backrooms world
        String backroomsWorldName = plugin.getBackroomsConfig().getBackroomsWorldName();
        if (!event.getBlock().getWorld().getName().equals(backroomsWorldName)) {
            return;
        }

        Location blockLoc = event.getBlock().getLocation();
        FlickerManager flickerManager = BackroomsPlugin.getFlickerManager();
        
        if (flickerManager == null) {
            return;
        }

        // If this location was marked as flickering, remove it
        if (flickerManager.isFlickering(blockLoc)) {
            flickerManager.removeFlickering(blockLoc);
        }
    }
}

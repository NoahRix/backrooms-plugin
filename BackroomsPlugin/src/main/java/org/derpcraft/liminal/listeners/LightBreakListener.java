package org.derpcraft.liminal.listeners;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.derpcraft.liminal.LiminalPlugin;
import org.derpcraft.liminal.effects.FlickerManager;

/**
 * Handles block break events for flickering lights in the Liminal world.
 *
 * <p>When a player breaks a flickering light (sea lantern), this listener removes
 * the location from the flickering set so the flicker effect stops being applied.</p>
 */
public class LightBreakListener implements Listener {

    private final LiminalPlugin plugin;

    public LightBreakListener(LiminalPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        // Check if we're in the liminal world
        String liminalWorldName = plugin.getLiminalConfig().getLiminalWorldName();
        if (!event.getBlock().getWorld().getName().equals(liminalWorldName)) {
            return;
        }

        Location blockLoc = event.getBlock().getLocation();
        FlickerManager flickerManager = LiminalPlugin.getFlickerManager();
        
        if (flickerManager == null) {
            return;
        }

        // If this location was marked as flickering, remove it
        if (flickerManager.isFlickering(blockLoc)) {
            flickerManager.removeFlickering(blockLoc);
        }
    }
}

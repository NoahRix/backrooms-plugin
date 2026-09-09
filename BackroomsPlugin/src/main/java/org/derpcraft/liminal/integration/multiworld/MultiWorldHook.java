package org.derpcraft.liminal.integration.multiworld;

import org.derpcraft.liminal.LiminalPlugin;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldInitEvent;

public class MultiWorldHook implements Listener {

    private final LiminalPlugin plugin;

    public MultiWorldHook(LiminalPlugin plugin) {
        this.plugin = plugin;
    }

    public void register() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onWorldInit(WorldInitEvent event) {
        String worldName = event.getWorld().getName();
        String liminalWorldName = plugin.getLiminalConfig().getLiminalWorldName();

        if (worldName.equals(liminalWorldName)) {
            plugin.getLogger().info("Liminal world '" + worldName + "' initialized with LiminalGen generator");
        }
    }
}

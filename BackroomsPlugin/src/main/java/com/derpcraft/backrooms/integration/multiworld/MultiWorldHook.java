package com.derpcraft.backrooms.integration.multiworld;

import com.derpcraft.backrooms.BackroomsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldInitEvent;

public class MultiWorldHook implements Listener {

    private final BackroomsPlugin plugin;

    public MultiWorldHook(BackroomsPlugin plugin) {
        this.plugin = plugin;
    }

    public void register() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onWorldInit(WorldInitEvent event) {
        String worldName = event.getWorld().getName();
        String backroomsWorldName = plugin.getBackroomsConfig().getBackroomsWorldName();

        if (worldName.equals(backroomsWorldName)) {
            plugin.getLogger().info("Backrooms world '" + worldName + "' initialized with BackroomsGen generator");
        }
    }
}

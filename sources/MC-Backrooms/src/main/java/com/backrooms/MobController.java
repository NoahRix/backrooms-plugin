package com.backrooms;

import org.bukkit.entity.Ambient;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Entity;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.NPC;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntitySpawnEvent;

public class MobController implements Listener {

    @EventHandler
    public void onMobSpawn(EntitySpawnEvent event) {
        Entity entity = event.getEntity();
        
        if (entity.getWorld().getName().equalsIgnoreCase("backrooms")) {
            
            // 1. Block passive animals (Cows, Sheep, Pigs, Chickens)
            if (entity instanceof Animals) {
                event.setCancelled(true);
                return;
            }
            
            // 2. Block ambient creatures (Bats)
            if (entity instanceof Ambient) {
                event.setCancelled(true);
                return;
            }
            
            // 3. Block villagers and custom NPCs
            if (entity instanceof NPC) {
                event.setCancelled(true);
                return;
            }
            
            // 4. Block accidental player-built or village defenders (Iron Golems)
            if (entity instanceof IronGolem) {
                event.setCancelled(true);
                return;
            }
            
            // 5. Block creepy block-movers (Endermen) so they don't dismantle your terracotta walls
            if (entity instanceof Enderman) {
                event.setCancelled(true);
                return;
            }
        }
    }
}
package org.derpcraft.liminal.integration.worldguard;

import org.derpcraft.liminal.LiminalPlugin;

public class WorldGuardHook {

    private final LiminalPlugin plugin;

    public WorldGuardHook(LiminalPlugin plugin) {
        this.plugin = plugin;
    }

    public void register() {
        plugin.getLogger().info("WorldGuard integration loaded");
    }
}

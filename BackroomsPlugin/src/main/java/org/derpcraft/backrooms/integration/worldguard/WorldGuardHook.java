package org.derpcraft.backrooms.integration.worldguard;

import org.derpcraft.backrooms.BackroomsPlugin;

public class WorldGuardHook {

    private final BackroomsPlugin plugin;

    public WorldGuardHook(BackroomsPlugin plugin) {
        this.plugin = plugin;
    }

    public void register() {
        plugin.getLogger().info("WorldGuard integration loaded");
    }
}

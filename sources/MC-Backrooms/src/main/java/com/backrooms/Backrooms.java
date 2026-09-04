package com.backrooms;

import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Backrooms extends JavaPlugin {

    @Override
    public void onEnable() {
        // Register our watertight mob isolation rules
        getServer().getPluginManager().registerEvents(new MobController(), this);
        getLogger().info("Backrooms plugin fully initialized and ready.");
    }

    @Override
    public void onDisable() {
        getLogger().info("Backrooms plugin shutting down.");
    }

    @Override
    public @Nullable ChunkGenerator getDefaultWorldGenerator(@NotNull String worldName, @Nullable String id) {
        // Tie the custom map algorithm directly to Paper's world manager
        return new BackroomsGenerator();
    }
}
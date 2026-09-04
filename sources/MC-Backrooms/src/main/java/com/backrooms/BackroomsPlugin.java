package com.backrooms;

import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BackroomsPlugin extends JavaPlugin {

    private static BackroomsPlugin instance;

    @Override
    public void onEnable() {
        instance = this;
    }

    public static BackroomsPlugin getInstance() { 
        return instance; 
    }

    @Override
    public @Nullable ChunkGenerator getDefaultWorldGenerator(@NotNull String worldName, @Nullable String id) {
        return new BackroomsGenerator();
    }
}
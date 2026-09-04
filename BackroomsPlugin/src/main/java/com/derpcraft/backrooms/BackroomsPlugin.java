package com.derpcraft.backrooms;

import com.derpcraft.backrooms.commands.BackroomsAdminCommand;
import com.derpcraft.backrooms.commands.BackroomsCommand;
import com.derpcraft.backrooms.config.BackroomsConfig;
import com.derpcraft.backrooms.generator.BackroomsChunkGenerator;
import com.derpcraft.backrooms.integration.bluemap.BlueMapHook;
import com.derpcraft.backrooms.integration.multiworld.MultiWorldHook;
import com.derpcraft.backrooms.listeners.PlayerListener;
import com.derpcraft.backrooms.listeners.WorldListener;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.logging.Level;

public class BackroomsPlugin extends JavaPlugin {

    private static BackroomsPlugin instance;
    private BackroomsConfig config;
    private BackroomsChunkGenerator chunkGenerator;
    private BlueMapHook blueMapHook;
    private MultiWorldHook multiWorldHook;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        config = new BackroomsConfig(this);
        config.load();

        chunkGenerator = new BackroomsChunkGenerator(config);

        PluginManager pm = getServer().getPluginManager();

        if (pm.getPlugin("BlueMap") != null) {
            try {
                blueMapHook = new BlueMapHook(this);
                blueMapHook.register();
                getLogger().info("BlueMap integration enabled");
            } catch (Exception e) {
                getLogger().log(Level.WARNING, "Failed to hook into BlueMap", e);
            }
        }

        if (pm.getPlugin("MultiWorld") != null) {
            try {
                multiWorldHook = new MultiWorldHook(this);
                multiWorldHook.register();
                getLogger().info("MultiWorld integration enabled");
            } catch (Exception e) {
                getLogger().log(Level.WARNING, "Failed to hook into MultiWorld", e);
            }
        }

        getCommand("backrooms").setExecutor(new BackroomsCommand(this));
        getCommand("backroomsadmin").setExecutor(new BackroomsAdminCommand(this));

        pm.registerEvents(new PlayerListener(this), this);
        pm.registerEvents(new WorldListener(this), this);

        getLogger().info("BackroomsGen v" + getDescription().getVersion() + " enabled");
    }

    @Override
    public void onDisable() {
        if (blueMapHook != null) {
            blueMapHook.unregister();
        }
        instance = null;
        getLogger().info("BackroomsGen disabled");
    }

    @Override
    public @Nullable ChunkGenerator getDefaultWorldGenerator(@NotNull String worldName, @Nullable String id) {
        return chunkGenerator;
    }

    public static BackroomsPlugin getInstance() {
        return instance;
    }

    public BackroomsConfig getBackroomsConfig() {
        return config;
    }

    public BackroomsChunkGenerator getChunkGenerator() {
        return chunkGenerator;
    }

    public @Nullable BlueMapHook getBlueMapHook() {
        return blueMapHook;
    }

    public @Nullable MultiWorldHook getMultiWorldHook() {
        return multiWorldHook;
    }

    public void reload() {
        reloadConfig();
        config.load();
        chunkGenerator = new BackroomsChunkGenerator(config);
        if (blueMapHook != null) {
            blueMapHook.unregister();
            blueMapHook.register();
        }
    }
}

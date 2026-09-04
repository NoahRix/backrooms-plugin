package com.derpcraft.backrooms.listeners;

import com.derpcraft.backrooms.BackroomsPlugin;
import com.derpcraft.backrooms.config.LevelConfig;
import org.bukkit.Difficulty;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.event.world.WorldInitEvent;
import org.bukkit.event.world.WorldLoadEvent;

public class WorldListener implements Listener {

    private final BackroomsPlugin plugin;

    public WorldListener(BackroomsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onWorldInit(WorldInitEvent event) {
        World world = event.getWorld();
        String backroomsWorldName = plugin.getBackroomsConfig().getBackroomsWorldName();

        if (!world.getName().equals(backroomsWorldName)) return;

        var cfg = plugin.getBackroomsConfig();
        try {
            world.setDifficulty(Difficulty.valueOf(cfg.getDifficulty()));
        } catch (IllegalArgumentException ignored) {
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onWorldLoad(WorldLoadEvent event) {
        World world = event.getWorld();
        String backroomsWorldName = plugin.getBackroomsConfig().getBackroomsWorldName();

        if (!world.getName().equals(backroomsWorldName)) return;

        var cfg = plugin.getBackroomsConfig();
        world.setSpawnFlags(cfg.isSpawnMonsters(), cfg.isSpawnAnimals());
        world.setGameRuleValue("doMobSpawning", String.valueOf(cfg.isMobSpawning()));
        world.setGameRuleValue("doWeatherCycle", String.valueOf(cfg.isWeatherEnabled()));
        world.setGameRuleValue("pvp", String.valueOf(cfg.isPvp()));
        world.setGameRuleValue("doHunger", String.valueOf(cfg.isHungerEnabled()));

        plugin.getLogger().info("Backrooms world '" + backroomsWorldName + "' loaded and configured");

        if (plugin.getBlueMapHook() != null) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                plugin.getBlueMapHook().unregister();
                plugin.getBlueMapHook().register();
            }, 100L);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onWeatherChange(WeatherChangeEvent event) {
        World world = event.getWorld();
        String backroomsWorldName = plugin.getBackroomsConfig().getBackroomsWorldName();

        if (!world.getName().equals(backroomsWorldName)) return;

        if (!plugin.getBackroomsConfig().isWeatherEnabled() && event.toWeatherState()) {
            event.setCancelled(true);
        }
    }
}

package org.derpcraft.backrooms.listeners;

import org.derpcraft.backrooms.BackroomsPlugin;
import org.bukkit.Difficulty;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.event.world.WorldInitEvent;
import org.bukkit.event.world.WorldLoadEvent;

/**
 * Handles world-related events for the Backrooms world.
 *
 * <p>Provides the following functionality:</p>
 * <ul>
 *   <li><b>World initialisation</b> &ndash; applies the configured difficulty when the
 *       Backrooms world is first created.</li>
 *   <li><b>World load</b> &ndash; applies game rules (mob spawning, weather, PvP, hunger)
 *       and re-registers BlueMap integration when the world loads.</li>
 *   <li><b>Weather control</b> &ndash; cancels weather changes if weather is disabled
 *       in the configuration.</li>
 * </ul>
 *
 * @see BackroomsPlugin
 */
public class WorldListener implements Listener {

    /** Reference to the owning plugin instance. */
    private final BackroomsPlugin plugin;

    /** Name of the Backrooms world. */
    private final String backroomsWorldName;

    /**
     * Constructs a new world event listener.
     *
     * @param plugin the owning plugin instance
     */
    public WorldListener(BackroomsPlugin plugin) {
        this.plugin = plugin;
        this.backroomsWorldName = plugin.getBackroomsConfig().getBackroomsWorldName();
    }

    /**
     * Applies the configured difficulty when the Backrooms world is initialised.
     *
     * @param event the world init event
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onWorldInit(WorldInitEvent event) {
        World world = event.getWorld();
        if (!world.getName().equals(backroomsWorldName)) return;

        var cfg = plugin.getBackroomsConfig();
        try {
            world.setDifficulty(Difficulty.valueOf(cfg.getDifficulty()));
        } catch (IllegalArgumentException ignored) {
        }
    }

    /**
     * Applies game rules and re-registers BlueMap when the Backrooms world loads.
     *
     * @param event the world load event
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onWorldLoad(WorldLoadEvent event) {
        World world = event.getWorld();
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

    /**
     * Cancels weather changes in the Backrooms world if weather is disabled.
     *
     * @param event the weather change event
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onWeatherChange(WeatherChangeEvent event) {
        World world = event.getWorld();
        if (!world.getName().equals(backroomsWorldName)) return;

        if (!plugin.getBackroomsConfig().isWeatherEnabled() && event.toWeatherState()) {
            event.setCancelled(true);
        }
    }
}

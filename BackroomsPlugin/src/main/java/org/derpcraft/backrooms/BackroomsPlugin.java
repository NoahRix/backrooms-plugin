package org.derpcraft.backrooms;

import org.derpcraft.backrooms.commands.BackroomsAdminCommand;
import org.derpcraft.backrooms.commands.BackroomsCommand;
import org.derpcraft.backrooms.config.BackroomsConfig;
import org.derpcraft.backrooms.generator.BackroomsChunkGenerator;
import org.derpcraft.backrooms.integration.bluemap.BlueMapHook;
import org.derpcraft.backrooms.integration.multiworld.MultiWorldHook;
import org.derpcraft.backrooms.listeners.PlayerListener;
import org.derpcraft.backrooms.listeners.WorldListener;
import org.derpcraft.backrooms.listeners.LightBreakListener;
import org.derpcraft.backrooms.listeners.RailsListener;
import org.derpcraft.backrooms.effects.FlickerManager;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.logging.Level;

/**
 * Main plugin class for BackroomsGen.
 *
 * <p>BackroomsGen is a multi-level Backrooms world generator for Paper/Spigot Minecraft
 * servers. It generates vertically-stacked liminal space levels, each with their own
 * aesthetic, room layout, and special features.</p>
 *
 * <h2>Architecture</h2>
 * <ul>
 *   <li>{@link BackroomsConfig} &ndash; loads and holds all configuration from {@code config.yml}</li>
 *   <li>{@link BackroomsChunkGenerator} &ndash; orchestrates chunk generation by delegating to levels</li>
 *   <li>{@link org.derpcraft.backrooms.generator.levels.BackroomsLevel} &ndash; abstract base for level generators</li>
 *   <li>{@link org.derpcraft.backrooms.generator.levels.Level0Lobby} &ndash; Level 0: The Lobby</li>
 *   <li>{@link org.derpcraft.backrooms.generator.levels.Level1HabitableZone} &ndash; Level 1: Habitable Zone</li>
 *   <li>{@link org.derpcraft.backrooms.generator.levels.Level2PipeDreams} &ndash; Level 2: Pipe Dreams</li>
 * </ul>
 *
 * <h2>Integrations</h2>
 * <p>Optional hooks into third-party plugins:</p>
 * <ul>
 *   <li><b>BlueMap</b> &ndash; renders the Backrooms as a dedicated 3D map with markers</li>
 *   <li><b>MultiWorld</b> &ndash; manages the Backrooms as a separate world</li>
 *   <li><b>WorldGuard</b> &ndash; region-based protection (future)</li>
 * </ul>
 *
 * @see BackroomsConfig
 * @see BackroomsChunkGenerator
 */
public class BackroomsPlugin extends JavaPlugin {

    /** Singleton instance of the plugin. */
    private static BackroomsPlugin instance;

    /** Plugin configuration. */
    private BackroomsConfig config;

    /** Chunk generator for the Backrooms world. */
    private BackroomsChunkGenerator chunkGenerator;

    /** Optional BlueMap integration hook. */
    private BlueMapHook blueMapHook;

    /** Optional MultiWorld integration hook. */
    private MultiWorldHook multiWorldHook;

    /** Manages flickering fluorescent light effects. */
    private static FlickerManager flickerManager;

    /**
     * Called when the plugin is enabled.
     *
     * <p>Initialises configuration, creates the chunk generator, sets up optional
     * integrations, registers commands and event listeners.</p>
     */
    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        config = new BackroomsConfig(this);
        config.load();

        chunkGenerator = new BackroomsChunkGenerator(config);
        flickerManager = new FlickerManager(this);

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
        pm.registerEvents(new LightBreakListener(this), this);
        pm.registerEvents(new RailsListener(this), this);

        getLogger().info("BackroomsGen v" + getDescription().getVersion() + " enabled");
    }

    /**
     * Called when the plugin is disabled.
     *
     * <p>Unregisters integrations and cleans up resources.</p>
     */
    @Override
    public void onDisable() {
        if (blueMapHook != null) {
            blueMapHook.unregister();
        }
        if (flickerManager != null) {
            flickerManager.shutdown();
        }
        instance = null;
        getLogger().info("BackroomsGen disabled");
    }

    /**
     * Returns the default world generator for the Backrooms world.
     *
     * <p>Called by the server when a world is created with {@code -g BackroomsGen}.</p>
     *
     * @param worldName the name of the world being created
     * @param id        optional generator ID (unused)
     * @return the chunk generator instance
     */
    @Override
    public @Nullable ChunkGenerator getDefaultWorldGenerator(@NotNull String worldName, @Nullable String id) {
        return chunkGenerator;
    }

    /**
     * Returns the singleton plugin instance.
     *
     * @return the plugin instance, or {@code null} if the plugin is not enabled
     */
    public static BackroomsPlugin getInstance() {
        return instance;
    }

    /**
     * Returns the plugin configuration.
     *
     * @return the Backrooms configuration
     */
    public BackroomsConfig getBackroomsConfig() {
        return config;
    }

    /**
     * Returns the chunk generator for the Backrooms world.
     *
     * @return the chunk generator
     */
    public BackroomsChunkGenerator getChunkGenerator() {
        return chunkGenerator;
    }

    /**
     * Returns the BlueMap integration hook, if available.
     *
     * @return the BlueMap hook, or {@code null} if BlueMap is not installed
     */
    public @Nullable BlueMapHook getBlueMapHook() {
        return blueMapHook;
    }

    /**
     * Returns the MultiWorld integration hook, if available.
     *
     * @return the MultiWorld hook, or {@code null} if MultiWorld is not installed
     */
    public @Nullable MultiWorldHook getMultiWorldHook() {
        return multiWorldHook;
    }

    /**
     * Returns the flicker manager for fluorescent light effects.
     *
     * @return the flicker manager, or {@code null} if the plugin is not enabled
     */
    public static @Nullable FlickerManager getFlickerManager() {
        return flickerManager;
    }

    /**
     * Reloads the plugin configuration and recreates the chunk generator.
     *
     * <p>Also re-registers the BlueMap integration to pick up any map configuration
     * changes.</p>
     */
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

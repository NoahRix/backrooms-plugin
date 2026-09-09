package org.derpcraft.backrooms.config;

import org.derpcraft.backrooms.generator.levels.BackroomsLevel;
import org.derpcraft.backrooms.generator.levels.GenericBackroomsLevel;
import org.derpcraft.backrooms.generator.levels.Level0Lobby;
import org.derpcraft.backrooms.generator.levels.Level1HabitableZone;
import org.derpcraft.backrooms.generator.levels.Level2PipeDreams;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads and holds all configuration for the Backrooms plugin.
 *
 * <p>Configuration is read from the plugin's {@code config.yml} and includes:</p>
 * <ul>
 *   <li><b>World settings</b> &ndash; world name, generation seed, grid cell size</li>
 *   <li><b>Level definitions</b> &ndash; Y ranges, materials, room dimensions for each level</li>
 *   <li><b>Gameplay settings</b> &ndash; mob spawning, loot, hazards, PvP, difficulty</li>
 *   <li><b>BlueMap integration</b> &ndash; map name, markers, overlays</li>
 * </ul>
 *
 * <p>After loading the raw {@link LevelConfig} data objects, this class also instantiates
 * the corresponding {@link BackroomsLevel} subclasses ({@link Level0Lobby},
 * {@link Level1HabitableZone}, {@link Level2PipeDreams}) which contain the actual
 * generation logic.</p>
 *
 * @see LevelConfig
 * @see BackroomsLevel
 */
public class BackroomsConfig {

    /** Reference to the owning plugin instance for config file access. */
    private final JavaPlugin plugin;

    /** Name of the Backrooms world (e.g. "backrooms"). */
    private String backroomsWorldName;

    /** Fixed generation seed; 0 means use the world's random seed. */
    private long generationSeed;

    /** Size of the grid cell used for room placement (in chunks). */
    private int gridCellSize;

    /** Probability (0.0&ndash;1.0) that a stairwell connects two levels. */
    private double stairwellChance;

    /** Probability (0.0&ndash;1.0) that a room contains loot. */
    private double lootRoomChance;

    /** Probability (0.0&ndash;1.0) that a room contains hazards. */
    private double hazardRoomChance;

    /** Whether mobs can spawn naturally in the Backrooms world. */
    private boolean mobSpawning;

    /** Whether loot generation is enabled. */
    private boolean lootGeneration;

    /** Whether environmental hazards (water, fire, cobwebs) are placed. */
    private boolean hazards;

    /** Whether hunger is enabled in the Backrooms world. */
    private boolean hungerEnabled;

    /** Whether PvP is enabled in the Backrooms world. */
    private boolean pvp;

    /** World difficulty (e.g. "NORMAL", "HARD"). */
    private String difficulty;

    /** Whether monsters spawn in the Backrooms world. */
    private boolean spawnMonsters;

    /** Whether animals spawn in the Backrooms world. */
    private boolean spawnAnimals;

    /** Whether weather cycles in the Backrooms world. */
    private boolean weatherEnabled;

    /** Whether BlueMap integration is enabled. */
    private boolean blueMapEnabled;

    /** Whether BlueMap markers are shown. */
    private boolean blueMapMarkers;

    /** Whether BlueMap overlays are shown. */
    private boolean blueMapOverlays;

    /** Whether a dedicated BlueMap map is created for the Backrooms. */
    private boolean blueMapDedicatedMap;

    /** Name of the BlueMap map for the Backrooms. */
    private String blueMapName;

    /** Map of level ID to {@link LevelConfig} data objects. */
    private final Map<String, LevelConfig> levels = new HashMap<>();

    /** Cached list of enabled level configs, sorted by Y descending (topmost first). */
    private List<LevelConfig> enabledLevelsCache = null;

    /** Cached list of enabled level configs, sorted by ring radius ascending (centre first). */
    private List<LevelConfig> radiusSortedCache = null;

    /** Cached map of level ID to enabled {@link BackroomsLevel} generator instance. */
    private Map<String, BackroomsLevel> levelInstancesCache = null;

    /**
     * Constructs a new configuration loader.
     *
     * @param plugin the owning plugin instance
     */
    public BackroomsConfig(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Loads (or reloads) all configuration from the plugin's {@code config.yml}.
     *
     * <p>This clears any previously cached level data and re-reads every section.
     * Call this after {@code reloadConfig()} to apply changes without restarting.</p>
     */
    public void load() {
        FileConfiguration cfg = plugin.getConfig();

        backroomsWorldName = cfg.getString("backrooms-world-name", "backrooms");
        generationSeed = cfg.getLong("generation.seed", 0);
        gridCellSize = cfg.getInt("generation.grid-cell-size", 16);
        stairwellChance = cfg.getDouble("generation.stairwell-chance", 0.02);
        lootRoomChance = cfg.getDouble("generation.loot-room-chance", 0.04);
        hazardRoomChance = cfg.getDouble("generation.hazard-room-chance", 0.03);

        mobSpawning = cfg.getBoolean("gameplay.mob-spawning", true);
        lootGeneration = cfg.getBoolean("gameplay.loot-generation", true);
        hazards = cfg.getBoolean("gameplay.hazards", true);
        hungerEnabled = cfg.getBoolean("gameplay.hunger-enabled", true);
        pvp = cfg.getBoolean("gameplay.pvp", false);
        difficulty = cfg.getString("gameplay.difficulty", "NORMAL");
        spawnMonsters = cfg.getBoolean("gameplay.spawn-monsters", true);
        spawnAnimals = cfg.getBoolean("gameplay.spawn-animals", false);
        weatherEnabled = cfg.getBoolean("gameplay.weather-enabled", false);

        blueMapEnabled = cfg.getBoolean("bluemap.enabled", true);
        blueMapMarkers = cfg.getBoolean("bluemap.markers", true);
        blueMapOverlays = cfg.getBoolean("bluemap.overlays", true);
        blueMapDedicatedMap = cfg.getBoolean("bluemap.dedicated-map", true);
        blueMapName = cfg.getString("bluemap.map-name", "Backrooms");

        levels.clear();
        enabledLevelsCache = null;
        radiusSortedCache = null;
        levelInstancesCache = null;

        ConfigurationSection levelsSection = cfg.getConfigurationSection("levels");
        if (levelsSection != null) {
            for (String key : levelsSection.getKeys(false)) {
                ConfigurationSection ls = levelsSection.getConfigurationSection(key);
                if (ls == null) continue;

                LevelConfig lc = new LevelConfig();
                lc.setId(key);
                lc.setEnabled(ls.getBoolean("enabled", true));
                lc.setName(ls.getString("name", key));
                lc.setMinY(ls.getInt("min-y", -64));
                lc.setMaxY(ls.getInt("max-y", 0));
                lc.setMinRadius(ls.getInt("min-radius", 0));
                lc.setMaxRadius(ls.getInt("max-radius", -1));
                lc.setCeilingHeight(ls.getInt("ceiling-height", 4));

                ConfigurationSection room = ls.getConfigurationSection("room");
                if (room != null) {
                    lc.setRoomMinWidth(room.getInt("min-width", 8));
                    lc.setRoomMaxWidth(room.getInt("max-width", 24));
                    lc.setRoomMinLength(room.getInt("min-length", 8));
                    lc.setRoomMaxLength(room.getInt("max-length", 24));
                    lc.setWallMaterial(Material.matchMaterial(room.getString("wall-material", "BIRCH_PLANKS")));
                    lc.setFloorMaterial(Material.matchMaterial(room.getString("floor-material", "LIGHT_GRAY_CONCRETE")));
                    lc.setCeilingMaterial(Material.matchMaterial(room.getString("ceiling-material", "SMOOTH_STONE")));
                }

                ConfigurationSection lighting = ls.getConfigurationSection("lighting");
                if (lighting != null) {
                    lc.setLightMaterial(Material.matchMaterial(lighting.getString("material", "SEA_LANTERN")));
                    lc.setLightSpacing(lighting.getInt("spacing", 5));
                }

                lc.setCorridorWidth(ls.getInt("corridor-width", 2));
                lc.setSpecialRoomChance(ls.getDouble("special-room-chance", 0.05));

                levels.put(key, lc);
            }
        }
    }

    /**
     * Returns the list of enabled level configs, sorted by ring radius ascending.
     *
     * <p>The centre-most level (smallest {@code min-radius}) comes first. Results are
     * cached after the first call and invalidated when {@link #load()} is called again.</p>
     *
     * @return an unmodifiable list of enabled level configs, centre-out
     */
    public List<LevelConfig> getRadiusSortedLevels() {
        if (radiusSortedCache == null) {
            List<LevelConfig> enabled = new ArrayList<>();
            for (LevelConfig lc : levels.values()) {
                if (lc.isEnabled()) enabled.add(lc);
            }
            enabled.sort((a, b) -> Integer.compare(a.getMinRadius(), b.getMinRadius()));
            radiusSortedCache = enabled;
        }
        return radiusSortedCache;
    }

    /**
     * Returns the list of enabled {@link LevelConfig} data objects, sorted by Y descending.
     *
     * <p>The topmost level (highest minY) comes first. Results are cached after the
     * first call and invalidated when {@link #load()} is called again.</p>
     *
     * @return an unmodifiable list of enabled level configs
     */
    public List<LevelConfig> getEnabledLevels() {
        if (enabledLevelsCache == null) {
            List<LevelConfig> enabled = new ArrayList<>(getRadiusSortedLevels());
            enabled.sort((a, b) -> Integer.compare(b.getMinY(), a.getMinY()));
            enabledLevelsCache = enabled;
        }
        return enabledLevelsCache;
    }

    /**
     * Returns the level whose ring contains the given chunk, or {@code null} if the
     * chunk lies outside every configured ring.
     *
     * <p>The world is arranged as concentric "ripple" rings around spawn (chunk 0,0):
     * each chunk belongs to exactly one level, chosen by the Euclidean distance of the
     * chunk's centre from the world origin measured in blocks. A chunk matches a level
     * when {@code minRadius <= dist} and ({@code maxRadius < 0} or {@code dist < maxRadius}).</p>
     *
     * @param chunkX the chunk's X coordinate
     * @param chunkZ the chunk's Z coordinate
     * @return the level config owning this chunk, or {@code null} for unclaimed chunks
     */
    public LevelConfig getLevelForChunk(int chunkX, int chunkZ) {
        double cx = chunkX * 16.0 + 8.0;
        double cz = chunkZ * 16.0 + 8.0;
        double dist = Math.sqrt(cx * cx + cz * cz);

        for (LevelConfig lc : getRadiusSortedLevels()) {
            if (dist >= lc.getMinRadius()
                    && (lc.getMaxRadius() < 0 || dist < lc.getMaxRadius())) {
                return lc;
            }
        }
        return null;
    }

    /**
     * Returns the enabled {@link BackroomsLevel} generator instances keyed by level ID.
     *
     * <p>Each level config is mapped to its concrete generator class:</p>
     * <ul>
     *   <li>{@code level0} &rarr; {@link Level0Lobby}</li>
     *   <li>{@code level1} &rarr; {@link Level1HabitableZone}</li>
     *   <li>{@code level2} &rarr; {@link Level2PipeDreams}</li>
     * </ul>
     *
     * <p>Unknown level IDs fall back to {@link GenericBackroomsLevel} so new levels can
     * be added purely through configuration.</p>
     *
     * @return an unmodifiable map of level ID to generator instance (enabled levels only)
     */
    public Map<String, BackroomsLevel> getLevelInstances() {
        if (levelInstancesCache == null) {
            Map<String, BackroomsLevel> instances = new HashMap<>();
            long seed = generationSeed != 0 ? generationSeed : 0;

            for (LevelConfig lc : getRadiusSortedLevels()) {
                instances.put(lc.getId(), createLevelInstance(lc, seed));
            }

            levelInstancesCache = instances;
        }
        return levelInstancesCache;
    }

    /**
     * Returns the generator instance for a single level ID, or {@code null} when the
     * level is unknown or disabled.
     *
     * @param id the level ID (e.g. "level0")
     * @return the level's generator instance, or {@code null}
     */
    public BackroomsLevel getLevelInstanceById(String id) {
        return getLevelInstances().get(id);
    }

    /**
     * Returns all enabled {@link BackroomsLevel} generator instances.
     *
     * @return a list of enabled level generator instances
     */
    public List<BackroomsLevel> getEnabledLevelInstances() {
        return new ArrayList<>(getLevelInstances().values());
    }

    /**
     * Creates the appropriate {@link BackroomsLevel} subclass for the given config.
     *
     * @param lc   the level configuration data
     * @param seed the generation seed
     * @return a new level generator instance
     */
    private BackroomsLevel createLevelInstance(LevelConfig lc, long seed) {
        return switch (lc.getId()) {
            case "level0" -> new Level0Lobby(lc, seed);
            case "level1" -> new Level1HabitableZone(lc, seed);
            case "level2" -> new Level2PipeDreams(lc, seed);
            default -> new GenericBackroomsLevel(lc, seed);
        };
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    /** Returns the name of the Backrooms world. */
    public String getBackroomsWorldName() { return backroomsWorldName; }

    /** Returns the fixed generation seed (0 means use world seed). */
    public long getGenerationSeed() { return generationSeed; }

    /** Returns the grid cell size for room placement. */
    public int getGridCellSize() { return gridCellSize; }

    /** Returns the stairwell connection probability. */
    public double getStairwellChance() { return stairwellChance; }

    /** Returns the loot room probability. */
    public double getLootRoomChance() { return lootRoomChance; }

    /** Returns the hazard room probability. */
    public double getHazardRoomChance() { return hazardRoomChance; }

    /** Returns whether mob spawning is enabled. */
    public boolean isMobSpawning() { return mobSpawning; }

    /** Returns whether loot generation is enabled. */
    public boolean isLootGeneration() { return lootGeneration; }

    /** Returns whether environmental hazards are enabled. */
    public boolean isHazards() { return hazards; }

    /** Returns whether hunger is enabled. */
    public boolean isHungerEnabled() { return hungerEnabled; }

    /** Returns whether PvP is enabled. */
    public boolean isPvp() { return pvp; }

    /** Returns the world difficulty string. */
    public String getDifficulty() { return difficulty; }

    /** Returns whether monsters spawn. */
    public boolean isSpawnMonsters() { return spawnMonsters; }

    /** Returns whether animals spawn. */
    public boolean isSpawnAnimals() { return spawnAnimals; }

    /** Returns whether weather is enabled. */
    public boolean isWeatherEnabled() { return weatherEnabled; }

    /** Returns whether BlueMap integration is enabled. */
    public boolean isBlueMapEnabled() { return blueMapEnabled; }

    /** Returns whether BlueMap markers are enabled. */
    public boolean isBlueMapMarkers() { return blueMapMarkers; }

    /** Returns whether BlueMap overlays are enabled. */
    public boolean isBlueMapOverlays() { return blueMapOverlays; }

    /** Returns whether a dedicated BlueMap map is used. */
    public boolean isBlueMapDedicatedMap() { return blueMapDedicatedMap; }

    /** Returns the BlueMap map name. */
    public String getBlueMapName() { return blueMapName; }

    /** Returns the raw map of level ID to {@link LevelConfig}. */
    public Map<String, LevelConfig> getLevels() { return levels; }
}

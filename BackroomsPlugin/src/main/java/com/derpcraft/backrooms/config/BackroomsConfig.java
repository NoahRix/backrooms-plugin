package com.derpcraft.backrooms.config;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BackroomsConfig {

    private final JavaPlugin plugin;

    private String backroomsWorldName;
    private long generationSeed;
    private int gridCellSize;
    private double stairwellChance;
    private double lootRoomChance;
    private double hazardRoomChance;

    private boolean mobSpawning;
    private boolean lootGeneration;
    private boolean hazards;
    private boolean hungerEnabled;
    private boolean pvp;
    private String difficulty;
    private boolean spawnMonsters;
    private boolean spawnAnimals;
    private boolean weatherEnabled;

    private boolean blueMapEnabled;
    private boolean blueMapMarkers;
    private boolean blueMapOverlays;
    private boolean blueMapDedicatedMap;
    private String blueMapName;

    private final Map<String, LevelConfig> levels = new HashMap<>();

    public BackroomsConfig(JavaPlugin plugin) {
        this.plugin = plugin;
    }

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

    public List<LevelConfig> getEnabledLevels() {
        List<LevelConfig> enabled = new ArrayList<>();
        for (LevelConfig lc : levels.values()) {
            if (lc.isEnabled()) enabled.add(lc);
        }
        enabled.sort((a, b) -> Integer.compare(b.getMinY(), a.getMinY()));
        return enabled;
    }

    public LevelConfig getLevelForY(int y) {
        for (LevelConfig lc : levels.values()) {
            if (lc.isEnabled() && y >= lc.getMinY() && y < lc.getMaxY()) {
                return lc;
            }
        }
        return null;
    }

    public String getBackroomsWorldName() { return backroomsWorldName; }
    public long getGenerationSeed() { return generationSeed; }
    public int getGridCellSize() { return gridCellSize; }
    public double getStairwellChance() { return stairwellChance; }
    public double getLootRoomChance() { return lootRoomChance; }
    public double getHazardRoomChance() { return hazardRoomChance; }
    public boolean isMobSpawning() { return mobSpawning; }
    public boolean isLootGeneration() { return lootGeneration; }
    public boolean isHazards() { return hazards; }
    public boolean isHungerEnabled() { return hungerEnabled; }
    public boolean isPvp() { return pvp; }
    public String getDifficulty() { return difficulty; }
    public boolean isSpawnMonsters() { return spawnMonsters; }
    public boolean isSpawnAnimals() { return spawnAnimals; }
    public boolean isWeatherEnabled() { return weatherEnabled; }
    public boolean isBlueMapEnabled() { return blueMapEnabled; }
    public boolean isBlueMapMarkers() { return blueMapMarkers; }
    public boolean isBlueMapOverlays() { return blueMapOverlays; }
    public boolean isBlueMapDedicatedMap() { return blueMapDedicatedMap; }
    public String getBlueMapName() { return blueMapName; }
    public Map<String, LevelConfig> getLevels() { return levels; }
}

package org.derpcraft.liminal.integration.bluemap;

import org.derpcraft.liminal.LiminalPlugin;
import org.derpcraft.liminal.config.LevelConfig;
import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.BlueMapWorld;
import de.bluecolored.bluemap.api.markers.*;
import de.bluecolored.bluemap.api.math.Color;
import de.bluecolored.bluemap.api.math.Shape;
import org.bukkit.World;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class BlueMapHook {

    private final LiminalPlugin plugin;
    private final Map<String, MarkerSet> markerSets = new ConcurrentHashMap<>();
    private Consumer<BlueMapAPI> enableListener;
    private Consumer<BlueMapAPI> disableListener;

    public BlueMapHook(LiminalPlugin plugin) {
        this.plugin = plugin;
    }

    public void register() {
        if (!plugin.getLiminalConfig().isBlueMapEnabled()) return;

        enableListener = api -> {
            initializeMarkerSets();
            registerMarkersOnMaps(api);
        };

        disableListener = api -> {
            cleanupMarkers(api);
        };

        BlueMapAPI.onEnable(enableListener);
        BlueMapAPI.onDisable(disableListener);
    }

    public void unregister() {
        if (enableListener != null) {
            BlueMapAPI.unregisterListener(enableListener);
        }
        if (disableListener != null) {
            BlueMapAPI.unregisterListener(disableListener);
        }
    }

    private void initializeMarkerSets() {
        markerSets.clear();

        if (plugin.getLiminalConfig().isBlueMapMarkers()) {
            MarkerSet specialRooms = MarkerSet.builder()
                    .label("Special Rooms")
                    .toggleable(true)
                    .defaultHidden(false)
                    .sorting(0)
                    .build();
            markerSets.put("liminal:special-rooms", specialRooms);

            MarkerSet hazards = MarkerSet.builder()
                    .label("Hazards")
                    .toggleable(true)
                    .defaultHidden(false)
                    .sorting(1)
                    .build();
            markerSets.put("liminal:hazards", hazards);

            MarkerSet stairwells = MarkerSet.builder()
                    .label("Stairwells")
                    .toggleable(true)
                    .defaultHidden(false)
                    .sorting(2)
                    .build();
            markerSets.put("liminal:stairwells", stairwells);

            MarkerSet loot = MarkerSet.builder()
                    .label("Loot")
                    .toggleable(true)
                    .defaultHidden(true)
                    .sorting(3)
                    .build();
            markerSets.put("liminal:loot", loot);
        }

        if (plugin.getLiminalConfig().isBlueMapOverlays()) {
            MarkerSet overlays = MarkerSet.builder()
                    .label("Level Zones")
                    .toggleable(true)
                    .defaultHidden(false)
                    .sorting(4)
                    .build();
            markerSets.put("liminal:level-zones", overlays);
        }
    }

    private void registerMarkersOnMaps(BlueMapAPI api) {
        String worldName = plugin.getLiminalConfig().getLiminalWorldName();
        Optional<BlueMapWorld> bmWorld = api.getWorld(worldName);

        if (bmWorld.isEmpty()) {
            Optional<World> bukkitWorld = Optional.ofNullable(plugin.getServer().getWorld(worldName));
            if (bukkitWorld.isPresent()) {
                bmWorld = api.getWorld(bukkitWorld.get());
            }
        }

        if (bmWorld.isEmpty()) {
            plugin.getLogger().warning("BlueMap: Liminal world '" + worldName + "' not found in BlueMap");
            return;
        }

        for (BlueMapMap map : bmWorld.get().getMaps()) {
            for (Map.Entry<String, MarkerSet> entry : markerSets.entrySet()) {
                map.getMarkerSets().put(entry.getKey(), entry.getValue());
            }
        }

        if (plugin.getLiminalConfig().isBlueMapOverlays()) {
            addLevelZoneOverlays(bmWorld.get());
        }
    }

    private void addLevelZoneOverlays(BlueMapWorld world) {
        MarkerSet overlays = markerSets.get("liminal:level-zones");
        if (overlays == null) return;

        int zoneRadius = 512;

        for (LevelConfig level : plugin.getLiminalConfig().getEnabledLevels()) {
            Shape rect = Shape.createRect(-zoneRadius, -zoneRadius, zoneRadius, zoneRadius);

            Color fillColor = getLevelColor(level.getId());
            Color lineColor = new Color(fillColor.getRed(), fillColor.getGreen(), fillColor.getBlue(), 1.0f);
            Color semiFill = new Color(fillColor.getRed(), fillColor.getGreen(), fillColor.getBlue(), 0.15f);

            ShapeMarker marker = ShapeMarker.builder()
                    .label(level.getName())
                    .shape(rect, level.getMinY())
                    .lineColor(lineColor)
                    .fillColor(semiFill)
                    .lineWidth(3)
                    .depthTestEnabled(false)
                    .centerPosition()
                    .build();

            overlays.put("level-zone-" + level.getId(), marker);
        }
    }

    private Color getLevelColor(String levelId) {
        return switch (levelId) {
            case "level0" -> new Color(255, 220, 50, 0.5f);
            case "level1" -> new Color(100, 100, 120, 0.5f);
            case "level2" -> new Color(60, 60, 80, 0.5f);
            default -> new Color(200, 200, 200, 0.5f);
        };
    }

    public void addSpecialRoomMarker(int x, int y, int z, String label, String detail) {
        MarkerSet set = markerSets.get("liminal:special-rooms");
        if (set == null) return;

        POIMarker marker = POIMarker.builder()
                .label(label)
                .position((double) x, (double) y, (double) z)
                .detail(detail)
                .defaultIcon()
                .minDistance(0)
                .maxDistance(5000)
                .build();

        set.put("special-" + x + "-" + y + "-" + z, marker);
    }

    public void addHazardMarker(int x, int y, int z, String hazardType) {
        MarkerSet set = markerSets.get("liminal:hazards");
        if (set == null) return;

        POIMarker marker = POIMarker.builder()
                .label("Hazard: " + hazardType)
                .position((double) x, (double) y, (double) z)
                .detail("Environmental hazard: " + hazardType)
                .defaultIcon()
                .minDistance(0)
                .maxDistance(2000)
                .styleClasses("hazard-marker")
                .build();

        set.put("hazard-" + x + "-" + y + "-" + z, marker);
    }

    public void addStairwellMarker(int x, int y, int z, String targetLevel) {
        MarkerSet set = markerSets.get("liminal:stairwells");
        if (set == null) return;

        POIMarker marker = POIMarker.builder()
                .label("Stairwell to " + targetLevel)
                .position((double) x, (double) y, (double) z)
                .detail("Connects to " + targetLevel)
                .defaultIcon()
                .minDistance(0)
                .maxDistance(5000)
                .build();

        set.put("stairwell-" + x + "-" + y + "-" + z, marker);
    }

    public void addLootMarker(int x, int y, int z) {
        MarkerSet set = markerSets.get("liminal:loot");
        if (set == null) return;

        POIMarker marker = POIMarker.builder()
                .label("Loot Container")
                .position((double) x, (double) y, (double) z)
                .detail("A lootable container")
                .defaultIcon()
                .minDistance(0)
                .maxDistance(1000)
                .build();

        set.put("loot-" + x + "-" + y + "-" + z, marker);
    }

    public void addLevelZoneOverlay(int x1, int z1, int x2, int z2, int y, String levelName, String levelId) {
        MarkerSet set = markerSets.get("liminal:level-zones");
        if (set == null) return;

        Shape rect = Shape.createRect(x1, z1, x2, z2);
        Color fillColor = getLevelColor(levelId);
        Color lineColor = new Color(fillColor.getRed(), fillColor.getGreen(), fillColor.getBlue(), 1.0f);
        Color semiFill = new Color(fillColor.getRed(), fillColor.getGreen(), fillColor.getBlue(), 0.2f);

        ShapeMarker marker = ShapeMarker.builder()
                .label(levelName)
                .shape(rect, y)
                .lineColor(lineColor)
                .fillColor(semiFill)
                .lineWidth(2)
                .depthTestEnabled(false)
                .centerPosition()
                .build();

        set.put("zone-" + levelId + "-" + x1 + "-" + z1, marker);
    }

    private void cleanupMarkers(BlueMapAPI api) {
        String worldName = plugin.getLiminalConfig().getLiminalWorldName();
        api.getWorld(worldName).ifPresent(world -> {
            for (BlueMapMap map : world.getMaps()) {
                for (String key : markerSets.keySet()) {
                    map.getMarkerSets().remove(key);
                }
            }
        });
        markerSets.clear();
    }
}

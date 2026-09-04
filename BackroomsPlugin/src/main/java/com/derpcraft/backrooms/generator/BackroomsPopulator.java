package com.derpcraft.backrooms.generator;

import com.derpcraft.backrooms.config.BackroomsConfig;
import com.derpcraft.backrooms.config.LevelConfig;
import org.bukkit.Material;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.LimitedRegion;
import org.bukkit.generator.WorldInfo;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Random;

public class BackroomsPopulator extends BlockPopulator {

    private final BackroomsConfig config;

    public BackroomsPopulator(BackroomsConfig config) {
        this.config = config;
    }

    @Override
    public void populate(@NotNull WorldInfo worldInfo, @NotNull Random random, int chunkX, int chunkZ, @NotNull LimitedRegion limitedRegion) {
        List<LevelConfig> levels = config.getEnabledLevels();
        if (levels.isEmpty()) return;

        long seed = config.getGenerationSeed() != 0 ? config.getGenerationSeed() : worldInfo.getSeed();
        Random seededRandom = new Random(seed ^ ((long) chunkX * 341873128712L + (long) chunkZ * 132897987541L));

        for (LevelConfig level : levels) {
            int levelFloor = level.getMinY();

            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    int globalX = chunkX * 16 + x;
                    int globalZ = chunkZ * 16 + z;

                    if (!limitedRegion.isInRegion(globalX, levelFloor + 1, globalZ)) continue;

                    Material floorBelow = limitedRegion.getType(globalX, levelFloor, globalZ);
                    if (floorBelow != level.getFloorMaterial()) continue;

                    if (config.isLootGeneration() && seededRandom.nextDouble() < 0.005) {
                        placeLoot(limitedRegion, globalX, levelFloor + 1, globalZ, seededRandom);
                    }

                    if (config.isHazards() && seededRandom.nextDouble() < 0.003) {
                        placeHazard(limitedRegion, globalX, levelFloor + 1, globalZ, seededRandom, level);
                    }
                }
            }
        }
    }

    private void placeLoot(@NotNull LimitedRegion region, int x, int y, int z, @NotNull Random random) {
        if (!region.isInRegion(x, y, z)) return;
        if (region.getType(x, y, z) != Material.AIR) return;

        Material container = random.nextBoolean() ? Material.CHEST : Material.BARREL;
        region.setType(x, y, z, container);
    }

    private void placeHazard(@NotNull LimitedRegion region, int x, int y, int z, @NotNull Random random, @NotNull LevelConfig level) {
        if (!region.isInRegion(x, y, z)) return;
        if (region.getType(x, y, z) != Material.AIR) return;

        int hazardType = random.nextInt(3);
        switch (hazardType) {
            case 0:
                if (region.isInRegion(x, y, z) && region.getType(x, y, z) == Material.AIR) {
                    region.setType(x, y, z, Material.WATER);
                }
                break;
            case 1:
                if (region.isInRegion(x, y - 1, z) && region.getType(x, y - 1, z) == level.getFloorMaterial()) {
                    region.setType(x, y, z, Material.FIRE);
                }
                break;
            case 2:
                if (region.isInRegion(x, y, z) && region.getType(x, y, z) == Material.AIR) {
                    region.setType(x, y, z, Material.COBWEB);
                    if (region.isInRegion(x, y + 1, z)) {
                        region.setType(x, y + 1, z, Material.COBWEB);
                    }
                }
                break;
        }
    }
}

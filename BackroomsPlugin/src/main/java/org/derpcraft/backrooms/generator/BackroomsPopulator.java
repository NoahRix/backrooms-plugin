package org.derpcraft.backrooms.generator;

import org.derpcraft.backrooms.config.BackroomsConfig;
import org.derpcraft.backrooms.config.LevelConfig;
import org.bukkit.Material;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.LimitedRegion;
import org.bukkit.generator.WorldInfo;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Random;

/**
 * Block populator for the Backrooms world.
 *
 * <p>Runs after the chunk noise generation phase and adds small-scale decorations
 * that benefit from knowing the surrounding blocks. Currently handles:</p>
 * <ul>
 *   <li><b>Loot containers</b> &ndash; chests and barrels placed on the floor inside rooms</li>
 *   <li><b>Hazards</b> &ndash; water puddles, fire patches, and cobwebs</li>
 * </ul>
 *
 * <p>Population is deterministic: the same seed and chunk coordinates always produce
 * the same decorations.</p>
 *
 * @see BackroomsChunkGenerator
 */
public class BackroomsPopulator extends BlockPopulator {

    /** Plugin configuration holding generation settings. */
    private final BackroomsConfig config;

    /**
     * Constructs a new Backrooms block populator.
     *
     * @param config the plugin configuration
     */
    public BackroomsPopulator(BackroomsConfig config) {
        this.config = config;
    }

    /**
     * Populates a chunk with loot containers and hazards.
     *
     * <p>For each enabled level, iterates over the chunk's floor blocks and randomly
     * places loot or hazard blocks based on the configured probabilities.</p>
     *
     * @param worldInfo    information about the world
     * @param random       the chunk-specific random
     * @param chunkX       the chunk's X coordinate
     * @param chunkZ       the chunk's Z coordinate
     * @param limitedRegion the mutable region for this chunk (with surrounding context)
     */
    @Override
    public void populate(@NotNull WorldInfo worldInfo, @NotNull Random random,
                         int chunkX, int chunkZ, @NotNull LimitedRegion limitedRegion) {
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

    /**
     * Places a loot container (chest or barrel) at the given position.
     *
     * @param region the mutable region
     * @param x      the block X coordinate
     * @param y      the block Y coordinate
     * @param z      the block Z coordinate
     * @param random the random source
     */
    private void placeLoot(@NotNull LimitedRegion region, int x, int y, int z, @NotNull Random random) {
        if (!region.isInRegion(x, y, z)) return;
        if (region.getType(x, y, z) != Material.AIR) return;

        Material container = random.nextBoolean() ? Material.CHEST : Material.BARREL;
        region.setType(x, y, z, container);
    }

    /**
     * Places a hazard block at the given position.
     *
     * <p>Hazard types are chosen randomly:</p>
     * <ul>
     *   <li><b>0</b> &ndash; water (flooding hazard)</li>
     *   <li><b>1</b> &ndash; fire (burn hazard, only if floor is present below)</li>
     *   <li><b>2</b> &ndash; cobweb (movement hazard, placed as a 2-high column)</li>
     * </ul>
     *
     * @param region the mutable region
     * @param x      the block X coordinate
     * @param y      the block Y coordinate
     * @param z      the block Z coordinate
     * @param random the random source
     * @param level  the level configuration for material checks
     */
    private void placeHazard(@NotNull LimitedRegion region, int x, int y, int z,
                             @NotNull Random random, @NotNull LevelConfig level) {
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

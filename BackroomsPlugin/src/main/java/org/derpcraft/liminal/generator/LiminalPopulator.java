package org.derpcraft.liminal.generator;

import org.derpcraft.liminal.config.LiminalConfig;
import org.derpcraft.liminal.config.LevelConfig;
import org.derpcraft.liminal.generator.levels.LiminalLevel;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.LimitedRegion;
import org.bukkit.generator.WorldInfo;

import org.jetbrains.annotations.NotNull;

import java.util.Random;

/**
 * Block populator for the Liminal world.
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
 * @see LiminalChunkGenerator
 */
public class LiminalPopulator extends BlockPopulator {

    /** Plugin configuration holding generation settings. */
    private final LiminalConfig config;

    /**
     * Constructs a new Liminal block populator.
     *
     * @param config the plugin configuration
     */
    public LiminalPopulator(LiminalConfig config) {
        this.config = config;
    }

    /**
     * Populates a chunk with loot containers and hazards.
     *
     * <p>Only the level whose ripple ring contains this chunk is populated: the
     * populator walks the chunk's ground-floor surface blocks and randomly places
     * loot or hazard blocks based on the configured probabilities.</p>
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
        LevelConfig level = config.getLevelForChunk(chunkX, chunkZ);
        if (level == null || !level.isEnabled()) return;

        LiminalLevel instance = config.getLevelInstanceById(level.getId());
        if (instance == null) return;

        long seed = config.getGenerationSeed() != 0 ? config.getGenerationSeed() : worldInfo.getSeed();
        Random seededRandom = new Random(seed ^ ((long) chunkX * 341873128712L + (long) chunkZ * 132897987541L));

        // Level 3: fill in station sign text and ghost-supply chests.
        if ("level3".equals(level.getId())) {
            decorateRails(limitedRegion, chunkX, chunkZ, instance, seededRandom);
            return;
        }

        int floorSurfaceY = instance.getFloorSurfaceY();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int globalX = chunkX * 16 + x;
                int globalZ = chunkZ * 16 + z;

                if (!limitedRegion.isInRegion(globalX, floorSurfaceY + 1, globalZ)) continue;

                Material floorBelow = limitedRegion.getType(globalX, floorSurfaceY, globalZ);
                if (floorBelow != level.getFloorMaterial()) continue;

                if (config.isLootGeneration() && seededRandom.nextDouble() < 0.005) {
                    placeLoot(limitedRegion, globalX, floorSurfaceY + 1, globalZ, seededRandom);
                }

                if (config.isHazards() && seededRandom.nextDouble() < 0.003) {
                    placeHazard(limitedRegion, globalX, floorSurfaceY + 1, globalZ, seededRandom, level);
                }
            }
        }
    }

    /** Cryptic messages the abandoned stations announce. */
    private static final String[] STATION_MESSAGES = {
            "DO NOT BOARD",
            "THE NEXT STOP IS YOURS",
            "WE ARE STILL WAITING",
            "SERVICE DISCONTINUED",
            "THE TUNNEL REMEMBERS",
            "MIND THE GAP",
            "NO EXIT",
            "LAST STOP. PROMISE.",
            "THE CARS COME BACK EMPTY",
            "TICKETS ARE MEMORIES",
    };

    /**
     * Decorates a Level 3 rail chunk: writes cryptic text onto the station sign
     * posts and fills station chests with the previous crew's supplies.
     */
    private void decorateRails(@NotNull LimitedRegion region, int chunkX, int chunkZ,
                               @NotNull LiminalLevel level, @NotNull Random random) {
        int floorSurfaceY = level.getFloorSurfaceY();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int globalX = chunkX * 16 + x;
                int globalZ = chunkZ * 16 + z;
                if (!region.isInRegion(globalX, floorSurfaceY + 1, globalZ)) continue;

                Material type = region.getType(globalX, floorSurfaceY + 1, globalZ);
                if (type == Material.OAK_SIGN) {
                    org.bukkit.block.BlockState state = region.getBlockState(globalX, floorSurfaceY + 1, globalZ);
                    if (state instanceof org.bukkit.block.Sign sign) {
                        sign.setLine(1, STATION_MESSAGES[random.nextInt(STATION_MESSAGES.length)]);
                        sign.update();
                    }
                } else if (type == Material.CHEST) {
                    org.bukkit.block.BlockState state = region.getBlockState(globalX, floorSurfaceY + 1, globalZ);
                    if (state instanceof org.bukkit.block.Chest chest) {
                        ItemStack[] supplies = {
                                new ItemStack(Material.RAIL, 4 + random.nextInt(10)),
                                new ItemStack(Material.POWERED_RAIL, 2 + random.nextInt(6)),
                                new ItemStack(Material.REDSTONE_TORCH, 2 + random.nextInt(6)),
                                new ItemStack(Material.REDSTONE, 1 + random.nextInt(8)),
                                new ItemStack(Material.COAL, 3 + random.nextInt(8)),
                                new ItemStack(Material.BOOK, 1 + random.nextInt(2)),
                        };
                        for (ItemStack item : supplies) {
                            if (random.nextDouble() < 0.6) chest.getInventory().addItem(item);
                        }
                        chest.update();
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

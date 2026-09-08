package org.derpcraft.backrooms.generator;

import org.derpcraft.backrooms.config.BackroomsConfig;
import org.derpcraft.backrooms.config.LevelConfig;
import org.derpcraft.backrooms.generator.levels.BackroomsLevel;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.WorldInfo;
import org.bukkit.generator.ChunkGenerator;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Chunk generator for the Backrooms world.
 *
 * <p>This generator delegates the actual block placement to a list of {@link BackroomsLevel}
 * instances, one for each enabled level in the configuration. Each level occupies a vertical
 * slice of the world and generates its own rooms, walls, lighting, and special features.</p>
 *
 * <h2>Generation flow</h2>
 * <ol>
 *   <li>For each enabled level, check if its Y range overlaps the world bounds</li>
 *   <li>Call {@link BackroomsLevel#generate} to produce all blocks for that level</li>
 *   <li>Place a bedrock layer at the world's minimum Y</li>
 * </ol>
 *
 * <p>The generator also provides a safe spawn location that places players 2 blocks
 * above the floor of the topmost level, preventing them from spawning inside blocks.</p>
 *
 * @see BackroomsLevel
 * @see BackroomsConfig
 */
public class BackroomsChunkGenerator extends ChunkGenerator {

    /** Plugin configuration holding world settings and level definitions. */
    private final BackroomsConfig config;

    /** Biome provider that returns THE_VOID for all positions (no natural biome features). */
    private final BackroomsBiomeProvider biomeProvider;

    /**
     * Constructs a new Backrooms chunk generator.
     *
     * @param config the plugin configuration containing level definitions and world settings
     */
    public BackroomsChunkGenerator(BackroomsConfig config) {
        this.config = config;
        this.biomeProvider = new BackroomsBiomeProvider();
    }

    /**
     * Generates the noise (base blocks) for a chunk by delegating to each enabled level.
     *
     * <p>Each level generates its own floor, ceiling, rooms, walls, lighting, and special
     * features within its configured Y range. Levels that fall outside the world's vertical
     * bounds are skipped.</p>
     *
     * @param worldInfo information about the world being generated
     * @param random    the world-specific random (not used; levels use seeded randomness)
     * @param chunkX    the chunk's X coordinate
     * @param chunkZ    the chunk's Z coordinate
     * @param chunkData the mutable chunk data to write blocks into
     */
    @Override
    public void generateNoise(@NotNull WorldInfo worldInfo, @NotNull Random random,
                              int chunkX, int chunkZ, @NotNull ChunkData chunkData) {
        long seed = config.getGenerationSeed() != 0 ? config.getGenerationSeed() : worldInfo.getSeed();
        int worldMinY = worldInfo.getMinHeight();
        int worldMaxY = worldInfo.getMaxHeight();

        int chunkStartX = chunkX * 16;
        int chunkStartZ = chunkZ * 16;
        int chunkEndX = chunkStartX + 16;
        int chunkEndZ = chunkStartZ + 16;

        for (BackroomsLevel level : config.getEnabledLevelInstances()) {
            LevelConfig lc = level.getConfig();

            if (lc.getMaxY() <= worldMinY || lc.getMinY() >= worldMaxY) {
                continue;
            }

            level.generate(chunkData, chunkStartX, chunkStartZ, chunkEndX, chunkEndZ, worldMinY, worldMaxY);
        }

        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                chunkData.setBlock(localX, worldMinY, localZ, Material.BEDROCK);
            }
        }
    }

    /**
     * Returns whether vanilla structures (villages, temples, etc.) should generate.
     *
     * <p>Always returns {@code true} to allow the generator to function, though the
     * Backrooms world typically has no vanilla structures due to the custom generation.</p>
     *
     * @return {@code true}
     */
    @Override
    public boolean shouldGenerateStructures() {
        return true;
    }

    /**
     * Returns the list of block populators for this generator.
     *
     * <p>Currently returns an empty list; all population is handled during the noise
     * generation phase by the level classes.</p>
     *
     * @param world the world being generated
     * @return an empty list
     */
    @Override
    public @NotNull List<BlockPopulator> getDefaultPopulators(@NotNull World world) {
        return Collections.emptyList();
    }

    /** {@inheritDoc} */
    @Override
    public void generateSurface(@NotNull WorldInfo worldInfo, @NotNull Random random,
                                int chunkX, int chunkZ, @NotNull ChunkData chunkData) {
    }

    /** {@inheritDoc} */
    @Override
    public void generateBedrock(@NotNull WorldInfo worldInfo, @NotNull Random random,
                                int chunkX, int chunkZ, @NotNull ChunkData chunkData) {
    }

    /** {@inheritDoc} */
    @Override
    public void generateCaves(@NotNull WorldInfo worldInfo, @NotNull Random random,
                              int chunkX, int chunkZ, @NotNull ChunkData chunkData) {
    }

    /**
     * Returns the base height for heightmap calculations.
     *
     * <p>Returns a fixed value of {@code worldMinY + 10} since the Backrooms world
     * has no natural terrain height variation.</p>
     *
     * @param worldInfo information about the world
     * @param random    the world-specific random
     * @param x         the block X coordinate
     * @param z         the block Z coordinate
     * @param heightMap the heightmap type being queried
     * @return a fixed base height
     */
    @Override
    public int getBaseHeight(@NotNull WorldInfo worldInfo, @NotNull Random random,
                             int x, int z, @NotNull org.bukkit.HeightMap heightMap) {
        return worldInfo.getMinHeight() + 10;
    }

    /**
     * Returns the biome provider for this generator.
     *
     * <p>The Backrooms world uses {@link BackroomsBiomeProvider} which returns
     * {@code THE_VOID} for all positions, preventing natural biome features.</p>
     *
     * @param worldInfo information about the world
     * @return the biome provider
     */
    @Override
    public @NotNull BiomeProvider getDefaultBiomeProvider(@NotNull WorldInfo worldInfo) {
        return biomeProvider;
    }

    /** {@inheritDoc} */
    @Override
    public boolean shouldGenerateNoise() { return false; }
    /** {@inheritDoc} */
    @Override
    public boolean shouldGenerateSurface() { return false; }
    /** {@inheritDoc} */
    @Override
    public boolean shouldGenerateBedrock() { return false; }
    /** {@inheritDoc} */
    @Override
    public boolean shouldGenerateCaves() { return false; }
    /** {@inheritDoc} */
    @Override
    public boolean shouldGenerateDecorations() { return false; }

    /**
     * Returns whether mobs should spawn in the Backrooms world.
     *
     * @return {@code true} if mob spawning is enabled in the configuration
     */
    @Override
    public boolean shouldGenerateMobs() { return config.isMobSpawning(); }

    /**
     * Returns a safe spawn location for players entering the Backrooms world.
     *
     * <p>The spawn point is placed 2 blocks above the floor of the topmost floor
     * of Level 0 (which has multiple floors), ensuring players don't spawn inside
     * the floor or ceiling. The X and Z coordinates are fixed at (8.5, 8.5) to
     * place the player in the centre of the spawn chunk.</p>
     *
     * @param world  the Backrooms world
     * @param random the world-specific random
     * @return a safe spawn location above the floor
     */
    @Override
    public @NotNull Location getFixedSpawnLocation(@NotNull World world, @NotNull Random random) {
        List<BackroomsLevel> enabledLevels = config.getEnabledLevelInstances();
        int spawnY = world.getMinHeight() + 4;

        if (!enabledLevels.isEmpty()) {
            BackroomsLevel topLevel = enabledLevels.get(0);
            
            // Check if this is Level 0 with multiple floors
            if (topLevel instanceof org.derpcraft.backrooms.generator.levels.Level0Lobby) {
                // Calculate the top floor of Level 0
                int floorOffset = 2; // Level 0's floor offset
                int ceilingHeight = topLevel.getConfig().getCeilingHeight();
                int floorHeight = floorOffset + ceilingHeight;
                int numFloors = 4; // Level 0 has 4 floors
                
                // Top floor starts at: minY + (numFloors - 1) * floorHeight + floorOffset
                int topFloorY = topLevel.getConfig().getMinY() + ((numFloors - 1) * floorHeight) + floorOffset;
                spawnY = topFloorY + 2; // 2 blocks above the floor
            } else {
                // Single floor level
                int floorY = topLevel.getConfig().getMinY() + topLevel.getConfig().getCeilingHeight() - 2;
                spawnY = floorY + 2;
            }
        }

        return new Location(world, 8.5, spawnY, 8.5);
    }

    /**
     * Returns the plugin configuration used by this generator.
     *
     * @return the Backrooms configuration
     */
    public BackroomsConfig getConfig() {
        return config;
    }
}

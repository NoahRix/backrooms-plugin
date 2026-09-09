package org.derpcraft.liminal.generator;

import org.derpcraft.liminal.config.LiminalConfig;
import org.derpcraft.liminal.config.LevelConfig;
import org.derpcraft.liminal.generator.levels.LiminalLevel;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.WorldInfo;
import org.bukkit.generator.ChunkGenerator;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Random;

/**
 * Chunk generator for the Liminal world.
 *
 * <p>This generator delegates the actual block placement to a list of {@link LiminalLevel}
 * instances, one for each enabled level in the configuration. The world is arranged as
 * concentric "ripple" rings around spawn: each chunk belongs to exactly one level, chosen
 * by the chunk centre's distance from the world origin, and that level fills its configured
 * Y range for the whole chunk.</p>
 *
 * <h2>Generation flow</h2>
 * <ol>
 *   <li>Determine which level's ring contains this chunk ({@link LiminalConfig#getLevelForChunk})</li>
 *   <li>Call {@link LiminalLevel#generate} to produce all blocks for that level</li>
 *   <li>Place a bedrock layer at the world's minimum Y</li>
 * </ol>
 *
 * <p>The generator also provides a safe spawn location that places players 2 blocks
 * above the floor of the innermost level, preventing them from spawning inside blocks.</p>
 *
 * @see LiminalLevel
 * @see LiminalConfig
 */
public class LiminalChunkGenerator extends ChunkGenerator {

    /** Plugin configuration holding world settings and level definitions. */
    private final LiminalConfig config;

    /** Biome provider that returns THE_VOID for all positions (no natural biome features). */
    private final LiminalBiomeProvider biomeProvider;

    /** Generates the transition corridors between adjacent level rings. */
    private final TransitionZoneGenerator transitionGenerator;

    /**
     * Constructs a new Liminal chunk generator.
     *
     * @param config the plugin configuration containing level definitions and world settings
     */
    public LiminalChunkGenerator(LiminalConfig config) {
        this.config = config;
        this.biomeProvider = new LiminalBiomeProvider();
        this.transitionGenerator = new TransitionZoneGenerator(config,
                config.getGenerationSeed() != 0 ? config.getGenerationSeed() : 0);
    }

    /**
     * Generates the noise (base blocks) for a chunk by delegating to the level whose
     * ring contains this chunk.
     *
     * <p>The world is arranged as concentric "ripple" rings around spawn: the level
     * generating in a chunk is chosen by the chunk centre's distance from the world
     * origin (see {@link LiminalConfig#getLevelForChunk}). The chosen level then
     * fills its configured Y range with floors, rooms, walls, lighting, and special
     * features. Chunks outside every ring stay empty (bedrock only).</p>
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

        LiminalConfig.TransitionInfo transition = config.getTransitionForChunk(chunkX, chunkZ);
        if (transition != null) {
            // Corridor chunk: sloped walkway with gradient materials, perimeter
            // walls, signage, and (when the outer level has rails) climbing rail.
            transitionGenerator.generate(chunkData, chunkStartX, chunkStartZ, transition);
        } else {
            LevelConfig ring = config.getLevelForChunk(chunkX, chunkZ);
            if (ring != null && ring.isEnabled()) {
                LiminalLevel level = config.getLevelInstanceById(ring.getId());
                if (level != null) {
                    level.generate(chunkData, chunkStartX, chunkStartZ, chunkEndX, chunkEndZ, worldMinY, worldMaxY);
                }
            }
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
     * Liminal world typically has no vanilla structures due to the custom generation.</p>
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
     * <p>The populator fills in station sign text and loot on Level 3, and
     * places ambient loot/hazards in the other levels' rooms.</p>
     *
     * @param world the world being generated
     * @return the Liminal block populator
     */
    @Override
    public @NotNull List<BlockPopulator> getDefaultPopulators(@NotNull World world) {
        return List.of(new LiminalPopulator(config));
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
     * <p>Returns a fixed value of {@code worldMinY + 10} since the Liminal world
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
     * <p>The Liminal world uses {@link LiminalBiomeProvider} which returns
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
     * Returns whether mobs should spawn in the Liminal world.
     *
     * @return {@code true} if mob spawning is enabled in the configuration
     */
    @Override
    public boolean shouldGenerateMobs() { return config.isMobSpawning(); }

    /**
     * Returns a safe spawn location for players entering the Liminal world.
     *
     * <p>The spawn point sits in the centre chunk (0,0), which always belongs to the
     * innermost ring (Level 0). The player's feet are placed one block above that
     * level's floor surface, at the centre of the spawn chunk (8.5, 8.5).</p>
     *
     * @param world  the Liminal world
     * @param random the world-specific random
     * @return a safe spawn location on the innermost level's floor
     */
    @Override
    public @NotNull Location getFixedSpawnLocation(@NotNull World world, @NotNull Random random) {
        int spawnY = world.getMinHeight() + 4;

        LevelConfig centreConfig = config.getLevelForChunk(0, 0);
        LiminalLevel centreLevel = centreConfig != null
                ? config.getLevelInstanceById(centreConfig.getId()) : null;

        if (centreLevel != null) {
            // Floor surface includes the level's elevation step; feet one block above it.
            spawnY = centreLevel.getConfig().getFloorSurfaceY() + 1;
        }

        return new Location(world, 8.5, spawnY, 8.5);
    }

    /**
     * Returns the plugin configuration used by this generator.
     *
     * @return the Liminal configuration
     */
    public LiminalConfig getConfig() {
        return config;
    }
}

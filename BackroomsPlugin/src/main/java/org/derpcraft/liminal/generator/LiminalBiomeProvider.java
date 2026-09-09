package org.derpcraft.liminal.generator;

import org.bukkit.block.Biome;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Biome provider for the Liminal world.
 *
 * <p>Returns {@link Biome#THE_VOID} for all positions, ensuring that no natural biome
 * features (grass, trees, water bodies, etc.) generate in the Liminal. This keeps
 * the world as a purely artificial, indoor environment.</p>
 *
 * @see LiminalChunkGenerator
 */
public class LiminalBiomeProvider extends BiomeProvider {

    /**
     * Returns the biome at the given position.
     *
     * @param worldInfo information about the world
     * @param x         the block X coordinate
     * @param y         the block Y coordinate
     * @param z         the block Z coordinate
     * @return always {@link Biome#THE_VOID}
     */
    @Override
    public @NotNull Biome getBiome(@NotNull WorldInfo worldInfo, int x, int y, int z) {
        return Biome.THE_VOID;
    }

    /**
     * Returns the list of all biomes used by this provider.
     *
     * @param worldInfo information about the world
     * @return a singleton list containing only {@link Biome#THE_VOID}
     */
    @Override
    public @NotNull List<Biome> getBiomes(@NotNull WorldInfo worldInfo) {
        return List.of(Biome.THE_VOID);
    }
}

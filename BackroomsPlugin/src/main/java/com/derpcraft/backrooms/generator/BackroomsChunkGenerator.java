package com.derpcraft.backrooms.generator;

import com.derpcraft.backrooms.config.BackroomsConfig;
import com.derpcraft.backrooms.config.LevelConfig;
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

public class BackroomsChunkGenerator extends ChunkGenerator {

    private final BackroomsConfig config;
    private final BackroomsBiomeProvider biomeProvider;

    public BackroomsChunkGenerator(BackroomsConfig config) {
        this.config = config;
        this.biomeProvider = new BackroomsBiomeProvider();
    }

    @Override
    public void generateNoise(@NotNull WorldInfo worldInfo, @NotNull Random random, int chunkX, int chunkZ, @NotNull ChunkData chunkData) {
        long seed = config.getGenerationSeed() != 0 ? config.getGenerationSeed() : worldInfo.getSeed();
        int minY = worldInfo.getMinHeight();

        LevelConfig level = config.getLevelForY(minY + 2);
        if (level == null) {
            level = config.getEnabledLevels().get(0);
        }

        int floorY = minY + 2;
        int ceilingY = floorY + level.getCeilingHeight();

        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                chunkData.setBlock(localX, minY, localZ, Material.BEDROCK);
                chunkData.setBlock(localX, floorY, localZ, level.getFloorMaterial());
                chunkData.setBlock(localX, ceilingY, localZ, level.getCeilingMaterial());
            }
        }

        Random chunkRand = new Random((long) chunkX * 341873128712L + (long) chunkZ * 132897987541L ^ seed);

        int roomMinWidth = level.getRoomMinWidth();
        int roomMaxWidth = level.getRoomMaxWidth();
        int roomMinLength = level.getRoomMinLength();
        int roomMaxLength = level.getRoomMaxLength();

        int gridX = Math.floorDiv(chunkX * 16, roomMaxWidth);
        int gridZ = Math.floorDiv(chunkZ * 16, roomMaxLength);

        long roomSeed = seed ^ ((long) gridX * 0x4f4f4f4fL) ^ ((long) gridZ * 0x2f2f2f2fL) ^ level.getId().hashCode();
        Random roomRand = new Random(roomSeed);

        int roomWidth = roomMinWidth + roomRand.nextInt(roomMaxWidth - roomMinWidth + 1);
        int roomLength = roomMinLength + roomRand.nextInt(roomMaxLength - roomMinLength + 1);

        int roomStartX = gridX * roomMaxWidth;
        int roomStartZ = gridZ * roomMaxLength;
        int roomEndX = roomStartX + roomWidth;
        int roomEndZ = roomStartZ + roomLength;

        int chunkStartX = chunkX * 16;
        int chunkStartZ = chunkZ * 16;
        int chunkEndX = chunkStartX + 16;
        int chunkEndZ = chunkStartZ + 16;

        boolean overlapsRoom = chunkEndX > roomStartX && chunkStartX < roomEndX &&
                               chunkEndZ > roomStartZ && chunkStartZ < roomEndZ;

        if (overlapsRoom) {
            for (int localX = 0; localX < 16; localX++) {
                for (int localZ = 0; localZ < 16; localZ++) {
                    int globalX = chunkStartX + localX;
                    int globalZ = chunkStartZ + localZ;

                    boolean isWall = false;
                    boolean isDoorway = false;

                    if (globalX == roomStartX || globalX == roomEndX - 1 ||
                        globalZ == roomStartZ || globalZ == roomEndZ - 1) {
                        isWall = true;

                        int doorwayPos = (int) ((roomWidth > roomLength ? roomWidth : roomLength) / 2);

                        if (globalX == roomStartX && globalZ == roomStartZ + doorwayPos) {
                            isDoorway = true;
                        } else if (globalX == roomEndX - 1 && globalZ == roomStartZ + doorwayPos) {
                            isDoorway = true;
                        } else if (globalZ == roomStartZ && globalX == roomStartX + doorwayPos) {
                            isDoorway = true;
                        } else if (globalZ == roomEndZ - 1 && globalX == roomStartX + doorwayPos) {
                            isDoorway = true;
                        }
                    }

                    for (int blockY = floorY + 1; blockY < ceilingY; blockY++) {
                        if (isDoorway) {
                            if (blockY <= floorY + 2) {
                                chunkData.setBlock(localX, blockY, localZ, Material.AIR);
                            } else {
                                chunkData.setBlock(localX, blockY, localZ, level.getWallMaterial());
                            }
                        } else if (isWall) {
                            chunkData.setBlock(localX, blockY, localZ, level.getWallMaterial());
                        } else {
                            chunkData.setBlock(localX, blockY, localZ, Material.AIR);
                        }
                    }
                }
            }
        }

        int lightSpacing = level.getLightSpacing();
        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                int globalX = chunkStartX + localX;
                int globalZ = chunkStartZ + localZ;

                if (globalX % lightSpacing == 0 && globalZ % lightSpacing == 0) {
                    if (overlapsRoom) {
                        chunkData.setBlock(localX, ceilingY, localZ, level.getLightMaterial());
                    }
                }
            }
        }

        if (chunkRand.nextInt(1000) < 4) {
            int chestX = chunkRand.nextInt(16);
            int chestZ = chunkRand.nextInt(16);
            if (overlapsRoom) {
                chunkData.setBlock(chestX, floorY + 1, chestZ, Material.CHEST);
            }
        }
    }

    @Override
    public boolean shouldGenerateStructures() {
        return true;
    }

    @Override
    public @NotNull List<BlockPopulator> getDefaultPopulators(@NotNull World world) {
        return Collections.emptyList();
    }

    @Override
    public void generateSurface(@NotNull WorldInfo worldInfo, @NotNull Random random, int chunkX, int chunkZ, @NotNull ChunkData chunkData) {
    }

    @Override
    public void generateBedrock(@NotNull WorldInfo worldInfo, @NotNull Random random, int chunkX, int chunkZ, @NotNull ChunkData chunkData) {
    }

    @Override
    public void generateCaves(@NotNull WorldInfo worldInfo, @NotNull Random random, int chunkX, int chunkZ, @NotNull ChunkData chunkData) {
    }

    @Override
    public int getBaseHeight(@NotNull WorldInfo worldInfo, @NotNull Random random, int x, int z, @NotNull org.bukkit.HeightMap heightMap) {
        return worldInfo.getMinHeight() + 10;
    }

    @Override
    public @NotNull BiomeProvider getDefaultBiomeProvider(@NotNull WorldInfo worldInfo) {
        return biomeProvider;
    }

    @Override
    public boolean shouldGenerateNoise() { return false; }
    @Override
    public boolean shouldGenerateSurface() { return false; }
    @Override
    public boolean shouldGenerateBedrock() { return false; }
    @Override
    public boolean shouldGenerateCaves() { return false; }
    @Override
    public boolean shouldGenerateDecorations() { return false; }
    @Override
    public boolean shouldGenerateMobs() { return config.isMobSpawning(); }

    @Override
    public @NotNull Location getFixedSpawnLocation(@NotNull World world, @NotNull Random random) {
        return new Location(world, 8.5, world.getMinHeight() + 4, 8.5);
    }

    public BackroomsConfig getConfig() {
        return config;
    }
}

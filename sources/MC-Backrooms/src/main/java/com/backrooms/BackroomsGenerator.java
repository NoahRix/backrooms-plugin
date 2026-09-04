package com.backrooms;

import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.LimitedRegion;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Random;

public class BackroomsGenerator extends ChunkGenerator {

    @Override
    public void generateNoise(@NotNull WorldInfo worldInfo, @NotNull Random random, int chunkX, int chunkZ, @NotNull ChunkData chunkData) {
        long seed = worldInfo.getSeed();
        int minY = worldInfo.getMinHeight();
        
        boolean isSpawnChunk = (chunkX == 0 && chunkZ == 0);

        long chunkSeed = (long) chunkX * 341873128712L + (long) chunkZ * 132897987541L ^ seed;
        Random chunkRand = new Random(chunkSeed);

        // 1. Strict Void Safety Rule
        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                chunkData.setBlock(localX, minY, localZ, Material.BEDROCK);
                chunkData.setBlock(localX, minY + 1, localZ, Material.LIGHT_GRAY_CONCRETE);
            }
        }

        int chunkRoll = chunkRand.nextInt(100);
        boolean isOreChunk = !isSpawnChunk && (chunkRoll < 5);
        boolean isTreeChunk = !isSpawnChunk && (!isOreChunk && chunkRoll < 8);
        boolean isMineshaftDrop = !isSpawnChunk && (!isOreChunk && !isTreeChunk && chunkRoll < 13);

        int currentY = minY + 2;
        int maxGenerationHeight = minY + 60;

        while (currentY < maxGenerationHeight) {
            int roomHeight = 3 + chunkRand.nextInt(5);
            int floorY = currentY;
            int ceilingY = floorY + roomHeight + 1;

            int interiorLayoutRoll = chunkRand.nextInt(100);

            for (int localX = 0; localX < 16; localX++) {
                for (int localZ = 0; localZ < 16; localZ++) {

                    chunkData.setBlock(localX, floorY, localZ, Material.LIGHT_GRAY_CONCRETE);
                    chunkData.setBlock(localX, ceilingY, localZ, Material.LIGHT_GRAY_CONCRETE);

                    if ((localX == 4 || localX == 12) && (localZ == 4 || localZ == 12)) {
                        chunkData.setBlock(localX, ceilingY, localZ, Material.PEARLESCENT_FROGLIGHT);
                    }

                    boolean isWall = (localX == 0 || localX == 15 || localZ == 0 || localZ == 15);

                    if (!isWall && !isSpawnChunk) {
                        if (interiorLayoutRoll < 30 && localX == 8 && localZ > 2 && localZ < 13) {
                            isWall = true;
                        } else if (interiorLayoutRoll >= 30 && interiorLayoutRoll < 60 && localZ == 8 && localX > 2 && localX < 13) {
                            isWall = true;
                        } else if (interiorLayoutRoll >= 60 && interiorLayoutRoll < 75 && localX == localZ && localX > 3 && localX < 12) {
                            isWall = true;
                        }
                    }

                    for (int blockY = floorY + 1; blockY < ceilingY; blockY++) {
                        if (isMineshaftDrop && localX > 5 && localX < 10 && localZ > 5 && localZ < 10 && blockY < (floorY + 16)) {
                            chunkData.setBlock(localX, blockY, localZ, Material.AIR);
                            continue;
                        }

                        if (isSpawnChunk) {
                            if (isWall) {
                                chunkData.setBlock(localX, blockY, localZ, Material.YELLOW_TERRACOTTA);
                            } else {
                                chunkData.setBlock(localX, blockY, localZ, Material.AIR);
                            }
                        } else if (isWall) {
                            chunkData.setBlock(localX, blockY, localZ, Material.YELLOW_TERRACOTTA);
                        } else if (isOreChunk && localX > 2 && localX < 13 && localZ > 2 && localZ < 13) {
                            int coreRoll = chunkRand.nextInt(100);
                            if (coreRoll < 25) {
                                chunkData.setBlock(localX, blockY, localZ, Material.COBBLESTONE);
                            } else if (coreRoll < 50) {
                                chunkData.setBlock(localX, blockY, localZ, Material.MOSSY_COBBLESTONE);
                            } else if (coreRoll < 70) {
                                chunkData.setBlock(localX, blockY, localZ, Material.DIRT);
                            } else if (coreRoll < 85) {
                                chunkData.setBlock(localX, blockY, localZ, Material.STONE);
                            } else if (coreRoll < 92) {
                                chunkData.setBlock(localX, blockY, localZ, Material.IRON_ORE);
                            } else if (coreRoll < 98) {
                                chunkData.setBlock(localX, blockY, localZ, Material.GOLD_ORE);
                            } else {
                                chunkData.setBlock(localX, blockY, localZ, Material.DIAMOND_ORE);
                            }
                        } else if (isTreeChunk && localX == 8 && localZ == 8) {
                            if (blockY == floorY + 1) {
                                chunkData.setBlock(localX, blockY, localZ, Material.DIRT);
                            } else if (blockY < ceilingY - 1) {
                                chunkData.setBlock(localX, blockY, localZ, Material.OAK_LOG);
                            } else {
                                chunkData.setBlock(localX, blockY, localZ, Material.OAK_LEAVES);
                            }
                        } else {
                            if (blockY == floorY + 1) {
                                int featureRoll = chunkRand.nextInt(1000);
                                if (featureRoll < 4) {
                                    chunkData.setBlock(localX, blockY, localZ, Material.CHEST);
                                } else if (featureRoll < 8) {
                                    chunkData.setBlock(localX, blockY, localZ, Material.SMOKER);
                                } else if (featureRoll == 12 && localX == 1) {
                                    chunkData.setBlock(localX, blockY + 1, localZ, Material.OAK_WALL_SIGN);
                                    org.bukkit.block.data.BlockData blockData = Material.OAK_WALL_SIGN.createBlockData();
                                    if (blockData instanceof Directional) {
                                        ((Directional) blockData).setFacing(BlockFace.EAST);
                                        chunkData.setBlock(localX, blockY + 1, localZ, blockData);
                                    }
                                }
                            } else {
                                chunkData.setBlock(localX, blockY, localZ, Material.AIR);
                            }
                        }
                    }
                }
            }
            currentY = ceilingY;
        }
    }

    // Tell the server's structure engine that it is allowed to plan vanilla structures here
    @Override
    public boolean shouldGenerateStructures() {
        return true;
    }

    // Inject our Structure Texturizer Populator to sweep the chunk after vanilla elements drop in
    @Override
    public @NotNull List<BlockPopulator> getDefaultPopulators(@NotNull org.bukkit.World world) {
        return Collections.singletonList(new BlockPopulator() {
            @Override
            public void populate(@NotNull WorldInfo worldInfo, @NotNull Random random, int chunkX, int chunkZ, @NotNull LimitedRegion region) {
                int minY = worldInfo.getMinHeight();
                int maxY = worldInfo.getMaxHeight();

                // Run a scan through the chunk's boundaries to catch structural blocks spawned by Minecraft
                for (int x = chunkX * 16; x < (chunkX * 16) + 16; x++) {
                    for (int z = chunkZ * 16; z < (chunkZ * 16) + 16; z++) {
                        for (int y = minY + 2; y < maxY - 10; y++) {
                            if (!region.isInRegion(x, y, z)) continue;

                            Material mat = region.getType(x, y, z);

                            // Swallowing filter: Turn structural materials into the internal hotel texture
                            if (mat == Material.COBBLESTONE || mat == Material.MOSSY_COBBLESTONE || 
                                mat == Material.STONE_BRICKS || mat == Material.MOSSY_STONE_BRICKS ||
                                mat == Material.OAK_PLANKS || mat == Material.DARK_OAK_PLANKS ||
                                mat == Material.SMOOTH_SANDSTONE || mat == Material.CHISELED_SANDSTONE ||
                                mat == Material.NETHER_BRICKS || mat == Material.CRACKED_STONE_BRICKS) {
                                
                                region.setType(x, y, z, Material.YELLOW_TERRACOTTA);
                            } 
                            else if (mat == Material.OAK_LOG || mat == Material.STRIPPED_OAK_LOG || 
                                     mat == Material.DARK_OAK_LOG || mat == Material.DIRT_PATH || 
                                     mat == Material.GRASS_BLOCK || mat == Material.SAND) {
                                
                                region.setType(x, y, z, Material.LIGHT_GRAY_CONCRETE);
                            }
                        }
                    }
                }
            }
        });
    }

    @Override
    public void generateSurface(@NotNull WorldInfo worldInfo, @NotNull Random random, int x, int z, @NotNull ChunkData chunkData) {}

    @Override
    public void generateBedrock(@NotNull WorldInfo worldInfo, @NotNull Random random, int x, int z, @NotNull ChunkData chunkData) {}
}
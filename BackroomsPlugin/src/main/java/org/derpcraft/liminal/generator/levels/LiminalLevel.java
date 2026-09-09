package org.derpcraft.liminal.generator.levels;

import org.derpcraft.liminal.config.LevelConfig;
import org.bukkit.Material;
import org.bukkit.generator.ChunkGenerator;

import java.util.Random;

/**
 * Abstract base class for all Liminal levels.
 *
 * <p>Each Liminal level occupies a vertical slice of the world defined by a Y-range
 * in the {@link LevelConfig}. This class contains the shared chunk generation logic
 * (floor/ceiling placement, room layout, wall generation, doorway carving, lighting,
 * and loot chest placement). Concrete subclasses can override individual methods to
 * customise behaviour for their specific liminal-space aesthetic.</p>
 *
 * <h2>Generation pipeline</h2>
 * <ol>
 *   <li>{@link #generateFloorAndCeiling} &ndash; lay the floor slab and ceiling slab</li>
 *   <li>{@link #calculateRoomLayout} &ndash; deterministic room grid from the seed</li>
 *   <li>{@link #generateWallsAndDoorways} &ndash; walls with doorway cut-outs</li>
 *   <li>{@link #generateLighting} &ndash; ceiling-mounted light blocks</li>
 *   <li>{@link #generateLootChest} &ndash; rare chest placement</li>
 *   <li>{@link #generateSpecialFeatures} &ndash; level-specific decoration hook</li>
 * </ol>
 *
 * @see Level0Lobby
 * @see Level1HabitableZone
 * @see Level2PipeDreams
 */
public abstract class LiminalLevel {

    /** Configuration data for this level (Y range, materials, room sizes, etc.). */
    protected final LevelConfig config;

    /** Global generation seed, shared across all levels for deterministic output. */
    protected final long seed;

    /**
     * Constructs a new level backed by the given configuration.
     *
     * @param config the {@link LevelConfig} holding material choices, Y bounds, room dimensions
     * @param seed   the world generation seed used for deterministic randomness
     */
    protected LiminalLevel(LevelConfig config, long seed) {
        this.config = config;
        this.seed = seed;
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Returns the {@link LevelConfig} that describes this level's parameters.
     *
     * @return the level configuration, never {@code null}
     */
    public LevelConfig getConfig() {
        return config;
    }

    /**
     * Generates all blocks for this level within a single chunk.
     *
     * <p>This is the main entry point called by {@code LiminalChunkGenerator}. It
     * orchestrates the full generation pipeline in the correct order.</p>
     *
     * @param chunkData    the mutable chunk data to write blocks into
     * @param chunkStartX  the world X coordinate of the chunk's western edge
     * @param chunkStartZ  the world Z coordinate of the chunk's northern edge
     * @param chunkEndX    the world X coordinate just past the chunk's eastern edge ({@code chunkStartX + 16})
     * @param chunkEndZ    the world Z coordinate just past the chunk's southern edge ({@code chunkStartZ + 16})
     * @param worldMinY    the minimum Y coordinate of the world
     * @param worldMaxY    the maximum Y coordinate of the world
     */
    public void generate(ChunkGenerator.ChunkData chunkData,
                         int chunkStartX, int chunkStartZ,
                         int chunkEndX, int chunkEndZ,
                         int worldMinY, int worldMaxY) {

        int effectiveMinY = Math.max(config.getMinY(), worldMinY);
        int effectiveMaxY = Math.min(config.getMaxY(), worldMaxY);

        int floorY = effectiveMinY + getFloorOffset();
        int ceilingY = floorY + config.getCeilingHeight();

        if (ceilingY > effectiveMaxY) {
            ceilingY = effectiveMaxY;
        }
        if (floorY >= ceilingY) {
            return;
        }

        generateFloorAndCeiling(chunkData, floorY, ceilingY);

        RoomLayout layout = calculateRoomLayout(chunkStartX, chunkStartZ, chunkEndX, chunkEndZ);

        generateWallsAndDoorways(chunkData, layout, floorY, ceilingY, chunkStartX, chunkStartZ);

        generateLighting(chunkData, layout, ceilingY, chunkStartX, chunkStartZ);

        generateLootChest(chunkData, layout, floorY, chunkStartX, chunkStartZ);

        generateSpecialFeatures(chunkData, layout, floorY, ceilingY, chunkStartX, chunkStartZ);
    }

    // -------------------------------------------------------------------------
    // Generation steps (can be overridden by subclasses)
    // -------------------------------------------------------------------------

    /**
     * Returns the vertical offset from the level's minimum Y to the actual floor surface.
     *
     * <p>The default is 2, leaving a 2-block-thick sub-floor space (useful for
     * hidden pipes, wiring, or crawlspaces). Subclasses can override this to
     * change the sub-floor depth.</p>
     *
     * @return the floor offset in blocks (default 2)
     */
    protected int getFloorOffset() {
        return 2;
    }

    /**
     * Returns the world Y coordinate of this level's ground floor surface
     * (the block players walk on for the bottom-most floor).
     *
     * @return {@code config.getMinY() + getFloorOffset()}
     */
    public int getFloorSurfaceY() {
        return config.getMinY() + getFloorOffset();
    }

    /**
     * Lays the floor slab and ceiling slab across the entire chunk column for this level.
     *
     * @param chunkData the mutable chunk data
     * @param floorY    the Y coordinate of the floor surface
     * @param ceilingY  the Y coordinate of the ceiling surface
     */
    protected void generateFloorAndCeiling(ChunkGenerator.ChunkData chunkData, int floorY, int ceilingY) {
        Material floorMat = config.getFloorMaterial();
        Material ceilingMat = config.getCeilingMaterial();

        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                chunkData.setBlock(localX, floorY, localZ, floorMat);
                chunkData.setBlock(localX, ceilingY, localZ, ceilingMat);
            }
        }
    }

    /**
     * Calculates the room layout for this chunk using a deterministic grid-based approach.
     *
     * <p>The world is divided into a grid whose cell size equals the level's maximum room
     * dimensions. Each cell is seeded to produce a room of random size within the configured
     * min/max range. The method then checks whether the current chunk overlaps the generated
     * room and computes doorway positions if it does.</p>
     *
     * @param chunkStartX world X of the chunk's western edge
     * @param chunkStartZ world Z of the chunk's northern edge
     * @param chunkEndX   world X just past the chunk's eastern edge
     * @param chunkEndZ   world Z just past the chunk's southern edge
     * @return a {@link RoomLayout} describing the room and its relationship to this chunk
     */
    protected RoomLayout calculateRoomLayout(int chunkStartX, int chunkStartZ,
                                             int chunkEndX, int chunkEndZ) {
        return calculateRoomLayout(chunkStartX, chunkStartZ, chunkEndX, chunkEndZ, 0);
    }

    /**
     * Calculates the room layout for this chunk on a specific floor of a multi-floor level.
     *
     * <p>The floor index is mixed into the room seed so stacked floors of the same level
     * get independent room layouts (otherwise every floor would have perfectly aligned
     * walls). The result stays deterministic: chunks within the same grid cell on the same
     * floor always compute the identical room.</p>
     *
     * @param chunkStartX world X of the chunk's western edge
     * @param chunkStartZ world Z of the chunk's northern edge
     * @param chunkEndX   world X just past the chunk's eastern edge
     * @param chunkEndZ   world Z just past the chunk's southern edge
     * @param floorIndex  the floor being generated (0 = bottom); single-floor levels pass 0
     * @return a {@link RoomLayout} describing the room and its relationship to this chunk
     */
    protected RoomLayout calculateRoomLayout(int chunkStartX, int chunkStartZ,
                                             int chunkEndX, int chunkEndZ, int floorIndex) {
        int roomMaxWidth = config.getRoomMaxWidth();
        int roomMaxLength = config.getRoomMaxLength();

        int gridX = Math.floorDiv(chunkStartX, roomMaxWidth);
        int gridZ = Math.floorDiv(chunkStartZ, roomMaxLength);

        long roomSeed = seed ^ ((long) gridX * 0x4f4f4f4fL) ^ ((long) gridZ * 0x2f2f2f2fL)
                      ^ config.getIdHashCode() ^ (floorIndex * 0x5f356495L);
        Random roomRand = new Random(roomSeed);

        int roomWidth = config.getRoomMinWidth() + roomRand.nextInt(roomMaxWidth - config.getRoomMinWidth() + 1);
        int roomLength = config.getRoomMinLength() + roomRand.nextInt(roomMaxLength - config.getRoomMinLength() + 1);

        int roomStartX = gridX * roomMaxWidth;
        int roomStartZ = gridZ * roomMaxLength;
        int roomEndX = roomStartX + roomWidth;
        int roomEndZ = roomStartZ + roomLength;

        boolean overlaps = chunkEndX > roomStartX && chunkStartX < roomEndX
                        && chunkEndZ > roomStartZ && chunkStartZ < roomEndZ;

        int doorwayPos = Math.max(roomWidth, roomLength) / 2;

        return new RoomLayout(
                roomStartX, roomStartZ, roomEndX, roomEndZ,
                roomWidth, roomLength, doorwayPos, overlaps
        );
    }

    /**
     * Generates the walls around a room and carves doorway openings.
     *
     * <p>Walls are placed on the four edges of the room rectangle. Each wall has a single
     * doorway opening centred on that edge. The doorway is 3 blocks tall (floor+1 to floor+3)
     * to allow players to walk through; the upper portion of the doorway is filled with the
     * wall material to maintain the enclosed feeling.</p>
     *
     * @param chunkData    the mutable chunk data
     * @param layout       the computed room layout for this chunk
     * @param floorY       the Y coordinate of the floor surface
     * @param ceilingY     the Y coordinate of the ceiling surface
     * @param chunkStartX  world X of the chunk's western edge
     * @param chunkStartZ  world Z of the chunk's northern edge
     */
    protected void generateWallsAndDoorways(ChunkGenerator.ChunkData chunkData,
                                            RoomLayout layout,
                                            int floorY, int ceilingY,
                                            int chunkStartX, int chunkStartZ) {
        if (!layout.overlapsRoom()) {
            return;
        }

        Material wallMat = config.getWallMaterial();
        int doorwayHeight = getDoorwayHeight();

        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                int globalX = chunkStartX + localX;
                int globalZ = chunkStartZ + localZ;

                boolean isWall = false;
                boolean isDoorway = false;

                if (globalX == layout.roomStartX() || globalX == layout.roomEndX() - 1 ||
                    globalZ == layout.roomStartZ() || globalZ == layout.roomEndZ() - 1) {
                    isWall = true;

                    if (globalX == layout.roomStartX() && globalZ == layout.roomStartZ() + layout.doorwayPos()) {
                        isDoorway = true;
                    } else if (globalX == layout.roomEndX() - 1 && globalZ == layout.roomStartZ() + layout.doorwayPos()) {
                        isDoorway = true;
                    } else if (globalZ == layout.roomStartZ() && globalX == layout.roomStartX() + layout.doorwayPos()) {
                        isDoorway = true;
                    } else if (globalZ == layout.roomEndZ() - 1 && globalX == layout.roomStartX() + layout.doorwayPos()) {
                        isDoorway = true;
                    }
                }

                for (int blockY = floorY + 1; blockY < ceilingY; blockY++) {
                    if (isDoorway) {
                        if (blockY <= floorY + doorwayHeight) {
                            chunkData.setBlock(localX, blockY, localZ, Material.AIR);
                        } else {
                            chunkData.setBlock(localX, blockY, localZ, wallMat);
                        }
                    } else if (isWall) {
                        chunkData.setBlock(localX, blockY, localZ, wallMat);
                    } else {
                        chunkData.setBlock(localX, blockY, localZ, Material.AIR);
                    }
                }
            }
        }
    }

    /**
     * Returns the height of doorway openings in blocks (measured from the floor surface).
     *
     * <p>The default is 2, giving a doorway from {@code floorY+1} to {@code floorY+2}
     * (player-height). Subclasses can override to make doorways taller or shorter.</p>
     *
     * @return doorway height in blocks (default 2)
     */
    protected int getDoorwayHeight() {
        return 2;
    }

    /**
     * Places light-emitting blocks on the ceiling at regular intervals.
     *
     * <p>Lights are placed on the ceiling slab at positions where both the global X and Z
     * coordinates are divisible by the level's configured light spacing. Lights are only
     * placed inside rooms (not in corridors).</p>
     *
     * @param chunkData    the mutable chunk data
     * @param layout       the computed room layout for this chunk
     * @param ceilingY     the Y coordinate of the ceiling surface
     * @param chunkStartX  world X of the chunk's western edge
     * @param chunkStartZ  world Z of the chunk's northern edge
     */
    protected void generateLighting(ChunkGenerator.ChunkData chunkData,
                                    RoomLayout layout,
                                    int ceilingY,
                                    int chunkStartX, int chunkStartZ) {
        if (!layout.overlapsRoom()) {
            return;
        }

        int spacing = config.getLightSpacing();
        Material lightMat = config.getLightMaterial();

        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                int globalX = chunkStartX + localX;
                int globalZ = chunkStartZ + localZ;

                if (globalX % spacing == 0 && globalZ % spacing == 0) {
                    chunkData.setBlock(localX, ceilingY, localZ, lightMat);
                }
            }
        }
    }

    /**
     * Places a loot chest at a rare, deterministic position within the room.
     *
     * <p>There is a 0.4% chance per chunk of a chest being placed. The position is
     * derived from a seeded random to remain deterministic across server restarts.</p>
     *
     * @param chunkData    the mutable chunk data
     * @param layout       the computed room layout for this chunk
     * @param floorY       the Y coordinate of the floor surface
     * @param chunkStartX  world X of the chunk's western edge
     * @param chunkStartZ  world Z of the chunk's northern edge
     */
    protected void generateLootChest(ChunkGenerator.ChunkData chunkData,
                                     RoomLayout layout,
                                     int floorY,
                                     int chunkStartX, int chunkStartZ) {
        if (!layout.overlapsRoom()) {
            return;
        }

        // floorY is mixed in so multi-floor levels don't place chests at the
        // same relative position on every floor.
        Random chunkRand = new Random(
                (long) chunkStartX * 341873128712L + (long) chunkStartZ * 132897987541L
                ^ seed ^ config.getIdHashCode() ^ (floorY * 0x2545f491L)
        );

        if (chunkRand.nextInt(1000) < 4) {
            int chestX = chunkRand.nextInt(16);
            int chestZ = chunkRand.nextInt(16);
            chunkData.setBlock(chestX, floorY + 1, chestZ, Material.CHEST);
        }
    }

    /**
     * Hook for level-specific decoration and features.
     *
     * <p>Called after the core generation pipeline (floor, walls, lighting, chests).
     * Subclasses can override this to add unique elements such as flooded floors,
     * boiler pipes, special room types, or ambient decoration.</p>
     *
     * <p>The default implementation does nothing.</p>
     *
     * @param chunkData    the mutable chunk data
     * @param layout       the computed room layout for this chunk
     * @param floorY       the Y coordinate of the floor surface
     * @param ceilingY     the Y coordinate of the ceiling surface
     * @param chunkStartX  world X of the chunk's western edge
     * @param chunkStartZ  world Z of the chunk's northern edge
     */
    protected void generateSpecialFeatures(ChunkGenerator.ChunkData chunkData,
                                           RoomLayout layout,
                                           int floorY, int ceilingY,
                                           int chunkStartX, int chunkStartZ) {
    }

    // -------------------------------------------------------------------------
    // Room layout record
    // -------------------------------------------------------------------------

    /**
     * Immutable record describing the room that overlaps (or doesn't) a given chunk.
     *
     * <p>Computed by {@link #calculateRoomLayout} and passed through the generation
     * pipeline so each step has access to the same room geometry without recomputation.</p>
     *
     * @param roomStartX  world X of the room's western edge
     * @param roomStartZ  world Z of the room's northern edge
     * @param roomEndX    world X just past the room's eastern edge
     * @param roomEndZ    world Z just past the room's southern edge
     * @param roomWidth   the room's width in blocks (X axis)
     * @param roomLength  the room's length in blocks (Z axis)
     * @param doorwayPos  the offset from the room corner to the centre of each doorway
     * @param overlaps    whether this room overlaps the current chunk
     */
    public record RoomLayout(
            int roomStartX, int roomStartZ,
            int roomEndX, int roomEndZ,
            int roomWidth, int roomLength,
            int doorwayPos,
            boolean overlapsRoom
    ) {
    }
}

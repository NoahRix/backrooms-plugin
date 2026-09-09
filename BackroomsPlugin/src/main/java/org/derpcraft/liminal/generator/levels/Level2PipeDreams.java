package org.derpcraft.liminal.generator.levels;

import org.derpcraft.liminal.config.LevelConfig;
import org.bukkit.Material;
import org.bukkit.generator.ChunkGenerator;

import java.util.Random;

/**
 * <b>Level 2 &ndash; Pipe Dreams</b>: the deepest generated level, evoking underground
 * maintenance tunnels and industrial pipe networks.
 *
 * <p>Far below the Lobby and Habitable Zone, this level feels raw and utilitarian:</p>
 * <ul>
 *   <li>Medium-sized rooms (8&ndash;28 blocks) with deepslate-brick walls</li>
 *   <li>Deepslate floors resembling rough-cut stone tunnel floors</li>
 *   <li>Redstone-lamp ceiling lights that give a dim, utilitarian glow</li>
 *   <li>Narrow corridors (2 blocks) reinforcing the tunnel aesthetic</li>
 * </ul>
 *
 * <h2>Special features</h2>
 * <p>Level 2 adds <b>iron-bar "pipe" columns</b> along room walls and <b>lava seeps</b>
 * on the floor. Bars are placed vertically against walls to simulate exposed pipe runs,
 * and rare lava blocks on the floor create hazardous glowing patches.</p>
 *
 * <h2>Configuration defaults (from {@code config.yml})</h2>
 * <table>
 *   <tr><td>Y range</td><td>64 to 128</td></tr>
 *   <tr><td>Wall</td><td>DEEPSLATE_BRICKS</td></tr>
 *   <tr><td>Floor</td><td>DEEPSLATE</td></tr>
 *   <tr><td>Ceiling</td><td>SMOOTH_STONE</td></tr>
 *   <tr><td>Light</td><td>REDSTONE_LAMP</td></tr>
 *   <tr><td>Ceiling height</td><td>5 blocks</td></tr>
 * </table>
 *
 * @see LiminalLevel
 * @see Level0Lobby
 * @see Level1HabitableZone
 */
public class Level2PipeDreams extends LiminalLevel {

    /**
     * Probability (0.0&ndash;1.0) that a pipe column (iron bars) is placed along a wall
     * inside a room.
     */
    private static final double PIPE_CHANCE = 0.03;

    /**
     * Probability (0.0&ndash;1.0) that a lava seep appears on the floor inside a room.
     * Kept very low to create rare but dangerous hazards.
     */
    private static final double LAVA_SEEP_CHANCE = 0.003;

    /**
     * Maximum height (in blocks) of an iron-bar pipe column along a wall.
     */
    private static final int MAX_PIPE_HEIGHT = 4;

    /**
     * Constructs the Level 2 &ndash; Pipe Dreams generator.
     *
     * @param config the level configuration (materials, room sizes, Y bounds)
     * @param seed   the world generation seed for deterministic output
     */
    public Level2PipeDreams(LevelConfig config, long seed) {
        super(config, seed);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Pipe Dreams uses a 1-block sub-floor so its walking surface aligns with the
     * other levels — with the ripple layout, adjacent rings must share the same floor
     * height for seamless transitions. Decorative pipe networks run along walls and
     * ceilings instead of a sub-floor gap.</p>
     */
    @Override
    protected int getFloorOffset() {
        return 1;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Doorways in Pipe Dreams are 2 blocks tall, maintaining the cramped tunnel
     * feel despite the 5-block ceiling height.</p>
     */
    @Override
    protected int getDoorwayHeight() {
        return 2;
    }

    /**
     * Adds iron-bar pipe columns along walls and rare lava seeps on the floor.
     *
     * <p><b>Pipe columns</b> are vertical stacks of iron bars placed against room walls,
     * simulating exposed industrial piping. They are only placed on wall-adjacent blocks
     * inside rooms. <b>Lava seeps</b> are rare lava blocks on the floor that create
     * glowing hazardous patches, reinforcing the danger of going deeper.</p>
     *
     * @param chunkData    the mutable chunk data
     * @param layout       the computed room layout for this chunk
     * @param floorY       the Y coordinate of the floor surface
     * @param ceilingY     the Y coordinate of the ceiling surface
     * @param chunkStartX  world X of the chunk's western edge
     * @param chunkStartZ  world Z of the chunk's northern edge
     */
    @Override
    protected void generateSpecialFeatures(ChunkGenerator.ChunkData chunkData,
                                           RoomLayout layout,
                                           int floorY, int ceilingY,
                                           int chunkStartX, int chunkStartZ) {
        if (!layout.overlapsRoom()) {
            return;
        }

        Random featureRand = new Random(
                seed ^ ((long) chunkStartX * 0x9e3779b9L) ^ ((long) chunkStartZ * 0x517cc1b7L) ^ config.getIdHashCode()
        );

        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                int globalX = chunkStartX + localX;
                int globalZ = chunkStartZ + localZ;

                boolean insideRoom = globalX > layout.roomStartX() && globalX < layout.roomEndX() - 1
                                  && globalZ > layout.roomStartZ() && globalZ < layout.roomEndZ() - 1;

                if (!insideRoom) {
                    continue;
                }

                boolean nearWall = globalX == layout.roomStartX() + 1 || globalX == layout.roomEndX() - 2
                                || globalZ == layout.roomStartZ() + 1 || globalZ == layout.roomEndZ() - 2;

                if (nearWall && featureRand.nextDouble() < PIPE_CHANCE) {
                    int pipeHeight = 2 + featureRand.nextInt(MAX_PIPE_HEIGHT - 1);
                    for (int i = 0; i < pipeHeight; i++) {
                        int pipeY = floorY + 1 + i;
                        if (pipeY < ceilingY) {
                            chunkData.setBlock(localX, pipeY, localZ, Material.IRON_BARS);
                        }
                    }
                }

                if (featureRand.nextDouble() < LAVA_SEEP_CHANCE) {
                    chunkData.setBlock(localX, floorY + 1, localZ, Material.LAVA);
                }
            }
        }
    }
}

package org.derpcraft.liminal.generator.levels;

import org.derpcraft.liminal.config.LevelConfig;
import org.bukkit.Material;
import org.bukkit.generator.ChunkGenerator;

import java.util.Random;

/**
 * <b>Level 1 &ndash; Habitable Zone</b>: a darker, more industrial liminal space.
 *
 * <p>Descending past the Lobby, the environment shifts to a harsher aesthetic:</p>
 * <ul>
 *   <li>Larger rooms (10&ndash;32 blocks) with polished-blackstone walls</li>
 *   <li>Dark polished-blackstone floors giving a warehouse or bunker feel</li>
 *   <li>Soul-lantern ceiling lights casting an eerie blue-green glow</li>
 *   <li>Wider corridors (3 blocks) allowing more open sight-lines</li>
 * </ul>
 *
 * <h2>Special features</h2>
 * <p>Level 1 adds <b>chain decorations</b> hanging from the ceiling and <b>water puddles</b>
 * on the floor. Chains are placed at low probability from the ceiling downward, and
 * puddles are shallow water blocks scattered across the floor inside rooms.</p>
 *
 * <h2>Configuration defaults (from {@code config.yml})</h2>
 * <table>
 *   <tr><td>Y range</td><td>0 to 64</td></tr>
 *   <tr><td>Wall</td><td>POLISHED_BLACKSTONE_BRICKS</td></tr>
 *   <tr><td>Floor</td><td>POLISHED_BLACKSTONE</td></tr>
 *   <tr><td>Ceiling</td><td>SMOOTH_STONE</td></tr>
 *   <tr><td>Light</td><td>SOUL_LANTERN</td></tr>
 *   <tr><td>Ceiling height</td><td>6 blocks</td></tr>
 * </table>
 *
 * @see LiminalLevel
 * @see Level0Lobby
 * @see Level2PipeDreams
 */
public class Level1HabitableZone extends LiminalLevel {

    /**
     * Probability (0.0&ndash;1.0) that a chain hangs from the ceiling at any given
     * light-grid position.
     */
    private static final double CHAIN_CHANCE = 0.15;

    /**
     * Probability (0.0&ndash;1.0) that a water puddle appears on the floor inside a room.
     */
    private static final double PUDDLE_CHANCE = 0.008;

    /**
     * Maximum length (in blocks) of a hanging chain decoration.
     */
    private static final int MAX_CHAIN_LENGTH = 3;

    /**
     * Constructs the Level 1 &ndash; Habitable Zone generator.
     *
     * @param config the level configuration (materials, room sizes, Y bounds)
     * @param seed   the world generation seed for deterministic output
     */
    public Level1HabitableZone(LevelConfig config, long seed) {
        super(config, seed);
    }

    /**
     * {@inheritDoc}
     *
     * <p>The Habitable Zone uses a 1-block sub-floor so its walking surface aligns
     * with the other levels — with the ripple layout, adjacent rings must share the
     * same floor height for seamless transitions.</p>
     */
    @Override
    protected int getFloorOffset() {
        return 1;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Doorways in the Habitable Zone are 3 blocks tall, taking advantage of the
     * taller 6-block ceiling to create more imposing entrances.</p>
     */
    @Override
    protected int getDoorwayHeight() {
        return 3;
    }

    /**
     * Adds hanging chains from the ceiling and water puddles on the floor.
     *
     * <p><b>Chains</b> are placed at light-grid intersections (where lights would go)
     * when no light is present, hanging downward from the ceiling. <b>Puddles</b> are
     * shallow water blocks scattered across the floor inside rooms, giving a damp,
     * neglected warehouse feel.</p>
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

                if (featureRand.nextDouble() < CHAIN_CHANCE) {
                    int chainLength = 1 + featureRand.nextInt(MAX_CHAIN_LENGTH);
                    for (int i = 0; i < chainLength; i++) {
                        int chainY = ceilingY - 1 - i;
                        if (chainY > floorY + 1) {
                            chunkData.setBlock(localX, chainY, localZ, Material.CHAIN);
                        }
                    }
                }

                if (featureRand.nextDouble() < PUDDLE_CHANCE) {
                    chunkData.setBlock(localX, floorY + 1, localZ, Material.WATER);
                }
            }
        }
    }
}

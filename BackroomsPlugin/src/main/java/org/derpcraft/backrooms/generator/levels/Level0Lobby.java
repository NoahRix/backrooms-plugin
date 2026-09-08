package org.derpcraft.backrooms.generator.levels;

import org.derpcraft.backrooms.config.LevelConfig;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;
import org.derpcraft.backrooms.BackroomsPlugin;
import org.derpcraft.backrooms.effects.FlickerManager;

import java.util.Random;

/**
 * <b>Level 0 &ndash; The Lobby</b>: the iconic yellow-wallpapered office liminal space.
 *
 * <p>This is the entry level of the Backrooms and the most recognisable. It features:</p>
 * <ul>
 *   <li>Tight, maze-like rooms with birch-plank walls</li>
 *   <li>Light-gray concrete floors reminiscent of worn office carpet</li>
 *   <li>Sea-lantern ceiling lights simulating buzzing fluorescent panels</li>
 *   <li>Small rooms (8&ndash;24 blocks) creating a claustrophobic atmosphere</li>
 * </ul>
 *
 * <h2>Special features</h2>
 * <p>Level 0 adds scattered <b>carpet patches</b> on the floor to enhance the office
 * aesthetic. These are placed deterministically using the chunk seed so they remain
 * consistent across server restarts.</p>
 *
 * <h2>Configuration defaults (from {@code config.yml})</h2>
 * <table>
 *   <tr><td>Y range</td><td>-64 to 0</td></tr>
 *   <tr><td>Wall</td><td>BIRCH_PLANKS</td></tr>
 *   <tr><td>Floor</td><td>LIGHT_GRAY_CONCRETE</td></tr>
 *   <tr><td>Ceiling</td><td>SMOOTH_STONE</td></tr>
 *   <tr><td>Light</td><td>SEA_LANTERN</td></tr>
 *   <tr><td>Ceiling height</td><td>4 blocks</td></tr>
 * </table>
 *
 * @see BackroomsLevel
 * @see Level1HabitableZone
 * @see Level2PipeDreams
 */
public class Level0Lobby extends BackroomsLevel {

    /**
     * Probability (0.0&ndash;1.0) that a carpet patch is placed on any given floor block
     * inside a room. Kept low so carpets feel like scattered remnants rather than wall-to-wall.
     */
    private static final double CARPET_CHANCE = 0.02;

    /**
     * Material used for the scattered carpet patches on the lobby floor.
     * Yellow wool evokes the classic yellow-carpet look of the original Backrooms.
     */
    private static final Material CARPET_MATERIAL = Material.YELLOW_CARPET;

    /**
     * Probability (0.0&ndash;1.0) that a sea lantern will flicker.
     * 10% of lanterns will have the flicker effect.
     */
    private static final double FLICKER_CHANCE = 0.10;

    /**
     * Constructs the Level 0 &ndash; Lobby generator.
     *
     * @param config the level configuration (materials, room sizes, Y bounds)
     * @param seed   the world generation seed for deterministic output
     */
    public Level0Lobby(LevelConfig config, long seed) {
        super(config, seed);
    }

    /**
     * {@inheritDoc}
     *
     * <p>The Lobby uses the default floor offset of 2 blocks, leaving a thin sub-floor
     * crawlspace beneath the office carpet.</p>
     */
    @Override
    protected int getFloorOffset() {
        return 2;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Doorways in the Lobby are 2 blocks tall, matching the low ceiling height
     * to maintain the claustrophobic feel.</p>
     */
    @Override
    protected int getDoorwayHeight() {
        return 2;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Places sea lanterns on the ceiling and marks 10% of them as flickering
     * for the fluorescent light effect. The flickering is deterministic based on
     * the world seed.</p>
     */
    @Override
    protected void generateLighting(ChunkGenerator.ChunkData chunkData,
                                    RoomLayout layout,
                                    int ceilingY,
                                    int chunkStartX, int chunkStartZ) {
        // Call parent to place all lanterns
        super.generateLighting(chunkData, layout, ceilingY, chunkStartX, chunkStartZ);

        // Mark 10% as flickering (only if flicker manager is available)
        FlickerManager flickerManager = BackroomsPlugin.getFlickerManager();
        if (flickerManager == null || !layout.overlapsRoom()) {
            return;
        }

        // Get the world for creating Location objects
        World world = BackroomsPlugin.getInstance().getServer().getWorld(
                BackroomsPlugin.getInstance().getBackroomsConfig().getBackroomsWorldName()
        );
        if (world == null) {
            return;
        }

        int spacing = config.getLightSpacing();
        Random flickerRand = new Random(
                seed ^ ((long) chunkStartX * 0x85ebca6bL) ^ ((long) chunkStartZ * 0xc2b2ae35L) ^ config.getIdHashCode()
        );

        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                int globalX = chunkStartX + localX;
                int globalZ = chunkStartZ + localZ;

                if (globalX % spacing == 0 && globalZ % spacing == 0) {
                    // Check if this lantern should flicker
                    if (flickerRand.nextDouble() < FLICKER_CHANCE) {
                        Location loc = new Location(world, globalX, ceilingY, globalZ);
                        flickerManager.markFlickering(loc);
                    }
                }
            }
        }
    }

    /**
     * Adds scattered yellow carpet patches on the lobby floor.
     *
     * <p>Carpet is placed on top of the floor material at a low probability, using
     * the chunk seed for deterministic placement. Only blocks inside rooms receive
     * carpet; corridors remain bare concrete.</p>
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

                if (insideRoom && featureRand.nextDouble() < CARPET_CHANCE) {
                    chunkData.setBlock(localX, floorY + 1, localZ, CARPET_MATERIAL);
                }
            }
        }
    }
}

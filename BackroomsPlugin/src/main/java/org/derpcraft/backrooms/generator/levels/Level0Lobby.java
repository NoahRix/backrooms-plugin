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
 *   <li><b>Multiple floors</b> connected by stairwells for vertical exploration</li>
 * </ul>
 *
 * <h2>Multi-floor structure</h2>
 * <p>Level 0 generates 4 floors stacked vertically within its Y range (-64 to 0).
 * The first floor sits directly on bedrock with a 1-block offset. Each floor has
 * 4 blocks of air space and a 1-block ceiling. Floors are connected by stairwells
 * that appear in certain rooms, allowing players to move between floors without
 * leaving the level.</p>
 *
 * <h2>Configuration defaults (from {@code config.yml})</h2>
 * <table>
 *   <tr><td>Y range</td><td>-64 to 0</td></tr>
 *   <tr><td>Wall</td><td>BIRCH_PLANKS</td></tr>
 *   <tr><td>Floor</td><td>LIGHT_GRAY_CONCRETE</td></tr>
 *   <tr><td>Ceiling</td><td>SMOOTH_STONE</td></tr>
 *   <tr><td>Light</td><td>SEA_LANTERN</td></tr>
 *   <tr><td>Ceiling height</td><td>4 blocks</td></tr>
 *   <tr><td>Number of floors</td><td>4</td></tr>
 * </table>
 *
 * @see BackroomsLevel
 * @see Level1HabitableZone
 * @see Level2PipeDreams
 */
public class Level0Lobby extends BackroomsLevel {

    /**
     * Number of floors to generate in Level 0.
     * Each floor's walking surface is placed directly above the previous floor's ceiling.
     */
    private static final int NUM_FLOORS = 4;

    /**
     * Probability (0.0&ndash;1.0) that a sea lantern will flicker.
     * 10% of lanterns will have the flicker effect.
     */
    private static final double FLICKER_CHANCE = 0.10;

    /**
     * Probability (0.0&ndash;1.0) that a room contains a stairwell connecting to the next floor.
     */
    private static final double STAIRWELL_CHANCE = 0.15;

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
     * <p>Generates multiple floors stacked vertically within Level 0's Y range.
     * Each floor is a complete Backrooms layer with its own rooms, walls, lighting,
     * and special features. Floors are connected by stairwells that appear in
     * certain rooms based on a deterministic seed.</p>
     */
    @Override
    public void generate(ChunkGenerator.ChunkData chunkData,
                         int chunkStartX, int chunkStartZ,
                         int chunkEndX, int chunkEndZ,
                         int worldMinY, int worldMaxY) {

        int effectiveMinY = Math.max(config.getMinY(), worldMinY);
        int effectiveMaxY = Math.min(config.getMaxY(), worldMaxY);

        // Calculate floor height: ceiling height + 1 (for the ceiling block itself)
        int floorHeight = config.getCeilingHeight() + 1;

        // Generate each floor
        for (int floorIndex = 0; floorIndex < NUM_FLOORS; floorIndex++) {
            int floorBaseY = effectiveMinY + (floorIndex * floorHeight);
            int floorY = floorBaseY + getFloorOffset();
            int ceilingY = floorY + config.getCeilingHeight();

            // Check if this floor fits within the world bounds
            if (ceilingY > effectiveMaxY) {
                break; // No more room for this floor
            }
            if (floorY >= ceilingY) {
                continue; // Skip invalid floors
            }

            // Generate this floor's structure
            generateFloorAndCeiling(chunkData, floorY, ceilingY);

            RoomLayout layout = calculateRoomLayout(chunkStartX, chunkStartZ, chunkEndX, chunkEndZ);

            generateWallsAndDoorways(chunkData, layout, floorY, ceilingY, chunkStartX, chunkStartZ);

            generateLighting(chunkData, layout, ceilingY, chunkStartX, chunkStartZ);

            generateLootChest(chunkData, layout, floorY, chunkStartX, chunkStartZ);

            generateSpecialFeatures(chunkData, layout, floorY, ceilingY, chunkStartX, chunkStartZ);

            // Generate stairwell if this isn't the top floor and the room qualifies
            if (floorIndex < NUM_FLOORS - 1 && layout.overlapsRoom()) {
                generateStairwell(chunkData, layout, floorY, ceilingY, chunkStartX, chunkStartZ, floorIndex);
            }
        }
    }

    /**
     * Generates a stairwell connecting this floor to the next floor above.
     *
     * <p>Stairwells are placed in rooms based on a deterministic seed. They consist
     * of a vertical shaft with stairs going up to the next floor. The stairwell
     * cuts through the ceiling of the current floor and the floor of the next floor.</p>
     *
     * @param chunkData    the mutable chunk data
     * @param layout       the computed room layout for this chunk
     * @param floorY       the Y coordinate of the current floor surface
     * @param ceilingY     the Y coordinate of the current floor's ceiling
     * @param chunkStartX  world X of the chunk's western edge
     * @param chunkStartZ  world Z of the chunk's northern edge
     * @param floorIndex   the index of the current floor (0 = bottom)
     */
    private void generateStairwell(ChunkGenerator.ChunkData chunkData,
                                   RoomLayout layout,
                                   int floorY, int ceilingY,
                                   int chunkStartX, int chunkStartZ,
                                   int floorIndex) {
        // Determine if this room has a stairwell
        long stairwellSeed = seed ^ ((long) chunkStartX * 0x6a09e667L) ^ ((long) chunkStartZ * 0xbb67ae85L)
                           ^ config.getIdHashCode() ^ (floorIndex * 0x9e3779b9L);
        Random stairwellRand = new Random(stairwellSeed);

        if (stairwellRand.nextDouble() >= STAIRWELL_CHANCE) {
            return; // No stairwell in this room
        }

        // Position the stairwell in the center of the room
        int roomCenterX = (layout.roomStartX() + layout.roomEndX()) / 2;
        int roomCenterZ = (layout.roomStartZ() + layout.roomEndZ()) / 2;

        // Check if the stairwell position is within this chunk
        int localX = roomCenterX - chunkStartX;
        int localZ = roomCenterZ - chunkStartZ;

        if (localX < 0 || localX >= 16 || localZ < 0 || localZ >= 16) {
            return; // Stairwell is in a different chunk
        }

        // Create a 3x3 stairwell shaft
        int floorHeight = config.getCeilingHeight() + 1;
        int nextFloorY = floorY + floorHeight;

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                int x = localX + dx;
                int z = localZ + dz;

                if (x < 0 || x >= 16 || z < 0 || z >= 16) {
                    continue;
                }

                // Clear the shaft from current floor to next floor
                for (int y = floorY; y < nextFloorY; y++) {
                    chunkData.setBlock(x, y, z, Material.AIR);
                }

                // Place stairs going up (spiral pattern)
                // Bottom layer: stairs facing north
                if (dx == 0 && dz == -1) {
                    chunkData.setBlock(x, floorY + 1, z, Material.OAK_STAIRS);
                }
                // Middle layers: alternating stairs
                else if (dx == 1 && dz == 0) {
                    chunkData.setBlock(x, floorY + 2, z, Material.OAK_STAIRS);
                }
                else if (dx == 0 && dz == 1) {
                    chunkData.setBlock(x, floorY + 3, z, Material.OAK_STAIRS);
                }
                // Top layer: stairs facing west to reach next floor
                else if (dx == -1 && dz == 0) {
                    chunkData.setBlock(x, floorY + 4, z, Material.OAK_STAIRS);
                }

                // Place walls around the stairwell
                if (Math.abs(dx) == 1 || Math.abs(dz) == 1) {
                    for (int y = floorY + 1; y < nextFloorY; y++) {
                        if (dx == -1 || dx == 1 || dz == -1 || dz == 1) {
                            // Only place walls on the edges, not corners
                            if ((Math.abs(dx) == 1 && dz == 0) || (Math.abs(dz) == 1 && dx == 0)) {
                                chunkData.setBlock(x, y, z, config.getWallMaterial());
                            }
                        }
                    }
                }
            }
        }

        // Add a light in the stairwell
        chunkData.setBlock(localX, nextFloorY - 1, localZ, Material.SEA_LANTERN);
    }

    /**
     * {@inheritDoc}
     *
     * <p>The Lobby uses a floor offset of 1 block, placing the floor surface directly
     * above the bedrock layer with no gap.</p>
     */
    @Override
    protected int getFloorOffset() {
        return 1;
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

        // Get the plugin instance (may be null during initial world generation)
        BackroomsPlugin plugin = BackroomsPlugin.getInstance();
        if (plugin == null) {
            return;
        }

        // Get the world for creating Location objects
        World world = plugin.getServer().getWorld(
                plugin.getBackroomsConfig().getBackroomsWorldName()
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
}

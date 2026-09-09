package org.derpcraft.liminal.generator.levels;

import org.derpcraft.liminal.config.LevelConfig;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;
import org.derpcraft.liminal.LiminalPlugin;
import org.derpcraft.liminal.effects.FlickerManager;

import java.util.Random;

/**
 * <b>Level 0 &ndash; The Lobby</b>: the iconic yellow-wallpapered office liminal space.
 *
 * <p>This is the entry level of the Liminal and the most recognisable. It features:</p>
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
 * @see LiminalLevel
 * @see Level1HabitableZone
 * @see Level2PipeDreams
 */
public class Level0Lobby extends LiminalLevel {

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
     * Each floor is a complete Liminal layer with its own rooms, walls, lighting,
     * and special features. Floors are connected by stairwells that appear in
     * certain rooms based on a deterministic seed.</p>
     */
    @Override
    public void generate(ChunkGenerator.ChunkData chunkData,
                         int chunkStartX, int chunkStartZ,
                         int chunkEndX, int chunkEndZ,
                         int worldMinY, int worldMaxY) {

        // Levels step upward as the rings expand outward (the transition
        // corridors ramp between elevations).
        int elevation = config.getElevationStep();
        int effectiveMinY = Math.max(config.getMinY() + elevation, worldMinY);
        int effectiveMaxY = Math.min(config.getMaxY() + elevation, worldMaxY);

        // Calculate floor height: ceiling height + 1 (for the ceiling block itself)
        int floorHeight = config.getCeilingHeight() + 1;

        // Generate each floor. pendingStairwellOpening carries the local chunk
        // coordinates of a stairwell built on the floor below; the opening
        // through the CURRENT floor's slab is carved right after this floor's
        // slab is laid (otherwise the slab would seal the shaft shut).
        int[] pendingStairwellOpening = null;

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

            // Carve the passage through this floor's slab for the stairwell
            // rising from the floor below (must happen after the slab is laid).
            if (pendingStairwellOpening != null) {
                carveStairwellOpening(chunkData, pendingStairwellOpening[0], pendingStairwellOpening[1], floorY);
                pendingStairwellOpening = null;
            }

            // Each floor gets its own room layout so walls never line up
            // between stacked floors.
            RoomLayout layout = calculateRoomLayout(chunkStartX, chunkStartZ, chunkEndX, chunkEndZ, floorIndex);

            generateWallsAndDoorways(chunkData, layout, floorY, ceilingY, chunkStartX, chunkStartZ);

            generateLighting(chunkData, layout, ceilingY, chunkStartX, chunkStartZ);

            generateLootChest(chunkData, layout, floorY, chunkStartX, chunkStartZ);

            generateSpecialFeatures(chunkData, layout, floorY, ceilingY, chunkStartX, chunkStartZ);

            // Generate stairwell if this isn't the top floor and the room qualifies
            if (floorIndex < NUM_FLOORS - 1 && layout.overlapsRoom()) {
                pendingStairwellOpening = generateStairwell(
                        chunkData, layout, floorY, ceilingY, chunkStartX, chunkStartZ, floorIndex);
            }
        }
    }

    /**
     * Generates a stairwell connecting this floor to the next floor above.
     *
     * <p>The stairwell is a 3x3 spiral staircase tucked into the <b>corner</b> of
     * the room: every floor's room anchors at the same grid-cell origin (only the
     * room sizes vary), so the shaft always sits at that shared corner with a
     * 1-block inset. Because every floor's wall lines run at offset 0 or at least
     * {@code roomMinWidth - 1} blocks away from the origin, no wall of any floor
     * above or below can ever cross the shaft column &mdash; collisions are
     * impossible by construction.</p>
     *
     * <p>Structure: a central support pillar topped with a sea lantern, oak stairs
     * winding up around it (south &rarr; west &rarr; north &rarr; east), wall
     * columns on the north/east/west faces, corners left open for the climb, and
     * the <b>south</b> face open as the entrance (facing the room interior). When
     * several floors in a row have stairwells they stack into a continuous
     * climbable tower at the same corner.</p>
     *
     * <p>The shaft clears the current floor's ceiling; the passage through the
     * <b>next</b> floor's slab is carved later (see {@link #carveStairwellOpening})
     * because that floor's slab is laid after this method runs and would otherwise
     * seal the shaft shut.</p>
     *
     * @param chunkData    the mutable chunk data
     * @param layout       the computed room layout for this chunk
     * @param floorY       the Y coordinate of the current floor surface
     * @param ceilingY     the Y coordinate of the current floor's ceiling
     * @param chunkStartX  world X of the chunk's western edge
     * @param chunkStartZ  world Z of the chunk's northern edge
     * @param floorIndex   the index of the current floor (0 = bottom)
     * @return the local chunk coordinates {@code {x, z}} of the shaft centre
     *         whose opening must be carved in the next floor's slab, or
     *         {@code null} when this room has no stairwell (or the shaft
     *         would span a chunk border)
     */
    private int[] generateStairwell(ChunkGenerator.ChunkData chunkData,
                                    RoomLayout layout,
                                    int floorY, int ceilingY,
                                    int chunkStartX, int chunkStartZ,
                                    int floorIndex) {
        // Determine if this room has a stairwell
        long stairwellSeed = seed ^ ((long) chunkStartX * 0x6a09e667L) ^ ((long) chunkStartZ * 0xbb67ae85L)
                           ^ config.getIdHashCode() ^ (floorIndex * 0x9e3779b9L);
        Random stairwellRand = new Random(stairwellSeed);

        if (stairwellRand.nextDouble() >= STAIRWELL_CHANCE) {
            return null; // No stairwell in this room
        }

        // Anchor the shaft at the room's minimum corner (shared by every floor,
        // so stacked stairwells align into a tower and no wall line of any floor
        // can cross the shaft column). The centre sits 2 blocks in from both
        // walls, giving a 1-block air gap between the shaft walls and the room's
        // own west/north walls.
        int roomCenterX = layout.roomStartX() + 2;
        int roomCenterZ = layout.roomStartZ() + 2;

        // Check if the stairwell position is within this chunk (with room for
        // the full 3x3 shaft, so shaft centres hugging a chunk border are skipped)
        int localX = roomCenterX - chunkStartX;
        int localZ = roomCenterZ - chunkStartZ;

        if (localX < 1 || localX > 14 || localZ < 1 || localZ > 14) {
            return null; // Stairwell is in a different chunk or spans the border
        }

        Material wallMat = config.getWallMaterial();
        int floorHeight = config.getCeilingHeight() + 1;
        int nextFloorY = floorY + floorHeight;

        // Clear the shaft volume (air space + this floor's ceiling). The floor
        // surface at floorY is left intact so the shaft has a solid base, and
        // the next floor's slab (at nextFloorY) is carved by carveStairwellOpening.
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int y = floorY + 1; y < nextFloorY; y++) {
                    chunkData.setBlock(localX + dx, y, localZ + dz, Material.AIR);
                }
            }
        }

        // Central support pillar with a light on top so the shaft is lit
        for (int y = floorY + 1; y < ceilingY; y++) {
            chunkData.setBlock(localX, y, localZ, wallMat);
        }
        chunkData.setBlock(localX, ceilingY, localZ, Material.SEA_LANTERN);

        // Enclose the north, east and west faces (corners stay open so the
        // spiral climb has room to turn). The south face stays open as the
        // entrance, facing the room interior. Stairs below overwrite their
        // own positions in these columns.
        for (int y = floorY + 1; y <= ceilingY; y++) {
            chunkData.setBlock(localX, y, localZ - 1, wallMat);     // north
            chunkData.setBlock(localX + 1, y, localZ, wallMat);     // east
            chunkData.setBlock(localX - 1, y, localZ, wallMat);     // west
        }

        // Spiral stairs winding up around the pillar (south -> west -> north -> east)
        chunkData.setBlock(localX, floorY + 1, localZ + 1, Material.OAK_STAIRS);
        chunkData.setBlock(localX - 1, floorY + 2, localZ, Material.OAK_STAIRS);
        chunkData.setBlock(localX, floorY + 3, localZ - 1, Material.OAK_STAIRS);
        chunkData.setBlock(localX + 1, ceilingY, localZ, Material.OAK_STAIRS);

        return new int[]{localX, localZ};
    }

    /**
     * Carves the 3x3 opening through a floor's slab for the stairwell rising
     * from the floor below, so players can step out of the shaft onto this floor.
     *
     * @param chunkData the mutable chunk data
     * @param localX    local chunk X of the shaft centre (recorded by generateStairwell)
     * @param localZ    local chunk Z of the shaft centre
     * @param floorY    the Y coordinate of this floor's surface (the slab to carve)
     */
    private void carveStairwellOpening(ChunkGenerator.ChunkData chunkData,
                                       int localX, int localZ, int floorY) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                chunkData.setBlock(localX + dx, floorY, localZ + dz, Material.AIR);
            }
        }
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
        FlickerManager flickerManager = LiminalPlugin.getFlickerManager();
        if (flickerManager == null || !layout.overlapsRoom()) {
            return;
        }

        // Get the plugin instance (may be null during initial world generation)
        LiminalPlugin plugin = LiminalPlugin.getInstance();
        if (plugin == null) {
            return;
        }

        // Get the world for creating Location objects
        World world = plugin.getServer().getWorld(
                plugin.getLiminalConfig().getLiminalWorldName()
        );
        if (world == null) {
            return;
        }

        int spacing = config.getLightSpacing();
        // ceilingY is mixed in so each floor gets its own flicker pattern
        Random flickerRand = new Random(
                seed ^ ((long) chunkStartX * 0x85ebca6bL) ^ ((long) chunkStartZ * 0xc2b2ae35L)
                ^ config.getIdHashCode() ^ ((long) ceilingY * 0x27d4eb2fL)
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

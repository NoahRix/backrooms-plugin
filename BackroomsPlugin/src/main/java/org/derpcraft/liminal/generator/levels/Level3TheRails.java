package org.derpcraft.liminal.generator.levels;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;
import org.derpcraft.liminal.LiminalPlugin;
import org.derpcraft.liminal.config.LevelConfig;
import org.derpcraft.liminal.effects.FlickerManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * <b>Level 3 &ndash; The Rails</b>: an endless abandoned rail network.
 *
 * <p>A grid of minecart lines stretches out into the darkness, kept alive by
 * redstone torches bolted to the track beds. The rails are fully powered so
 * players (and things) can still ride them &mdash; which is exactly the problem:
 * the network works perfectly, but nobody remembers who it was built for.
 * Empty minecarts roam the lines on their own, signs announce departures that
 * will never come, and cobwebs choke the track sides. The dim red glow of the
 * torches is the only light for blocks in every direction.</p>
 *
 * <h2>Rail grid</h2>
 * <ul>
 *   <li><b>X lines</b> run along the X axis at world Z &equiv; 16 (mod 32)</li>
 *   <li><b>Z lines</b> run along the Z axis at world X &equiv; 16 (mod 32)</li>
 *   <li>Lines cross every 32 blocks. The Z line passes <b>over</b> the X line on
 *       a short viaduct (rails climb 4 blocks onto a beam, then descend) so both
 *       axes stay fully traversable and riders never get blocked at a junction.
 *       The X line passes beneath with two blocks of clearance.</li>
 *   <li>Every 8th rail is a {@link Material#POWERED_RAIL} with a stable redstone
 *       torch beside it. Boosting torches are <b>never</b> registered as
 *       flickering &mdash; killing one would strand minecarts mid-network.</li>
 * </ul>
 *
 * <h2>Creepy details</h2>
 * <ul>
 *   <li><b>Broken track</b> &mdash; optional ({@code broken-track-chance}, default 0):
 *       some rail cells are simply missing, as if the line was interrupted.</li>
 *   <li><b>Cobwebs</b> &mdash; chance ({@code cobweb-chance}) to find cobwebs beside a
 *       rail. Cobwebs never occupy a rail cell or the column above it, so carts
 *       are never obstructed.</li>
 *   <li><b>Ghost minecarts</b> &mdash; ({@code ghost-cart-chance}) some chunks spawn
 *       empty minecarts on the powered rails when they load. Boosted by the
 *       torches, they roam the network on their own indefinitely.</li>
 *   <li><b>Stations</b> &mdash; every 96 blocks along an X line, a small platform
 *       with sign posts (filled in by the populator with cryptic messages),
 *       a supply chest, and torches.</li>
 *   <li><b>Flickering torches</b> &mdash; decorative floor torches away from the
 *       tracks briefly "die" (swap to air) for nearby players. Torches near the
 *       rails are always stable.</li>
 * </ul>
 *
 * @see LiminalLevel
 * @see Level0Lobby
 * @see GenericLiminalLevel
 */
public class Level3TheRails extends LiminalLevel {

    /** Grid spacing of the rail network, in blocks (one line every 32). */
    public static final int GRID = 32;

    /** Offset of the rail lines within each grid cell (lines at coord % GRID == LINE_OFFSET). */
    public static final int LINE_OFFSET = 16;

    /** One powered rail every N blocks along a line, at line-axis coords divisible by this. */
    public static final int POWER_EVERY = 8;

    /** One station every N blocks along an X line, at coords divisible by this. */
    public static final int STATION_EVERY = 96;

    /** Half-length of the viaduct approach (ramp cells at crossings 1..4 either side). */
    public static final int VIADUCT_HALF = 4;

    /** Rail height of the viaduct beam above the floor-level rail (keeps 2 air for riders). */
    private static final int VIADUCT_HEIGHT = 4;

    /**
     * Constructs the Level 3 &ndash; The Rails generator.
     *
     * @param config the level configuration (materials, room sizes, Y bounds, ring radii)
     * @param seed   the world generation seed for deterministic output
     */
    public Level3TheRails(LevelConfig config, long seed) {
        super(config, seed);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Rails run at floor level on a grid; crossings use viaducts so both axes
     * are traversable. Powered rails with stable torch boosters keep carts moving.
     * Decorative torches (flickering, away from the tracks) light the rest, and
     * cobwebs cling to the track sides.</p>
     */
    @Override
    protected void generateSpecialFeatures(ChunkGenerator.ChunkData chunkData,
                                           RoomLayout layout,
                                           int floorY, int ceilingY,
                                           int chunkStartX, int chunkStartZ) {
        Random railRand = new Random(
                seed ^ ((long) chunkStartX * 0x1b873593L) ^ ((long) chunkStartZ * 0xcc9e2d51L)
                ^ config.getIdHashCode() ^ 0x5a3f0c77L);

        int railY = floorY + 1; // rails sit directly on the floor slab
        List<int[]> railCells = new ArrayList<>();

        for (int localX = 0; localX < 16; localX++) {
            int wx = chunkStartX + localX;
            for (int localZ = 0; localZ < 16; localZ++) {
                int wz = chunkStartZ + localZ;
                int xm = mod(wx, GRID);
                int zm = mod(wz, GRID);
                boolean onXLine = zm == LINE_OFFSET;
                boolean onZLine = xm == LINE_OFFSET;
                if (!onXLine && !onZLine) continue;

                if (onXLine && onZLine) {
                    // Crossing: plain rail keeps the X line continuous through the
                    // junction; the Z line hops over on the viaduct instead.
                    placeRail(chunkData, wx, railY, wz, false, true, Material.RAIL);
                    railCells.add(new int[]{wx, railY, wz});
                    continue;
                }

                // Z-line cells inside a viaduct approach are placed by placeViaduct.
                if (onZLine && Math.abs(zm - LINE_OFFSET) <= VIADUCT_HALF) continue;

                boolean powered = onXLine ? mod(wx, POWER_EVERY) == 0 : mod(wz, POWER_EVERY) == 0;

                // Broken track: leave a gap (config-gated, default never).
                if (railRand.nextDouble() < config.getBrokenTrackChance()) continue;

                placeRail(chunkData, wx, railY, wz, powered, onXLine, null);
                railCells.add(new int[]{wx, railY, wz});
            }
        }

        // Viaducts: Z lines bridge over X lines at every crossing.
        for (int localX = 0; localX < 16; localX++) {
            int wx = chunkStartX + localX;
            if (mod(wx, GRID) != LINE_OFFSET) continue;
            // Both crossings that could reach into this chunk (the viaduct spans
            // the crossing's chunk and its northern neighbour).
            for (long base = Math.floorDiv((long) chunkStartZ - VIADUCT_HALF, GRID);
                 base <= Math.floorDiv((long) chunkStartZ + 15 + VIADUCT_HALF, GRID);
                 base++) {
                int crossingZ = (int) (base * GRID + LINE_OFFSET);
                for (int dz = -VIADUCT_HALF; dz <= VIADUCT_HALF; dz++) {
                    int wz = crossingZ + dz;
                    int lz = wz - chunkStartZ;
                    if (lz < 0 || lz >= 16) continue;
                    placeViaductCell(chunkData, wx, railY, wz, dz);
                }
            }
        }

        // Stations: platforms along X lines every STATION_EVERY blocks (station
        // coordinates are multiples of 16, so a station never spans a chunk border).
        for (int localX = 0; localX < 16; localX++) {
            int wx = chunkStartX + localX;
            if (mod(wx, STATION_EVERY) != 0) continue;
            for (int localZ = 0; localZ < 16; localZ++) {
                int wz = chunkStartZ + localZ;
                if (mod(wz, GRID) != LINE_OFFSET) continue;
                placeStation(chunkData, wx, railY, wz);
            }
        }

        // Cobwebs beside the rails (never on a rail cell or in the column above one).
        for (int[] cell : railCells) {
            if (railRand.nextDouble() >= config.getCobwebChance()) continue;
            int side = railRand.nextInt(4);
            int sx = cell[0] + (side == 0 ? 1 : side == 1 ? -1 : 0);
            int sz = cell[2] + (side == 2 ? 1 : side == 3 ? -1 : 0);
            int lx = sx - chunkStartX;
            int lz = sz - chunkStartZ;
            if (lx < 0 || lx >= 16 || lz < 0 || lz >= 16) continue;
            if (chunkData.getType(lx, cell[1], lz) == Material.AIR) {
                chunkData.setBlock(lx, cell[1], lz, Material.COBWEB);
            }
        }
    }

    /**
     * Places a rail (optionally powered) on the track bed with an explicit
     * shape. Rails placed with a bare material default to a north-south shape,
     * which derails carts on X-running lines (no block updates happen during
     * world generation to correct them). The archway carve keeps two blocks
     * above the rail clear so riders fit through walls.
     *
     * @param chunkData      the mutable chunk data
     * @param x              world X of the rail
     * @param railY          world Y of the rail block
     * @param z              world Z of the rail
     * @param powered        whether to place a powered rail (with a booster torch)
     * @param onXLine        whether this cell belongs to an X-running line
     * @param forcedMaterial explicit material override (null = rail or powered rail)
     */
    private void placeRail(ChunkGenerator.ChunkData chunkData, int x, int railY, int z,
                           boolean powered, boolean onXLine, Material forcedMaterial) {
        int lx = x & 15;
        int lz = z & 15;
        org.bukkit.block.data.Rail data;
        if (forcedMaterial != null) {
            data = (org.bukkit.block.data.Rail) forcedMaterial.createBlockData();
        } else if (powered) {
            data = (org.bukkit.block.data.Rail) Material.POWERED_RAIL.createBlockData();
            ((org.bukkit.block.data.Powerable) data).setPowered(true); // belt-and-braces: the torch beside it also powers this
        } else {
            data = (org.bukkit.block.data.Rail) Material.RAIL.createBlockData();
        }
        data.setShape(onXLine ? org.bukkit.block.data.Rail.Shape.EAST_WEST
                              : org.bukkit.block.data.Rail.Shape.NORTH_SOUTH);
        chunkData.setBlock(lx, railY, lz, data);
        // Archway: keep three blocks above the rail clear so riders fit through
        // walls comfortably (walls are up to 7 blocks tall at ceiling-height 8).
        chunkData.setBlock(lx, railY + 1, lz, Material.AIR);
        chunkData.setBlock(lx, railY + 2, lz, Material.AIR);
        chunkData.setBlock(lx, railY + 3, lz, Material.AIR);

        if (powered && forcedMaterial == null) {
            placeBoosterTorch(chunkData, x, railY, z, onXLine);
        }
    }

    /**
     * Places a stable redstone torch beside a powered rail to keep it active.
     * The torch is offset <b>perpendicular</b> to the line (placing it along the
     * line would occupy the next track cell and be overwritten by the rail).
     * The torch is never registered for flicker &mdash; dying boosters would
     * strand minecarts mid-network.
     */
    private void placeBoosterTorch(ChunkGenerator.ChunkData chunkData, int x, int railY, int z, boolean onXLine) {
        int lx = onXLine ? (x & 15) : ((x + 1) & 15);
        int lz = onXLine ? ((z + 1) & 15) : (z & 15);
        chunkData.setBlock(lx, railY, lz, Material.REDSTONE_TORCH);
    }

    /**
     * Builds one cell of the viaduct where a Z line bridges over an X line.
     *
     * <p>Beam cells (dz in -1..1) float at {@code railY + VIADUCT_HEIGHT} on
     * supports at {@code railY + VIADUCT_HEIGHT - 1}, leaving two blocks of air
     * above the X line for riders. Ramp cells (dz in ±2..±4) step down by one
     * block each with explicit ascending shapes (world generation performs no
     * block updates, so shapes must be set, not inferred). A booster pylon
     * stands east of every non-centre cell, topped with a stable redstone torch
     * that powers the adjacent powered rail.</p>
     *
     * @param chunkData the mutable chunk data
     * @param x         world X of the crossing (the Z line's axis)
     * @param railY     world Y of floor-level rails
     * @param z         world Z of this viaduct cell
     * @param dz        offset from the crossing centre (-4..+4)
     */
    private void placeViaductCell(ChunkGenerator.ChunkData chunkData, int x, int railY, int z, int dz) {
        Material wallMat = config.getWallMaterial();
        int height = VIADUCT_HEIGHT - Math.max(0, Math.abs(dz) - 1);
        int railAt = railY + height;
        int lx = x & 15;
        int lz = z & 15;

        org.bukkit.block.data.Rail data =
                (org.bukkit.block.data.Rail) Material.POWERED_RAIL.createBlockData();
        ((org.bukkit.block.data.Powerable) data).setPowered(true);
        if (Math.abs(dz) <= 1) {
            data.setShape(org.bukkit.block.data.Rail.Shape.NORTH_SOUTH); // flat beam
        } else if (dz < 0) {
            // North-side ramp: rises toward the crossing (+Z) — ascending south.
            data.setShape(org.bukkit.block.data.Rail.Shape.ASCENDING_SOUTH);
        } else {
            // South-side ramp: rises toward the crossing (-Z) — ascending north.
            data.setShape(org.bukkit.block.data.Rail.Shape.ASCENDING_NORTH);
        }

        chunkData.setBlock(lx, railAt - 1, lz, wallMat);
        chunkData.setBlock(lx, railAt, lz, data);

        // Open arch above the viaduct: where a room wall crosses the crossing
        // column, the beam would otherwise sit buried inside the wall. Carve
        // everything above the rail up to (but not including) the ceiling slab
        // so the viaduct always passes through walls as an open gateway.
        int ceilingY = railY - 1 + config.getCeilingHeight();
        for (int y = railAt + 1; y < ceilingY; y++) {
            chunkData.setBlock(lx, y, lz, Material.AIR);
        }

        // Booster pylon east of the track. The centre column (dz == 0) is skipped:
        // the X line passes through it; the beam is powered from its ends anyway
        // (the data also carries powered=true).
        if (dz != 0) {
            int plx = (x + 1) & 15;
            for (int y = railY; y < railAt; y++) {
                chunkData.setBlock(plx, y, lz, wallMat);
            }
            chunkData.setBlock(plx, railAt, lz, Material.REDSTONE_TORCH);
            // Keep the pylon slot open through walls as well.
            for (int y = railAt + 1; y < ceilingY; y++) {
                chunkData.setBlock(plx, y, lz, Material.AIR);
            }
        }
    }

    /**
     * Builds a small abandoned station platform beside an X line:
     * a 6x3 polished-basalt platform with two sign posts (text is filled in
     * later by the populator), a supply chest, and two torches.
     */
    private void placeStation(ChunkGenerator.ChunkData chunkData, int x, int railY, int z) {
        // Platform strip on the +Z side of the track (stays inside this chunk).
        for (int dx = 0; dx < 6; dx++) {
            for (int dz = 2; dz <= 4; dz++) {
                chunkData.setBlock((x + dx) & 15, railY - 1, (z + dz) & 15, Material.POLISHED_BASALT);
            }
        }

        // Sign posts (empty; the populator writes the cryptic messages).
        chunkData.setBlock((x + 1) & 15, railY, (z + 2) & 15, Material.OAK_SIGN);
        chunkData.setBlock((x + 4) & 15, railY, (z + 2) & 15, Material.OAK_SIGN);

        // Supply chest and lighting.
        chunkData.setBlock((x + 2) & 15, railY, (z + 4) & 15, Material.CHEST);
        chunkData.setBlock((x + 0) & 15, railY, (z + 2) & 15, Material.REDSTONE_TORCH);
        chunkData.setBlock((x + 5) & 15, railY, (z + 2) & 15, Material.REDSTONE_TORCH);
    }

    /**
     * {@inheritDoc}
     *
     * <p>The Rails is lit by redstone torches standing on the floor (redstone
     * torches cannot hang from ceilings). Torches on or beside a rail line cell
     * are skipped entirely, keeping the tracks dark; torches further away
     * flicker briefly for nearby players. Booster torches placed along the
     * powered rails are never registered here, so the network never browns out.</p>
     */
    @Override
    protected void generateLighting(ChunkGenerator.ChunkData chunkData,
                                    RoomLayout layout,
                                    int ceilingY,
                                    int chunkStartX, int chunkStartZ) {
        int spacing = config.getLightSpacing();
        Material torchMat = config.getLightMaterial() != null ? config.getLightMaterial() : Material.REDSTONE_TORCH;
        int floorSurfaceY = getFloorSurfaceY();

        FlickerManager flickerManager = LiminalPlugin.getFlickerManager();
        LiminalPlugin plugin = LiminalPlugin.getInstance();
        World world = null;
        if (plugin != null) {
            world = plugin.getServer().getWorld(plugin.getLiminalConfig().getLiminalWorldName());
        }

        Random flickerRand = new Random(
                seed ^ ((long) chunkStartX * 0x85ebca6bL) ^ ((long) chunkStartZ * 0xc2b2ae35L)
                ^ config.getIdHashCode() ^ ((long) ceilingY * 0x27d4eb2fL));

        for (int localX = 0; localX < 16; localX++) {
            int wx = chunkStartX + localX;
            for (int localZ = 0; localZ < 16; localZ++) {
                int wz = chunkStartZ + localZ;
                if (mod(wx, spacing) != 0 || mod(wz, spacing) != 0) continue;
                if (isNearRailLine(wx, wz)) continue; // keep the tracks dark

                chunkData.setBlock(localX, floorSurfaceY + 1, localZ, torchMat);

                // ~25% of room torches flicker (never the ones near the tracks).
                if (world != null && flickerManager != null && flickerRand.nextDouble() < 0.25) {
                    flickerManager.markFlickering(new Location(world, wx, floorSurfaceY + 1, wz),
                            torchMat, Material.AIR);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>The Rails uses a 1-block sub-floor so its walking surface aligns with the
     * other levels — adjacent rings must share the same floor height for seamless
     * transitions (and minecarts rolling in from a neighbouring ring).</p>
     */
    @Override
    protected int getFloorOffset() {
        return 1;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Doorways in The Rails are 3 blocks tall so a minecart with a rider
     * passes through wall archways comfortably.</p>
     */
    @Override
    protected int getDoorwayHeight() {
        return 3;
    }

    /**
     * Returns the world positions of the powered rails inside a chunk, along with
     * their travel axis, so ghost minecarts can be spawned on them at load time.
     *
     * <p>Static and deterministic so the runtime spawner agrees with the
     * generator without needing any stored state. The Y component is a -1
     * marker; the caller resolves it from the level's floor surface.</p>
     *
     * @param chunkX the chunk's X coordinate
     * @param chunkZ the chunk's Z coordinate
     * @return list of {@code {x, -1, z, axis}} where axis 0 = X line, 1 = Z line
     */
    public static List<int[]> poweredRailCells(int chunkX, int chunkZ) {
        List<int[]> cells = new ArrayList<>();
        int chunkStartX = chunkX * 16;
        int chunkStartZ = chunkZ * 16;
        for (int i = 0; i < 16; i++) {
            int wx = chunkStartX + i;
            int wz = chunkStartZ + i;
            if (mod(wz, GRID) == LINE_OFFSET && mod(wx, POWER_EVERY) == 0 && mod(wx, GRID) != LINE_OFFSET) {
                cells.add(new int[]{wx, -1, wz, 0});
            }
            if (mod(wx, GRID) == LINE_OFFSET && mod(wz, POWER_EVERY) == 0 && mod(wz, GRID) != LINE_OFFSET) {
                cells.add(new int[]{wx, -1, wz, 1});
            }
        }
        return cells;
    }

    /**
     * True when a world position sits on a rail line cell (either axis).
     *
     * @param x world X
     * @param z world Z
     * @return whether (x, z) is part of the rail grid
     */
    public static boolean isRailLine(int x, int z) {
        return mod(x, GRID) == LINE_OFFSET || mod(z, GRID) == LINE_OFFSET;
    }

    /**
     * True when a world position is on or directly beside a rail line &mdash;
     * decorative torches there would be too close to the rolling stock.
     *
     * @param x world X
     * @param z world Z
     * @return whether the position borders the rail grid
     */
    private static boolean isNearRailLine(int x, int z) {
        int xm = mod(x, GRID);
        int zm = mod(z, GRID);
        int nearLo = LINE_OFFSET - 1;
        int nearHi = LINE_OFFSET + 1;
        return (xm >= nearLo && xm <= nearHi) || (zm >= nearLo && zm <= nearHi);
    }

    /** Positive modulo (Java's % can go negative). */
    private static int mod(int value, int m) {
        return Math.floorMod(value, m);
    }
}

package org.derpcraft.liminal.generator;

import java.util.HashMap;
import java.util.Map;

/**
 * Plans room layouts for the Liminal world using a deterministic grid-based approach.
 *
 * <p>The world is divided into a grid of cells. Each cell is independently seeded to
 * decide whether it contains a room, what doors it has, and whether it is a special
 * room type. Results are cached to avoid redundant computation when multiple chunks
 * query the same cell.</p>
 *
 * <h2>Room types</h2>
 * <ul>
 *   <li>{@link RoomType#NORMAL} &ndash; a standard empty room</li>
 *   <li>{@link RoomType#LOOT} &ndash; contains loot containers</li>
 *   <li>{@link RoomType#HAZARD} &ndash; contains environmental hazards</li>
 *   <li>{@link RoomType#STAIRWELL} &ndash; connects to the level above/below</li>
 *   <li>{@link RoomType#FLOODED} &ndash; partially filled with water</li>
 *   <li>{@link RoomType#BOILER} &ndash; contains boiler machinery decoration</li>
 * </ul>
 *
 * @see org.derpcraft.liminal.generator.levels.LiminalLevel
 */
public class RoomPlanner {

    /** Global generation seed. */
    private final long seed;

    /** Size of each grid cell in blocks. */
    private final int cellSize;

    /** Cache of computed room info, keyed by packed cell coordinates. */
    private final Map<Long, RoomInfo> cache = new HashMap<>();

    /**
     * Constructs a new room planner.
     *
     * @param seed     the generation seed for deterministic room placement
     * @param cellSize the size of each grid cell in blocks
     */
    public RoomPlanner(long seed, int cellSize) {
        this.seed = seed;
        this.cellSize = cellSize;
    }

    /**
     * Returns the room info for the cell containing the given world coordinates.
     *
     * <p>If the cell has not been computed yet, it is generated and cached. The
     * level ID is incorporated into the seed to ensure different levels produce
     * different room layouts even at the same X/Z position.</p>
     *
     * @param globalX the world X coordinate
     * @param globalZ the world Z coordinate
     * @param levelId the level identifier (used to differentiate layouts per level)
     * @return the room info for the cell
     */
    public RoomInfo getRoom(int globalX, int globalZ, String levelId) {
        int cellX = Math.floorDiv(globalX, cellSize);
        int cellZ = Math.floorDiv(globalZ, cellSize);
        long key = packKey(cellX, cellZ, levelId.hashCode());

        return cache.computeIfAbsent(key, k -> generateRoom(cellX, cellZ, levelId));
    }

    /**
     * Generates the room info for a single grid cell.
     *
     * @param cellX   the cell's X coordinate in the grid
     * @param cellZ   the cell's Z coordinate in the grid
     * @param levelId the level identifier
     * @return the generated room info
     */
    private RoomInfo generateRoom(int cellX, int cellZ, String levelId) {
        long cellSeed = seed ^ ((long) cellX * 0x4f4f4f4fL) ^ ((long) cellZ * 0x2f2f2f2fL) ^ levelId.hashCode();
        java.util.Random random = new java.util.Random(cellSeed);

        boolean isRoom = random.nextDouble() < 0.7;
        if (!isRoom) {
            return RoomInfo.EMPTY;
        }

        boolean hasNorthDoor = random.nextDouble() < 0.7;
        boolean hasSouthDoor = random.nextDouble() < 0.7;
        boolean hasEastDoor = random.nextDouble() < 0.7;
        boolean hasWestDoor = random.nextDouble() < 0.7;

        int northDoorPos = 4 + random.nextInt(8);
        int southDoorPos = 4 + random.nextInt(8);
        int westDoorPos = 4 + random.nextInt(8);
        int eastDoorPos = 4 + random.nextInt(8);

        boolean isSpecial = random.nextDouble() < 0.05;
        RoomType roomType = isSpecial ? RoomType.values()[1 + random.nextInt(RoomType.values().length - 1)] : RoomType.NORMAL;

        return new RoomInfo(true, cellSize, cellSize, hasNorthDoor, hasSouthDoor, hasEastDoor, hasWestDoor,
                northDoorPos, southDoorPos, westDoorPos, eastDoorPos, roomType);
    }

    /**
     * Packs cell coordinates and a level hash into a single 64-bit cache key.
     *
     * @param x         the cell X coordinate
     * @param z         the cell Z coordinate
     * @param levelHash the hash code of the level ID
     * @return the packed key
     */
    private long packKey(int x, int z, int levelHash) {
        return ((long) x & 0xFFFFFFFFL) | (((long) z & 0xFFFFFFFFL) << 32) ^ ((long) levelHash << 16);
    }

    /**
     * Enumeration of special room types that can appear in the Liminal.
     */
    public enum RoomType {
        /** A standard empty room. */
        NORMAL,
        /** A room containing loot containers. */
        LOOT,
        /** A room with environmental hazards. */
        HAZARD,
        /** A room with a stairwell connecting to another level. */
        STAIRWELL,
        /** A room partially flooded with water. */
        FLOODED,
        /** A room containing boiler machinery decoration. */
        BOILER
    }

    /**
     * Immutable data class describing a single room in the Liminal grid.
     *
     * <p>Contains the room's dimensions, door positions, and type. The
     * {@link #EMPTY} constant represents a cell with no room.</p>
     */
    public static class RoomInfo {

        /** Singleton representing an empty cell with no room. */
        public static final RoomInfo EMPTY = new RoomInfo(false, 0, 0, false, false, false, false, 0, 0, 0, 0, RoomType.NORMAL);

        private final boolean room;
        private final int width;
        private final int depth;
        private final boolean hasNorthDoor;
        private final boolean hasSouthDoor;
        private final boolean hasEastDoor;
        private final boolean hasWestDoor;
        private final int northDoorPos;
        private final int southDoorPos;
        private final int westDoorPos;
        private final int eastDoorPos;
        private final RoomType roomType;

        /**
         * Constructs a new RoomInfo.
         *
         * @param room          whether this cell contains a room
         * @param width         the room width in blocks
         * @param depth         the room depth in blocks
         * @param hasNorthDoor  whether the north wall has a doorway
         * @param hasSouthDoor  whether the south wall has a doorway
         * @param hasEastDoor   whether the east wall has a doorway
         * @param hasWestDoor   whether the west wall has a doorway
         * @param northDoorPos  the X/Z offset of the north doorway
         * @param southDoorPos  the X/Z offset of the south doorway
         * @param westDoorPos   the X/Z offset of the west doorway
         * @param eastDoorPos   the X/Z offset of the east doorway
         * @param roomType      the type of room
         */
        public RoomInfo(boolean room, int width, int depth, boolean hasNorthDoor, boolean hasSouthDoor,
                        boolean hasEastDoor, boolean hasWestDoor, int northDoorPos, int southDoorPos,
                        int westDoorPos, int eastDoorPos, RoomType roomType) {
            this.room = room;
            this.width = width;
            this.depth = depth;
            this.hasNorthDoor = hasNorthDoor;
            this.hasSouthDoor = hasSouthDoor;
            this.hasEastDoor = hasEastDoor;
            this.hasWestDoor = hasWestDoor;
            this.northDoorPos = northDoorPos;
            this.southDoorPos = southDoorPos;
            this.westDoorPos = westDoorPos;
            this.eastDoorPos = eastDoorPos;
            this.roomType = roomType;
        }

        /** Returns whether this cell contains a room. */
        public boolean isRoom() { return room; }
        /** Returns the room width in blocks. */
        public int getWidth() { return width; }
        /** Returns the room depth in blocks. */
        public int getDepth() { return depth; }
        /** Returns the room type. */
        public RoomType getRoomType() { return roomType; }

        /** Returns whether the north wall has a doorway. */
        public boolean hasNorthDoor() { return hasNorthDoor; }
        /** Returns whether the south wall has a doorway. */
        public boolean hasSouthDoor() { return hasSouthDoor; }
        /** Returns whether the east wall has a doorway. */
        public boolean hasEastDoor() { return hasEastDoor; }
        /** Returns whether the west wall has a doorway. */
        public boolean hasWestDoor() { return hasWestDoor; }

        /** Returns the offset of the north doorway. */
        public int getNorthDoorPos() { return northDoorPos; }
        /** Returns the offset of the south doorway. */
        public int getSouthDoorPos() { return southDoorPos; }
        /** Returns the offset of the west doorway. */
        public int getWestDoorPos() { return westDoorPos; }
        /** Returns the offset of the east doorway. */
        public int getEastDoorPos() { return eastDoorPos; }
    }
}

package com.derpcraft.backrooms.generator;

import java.util.HashMap;
import java.util.Map;

public class RoomPlanner {

    private final long seed;
    private final int cellSize;
    private final Map<Long, RoomInfo> cache = new HashMap<>();

    public RoomPlanner(long seed, int cellSize) {
        this.seed = seed;
        this.cellSize = cellSize;
    }

    public RoomInfo getRoom(int globalX, int globalZ, String levelId) {
        int cellX = Math.floorDiv(globalX, cellSize);
        int cellZ = Math.floorDiv(globalZ, cellSize);
        long key = packKey(cellX, cellZ, levelId.hashCode());

        return cache.computeIfAbsent(key, k -> generateRoom(cellX, cellZ, levelId));
    }

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

    private long packKey(int x, int z, int levelHash) {
        return ((long) x & 0xFFFFFFFFL) | (((long) z & 0xFFFFFFFFL) << 32) ^ ((long) levelHash << 16);
    }

    public enum RoomType {
        NORMAL, LOOT, HAZARD, STAIRWELL, FLOODED, BOILER
    }

    public static class RoomInfo {
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

        public boolean isRoom() { return room; }
        public int getWidth() { return width; }
        public int getDepth() { return depth; }
        public RoomType getRoomType() { return roomType; }

        public boolean hasNorthDoor() { return hasNorthDoor; }
        public boolean hasSouthDoor() { return hasSouthDoor; }
        public boolean hasEastDoor() { return hasEastDoor; }
        public boolean hasWestDoor() { return hasWestDoor; }

        public int getNorthDoorPos() { return northDoorPos; }
        public int getSouthDoorPos() { return southDoorPos; }
        public int getWestDoorPos() { return westDoorPos; }
        public int getEastDoorPos() { return eastDoorPos; }
    }
}

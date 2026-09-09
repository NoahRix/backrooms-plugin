package org.derpcraft.backrooms.config;

import org.bukkit.Material;

/**
 * Data class holding the configuration parameters for a single Backrooms level.
 *
 * <p>Each level occupies a vertical slice of the world defined by {@link #minY} and
 * {@link #maxY}. Within that slice, rooms are generated using the configured materials,
 * dimensions, and lighting parameters.</p>
 *
 * <h2>Level layout</h2>
 * <pre>
 *   maxY  ─────────────────────  top of level's Y range
 *           (void / next level above)
 *   ───────────────────────────
 *           ceiling slab (ceilingMaterial)
 *           air space (ceilingHeight blocks)
 *           light blocks embedded in ceiling
 *           walls (wallMaterial) with doorways
 *   ───────────────────────────
 *           floor slab (floorMaterial)
 *           sub-floor space (floorOffset blocks, typically 2)
 *   ───────────────────────────
 *   minY  ─────────────────────  bottom of level's Y range
 * </pre>
 *
 * <p>This class is a plain data holder. The actual generation logic lives in the
 * {@link org.derpcraft.backrooms.generator.levels.BackroomsLevel} subclasses.</p>
 *
 * @see org.derpcraft.backrooms.generator.levels.BackroomsLevel
 * @see org.derpcraft.backrooms.config.BackroomsConfig
 */
public class LevelConfig {

    /** Unique identifier for this level (e.g. "level0", "level1"). */
    private String id;

    /** Cached hash code of the level ID, used as a seed component for room generation. */
    private int idHashCode;

    /** Whether this level is enabled and should be generated. */
    private boolean enabled;

    /** Display name shown to players (e.g. "Level 0 - The Lobby"). */
    private String name;

    /** Minimum Y coordinate of this level's vertical slice (inclusive). */
    private int minY;

    /** Maximum Y coordinate of this level's vertical slice (exclusive). */
    private int maxY;

    /**
     * Inner radius of this level's ring around the world spawn, in blocks (inclusive).
     * The level generates in every chunk whose centre lies at least this far from spawn.
     */
    private int minRadius;

    /**
     * Outer radius of this level's ring around the world spawn, in blocks (exclusive).
     * A negative value means the ring extends to infinity (the level fills everything
     * beyond {@link #minRadius}).
     */
    private int maxRadius;

    /**
     * Height of the air space between the floor slab and ceiling slab, in blocks.
     * This determines how tall the rooms feel.
     */
    private int ceilingHeight;

    /** Minimum width (X axis) of generated rooms, in blocks. */
    private int roomMinWidth;

    /** Maximum width (X axis) of generated rooms, in blocks. */
    private int roomMaxWidth;

    /** Minimum length (Z axis) of generated rooms, in blocks. */
    private int roomMinLength;

    /** Maximum length (Z axis) of generated rooms, in blocks. */
    private int roomMaxLength;

    /** Material used for room walls. */
    private Material wallMaterial;

    /** Material used for the floor slab. */
    private Material floorMaterial;

    /** Material used for the ceiling slab. */
    private Material ceilingMaterial;

    /** Material used for ceiling-mounted light blocks. */
    private Material lightMaterial;

    /**
     * Spacing between ceiling lights in blocks. Lights are placed at positions where
     * both global X and Z are divisible by this value.
     */
    private int lightSpacing;

    /** Width of corridors between rooms, in blocks. */
    private int corridorWidth;

    /**
     * Probability (0.0&ndash;1.0) that a room is designated as a "special" room
     * (e.g. loot room, hazard room, stairwell).
     */
    private double specialRoomChance;

    /**
     * Probability (0.0&ndash;1.0) that a rail cell is left empty (broken track).
     * Only used by rail-based levels; 0 disables broken track entirely.
     */
    private double brokenTrackChance;

    /**
     * Probability (0.0&ndash;1.0) that a cobweb is placed adjacent to a rail.
     * Cobwebs never occupy a rail cell or the column above it.
     */
    private double cobwebChance;

    /**
     * Probability (0.0&ndash;1.0) that a rail chunk spawns ghost minecarts
     * (empty carts that roam the powered network on their own).
     */
    private double ghostCartChance;

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    /** Returns the unique level identifier. */
    public String getId() { return id; }

    /**
     * Sets the level identifier and caches its hash code for seed computation.
     *
     * @param id the level ID (e.g. "level0")
     */
    public void setId(String id) { this.id = id; this.idHashCode = id.hashCode(); }

    /** Returns the cached hash code of the level ID. */
    public int getIdHashCode() { return idHashCode; }

    /** Returns whether this level is enabled. */
    public boolean isEnabled() { return enabled; }

    /** Sets whether this level is enabled. */
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    /** Returns the display name of this level. */
    public String getName() { return name; }

    /** Sets the display name of this level. */
    public void setName(String name) { this.name = name; }

    /** Returns the minimum Y coordinate (inclusive). */
    public int getMinY() { return minY; }

    /** Sets the minimum Y coordinate (inclusive). */
    public void setMinY(int minY) { this.minY = minY; }

    /** Returns the maximum Y coordinate (exclusive). */
    public int getMaxY() { return maxY; }

    /** Sets the maximum Y coordinate (exclusive). */
    public void setMaxY(int maxY) { this.maxY = maxY; }

    /** Returns the inner ring radius in blocks (inclusive, measured from world spawn). */
    public int getMinRadius() { return minRadius; }

    /** Sets the inner ring radius in blocks (inclusive, measured from world spawn). */
    public void setMinRadius(int minRadius) { this.minRadius = minRadius; }

    /** Returns the outer ring radius in blocks (exclusive), or a negative value for unlimited. */
    public int getMaxRadius() { return maxRadius; }

    /** Sets the outer ring radius in blocks (exclusive); a negative value means unlimited. */
    public void setMaxRadius(int maxRadius) { this.maxRadius = maxRadius; }

    /** Returns the ceiling height in blocks. */
    public int getCeilingHeight() { return ceilingHeight; }

    /** Sets the ceiling height in blocks. */
    public void setCeilingHeight(int ceilingHeight) { this.ceilingHeight = ceilingHeight; }

    /** Returns the minimum room width in blocks. */
    public int getRoomMinWidth() { return roomMinWidth; }

    /** Sets the minimum room width in blocks. */
    public void setRoomMinWidth(int roomMinWidth) { this.roomMinWidth = roomMinWidth; }

    /** Returns the maximum room width in blocks. */
    public int getRoomMaxWidth() { return roomMaxWidth; }

    /** Sets the maximum room width in blocks. */
    public void setRoomMaxWidth(int roomMaxWidth) { this.roomMaxWidth = roomMaxWidth; }

    /** Returns the minimum room length in blocks. */
    public int getRoomMinLength() { return roomMinLength; }

    /** Sets the minimum room length in blocks. */
    public void setRoomMinLength(int roomMinLength) { this.roomMinLength = roomMinLength; }

    /** Returns the maximum room length in blocks. */
    public int getRoomMaxLength() { return roomMaxLength; }

    /** Sets the maximum room length in blocks. */
    public void setRoomMaxLength(int roomMaxLength) { this.roomMaxLength = roomMaxLength; }

    /** Returns the wall material. */
    public Material getWallMaterial() { return wallMaterial; }

    /** Sets the wall material. */
    public void setWallMaterial(Material wallMaterial) { this.wallMaterial = wallMaterial; }

    /** Returns the floor material. */
    public Material getFloorMaterial() { return floorMaterial; }

    /** Sets the floor material. */
    public void setFloorMaterial(Material floorMaterial) { this.floorMaterial = floorMaterial; }

    /** Returns the ceiling material. */
    public Material getCeilingMaterial() { return ceilingMaterial; }

    /** Sets the ceiling material. */
    public void setCeilingMaterial(Material ceilingMaterial) { this.ceilingMaterial = ceilingMaterial; }

    /** Returns the light material. */
    public Material getLightMaterial() { return lightMaterial; }

    /** Sets the light material. */
    public void setLightMaterial(Material lightMaterial) { this.lightMaterial = lightMaterial; }

    /** Returns the light spacing in blocks. */
    public int getLightSpacing() { return lightSpacing; }

    /** Sets the light spacing in blocks. */
    public void setLightSpacing(int lightSpacing) { this.lightSpacing = lightSpacing; }

    /** Returns the corridor width in blocks. */
    public int getCorridorWidth() { return corridorWidth; }

    /** Sets the corridor width in blocks. */
    public void setCorridorWidth(int corridorWidth) { this.corridorWidth = corridorWidth; }

    /** Returns the special room probability (0.0&ndash;1.0). */
    public double getSpecialRoomChance() { return specialRoomChance; }

    /** Sets the special room probability (0.0&ndash;1.0). */
    public void setSpecialRoomChance(double specialRoomChance) { this.specialRoomChance = specialRoomChance; }

    /** Returns the broken-track probability (0 disables broken track). */
    public double getBrokenTrackChance() { return brokenTrackChance; }

    /** Sets the broken-track probability (0 disables broken track). */
    public void setBrokenTrackChance(double brokenTrackChance) { this.brokenTrackChance = brokenTrackChance; }

    /** Returns the rail-adjacent cobweb probability. */
    public double getCobwebChance() { return cobwebChance; }

    /** Sets the rail-adjacent cobweb probability. */
    public void setCobwebChance(double cobwebChance) { this.cobwebChance = cobwebChance; }

    /** Returns the ghost minecart spawn probability per rail chunk. */
    public double getGhostCartChance() { return ghostCartChance; }

    /** Sets the ghost minecart spawn probability per rail chunk. */
    public void setGhostCartChance(double ghostCartChance) { this.ghostCartChance = ghostCartChance; }

    /**
     * Returns the total height of this level's vertical slice in blocks.
     *
     * @return {@code maxY - minY}
     */
    public int getHeight() { return maxY - minY; }
}

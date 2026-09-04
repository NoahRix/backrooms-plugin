package com.derpcraft.backrooms.config;

import org.bukkit.Material;

public class LevelConfig {

    private String id;
    private boolean enabled;
    private String name;
    private int minY;
    private int maxY;
    private int ceilingHeight;
    private int roomMinWidth;
    private int roomMaxWidth;
    private int roomMinLength;
    private int roomMaxLength;
    private Material wallMaterial;
    private Material floorMaterial;
    private Material ceilingMaterial;
    private Material lightMaterial;
    private int lightSpacing;
    private int corridorWidth;
    private double specialRoomChance;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getMinY() { return minY; }
    public void setMinY(int minY) { this.minY = minY; }
    public int getMaxY() { return maxY; }
    public void setMaxY(int maxY) { this.maxY = maxY; }
    public int getCeilingHeight() { return ceilingHeight; }
    public void setCeilingHeight(int ceilingHeight) { this.ceilingHeight = ceilingHeight; }
    public int getRoomMinWidth() { return roomMinWidth; }
    public void setRoomMinWidth(int roomMinWidth) { this.roomMinWidth = roomMinWidth; }
    public int getRoomMaxWidth() { return roomMaxWidth; }
    public void setRoomMaxWidth(int roomMaxWidth) { this.roomMaxWidth = roomMaxWidth; }
    public int getRoomMinLength() { return roomMinLength; }
    public void setRoomMinLength(int roomMinLength) { this.roomMinLength = roomMinLength; }
    public int getRoomMaxLength() { return roomMaxLength; }
    public void setRoomMaxLength(int roomMaxLength) { this.roomMaxLength = roomMaxLength; }
    public Material getWallMaterial() { return wallMaterial; }
    public void setWallMaterial(Material wallMaterial) { this.wallMaterial = wallMaterial; }
    public Material getFloorMaterial() { return floorMaterial; }
    public void setFloorMaterial(Material floorMaterial) { this.floorMaterial = floorMaterial; }
    public Material getCeilingMaterial() { return ceilingMaterial; }
    public void setCeilingMaterial(Material ceilingMaterial) { this.ceilingMaterial = ceilingMaterial; }
    public Material getLightMaterial() { return lightMaterial; }
    public void setLightMaterial(Material lightMaterial) { this.lightMaterial = lightMaterial; }
    public int getLightSpacing() { return lightSpacing; }
    public void setLightSpacing(int lightSpacing) { this.lightSpacing = lightSpacing; }
    public int getCorridorWidth() { return corridorWidth; }
    public void setCorridorWidth(int corridorWidth) { this.corridorWidth = corridorWidth; }
    public double getSpecialRoomChance() { return specialRoomChance; }
    public void setSpecialRoomChance(double specialRoomChance) { this.specialRoomChance = specialRoomChance; }

    public int getHeight() { return maxY - minY; }
}

package org.derpcraft.backrooms.effects;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.derpcraft.backrooms.BackroomsPlugin;

import java.util.Set;

/**
 * Per-player task that plays a proximity-based fluorescent light buzzing sound.
 * 
 * <p>This task runs on a repeating schedule and finds the nearest flickering light source.
 * The sound is played from the light's location, allowing Minecraft's built-in distance
 * attenuation to dynamically adjust the volume as the player moves closer or further away.</p>
 * 
 * <p>By playing from the light's location (not the player's location), the volume changes
 * naturally and continuously as the player moves, creating a realistic proximity-based
 * audio effect without needing to manually calculate volume.</p>
 * 
 * <p>The effect is purely auditory - no visual changes are made.</p>
 * 
 * @see FlickerManager
 */
public class BuzzTask extends BukkitRunnable {

    /** The flicker manager that owns this task. */
    private final FlickerManager manager;

    /** The player receiving the buzz effect. */
    private final Player player;

    /** Maximum distance (in blocks) at which the buzz can be heard. */
    private static final double MAX_DISTANCE = 15.0;

    /** Volume at the sound source (Minecraft will attenuate based on distance). */
    private static final float SOURCE_VOLUME = 0.5f;

    /** Pitch for the buzz sound (1.0 = normal). */
    private static final float BUZZ_PITCH = 1.0f;

    /** Ticks between each buzz sound play (100 ticks = 5 seconds). */
    private static final long BUZZ_INTERVAL = 100L; // Play every 5 seconds to avoid overlap

    /**
     * Constructs a new buzz task for a player.
     *
     * @param manager the flicker manager
     * @param player  the player to buzz for
     */
    public BuzzTask(FlickerManager manager, Player player) {
        this.manager = manager;
        this.player = player;
    }

    @Override
    public void run() {
        if (!player.isOnline() || !player.isValid()) {
            cancel();
            return;
        }

        // Check if player is still in the backrooms world
        String backroomsWorldName = BackroomsPlugin.getInstance().getBackroomsConfig().getBackroomsWorldName();
        if (!player.getWorld().getName().equals(backroomsWorldName)) {
            cancel();
            return;
        }

        // Get all flickering lantern locations
        Set<Location> flickeringLanterns = manager.getFlickeringLanterns();
        if (flickeringLanterns.isEmpty()) {
            return;
        }

        // Find the nearest flickering lantern
        Location playerLoc = player.getLocation();
        Location nearestLight = null;
        double nearestDistance = Double.MAX_VALUE;

        for (Location lightLoc : flickeringLanterns) {
            // Only check lights in the same world
            if (lightLoc.getWorld() != player.getWorld()) {
                continue;
            }

            double distance = playerLoc.distance(lightLoc);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestLight = lightLoc;
            }
        }

        // If no light found or too far away, don't play sound
        if (nearestLight == null || nearestDistance > MAX_DISTANCE) {
            return;
        }

        // Play the buzz sound from the light's location
        // Minecraft will automatically attenuate the volume based on player distance
        player.playSound(nearestLight, "backrooms.fluorescent_buzz", SOURCE_VOLUME, BUZZ_PITCH);
    }

    /**
     * Starts the buzz task with the configured interval.
     */
    public void start() {
        runTaskTimer(manager.getPlugin(), 0L, BUZZ_INTERVAL);
    }
}

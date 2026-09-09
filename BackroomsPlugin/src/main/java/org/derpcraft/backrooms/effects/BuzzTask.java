package org.derpcraft.backrooms.effects;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.derpcraft.backrooms.BackroomsPlugin;

import java.util.Set;

/**
 * Per-player task that plays a proximity-based fluorescent light buzzing sound.
 * 
 * <p>This task runs on a repeating schedule and calculates the distance from the player
 * to the nearest flickering light source. The volume of the buzz sound increases as the
 * player gets closer to a light source, creating a realistic proximity-based audio effect.</p>
 * 
 * <p>The sound is played from the nearest light's location (not the player's location)
 * to provide spatial audio cues. Beyond a maximum distance threshold, no sound is played.</p>
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

    /** Maximum volume when player is at the light source. */
    private static final float MAX_VOLUME = 1.0f;

    /** Minimum volume when player is at maximum distance. */
    private static final float MIN_VOLUME = 0.0f;

    /** Maximum distance (in blocks) at which the buzz can be heard. */
    private static final double MAX_DISTANCE = 10.0;

    /** Distance at which maximum volume is reached (very close to light). */
    private static final double MIN_DISTANCE = 1.5;

    /** Exponential falloff power (higher = more aggressive dropoff). */
    private static final double FALLOFF_POWER = 3.0;

    /** Pitch for the buzz sound (1.0 = normal). */
    private static final float BUZZ_PITCH = 1.0f;

    /** Ticks between each buzz sound play (20 ticks = 1 second). */
    private static final long BUZZ_INTERVAL = 20L; // Play every 1 second for responsive proximity

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

        // Calculate volume based on distance (inverse relationship)
        float volume = calculateVolume(nearestDistance);

        // Play the buzz sound at the player's location with calculated volume
        // This gives us full control over what the player hears
        player.playSound(playerLoc, "backrooms.fluorescent_buzz", volume, BUZZ_PITCH);
    }

    /**
     * Calculates the volume based on distance to the nearest light source.
     * 
     * <p>Uses an exponential falloff curve for aggressive distance-based volume reduction:
     * <ul>
     *   <li>At MIN_DISTANCE or closer: MAX_VOLUME (loud and clear)</li>
     *   <li>At MAX_DISTANCE: MIN_VOLUME (inaudible)</li>
     *   <li>Between: exponential falloff (volume drops off very quickly)</li>
     * </ul>
     * 
     * <p>The exponential curve makes the sound feel more realistic - you only hear
     * the buzz when you're close to the light, and it fades rapidly as you move away.</p>
     * 
     * @param distance the distance to the nearest light source
     * @return the calculated volume (0.0 to 1.0)
     */
    private float calculateVolume(double distance) {
        if (distance <= MIN_DISTANCE) {
            return MAX_VOLUME;
        }
        if (distance >= MAX_DISTANCE) {
            return MIN_VOLUME;
        }

        // Exponential falloff: volume drops off aggressively with distance
        double ratio = (distance - MIN_DISTANCE) / (MAX_DISTANCE - MIN_DISTANCE);
        double falloff = Math.pow(1.0 - ratio, FALLOFF_POWER);
        return (float) (MIN_VOLUME + (MAX_VOLUME - MIN_VOLUME) * falloff);
    }

    /**
     * Starts the buzz task with the configured interval.
     */
    public void start() {
        runTaskTimer(manager.getPlugin(), 0L, BUZZ_INTERVAL);
    }
}

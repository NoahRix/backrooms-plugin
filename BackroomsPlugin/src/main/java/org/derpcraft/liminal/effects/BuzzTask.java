package org.derpcraft.liminal.effects;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.derpcraft.liminal.LiminalPlugin;

import java.util.Set;

/**
 * Per-player task that plays a proximity-based fluorescent light buzzing sound.
 * 
 * <p>This task periodically plays the buzz sound from the nearest flickering light's location.
 * Minecraft's built-in distance attenuation handles the volume based on the player's distance
 * from the sound source, creating a realistic proximity-based audio effect.</p>
 * 
 * @see FlickerManager
 */
public class BuzzTask extends BukkitRunnable {

    /** The flicker manager that owns this task. */
    private final FlickerManager manager;

    /** The player receiving the buzz effect. */
    private final Player player;

    /** Volume for the buzz sound at the source. */
    private static final float BUZZ_VOLUME = 0.5f;

    /** Pitch for the buzz sound (1.0 = normal). */
    private static final float BUZZ_PITCH = 1.0f;

    /** Ticks between each buzz sound play (300 ticks = 15 seconds). */
    private static final long BUZZ_INTERVAL = 300L;

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

        // Check if player is still in the liminal world
        String liminalWorldName = LiminalPlugin.getInstance().getLiminalConfig().getLiminalWorldName();
        if (!player.getWorld().getName().equals(liminalWorldName)) {
            cancel();
            return;
        }

        // Find the nearest flickering lantern to play the sound from
        Set<Location> flickeringLanterns = manager.getFlickeringLanterns();
        if (flickeringLanterns.isEmpty()) {
            return;
        }

        Location playerLoc = player.getLocation();
        Location nearestLight = null;
        double nearestDistance = Double.MAX_VALUE;

        for (Location lightLoc : flickeringLanterns) {
            if (lightLoc.getWorld() != player.getWorld()) {
                continue;
            }

            double distance = playerLoc.distance(lightLoc);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestLight = lightLoc;
            }
        }

        // Play the sound from the nearest light's location
        // Minecraft will attenuate the volume based on player distance
        if (nearestLight != null) {
            player.playSound(nearestLight, "liminal.fluorescent_buzz", BUZZ_VOLUME, BUZZ_PITCH);
        }
    }

    /**
     * Starts the buzz task.
     */
    public void start() {
        runTaskTimer(manager.getPlugin(), 0L, BUZZ_INTERVAL);
    }
}

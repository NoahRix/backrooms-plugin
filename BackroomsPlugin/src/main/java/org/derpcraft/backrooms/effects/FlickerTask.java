package org.derpcraft.backrooms.effects;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

/**
 * Per-player repeating task that creates the flickering fluorescent light effect.
 *
 * <p>Each tick, this task:</p>
 * <ol>
 *   <li>Finds flickering lanterns within the player's view distance</li>
 *   <li>Randomly selects a few to flicker</li>
 *   <li>Sends block change packets to make them appear dimmer</li>
 *   <li>Schedules restoration after a random duration (1-4 ticks)</li>
 *   <li>Optionally plays a subtle electrical sound</li>
 * </ol>
 *
 * <p>The effect is purely visual - no actual blocks are modified. When the task
 * is cancelled, all in-flight flickers are restored to their original state.</p>
 *
 * @see FlickerManager
 */
public class FlickerTask extends BukkitRunnable {

    /** The flicker manager that owns this task. */
    private final FlickerManager manager;

    /** The player receiving the flicker effect. */
    private final Player player;

    /** The dimmer block to use during flicker. */
    private final Material dimmerBlock = Material.GRAY_CONCRETE;

    /** Maximum distance (in blocks) to search for flickering lanterns. */
    private static final int RANGE = 48;

    /** Maximum number of lanterns to flicker per tick. */
    private static final int MAX_PER_TICK = 3;

    /** Probability of playing a sound when a lantern flickers (0.0-1.0). */
    private static final double SOUND_CHANCE = 0.2;

    /** Volume for ambient electrical hum (very subtle). */
    private static final float AMBIENT_VOLUME = 0.15f;

    /** Volume for flicker pop sound. */
    private static final float FLICKER_VOLUME = 0.3f;

    /** Map of locations currently mid-flicker to their original block data. */
    private final Map<Location, BlockData> activeFlickers = new HashMap<>();

    /** Random instance for this task. */
    private final Random random = new Random();

    /**
     * Constructs a new flicker task for a player.
     *
     * @param manager the flicker manager
     * @param player  the player to flicker for
     */
    public FlickerTask(FlickerManager manager, Player player) {
        this.manager = manager;
        this.player = player;
    }

    @Override
    public void run() {
        if (!player.isOnline() || !player.isValid()) {
            cancel();
            return;
        }

        // Restore any completed flickers
        restoreCompletedFlickers();

        // Find nearby flickering lanterns
        List<Location> nearby = findNearbyFlickeringLanterns();
        if (nearby.isEmpty()) {
            return;
        }

        // Randomly select some to flicker this tick
        Collections.shuffle(nearby, random);
        int count = Math.min(MAX_PER_TICK, nearby.size());

        for (int i = 0; i < count; i++) {
            Location loc = nearby.get(i);

            // Skip if already flickering
            if (activeFlickers.containsKey(loc)) {
                continue;
            }

            // Start flicker
            startFlicker(loc);
        }
    }

    /**
     * Finds all flickering lanterns within range of the player.
     *
     * @return list of nearby flickering locations
     */
    private List<Location> findNearbyFlickeringLanterns() {
        List<Location> nearby = new ArrayList<>();
        Location playerLoc = player.getLocation();

        for (Location loc : manager.getFlickeringLanterns()) {
            if (loc.getWorld() != player.getWorld()) {
                continue;
            }

            // Verify the block is still a sea lantern (hasn't been broken)
            if (loc.getBlock().getType() != Material.SEA_LANTERN) {
                continue;
            }

            double distance = loc.distanceSquared(playerLoc);
            if (distance <= RANGE * RANGE) {
                nearby.add(loc);
            }
        }

        return nearby;
    }

    /**
     * Starts a flicker effect at the given location.
     *
     * <p>Sends a block change packet to make the lantern appear dimmer, then
     * schedules restoration after a random duration.</p>
     *
     * @param loc the lantern location
     */
    private void startFlicker(Location loc) {
        // Verify the block is still a light source (sea lantern)
        if (loc.getBlock().getType() != Material.SEA_LANTERN) {
            return; // Block has been broken or changed, skip it
        }

        // Store original block data
        BlockData original = loc.getBlock().getBlockData();
        activeFlickers.put(loc, original);

        // Send dimmer block change
        player.sendBlockChange(loc, dimmerBlock.createBlockData());

        // Play flicker sound
        if (random.nextDouble() < SOUND_CHANCE) {
            player.playSound(loc, "backrooms.fluorescent_buzz", FLICKER_VOLUME, 1.0f);
        }

        // Schedule restoration (1-4 ticks)
        int duration = 1 + random.nextInt(4);
        new BukkitRunnable() {
            @Override
            public void run() {
                restoreFlicker(loc);
            }
        }.runTaskLater(manager.getPlugin(), duration);
    }

    /**
     * Restores a single flicker to its original state.
     *
     * @param loc the location to restore
     */
    private void restoreFlicker(Location loc) {
        BlockData original = activeFlickers.remove(loc);
        if (original != null) {
            player.sendBlockChange(loc, original);
        }
    }

    /**
     * Restores all active flickers to their original state.
     *
     * <p>Called when the task is cancelled to ensure no blocks are left in
     * a mid-flicker state.</p>
     */
    public void restoreAll() {
        for (Map.Entry<Location, BlockData> entry : activeFlickers.entrySet()) {
            player.sendBlockChange(entry.getKey(), entry.getValue());
        }
        activeFlickers.clear();
    }

    /**
     * Restores any flickers that have completed (no longer in activeFlickers).
     *
     * <p>This is a safety check to ensure we don't accumulate stale entries.</p>
     */
    private void restoreCompletedFlickers() {
        // This is handled by the scheduled restoration tasks
        // Just a placeholder for future cleanup logic if needed
    }
}

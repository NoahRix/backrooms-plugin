package org.derpcraft.liminal.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.minecart.RideableMinecart;
import org.bukkit.entity.minecart.StorageMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;
import org.derpcraft.liminal.LiminalPlugin;
import org.derpcraft.liminal.config.LevelConfig;
import org.derpcraft.liminal.generator.levels.LiminalLevel;
import org.derpcraft.liminal.generator.levels.Level3TheRails;

import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Spawns ghost minecarts on Level 3's powered rail network.
 *
 * <p>When a rail chunk loads for the first time (tracked via the chunk's
 * persistent data container so each chunk only ever gets one visit), the
 * listener rolls the level's {@code ghost-cart-chance} and, on success, spawns
 * an empty minecart on one of the chunk's powered rails with a small push.
 * The stable booster torches keep it rolling; from then on the cart roams the
 * network on its own. Some carts are chest carts still holding the previous
 * crew's supplies.</p>
 *
 * <p>Everything is deterministic per chunk (seeded from the world seed and the
 * chunk coordinates), so regenerating a world reproduces the same hauntings.</p>
 */
public class RailsListener implements Listener {

    private final LiminalPlugin plugin;
    private final NamespacedKey cartFlagKey;

    public RailsListener(LiminalPlugin plugin) {
        this.plugin = plugin;
        this.cartFlagKey = new NamespacedKey(plugin, "rails_carts_spawned");
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        String worldName = plugin.getLiminalConfig().getLiminalWorldName();
        if (!event.getWorld().getName().equals(worldName)) return;

        Chunk chunk = event.getChunk();

        // One visit per chunk, ever — persists across restarts.
        if (chunk.getPersistentDataContainer().has(cartFlagKey, PersistentDataType.BYTE)) return;
        chunk.getPersistentDataContainer().set(cartFlagKey, PersistentDataType.BYTE, (byte) 1);

        LevelConfig ring = plugin.getLiminalConfig().getLevelForChunk(chunk.getX(), chunk.getZ());
        if (ring == null || !ring.isEnabled() || !"level3".equals(ring.getId())) return;

        double chance = ring.getGhostCartChance();
        if (chance <= 0) return;

        LiminalLevel level = plugin.getLiminalConfig().getLevelInstanceById(ring.getId());
        if (level == null) return;
        int railY = level.getFloorSurfaceY() + 1;

        long seed = plugin.getLiminalConfig().getGenerationSeed();
        if (seed == 0) seed = event.getWorld().getSeed();
        Random random = new Random(seed
                ^ ((long) chunk.getX() * 0x1b873593L)
                ^ ((long) chunk.getZ() * 0xcc9e2d51L)
                ^ 0x7a11e57L);
        if (random.nextDouble() >= chance) return;

        // Pick a candidate powered rail cell, verifying the block actually is one
        // (broken track or terrain edits may have removed it).
        List<int[]> cells = Level3TheRails.poweredRailCells(chunk.getX(), chunk.getZ());
        Collections.shuffle(cells, random);
        for (int[] cell : cells) {
            Block block = chunk.getBlock(cell[0] & 15, railY, cell[2] & 15);
            if (block.getType() != Material.POWERED_RAIL) continue;

            final double dx = cell[3] == 0 ? (random.nextBoolean() ? 0.4 : -0.4) : 0;
            final double dz = cell[3] == 1 ? (random.nextBoolean() ? 0.4 : -0.4) : 0;
            final boolean chestCart = random.nextDouble() < 0.25;
            final Location at = new Location(event.getWorld(), cell[0] + 0.5, railY + 0.06, cell[2] + 0.5);

            // Spawn one tick later so the freshly loaded chunk is fully ready.
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!event.getWorld().isChunkLoaded(chunk.getX(), chunk.getZ())) return;
                Minecart cart = chestCart
                        ? event.getWorld().spawn(at, StorageMinecart.class)
                        : event.getWorld().spawn(at, RideableMinecart.class);
                cart.setVelocity(new Vector(dx, 0, dz));
                if (cart instanceof StorageMinecart chest) {
                    fillSupplyChest(chest, random);
                }
            });
            return; // one cart per chunk at most
        }
    }

    /** Fills a ghost chest cart with a few supplies the previous crew left behind. */
    private void fillSupplyChest(StorageMinecart cart, Random random) {
        ItemStack[] supplies = {
                new ItemStack(Material.RAIL, 4 + random.nextInt(8)),
                new ItemStack(Material.POWERED_RAIL, 2 + random.nextInt(4)),
                new ItemStack(Material.REDSTONE_TORCH, 2 + random.nextInt(4)),
                new ItemStack(Material.REDSTONE, 1 + random.nextInt(6)),
                new ItemStack(Material.COAL, 3 + random.nextInt(6)),
                new ItemStack(Material.BOOK, 1),
                new ItemStack(Material.NAME_TAG, 1),
        };
        for (ItemStack item : supplies) {
            if (random.nextDouble() < 0.5) {
                cart.getInventory().addItem(item);
            }
        }
    }
}

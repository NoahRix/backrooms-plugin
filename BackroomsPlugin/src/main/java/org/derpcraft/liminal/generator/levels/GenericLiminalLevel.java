package org.derpcraft.liminal.generator.levels;

import org.derpcraft.liminal.config.LevelConfig;

/**
 * Generic concrete level generator used for any level that has no dedicated subclass.
 *
 * <p>This makes it possible to add new levels purely through {@code config.yml}: define
 * a level entry with its ring radii and materials, and it generates with the standard
 * Liminal pipeline (floor, rooms, walls, doorways, lighting, loot chests). A dedicated
 * subclass can be introduced later for special features without changing the config.</p>
 *
 * @see LiminalLevel
 * @see Level0Lobby
 */
public class GenericLiminalLevel extends LiminalLevel {

    /**
     * Constructs a generic level generator.
     *
     * @param config the level configuration (materials, room sizes, Y bounds, ring radii)
     * @param seed   the world generation seed for deterministic output
     */
    public GenericLiminalLevel(LevelConfig config, long seed) {
        super(config, seed);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Generic levels use a floor offset of 1 so their walking surface sits directly
     * on the sub-floor base, matching the other default levels for seamless ring
     * transitions.</p>
     */
    @Override
    protected int getFloorOffset() {
        return 1;
    }
}

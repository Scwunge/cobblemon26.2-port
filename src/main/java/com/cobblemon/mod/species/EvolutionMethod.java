package com.cobblemon.mod.species;

/**
 * How a mon evolves into its next form.
 * Gen 1 uses level-up, elemental gems (stones), and trade (level proxy).
 */
public enum EvolutionMethod {
    /** Reach the linked evolution level. */
    LEVEL,
    /** Fire Stone / Fire Gem. */
    FIRE_GEM,
    /** Water Stone / Water Gem. */
    WATER_GEM,
    /** Thunder Stone / Thunder Gem. */
    THUNDER_GEM,
    /** Leaf Stone / Leaf Gem. */
    LEAF_GEM,
    /** Moon Stone / Moon Gem. */
    MOON_GEM,
    /**
     * Trade evolution. In this mod: high-level proxy (see linked level),
     * so chains still complete without a link-cable system.
     */
    TRADE,
    /** No further evolution. */
    NONE
}

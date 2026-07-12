package com.cobblemon.mod.worldgen;

import com.cobblemon.mod.Cobblemon;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * Official Cobblemon loot tables (from datapack {@code data/cobblemon/loot_table/…})
 * for feature landmarks / ruins chests.
 */
public final class CobblemonLoot {
    private CobblemonLoot() {}

    public static ResourceKey<LootTable> table(String path) {
        return ResourceKey.create(
                Registries.LOOT_TABLE,
                Identifier.fromNamespaceAndPath(Cobblemon.MOD_ID, path)
        );
    }

    /** Mini-ruin / generic structure chest. */
    public static final ResourceKey<LootTable> RUINS_COMMON = table("ruins/gilded_chests/ruins");
    public static final ResourceKey<LootTable> RUINS_BASE = table("ruins/gilded_chests/base");

    /** Tower-style deserted loot. */
    public static final ResourceKey<LootTable> DESERTED_TOWER = table("ruins/common/deserted_tower_ruins");
    public static final ResourceKey<LootTable> CRUMBLING_ARCH = table("ruins/common/crumbling_arch_ruins");
    public static final ResourceKey<LootTable> HIDDEN_BUNKER = table("ruins/common/hidden_bunker_ruins");

    /** Pokémon Center-style supplies. */
    public static final ResourceKey<LootTable> POKECENTER = table("villages/village_pokecenters");

    /** Dig / fossil-adjacent. */
    public static final ResourceKey<LootTable> FOSSIL_DEN = table("fossils/common/prehistoric_lush_den");
    public static final ResourceKey<LootTable> FOSSIL_POND = table("fossils/common/prehistoric_frozen_pond");

    /** Shipwreck-style treasure. */
    public static final ResourceKey<LootTable> SHIP_TREASURE = table("shipwreck_coves/gilded_chests/lesser_treasure");

    public static ResourceKey<LootTable> pickRuin(RandomSource random) {
        if (random == null) {
            return RUINS_COMMON;
        }
        float r = random.nextFloat();
        if (r < 0.35f) {
            return RUINS_COMMON;
        }
        if (r < 0.55f) {
            return DESERTED_TOWER;
        }
        if (r < 0.75f) {
            return CRUMBLING_ARCH;
        }
        if (r < 0.9f) {
            return HIDDEN_BUNKER;
        }
        return RUINS_BASE;
    }

    public static ResourceKey<LootTable> pickCenter(RandomSource random) {
        return POKECENTER;
    }

    public static ResourceKey<LootTable> pickTower(RandomSource random) {
        return random != null && random.nextBoolean() ? DESERTED_TOWER : CRUMBLING_ARCH;
    }

    public static ResourceKey<LootTable> pickDigSite(RandomSource random) {
        return random != null && random.nextBoolean() ? FOSSIL_DEN : FOSSIL_POND;
    }
}

package com.cobblemon.mod.species;

import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;

/**
 * Cobblemon-inspired spawn buckets. A spawn attempt first rolls a bucket,
 * then picks a species inside that bucket by relative weight (typically 0.1–10).
 */
public enum SpawnRarity implements StringRepresentable {
    /** Everyday filler — Cobblemon best-spawner ~94.3%. */
    COMMON("common", 94.3f),
    /** Habitat specialists and mid-stage forms (~5%). */
    UNCOMMON("uncommon", 5.0f),
    /** Strong mid/final stages and scarce lines (~0.5%). */
    RARE("rare", 0.5f),
    /** Starters in the wild, pseudo-legendaries (~0.2%). */
    ULTRA_RARE("ultra_rare", 0.2f),
    /** True legendaries — tiny share when any entry exists. */
    LEGENDARY("legendary", 0.05f);

    private final String id;
    /** Relative chance this bucket is selected when eligible species exist. */
    private final float bucketChance;

    SpawnRarity(String id, float bucketChance) {
        this.id = id;
        this.bucketChance = bucketChance;
    }

    public float bucketChance() {
        return bucketChance;
    }

    /**
     * Weighted bucket pick among buckets that still have candidates.
     */
    public static SpawnRarity pickBucket(RandomSource random, java.util.EnumSet<SpawnRarity> available) {
        if (available == null || available.isEmpty()) {
            return COMMON;
        }
        float total = 0f;
        for (SpawnRarity r : available) {
            total += r.bucketChance;
        }
        if (total <= 0f) {
            return available.iterator().next();
        }
        float roll = random.nextFloat() * total;
        for (SpawnRarity r : values()) {
            if (!available.contains(r)) {
                continue;
            }
            roll -= r.bucketChance;
            if (roll <= 0f) {
                return r;
            }
        }
        // Fallback: last available in enum order
        SpawnRarity last = COMMON;
        for (SpawnRarity r : values()) {
            if (available.contains(r)) {
                last = r;
            }
        }
        return last;
    }

    @Override
    public String getSerializedName() {
        return id;
    }
}

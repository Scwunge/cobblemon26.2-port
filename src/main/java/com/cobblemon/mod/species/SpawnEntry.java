package com.cobblemon.mod.species;

/**
 * One Cobblemon-style wild spawn rule (Gen 1 import).
 */
public final class SpawnEntry {
    public enum Position { GROUNDED, SUBMERGED, SURFACE }
    public enum TimeMode { ANY, DAY, NIGHT }

    public final String speciesId;
    public final SpawnRarity bucket;
    public final float weight;
    public final int levelMin;
    public final int levelMax;
    public final Position position;
    public final String[] exactBiomes;
    public final String[] biomeKeywords;
    public final boolean anyOverworld;
    public final boolean overworld;
    public final boolean nether;
    public final boolean end;
    public final String[] antiExact;
    public final String[] antiKeywords;
    public final int minSkyLight; // -1 = ignore
    public final int maxSkyLight;
    public final int minY;
    public final int maxY;
    public final int canSeeSky; // -1 ignore, 0 false, 1 true
    public final TimeMode timeMode;
    public final boolean noRain;

    public SpawnEntry(
            String speciesId,
            SpawnRarity bucket,
            float weight,
            int levelMin,
            int levelMax,
            Position position,
            String[] exactBiomes,
            String[] biomeKeywords,
            boolean anyOverworld,
            boolean overworld,
            boolean nether,
            boolean end,
            String[] antiExact,
            String[] antiKeywords,
            int minSkyLight,
            int maxSkyLight,
            int minY,
            int maxY,
            int canSeeSky,
            TimeMode timeMode,
            boolean noRain
    ) {
        this.speciesId = speciesId;
        this.bucket = bucket;
        this.weight = weight;
        this.levelMin = levelMin;
        this.levelMax = levelMax;
        this.position = position;
        this.exactBiomes = exactBiomes;
        this.biomeKeywords = biomeKeywords;
        this.anyOverworld = anyOverworld;
        this.overworld = overworld;
        this.nether = nether;
        this.end = end;
        this.antiExact = antiExact;
        this.antiKeywords = antiKeywords;
        this.minSkyLight = minSkyLight;
        this.maxSkyLight = maxSkyLight;
        this.minY = minY;
        this.maxY = maxY;
        this.canSeeSky = canSeeSky;
        this.timeMode = timeMode;
        this.noRain = noRain;
    }
}

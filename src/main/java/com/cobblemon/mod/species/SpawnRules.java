package com.cobblemon.mod.species;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.material.Fluids;

/**
 * Cobblemon-aligned wild species selection using imported Gen 1 spawn pools
 * ({@link Gen1SpawnPool} from {@code porter/11_data_spawning}).
 * <ol>
 *   <li>Filter pool entries by biome / light / time / position / dimension</li>
 *   <li>Roll a rarity bucket (Cobblemon weights)</li>
 *   <li>Pick a weighted entry inside that bucket</li>
 * </ol>
 */
public final class SpawnRules {
    private SpawnRules() {}

    public record Pick(MonSpecies species, int level, SpawnEntry.Position position, String speciesId) {
        public Pick(MonSpecies species, int level, SpawnEntry.Position position) {
            this(species, level, position, species == null ? "rattata" : species.id());
        }
    }

    public static MonSpecies pickSpecies(Level level, BlockPos pos, RandomSource random) {
        return pick(level, pos, random).species();
    }

    public static Pick pick(Level level, BlockPos pos, RandomSource random) {
        SpawnContext ctx = SpawnContext.from(level, pos);

        Map<SpawnRarity, List<SpawnEntry>> byBucket = new EnumMap<>(SpawnRarity.class);
        for (SpawnRarity r : SpawnRarity.values()) {
            byBucket.put(r, new ArrayList<>());
        }

        for (SpawnEntry e : SpawnPoolLoader.entries()) {
            if (!matches(e, ctx)) {
                continue;
            }
            // Any known datapack species id (Gen1 enum or full national dex registry)
            SpeciesHandle handle = SpeciesHandle.of(e.speciesId);
            if (SpeciesRegistry.get(handle.id()).isEmpty() && handle.asEnum().isEmpty()) {
                continue;
            }
            // Skip mons without real Cobblemon models
            if (DisabledSpecies.isDisabled(handle.id())) {
                continue;
            }
            byBucket.get(e.bucket).add(e);
        }

        EnumSet<SpawnRarity> available = EnumSet.noneOf(SpawnRarity.class);
        for (SpawnRarity r : SpawnRarity.values()) {
            if (!byBucket.get(r).isEmpty()) {
                available.add(r);
            }
        }

        if (available.isEmpty()) {
            // Soft fallback: common overworld filler only
            MonSpecies fallback = ctx.waterish ? MonSpecies.MAGIKARP : MonSpecies.RATTATA;
            int lvl = 3 + random.nextInt(8);
            return new Pick(fallback, lvl, ctx.waterish ? SpawnEntry.Position.SUBMERGED : SpawnEntry.Position.GROUNDED);
        }

        SpawnRarity bucket = SpawnRarity.pickBucket(random, available);
        List<SpawnEntry> pool = byBucket.get(bucket);
        if (pool == null || pool.isEmpty()) {
            for (SpawnRarity r : SpawnRarity.values()) {
                if (!byBucket.get(r).isEmpty()) {
                    pool = byBucket.get(r);
                    break;
                }
            }
        }
        if (pool == null || pool.isEmpty()) {
            return new Pick(MonSpecies.RATTATA, 5, SpawnEntry.Position.GROUNDED);
        }

        SpawnEntry chosen = pickWeighted(pool, random);
        SpeciesHandle handle = SpeciesHandle.of(chosen.speciesId);
        // Prefer real Gen1 enum when available; otherwise type-matched fallback for entity systems
        MonSpecies species = handle.asEnumOrFallback();
        int monLevel = rollLevel(chosen, random);
        return new Pick(species, monLevel, chosen.position, handle.id());
    }

    public static int rollLevel(MonSpecies species, RandomSource random) {
        // Legacy API — prefer entry-based levels via pick()
        if (species.isLegendary()) {
            if (species == MonSpecies.MEWTWO) return 40 + random.nextInt(21);
            if (species == MonSpecies.MEW) return 55 + random.nextInt(21);
            return 40 + random.nextInt(21);
        }
        int base = 3 + random.nextInt(10);
        if (species.stage() >= 2) base += 8;
        if (species.stage() >= 3) base += 12;
        return Math.min(OwnedMon.MAX_LEVEL, base);
    }

    public static int rollLevel(SpawnEntry entry, RandomSource random) {
        int min = Math.max(1, entry.levelMin);
        int max = Math.max(min, entry.levelMax);
        return min + random.nextInt(max - min + 1);
    }

    public static boolean canDespawnWild(boolean companion, long ticksAlive, MonSpecies species) {
        if (companion) {
            return false;
        }
        long limit = species.wildDespawnSeconds() * 20L;
        return ticksAlive > limit;
    }

    private static boolean matches(SpawnEntry e, SpawnContext ctx) {
        // Dimension gates
        if (ctx.nether) {
            if (e.end) return false;
            // Nether: need nether flag or basalt/nether biomes in the entry
            boolean netherOk = e.nether
                    || exactHit(e.exactBiomes, ctx.biomePath)
                    || keywordHit(e.biomeKeywords, "basalt")
                    || keywordHit(e.biomeKeywords, "nether");
            if (!netherOk) return false;
        } else if (ctx.end) {
            if (!e.end) return false;
        } else {
            // Overworld — skip pure nether-only / end-only entries
            if (e.nether && !e.overworld) return false;
            if (e.end) return false;
        }

        // Position type
        if (e.position == SpawnEntry.Position.SUBMERGED || e.position == SpawnEntry.Position.SURFACE) {
            if (!ctx.waterish) return false;
        } else {
            // grounded — prefer dry feet; allow swampy edges
            if (ctx.fullySubmerged) return false;
        }

        // Y range
        if (ctx.y < e.minY || ctx.y > e.maxY) return false;

        // Sky light
        if (e.minSkyLight >= 0 && ctx.skyLight < e.minSkyLight) return false;
        if (e.maxSkyLight >= 0 && ctx.skyLight > e.maxSkyLight) return false;

        // canSeeSky proxy via sky light
        if (e.canSeeSky == 1 && ctx.skyLight < 8) return false;
        if (e.canSeeSky == 0 && ctx.skyLight >= 12) return false;

        // Time
        if (e.timeMode == SpawnEntry.TimeMode.NIGHT && !ctx.night) return false;
        if (e.timeMode == SpawnEntry.TimeMode.DAY && ctx.night) return false;

        // Rain
        if (e.noRain && ctx.raining) return false;

        // Anti-biomes first
        if (exactHit(e.antiExact, ctx.biomePath)) return false;
        if (keywordHit(e.antiKeywords, ctx.biomePath)) return false;

        // Biome match
        if (e.anyOverworld) {
            return !ctx.nether && !ctx.end;
        }
        if (exactHit(e.exactBiomes, ctx.biomePath)) {
            return true;
        }
        if (keywordHit(e.biomeKeywords, ctx.biomePath)) {
            return true;
        }
        // Nether basalt special-case: volcanic keyword pool
        if (ctx.nether && e.nether) {
            return true;
        }
        return false;
    }

    private static boolean exactHit(String[] exact, String biomePath) {
        if (exact == null || exact.length == 0) return false;
        for (String b : exact) {
            if (b == null) continue;
            if (biomePath.equals(b) || biomePath.endsWith("/" + b)) return true;
        }
        return false;
    }

    private static boolean keywordHit(String[] keywords, String biomePath) {
        if (keywords == null || keywords.length == 0) return false;
        String path = biomePath == null ? "" : biomePath.toLowerCase(Locale.ROOT).replace(' ', '_');
        for (String k : keywords) {
            if (k == null || k.isEmpty()) continue;
            String key = k.toLowerCase(Locale.ROOT).replace(' ', '_');
            // Porter stores tags like "forest" / "jungle" — match path fragments
            if (path.contains(key) || key.contains(path)) return true;
            // Cobblemon-style is_forest → forest
            if (key.startsWith("is_") && path.contains(key.substring(3))) return true;
        }
        return false;
    }

    private static SpawnEntry pickWeighted(List<SpawnEntry> pool, RandomSource random) {
        float total = 0f;
        for (SpawnEntry e : pool) {
            total += Math.max(0.01f, e.weight);
        }
        float roll = random.nextFloat() * total;
        for (SpawnEntry e : pool) {
            roll -= Math.max(0.01f, e.weight);
            if (roll <= 0f) {
                return e;
            }
        }
        return pool.get(pool.size() - 1);
    }

    /** Local habitat snapshot — no canSeeSky / cross-chunk heightmaps. */
    private static final class SpawnContext {
        final String biomePath;
        final boolean night;
        final boolean raining;
        final boolean waterish;
        final boolean fullySubmerged;
        final boolean nether;
        final boolean end;
        final int skyLight;
        final int y;

        private SpawnContext(
                String biomePath,
                boolean night,
                boolean raining,
                boolean waterish,
                boolean fullySubmerged,
                boolean nether,
                boolean end,
                int skyLight,
                int y
        ) {
            this.biomePath = biomePath;
            this.night = night;
            this.raining = raining;
            this.waterish = waterish;
            this.fullySubmerged = fullySubmerged;
            this.nether = nether;
            this.end = end;
            this.skyLight = skyLight;
            this.y = y;
        }

        static SpawnContext from(Level level, BlockPos pos) {
            String biomePath = "plains";
            try {
                biomePath = level.getBiome(pos).unwrapKey()
                        .map(k -> k.identifier().getPath())
                        .orElse("plains")
                        .toLowerCase(Locale.ROOT);
            } catch (Exception ignored) {
            }

            long dayTime;
            try {
                dayTime = level.getOverworldClockTime() % 24000L;
            } catch (Exception e) {
                dayTime = 6000L;
            }
            boolean night = dayTime >= 13000L && dayTime < 23000L;

            boolean raining = false;
            try {
                raining = level.isRainingAt(pos);
            } catch (Exception ignored) {
            }

            boolean waterish = false;
            boolean fullySubmerged = false;
            try {
                var fluid = level.getFluidState(pos);
                var fluidBelow = level.getFluidState(pos.below());
                fullySubmerged = fluid.is(Fluids.WATER) || fluid.is(Fluids.FLOWING_WATER);
                waterish = fullySubmerged
                        || fluidBelow.is(Fluids.WATER)
                        || fluidBelow.is(Fluids.FLOWING_WATER)
                        || biomePath.contains("ocean")
                        || biomePath.contains("river")
                        || biomePath.contains("beach")
                        || biomePath.contains("swamp");
            } catch (Exception ignored) {
                waterish = biomePath.contains("ocean") || biomePath.contains("river");
            }

            boolean nether = level.dimension() == Level.NETHER;
            boolean end = level.dimension() == Level.END;

            int skyLight = 15;
            try {
                skyLight = level.getBrightness(LightLayer.SKY, pos);
            } catch (Exception ignored) {
            }

            return new SpawnContext(
                    biomePath, night, raining, waterish, fullySubmerged, nether, end, skyLight, pos.getY()
            );
        }
    }
}

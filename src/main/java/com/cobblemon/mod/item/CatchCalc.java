package com.cobblemon.mod.item;

import com.cobblemon.mod.species.MonSpecies;
import com.cobblemon.mod.species.MonStatus;
import com.cobblemon.mod.species.OwnedMon;
import com.cobblemon.mod.species.SpeciesHandle;
import com.cobblemon.mod.species.SpawnRarity;
import net.minecraft.util.RandomSource;

/**
 * Mainline / Cobblemon-style capture math (Gen 3+ shake model).
 * <p>
 * {@code a = ((3M − 2H) × catchRate × ball × status) / (3M)}
 * then up to 4 shake checks from {@code b = 65536 / √(√(255/a))}.
 */
public final class CatchCalc {
    private CatchCalc() {}

    /** Result of a capture attempt — how many shakes succeed, and if fully caught. */
    public record Result(boolean caught, int shakesSucceeded, int shakesRequired, float aValue) {
        public static Result guaranteed() {
            return new Result(true, 4, 4, 255f);
        }

        public static Result failed(int shakes, float a) {
            return new Result(false, shakes, 4, a);
        }
    }

    public static float catchChance(float hpRatio, MonSpecies species, CubeBallTier tier) {
        return catchChance(hpRatio, species == null ? "rattata" : species.id(), tier, MonStatus.NONE);
    }

    public static float catchChance(float hpRatio, String speciesId, CubeBallTier tier) {
        return catchChance(hpRatio, speciesId, tier, MonStatus.NONE);
    }

    /** Approximate single-roll success probability (UI / battle log). */
    public static float catchChance(float hpRatio, String speciesId, CubeBallTier tier, MonStatus status) {
        float a = computeA(hpRatio, speciesId, tier, status);
        if (a >= 255f) {
            return 1.0f;
        }
        if (a <= 0f) {
            return 0.02f;
        }
        // P(all 4 shakes) ≈ (b/65536)^4
        float b = shakeThreshold(a);
        float p = b / 65536f;
        float all = p * p * p * p;
        return Math.max(0.01f, Math.min(0.99f, all));
    }

    public static Result roll(
            float hpRatio,
            String speciesId,
            CubeBallTier tier,
            MonStatus status,
            RandomSource random
    ) {
        return roll(hpRatio, speciesId, tier, status, random, null);
    }

    public static Result roll(
            float hpRatio,
            String speciesId,
            CubeBallTier tier,
            MonStatus status,
            RandomSource random,
            BallContext context
    ) {
        CubeBallTier ball = tier != null ? tier : CubeBallTier.POKE;
        if (ball.catchMultiplier() >= 100f) {
            return Result.guaranteed();
        }
        float a = computeA(hpRatio, speciesId, ball, status, context);
        if (a >= 255f) {
            return Result.guaranteed();
        }
        float b = shakeThreshold(a);
        int ok = 0;
        for (int i = 0; i < 4; i++) {
            int roll = random.nextInt(65536);
            if (roll < b) {
                ok++;
            } else {
                return Result.failed(ok, a);
            }
        }
        return new Result(true, 4, 4, a);
    }

    /** Capture value a (0–255+). */
    public static float computeA(float hpRatio, String speciesId, CubeBallTier tier, MonStatus status) {
        return computeA(hpRatio, speciesId, tier, status, null);
    }

    public static float computeA(
            float hpRatio, String speciesId, CubeBallTier tier, MonStatus status, BallContext context
    ) {
        hpRatio = Math.max(0.01f, Math.min(1.0f, hpRatio));
        CubeBallTier ball = tier != null ? tier : CubeBallTier.POKE;
        float mult = context != null ? context.multiplierFor(ball) : ball.catchMultiplier();
        if (mult >= 100f) {
            return 255f;
        }
        // Beast without context still uses base handling
        if (context == null && ball == CubeBallTier.BEAST) {
            SpeciesHandle handle = SpeciesHandle.of(speciesId);
            boolean target = false;
            var data = com.cobblemon.mod.species.SpeciesRegistry.get(handle.id()).orElse(null);
            if (data != null && data.labels() != null) {
                for (String l : data.labels()) {
                    String s = l.toLowerCase();
                    if (s.contains("ultra_beast") || s.contains("paradox") || s.contains("beast")) {
                        target = true;
                        break;
                    }
                }
            }
            mult = target ? 5.0f : 0.1f;
        }

        int catchRate = speciesCatchRate(speciesId);
        float statusMod = statusMultiplier(status);
        float a = ((3.0f - 2.0f * hpRatio) * catchRate * mult * statusMod) / 3.0f;
        return Math.max(1f, a);
    }

    /** Post-catch ball effects (Heal Ball, Friend Ball, etc.). */
    public static OwnedMon applyCatchEffects(OwnedMon mon, CubeBallTier tier) {
        if (mon == null || tier == null) {
            return mon;
        }
        return switch (tier) {
            case HEAL -> mon.healed();
            case FRIEND, LUXURY -> mon; // friendship not tracked fully; keep mon as-is
            default -> mon;
        };
    }

    private static float shakeThreshold(float a) {
        // b = 65536 / (255/a)^0.25
        if (a >= 255f) {
            return 65536f;
        }
        double inner = Math.sqrt(Math.sqrt(255.0 / Math.max(1.0, a)));
        return (float) (65536.0 / Math.max(1.0, inner));
    }

    public static float statusMultiplier(MonStatus status) {
        if (status == null || status.isNone()) {
            return 1.0f;
        }
        return switch (status) {
            case SLEEP, FREEZE -> 2.5f;
            case PARALYSIS, POISON, BURN -> 1.5f;
            default -> 1.0f;
        };
    }

    public static int speciesCatchRate(String speciesId) {
        SpeciesHandle handle = SpeciesHandle.of(speciesId);
        int rate = handle.catchRate();
        if (rate > 0) {
            return Math.max(3, Math.min(255, rate));
        }
        return speciesCatchRate(handle.asEnumOrFallback());
    }

    public static int speciesCatchRate(MonSpecies species) {
        if (species == null) {
            return 190;
        }
        var six = com.cobblemon.mod.species.CobblemonBaseStats.of(species);
        if (six != null) {
            return Math.max(3, Math.min(255, six.catchRate()));
        }
        if (species.isLegendary()) {
            return species == MonSpecies.MEW ? 3
                    : species == MonSpecies.MEWTWO ? 8
                    : 15;
        }
        if (species.spawnRarity() == SpawnRarity.ULTRA_RARE) {
            return 45;
        }
        if (species.spawnRarity() == SpawnRarity.RARE || species.stage() >= 3) {
            return 60;
        }
        if (species.stage() == 2) {
            return 90;
        }
        if (species.isStarter()) {
            return 45;
        }
        return switch (species.spawnRarity()) {
            case UNCOMMON -> 120;
            default -> 190;
        };
    }
}

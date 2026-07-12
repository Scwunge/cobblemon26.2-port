package com.cobblemon.mod.battle;

import com.cobblemon.mod.species.Ability;
import com.cobblemon.mod.species.HeldItems;
import com.cobblemon.mod.species.MonElement;
import com.cobblemon.mod.species.MonSpecies;
import com.cobblemon.mod.species.Nature;
import com.cobblemon.mod.species.OwnedMon;
import com.cobblemon.mod.species.StatBlock;
import com.cobblemon.mod.species.MonForm;
import net.minecraft.util.RandomSource;

/**
 * Damage formula using full stat block + nature + ability hooks.
 */
public final class BattleCalc {
    private BattleCalc() {}

    public static int attackStat(MonSpecies species, int level) {
        return StatBlock.compute(species, level, Nature.HARDY, MonForm.NORMAL).atk();
    }

    public static int defenseStat(MonSpecies species, int level) {
        return StatBlock.compute(species, level, Nature.HARDY, MonForm.NORMAL).def();
    }

    public static int maxHp(MonSpecies species, int level) {
        return species.maxHpForLevel(level);
    }

    public static int attackStat(OwnedMon mon) {
        StatBlock s = mon.stats();
        return s.atk();
    }

    public static int specialAttackStat(OwnedMon mon) {
        return mon.stats().spAtk();
    }

    public static int defenseStat(OwnedMon mon) {
        return mon.stats().def();
    }

    public static int specialDefenseStat(OwnedMon mon) {
        return mon.stats().spDef();
    }

    public static int speedStat(OwnedMon mon) {
        return mon.stats().speed();
    }

    public static DamageResult useMove(
            MonMove move,
            MonSpecies atkSpecies,
            int atkLevel,
            MonSpecies defSpecies,
            int defLevel,
            RandomSource random,
            boolean focused
    ) {
        // Legacy path — neutral nature
        OwnedMon fakeAtk = OwnedMon.createWild(atkSpecies, atkLevel);
        OwnedMon fakeDef = OwnedMon.createWild(defSpecies, defLevel);
        return useMove(move, fakeAtk, fakeDef, random, focused);
    }

    public static DamageResult useMove(
            MonMove move,
            OwnedMon attacker,
            OwnedMon defender,
            RandomSource random,
            boolean focused
    ) {
        if (move.isStatus()) {
            return applyStatus(move);
        }

        // Accuracy check (0–100), held Wide Lens etc.
        int acc = Math.max(1, Math.min(100, Math.round(move.accuracy() * HeldItems.accuracyMul(attacker.heldItem()))));
        if (random.nextInt(100) >= acc) {
            return DamageResult.miss(move);
        }

        // Use datapack types (full dex) — not Gen1 enum stand-in
        MonElement defType = defender.primaryType();
        float typeMult = TypeChart.multiplier(move.element(), defType);
        if (defender.secondaryType().isPresent()) {
            float m2 = TypeChart.multiplier(move.element(), defender.secondaryType().get());
            typeMult = typeMult * m2;
        }

        Ability defAb = defender.ability();
        float defMul = defAb.defensiveBoost(move.element());
        if (defMul <= 0f) {
            return DamageResult.immune(move);
        }
        typeMult *= defMul;

        if (move == MonMove.COSMIC_QUERY && typeMult > 0f && typeMult < 1f) {
            typeMult = 1.0f;
        }
        if (attacker.ability() == Ability.MAXIMUM_TRUTH && move.element() == MonElement.PSYCHIC
                && typeMult > 0f && typeMult < 1f) {
            typeMult = Math.min(1.0f, typeMult + 0.25f);
        }
        if (typeMult <= 0f) {
            return DamageResult.immune(move);
        }

        boolean special = move.category() == MoveCategory.SPECIAL;
        int atk = special ? specialAttackStat(attacker) : attackStat(attacker);
        int def = special ? specialDefenseStat(defender) : defenseStat(defender);

        // Status: burn halves physical attack; paralysis halves speed (used elsewhere)
        if (!special) {
            atk = Math.max(1, Math.round(atk * attacker.status().physicalAtkMul()));
        }

        // Held items
        String held = attacker.heldItem();
        if (special) {
            atk = Math.max(1, Math.round(atk * HeldItems.specialAtkMul(held)));
            def = Math.max(1, Math.round(def * HeldItems.specialDefMul(defender.heldItem())));
        } else {
            atk = Math.max(1, Math.round(atk * HeldItems.physicalAtkMul(held)));
            def = Math.max(1, Math.round(def * HeldItems.physicalDefMul(defender.heldItem())));
        }
        // Speed-affecting items don't change damage but choice scarf is speed-only

        if (focused) {
            atk = (int) (atk * 1.35f);
        }

        float stab = move.element() == attacker.primaryType() ? 1.5f : 1.0f;
        if (attacker.secondaryType().isPresent()
                && move.element() == attacker.secondaryType().get()) {
            stab = 1.5f;
        }
        float abOff = attacker.ability().offensiveBoost(move.element(), attacker.hpRatio());
        boolean superEff = typeMult > 1f;
        float heldOff = HeldItems.outgoingDamageMul(held, superEff, stab >= 1.5f);
        int power = Math.max(1, move.power());
        int level = attacker.level();

        float base = ((2f * level / 5f + 2f) * power * atk / Math.max(1f, def)) / 50f + 2f;
        // Showdown-style damage roll 85–100%
        float roll = 0.85f + random.nextFloat() * 0.15f;
        // Critical: ~1/24 base (Gen 6+ stage 0); high-priority / focused hits slightly better
        int critBonus = HeldItems.critStageBonus(held);
        int critDenom = focused || move.priority() > 0 || critBonus > 0 ? 8 : 24;
        if (critBonus > 1) {
            critDenom = 4;
        }
        boolean crit = random.nextInt(Math.max(2, critDenom)) == 0;
        float critMul = crit ? 1.5f : 1f;

        // Multi-hit lite: moves with "double", "fury", "barrage", "pin_missile" style ids hit 2–3 times
        int hits = multiHitCount(move, random);
        int dmg = 0;
        for (int h = 0; h < hits; h++) {
            float hitRoll = 0.85f + random.nextFloat() * 0.15f;
            dmg += Math.max(1, Math.round(base * stab * typeMult * abOff * heldOff * hitRoll * critMul));
        }

        // Chance to apply a simple primary status from contact-ish moves
        StatusEffect inflict = null;
        if (!crit && dmg > 0 && random.nextFloat() < statusProcChance(move)) {
            inflict = statusFromMove(move);
        }
        // unused field carries hit count for multi-hit logging
        return new DamageResult(move, dmg, typeMult, false, false, inflict, hits, crit);
    }

    private static int multiHitCount(MonMove move, RandomSource random) {
        String id = move.id();
        if (id.contains("double") || id.contains("dual") || id.contains("twin")) {
            return 2;
        }
        if (id.contains("fury") || id.contains("barrage") || id.contains("missile") || id.contains("spike")) {
            return 2 + random.nextInt(2); // 2–3
        }
        return 1;
    }

    private static float statusProcChance(MonMove move) {
        if (move == null) {
            return 0f;
        }
        // Rough Showdown-like secondary rates for our simplified move set
        String id = move.id();
        if (id.contains("thunder") || id.contains("shock") || id.contains("spark")) {
            return 0.10f;
        }
        if (id.contains("ember") || id.contains("flame") || id.contains("fire") || id.contains("burn")) {
            return 0.10f;
        }
        if (id.contains("poison") || id.contains("toxic") || id.contains("sludge")) {
            return 0.30f;
        }
        if (id.contains("ice") || id.contains("freeze") || id.contains("blizzard")) {
            return 0.10f;
        }
        if (id.contains("sleep") || id.contains("hypnosis") || id.contains("spore")) {
            return 0.0f; // pure status handled elsewhere
        }
        return 0f;
    }

    private static StatusEffect statusFromMove(MonMove move) {
        String id = move.id();
        if (id.contains("thunder") || id.contains("shock") || id.contains("spark")) {
            return StatusEffect.PARALYZE;
        }
        if (id.contains("ember") || id.contains("flame") || id.contains("fire") || id.contains("burn")) {
            return StatusEffect.BURN;
        }
        if (id.contains("poison") || id.contains("toxic") || id.contains("sludge")) {
            return StatusEffect.POISON;
        }
        if (id.contains("ice") || id.contains("freeze") || id.contains("blizzard")) {
            return StatusEffect.FREEZE;
        }
        return null;
    }

    private static DamageResult applyStatus(MonMove move) {
        return switch (move) {
            case REST_SOFT -> new DamageResult(move, 0, 1f, false, false, StatusEffect.HEAL_SELF, 0);
            case HARDEN -> new DamageResult(move, 0, 1f, false, false, StatusEffect.DEF_UP, 0);
            case FOCUS, ASK_EVERYTHING -> new DamageResult(move, 0, 1f, false, false, StatusEffect.FOCUS, 0);
            case GROWL -> new DamageResult(move, 0, 1f, false, false, StatusEffect.ATK_DOWN, 0);
            default -> {
                // Named status moves
                String id = move.id();
                if (id.contains("sleep") || id.contains("hypnosis") || id.contains("spore") || id.contains("sing")) {
                    yield new DamageResult(move, 0, 1f, false, false, StatusEffect.SLEEP, 0);
                }
                if (id.contains("toxic") || id.contains("poison")) {
                    yield new DamageResult(move, 0, 1f, false, false, StatusEffect.POISON, 0);
                }
                if (id.contains("thunder_wave") || id.contains("stun") || id.contains("glare")) {
                    yield new DamageResult(move, 0, 1f, false, false, StatusEffect.PARALYZE, 0);
                }
                if (id.contains("will.o") || id.contains("willowisp") || id.contains("will_o")) {
                    yield new DamageResult(move, 0, 1f, false, false, StatusEffect.BURN, 0);
                }
                yield new DamageResult(move, 0, 1f, false, false, null, 0);
            }
        };
    }

    public static int expForWin(int winnerLevel, int foeLevel, int foeStage) {
        // Closer to scaled mainline: base × level factors
        int base = 30 + foeLevel * 10 + foeStage * 12;
        if (winnerLevel < foeLevel) {
            base += (foeLevel - winnerLevel) * 4;
        } else if (winnerLevel > foeLevel + 5) {
            base = Math.max(15, base - (winnerLevel - foeLevel) * 2);
        }
        return Math.max(10, base);
    }

    public enum StatusEffect {
        HEAL_SELF,
        DEF_UP,
        ATK_DOWN,
        FOCUS,
        BURN,
        POISON,
        PARALYZE,
        SLEEP,
        FREEZE
    }

    public record DamageResult(
            MonMove move,
            int damage,
            float typeMult,
            boolean missed,
            boolean immune,
            StatusEffect status,
            int unused,
            boolean critical
    ) {
        public DamageResult(MonMove move, int damage, float typeMult, boolean missed, boolean immune,
                            StatusEffect status, int unused) {
            this(move, damage, typeMult, missed, immune, status, unused, false);
        }

        public static DamageResult miss(MonMove move) {
            return new DamageResult(move, 0, 1f, true, false, null, 0, false);
        }

        public static DamageResult immune(MonMove move) {
            return new DamageResult(move, 0, 0f, false, true, null, 0, false);
        }
    }
}

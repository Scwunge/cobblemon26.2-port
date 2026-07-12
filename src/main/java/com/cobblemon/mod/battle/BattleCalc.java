package com.cobblemon.mod.battle;

import com.cobblemon.mod.species.Ability;
import com.cobblemon.mod.species.HeldItems;
import com.cobblemon.mod.species.MonElement;
import com.cobblemon.mod.species.MonForm;
import com.cobblemon.mod.species.MonSpecies;
import com.cobblemon.mod.species.MonStatus;
import com.cobblemon.mod.species.Nature;
import com.cobblemon.mod.species.OwnedMon;
import com.cobblemon.mod.species.StatBlock;
import net.minecraft.util.RandomSource;

/**
 * Damage formula using full stat block + nature + ability + {@link BattleStages}.
 * Move stats prefer official Showdown data via {@link BattleMove}.
 */
public final class BattleCalc {
    /** Optional field context for weather/terrain multipliers during damage calc. */
    private static final ThreadLocal<BattleFieldEffects> FIELD_CTX = new ThreadLocal<>();

    public static void pushField(BattleFieldEffects field) {
        FIELD_CTX.set(field);
    }

    public static void popField() {
        FIELD_CTX.remove();
    }

    public static DamageResult useMove(
            BattleMove move,
            OwnedMon attacker,
            OwnedMon defender,
            BattleStages atkStages,
            BattleStages defStages,
            RandomSource random,
            boolean focused,
            BattleFieldEffects field
    ) {
        pushField(field);
        try {
            return useMove(move, attacker, defender, atkStages, defStages, random, focused);
        } finally {
            popField();
        }
    }
    private BattleCalc() {}

    public static int attackStat(OwnedMon mon) {
        return mon.stats().atk();
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

    public static int speedStat(OwnedMon mon, BattleStages stages) {
        int base = speedStat(mon);
        float mul = stages != null ? stages.multiplier(StatKind.SPE) : 1f;
        if (mon.status() == MonStatus.PARALYSIS) {
            mul *= 0.5f;
        }
        return Math.max(1, Math.round(base * mul));
    }

    public static DamageResult useMove(
            MonMove move,
            OwnedMon attacker,
            OwnedMon defender,
            RandomSource random,
            boolean focused
    ) {
        return useMove(BattleMove.of(move), attacker, defender, null, null, random, focused);
    }

    public static DamageResult useMove(
            BattleMove move,
            OwnedMon attacker,
            OwnedMon defender,
            RandomSource random,
            boolean focused
    ) {
        return useMove(move, attacker, defender, null, null, random, focused);
    }

    /**
     * @param atkStages attacker stages (nullable → neutral)
     * @param defStages defender stages (nullable → neutral)
     */
    public static DamageResult useMove(
            BattleMove move,
            OwnedMon attacker,
            OwnedMon defender,
            BattleStages atkStages,
            BattleStages defStages,
            RandomSource random,
            boolean focused
    ) {
        BattleStages aSt = atkStages != null ? atkStages : new BattleStages();
        BattleStages dSt = defStages != null ? defStages : new BattleStages();

        if (move.isStatus()) {
            return applyStatusMove(move, random);
        }

        // Accuracy with ACC/EVA stages
        if (move.getAccuracy() < 101) {
            float stageMul = aSt.accuracyMul(dSt);
            int acc = Math.max(1, Math.min(100, Math.round(move.getAccuracy() * stageMul * HeldItems.accuracyMul(attacker.heldItem()))));
            if (random.nextInt(100) >= acc) {
                return DamageResult.miss(move);
            }
        }

        MonElement defType = defender.primaryType();
        float typeMult = TypeChart.multiplier(move.getElement(), defType);
        if (defender.secondaryType().isPresent()) {
            typeMult *= TypeChart.multiplier(move.getElement(), defender.secondaryType().get());
        }

        Ability defAb = defender.ability();
        float defMul = defAb.defensiveBoost(move.getElement());
        if (defMul <= 0f) {
            return DamageResult.immune(move);
        }
        typeMult *= defMul;

        if ("cosmicquery".equals(ShowdownMoveDex.canonicalize(move.getId())) && typeMult > 0f && typeMult < 1f) {
            typeMult = 1.0f;
        }
        if (attacker.ability() == Ability.MAXIMUM_TRUTH && move.getElement() == MonElement.PSYCHIC
                && typeMult > 0f && typeMult < 1f) {
            typeMult = Math.min(1.0f, typeMult + 0.25f);
        }
        if (typeMult <= 0f) {
            return DamageResult.immune(move);
        }

        boolean special = move.getCategory() == MoveCategory.SPECIAL;
        int atk = special ? specialAttackStat(attacker) : attackStat(attacker);
        int def = special ? specialDefenseStat(defender) : defenseStat(defender);

        // Stages
        atk = Math.max(1, Math.round(atk * aSt.multiplier(special ? StatKind.SPA : StatKind.ATK)));
        def = Math.max(1, Math.round(def * dSt.multiplier(special ? StatKind.SPD : StatKind.DEF)));

        if (!special) {
            atk = Math.max(1, Math.round(atk * attacker.status().physicalAtkMul()));
        }

        String held = attacker.heldItem();
        if (special) {
            atk = Math.max(1, Math.round(atk * HeldItems.specialAtkMul(held)));
            def = Math.max(1, Math.round(def * HeldItems.specialDefMul(defender.heldItem())));
        } else {
            atk = Math.max(1, Math.round(atk * HeldItems.physicalAtkMul(held)));
            def = Math.max(1, Math.round(def * HeldItems.physicalDefMul(defender.heldItem())));
        }

        if (focused) {
            atk = (int) (atk * 1.35f);
        }

        float stab = move.getElement() == attacker.primaryType() ? 1.5f : 1.0f;
        if (attacker.secondaryType().isPresent()
                && move.getElement() == attacker.secondaryType().get()) {
            stab = 1.5f;
        }
        float abOff = attacker.ability().offensiveBoost(move.getElement(), attacker.hpRatio());
        boolean superEff = typeMult > 1f;
        float heldOff = HeldItems.outgoingDamageMul(held, superEff, stab >= 1.5f);
        int power = Math.max(1, BattleEngine.resolveMovePower(move.getId()));
        if (power <= 0) {
            power = Math.max(1, move.getPower());
        }
        int level = attacker.level();

        float base = ((2f * level / 5f + 2f) * power * atk / Math.max(1f, def)) / 50f + 2f;
        // Field weather/terrain type modifiers (when session field is threaded via ThreadLocal optional)
        BattleFieldEffects fieldFx = FIELD_CTX.get();
        if (fieldFx != null) {
            base *= fieldFx.typeMultiplier(move.getElement(), !special);
        }
        int critBonus = HeldItems.critStageBonus(held);
        int critDenom = focused || move.getPriority() > 0 || critBonus > 0 ? 8 : 24;
        if (critBonus > 1) {
            critDenom = 4;
        }
        boolean crit = random.nextInt(Math.max(2, critDenom)) == 0;
        float critMul = crit ? 1.5f : 1f;

        int hits = multiHitCount(move, random);
        float otherMul = abOff * heldOff;
        int dmg;
        // Prefer Showdown-style JS damage when combat bridge is ready; fall back to native.
        Integer jsDmg = null;
        if (com.cobblemon.mod.battle.BattleEngine.useShowdownCombat()) {
            float firstRoll = 0.85f + random.nextFloat() * 0.15f;
            // Turn protocol: enrich move from live Dex when sim is up, then JS damage formula
            jsDmg = com.cobblemon.mod.battle.graal.ShowdownTurnProtocol.resolveDamage(
                    move, attacker, defender, aSt, dSt,
                    typeMult, stab, otherMul, crit, hits, firstRoll
            );
        }
        if (jsDmg != null) {
            dmg = Math.max(0, jsDmg);
        } else {
            dmg = 0;
            for (int h = 0; h < hits; h++) {
                float hitRoll = 0.85f + random.nextFloat() * 0.15f;
                dmg += Math.max(1, Math.round(base * stab * typeMult * otherMul * hitRoll * critMul));
            }
        }

        // Drain heal from damage
        int drainHeal = 0;
        if (move.getDrainFraction() > 0f && dmg > 0) {
            drainHeal = Math.max(1, Math.round(dmg * move.getDrainFraction()));
        }

        // Secondary (chance status / boosts)
        MonStatus inflictStatus = null;
        java.util.List<ShowdownMoveDex.StatBoost> secondaryBoosts = java.util.List.of();
        ShowdownMoveDex.Secondary sec = move.getSecondary();
        if (sec != null && dmg > 0 && random.nextInt(100) < sec.getChance()) {
            if (sec.getStatus() != null && !sec.getStatus().isNone()) {
                inflictStatus = sec.getStatus();
            }
            if (sec.getBoosts() != null && !sec.getBoosts().isEmpty()) {
                secondaryBoosts = sec.getBoosts();
            }
        }

        return new DamageResult(
                move, dmg, typeMult, false, false, inflictStatus, secondaryBoosts,
                move.getSelfBoosts(), drainHeal, hits, crit
        );
    }

    private static int multiHitCount(BattleMove move, RandomSource random) {
        String id = move.getId();
        if (id.contains("double") || id.contains("dual") || id.contains("twin")) {
            return 2;
        }
        if (id.contains("fury") || id.contains("barrage") || id.contains("missile")
                || id.contains("bulletseed") || id.contains("iciclespear") || id.contains("rockblast")) {
            return 2 + random.nextInt(2);
        }
        return 1;
    }

    private static DamageResult applyStatusMove(BattleMove move, RandomSource random) {
        MonStatus status = null;
        java.util.List<ShowdownMoveDex.StatBoost> targetBoosts = java.util.List.of();
        java.util.List<ShowdownMoveDex.StatBoost> selfBoosts = move.getSelfBoosts() != null
                ? move.getSelfBoosts() : java.util.List.of();

        ShowdownMoveDex.Secondary sec = move.getSecondary();
        if (sec != null && random.nextInt(100) < sec.getChance()) {
            if (sec.getStatus() != null) {
                status = sec.getStatus();
            }
            if (sec.getBoosts() != null && !sec.getBoosts().isEmpty()) {
                targetBoosts = sec.getBoosts();
            }
        }

        // Keyword fallback when Showdown data missing
        if (status == null && (sec == null || sec.getStatus() == null)) {
            String id = ShowdownMoveDex.canonicalize(move.getId());
            if (id.contains("sleep") || id.contains("hypnosis") || id.contains("spore")
                    || id.contains("sing") || id.contains("yawn") || id.equals("lovelykiss") || id.equals("darkvoid")) {
                status = MonStatus.SLEEP;
            } else if (id.contains("toxic") || id.equals("poisonpowder") || id.equals("poisongas")) {
                status = MonStatus.POISON;
            } else if (id.equals("thunderwave") || id.contains("stunspore") || id.equals("glare")) {
                status = MonStatus.PARALYSIS;
            } else if (id.contains("willowisp") || id.contains("willo")) {
                status = MonStatus.BURN;
            }
        }

        boolean healSelf = false;
        String id = ShowdownMoveDex.canonicalize(move.getId());
        if (id.equals("restsoft") || id.equals("rest") || id.equals("recover")
                || id.equals("roost") || id.equals("synthesis") || id.equals("moonlight")
                || id.equals("morningsun") || id.equals("softboiled") || id.equals("milkdrink")) {
            healSelf = true;
        }

        return new DamageResult(
                move, 0, 1f, false, false, status, targetBoosts, selfBoosts,
                healSelf ? -1 : 0,
                0, false
        );
    }

    public static int expForWin(int winnerLevel, int foeLevel, int foeStage) {
        int base = 30 + foeLevel * 10 + foeStage * 12;
        if (winnerLevel < foeLevel) {
            base += (foeLevel - winnerLevel) * 4;
        } else if (winnerLevel > foeLevel + 5) {
            base = Math.max(15, base - (winnerLevel - foeLevel) * 2);
        }
        return Math.max(10, base);
    }

    /**
     * @param drainHeal HP restored to attacker from drain (&gt;0), or −1 for status 25% heal, or 0 none
     * @param hits multi-hit count
     */
    public record DamageResult(
            BattleMove move,
            int damage,
            float typeMult,
            boolean missed,
            boolean immune,
            MonStatus inflictStatus,
            java.util.List<ShowdownMoveDex.StatBoost> targetBoosts,
            java.util.List<ShowdownMoveDex.StatBoost> selfBoosts,
            int drainHeal,
            int hits,
            boolean critical
    ) {
        public static DamageResult miss(BattleMove move) {
            return new DamageResult(move, 0, 1f, true, false, null, java.util.List.of(), java.util.List.of(), 0, 0, false);
        }

        public static DamageResult immune(BattleMove move) {
            return new DamageResult(move, 0, 0f, false, true, null, java.util.List.of(), java.util.List.of(), 0, 0, false);
        }

        /** @deprecated use {@link #hits()} */
        public int unused() {
            return hits;
        }

        /** @deprecated use {@link #inflictStatus()} */
        public Object status() {
            return inflictStatus;
        }
    }
}

package com.cobblemon.mod.species;

/**
 * Resolved integer stats for a creature at a given level (after nature + form + IVs/EVs).
 */
public record StatBlock(int hp, int atk, int def, int spAtk, int spDef, int speed) {
    public static final int IV_MAX = 31;
    public static final int EV_MAX_STAT = 252;
    public static final int EV_MAX_TOTAL = 510;

    public int get(StatId id) {
        return switch (id) {
            case HP -> hp;
            case ATK -> atk;
            case DEF -> def;
            case SP_ATK -> spAtk;
            case SP_DEF -> spDef;
            case SPEED -> speed;
        };
    }

    /**
     * Classic-style without IVs/EVs (neutral 0 IV / 0 EV).
     */
    public static StatBlock compute(MonSpecies species, int level, Nature nature, MonForm form) {
        return compute(SpeciesHandle.of(species), level, nature, form, null, null);
    }

    /** Stats from any species id (enum Gen1 or full datapack registry). */
    public static StatBlock compute(SpeciesHandle species, int level, Nature nature, MonForm form) {
        return compute(species, level, nature, form, null, null);
    }

    /**
     * Mainline-ish: HP = floor(((2B+IV+EV/4)*L)/100)+L+10
     * others = floor((floor(((2B+IV+EV/4)*L)/100)+5) * nature) * form
     * IVs 0–31, EVs 0–252 per stat.
     */
    public static StatBlock compute(
            SpeciesHandle species, int level, Nature nature, MonForm form,
            int[] ivs, int[] evs
    ) {
        level = Math.max(1, Math.min(OwnedMon.MAX_LEVEL, level));
        float formMul = form == null ? 1f : form.statMultiplier();
        Nature n = nature == null ? Nature.HARDY : nature;
        int[] iv = normalizeSix(ivs, 0, IV_MAX, 0);
        int[] ev = normalizeSix(evs, 0, EV_MAX_STAT, 0);

        CobblemonBaseStats.Six six = species.baseStats();
        int[] bases = {six.hp(), six.atk(), six.def(), six.spa(), six.spd(), six.spe()};

        int hp = calcHp(bases[0], iv[0], ev[0], level);
        if (form == MonForm.ALPHA) {
            hp = Math.max(10, Math.round(hp * 1.1f));
        }

        int atk = calcStat(bases[1], iv[1], ev[1], level, n.multiplier(StatId.ATK), formMul);
        int def = calcStat(bases[2], iv[2], ev[2], level, n.multiplier(StatId.DEF), formMul);
        int spa = calcStat(bases[3], iv[3], ev[3], level, n.multiplier(StatId.SP_ATK), formMul);
        int spdef = calcStat(bases[4], iv[4], ev[4], level, n.multiplier(StatId.SP_DEF), formMul);
        int spe = calcStat(bases[5], iv[5], ev[5], level, n.multiplier(StatId.SPEED), formMul);

        return new StatBlock(hp, atk, def, spa, spdef, spe);
    }

    private static int calcHp(int base, int iv, int ev, int level) {
        return Math.max(10, (int) Math.floor(((2.0 * base + iv + ev / 4.0) * level) / 100.0) + level + 10);
    }

    private static int calcStat(int base, int iv, int ev, int level, float natureMul, float formMul) {
        double raw = Math.floor(((2.0 * base + iv + ev / 4.0) * level) / 100.0) + 5.0;
        return Math.max(1, (int) Math.round(raw * natureMul * formMul));
    }

    public static int[] normalizeSix(int[] raw, int min, int max, int fill) {
        int[] out = new int[]{fill, fill, fill, fill, fill, fill};
        if (raw != null) {
            for (int i = 0; i < 6 && i < raw.length; i++) {
                out[i] = Math.max(min, Math.min(max, raw[i]));
            }
        }
        return out;
    }

    /** Random IVs 0–31 each. */
    public static int[] rollIvs(net.minecraft.util.RandomSource random) {
        int[] iv = new int[6];
        for (int i = 0; i < 6; i++) {
            iv[i] = random.nextInt(IV_MAX + 1);
        }
        return iv;
    }
}

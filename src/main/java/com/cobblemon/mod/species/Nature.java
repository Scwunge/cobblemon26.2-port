package com.cobblemon.mod.species;

import java.util.Locale;
import java.util.Optional;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;

/**
 * Personality that boosts one stat and cuts another (±10%). Neutral natures are flat.
 */
public enum Nature implements StringRepresentable {
    HARDY(null, null, "Steady and unremarkable."),
    LONELY(StatId.ATK, StatId.DEF, "Craves company; hits hard, takes hits poorly."),
    BRAVE(StatId.ATK, StatId.SPEED, "Charges first — speed be damned."),
    ADAMANT(StatId.ATK, StatId.SP_ATK, "All muscle, little mysticism."),
    NAUGHTY(StatId.ATK, StatId.SP_DEF, "Mean streak; weak to special pressure."),
    BOLD(StatId.DEF, StatId.ATK, "Calm wall; soft punches."),
    DOCILE(null, null, "Gentle and balanced."),
    RELAXED(StatId.DEF, StatId.SPEED, "Sturdy but lazy on the feet."),
    IMPISH(StatId.DEF, StatId.SP_ATK, "Playful tank; weak specials."),
    LAX(StatId.DEF, StatId.SP_DEF, "Loose guard against specials."),
    TIMID(StatId.SPEED, StatId.ATK, "Quick feet, soft claws."),
    HASTY(StatId.SPEED, StatId.DEF, "Glass cannon of mobility."),
    SERIOUS(null, null, "Focused, no frills."),
    JOLLY(StatId.SPEED, StatId.SP_ATK, "Cheerful sprinter; dull specials."),
    NAIVE(StatId.SPEED, StatId.SP_DEF, "Fast and trusting."),
    MODEST(StatId.SP_ATK, StatId.ATK, "Mind over muscle."),
    MILD(StatId.SP_ATK, StatId.DEF, "Soft shell, sharp mind."),
    QUIET(StatId.SP_ATK, StatId.SPEED, "Powerful specials; slow."),
    BASHFUL(null, null, "Shy and even-tempered."),
    RASH(StatId.SP_ATK, StatId.SP_DEF, "Reckless caster."),
    CALM(StatId.SP_DEF, StatId.ATK, "Composed shield; soft attack."),
    GENTLE(StatId.SP_DEF, StatId.DEF, "Soft armor, strong will."),
    SASSY(StatId.SP_DEF, StatId.SPEED, "Special wall that won't hurry."),
    CAREFUL(StatId.SP_DEF, StatId.SP_ATK, "Wary of special tricks."),
    QUIRKY(null, null, "Unpredictable, statistically flat.");

    private final StatId up;
    private final StatId down;
    private final String blurb;

    Nature(StatId up, StatId down, String blurb) {
        this.up = up;
        this.down = down;
        this.blurb = blurb;
    }

    public Optional<StatId> boosted() {
        return Optional.ofNullable(up);
    }

    public Optional<StatId> cut() {
        return Optional.ofNullable(down);
    }

    public String blurb() {
        return blurb;
    }

    /** Multiplier for a stat (1.1 / 0.9 / 1.0). HP is never affected. */
    public float multiplier(StatId stat) {
        if (stat == StatId.HP) {
            return 1.0f;
        }
        if (up == stat) {
            return 1.1f;
        }
        if (down == stat) {
            return 0.9f;
        }
        return 1.0f;
    }

    public MutableComponent displayName() {
        return Component.translatable("cobblemon.nature." + getSerializedName());
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Nature byId(String id) {
        if (id == null || id.isBlank()) {
            return HARDY;
        }
        try {
            return Nature.valueOf(id.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return HARDY;
        }
    }

    public static Nature random(RandomSource random) {
        Nature[] all = values();
        return all[random.nextInt(all.length)];
    }
}

package com.cobblemon.mod.species;

import java.util.Locale;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;

/**
 * Visual / minor-stat variants of a species (not full mega-evo).
 */
public enum MonForm implements StringRepresentable {
    NORMAL("normal", 1.0f, 0xFFFFFF, "Standard appearance."),
    SHINY("shiny", 1.05f, 0xFFD700, "Rare recolor — slightly stronger aura."),
    ALPHA("alpha", 1.12f, 0xFF4444, "Larger, bolder wilds — size and power up."),
    SHADOW("shadow", 1.08f, 0x4A0080, "Dark-tinted variant with a fierce streak.");

    private final String id;
    private final float statMul;
    private final int tintHint;
    private final String blurb;

    MonForm(String id, float statMul, int tintHint, String blurb) {
        this.id = id;
        this.statMul = statMul;
        this.tintHint = tintHint;
        this.blurb = blurb;
    }

    public String id() {
        return id;
    }

    /** Multiplier applied on top of level/nature stats (not HP for SHINY only mild). */
    public float statMultiplier() {
        return statMul;
    }

    public int tintHint() {
        return tintHint;
    }

    public String blurb() {
        return blurb;
    }

    public boolean isDefault() {
        return this == NORMAL;
    }

    public MutableComponent displayName() {
        return Component.translatable("form.cobblemon." + id);
    }

    @Override
    public String getSerializedName() {
        return id;
    }

    public static MonForm byId(String id) {
        if (id == null || id.isBlank()) {
            return NORMAL;
        }
        String key = id.toLowerCase(Locale.ROOT);
        for (MonForm f : values()) {
            if (f.id.equals(key)) {
                return f;
            }
        }
        return NORMAL;
    }

    /** Default wild shiny odds (Gen 6+ base rate). Master Ball does not affect this. */
    public static final int SHINY_ODDS = 4096;

    /** Wild roll: 1/4096 shiny, ~1/40 alpha, ~1/50 shadow, else normal. */
    public static MonForm rollWild(RandomSource random) {
        if (random.nextInt(SHINY_ODDS) == 0) {
            return SHINY;
        }
        if (random.nextInt(40) == 0) {
            return ALPHA;
        }
        if (random.nextInt(50) == 0) {
            return SHADOW;
        }
        return NORMAL;
    }

    /** True for the shiny recolor form. */
    public boolean isShiny() {
        return this == SHINY;
    }
}

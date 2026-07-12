package com.cobblemon.mod.species;

import java.util.Locale;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;

/**
 * Gender for individual creatures. Species define male chance (0–1); -1 = always genderless.
 */
public enum MonGender implements StringRepresentable {
    MALE("male", "♂"),
    FEMALE("female", "♀"),
    GENDERLESS("genderless", "—");

    private final String id;
    private final String symbol;

    MonGender(String id, String symbol) {
        this.id = id;
        this.symbol = symbol;
    }

    public String id() {
        return id;
    }

    public String symbol() {
        return symbol;
    }

    public MutableComponent displayName() {
        return Component.translatable("gender.cobblemon." + id);
    }

    @Override
    public String getSerializedName() {
        return id;
    }

    public static MonGender byId(String id) {
        if (id == null) {
            return GENDERLESS;
        }
        return switch (id.toLowerCase(Locale.ROOT)) {
            case "male", "m" -> MALE;
            case "female", "f" -> FEMALE;
            default -> GENDERLESS;
        };
    }

    /**
     * @param maleRatio 0..1 chance of male; negative = always genderless
     */
    public static MonGender roll(RandomSource random, float maleRatio) {
        if (maleRatio < 0f) {
            return GENDERLESS;
        }
        maleRatio = Math.min(1f, Math.max(0f, maleRatio));
        return random.nextFloat() < maleRatio ? MALE : FEMALE;
    }
}

package com.cobblemon.mod.species;

import java.util.Locale;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.StringRepresentable;

/**
 * Primary status conditions (Cobblemon / mainline-style).
 */
public enum MonStatus implements StringRepresentable {
    NONE("none", "Healthy"),
    BURN("burn", "Burned"),
    POISON("poison", "Poisoned"),
    PARALYSIS("paralysis", "Paralyzed"),
    SLEEP("sleep", "Asleep"),
    FREEZE("freeze", "Frozen");

    private final String id;
    private final String english;

    MonStatus(String id, String english) {
        this.id = id;
        this.english = english;
    }

    public String id() {
        return id;
    }

    public String english() {
        return english;
    }

    public boolean isNone() {
        return this == NONE;
    }

    public MutableComponent displayName() {
        return Component.translatable("status.cobblemon." + id);
    }

    /** Residual HP fraction of max lost at end of turn (0 = none). */
    public float residualFraction() {
        return switch (this) {
            case BURN, POISON -> 1f / 8f;
            default -> 0f;
        };
    }

    /** Attack multiplier while burned (physical). */
    public float physicalAtkMul() {
        return this == BURN ? 0.5f : 1f;
    }

    /** Speed multiplier while paralyzed. */
    public float speedMul() {
        return this == PARALYSIS ? 0.5f : 1f;
    }

    /** Chance (0-1) to be fully immobilized this turn. */
    public float skipTurnChance() {
        return switch (this) {
            case PARALYSIS -> 0.25f;
            case SLEEP, FREEZE -> 1.0f;
            default -> 0f;
        };
    }

    public static MonStatus byId(String raw) {
        if (raw == null || raw.isBlank()) {
            return NONE;
        }
        String s = raw.toLowerCase(Locale.ROOT);
        for (MonStatus st : values()) {
            if (st.id.equals(s) || st.name().equalsIgnoreCase(s)) {
                return st;
            }
        }
        return NONE;
    }

    /** Which medicine ids cure this status (Cobblemon StatusCureItem mapping). */
    public boolean curedBy(String medicineId) {
        if (medicineId == null) {
            return false;
        }
        String m = medicineId.toLowerCase(Locale.ROOT);
        if (m.equals("full_heal") || m.equals("full_restore") || m.equals("heal_powder") || m.equals("lum_berry")) {
            return !isNone();
        }
        return switch (this) {
            case POISON -> m.equals("antidote") || m.equals("pecha_berry");
            case BURN -> m.equals("burn_heal") || m.equals("rawst_berry");
            case PARALYSIS -> m.equals("paralyze_heal") || m.equals("cheri_berry");
            case SLEEP -> m.equals("awakening") || m.equals("chesto_berry");
            case FREEZE -> m.equals("ice_heal") || m.equals("aspear_berry");
            default -> false;
        };
    }

    @Override
    public String getSerializedName() {
        return id;
    }
}

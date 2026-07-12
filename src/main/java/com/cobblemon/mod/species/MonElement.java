package com.cobblemon.mod.species;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.StringRepresentable;

/**
 * Cobblemon type chart roots — Pokémon-inspired names, original presentation.
 * RGB accents drive HUD chips and starter cards.
 */
public enum MonElement implements StringRepresentable {
    FIRE("fire", "Fire", ChatFormatting.RED, 0xE85D04),
    WATER("water", "Water", ChatFormatting.AQUA, 0x4CC9F0),
    GRASS("grass", "Grass", ChatFormatting.GREEN, 0x52B788),
    ELECTRIC("electric", "Electric", ChatFormatting.YELLOW, 0xFEE440),
    GROUND("ground", "Ground", ChatFormatting.GOLD, 0xD4A373),
    FLYING("flying", "Flying", ChatFormatting.WHITE, 0xCAF0F8),
    FIGHTING("fighting", "Fighting", ChatFormatting.DARK_RED, 0xC1121F),
    DRAGON("dragon", "Dragon", ChatFormatting.DARK_BLUE, 0x5A189A),
    DARK("dark", "Dark", ChatFormatting.DARK_PURPLE, 0x3C096C),
    PSYCHIC("psychic", "Psychic", ChatFormatting.LIGHT_PURPLE, 0xFF6BCB),
    ROCK("rock", "Rock", ChatFormatting.GRAY, 0xADB5BD),
    ICE("ice", "Ice", ChatFormatting.BLUE, 0x90E0EF),
    STEEL("steel", "Steel", ChatFormatting.DARK_GRAY, 0x8D99AE),
    FAIRY("fairy", "Fairy", ChatFormatting.LIGHT_PURPLE, 0xFFAFCC),
    BUG("bug", "Bug", ChatFormatting.DARK_GREEN, 0xA7C957),
    POISON("poison", "Poison", ChatFormatting.DARK_PURPLE, 0x9B5DE5),
    GHOST("ghost", "Ghost", ChatFormatting.DARK_AQUA, 0x7B2CBF),
    NORMAL("normal", "Normal", ChatFormatting.GRAY, 0xDEE2E6);

    private final String id;
    private final String english;
    private final ChatFormatting formatting;
    private final int rgb;

    MonElement(String id, String english, ChatFormatting formatting, int rgb) {
        this.id = id;
        this.english = english;
        this.formatting = formatting;
        this.rgb = rgb;
    }

    public ChatFormatting color() {
        return formatting;
    }

    /** 0xRRGGBB used for HUD fills (with alpha applied by callers). */
    public int rgb() {
        return rgb;
    }

    public int argb(int alpha) {
        return ((alpha & 0xFF) << 24) | (rgb & 0xFFFFFF);
    }

    public MutableComponent displayName() {
        // Always use hardcoded English for UI reliability (lang files vary / miss keys)
        return Component.literal(english).withStyle(formatting);
    }

    /** Plain English type name for HUD/party (never a translation key). */
    public String englishName() {
        return english;
    }

    public String id() {
        return id;
    }

    @Override
    public String getSerializedName() {
        return id;
    }

    public static MonElement byId(String id) {
        if (id == null) {
            return NORMAL;
        }
        // Migrate old Cobblemon 0.1 element ids
        return switch (id.toLowerCase()) {
            case "ember" -> FIRE;
            case "tide" -> WATER;
            case "verdant" -> GRASS;
            case "spark" -> ELECTRIC;
            case "stone" -> ROCK;
            case "shade" -> DARK;
            case "breeze" -> FLYING;
            default -> {
                for (MonElement e : values()) {
                    if (e.id.equalsIgnoreCase(id)) {
                        yield e;
                    }
                }
                yield NORMAL;
            }
        };
    }
}

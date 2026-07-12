package com.cobblemon.mod.client;

import com.cobblemon.mod.species.MonElement;
import com.cobblemon.mod.species.MonSpecies;
import com.cobblemon.mod.species.OwnedMon;
import com.cobblemon.mod.species.SpeciesHandle;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * Safe display strings for UI — never show raw translation keys like
 * {@code type.cobblemon.fire} when a lang entry is missing.
 * Prefer {@link #species(OwnedMon)} / {@link #species(String)} over {@link MonSpecies}
 * so full-dex mons are not remapped to Gen1 stand-ins.
 */
public final class LangNames {
    private LangNames() {}

    public static MutableComponent species(OwnedMon mon) {
        if (mon == null) {
            return Component.literal("???");
        }
        return Component.literal(mon.displayName().getString());
    }

    public static MutableComponent species(MonSpecies species) {
        if (species == null) {
            return Component.literal("???");
        }
        // Still resolve via handle so datapack names win when enum id matches
        return species(species.id());
    }

    public static MutableComponent species(String id) {
        if (id == null || id.isBlank()) {
            return Component.literal("???");
        }
        String name = SpeciesHandle.of(id).displayName().getString();
        if (looksLikeKey(name)) {
            name = titleCase(id);
        }
        return Component.literal(name);
    }

    public static MutableComponent species(String id, Component fromLang) {
        String s = fromLang == null ? "" : fromLang.getString();
        if (looksLikeKey(s)) {
            // Try SpeciesRegistry display name
            s = SpeciesHandle.of(id).displayName().getString();
        }
        if (looksLikeKey(s)) {
            s = titleCase(id);
        }
        return Component.literal(s);
    }

    public static MutableComponent type(MonElement element) {
        if (element == null) {
            return Component.literal("???");
        }
        return Component.literal(element.englishName()).withStyle(element.color());
    }

    public static MutableComponent move(com.cobblemon.mod.battle.MonMove move) {
        if (move == null) {
            return Component.literal("???");
        }
        return Component.literal(move.englishName());
    }

    public static boolean looksLikeKey(String s) {
        if (s == null || s.isBlank()) {
            return true;
        }
        // raw keys always contain a dot and a known prefix
        return s.contains(".") && (s.startsWith("type.") || s.startsWith("species.")
                || s.startsWith("cobblemon.") || s.startsWith("item.") || s.startsWith("nature.")
                || s.startsWith("ability.") || s.contains("cobblemon."));
    }

    public static String titleCase(String id) {
        if (id == null || id.isBlank()) {
            return "???";
        }
        String[] parts = id.replace('_', ' ').split(" ");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) {
                continue;
            }
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(p.charAt(0)));
            if (p.length() > 1) {
                sb.append(p.substring(1).toLowerCase());
            }
        }
        return sb.toString();
    }
}

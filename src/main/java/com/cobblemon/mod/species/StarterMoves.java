package com.cobblemon.mod.species;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

import com.cobblemon.mod.battle.BattleMove;
import com.cobblemon.mod.battle.DatapackLearnsets;
import com.cobblemon.mod.battle.MoveCategory;

/**
 * Ensures every starter (and similar gifted mon) has a usable early kit:
 * at least one damaging attack and one 0-power defensive/status move.
 */
public final class StarterMoves {
    private StarterMoves() {}

    /**
     * Build a 2–4 move set for a starter at {@code level} (typically 5).
     */
    public static List<String> forStarter(String speciesId, int level) {
        List<BattleMove> known = DatapackLearnsets.battleMovesKnownAtLevel(speciesId, level);
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        for (BattleMove m : known) {
            if (m != null && m.getId() != null && !m.getId().isBlank()) {
                ids.add(m.getId().toLowerCase(Locale.ROOT));
            }
        }

        SpeciesHandle handle = SpeciesHandle.of(speciesId);
        MonElement type = handle.primaryType();

        boolean hasAttack = false;
        boolean hasDefense = false;
        for (String id : ids) {
            BattleMove bm = BattleMove.resolve(id);
            if (bm.getPower() > 0 && bm.getCategory() != MoveCategory.STATUS) {
                hasAttack = true;
            }
            if (isDefensiveStatus(bm)) {
                hasDefense = true;
            }
        }

        if (!hasAttack) {
            ids.add(defaultAttack(type));
        }
        if (!hasDefense) {
            ids.add(defaultDefense(type));
        }

        // Prefer order: attack STAB-ish first if we just injected, keep learnset order otherwise
        List<String> out = new ArrayList<>(ids);
        // Cap at 4
        if (out.size() > 4) {
            // Prefer keeping at least one attack + one defense
            List<String> trimmed = new ArrayList<>();
            String attackKeep = null;
            String defenseKeep = null;
            for (String id : out) {
                BattleMove bm = BattleMove.resolve(id);
                if (attackKeep == null && bm.getPower() > 0 && bm.getCategory() != MoveCategory.STATUS) {
                    attackKeep = id;
                }
                if (defenseKeep == null && isDefensiveStatus(bm)) {
                    defenseKeep = id;
                }
            }
            if (attackKeep != null) trimmed.add(attackKeep);
            if (defenseKeep != null && !defenseKeep.equals(attackKeep)) trimmed.add(defenseKeep);
            for (String id : out) {
                if (trimmed.size() >= 4) break;
                if (!trimmed.contains(id)) trimmed.add(id);
            }
            out = trimmed;
        }
        // Final guarantee
        hasAttack = out.stream().anyMatch(id -> {
            BattleMove bm = BattleMove.resolve(id);
            return bm.getPower() > 0 && bm.getCategory() != MoveCategory.STATUS;
        });
        hasDefense = out.stream().anyMatch(id -> isDefensiveStatus(BattleMove.resolve(id)));
        if (!hasAttack) {
            if (out.size() >= 4) out.set(0, defaultAttack(type));
            else out.add(0, defaultAttack(type));
        }
        if (!hasDefense) {
            String def = defaultDefense(type);
            if (out.size() >= 4) {
                // replace last non-attack if possible
                out.set(out.size() - 1, def);
            } else {
                out.add(def);
            }
        }
        return out;
    }

    /** True for 0-power status that buffs self or debuffs foe (defensive utility). */
    public static boolean isDefensiveStatus(BattleMove bm) {
        if (bm == null) return false;
        if (bm.getPower() > 0 && bm.getCategory() != MoveCategory.STATUS) return false;
        if (!bm.isStatus() && bm.getPower() > 0) return false;
        String id = bm.getId() == null ? "" : bm.getId().toLowerCase(Locale.ROOT);
        // Explicit denylist of pure non-defensive status (heal/setup that isn't "defense")
        // Allow: stat downs on foe, stat ups on self defense, protect, withdraw, etc.
        // Any status / 0-power move counts as the defensive slot for starters
        return bm.isStatus() || bm.getPower() <= 0;
    }

    private static String defaultAttack(MonElement type) {
        if (type == null) return "tackle";
        return switch (type) {
            case FIRE -> "ember";
            case WATER -> "watergun";
            case GRASS -> "vinewhip";
            case ELECTRIC -> "thundershock";
            case ICE -> "powdersnow";
            case FIGHTING -> "rocksmash";
            case POISON -> "acid";
            case GROUND -> "mudslap";
            case FLYING -> "gust";
            case PSYCHIC -> "confusion";
            case BUG -> "bugbite";
            case ROCK -> "rollout";
            case GHOST -> "astonish";
            case DRAGON -> "twister";
            case DARK -> "pursuit";
            case STEEL -> "metalclaw";
            case FAIRY -> "fairywind";
            default -> "tackle";
        };
    }

    /** 0-damage defensive / utility status. */
    private static String defaultDefense(MonElement type) {
        if (type == null) return "growl";
        return switch (type) {
            case WATER -> "withdraw";
            case FIRE -> "smokescreen";
            case GRASS -> "growl";
            case ROCK, STEEL -> "harden";
            case FIGHTING -> "leer";
            case NORMAL -> "tailwhip";
            case PSYCHIC -> "growl";
            case GROUND -> "sandattack";
            case ELECTRIC -> "growl";
            case ICE -> "growl";
            case DARK -> "leer";
            case GHOST -> "growl";
            case DRAGON -> "leer";
            case FAIRY -> "growl";
            case BUG -> "stringshot";
            case POISON -> "growl";
            case FLYING -> "growl";
        };
    }
}

package com.cobblemon.mod.species;

import java.util.Locale;
import java.util.Set;

/**
 * Cobblemon-inspired held-item combat modifiers.
 * IDs match imported content item ids (e.g. choice_band, leftovers).
 */
public final class HeldItems {
    private static final Set<String> KNOWN = Set.of(
            "choice_band", "choice_specs", "choice_scarf",
            "life_orb", "expert_belt", "muscle_band", "wise_glasses",
            "leftovers", "black_sludge", "shell_bell", "assault_vest",
            "focus_sash", "focus_band", "rocky_helmet", "eviolite",
            "light_ball", "thick_club", "metal_powder", "quick_powder",
            "deep_sea_tooth", "deep_sea_scale", "soul_dew",
            "adamant_orb", "lustrous_orb", "griseous_orb",
            "scope_lens", "wide_lens", "zoom_lens", "razor_claw", "razor_fang",
            "kings_rock", "flame_orb", "toxic_orb", "sticky_barb",
            "iron_ball", "lagging_tail", "shed_shell", "big_root",
            "metronome", "binding_band", "grip_claw", "float_stone",
            "air_balloon", "safety_goggles", "weakness_policy",
            "oran_berry", "sitrus_berry", "lum_berry", "leppa_berry",
            "cheri_berry", "chesto_berry", "pecha_berry", "rawst_berry",
            "aspear_berry", "persim_berry", "figy_berry", "wiki_berry",
            "mago_berry", "aguav_berry", "iapapa_berry"
    );

    private HeldItems() {}

    public static float physicalAtkMul(String heldId) {
        String id = norm(heldId);
        if (id.isEmpty()) return 1f;
        if (id.equals("choice_band") || id.equals("muscle_band")) return 1.5f;
        if (id.equals("thick_club")) return 2.0f; // Cubone/Marowak in real games — apply lightly always
        if (id.equals("light_ball")) return 1.5f; // Pikachu physical too
        return 1f;
    }

    public static float specialAtkMul(String heldId) {
        String id = norm(heldId);
        if (id.isEmpty()) return 1f;
        if (id.equals("choice_specs") || id.equals("wise_glasses")) return 1.5f;
        if (id.equals("light_ball") || id.equals("deep_sea_tooth")) return 1.5f;
        if (id.equals("soul_dew") || id.equals("adamant_orb") || id.equals("lustrous_orb") || id.equals("griseous_orb")) {
            return 1.2f;
        }
        return 1f;
    }

    public static float speedMul(String heldId) {
        String id = norm(heldId);
        if (id.isEmpty()) return 1f;
        if (id.equals("choice_scarf") || id.equals("quick_powder")) return 1.5f;
        if (id.equals("iron_ball") || id.equals("lagging_tail") || id.equals("float_stone")) return 0.5f;
        return 1f;
    }

    public static float damageTakenMul(String heldId, boolean superEffective) {
        String id = norm(heldId);
        if (id.isEmpty()) return 1f;
        if (id.equals("eviolite")) return 0.75f; // simplified always-on bulk
        if (superEffective && id.equals("weakness_policy")) return 1f; // policy is offensive trigger
        return 1f;
    }

    public static float outgoingDamageMul(String heldId, boolean superEffective, boolean stab) {
        String id = norm(heldId);
        if (id.isEmpty()) return 1f;
        if (superEffective && id.equals("expert_belt")) return 1.2f;
        if (id.equals("life_orb")) return 1.3f;
        if (superEffective && id.equals("weakness_policy")) return 1.5f; // after taking SE (simplified always when SE out)
        return 1f;
    }

    public static float specialDefMul(String heldId) {
        String id = norm(heldId);
        if (id.equals("assault_vest") || id.equals("deep_sea_scale") || id.equals("metal_powder") || id.equals("eviolite")) {
            return 1.5f;
        }
        return 1f;
    }

    public static float physicalDefMul(String heldId) {
        String id = norm(heldId);
        if (id.equals("eviolite") || id.equals("metal_powder")) return 1.5f;
        return 1f;
    }

    /** Crit stage bonus (0 = none, 1 = high crit). */
    public static int critStageBonus(String heldId) {
        String id = norm(heldId);
        if (id.equals("scope_lens") || id.equals("razor_claw")) return 1;
        return 0;
    }

    /** Accuracy mult for moves. */
    public static float accuracyMul(String heldId) {
        String id = norm(heldId);
        if (id.equals("wide_lens")) return 1.1f;
        if (id.equals("zoom_lens")) return 1.2f;
        return 1f;
    }

    /** Residual heal fraction of max HP at end of turn (leftovers / black sludge). */
    public static float residualHealFraction(String heldId, MonElement primary) {
        String id = norm(heldId);
        if (id.equals("leftovers") || id.equals("shell_bell") || id.equals("big_root")) return 1f / 16f;
        if (id.equals("black_sludge")) {
            return primary == MonElement.POISON ? 1f / 16f : -1f / 8f;
        }
        // Pinch berries handled in BattleBerry
        return 0f;
    }

    public static float lifeOrbRecoil(String heldId) {
        return "life_orb".equals(norm(heldId)) ? 0.1f : 0f;
    }

    /** Rocky Helmet contact damage fraction. */
    public static float rockyHelmetRecoil(String heldId) {
        return "rocky_helmet".equals(norm(heldId)) ? 1f / 6f : 0f;
    }

    public static boolean isChoiceLocking(String heldId) {
        String id = norm(heldId);
        return id.equals("choice_band") || id.equals("choice_specs") || id.equals("choice_scarf");
    }

    public static boolean isBerry(String heldId) {
        String id = norm(heldId);
        return id.endsWith("_berry");
    }

    public static boolean canHold(String itemId) {
        if (itemId == null || itemId.isBlank()) return false;
        String id = norm(itemId);
        // Not throwable catch balls
        if (id.endsWith("_ball") && !id.equals("iron_ball") && !id.equals("light_ball") && !id.equals("smoke_ball")) {
            return false;
        }
        if (id.contains("potion") || id.contains("revive") || id.contains("ether") || id.contains("elixir")) {
            return false;
        }
        if (id.contains("rod") || id.contains("fossil") || id.contains("bait")) {
            return false;
        }
        // Evolution stones not holdable
        if (id.equals("fire_stone") || id.equals("water_stone") || id.equals("thunder_stone")
                || id.equals("leaf_stone") || id.equals("moon_stone") || id.equals("sun_stone")
                || id.equals("dawn_stone") || id.equals("dusk_stone") || id.equals("shiny_stone")
                || id.equals("ice_stone")) {
            return false;
        }
        if (KNOWN.contains(id)) return true;
        if (id.endsWith("_berry")) return true;
        if (id.endsWith("_plate") || id.endsWith("_memory") || id.endsWith("_drive")) return true;
        if (id.contains("choice_") || id.contains("orb") || id.contains("band") || id.contains("scarf")
                || id.contains("specs") || id.contains("lens") || id.contains("helmet")
                || id.contains("vest") || id.contains("powder") || id.contains("scale")
                || id.contains("tooth") || id.contains("claw") || id.contains("fang")
                || id.contains("rock") || id.contains("incense") || id.contains("charm")) {
            return true;
        }
        return false;
    }

    private static String norm(String heldId) {
        return heldId == null ? "" : heldId.toLowerCase(Locale.ROOT).trim();
    }
}

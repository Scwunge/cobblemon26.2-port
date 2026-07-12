package com.cobblemon.mod.content;

import java.util.Locale;

import com.cobblemon.mod.item.CubeBallTier;
import com.cobblemon.mod.species.EvolutionMethod;
import com.cobblemon.mod.species.HeldItems;
import com.cobblemon.mod.species.Nature;

/**
 * Classifies imported Cobblemon content IDs into behavior kinds (mirrors Cobblemon item/block types).
 */
public final class ContentKind {
    private ContentKind() {}

    public enum ItemRole {
        BALL,
        MEDICINE,
        BERRY,
        CANDY,
        EVOLUTION_STONE,
        MINT,
        MINT_SEEDS,
        MULCH,
        FOSSIL,
        HELD,
        BAIT,
        VITAMIN,
        POKEDEX,
        GENERIC
    }

    public enum BlockRole {
        MACHINE,
        MEDICINE_DISPLAY,
        BERRY_CROP,
        WOOD_LOG,
        WOOD_PLANKS,
        WOOD_SLAB,
        WOOD_STAIRS,
        WOOD_FENCE,
        WOOD_FENCE_GATE,
        WOOD_DOOR,
        WOOD_TRAPDOOR,
        WOOD_BUTTON,
        WOOD_PRESSURE,
        WOOD_SIGN,
        LEAVES,
        SAPLING,
        ORE,
        CROP,
        MULCH,
        APRICORN,
        GENERIC
    }

    /**
     * Item IDs that exist only as 3D / animation model files (not real items).
     * Official Cobblemon keeps 7 Pokédex colours; {@code *_model*} variants are poses.
     */
    public static boolean isModelOnlyItem(String id) {
        if (id == null) return false;
        String s = id.toLowerCase(Locale.ROOT);
        // ball 3D mesh ids, pokedex open/flat/scanning meshes, generic *_model*
        if (s.contains("_model")) return true;
        if (s.startsWith("pokedex_") && (s.contains("_flat") || s.contains("_off")
                || s.contains("scanning") || s.endsWith("_open"))) {
            return true;
        }
        return false;
    }

    /** Real giveable Pokédex items: pokedex_red … pokedex_yellow (7 colours). */
    public static boolean isPokedexItem(String id) {
        if (id == null) return false;
        String s = id.toLowerCase(Locale.ROOT);
        if (isModelOnlyItem(s)) return false;
        return s.equals("pokedex") || s.matches("pokedex_(red|blue|green|yellow|pink|black|white)");
    }

    public static ItemRole itemRole(String id) {
        if (id == null) return ItemRole.GENERIC;
        String s = id.toLowerCase(Locale.ROOT);
        if (isModelOnlyItem(s)) return ItemRole.GENERIC;
        if (isPokedexItem(s)) return ItemRole.POKEDEX;
        if (s.endsWith("_ball") && !s.contains("model") && !s.equals("iron_ball") && !s.equals("light_ball") && !s.equals("smoke_ball")) {
            return ItemRole.BALL;
        }
        if (s.endsWith("_berry")) return ItemRole.BERRY;
        if (s.endsWith("_mint_seeds")) return ItemRole.MINT_SEEDS;
        if (s.endsWith("_mint") || s.equals("mint")) return ItemRole.MINT;
        if (s.contains("mulch")) return ItemRole.MULCH;
        if (s.contains("fossil") && !s.contains("analyzer")) return ItemRole.FOSSIL;
        if (s.equals("rare_candy") || s.startsWith("exp_candy") || s.endsWith("_candy") && s.contains("exp")) {
            return ItemRole.CANDY;
        }
        if (isEvolutionStone(s)) return ItemRole.EVOLUTION_STONE;
        if (isMedicine(s)) return ItemRole.MEDICINE;
        if (isBait(s)) return ItemRole.BAIT;
        if (isVitamin(s)) return ItemRole.VITAMIN;
        if (HeldItems.canHold(s) || isLikelyHeld(s)) return ItemRole.HELD;
        return ItemRole.GENERIC;
    }

    public static boolean isBait(String s) {
        if (s == null) return false;
        String id = s.toLowerCase(Locale.ROOT);
        return id.contains("bait") || id.contains("lure") && !id.contains("ball")
                || id.equals("poke_bait") || id.contains("honey") && id.contains("poke");
    }

    public static boolean isVitamin(String s) {
        if (s == null) return false;
        String id = s.toLowerCase(Locale.ROOT);
        return id.equals("hp_up") || id.equals("protein") || id.equals("iron")
                || id.equals("calcium") || id.equals("zinc") || id.equals("carbos")
                || id.equals("pp_up") || id.equals("pp_max") || id.equals("rare_candy")
                || id.contains("feather") && (id.contains("health") || id.contains("muscle")
                || id.contains("resist") || id.contains("genius") || id.contains("clever") || id.contains("swift"));
    }

    private static boolean isLikelyHeld(String s) {
        String id = s.toLowerCase(Locale.ROOT);
        return id.contains("choice_") || id.contains("band") || id.contains("scarf") || id.contains("specs")
                || id.equals("leftovers") || id.equals("life_orb") || id.equals("expert_belt")
                || id.equals("assault_vest") || id.equals("black_sludge") || id.equals("shell_bell")
                || id.equals("light_ball") || id.equals("thick_club") || id.equals("metal_powder")
                || id.equals("quick_powder") || id.equals("deep_sea_tooth") || id.equals("deep_sea_scale")
                || id.equals("soul_dew") || id.equals("adamant_orb") || id.equals("lustrous_orb")
                || id.equals("griseous_orb") || id.endsWith("_plate") || id.endsWith("_drive")
                || id.endsWith("_memory") || id.contains("booster_energy") || id.equals("eviolite")
                || id.equals("rocky_helmet") || id.equals("focus_sash") || id.equals("focus_band")
                || id.equals("scope_lens") || id.equals("wide_lens") || id.equals("zoom_lens")
                || id.equals("muscle_band") || id.equals("wise_glasses") || id.equals("razor_claw")
                || id.equals("razor_fang") || id.equals("kings_rock") || id.equals("flame_orb")
                || id.equals("toxic_orb") || id.equals("sticky_barb") || id.equals("iron_ball")
                || id.equals("lagging_tail") || id.equals("shed_shell") || id.equals("big_root")
                || id.equals("metronome") || id.equals("binding_band") || id.equals("grip_claw");
    }

    public static BlockRole blockRole(String id) {
        if (id == null) return BlockRole.GENERIC;
        String s = id.toLowerCase(Locale.ROOT);
        if (com.cobblemon.mod.block.MachineBlock.kindForId(s) != null) return BlockRole.MACHINE;
        // Plantable crop blocks first — some share names with medicine items (e.g. revival_herb)
        if (s.equals("revival_herb") || s.equals("medicinal_leek") || s.equals("hearty_grains")
                || s.equals("big_root") || s.equals("pep_up_flower") || s.contains("vivichoke")
                || s.contains("bugwort")) {
            return BlockRole.CROP;
        }
        if (isMedicine(s)) return BlockRole.MEDICINE_DISPLAY;
        if (s.endsWith("_berry")) return BlockRole.BERRY_CROP;
        if (s.contains("mulch")) return BlockRole.MULCH;
        if (s.endsWith("_leaves") || s.contains("leaves")) return BlockRole.LEAVES;
        // Only real plantable saplings — potted variants stay GENERIC decoration
        if (s.endsWith("_apricorn_sapling") || s.equals("saccharine_sapling")) return BlockRole.SAPLING;
        if ((s.endsWith("_sapling") || s.contains("sapling")) && !s.startsWith("potted_")) return BlockRole.CROP;
        if (s.endsWith("_log") || s.endsWith("_wood") || s.contains("_log_") || s.equals("apricorn_log") || s.equals("apricorn_wood")
                || s.contains("saccharine_log") || s.contains("saccharine_wood")) {
            return BlockRole.WOOD_LOG;
        }
        if (s.endsWith("_planks") || s.equals("apricorn_planks")) return BlockRole.WOOD_PLANKS;
        if (s.endsWith("_slab")) return BlockRole.WOOD_SLAB;
        if (s.endsWith("_stairs")) return BlockRole.WOOD_STAIRS;
        if (s.endsWith("_fence_gate")) return BlockRole.WOOD_FENCE_GATE;
        if (s.endsWith("_fence") && !s.contains("gate")) return BlockRole.WOOD_FENCE;
        if (s.endsWith("_door") && !s.contains("trap")) return BlockRole.WOOD_DOOR;
        if (s.endsWith("_trapdoor")) return BlockRole.WOOD_TRAPDOOR;
        if (s.endsWith("_button")) return BlockRole.WOOD_BUTTON;
        if (s.contains("pressure_plate")) return BlockRole.WOOD_PRESSURE;
        if (s.contains("_sign")) return BlockRole.WOOD_SIGN;
        if (s.endsWith("_ore") || s.endsWith("_stone_block") || s.contains("tumblestone")) return BlockRole.ORE;
        // Color mint plants (red_mint …) — not nature mints like adamant_mint (items only)
        if (s.equals("red_mint") || s.equals("blue_mint") || s.equals("cyan_mint")
                || s.equals("green_mint") || s.equals("pink_mint") || s.equals("white_mint")
                || s.endsWith("_mint_plant")) {
            return BlockRole.CROP;
        }
        if (s.contains("stage_") || s.contains("_stage") || s.contains("grains") || s.contains("leek")
                || s.contains("herb") || s.contains("vivichoke") || s.contains("bugwort") || s.contains("pep_up")) {
            return BlockRole.CROP;
        }
        if (s.endsWith("_apricorn") || s.equals("black_apricorn") || s.equals("blue_apricorn")
                || s.equals("green_apricorn") || s.equals("pink_apricorn") || s.equals("red_apricorn")
                || s.equals("white_apricorn") || s.equals("yellow_apricorn")) {
            return BlockRole.APRICORN;
        }
        return BlockRole.GENERIC;
    }

    public static boolean isMedicine(String s) {
        return s.equals("potion") || s.equals("super_potion") || s.equals("hyper_potion")
                || s.equals("max_potion") || s.equals("full_restore") || s.equals("full_heal")
                || s.equals("revive") || s.equals("max_revive")
                || s.equals("ether") || s.equals("max_ether") || s.equals("elixir") || s.equals("max_elixir")
                || s.equals("antidote") || s.equals("awakening") || s.equals("burn_heal")
                || s.equals("ice_heal") || s.equals("paralyze_heal") || s.equals("heal_powder")
                || s.equals("energy_root") || s.equals("revival_herb") || s.equals("remedy");
    }

    public static boolean isEvolutionStone(String s) {
        return s.equals("fire_stone") || s.equals("water_stone") || s.equals("thunder_stone")
                || s.equals("leaf_stone") || s.equals("moon_stone") || s.equals("sun_stone")
                || s.equals("dawn_stone") || s.equals("dusk_stone") || s.equals("shiny_stone")
                || s.equals("ice_stone");
    }

    /** Cobblemon ball id → exact ball type (Poké, Great, Master, …). */
    public static CubeBallTier ballTier(String id) {
        return CubeBallTier.byId(id);
    }

    public static boolean isCatchBall(String id) {
        return CubeBallTier.isCatchBall(id);
    }

    /**
     * Cobblemon potion heal amounts (approx mainline / bag scripts).
     * negative = full heal; -2 = revive half; -3 = revive full.
     */
    public static int medicineHealAmount(String id) {
        return switch (id.toLowerCase(Locale.ROOT)) {
            case "potion", "energy_root", "remedy" -> 20;
            case "super_potion" -> 50;
            case "hyper_potion" -> 120;
            case "max_potion", "full_restore", "heal_powder" -> -1; // full
            case "revive" -> -2;
            case "max_revive", "revival_herb" -> -3;
            case "oran_berry" -> 10;
            case "sitrus_berry" -> -4; // 25% max
            // PP restore codes (see UseItemOnMonPayload.applyMedicine)
            case "ether" -> -10;       // +10 PP one move
            case "max_ether" -> -11;   // full PP one move
            case "elixir" -> -12;      // +10 PP all moves
            case "max_elixir" -> -13;  // full PP all moves
            case "leppa_berry" -> -10;
            case "antidote", "awakening", "burn_heal", "ice_heal", "paralyze_heal", "full_heal" -> 0; // status-only
            default -> {
                if (id.endsWith("_berry")) yield 10;
                yield 20;
            }
        };
    }

    public static EvolutionMethod stoneMethod(String id) {
        return switch (id.toLowerCase(Locale.ROOT)) {
            case "fire_stone", "sun_stone" -> EvolutionMethod.FIRE_GEM;
            case "water_stone", "ice_stone" -> EvolutionMethod.WATER_GEM;
            case "thunder_stone" -> EvolutionMethod.THUNDER_GEM;
            case "leaf_stone" -> EvolutionMethod.LEAF_GEM;
            case "moon_stone", "dusk_stone", "dawn_stone", "shiny_stone" -> EvolutionMethod.MOON_GEM;
            default -> EvolutionMethod.NONE;
        };
    }

    public static Nature mintNature(String id) {
        // adamant_mint → adamant
        String key = id.toLowerCase(Locale.ROOT);
        if (key.endsWith("_mint")) {
            key = key.substring(0, key.length() - 5);
        }
        return Nature.byId(key);
    }

    public static int candyLevels(String id) {
        return switch (id.toLowerCase(Locale.ROOT)) {
            case "exp_candy_xs" -> 1;
            case "exp_candy_s" -> 2;
            case "exp_candy_m", "health_candy" -> 3;
            case "exp_candy_l" -> 5;
            case "exp_candy_xl", "rare_candy" -> 1; // rare candy = +1 level exactly
            default -> 1;
        };
    }

    public static boolean candyIsRare(String id) {
        return "rare_candy".equals(id);
    }
}

package com.cobblemon.mod.battle;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.cobblemon.mod.species.MonElement;
import com.cobblemon.mod.species.MonSpecies;

/**
 * Generates a level-up learnset for every species from type + stage.
 * Deterministic so saves stay consistent.
 */
public final class Learnsets {
    private Learnsets() {}

    /** Level → move learned at that level (and kept if known at catch). */
    public static Map<Integer, MonMove> forSpecies(MonSpecies species) {
        // Legendary psychic kit for Mew / Mewtwo
        if (species == MonSpecies.MEW || species == MonSpecies.MEWTWO) {
            return grokLearnset(species);
        }

        MonElement t = species.element();
        int stage = species.stage();
        Map<Integer, MonMove> map = new LinkedHashMap<>();

        // Universal basics
        map.put(1, MonMove.TACKLE);
        map.put(3, MonMove.GROWL);

        MonMove[] ladder = typeLadder(t);
        // Stage shifts power band: stage 1 early moves, stage 3 stronger earlier
        int[] levels = stage == 1
                ? new int[]{5, 10, 16, 22, 28, 34, 40, 48, 56, 65, 75, 85}
                : stage == 2
                ? new int[]{1, 8, 14, 20, 26, 32, 38, 46, 54, 62, 72, 82}
                : new int[]{1, 6, 12, 18, 24, 30, 36, 44, 52, 60, 70, 80};

        for (int i = 0; i < ladder.length && i < levels.length; i++) {
            map.put(levels[i], ladder[i]);
        }

        // Signature late-game
        if (stage >= 2) {
            map.put(Math.min(90, 50 + stage * 10), signature(t));
        }
        map.put(100, MonMove.BODY_SLAM);

        // Utility sprinkled in
        if (t == MonElement.ROCK || t == MonElement.STEEL) {
            map.put(12, MonMove.HARDEN);
        }
        if (t == MonElement.FIGHTING) {
            map.put(15, MonMove.FOCUS);
        }
        map.putIfAbsent(20, MonMove.REST_SOFT);

        return map;
    }

    /** Prefer Cobblemon datapack learnsets; fall back to procedural type ladder. */
    public static List<MonMove> movesKnownAtLevel(MonSpecies species, int level) {
        if (species == null) {
            return List.of(MonMove.TACKLE);
        }
        return DatapackLearnsets.movesKnownAtLevel(species.id(), level);
    }

    public static List<MonMove> movesKnownAtLevel(String speciesId, int level) {
        return DatapackLearnsets.movesKnownAtLevel(speciesId, level);
    }

    /** Procedural fallback (used by DatapackLearnsets when no pack data). */
    public static List<MonMove> proceduralMovesKnownAtLevel(MonSpecies species, int level) {
        level = Math.max(1, Math.min(100, level));
        List<Map.Entry<Integer, MonMove>> entries = new ArrayList<>(forSpecies(species).entrySet());
        entries.sort(Comparator.comparingInt(Map.Entry::getKey));
        List<MonMove> known = new ArrayList<>();
        for (Map.Entry<Integer, MonMove> e : entries) {
            if (e.getKey() > level) {
                break;
            }
            known.remove(e.getValue());
            known.add(e.getValue());
        }
        if (known.size() <= 4) {
            return known;
        }
        return new ArrayList<>(known.subList(known.size() - 4, known.size()));
    }

    /** Move learned exactly at this level, if any. */
    public static MonMove learnAt(MonSpecies species, int level) {
        if (species == null) {
            return null;
        }
        return learnAt(species.id(), level);
    }

    public static MonMove learnAt(String speciesId, int level) {
        return DatapackLearnsets.learnAt(speciesId, level);
    }

    public static MonMove proceduralLearnAt(MonSpecies species, int level) {
        return forSpecies(species).get(level);
    }

    private static MonMove[] typeLadder(MonElement t) {
        return switch (t) {
            case FIRE -> new MonMove[]{
                    MonMove.EMBER_SNAP, MonMove.FLAME_PAW, MonMove.QUICK_STRIKE, MonMove.HEAT_WAVE,
                    MonMove.BODY_SLAM, MonMove.INFERNO_CRASH, MonMove.HEAT_WAVE, MonMove.INFERNO_CRASH
            };
            case WATER -> new MonMove[]{
                    MonMove.SPLASH_JAB, MonMove.BUBBLE_BURST, MonMove.TIDE_CRASH, MonMove.BODY_SLAM,
                    MonMove.ABYSS_PULSE, MonMove.TIDE_CRASH, MonMove.ABYSS_PULSE, MonMove.BODY_SLAM
            };
            case GRASS -> new MonMove[]{
                    MonMove.LEAF_CLIP, MonMove.SEED_SHOT, MonMove.VINE_LASH, MonMove.SOLAR_BURST,
                    MonMove.BODY_SLAM, MonMove.VINE_LASH, MonMove.SOLAR_BURST, MonMove.HARDEN
            };
            case ELECTRIC -> new MonMove[]{
                    MonMove.STATIC_NIBBLE, MonMove.SPARK_ARC, MonMove.THUNDER_PAW, MonMove.QUICK_STRIKE,
                    MonMove.STORM_BOLT, MonMove.THUNDER_PAW, MonMove.STORM_BOLT, MonMove.BODY_SLAM
            };
            case FLYING -> new MonMove[]{
                    MonMove.GUST_FLUFF, MonMove.WING_BUFF, MonMove.SKY_DIVE, MonMove.QUICK_STRIKE,
                    MonMove.TEMPEST, MonMove.SKY_DIVE, MonMove.TEMPEST, MonMove.BODY_SLAM
            };
            case ROCK -> new MonMove[]{
                    MonMove.PEBBLE_TOSS, MonMove.HARDEN, MonMove.STONE_CRASH, MonMove.BODY_SLAM,
                    MonMove.STONE_CRASH, MonMove.DIRT_KICK, MonMove.BODY_SLAM, MonMove.STONE_CRASH
            };
            case GROUND -> new MonMove[]{
                    MonMove.DIRT_KICK, MonMove.HARDEN, MonMove.QUAKE_STOMP, MonMove.BODY_SLAM,
                    MonMove.QUAKE_STOMP, MonMove.DIRT_KICK, MonMove.BODY_SLAM, MonMove.QUAKE_STOMP
            };
            case FIGHTING -> new MonMove[]{
                    MonMove.QUICK_JAB, MonMove.FOCUS, MonMove.POWER_FIST, MonMove.BODY_SLAM,
                    MonMove.POWER_FIST, MonMove.QUICK_JAB, MonMove.BODY_SLAM, MonMove.POWER_FIST
            };
            case DARK -> new MonMove[]{
                    MonMove.SHADOW_NIP, MonMove.NIGHT_EDGE, MonMove.QUICK_STRIKE, MonMove.BODY_SLAM,
                    MonMove.NIGHT_EDGE, MonMove.SHADOW_NIP, MonMove.BODY_SLAM, MonMove.NIGHT_EDGE
            };
            case DRAGON -> new MonMove[]{
                    MonMove.SCALE_SWIPE, MonMove.DRACO_ROAR, MonMove.BODY_SLAM, MonMove.WING_BUFF,
                    MonMove.DRACO_ROAR, MonMove.SCALE_SWIPE, MonMove.BODY_SLAM, MonMove.DRACO_ROAR
            };
            case PSYCHIC -> new MonMove[]{
                    MonMove.MIND_PING, MonMove.PSY_BURST, MonMove.FOCUS, MonMove.BODY_SLAM,
                    MonMove.PSY_BURST, MonMove.MIND_PING, MonMove.BODY_SLAM, MonMove.PSY_BURST
            };
            case ICE -> new MonMove[]{
                    MonMove.FROST_NIP, MonMove.ICE_SHARD, MonMove.GLACIER_CRASH, MonMove.BODY_SLAM,
                    MonMove.GLACIER_CRASH, MonMove.ICE_SHARD, MonMove.BODY_SLAM, MonMove.GLACIER_CRASH
            };
            case STEEL -> new MonMove[]{
                    MonMove.METAL_TAP, MonMove.HARDEN, MonMove.IRON_CRASH, MonMove.BODY_SLAM,
                    MonMove.IRON_CRASH, MonMove.METAL_TAP, MonMove.BODY_SLAM, MonMove.IRON_CRASH
            };
            case FAIRY -> new MonMove[]{
                    MonMove.SPARKLE, MonMove.MOON_DUST, MonMove.QUICK_STRIKE, MonMove.BODY_SLAM,
                    MonMove.MOON_DUST, MonMove.SPARKLE, MonMove.BODY_SLAM, MonMove.MOON_DUST
            };
            case BUG -> new MonMove[]{
                    MonMove.NIBBLE, MonMove.SILK_SHOT, MonMove.QUICK_STRIKE, MonMove.BODY_SLAM,
                    MonMove.SILK_SHOT, MonMove.NIBBLE, MonMove.BODY_SLAM, MonMove.SILK_SHOT
            };
            case POISON -> new MonMove[]{
                    MonMove.TOXIN_DAB, MonMove.VENOM_SPIKE, MonMove.QUICK_STRIKE, MonMove.BODY_SLAM,
                    MonMove.VENOM_SPIKE, MonMove.TOXIN_DAB, MonMove.BODY_SLAM, MonMove.VENOM_SPIKE
            };
            case GHOST -> new MonMove[]{
                    MonMove.PHASE_TOUCH, MonMove.HAUNT, MonMove.SHADOW_NIP, MonMove.BODY_SLAM,
                    MonMove.HAUNT, MonMove.PHASE_TOUCH, MonMove.BODY_SLAM, MonMove.HAUNT
            };
            default -> new MonMove[]{
                    MonMove.TACKLE, MonMove.QUICK_STRIKE, MonMove.BODY_SLAM, MonMove.REST_SOFT,
                    MonMove.BODY_SLAM, MonMove.QUICK_STRIKE, MonMove.BODY_SLAM, MonMove.TACKLE
            };
        };
    }

    private static MonMove signature(MonElement t) {
        return switch (t) {
            case FIRE -> MonMove.INFERNO_CRASH;
            case WATER -> MonMove.ABYSS_PULSE;
            case GRASS -> MonMove.SOLAR_BURST;
            case ELECTRIC -> MonMove.STORM_BOLT;
            case FLYING -> MonMove.TEMPEST;
            case DRAGON -> MonMove.DRACO_ROAR;
            case PSYCHIC -> MonMove.PSY_BURST;
            case ICE -> MonMove.GLACIER_CRASH;
            case STEEL -> MonMove.IRON_CRASH;
            case DARK -> MonMove.NIGHT_EDGE;
            case FIGHTING -> MonMove.POWER_FIST;
            case ROCK -> MonMove.STONE_CRASH;
            case GROUND -> MonMove.QUAKE_STOMP;
            case FAIRY -> MonMove.MOON_DUST;
            case BUG -> MonMove.SILK_SHOT;
            case POISON -> MonMove.VENOM_SPIKE;
            case GHOST -> MonMove.HAUNT;
            default -> MonMove.BODY_SLAM;
        };
    }

    /**
     * Custom line for Grokling → Grokmon → Groknova.
     * Early tools, mid-game truth beam, late-game starforged finishers.
     */
    private static Map<Integer, MonMove> grokLearnset(MonSpecies species) {
        Map<Integer, MonMove> map = new LinkedHashMap<>();
        if (species.stage() >= 3) {
            map.put(1, MonMove.MAXIMUM_TRUTH);
        } else {
            map.put(1, MonMove.WIT_SPARK);
        }
        map.put(3, MonMove.GROWL);
        map.put(5, MonMove.ASK_EVERYTHING);
        map.put(8, MonMove.MIND_PING);
        map.put(12, MonMove.COSMIC_QUERY);
        map.put(16, MonMove.QUICK_STRIKE);
        map.put(18, MonMove.VOID_LAUGH);
        map.put(22, MonMove.FOCUS);
        map.put(26, MonMove.PSY_BURST);
        if (species.stage() >= 2) {
            map.put(10, MonMove.TRUTH_BEAM);
        }
        map.put(30, MonMove.TRUTH_BEAM);
        map.put(36, MonMove.NIGHT_EDGE);
        map.put(40, MonMove.MAXIMUM_TRUTH);
        map.put(48, MonMove.BODY_SLAM);
        map.put(56, MonMove.DRACO_ROAR);
        map.put(65, MonMove.MAXIMUM_TRUTH);
        if (species.stage() >= 3) {
            map.put(20, MonMove.STARFORGE_WIT);
        }
        map.put(75, MonMove.STARFORGE_WIT);
        map.put(90, MonMove.STARFORGE_WIT);
        map.put(100, MonMove.STARFORGE_WIT);
        return map;
    }
}

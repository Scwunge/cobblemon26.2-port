package com.cobblemon.mod.battle;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.cobblemon.mod.species.SpeciesHandle;
import com.cobblemon.mod.species.SpeciesRegistry;

/**
 * Level-up learnsets from Cobblemon species datapack ({@code "12:razorleaf"} style).
 * Resolves to {@link BattleMove} (Showdown stats when available).
 * Falls back to procedural {@link Learnsets} when data is missing.
 */
public final class DatapackLearnsets {
    private static final Pattern LV_MOVE = Pattern.compile("^(\\d+):([a-z0-9_]+)$", Pattern.CASE_INSENSITIVE);

    private DatapackLearnsets() {}

    /** Level → official move id (canonicalized). */
    public static Map<Integer, String> forSpeciesIds(String speciesId) {
        SpeciesRegistry.SpeciesData data = SpeciesRegistry.get(speciesId).orElse(null);
        if (data == null || data.levelMoves() == null || data.levelMoves().isEmpty()) {
            Map<Integer, String> fallback = new LinkedHashMap<>();
            for (var e : Learnsets.forSpecies(SpeciesHandle.of(speciesId).asEnumOrFallback()).entrySet()) {
                fallback.put(e.getKey(), ShowdownMoveDex.canonicalize(e.getValue().id()));
            }
            return fallback;
        }
        Map<Integer, String> map = new LinkedHashMap<>();
        for (String raw : data.levelMoves()) {
            Matcher m = LV_MOVE.matcher(raw.trim());
            if (!m.matches()) {
                continue;
            }
            int lv = Integer.parseInt(m.group(1));
            map.put(lv, ShowdownMoveDex.canonicalize(m.group(2)));
        }
        if (map.isEmpty()) {
            Map<Integer, String> fallback = new LinkedHashMap<>();
            for (var e : Learnsets.forSpecies(SpeciesHandle.of(speciesId).asEnumOrFallback()).entrySet()) {
                fallback.put(e.getKey(), ShowdownMoveDex.canonicalize(e.getValue().id()));
            }
            return fallback;
        }
        return map;
    }

    public static Map<Integer, MonMove> forSpeciesId(String speciesId) {
        Map<Integer, MonMove> map = new LinkedHashMap<>();
        for (var e : forSpeciesIds(speciesId).entrySet()) {
            map.put(e.getKey(), MoveAliases.resolve(e.getValue()));
        }
        return map;
    }

    public static List<MonMove> movesKnownAtLevel(String speciesId, int level) {
        return battleMovesKnownAtLevel(speciesId, level).stream()
                .map(bm -> MoveAliases.resolve(bm.getId()))
                .toList();
    }

    public static List<BattleMove> battleMovesKnownAtLevel(String speciesId, int level) {
        level = Math.max(1, Math.min(100, level));
        SpeciesRegistry.SpeciesData data = SpeciesRegistry.get(speciesId).orElse(null);
        if (data != null && data.levelMoves() != null && !data.levelMoves().isEmpty()) {
            List<Map.Entry<Integer, String>> entries = new ArrayList<>(forSpeciesIds(speciesId).entrySet());
            entries.sort(Comparator.comparingInt(Map.Entry::getKey));
            List<String> known = new ArrayList<>();
            for (Map.Entry<Integer, String> e : entries) {
                if (e.getKey() > level) {
                    break;
                }
                known.remove(e.getValue());
                known.add(e.getValue());
            }
            if (!known.isEmpty()) {
                if (known.size() > 4) {
                    known = new ArrayList<>(known.subList(known.size() - 4, known.size()));
                }
                return known.stream().map(BattleMove::resolve).toList();
            }
        }
        return Learnsets.proceduralMovesKnownAtLevel(SpeciesHandle.of(speciesId).asEnumOrFallback(), level)
                .stream()
                .map(m -> BattleMove.resolve(m.id()))
                .toList();
    }

    public static MonMove learnAt(String speciesId, int level) {
        SpeciesRegistry.SpeciesData data = SpeciesRegistry.get(speciesId).orElse(null);
        if (data != null && data.levelMoves() != null && !data.levelMoves().isEmpty()) {
            String id = forSpeciesIds(speciesId).get(level);
            if (id != null) {
                return MoveAliases.resolve(id);
            }
        }
        return Learnsets.proceduralLearnAt(SpeciesHandle.of(speciesId).asEnumOrFallback(), level);
    }

    public static BattleMove battleLearnAt(String speciesId, int level) {
        MonMove m = learnAt(speciesId, level);
        if (m == null) {
            return null;
        }
        SpeciesRegistry.SpeciesData data = SpeciesRegistry.get(speciesId).orElse(null);
        if (data != null && data.levelMoves() != null && !data.levelMoves().isEmpty()) {
            String id = forSpeciesIds(speciesId).get(level);
            if (id != null) {
                return BattleMove.resolve(id);
            }
        }
        return BattleMove.resolve(m.id());
    }
}

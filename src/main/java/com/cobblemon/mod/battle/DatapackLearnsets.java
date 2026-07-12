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
 * Falls back to procedural {@link Learnsets} when data is missing.
 */
public final class DatapackLearnsets {
    private static final Pattern LV_MOVE = Pattern.compile("^(\\d+):([a-z0-9_]+)$", Pattern.CASE_INSENSITIVE);

    private DatapackLearnsets() {}

    public static Map<Integer, MonMove> forSpeciesId(String speciesId) {
        SpeciesRegistry.SpeciesData data = SpeciesRegistry.get(speciesId).orElse(null);
        if (data == null || data.levelMoves() == null || data.levelMoves().isEmpty()) {
            return Learnsets.forSpecies(SpeciesHandle.of(speciesId).asEnumOrFallback());
        }
        Map<Integer, MonMove> map = new LinkedHashMap<>();
        for (String raw : data.levelMoves()) {
            Matcher m = LV_MOVE.matcher(raw.trim());
            if (!m.matches()) {
                continue;
            }
            int lv = Integer.parseInt(m.group(1));
            MonMove move = MoveAliases.resolve(m.group(2));
            map.put(lv, move);
        }
        if (map.isEmpty()) {
            return Learnsets.forSpecies(SpeciesHandle.of(speciesId).asEnumOrFallback());
        }
        return map;
    }

    public static List<MonMove> movesKnownAtLevel(String speciesId, int level) {
        level = Math.max(1, Math.min(100, level));
        Map<Integer, MonMove> table = forSpeciesId(speciesId);
        // Detect pure procedural table vs pack: if registry has level moves, use them
        SpeciesRegistry.SpeciesData data = SpeciesRegistry.get(speciesId).orElse(null);
        if (data != null && data.levelMoves() != null && !data.levelMoves().isEmpty()) {
            List<Map.Entry<Integer, MonMove>> entries = new ArrayList<>(table.entrySet());
            entries.sort(Comparator.comparingInt(Map.Entry::getKey));
            List<MonMove> known = new ArrayList<>();
            for (Map.Entry<Integer, MonMove> e : entries) {
                if (e.getKey() > level) {
                    break;
                }
                known.remove(e.getValue());
                known.add(e.getValue());
            }
            if (!known.isEmpty()) {
                if (known.size() <= 4) {
                    return known;
                }
                return new ArrayList<>(known.subList(known.size() - 4, known.size()));
            }
        }
        return Learnsets.proceduralMovesKnownAtLevel(SpeciesHandle.of(speciesId).asEnumOrFallback(), level);
    }

    public static MonMove learnAt(String speciesId, int level) {
        SpeciesRegistry.SpeciesData data = SpeciesRegistry.get(speciesId).orElse(null);
        if (data != null && data.levelMoves() != null && !data.levelMoves().isEmpty()) {
            MonMove m = forSpeciesId(speciesId).get(level);
            if (m != null) {
                return m;
            }
        }
        return Learnsets.proceduralLearnAt(SpeciesHandle.of(speciesId).asEnumOrFallback(), level);
    }
}

package com.cobblemon.mod.battle;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.cobblemon.mod.species.MonGender;
import com.cobblemon.mod.species.OwnedMon;
import com.cobblemon.mod.species.SpeciesRegistry;
import net.minecraft.util.RandomSource;

/**
 * E3 — egg / inherited moves when breeding.
 * <p>
 * Rules (playable Cobblemon-style subset):
 * <ol>
 *   <li>Father's (then mother's) known moves that sit in the baby's {@code egg:} pool pass first.</li>
 *   <li>Either parent's known level-up moves for the baby species may pass next.</li>
 *   <li>Remaining slots fill with the baby's level-1 learnset.</li>
 * </ol>
 */
public final class EggMoveInheritance {
    private EggMoveInheritance() {}

    /** Official egg-move ids for a species (from species_index / datapack). */
    public static List<String> eggPool(String speciesId) {
        if (speciesId == null || speciesId.isBlank()) {
            return List.of();
        }
        return SpeciesRegistry.get(speciesId)
                .map(SpeciesRegistry.SpeciesData::eggMoves)
                .orElse(List.of())
                .stream()
                .map(EggMoveInheritance::canon)
                .filter(s -> !s.isEmpty())
                .distinct()
                .toList();
    }

    /** Level-up move ids the baby can ever learn by leveling. */
    public static Set<String> levelUpPool(String speciesId) {
        Set<String> out = new LinkedHashSet<>();
        for (String id : DatapackLearnsets.forSpeciesIds(speciesId).values()) {
            String c = canon(id);
            if (!c.isEmpty()) {
                out.add(c);
            }
        }
        return out;
    }

    /**
     * Build up to {@link OwnedMon#MAX_MOVES} move ids for a hatched baby.
     *
     * @param babySpecies mother species (egg species)
     */
    public static List<String> inherit(OwnedMon parentA, OwnedMon parentB, String babySpecies, RandomSource random) {
        String species = babySpecies == null || babySpecies.isBlank() ? "rattata" : babySpecies.toLowerCase(Locale.ROOT);
        Set<String> egg = new LinkedHashSet<>(eggPool(species));
        Set<String> levelUp = levelUpPool(species);

        OwnedMon father = pickFather(parentA, parentB);
        OwnedMon mother = pickMother(parentA, parentB, father);

        List<String> out = new ArrayList<>(OwnedMon.MAX_MOVES);

        // 1) Egg moves — father first (classic), then mother
        passMatching(father, egg, out);
        passMatching(mother, egg, out);

        // 2) Level-up moves known by either parent that the baby can learn
        passMatching(father, levelUp, out);
        passMatching(mother, levelUp, out);

        // 3) Fill with level-1 kit
        List<String> base = DatapackLearnsets.battleMovesKnownAtLevel(species, 1).stream()
                .map(BattleMove::getId)
                .map(EggMoveInheritance::canon)
                .filter(s -> !s.isEmpty())
                .toList();
        for (String id : base) {
            if (out.size() >= OwnedMon.MAX_MOVES) {
                break;
            }
            if (!out.contains(id)) {
                out.add(id);
            }
        }

        // Safety: never empty
        if (out.isEmpty()) {
            out.add("tackle");
        }

        // Light shuffle of inherited slots only when we have room noise (keeps L1 fill stable)
        if (random != null && out.size() > 2 && random.nextFloat() < 0.15f) {
            int i = random.nextInt(Math.min(2, out.size()));
            int j = random.nextInt(out.size());
            if (i != j) {
                String tmp = out.get(i);
                out.set(i, out.get(j));
                out.set(j, tmp);
            }
        }
        return List.copyOf(out.subList(0, Math.min(OwnedMon.MAX_MOVES, out.size())));
    }

    private static void passMatching(OwnedMon parent, Set<String> pool, List<String> out) {
        if (parent == null || pool == null || pool.isEmpty() || out.size() >= OwnedMon.MAX_MOVES) {
            return;
        }
        for (String raw : parent.moveIds()) {
            if (out.size() >= OwnedMon.MAX_MOVES) {
                return;
            }
            String id = canon(raw);
            if (id.isEmpty() || out.contains(id)) {
                continue;
            }
            if (pool.contains(id)) {
                out.add(id);
            }
        }
    }

    private static OwnedMon pickFather(OwnedMon a, OwnedMon b) {
        if (a != null && a.gender() == MonGender.MALE) {
            return a;
        }
        if (b != null && b.gender() == MonGender.MALE) {
            return b;
        }
        return a != null ? a : b;
    }

    private static OwnedMon pickMother(OwnedMon a, OwnedMon b, OwnedMon father) {
        if (a != null && a.gender() == MonGender.FEMALE) {
            return a;
        }
        if (b != null && b.gender() == MonGender.FEMALE) {
            return b;
        }
        // Genderless / same — use the non-father parent
        if (a != null && a != father) {
            return a;
        }
        if (b != null && b != father) {
            return b;
        }
        return father;
    }

    private static String canon(String id) {
        if (id == null || id.isBlank()) {
            return "";
        }
        return ShowdownMoveDex.canonicalize(id);
    }
}

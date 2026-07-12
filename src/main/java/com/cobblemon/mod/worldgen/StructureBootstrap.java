package com.cobblemon.mod.worldgen;

import com.cobblemon.mod.Cobblemon;

/**
 * S1 — structure policy for the community 26.2 port.
 * <p>
 * <b>Never</b> enable Cobblemon structure <em>tags</em> or {@code worldgen/structure_set}
 * until matching structure definitions exist for this MC version. Doing so freezes
 * world creation with {@code Unbound values in minecraft:worldgen/structure}.
 * <p>
 * Safe landmarks live as {@link Feature}s only:
 * <ul>
 *   <li>{@link MiniRuinFeature} — mossy ruin rings</li>
 *   <li>{@link LandmarkFeature.Kind#CENTER} — center pad</li>
 *   <li>{@link LandmarkFeature.Kind#TOWER} — lookout tower stub</li>
 *   <li>{@link LandmarkFeature.Kind#DIG_SITE} — fossil dig pit</li>
 * </ul>
 * Placed via {@link OverworldDecorationFeature} (one biome-modifier entry, no cycles).
 */
public final class StructureBootstrap {
    private static boolean logged;

    /** Human-readable list of active safe landmark feature ids. */
    public static final String[] SAFE_LANDMARK_FEATURES = {
            "cobblemon:mini_ruin",
            "cobblemon:landmark_center",
            "cobblemon:landmark_tower",
            "cobblemon:landmark_dig_site",
    };

    private StructureBootstrap() {}

    public static void bootstrap() {
        if (logged) {
            return;
        }
        logged = true;
        Cobblemon.LOGGER.info(
                "Structure registry: SAFE MODE — no structure tags/sets registered. "
                        + "Landmarks via features only: {} (plus apricorn/berry/mint/ore decoration).",
                String.join(", ", SAFE_LANDMARK_FEATURES)
        );
    }

    /** True — we intentionally do not touch the structure registry. */
    public static boolean isStructureRegistryDisabled() {
        return true;
    }
}

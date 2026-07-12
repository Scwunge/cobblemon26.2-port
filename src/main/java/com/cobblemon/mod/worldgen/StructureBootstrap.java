package com.cobblemon.mod.worldgen;

import com.cobblemon.mod.Cobblemon;

/**
 * Structure worldgen is intentionally deferred for the 26.1 community port.
 * <p>
 * Cobblemon 1.7.3 ships hundreds of structure NBTs + tags. Importing structure
 * <em>tags</em> without matching structure definitions freezes world creation
 * with {@code Unbound values in minecraft:worldgen/structure}.
 * <p>
 * When the official team ports structures:
 * <ol>
 *   <li>Register structure types / pieces for MC 26.1</li>
 *   <li>Copy {@code structure/*.nbt} + {@code worldgen/structure/*.json}</li>
 *   <li>Only then enable structure tags / biome has_structure tags</li>
 * </ol>
 * Until then, this class documents the gate and logs once at boot.
 */
public final class StructureBootstrap {
    private static boolean logged;

    private StructureBootstrap() {}

    public static void bootstrap() {
        if (logged) {
            return;
        }
        logged = true;
        Cobblemon.LOGGER.info(
                "Structure registry: deferred (safe mode) — full Cobblemon NBT ruins not registered. " +
                "ACTIVE worldgen: apricorn/saccharine trees, berries, mints/herbs, evo-stone ores, mini-ruins (feature)."
        );
    }
}


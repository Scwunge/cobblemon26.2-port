package com.cobblemon.mod.species;

/**
 * One evolution option from a species (level, gem, or trade).
 * A species may have several (e.g. Eevee → Vaporeon / Jolteon / Flareon).
 */
public record EvoBranch(MonSpecies into, EvolutionMethod method, int level) {
    public boolean matchesLevel(int monLevel) {
        return method == EvolutionMethod.LEVEL || method == EvolutionMethod.TRADE
                ? monLevel >= level
                : false;
    }

    public boolean matchesGem(EvolutionMethod gem) {
        return method == gem && method != EvolutionMethod.LEVEL
                && method != EvolutionMethod.TRADE
                && method != EvolutionMethod.NONE;
    }
}

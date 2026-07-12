package com.cobblemon.mod.pokedex

/**
 * Pokédex registration progress for a species id (lite tracker).
 * Persist still uses seen/caught sets on [com.cobblemon.mod.party.PlayerPokedex];
 * this enum is the derived view for UI / queries.
 */
enum class PokedexProgress {
    NONE,
    SEEN,
    CAUGHT;

    fun isSeen(): Boolean = this != NONE

    fun isCaught(): Boolean = this == CAUGHT
}

package com.cobblemon.mod.kotlin

import com.cobblemon.mod.Cobblemon
import com.cobblemon.mod.battle.BattleMove
import com.cobblemon.mod.battle.ShowdownMoveDex
import com.cobblemon.mod.species.StarterCatalog
import com.cobblemon.mod.util.BitUtils
import com.cobblemon.mod.util.CobblemonResource
import com.cobblemon.mod.util.Collections
import com.cobblemon.mod.util.DataKeys
import com.cobblemon.mod.util.Lang

/**
 * Kotlin bootstrap — proves mixed Java/Kotlin toolchain and logs util readiness.
 * Call from Java during common setup ([com.cobblemon.mod.Cobblemon]).
 */
object CobblemonKotlin {
    const val LANG: String = "kotlin"

    @JvmStatic
    fun bootstrap() {
        val sampleId = CobblemonResource.id("poke_ball")
        val starterCount = StarterCatalog.categories().size
        val bitProbe = BitUtils.getBitForByte(BitUtils.setBitForByte(0, 1, true), 1)
        val emptyPick = Collections.weightedSelection(emptyList<String>()) { 1.0 }
        val speciesKey = DataKeys.POKEMON_SPECIES_IDENTIFIER
        val title = Lang.ui("starter.title").string
        val vine = BattleMove.resolve("vinewhip")

        Cobblemon.LOGGER.info(
            "Cobblemon Kotlin ready — starters={} showdownMoves={} vinewhip={}pwr/{} id={} bit={} emptyPick={} key={} title={}",
            starterCount,
            ShowdownMoveDex.size(),
            vine.power,
            vine.element,
            sampleId,
            bitProbe,
            emptyPick,
            speciesKey,
            title,
        )
    }
}

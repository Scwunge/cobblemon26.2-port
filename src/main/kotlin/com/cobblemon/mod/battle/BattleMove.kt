package com.cobblemon.mod.battle

import com.cobblemon.mod.species.MonStatus
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent

/**
 * Unified battle move — prefers official Showdown stats, falls back to [MonMove].
 */
data class BattleMove(
    val id: String,
    val displayName: String,
    val element: com.cobblemon.mod.species.MonElement,
    val category: MoveCategory,
    val power: Int,
    /** 0–100 hit chance; 101 = always hits. */
    val accuracy: Int,
    val priority: Int,
    val maxPp: Int,
    val blurb: String = "",
    val selfBoosts: List<ShowdownMoveDex.StatBoost> = emptyList(),
    val drainFraction: Float = 0f,
    val secondary: ShowdownMoveDex.Secondary? = null,
) {
    fun isStatus(): Boolean = category == MoveCategory.STATUS || power <= 0

    fun englishName(): String = displayName

    fun displayNameComponent(): MutableComponent {
        val fromLang = Component.translatable("cobblemon.move.$id")
        val s = fromLang.string
        if (s.isNullOrBlank() || s.contains('.') || s.startsWith("cobblemon.") || s.startsWith("move.")) {
            return Component.literal(displayName)
        }
        return Component.literal(s)
    }

    companion object {
        @JvmStatic
        fun of(mon: MonMove): BattleMove {
            val base = BattleMove(
                id = mon.id(),
                displayName = mon.englishName(),
                element = mon.element(),
                category = mon.category(),
                power = mon.power(),
                accuracy = mon.accuracy(),
                priority = mon.priority(),
                maxPp = mon.defaultMaxPp(),
                blurb = mon.blurb(),
            )
            return applyLegacyEffects(base, mon)
        }

        @JvmStatic
        fun of(entry: ShowdownMoveDex.Entry): BattleMove = BattleMove(
            id = entry.id,
            displayName = entry.name,
            element = entry.type,
            category = entry.category,
            power = entry.basePower,
            accuracy = entry.accuracy,
            priority = entry.priority,
            maxPp = entry.pp,
            blurb = "",
            selfBoosts = entry.selfBoosts,
            drainFraction = entry.drainFraction,
            secondary = entry.secondary,
        )

        @JvmStatic
        fun resolve(rawId: String?): BattleMove {
            if (rawId.isNullOrBlank()) {
                return of(MonMove.TACKLE)
            }
            val key = ShowdownMoveDex.canonicalize(rawId)
            ShowdownMoveDex.get(key)?.let { return of(it) }

            MonMove.byId(rawId).orElse(null)?.let { return of(it) }
            MonMove.byId(key).orElse(null)?.let { return of(it) }

            val aliased = MoveAliases.resolve(rawId)
            ShowdownMoveDex.get(ShowdownMoveDex.canonicalize(rawId))?.let { return of(it) }
            return of(aliased)
        }

        /** Map our simplified MonMove set into boost/status secondaries. */
        private fun applyLegacyEffects(base: BattleMove, mon: MonMove): BattleMove {
            return when (mon) {
                MonMove.GROWL -> base.copy(
                    secondary = ShowdownMoveDex.Secondary(100, null, listOf(ShowdownMoveDex.StatBoost(StatKind.ATK, -1))),
                )
                MonMove.HARDEN -> base.copy(
                    selfBoosts = listOf(ShowdownMoveDex.StatBoost(StatKind.DEF, 1)),
                )
                MonMove.FOCUS, MonMove.ASK_EVERYTHING -> base.copy(
                    selfBoosts = listOf(ShowdownMoveDex.StatBoost(StatKind.ATK, 1)),
                )
                MonMove.REST_SOFT -> base
                MonMove.EMBER_SNAP, MonMove.FLAME_PAW, MonMove.HEAT_WAVE -> base.copy(
                    secondary = ShowdownMoveDex.Secondary(10, MonStatus.BURN, emptyList()),
                )
                MonMove.STATIC_NIBBLE, MonMove.SPARK_ARC, MonMove.THUNDER_PAW, MonMove.STORM_BOLT -> base.copy(
                    secondary = ShowdownMoveDex.Secondary(10, MonStatus.PARALYSIS, emptyList()),
                )
                MonMove.TOXIN_DAB, MonMove.VENOM_SPIKE -> base.copy(
                    secondary = ShowdownMoveDex.Secondary(30, MonStatus.POISON, emptyList()),
                )
                MonMove.FROST_NIP, MonMove.ICE_SHARD, MonMove.GLACIER_CRASH -> base.copy(
                    secondary = ShowdownMoveDex.Secondary(10, MonStatus.FREEZE, emptyList()),
                )
                else -> base
            }
        }
    }
}

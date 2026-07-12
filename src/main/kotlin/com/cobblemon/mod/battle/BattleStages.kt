package com.cobblemon.mod.battle

/**
 * Mainline-style stat stages: −6 … +6.
 * Multipliers: stage ≥ 0 → (2+s)/2 ; stage &lt; 0 → 2/(2−s)
 */
enum class StatKind {
    ATK, DEF, SPA, SPD, SPE, ACC, EVA;

    companion object {
        @JvmStatic
        fun fromShowdown(key: String): StatKind? = when (key.lowercase()) {
            "atk", "attack" -> ATK
            "def", "defense", "defence" -> DEF
            "spa", "spatk", "specialattack", "special_attack" -> SPA
            "spd", "spdef", "specialdefense", "special_defence", "specialdefence" -> SPD
            "spe", "speed" -> SPE
            "accuracy", "acc" -> ACC
            "evasion", "eva", "evasiveness" -> EVA
            else -> null
        }
    }
}

class BattleStages {
    private val stages = IntArray(StatKind.entries.size)

    fun get(kind: StatKind): Int = stages[kind.ordinal]

    /** Apply delta; returns clamped new stage. */
    fun add(kind: StatKind, delta: Int): Int {
        val i = kind.ordinal
        stages[i] = (stages[i] + delta).coerceIn(-6, 6)
        return stages[i]
    }

    fun clear() {
        stages.fill(0)
    }

    fun copyFrom(other: BattleStages) {
        System.arraycopy(other.stages, 0, stages, 0, stages.size)
    }

    fun multiplier(kind: StatKind): Float {
        val s = get(kind)
        return if (s >= 0) {
            (2f + s) / 2f
        } else {
            2f / (2f - s)
        }
    }

    /** Accuracy stage for attacker vs target evasion (relative). */
    fun accuracyMul(targetEvasion: BattleStages): Float {
        val stage = (get(StatKind.ACC) - targetEvasion.get(StatKind.EVA)).coerceIn(-6, 6)
        return if (stage >= 0) {
            (3f + stage) / 3f
        } else {
            3f / (3f - stage)
        }
    }

    companion object {
        @JvmStatic
        fun stageLabel(delta: Int): String = when {
            delta >= 3 -> "rose drastically!"
            delta == 2 -> "rose sharply!"
            delta == 1 -> "rose!"
            delta == -1 -> "fell!"
            delta == -2 -> "harshly fell!"
            delta <= -3 -> "severely fell!"
            else -> "didn't change."
        }

        @JvmStatic
        fun statName(kind: StatKind): String = when (kind) {
            StatKind.ATK -> "Attack"
            StatKind.DEF -> "Defense"
            StatKind.SPA -> "Sp. Atk"
            StatKind.SPD -> "Sp. Def"
            StatKind.SPE -> "Speed"
            StatKind.ACC -> "accuracy"
            StatKind.EVA -> "evasiveness"
        }
    }
}

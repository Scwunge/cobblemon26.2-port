package com.cobblemon.mod.battle

import com.cobblemon.mod.battle.graal.ShowdownCombatBridge
import com.cobblemon.mod.battle.graal.ShowdownRunner
import com.cobblemon.mod.battle.graal.ShowdownSim

/**
 * Preference / bridge layer for battle backends.
 *
 * - [Mode.NATIVE] — pure JVM damage ([BattleCalc])
 * - [Mode.SHOWDOWN_PROBE] — Graal up; lookups
 * - [Mode.SHOWDOWN_COMBAT] — JS damage formula active
 * - [Mode.SHOWDOWN_SIM] — CommonJS Showdown `index.js` / Dex loaded
 */
object BattleEngine {
    enum class Mode {
        NATIVE,
        SHOWDOWN_PROBE,
        SHOWDOWN_COMBAT,
        /** Full (or dex-fallback) Showdown sim environment ready. */
        SHOWDOWN_SIM,
    }

    @JvmStatic
    fun preferredMode(): Mode = when {
        ShowdownSim.isReady() && ShowdownCombatBridge.isReady() -> Mode.SHOWDOWN_SIM
        ShowdownSim.isReady() -> Mode.SHOWDOWN_SIM
        ShowdownCombatBridge.isReady() -> Mode.SHOWDOWN_COMBAT
        ShowdownRunner.isAvailable() -> Mode.SHOWDOWN_PROBE
        else -> Mode.NATIVE
    }

    @JvmStatic
    fun resolveMovePower(moveId: String?): Int {
        val native = BattleMove.resolve(moveId).power
        if (preferredMode() == Mode.NATIVE) return native
        val fromJs = ShowdownRunner.getMovePower(moveId ?: return native)
        return fromJs ?: native
    }

    /** True when damaging hits should try the JS formula first. */
    @JvmStatic
    fun useShowdownCombat(): Boolean = ShowdownCombatBridge.isReady()

    /** Native combat always remains the fallback path inside [BattleCalc]. */
    @JvmStatic
    fun useNativeCombat(): Boolean = !useShowdownCombat()

    @JvmStatic
    fun describe(): String {
        val mode = preferredMode()
        val runner = if (ShowdownRunner.isAvailable()) "ok" else "no"
        val combat = if (ShowdownCombatBridge.isReady()) {
            "JS(${ShowdownCombatBridge.lastMessage()})"
        } else {
            "native(${ShowdownCombatBridge.lastMessage()})"
        }
        val sim = if (ShowdownSim.isReady()) {
            "sim(${ShowdownSim.lastMessage()})"
        } else {
            "sim-off(${ShowdownSim.lastMessage()})"
        }
        return "BattleEngine mode=$mode · damage=$combat · $sim · runner=$runner"
    }
}

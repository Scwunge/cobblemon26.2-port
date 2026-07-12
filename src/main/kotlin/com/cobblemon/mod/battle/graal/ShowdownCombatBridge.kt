package com.cobblemon.mod.battle.graal

import com.cobblemon.mod.Cobblemon
import com.cobblemon.mod.battle.BattleMove
import com.cobblemon.mod.battle.BattleStages
import com.cobblemon.mod.battle.MoveCategory
import com.cobblemon.mod.battle.ShowdownMoveDex
import com.cobblemon.mod.battle.StatKind
import com.cobblemon.mod.species.OwnedMon
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.HostAccess
import org.graalvm.polyglot.Value
import java.util.concurrent.atomic.AtomicBoolean
import java.util.zip.ZipInputStream
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.outputStream
import kotlin.io.path.pathString
import kotlin.io.path.writeText

/**
 * Showdown combat bridge v1 — runs a **Showdown-style damage formula in Graal JS**
 * using move/species data we already load natively.
 *
 * This is NOT the full node Showdown sim (that needs CommonJS + multi-file require).
 * It is the real next step: live combat can prefer JS damage when the bridge is ready.
 *
 * Feature flag: [enabled] (default true). Falls back to native always on failure.
 */
object ShowdownCombatBridge {
    /**
     * When true and [isReady], [BattleCalc] may use JS for the core damage roll.
     * Set false to force native damage only.
     */
    @JvmField
    @Volatile
    var enabled: Boolean = true

    @Volatile
    private var ready: Boolean = false

    @Volatile
    private var lastMessage: String = "not bootstrapped"

    @Volatile
    private var context: Context? = null

    private val typeChartInjected = AtomicBoolean(false)

    @JvmStatic
    fun isReady(): Boolean = ready && enabled && context != null

    @JvmStatic
    fun lastMessage(): String = lastMessage

    /**
     * Bootstrap after [ShowdownRunner.bootstrap]. Safe; never throws.
     */
    @JvmStatic
    fun bootstrap() {
        ready = false
        lastMessage = "starting"
        closeQuiet()
        if (!enabled) {
            lastMessage = "disabled by flag"
            Cobblemon.LOGGER.info("ShowdownCombatBridge: disabled (enabled=false)")
            return
        }
        try {
            // Prefer reusing spike/runner success signal
            if (!ShowdownRunner.isAvailable() && !GraalSpike.lastSuccess) {
                lastMessage = "Graal unavailable"
                Cobblemon.LOGGER.warn("ShowdownCombatBridge: skip — Graal not available")
                return
            }

            try {
                System.setProperty("polyglot.engine.WarnInterpreterOnly", "false")
            } catch (_: Throwable) {
            }

            val ctx = Context.newBuilder("js")
                .option("engine.WarnInterpreterOnly", "false")
                .allowHostAccess(HostAccess.NONE)
                .allowAllAccess(false)
                .build()
            context = ctx

            extractShowdownHint()
            injectDamageEngine(ctx)

            // Smoke: level 50, 100 power, 100 atk, 100 def → base roughly ((2*50/5+2)*100*100/100)/50+2 = 42
            val smoke = ctx.eval(
                "js",
                """
                globalThis.__cobbleDamage({
                  level: 50, power: 100, atk: 100, def: 100,
                  stab: 1.5, typeMult: 2.0, other: 1.0,
                  crit: false, roll: 1.0, hits: 1
                })
                """.trimIndent(),
            )
            val smokeDmg = smoke.getMember("damage").asInt()
            if (smokeDmg < 1) {
                throw IllegalStateException("smoke damage invalid: $smokeDmg")
            }

            ready = true
            lastMessage = "ok; smokeDamage=$smokeDmg; extract=${lastExtractNote}"
            Cobblemon.LOGGER.info("ShowdownCombatBridge: READY — {}", lastMessage)
        } catch (t: Throwable) {
            ready = false
            lastMessage = t.javaClass.simpleName + ": " + (t.message ?: "")
            Cobblemon.LOGGER.error(
                "ShowdownCombatBridge: FAILURE — {} — combat stays native",
                lastMessage,
                t,
            )
            closeQuiet()
        }
    }

    @Volatile
    private var lastExtractNote: String = "none"

    /**
     * Extract showdown.zip to a game-relative folder for future full-sim work
     * (config/cobblemon/showdown/). Best-effort only.
     */
    private fun extractShowdownHint() {
        try {
            val dest = java.nio.file.Path.of("config", "cobblemon", "showdown")
            if (dest.resolve(".extracted").exists()) {
                lastExtractNote = "already at ${dest.pathString}"
                return
            }
            val stream = ShowdownCombatBridge::class.java.getResourceAsStream("/data/cobblemon/showdown.zip")
            if (stream == null) {
                lastExtractNote = "zip missing"
                return
            }
            var count = 0
            stream.use { raw ->
                ZipInputStream(raw).use { zin ->
                    var entry = zin.nextEntry
                    while (entry != null) {
                        if (!entry.isDirectory) {
                            val out = dest.resolve(entry.name)
                            out.parent?.createDirectories()
                            out.outputStream().use { os -> zin.copyTo(os) }
                            count++
                        }
                        entry = zin.nextEntry
                    }
                }
            }
            dest.createDirectories()
            dest.resolve(".extracted").writeText("ok count=$count\n")
            lastExtractNote = "extracted $count files → ${dest.pathString}"
            Cobblemon.LOGGER.info("ShowdownCombatBridge: {}", lastExtractNote)
        } catch (t: Throwable) {
            lastExtractNote = "extract failed: ${t.message}"
            Cobblemon.LOGGER.warn("ShowdownCombatBridge: extract failed: {}", t.toString())
        }
    }

    private fun injectDamageEngine(ctx: Context) {
        // Showdown-like damage: (((2*L/5+2)*Power*A/D)/50+2) * modifiers * random
        ctx.eval(
            "js",
            """
            globalThis.__cobbleDamage = function(p) {
              var level = p.level|0, power = p.power|0, atk = p.atk|0, def = p.def|0;
              if (power <= 0) return { damage: 0, immune: false, missed: false };
              if (p.typeMult <= 0) return { damage: 0, immune: true, missed: false };
              def = Math.max(1, def);
              atk = Math.max(1, atk);
              var base = Math.floor(Math.floor(Math.floor(2 * level / 5 + 2) * power * atk / def) / 50) + 2;
              var mod = 1.0;
              mod *= (p.stab || 1);
              mod *= (p.typeMult || 1);
              mod *= (p.other || 1);
              if (p.crit) mod *= 1.5;
              var roll = (typeof p.roll === 'number') ? p.roll : (0.85 + Math.random() * 0.15);
              mod *= roll;
              var hits = Math.max(1, p.hits|0);
              var dmg = 0;
              for (var i = 0; i < hits; i++) {
                var hRoll = (i === 0) ? roll : (0.85 + Math.random() * 0.15);
                var m = (p.stab||1) * (p.typeMult||1) * (p.other||1) * (p.crit?1.5:1) * hRoll;
                dmg += Math.max(1, Math.floor(base * m));
              }
              return { damage: dmg, immune: false, missed: false, base: base, mod: mod };
            };
            globalThis.__cobbleTypeMult = function(atk, def1, def2) {
              // filled from host each call via numbers — placeholder; host computes typeMult
              return 1;
            };
            """.trimIndent(),
        )
        typeChartInjected.set(true)
    }

    /**
     * Compute damaging-move HP loss via JS. Returns null if bridge not ready / status move / error.
     * Caller still applies accuracy, stages, status, drain natively.
     */
    @JvmStatic
    fun tryDamage(
        move: BattleMove,
        attacker: OwnedMon,
        defender: OwnedMon,
        atkStages: BattleStages?,
        defStages: BattleStages?,
        typeMult: Float,
        stab: Float,
        otherMul: Float,
        crit: Boolean,
        hits: Int,
        randomRoll: Float,
    ): Int? {
        if (!isReady() || move.isStatus() || move.power <= 0) return null
        if (typeMult <= 0f) return 0
        val ctx = context ?: return null
        return try {
            val special = move.category == MoveCategory.SPECIAL
            var atk = if (special) attacker.stats().spAtk() else attacker.stats().atk()
            var def = if (special) defender.stats().spDef() else defender.stats().def()
            val aSt = atkStages ?: BattleStages()
            val dSt = defStages ?: BattleStages()
            atk = maxOf(1, Math.round(atk * aSt.multiplier(if (special) StatKind.SPA else StatKind.ATK)))
            def = maxOf(1, Math.round(def * dSt.multiplier(if (special) StatKind.SPD else StatKind.DEF)))
            if (!special) {
                atk = maxOf(1, Math.round(atk * attacker.status().physicalAtkMul()))
            }

            val power = BattleEngineResolve.power(move)
            val js = buildString {
                append("globalThis.__cobbleDamage({")
                append("level:").append(attacker.level()).append(',')
                append("power:").append(power).append(',')
                append("atk:").append(atk).append(',')
                append("def:").append(def).append(',')
                append("stab:").append(stab).append(',')
                append("typeMult:").append(typeMult).append(',')
                append("other:").append(otherMul).append(',')
                append("crit:").append(crit).append(',')
                append("roll:").append(randomRoll.coerceIn(0.85f, 1.0f)).append(',')
                append("hits:").append(hits.coerceAtLeast(1))
                append("})")
            }
            val result: Value = ctx.eval("js", js)
            if (result.getMember("immune")?.asBoolean() == true) return 0
            result.getMember("damage").asInt()
        } catch (t: Throwable) {
            Cobblemon.LOGGER.debug("ShowdownCombatBridge.tryDamage failed: {}", t.toString())
            null
        }
    }

    @JvmStatic
    fun shutdown() {
        closeQuiet()
        ready = false
        lastMessage = "shutdown"
    }

    private fun closeQuiet() {
        try {
            context?.close()
        } catch (_: Throwable) {
        }
        context = null
    }
}

/** Tiny helper so combat bridge can use BattleEngine without circular init issues. */
internal object BattleEngineResolve {
    fun power(move: BattleMove): Int {
        val fromDex = ShowdownMoveDex.get(move.id)?.basePower
        if (fromDex != null && fromDex > 0) return fromDex
        return maxOf(1, move.power)
    }
}

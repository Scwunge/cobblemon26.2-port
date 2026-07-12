package com.cobblemon.mod.battle.graal

import com.cobblemon.mod.Cobblemon
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.HostAccess
import org.graalvm.polyglot.PolyglotAccess
import org.graalvm.polyglot.Value
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipInputStream
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.io.path.pathString
import kotlin.io.path.readText
import kotlin.io.path.writeText

/**
 * Full Showdown CommonJS environment (official Cobblemon-style).
 *
 * - Extracts [showdown.zip] to [showdownDir] (default `config/cobblemon/showdown`)
 * - Opens a Graal JS context with `js.commonjs-require`
 * - Evals root `index.js` (defines `startBattle`, `sendBattleMessage`, …)
 *
 * If boot fails (missing browserify shims, etc.), [isReady] stays false and combat
 * keeps using [ShowdownCombatBridge] / native.
 */
object ShowdownSim {
    @Volatile
    private var context: Context? = null

    @Volatile
    private var ready: Boolean = false

    @Volatile
    private var lastMessage: String = "not bootstrapped"

    @Volatile
    private var showdownDir: Path? = null

    @JvmStatic
    fun isReady(): Boolean = ready && context != null

    @JvmStatic
    fun lastMessage(): String = lastMessage

    @JvmStatic
    fun showdownPath(): String = showdownDir?.pathString ?: ""

    /**
     * Bootstrap after zip is on classpath. Prefer after [ShowdownCombatBridge.bootstrap]
     * so extract may already have run. Never throws.
     */
    @JvmStatic
    fun bootstrap() {
        ready = false
        lastMessage = "starting"
        closeQuiet()
        try {
            if (!GraalSpike.lastSuccess && !ShowdownRunner.isAvailable()) {
                lastMessage = "Graal not available"
                Cobblemon.LOGGER.warn("ShowdownSim: skip — {}", lastMessage)
                return
            }

            val dir = ensureExtracted()
            showdownDir = dir
            val index = dir.resolve("index.js")
            if (!index.isRegularFile()) {
                lastMessage = "index.js missing under ${dir.pathString}"
                Cobblemon.LOGGER.error("ShowdownSim: {}", lastMessage)
                return
            }

            try {
                System.setProperty("polyglot.engine.WarnInterpreterOnly", "false")
            } catch (_: Throwable) {
            }

            val cwd = dir.toAbsolutePath().normalize()
            val hostAccess = HostAccess.newBuilder(HostAccess.EXPLICIT)
                .allowIterableAccess(true)
                .allowArrayAccess(true)
                .allowListAccess(true)
                .allowMapAccess(true)
                .build()

            val builder = Context.newBuilder("js")
                .allowIO(true)
                .allowExperimentalOptions(true)
                .allowPolyglotAccess(PolyglotAccess.ALL)
                .allowHostAccess(hostAccess)
                .allowCreateThread(true)
                .option("engine.WarnInterpreterOnly", "false")
                .option("js.commonjs-require", "true")
                .option("js.commonjs-require-cwd", cwd.pathString)
                // Official Cobblemon polyfill map; may no-op if packages missing in extract
                .option(
                    "js.commonjs-core-modules-replacements",
                    "buffer:buffer/,crypto:crypto-browserify,path:path-browserify",
                )

            val ctx = try {
                builder.build()
            } catch (t: Throwable) {
                // Older polyglot may not support some options — retry minimal commonjs
                Cobblemon.LOGGER.warn("ShowdownSim: full Context options failed ({}), retrying minimal", t.message)
                Context.newBuilder("js")
                    .allowIO(true)
                    .allowExperimentalOptions(true)
                    .allowHostAccess(hostAccess)
                    .option("engine.WarnInterpreterOnly", "false")
                    .option("js.commonjs-require", "true")
                    .option("js.commonjs-require-cwd", cwd.pathString)
                    .build()
            }
            context = ctx

            // Official stub so path code that expects process.cwd works
            ctx.eval(
                "js",
                """
                globalThis.process = globalThis.process || {};
                globalThis.process.cwd = function() { return ${jsString(cwd.pathString)}; };
                globalThis.process.env = globalThis.process.env || {};
                """.trimIndent(),
            )

            val indexSrc = index.readText(StandardCharsets.UTF_8)
            ctx.eval("js", indexSrc)

            // Verify global API from Cobblemon's index.js
            val bindings = ctx.getBindings("js")
            val hasStart = bindings.getMember("startBattle")?.canExecute() == true
            val hasSend = bindings.getMember("sendBattleMessage")?.canExecute() == true
            if (!hasStart) {
                // Try alternate: require sim and expose helpers ourselves
                tryDexFallback(ctx, cwd)
            } else {
                // Smoke: getTypeChart if present
                val chart = try {
                    val fn = bindings.getMember("getTypeChart")
                    if (fn != null && fn.canExecute()) {
                        val s = fn.execute().asString()
                        "typeChartChars=${s.length}"
                    } else {
                        "no getTypeChart"
                    }
                } catch (t: Throwable) {
                    "typeChart fail: ${t.message}"
                }
                ready = true
                lastMessage = "ok; startBattle=$hasStart sendBattleMessage=$hasSend; $chart; dir=${cwd.pathString}"
            }

            if (ready) {
                installOutbox(ctx)
                Cobblemon.LOGGER.info("ShowdownSim: READY — {}", lastMessage)
            } else if (!lastMessage.startsWith("ok")) {
                // tryDexFallback may have set ready
                if (!ready) {
                    Cobblemon.LOGGER.warn("ShowdownSim: incomplete boot — {}", lastMessage)
                } else {
                    installOutbox(ctx)
                }
            }
        } catch (t: Throwable) {
            ready = false
            lastMessage = t.javaClass.simpleName + ": " + (t.message ?: "")
            Cobblemon.LOGGER.error(
                "ShowdownSim: FAILURE — {} — using combat bridge / native only",
                lastMessage,
                t,
            )
            closeQuiet()
        }
    }

    /**
     * JS → Java bridge without HostAccess callables: Showdown stubs push protocol
     * text into `globalThis.__cobbleOutbox`, and we [flushOutbox] after each start/send.
     */
    private fun installOutbox(ctx: Context) {
        try {
            ctx.eval(
                "js",
                """
                globalThis.__cobbleOutbox = globalThis.__cobbleOutbox || [];
                globalThis.__cobbleOnShowdownOutput = function(id, m) {
                  try {
                    globalThis.__cobbleOutbox.push({ id: String(id || ''), m: String(m || '') });
                    if (globalThis.__cobbleOutbox.length > 500) {
                      globalThis.__cobbleOutbox.splice(0, globalThis.__cobbleOutbox.length - 400);
                    }
                  } catch (e) {}
                };
                """.trimIndent(),
            )
        } catch (t: Throwable) {
            Cobblemon.LOGGER.debug("ShowdownSim.installOutbox failed: {}", t.toString())
        }
    }

    /** Drain JS outbox into [ShowdownTurnProtocol.onSimOutput]. Returns number of chunks. */
    @JvmStatic
    fun flushOutbox(): Int {
        if (!ready) return 0
        val ctx = context ?: return 0
        return try {
            val raw = ctx.eval(
                "js",
                """
                (function() {
                  var box = globalThis.__cobbleOutbox || [];
                  globalThis.__cobbleOutbox = [];
                  return JSON.stringify(box);
                })()
                """.trimIndent(),
            )
            val json = raw?.asString() ?: return 0
            if (json.isBlank() || json == "[]") return 0
            val arr = com.google.gson.JsonParser.parseString(json).asJsonArray
            var n = 0
            for (el in arr) {
                val o = el.asJsonObject
                val id = o.get("id")?.asString ?: ""
                val m = o.get("m")?.asString ?: ""
                if (id.isNotBlank() && m.isNotBlank()) {
                    ShowdownTurnProtocol.onSimOutput(id, m)
                    n++
                }
            }
            n
        } catch (t: Throwable) {
            Cobblemon.LOGGER.debug("ShowdownSim.flushOutbox failed: {}", t.toString())
            0
        }
    }

    /** True when full protocol battles (startBattle) are available — not just dex fallback. */
    @JvmStatic
    fun canRunProtocolBattles(): Boolean {
        if (!ready) return false
        val ctx = context ?: return false
        return try {
            ctx.getBindings("js").getMember("startBattle")?.canExecute() == true
        } catch (_: Throwable) {
            false
        }
    }

    /**
     * If full index.js fails partial, try `require('./sim/dex')` for move/type data only.
     */
    private fun tryDexFallback(ctx: Context, cwd: Path) {
        try {
            ctx.eval(
                "js",
                """
                (function() {
                  var Dex = require('./sim/dex').Dex;
                  globalThis.__showdownDex = Dex;
                  globalThis.__showdownGetMove = function(id) {
                    var m = Dex.moves.get(id);
                    if (!m || !m.exists) return null;
                    return JSON.stringify({
                      id: m.id,
                      name: m.name,
                      basePower: m.basePower|0,
                      type: m.type,
                      category: m.category,
                      accuracy: (m.accuracy === true) ? 101 : (m.accuracy|0),
                      priority: m.priority|0,
                      pp: m.pp|0
                    });
                  };
                  return Dex.moves.get('vinewhip').basePower|0;
                })()
                """.trimIndent(),
            ).let { v ->
                val pwr = v.asInt()
                ready = pwr > 0
                lastMessage = "dex-fallback; vinewhip=$pwr; dir=${cwd.pathString}"
                if (ready) {
                    Cobblemon.LOGGER.info("ShowdownSim: DEX FALLBACK READY — {}", lastMessage)
                }
            }
        } catch (t: Throwable) {
            lastMessage = "index + dex-fallback failed: ${t.message}"
            ready = false
        }
    }

    /**
     * Look up move JSON from live Dex if sim/dex loaded. Null if unavailable.
     */
    @JvmStatic
    fun getMoveJson(id: String): String? {
        if (!ready) return null
        val ctx = context ?: return null
        return try {
            val bindings = ctx.getBindings("js")
            val fn = bindings.getMember("__showdownGetMove")
            if (fn != null && fn.canExecute()) {
                val v = fn.execute(id)
                if (v.isNull) null else v.asString()
            } else {
                // Full index boot may not define helper — define on the fly
                ctx.eval(
                    "js",
                    """
                    (function(id) {
                      try {
                        var Dex = require('./sim/dex').Dex;
                        var m = Dex.moves.get(id);
                        if (!m || !m.exists) return null;
                        return JSON.stringify({
                          id: m.id, name: m.name, basePower: m.basePower|0,
                          type: m.type, category: m.category,
                          accuracy: (m.accuracy === true) ? 101 : (m.accuracy|0),
                          priority: m.priority|0, pp: m.pp|0
                        });
                      } catch (e) { return null; }
                    })(${jsString(id)})
                    """.trimIndent(),
                ).let { if (it.isNull) null else it.asString() }
            }
        } catch (t: Throwable) {
            Cobblemon.LOGGER.debug("ShowdownSim.getMoveJson failed: {}", t.toString())
            null
        }
    }

    /**
     * Start a protocol battle if full `startBattle` exists.
     * [messages] are Showdown protocol lines (e.g. `>start {...}`).
     * Returns false if sim not ready or call fails.
     */
    @JvmStatic
    fun startBattle(battleId: String, messages: Array<String>): Boolean {
        if (!ready) return false
        val ctx = context ?: return false
        return try {
            val fn = ctx.getBindings("js").getMember("startBattle")
            if (fn == null || !fn.canExecute()) return false
            // graalShowdown host object — pass a minimal stub with log/sendFromShowdown
            val stub = mapOf(
                "log" to { msg: Any? -> Cobblemon.LOGGER.debug("Showdown: {}", msg) },
                "sendFromShowdown" to { id: Any?, msg: Any? ->
                    ShowdownTurnProtocol.onSimOutput(id?.toString() ?: "", msg?.toString() ?: "")
                },
            )
            // HostAccess.EXPLICIT may block map execute — use pure JS wrapper instead
            ctx.getBindings("js").putMember("__cobbleBattleId", battleId)
            val arr = messages.joinToString(prefix = "[", postfix = "]") { jsString(it) }
            ctx.eval(
                "js",
                """
                (function() {
                  var msgs = $arr;
                  var stub = {
                    log: function(m) { /* no-op or print */ },
                    sendFromShowdown: function(id, m) {
                      if (typeof globalThis.__cobbleOnShowdownOutput === 'function') {
                        globalThis.__cobbleOnShowdownOutput(String(id), String(m));
                      } else {
                        globalThis.__cobbleOutbox = globalThis.__cobbleOutbox || [];
                        globalThis.__cobbleOutbox.push({ id: String(id || ''), m: String(m || '') });
                      }
                    }
                  };
                  startBattle(stub, __cobbleBattleId, msgs);
                  return true;
                })()
                """.trimIndent(),
            )
            flushOutbox()
            true
        } catch (t: Throwable) {
            Cobblemon.LOGGER.warn("ShowdownSim.startBattle failed: {}", t.toString())
            false
        }
    }

    @JvmStatic
    fun sendBattleMessage(battleId: String, messages: Array<String>): Boolean {
        if (!ready) return false
        val ctx = context ?: return false
        return try {
            val fn = ctx.getBindings("js").getMember("sendBattleMessage")
            if (fn == null || !fn.canExecute()) return false
            val arr = messages.joinToString(prefix = "[", postfix = "]") { jsString(it) }
            ctx.eval(
                "js",
                "sendBattleMessage(${jsString(battleId)}, $arr)",
            )
            flushOutbox()
            true
        } catch (t: Throwable) {
            Cobblemon.LOGGER.debug("ShowdownSim.sendBattleMessage failed: {}", t.toString())
            false
        }
    }

    @JvmStatic
    fun endBattle(battleId: String): Boolean {
        if (!ready) return false
        val ctx = context ?: return false
        return try {
            val fn = ctx.getBindings("js").getMember("endBattle")
            if (fn == null || !fn.canExecute()) return false
            fn.execute(battleId)
            true
        } catch (t: Throwable) {
            false
        }
    }

    @JvmStatic
    fun shutdown() {
        closeQuiet()
        ready = false
        lastMessage = "shutdown"
    }

    private fun ensureExtracted(): Path {
        val dest = Path.of("config", "cobblemon", "showdown")
        val marker = dest.resolve(".extracted")
        val index = dest.resolve("index.js")
        if (marker.exists() && index.isRegularFile()) {
            return dest.toAbsolutePath().normalize()
        }
        dest.createDirectories()
        val stream = ShowdownSim::class.java.getResourceAsStream("/data/cobblemon/showdown.zip")
            ?: throw IllegalStateException("showdown.zip missing on classpath")
        var count = 0
        stream.use { raw ->
            ZipInputStream(raw).use { zin ->
                var entry = zin.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val out = dest.resolve(entry.name)
                        out.parent?.createDirectories()
                        Files.newOutputStream(out).use { os -> zin.copyTo(os) }
                        count++
                    }
                    entry = zin.nextEntry
                }
            }
        }
        marker.writeText("count=$count\n")
        Cobblemon.LOGGER.info("ShowdownSim: extracted {} files to {}", count, dest.toAbsolutePath())
        return dest.toAbsolutePath().normalize()
    }

    private fun closeQuiet() {
        try {
            context?.close()
        } catch (_: Throwable) {
        }
        context = null
    }

    private fun jsString(s: String): String =
        "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"")
            .replace("\n", "\\n").replace("\r", "\\r") + "\""
}

package com.cobblemon.mod.battle.graal

import com.cobblemon.mod.Cobblemon
import com.cobblemon.mod.battle.ShowdownMoveDex
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.HostAccess
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.zip.ZipInputStream

/**
 * Showdown runner v0 — holds an optional Graal JS [Context], probes moves via JS,
 * and exposes a thin lookup API. **Not** a full battle sim; native [com.cobblemon.mod.battle.BattleManager]
 * remains the live engine.
 *
 * Safe: all failures are caught; never crashes mod bootstrap.
 */
object ShowdownRunner {
    @Volatile
    private var context: Context? = null

    @Volatile
    private var available: Boolean = false

    @Volatile
    private var lastMessage: String = "not bootstrapped"

    /** Probe sample moves injected into JS for getMovePower JS path. */
    private val probeMoveIds = listOf(
        "vinewhip", "ember", "tackle", "thunderbolt", "gigadrain", "swordsdance",
    )

    @JvmStatic
    fun isAvailable(): Boolean = available

    @JvmStatic
    fun lastMessage(): String = lastMessage

    /**
     * Bootstrap after [ShowdownMoveDex.bootstrap] and [GraalSpike.tryBootstrap].
     * Prefer reusing a successful spike signal; still tries own Context if needed.
     * Never throws.
     */
    @JvmStatic
    fun bootstrap() {
        available = false
        lastMessage = "starting"
        shutdownQuiet()
        try {
            if (!GraalSpike.lastSuccess) {
                // Spike failed — still attempt own context (may recover if spike was flaky).
                Cobblemon.LOGGER.info(
                    "ShowdownRunner: GraalSpike.lastSuccess=false ({}); trying own Context",
                    GraalSpike.lastMessage,
                )
            }

            try {
                System.setProperty("polyglot.engine.WarnInterpreterOnly", "false")
            } catch (_: Throwable) {
                // ignore
            }

            val ctx = Context.newBuilder("js")
                .option("engine.WarnInterpreterOnly", "false")
                .allowHostAccess(HostAccess.NONE)
                .allowAllAccess(false)
                .build()
            context = ctx

            val movesSnippet = readMovesJsSnippet(maxChars = 48_000)
            val basePowerCount = if (movesSnippet != null) {
                countBasePowerViaJs(ctx, movesSnippet)
            } else {
                -1
            }

            injectMoveRegistry(ctx)

            val vineJs = lookupPowerViaJs(ctx, "vinewhip")
            val vineNative = ShowdownMoveDex.get("vinewhip")?.basePower

            available = true
            lastMessage =
                "ok; basePowerCountInSnippet=$basePowerCount; js.vinewhip=$vineJs; native.vinewhip=$vineNative"
            Cobblemon.LOGGER.info("ShowdownRunner: bootstrap SUCCESS — {}", lastMessage)
        } catch (t: Throwable) {
            available = false
            lastMessage = t.javaClass.simpleName + ": " + (t.message ?: "(no message)")
            Cobblemon.LOGGER.error(
                "ShowdownRunner: bootstrap FAILURE — {} — falling back to native battle engine",
                lastMessage,
                t,
            )
            shutdownQuiet()
        }
    }

    /**
     * Move base power: prefer native [ShowdownMoveDex], then JS-injected registry.
     */
    @JvmStatic
    fun getMovePower(id: String): Int? {
        if (id.isBlank()) return null
        val key = ShowdownMoveDex.canonicalize(id)
        ShowdownMoveDex.get(key)?.basePower?.let { return it }
        if (!available) return null
        return try {
            val ctx = context ?: return null
            lookupPowerViaJs(ctx, key)
        } catch (t: Throwable) {
            Cobblemon.LOGGER.debug("ShowdownRunner.getMovePower JS path failed for {}: {}", id, t.toString())
            null
        }
    }

    @JvmStatic
    fun shutdown() {
        try {
            shutdownQuiet()
            available = false
            lastMessage = "shutdown"
            Cobblemon.LOGGER.info("ShowdownRunner: shutdown")
        } catch (t: Throwable) {
            Cobblemon.LOGGER.warn("ShowdownRunner: shutdown error: {}", t.toString())
        }
    }

    private fun shutdownQuiet() {
        try {
            context?.close()
        } catch (_: Throwable) {
            // ignore
        }
        context = null
    }

    /**
     * Extract `data/moves.js` prefix from showdown.zip (string eval path; no full sim load).
     */
    private fun readMovesJsSnippet(maxChars: Int): String? {
        val stream = ShowdownRunner::class.java.getResourceAsStream("/data/cobblemon/showdown.zip")
            ?: return null
        stream.use { raw ->
            ZipInputStream(raw).use { zin ->
                var entry = zin.nextEntry
                while (entry != null) {
                    if (entry.name == "data/moves.js") {
                        val bytes = zin.readNBytes(maxChars * 2) // UTF-8 safety margin
                        val text = String(bytes, StandardCharsets.UTF_8)
                        return if (text.length > maxChars) text.substring(0, maxChars) else text
                    }
                    entry = zin.nextEntry
                }
            }
        }
        return null
    }

    /**
     * Count `"basePower"` occurrences in a moves.js snippet via JS (proves string → eval path).
     */
    private fun countBasePowerViaJs(ctx: Context, snippet: String): Int {
        // Host string via bindings (HostAccess.NONE still allows putMember of primitives/strings).
        val bindings = ctx.getBindings("js")
        bindings.putMember("__movesSnippet", snippet)
        val result = ctx.eval(
            "js",
            """
            (function() {
              var s = __movesSnippet;
              var n = 0, i = 0;
              while (true) {
                var j = s.indexOf('basePower', i);
                if (j < 0) break;
                n++;
                i = j + 9;
              }
              return n;
            })()
            """.trimIndent(),
        )
        bindings.removeMember("__movesSnippet")
        return result.asInt()
    }

    /**
     * Inject a tiny JSON map of a few moves from [ShowdownMoveDex] into JS as `globalThis.__cobbleMoveDex`.
     */
    private fun injectMoveRegistry(ctx: Context) {
        val pairs = ArrayList<String>(probeMoveIds.size)
        for (id in probeMoveIds) {
            val entry = ShowdownMoveDex.get(id) ?: continue
            val safeId = entry.id.lowercase(Locale.ROOT)
            pairs.add("\"$safeId\":${entry.basePower}")
        }
        // Always include a hard-coded fallback sample so JS path works even if dex empty
        if (pairs.isEmpty()) {
            pairs.add("\"vinewhip\":45")
            pairs.add("\"ember\":40")
            pairs.add("\"tackle\":40")
        }
        val json = pairs.joinToString(prefix = "{", postfix = "}")
        ctx.eval(
            "js",
            """
            globalThis.__cobbleMoveDex = $json;
            globalThis.__cobbleGetPower = function(id) {
              if (!id) return null;
              var key = String(id).toLowerCase().replace(/[-_]/g, '');
              var p = globalThis.__cobbleMoveDex[key];
              return (typeof p === 'number') ? p : null;
            };
            """.trimIndent(),
        )
    }

    private fun lookupPowerViaJs(ctx: Context, id: String): Int? {
        val key = ShowdownMoveDex.canonicalize(id)
        // Pass id as JSON string literal
        val literal = "\"" + key.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
        val value = ctx.eval("js", "globalThis.__cobbleGetPower($literal)")
        if (value.isNull) return null
        return try {
            value.asInt()
        } catch (_: Throwable) {
            null
        }
    }
}

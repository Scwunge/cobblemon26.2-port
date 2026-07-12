package com.cobblemon.mod.battle.graal

import com.cobblemon.mod.Cobblemon
import java.util.zip.ZipInputStream

/**
 * Minimal GraalVM polyglot/JS spike for Java 25.
 *
 * Verifies we can create a JS [org.graalvm.polyglot.Context], eval trivial code,
 * and optionally touch showdown.zip — **not** a full Showdown battle runner.
 *
 * Safe: all failures are caught; never crashes mod bootstrap.
 * Disable deps via `graalSpikeEnabled = false` in build.gradle if resolution breaks.
 */
object GraalSpike {
    /** Last bootstrap outcome for diagnostics. */
    @Volatile
    @JvmStatic
    var lastSuccess: Boolean = false
        private set

    @Volatile
    @JvmStatic
    var lastMessage: String = "not run"
        private set

    /**
     * Attempt polyglot JS bootstrap. Call after [com.cobblemon.mod.battle.ShowdownMoveDex.bootstrap].
     * Never throws.
     */
    @JvmStatic
    fun tryBootstrap() {
        lastSuccess = false
        lastMessage = "starting"
        try {
            // Quiet interpreter-only warning on OpenJDK (no Graal compiler on classpath as upgrade module).
            try {
                System.setProperty("polyglot.engine.WarnInterpreterOnly", "false")
            } catch (_: Throwable) {
                // ignore security manager / read-only props
            }

            val contextClass = Class.forName("org.graalvm.polyglot.Context")
            Cobblemon.LOGGER.info("GraalSpike: polyglot classes present ({})", contextClass.name)

            // Reflective path avoids hard-link failures if deps are disabled at compile-time later;
            // direct API is fine while graalSpikeEnabled=true and deps on compile classpath.
            val result = runDirectEval()
            lastSuccess = true
            lastMessage = result
            Cobblemon.LOGGER.info("GraalSpike: SUCCESS — {}", result)
        } catch (t: Throwable) {
            lastSuccess = false
            lastMessage = t.javaClass.simpleName + ": " + (t.message ?: "(no message)")
            Cobblemon.LOGGER.error(
                "GraalSpike: FAILURE — {} — full Showdown via Graal not available this session",
                lastMessage,
                t,
            )
        }
    }

    /**
     * Direct polyglot API (compiled against org.graalvm.polyglot when deps enabled).
     */
    private fun runDirectEval(): String {
        // Direct polyglot API — compiled only when graalSpikeEnabled deps are on the classpath.
        org.graalvm.polyglot.Context.newBuilder("js")
            .option("engine.WarnInterpreterOnly", "false")
            .allowAllAccess(false)
            .build()
            .use { ctx ->
                val value = ctx.eval("js", "1+1")
                val sum = value.asInt()
                if (sum != 2) {
                    throw IllegalStateException("expected 1+1=2, got $sum")
                }

                val languages = ctx.engine.languages.keys.joinToString(",")
                val showdownHint = probeShowdownZip(ctx)

                return "js ok: 1+1=$sum; languages=[$languages]; $showdownHint"
            }
    }

    /**
     * Optional light touch of showdown.zip: eval a tiny pure-JS string (not the full sim).
     * Confirms zip is readable and JS can process a string resembling Showdown data shape.
     */
    private fun probeShowdownZip(ctx: org.graalvm.polyglot.Context): String {
        return try {
            val stream = GraalSpike::class.java.getResourceAsStream("/data/cobblemon/showdown.zip")
            if (stream == null) {
                return "showdown.zip: missing on classpath"
            }
            var movesJsBytes = -1L
            stream.use { raw ->
                ZipInputStream(raw).use { zin ->
                    var entry = zin.nextEntry
                    while (entry != null) {
                        if (entry.name == "data/moves.js") {
                            // Don't load entire file into Graal — just measure presence/size for spike.
                            var n = 0L
                            val buf = ByteArray(8192)
                            while (true) {
                                val r = zin.read(buf)
                                if (r < 0) break
                                n += r
                            }
                            movesJsBytes = n
                            break
                        }
                        entry = zin.nextEntry
                    }
                }
            }
            if (movesJsBytes < 0) {
                return "showdown.zip: present but data/moves.js not found"
            }
            // Tiny JS using the byte length as input — proves eval + host numbers, not full Showdown.
            val js = "({ ok: true, movesJsBytes: $movesJsBytes, sample: 'vinewhip'.length })"
            val obj = ctx.eval("js", js)
            val ok = obj.getMember("ok").asBoolean()
            val bytes = obj.getMember("movesJsBytes").asLong()
            val sampleLen = obj.getMember("sample").asInt()
            "showdown.zip: moves.js=${bytes}B jsProbe(ok=$ok,vinewhipLen=$sampleLen)"
        } catch (t: Throwable) {
            "showdown probe failed: ${t.javaClass.simpleName}: ${t.message}"
        }
    }
}

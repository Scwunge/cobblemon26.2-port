package com.cobblemon.mod.molang

import com.cobblemon.mod.Cobblemon
import java.io.File
import java.net.URI
import java.nio.charset.StandardCharsets
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.util.Collections
import java.util.concurrent.atomic.AtomicReference
import java.util.jar.JarFile

/**
 * Scans classpath resources under `/data/cobblemon/molang/` (nested), counts `.molang`
 * files, loads a few known AI/task scripts, and probes the tiny [MoLangEngine].
 *
 * Never throws — safe for [com.cobblemon.mod.Cobblemon] common setup.
 */
object MoLangBootstrap {
    private const val ROOT = "data/cobblemon/molang"
    private const val ROOT_ABS = "/$ROOT"

    private val knownScripts = listOf(
        "home_walk_task.molang",
        "wander_around_hive.molang",
    )

    private val scriptsRef = AtomicReference<List<MoLangScript>>(emptyList())

    @Volatile
    private var fileCount: Int = 0

    @Volatile
    private var lastMessage: String = "not bootstrapped"

    @JvmStatic
    fun fileCount(): Int = fileCount

    @JvmStatic
    fun lastMessage(): String = lastMessage

    @JvmStatic
    fun scripts(): List<MoLangScript> = scriptsRef.get()

    /**
     * Bootstrap after Showdown/structure setup. Safe if no files present.
     */
    @JvmStatic
    fun bootstrap() {
        try {
            val paths = discoverMolangPaths()
            fileCount = paths.size

            val loaded = ArrayList<MoLangScript>(knownScripts.size)
            for (name in knownScripts) {
                val rel = "$ROOT/$name"
                val source = readResource("/$rel") ?: readResource(rel) ?: continue
                val probeCtx = mapOf(
                    "q.grounded" to 1.0,
                    "q.entity.world.game_time" to 40.0,
                    "variable.x" to 2.0,
                )
                val evalResult = MoLangEngine.tryEvalScript(source, probeCtx)
                loaded.add(MoLangScript(path = rel, source = source, lastEval = evalResult))
            }
            scriptsRef.set(loaded)

            // Smoke-test the engine with pure expressions (always runs).
            val smoke = MoLangEngine.eval(
                "math.random(1, 1) + (2 * 3) - 1",
                mapOf("q.grounded" to 1.0, "variable.x" to 4.0),
            )
            val cmp = MoLangEngine.eval(
                "q.grounded == 1 && variable.x > 3",
                mapOf("q.grounded" to 1.0, "variable.x" to 4.0),
            )

            lastMessage =
                "files=$fileCount loadedKnown=${loaded.size} smoke=$smoke cmp=$cmp"

            val evalHints = loaded.joinToString(", ") { s ->
                val short = s.path.substringAfterLast('/')
                "$short=${s.lastEval ?: "stored"}"
            }

            Cobblemon.LOGGER.info(
                "MoLangBootstrap: {} · known: [{}]",
                lastMessage,
                evalHints.ifEmpty { "none found" },
            )
        } catch (t: Throwable) {
            fileCount = 0
            scriptsRef.set(emptyList())
            lastMessage = t.javaClass.simpleName + ": " + (t.message ?: "(no message)")
            Cobblemon.LOGGER.warn("MoLangBootstrap: failed safely — {}", lastMessage)
        }
    }

    // ── resource discovery ───────────────────────────────────────────────

    private fun discoverMolangPaths(): List<String> {
        val found = LinkedHashSet<String>()
        try {
            val cl = MoLangBootstrap::class.java.classLoader
                ?: ClassLoader.getSystemClassLoader()

            val roots = cl.getResources(ROOT)
            while (roots.hasMoreElements()) {
                val url = roots.nextElement()
                try {
                    when (url.protocol) {
                        "file" -> collectFromFileUrl(url.toURI(), found)
                        "jar" -> collectFromJarUrl(url, found)
                        else -> {
                            // Best-effort: nio FileSystem for jar-like schemes
                            collectFromGenericUri(url.toURI(), found)
                        }
                    }
                } catch (_: Throwable) {
                    // ignore per-root failures
                }
            }

            // Class.getResource on the directory (dev / some loaders)
            try {
                val dirUrl = MoLangBootstrap::class.java.getResource(ROOT_ABS)
                if (dirUrl != null) {
                    when (dirUrl.protocol) {
                        "file" -> collectFromFileUrl(dirUrl.toURI(), found)
                        "jar" -> collectFromJarUrl(dirUrl, found)
                    }
                }
            } catch (_: Throwable) {
                // ignore
            }

            // Fallback: known nested names if directory listing unavailable
            if (found.isEmpty()) {
                val candidates = listOf(
                    "home_walk_task.molang",
                    "wander_around_hive.molang",
                    "client/swing_arm.molang",
                    "npc/basic-npc-interaction.molang",
                    "npc/configure_npc_party.molang",
                    "npc/instant_battle_interaction.molang",
                    "npc/run_callback_dialogue.molang",
                    "pokemon/get_mad_at_thrower.molang",
                    "pokemon/configurers/apply_bee_properties.molang",
                    "pokemon/configurers/apply_fox_properties.molang",
                    "pokemon/configurers/apply_species_properties.molang",
                )
                for (c in candidates) {
                    val stream = MoLangBootstrap::class.java.getResourceAsStream("$ROOT_ABS/$c")
                        ?: cl.getResourceAsStream("$ROOT/$c")
                    if (stream != null) {
                        stream.close()
                        found.add("$ROOT/$c")
                    }
                }
            }
        } catch (_: Throwable) {
            // return whatever we have
        }
        return found.toList().sorted()
    }

    private fun collectFromFileUrl(uri: URI, out: MutableSet<String>) {
        val root = Path.of(uri)
        if (!Files.exists(root)) return
        if (Files.isRegularFile(root) && root.toString().endsWith(".molang", ignoreCase = true)) {
            out.add(normalizeResourcePath(root.fileName.toString()))
            return
        }
        if (!Files.isDirectory(root)) return
        Files.walk(root).use { stream ->
            stream.filter { Files.isRegularFile(it) && it.toString().endsWith(".molang", true) }
                .forEach { p ->
                    val rel = root.relativize(p).toString().replace(File.separatorChar, '/')
                    out.add("$ROOT/$rel")
                }
        }
    }

    private fun collectFromJarUrl(url: java.net.URL, out: MutableSet<String>) {
        // jar:file:/path/to.jar!/data/cobblemon/molang
        val external = url.toExternalForm()
        val bang = external.indexOf("!/")
        if (bang < 0) return
        val prefix = "$ROOT/"
        // jar:file:/...!/… → file:/...
        val fileUri = external.substring(4, bang)
        try {
            JarFile(File(URI(fileUri))).use { jar ->
                val entries = jar.entries()
                while (entries.hasMoreElements()) {
                    val e = entries.nextElement()
                    if (e.isDirectory) continue
                    val name = e.name.trimStart('/')
                    if (name.startsWith(prefix) && name.endsWith(".molang", ignoreCase = true)) {
                        out.add(name)
                    }
                }
            }
            return
        } catch (_: Throwable) {
            // fall through to NIO FS
        }
        try {
            val jarUri = URI(external.substring(0, bang + 2))
            val fs = try {
                FileSystems.getFileSystem(jarUri)
            } catch (_: Throwable) {
                FileSystems.newFileSystem(jarUri, Collections.emptyMap<String, Any>())
            }
            val root = fs.getPath(ROOT)
            if (Files.exists(root)) {
                Files.walk(root).use { stream ->
                    stream.filter { Files.isRegularFile(it) && it.toString().endsWith(".molang", true) }
                        .forEach { p ->
                            val s = p.toString().replace('\\', '/').trimStart('/')
                            out.add(s)
                        }
                }
            }
        } catch (_: Throwable) {
            // ignore
        }
    }

    private fun collectFromGenericUri(uri: URI, out: MutableSet<String>) {
        try {
            val path = Path.of(uri)
            collectFromFileUrl(path.toUri(), out)
        } catch (_: Throwable) {
            // ignore
        }
    }

    private fun normalizeResourcePath(name: String): String =
        if (name.startsWith(ROOT)) name else "$ROOT/$name"

    private fun readResource(path: String): String? {
        return try {
            val stream = MoLangBootstrap::class.java.getResourceAsStream(
                if (path.startsWith("/")) path else "/$path",
            ) ?: MoLangBootstrap::class.java.classLoader?.getResourceAsStream(
                path.removePrefix("/"),
            )
            stream?.use { String(it.readAllBytes(), StandardCharsets.UTF_8) }
        } catch (_: Throwable) {
            null
        }
    }
}

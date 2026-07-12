package com.cobblemon.mod.battle

import com.cobblemon.mod.Cobblemon
import com.cobblemon.mod.species.MonElement
import com.cobblemon.mod.species.MonStatus
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.zip.ZipInputStream

/**
 * Loads official Showdown move stats from `data/cobblemon/showdown.zip` (`data/moves.js`).
 * Parses power/type plus secondary / drain / boosts for the native battle engine.
 */
object ShowdownMoveDex {
    data class StatBoost(val kind: StatKind, val stages: Int)

    data class Secondary(
        val chance: Int,
        val status: MonStatus?,
        val boosts: List<StatBoost>,
    )

    data class Entry(
        val id: String,
        val name: String,
        val type: MonElement,
        val category: MoveCategory,
        val basePower: Int,
        /** 0–100; 101 means "always hits" (Showdown accuracy: true). */
        val accuracy: Int,
        val priority: Int,
        val pp: Int,
        /** User boosts from status setup moves (`boosts: { atk: 2 }`). */
        val selfBoosts: List<StatBoost> = emptyList(),
        /** Drain heal fraction of damage dealt (e.g. 0.5 for Giga Drain). */
        val drainFraction: Float = 0f,
        val secondary: Secondary? = null,
    ) {
        fun isStatus(): Boolean = category == MoveCategory.STATUS || basePower <= 0
    }

    @Volatile
    private var byId: Map<String, Entry> = emptyMap()

    @JvmStatic
    fun size(): Int = byId.size

    @JvmStatic
    fun isLoaded(): Boolean = byId.isNotEmpty()

    @JvmStatic
    fun get(id: String?): Entry? {
        if (id.isNullOrBlank()) return null
        return byId[canonicalize(id)]
    }

    @JvmStatic
    fun bootstrap() {
        if (byId.isNotEmpty()) return
        val loaded = LinkedHashMap<String, Entry>(4096)
        try {
            val stream = ShowdownMoveDex::class.java.getResourceAsStream("/data/cobblemon/showdown.zip")
            if (stream == null) {
                Cobblemon.LOGGER.warn("ShowdownMoveDex: showdown.zip not on classpath")
                return
            }
            stream.use { zinRaw ->
                ZipInputStream(zinRaw).use { zin ->
                    var entry = zin.nextEntry
                    while (entry != null) {
                        if (entry.name == "data/moves.js") {
                            parseMovesJs(BufferedReader(InputStreamReader(zin, StandardCharsets.UTF_8)), loaded)
                            break
                        }
                        entry = zin.nextEntry
                    }
                }
            }
            byId = loaded
            val withSec = loaded.values.count { it.secondary != null }
            val withDrain = loaded.values.count { it.drainFraction > 0f }
            val withBoost = loaded.values.count { it.selfBoosts.isNotEmpty() }
            Cobblemon.LOGGER.info(
                "ShowdownMoveDex: loaded {} moves (secondary={}, drain={}, selfBoosts={})",
                loaded.size, withSec, withDrain, withBoost,
            )
        } catch (t: Throwable) {
            Cobblemon.LOGGER.error("ShowdownMoveDex: failed to load moves", t)
            byId = emptyMap()
        }
    }

    @JvmStatic
    fun canonicalize(id: String): String =
        id.lowercase(Locale.ROOT).replace("-", "").replace("_", "").trim()

    private fun parseMovesJs(reader: BufferedReader, out: MutableMap<String, Entry>) {
        val text = reader.readText()
        val keyRe = Regex("""(?m)^  "?([a-zA-Z0-9]+)"?\s*:\s*\{""")
        val matches = keyRe.findAll(text).toList()
        for (i in matches.indices) {
            val m = matches[i]
            val id = m.groupValues[1].lowercase(Locale.ROOT)
            if (id == "constructor" || id == "prototype") continue
            val start = m.range.last
            val end = if (i + 1 < matches.size) matches[i + 1].range.first else text.length
            val block = text.substring(start, end)
            val entry = parseBlock(id, block) ?: continue
            out[id] = entry
        }
    }

    private fun parseBlock(id: String, block: String): Entry? {
        val name = stringField(block, "name") ?: id.replaceFirstChar { it.uppercase() }
        val typeStr = stringField(block, "type") ?: "Normal"
        val catStr = stringField(block, "category") ?: "Physical"
        val basePower = intField(block, "basePower") ?: 0
        val pp = intField(block, "pp") ?: 20
        val priority = intField(block, "priority") ?: 0
        val accuracy = accuracyField(block)

        val category = when (catStr.lowercase(Locale.ROOT)) {
            "special" -> MoveCategory.SPECIAL
            "status" -> MoveCategory.STATUS
            else -> MoveCategory.PHYSICAL
        }

        // Top-level boosts: status debuffs → foe (Growl); status buffs → self (Swords Dance)
        val topBoosts = parseBoostsBlock(block, "boosts")
        val statusDebuff = category == MoveCategory.STATUS && topBoosts.isNotEmpty() && topBoosts.all { it.stages < 0 }
        val selfBoosts = when {
            category == MoveCategory.STATUS && topBoosts.isNotEmpty() && !statusDebuff -> topBoosts
            else -> emptyList()
        }
        val drain = parseDrain(block)
        var secondary = parseSecondary(block)
        if (secondary == null && statusDebuff) {
            secondary = Secondary(100, null, topBoosts)
        }
        // Top-level status (Thunder Wave, Sleep Powder) → 100% secondary status
        val topStatus = parseStatus(block)
        if (topStatus != null && (secondary == null || secondary.status == null)) {
            val boosts = secondary?.boosts ?: emptyList()
            val chance = secondary?.chance ?: 100
            secondary = Secondary(chance, topStatus, boosts)
        }

        return Entry(
            id = id,
            name = name,
            type = monElement(typeStr),
            category = category,
            basePower = basePower.coerceAtLeast(0),
            accuracy = accuracy,
            priority = priority,
            pp = pp.coerceIn(1, 64),
            selfBoosts = selfBoosts,
            drainFraction = drain,
            secondary = secondary,
        )
    }

    private fun parseDrain(block: String): Float {
        // drain: [1, 2] => 1/2 of damage healed
        val m = Regex("""\bdrain\s*:\s*\[\s*(\d+)\s*,\s*(\d+)\s*\]""").find(block) ?: return 0f
        val a = m.groupValues[1].toIntOrNull() ?: return 0f
        val b = m.groupValues[2].toIntOrNull() ?: return 0f
        if (b <= 0) return 0f
        return (a.toFloat() / b.toFloat()).coerceIn(0f, 1f)
    }

    private fun parseSecondary(block: String): Secondary? {
        // secondary: { chance: 10, status: 'brn', boosts: { spd: -1 } }
        val secIdx = block.indexOf("secondary:")
        if (secIdx < 0) {
            // also secondary: null
            return null
        }
        val after = block.substring(secIdx)
        if (Regex("""secondary\s*:\s*null""").containsMatchIn(after.take(40))) {
            return null
        }
        val braceStart = after.indexOf('{')
        if (braceStart < 0) return null
        val secBlock = extractBalanced(after, braceStart) ?: return null

        val chance = intField(secBlock, "chance") ?: 100
        val status = parseStatus(secBlock)
        val boosts = parseBoostsBlock(secBlock, "boosts")
        if (status == null && boosts.isEmpty()) {
            // volatile status etc. — skip for now
            return null
        }
        return Secondary(chance.coerceIn(1, 100), status, boosts)
    }

    private fun parseStatus(block: String): MonStatus? {
        val m = Regex("""\bstatus\s*:\s*['"]([a-zA-Z]+)['"]""").find(block) ?: return null
        return when (m.groupValues[1].lowercase(Locale.ROOT)) {
            "brn" -> MonStatus.BURN
            "psn", "tox" -> MonStatus.POISON
            "par" -> MonStatus.PARALYSIS
            "slp" -> MonStatus.SLEEP
            "frz" -> MonStatus.FREEZE
            else -> null
        }
    }

    private fun parseBoostsBlock(parent: String, field: String): List<StatBoost> {
        val re = Regex("""\b$field\s*:\s*\{""")
        val m = re.find(parent) ?: return emptyList()
        val start = m.range.last // at '{'
        val block = extractBalanced(parent, start) ?: return emptyList()
        val out = ArrayList<StatBoost>()
        val pairRe = Regex("""\b(atk|def|spa|spd|spe|accuracy|evasion)\s*:\s*(-?\d+)""", RegexOption.IGNORE_CASE)
        for (pm in pairRe.findAll(block)) {
            val kind = StatKind.fromShowdown(pm.groupValues[1]) ?: continue
            val stages = pm.groupValues[2].toIntOrNull() ?: continue
            if (stages != 0) {
                out.add(StatBoost(kind, stages.coerceIn(-6, 6)))
            }
        }
        return out
    }

    private fun extractBalanced(text: String, openBraceIndex: Int): String? {
        if (openBraceIndex < 0 || openBraceIndex >= text.length || text[openBraceIndex] != '{') return null
        var depth = 0
        for (i in openBraceIndex until text.length) {
            when (text[i]) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return text.substring(openBraceIndex, i + 1)
                }
            }
        }
        return null
    }

    private fun stringField(block: String, field: String): String? {
        val re = Regex("""\b$field\s*:\s*"([^"]+)"""")
        return re.find(block)?.groupValues?.get(1)
    }

    private fun intField(block: String, field: String): Int? {
        val re = Regex("""\b$field\s*:\s*(-?\d+)""")
        return re.find(block)?.groupValues?.get(1)?.toIntOrNull()
    }

    private fun accuracyField(block: String): Int {
        if (Regex("""\baccuracy\s*:\s*true\b""").containsMatchIn(block)) {
            return 101
        }
        return intField(block, "accuracy")?.coerceIn(1, 100) ?: 100
    }

    private fun monElement(type: String): MonElement {
        return try {
            MonElement.valueOf(type.uppercase(Locale.ROOT))
        } catch (_: Exception) {
            when (type.lowercase(Locale.ROOT)) {
                "fairy" -> MonElement.FAIRY
                "dark" -> MonElement.DARK
                "steel" -> MonElement.STEEL
                "???", "typeless", "normal" -> MonElement.NORMAL
                else -> MonElement.NORMAL
            }
        }
    }
}

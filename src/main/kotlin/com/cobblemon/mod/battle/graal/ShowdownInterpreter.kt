package com.cobblemon.mod.battle.graal

/**
 * Parses common Pokémon Showdown battle protocol lines into typed events.
 *
 * Supported prefixes (v0 scaffold):
 * - `|move|` — actor used a move (optional target)
 * - `|-damage|` — HP change from damage
 * - `|-heal|` — HP change from healing
 * - `|-status|` — status inflicted
 * - `|faint|` — Pokémon fainted
 * - `|switch|` — active Pokémon switched in
 * - `|win|` — battle winner
 *
 * ## Full engine still needs
 * - **Packed teams**: full PS pack format (species|item|ability|moves|…|level|…) and
 *   `>player pN {"name":…,"team":…}` so the sim can actually start with real sets.
 * - **Choice request / request JSON**: parse `|request|` / sideupdate choice payloads
 *   (active moves, forceSwitch, wait) and map UI actions back to `>pN move N` /
 *   `>pN switch N` messages via [ShowdownSim.sendBattleMessage].
 * - Residual / weather / ability / item lines (`|-weather|`, `|-ability|`, `|-enditem|`, …)
 * - Double-battle slot mapping (`p1a`/`p1b`) into [com.cobblemon.mod.battle.BattleSession]
 * - Authoritative HP/status sync from these events into the native session (or full
 *   sim-authoritative mode with native UI as a view only)
 *
 * This class is pure parsing — never throws on malformed input; returns [ShowdownEvent.Unknown].
 */
object ShowdownInterpreter {

    /**
     * Sealed result types for protocol lines the port understands today.
     */
    sealed class ShowdownEvent {
        abstract val raw: String

        data class Move(
            val actor: String,
            val move: String,
            val target: String?,
            override val raw: String,
        ) : ShowdownEvent()

        data class Damage(
            val pokemon: String,
            val hpInfo: String,
            val from: String?,
            override val raw: String,
        ) : ShowdownEvent()

        data class Heal(
            val pokemon: String,
            val hpInfo: String,
            val from: String?,
            override val raw: String,
        ) : ShowdownEvent()

        data class Status(
            val pokemon: String,
            val status: String,
            override val raw: String,
        ) : ShowdownEvent()

        data class Faint(
            val pokemon: String,
            override val raw: String,
        ) : ShowdownEvent()

        data class Switch(
            val playerSlot: String,
            val details: String,
            val hpInfo: String?,
            override val raw: String,
        ) : ShowdownEvent()

        data class Win(
            val winner: String,
            override val raw: String,
        ) : ShowdownEvent()

        data class Unknown(
            override val raw: String,
            val type: String = "",
        ) : ShowdownEvent()
    }

    /**
     * Parse a multi-line Showdown protocol blob (or single line) into events.
     * Blank / header-only lines are skipped.
     */
    @JvmStatic
    fun parse(message: String): List<ShowdownEvent> {
        if (message.isBlank()) return emptyList()
        val out = ArrayList<ShowdownEvent>(8)
        for (line in message.split('\n', '\r')) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue
            // Protocol lines start with `|`; also accept lines that lost the leading `|`
            val event = parseLine(if (trimmed.startsWith("|")) trimmed else "|$trimmed")
            if (event != null) {
                out.add(event)
            }
        }
        return out
    }

    /**
     * Parse one protocol line. Returns null only for pure empty / `|` alone.
     */
    @JvmStatic
    fun parseLine(line: String): ShowdownEvent? {
        val raw = line.trim()
        if (raw.isEmpty() || raw == "|") return null
        val parts = raw.split('|')
        // parts[0] is empty when line starts with `|`
        if (parts.size < 2) {
            return ShowdownEvent.Unknown(raw)
        }
        val type = parts[1]
        return try {
            when (type) {
                "move" -> parseMove(parts, raw)
                "-damage" -> parseDamageLike(parts, raw, heal = false)
                "-heal" -> parseDamageLike(parts, raw, heal = true)
                "-status" -> parseStatus(parts, raw)
                "faint" -> parseFaint(parts, raw)
                "switch" -> parseSwitch(parts, raw)
                "win" -> parseWin(parts, raw)
                else -> ShowdownEvent.Unknown(raw, type)
            }
        } catch (_: Throwable) {
            ShowdownEvent.Unknown(raw, type)
        }
    }

    /** True if this event is one of the v0 “action log” types (not Unknown). */
    @JvmStatic
    fun isActionEvent(event: ShowdownEvent): Boolean =
        event !is ShowdownEvent.Unknown

    /** Compact one-line summary for battle log UI. */
    @JvmStatic
    fun summarize(event: ShowdownEvent): String = when (event) {
        is ShowdownEvent.Move -> {
            val t = event.target?.let { " → $it" } ?: ""
            "${event.actor} used ${event.move}$t"
        }
        is ShowdownEvent.Damage ->
            "${event.pokemon} took damage (${event.hpInfo})" +
                (event.from?.let { " from $it" } ?: "")
        is ShowdownEvent.Heal ->
            "${event.pokemon} healed (${event.hpInfo})" +
                (event.from?.let { " from $it" } ?: "")
        is ShowdownEvent.Status ->
            "${event.pokemon} → status ${event.status}"
        is ShowdownEvent.Faint ->
            "${event.pokemon} fainted"
        is ShowdownEvent.Switch ->
            "Switch ${event.playerSlot}: ${event.details}" +
                (event.hpInfo?.let { " ($it)" } ?: "")
        is ShowdownEvent.Win ->
            "${event.winner} wins"
        is ShowdownEvent.Unknown ->
            if (event.type.isNotEmpty()) "unhandled |${event.type}|" else "unhandled protocol"
    }

    // ── private parsers ──────────────────────────────────────────────────────

    private fun parseMove(parts: List<String>, raw: String): ShowdownEvent {
        // |move|POKEMON|MOVE|TARGET
        val actor = parts.getOrElse(2) { "" }
        val move = parts.getOrElse(3) { "" }
        val target = parts.getOrNull(4)?.takeIf { it.isNotBlank() && !it.startsWith("[") }
        return ShowdownEvent.Move(actor, move, target, raw)
    }

    private fun parseDamageLike(parts: List<String>, raw: String, heal: Boolean): ShowdownEvent {
        // |-damage|POKEMON|HP|...  or  |-heal|POKEMON|HP|[from] ...
        val pokemon = parts.getOrElse(2) { "" }
        val hp = parts.getOrElse(3) { "" }
        val from = extractFrom(parts)
        return if (heal) {
            ShowdownEvent.Heal(pokemon, hp, from, raw)
        } else {
            ShowdownEvent.Damage(pokemon, hp, from, raw)
        }
    }

    private fun parseStatus(parts: List<String>, raw: String): ShowdownEvent {
        // |-status|POKEMON|STATUS
        return ShowdownEvent.Status(
            pokemon = parts.getOrElse(2) { "" },
            status = parts.getOrElse(3) { "" },
            raw = raw,
        )
    }

    private fun parseFaint(parts: List<String>, raw: String): ShowdownEvent {
        // |faint|POKEMON
        return ShowdownEvent.Faint(parts.getOrElse(2) { "" }, raw)
    }

    private fun parseSwitch(parts: List<String>, raw: String): ShowdownEvent {
        // |switch|POKEMON|DETAILS|HP STATUS
        return ShowdownEvent.Switch(
            playerSlot = parts.getOrElse(2) { "" },
            details = parts.getOrElse(3) { "" },
            hpInfo = parts.getOrNull(4)?.takeIf { it.isNotBlank() },
            raw = raw,
        )
    }

    private fun parseWin(parts: List<String>, raw: String): ShowdownEvent {
        // |win|USERNAME
        return ShowdownEvent.Win(parts.getOrElse(2) { "" }, raw)
    }

    private fun extractFrom(parts: List<String>): String? {
        for (i in 4 until parts.size) {
            val p = parts[i]
            if (p.startsWith("[from]")) {
                return p.removePrefix("[from]").trim().ifBlank { null }
                    ?: parts.getOrNull(i + 1)
            }
        }
        return null
    }
}

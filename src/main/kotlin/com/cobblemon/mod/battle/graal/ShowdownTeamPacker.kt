package com.cobblemon.mod.battle.graal

import com.cobblemon.mod.species.OwnedMon
import com.cobblemon.mod.species.SpeciesHandle

/**
 * Builds Pokémon Showdown **packed team** strings (PS export format used by BattleStream).
 *
 * Packed field order (pipe-separated, no spaces):
 * ```
 * nickname|species|item|ability|moves|nature|EVs|gender|IVs|shiny|level|happiness|pokeball|hpType|pokebusiness|teraType
 * ```
 * We fill the fields we have; empty segments are left blank (Showdown accepts defaults).
 *
 * @see <a href="https://github.com/smogon/pokemon-showdown/blob/master/sim/TEAMS.md">Showdown TEAMS.md</a>
 */
object ShowdownTeamPacker {

    /**
     * Pack one mon into a single-line PS set (no trailing newline).
     */
    @JvmStatic
    fun packMon(mon: OwnedMon): String {
        val species = canonicalizeSpecies(mon.speciesId())
        val nickname = mon.nickname().takeIf { it.isNotBlank() } ?: ""
        val item = mon.heldItem().takeIf { it.isNotBlank() }?.replace("cobblemon:", "") ?: ""
        val ability = mon.ability().id().takeIf { it.isNotBlank() } ?: ""
        val moves = mon.moveIds().take(4).joinToString(",") { canonicalizeMove(it) }
        val nature = mon.nature().getSerializedName().replaceFirstChar { it.uppercase() }
        val evs = packEvs(mon)
        val gender = when (mon.gender().id()) {
            "male", "m" -> "M"
            "female", "f" -> "F"
            else -> ""
        }
        val ivs = packIvs(mon)
        val shiny = if (mon.isShiny() || mon.form().id() == "shiny") "S" else ""
        val level = mon.level().coerceIn(1, 100).toString()
        // nickname|species|item|ability|moves|nature|EVs|gender|IVs|shiny|level|...
        return listOf(
            nickname,
            species,
            item,
            ability,
            moves,
            nature,
            evs,
            gender,
            ivs,
            shiny,
            level,
        ).joinToString("|")
    }

    /**
     * Pack a full team (1–6 mons) as multi-line packed string.
     */
    @JvmStatic
    fun packTeam(mons: List<OwnedMon>): String =
        mons.filterNotNull().take(6).joinToString("\n") { packMon(it) }

    /**
     * Wild proxy mon as a one-mon team.
     */
    @JvmStatic
    fun packWild(speciesId: String, level: Int, moves: List<String>, hp: Int = -1): String {
        val species = canonicalizeSpecies(speciesId)
        val mv = moves.take(4).joinToString(",") { canonicalizeMove(it) }
        val lv = level.coerceIn(1, 100)
        // empty nick|species|||moves|||| | |level
        return "|$species|||$mv|||||$lv"
    }

    /**
     * JSON fragment for `>player pN {"name":"…","team":"…"}` — team is packed string.
     * Escapes quotes in team for embedding in a JS object literal.
     */
    @JvmStatic
    fun playerMessage(playerKey: String, name: String, teamPacked: String): String {
        val escapedTeam = teamPacked
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
        val escapedName = name.replace("\\", "\\\\").replace("\"", "\\\"")
        return ">player $playerKey {\"name\":\"$escapedName\",\"team\":\"$escapedTeam\"}"
    }

    @JvmStatic
    fun startMessages(formatId: String = "gen9customgame"): Array<String> =
        arrayOf(">start {\"formatid\":\"$formatId\"}")

    private fun canonicalizeSpecies(id: String): String {
        val raw = id.lowercase().removePrefix("cobblemon:")
        // Showdown uses no hyphens for most; keep known forms as-is if needed
        return SpeciesHandle.of(raw).id().replace("_", "").replace("-", "")
            .replaceFirstChar { it.uppercase() }
            .let { s ->
                // Showdown species are usually lowercase id without spaces — use raw id
                raw.replace("_", "").ifBlank { s.lowercase() }
            }
    }

    private fun canonicalizeMove(id: String): String =
        id.lowercase().removePrefix("cobblemon:").replace("_", "").replace("-", "").replace(" ", "")

    private fun packEvs(mon: OwnedMon): String {
        // EVs left blank until public Genetics API; Showdown uses defaults
        return ""
    }

    private fun packIvs(mon: OwnedMon): String {
        return ""
    }
}

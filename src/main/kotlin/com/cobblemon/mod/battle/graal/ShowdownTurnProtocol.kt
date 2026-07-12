package com.cobblemon.mod.battle.graal

import com.cobblemon.mod.Cobblemon
import com.cobblemon.mod.battle.BattleMove
import com.cobblemon.mod.battle.BattleStages
import com.cobblemon.mod.species.OwnedMon
import com.google.gson.JsonParser
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Single-turn / protocol glue between our wild battles and Showdown JS.
 *
 * When full sim is ready, can open a BattleStream via [ShowdownSim.startBattle].
 * Always falls back to [ShowdownCombatBridge] / native for actual damage if needed.
 *
 * Captures sim output lines and runs them through [ShowdownInterpreter] (best-effort,
 * never crashes the battle loop).
 *
 * ## Full engine still needs (see also [ShowdownInterpreter] KDoc)
 * - **Packed teams**: real PS team strings (not [packLite]) passed in `>player` messages.
 * - **Choice request**: handle `|request|` JSON (active moves / forceSwitch) and route
 *   player UI choices as `>p1 move N` / `>p1 switch N` via [ShowdownSim.sendBattleMessage].
 * - Apply interpreted HP/status/faint/win events into [com.cobblemon.mod.battle.BattleSession]
 *   instead of only logging summaries.
 */
object ShowdownTurnProtocol {
    private val outputByBattle = ConcurrentHashMap<String, ConcurrentLinkedQueue<String>>()
    private val eventsByBattle = ConcurrentHashMap<String, ConcurrentLinkedQueue<ShowdownInterpreter.ShowdownEvent>>()

    @Volatile
    private var lastMessage: String = "idle"

    @Volatile
    private var lastParsedSummary: String = ""

    @JvmStatic
    fun lastMessage(): String = lastMessage

    @JvmStatic
    fun lastParsedSummary(): String = lastParsedSummary

    /**
     * Called from JS stub when sim emits protocol text.
     * Stores raw lines and best-effort parses via [ShowdownInterpreter] — never throws.
     */
    @JvmStatic
    fun onSimOutput(battleId: String, message: String) {
        if (battleId.isBlank() || message.isBlank()) return
        try {
            outputByBattle
                .getOrPut(battleId) { ConcurrentLinkedQueue() }
                .add(message)
            // Keep queues bounded
            val q = outputByBattle[battleId]
            while (q != null && q.size > 200) {
                q.poll()
            }

            val parsed = ShowdownInterpreter.parse(message)
            if (parsed.isNotEmpty()) {
                val eq = eventsByBattle.getOrPut(battleId) { ConcurrentLinkedQueue() }
                for (ev in parsed) {
                    eq.add(ev)
                    if (ShowdownInterpreter.isActionEvent(ev)) {
                        lastParsedSummary = ShowdownInterpreter.summarize(ev)
                        Cobblemon.LOGGER.debug(
                            "ShowdownTurnProtocol[{}]: {}",
                            battleId,
                            lastParsedSummary,
                        )
                    }
                }
                while (eq.size > 400) {
                    eq.poll()
                }
            }
        } catch (t: Throwable) {
            // Never crash the sim callback path
            Cobblemon.LOGGER.debug("ShowdownTurnProtocol.onSimOutput ignored error: {}", t.toString())
        }
    }

    @JvmStatic
    fun drainOutput(battleId: String): List<String> {
        val q = outputByBattle.remove(battleId) ?: return emptyList()
        return q.toList()
    }

    /** Drain interpreted events for a battle (empty if none / unknown id). */
    @JvmStatic
    fun drainEvents(battleId: String): List<ShowdownInterpreter.ShowdownEvent> {
        val q = eventsByBattle.remove(battleId) ?: return emptyList()
        return q.toList()
    }

    /**
     * Peek (without removing) recent action summaries for debugging / future UI log merge.
     */
    @JvmStatic
    fun peekEventSummaries(battleId: String, max: Int = 16): List<String> {
        val q = eventsByBattle[battleId] ?: return emptyList()
        return q.asSequence()
            .filter { ShowdownInterpreter.isActionEvent(it) }
            .map { ShowdownInterpreter.summarize(it) }
            .take(max.coerceAtLeast(0))
            .toList()
    }

    /**
     * Enrich a [BattleMove] from live Showdown Dex when [ShowdownSim] is ready.
     */
    @JvmStatic
    fun enrichMove(move: BattleMove): BattleMove {
        if (!ShowdownSim.isReady()) return move
        val json = ShowdownSim.getMoveJson(move.id) ?: return move
        return try {
            val o = JsonParser.parseString(json).asJsonObject
            val power = o.get("basePower")?.asInt ?: move.power
            val acc = o.get("accuracy")?.asInt ?: move.accuracy
            val pri = o.get("priority")?.asInt ?: move.priority
            val pp = o.get("pp")?.asInt ?: move.maxPp
            val name = o.get("name")?.asString ?: move.displayName
            move.copy(
                displayName = name,
                power = power,
                accuracy = acc,
                priority = pri,
                maxPp = pp,
            )
        } catch (t: Throwable) {
            move
        }
    }

    /**
     * Resolve damage for one damaging hit.
     * Order: combat bridge JS → null (caller uses native).
     * Optionally logs that sim dex metadata was used.
     */
    @JvmStatic
    fun resolveDamage(
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
        val m = enrichMove(move)
        val dmg = ShowdownCombatBridge.tryDamage(
            m, attacker, defender, atkStages, defStages,
            typeMult, stab, otherMul, crit, hits, randomRoll,
        )
        if (dmg != null) {
            lastMessage = "js-damage move=${m.id} dmg=$dmg simReady=${ShowdownSim.isReady()}"
        }
        return dmg
    }

    /**
     * Attempt to open a protocol battle for a wild encounter.
     * Uses [ShowdownTeamPacker] packed teams; may fail if sim boot incomplete — safe to ignore.
     *
     * When sim emits output, [onSimOutput] feeds [ShowdownInterpreter] automatically.
     * Call [drainAndApply] from the battle loop to push HP/status into the session.
     *
     * @return battle id if started, null otherwise
     */
    @JvmStatic
    fun tryStartWildProtocol(
        playerSpecies: String,
        playerLevel: Int,
        playerMoves: List<String>,
        wildSpecies: String,
        wildLevel: Int,
        wildMoves: List<String>,
    ): String? {
        if (!ShowdownSim.isReady()) return null
        return try {
            val id = UUID.randomUUID().toString()
            val p1Team = ShowdownTeamPacker.packWild(playerSpecies, playerLevel, playerMoves)
            val p2Team = ShowdownTeamPacker.packWild(wildSpecies, wildLevel, wildMoves)
            val messages = arrayOf(
                *ShowdownTeamPacker.startMessages("gen9customgame"),
                ShowdownTeamPacker.playerMessage("p1", "Player", p1Team),
                ShowdownTeamPacker.playerMessage("p2", "Wild", p2Team),
            )
            val ok = ShowdownSim.startBattle(id, messages)
            if (ok) {
                lastMessage = "protocol-start id=$id packed=true"
                Cobblemon.LOGGER.info("ShowdownTurnProtocol: started packed protocol battle {}", id)
                id
            } else {
                // Fallback: random format smoke (no team) so stream still exercises
                val id2 = UUID.randomUUID().toString()
                val ok2 = ShowdownSim.startBattle(
                    id2,
                    arrayOf(
                        ">start {\"formatid\":\"gen9randombattle\"}",
                        ">player p1 {\"name\":\"Player\"}",
                        ">player p2 {\"name\":\"Wild\"}",
                    ),
                )
                if (ok2) {
                    lastMessage = "protocol-start id=$id2 random-fallback"
                    id2
                } else {
                    lastMessage = "protocol-start failed"
                    null
                }
            }
        } catch (t: Throwable) {
            lastMessage = "protocol-start error: ${t.message}"
            null
        }
    }

    /**
     * Start protocol battle from real [OwnedMon] + wild proxy (preferred).
     */
    @JvmStatic
    fun tryStartWildProtocolFromOwned(
        playerMon: OwnedMon,
        wildSpecies: String,
        wildLevel: Int,
        wildMoves: List<String>,
    ): String? {
        if (!ShowdownSim.isReady()) return null
        return try {
            val id = UUID.randomUUID().toString()
            val p1Team = ShowdownTeamPacker.packMon(playerMon)
            val p2Team = ShowdownTeamPacker.packWild(wildSpecies, wildLevel, wildMoves)
            val messages = arrayOf(
                *ShowdownTeamPacker.startMessages("gen9customgame"),
                ShowdownTeamPacker.playerMessage("p1", "Player", p1Team),
                ShowdownTeamPacker.playerMessage("p2", "Wild", p2Team),
            )
            if (ShowdownSim.startBattle(id, messages)) {
                lastMessage = "protocol-start owned id=$id"
                Cobblemon.LOGGER.info("ShowdownTurnProtocol: owned packed battle {}", id)
                id
            } else {
                tryStartWildProtocol(
                    playerMon.speciesId(),
                    playerMon.level(),
                    playerMon.moveIds(),
                    wildSpecies,
                    wildLevel,
                    wildMoves,
                )
            }
        } catch (t: Throwable) {
            lastMessage = "protocol-start owned error: ${t.message}"
            null
        }
    }

    /**
     * Send a move choice to an open protocol battle (p1 = player).
     * @param moveSlot 1-based Showdown slot
     */
    @JvmStatic
    fun sendPlayerMove(battleId: String, moveSlot: Int): Boolean {
        if (battleId.isBlank() || !ShowdownSim.isReady()) return false
        return try {
            val slot = moveSlot.coerceIn(1, 4)
            ShowdownSim.sendBattleMessage(battleId, arrayOf(">p1 move $slot"))
            lastMessage = "choice move $slot id=$battleId"
            true
        } catch (t: Throwable) {
            lastMessage = "choice failed: ${t.message}"
            false
        }
    }

    /**
     * Send wild / p2 move choice (1-based slot).
     */
    @JvmStatic
    fun sendWildMove(battleId: String, moveSlot: Int): Boolean {
        if (battleId.isBlank() || !ShowdownSim.isReady()) return false
        return try {
            val slot = moveSlot.coerceIn(1, 4)
            ShowdownSim.sendBattleMessage(battleId, arrayOf(">p2 move $slot"))
            lastMessage = "choice p2 move $slot id=$battleId"
            true
        } catch (t: Throwable) {
            lastMessage = "p2 choice failed: ${t.message}"
            false
        }
    }

    @JvmStatic
    fun sendPlayerSwitch(battleId: String, partySlot1Based: Int): Boolean {
        if (battleId.isBlank() || !ShowdownSim.isReady()) return false
        return try {
            val slot = partySlot1Based.coerceIn(1, 6)
            ShowdownSim.sendBattleMessage(battleId, arrayOf(">p1 switch $slot"))
            lastMessage = "choice switch $slot id=$battleId"
            true
        } catch (t: Throwable) {
            lastMessage = "switch choice failed: ${t.message}"
            false
        }
    }

    /**
     * B1 authoritative turn: both sides choose, sim resolves, events drive HP/status.
     *
     * @param playerMoveSlot0 0-based move index on player's active mon
     * @param wildMoveSlot0 0-based move index for wild (AI pick)
     * @return true if sim produced actionable events (session HP updated from sim)
     */
    @JvmStatic
    fun runAuthoritativeTurn(
        session: com.cobblemon.mod.battle.BattleSession,
        playerMoveSlot0: Int,
        wildMoveSlot0: Int,
    ): Boolean {
        val battleId = session.showdownBattleId() ?: return false
        if (!ShowdownSim.canRunProtocolBattles()) return false
        return try {
            val p1 = (playerMoveSlot0 + 1).coerceIn(1, 4)
            val p2 = (wildMoveSlot0 + 1).coerceIn(1, 4)
            val beforePlayer = session.playerMon()?.hp() ?: 0
            val beforeWild = session.wildHp()
            // Clear stale events so we only apply this turn
            drainEvents(battleId)
            ShowdownSim.flushOutbox()
            // Cobblemon-style: both sides choose in one message batch when possible
            var ok = ShowdownSim.sendBattleMessage(
                battleId,
                arrayOf(">p1 move $p1", ">p2 move $p2"),
            )
            if (!ok) {
                val ok1 = ShowdownSim.sendBattleMessage(battleId, arrayOf(">p1 move $p1"))
                val ok2 = ShowdownSim.sendBattleMessage(battleId, arrayOf(">p2 move $p2"))
                ok = ok1 || ok2
            }
            // Alternate PS default format (no spaces)
            if (!ok) {
                ok = ShowdownSim.sendBattleMessage(battleId, arrayOf("p1 move $p1", "p2 move $p2"))
            }
            ShowdownSim.flushOutbox()
            // Brief second flush — some streams emit async
            try {
                Thread.sleep(15)
            } catch (_: InterruptedException) {
            }
            ShowdownSim.flushOutbox()
            val applied = drainAndApply(battleId, session)
            val hpChanged = (session.playerMon()?.hp() ?: 0) != beforePlayer || session.wildHp() != beforeWild
            lastMessage = "auth turn p1=$p1 p2=$p2 applied=$applied hpChanged=$hpChanged"
            if (applied > 0 || hpChanged) {
                session.setSimAuthoritative(true)
                true
            } else {
                session.setSimAuthoritative(false)
                false
            }
        } catch (t: Throwable) {
            lastMessage = "auth turn error: ${t.message}"
            Cobblemon.LOGGER.debug("runAuthoritativeTurn failed: {}", t.toString())
            false
        }
    }

    /**
     * Drain interpreter events for [battleId] and apply them to [session].
     * Returns number of events applied.
     */
    @JvmStatic
    fun drainAndApply(battleId: String, session: com.cobblemon.mod.battle.BattleSession): Int {
        if (battleId.isBlank() || session == null) return 0
        return try {
            ShowdownSim.flushOutbox()
            val events = drainEvents(battleId)
            if (events.isEmpty()) return 0
            com.cobblemon.mod.battle.ShowdownSessionBridge.applyEvents(session, events)
            events.size
        } catch (t: Throwable) {
            Cobblemon.LOGGER.debug("drainAndApply failed: {}", t.toString())
            0
        }
    }

    /**
     * Feed arbitrary protocol text through the interpreter for a logical battle id
     * (unit tests / native→log bridges). Never throws.
     */
    @JvmStatic
    fun interpretChunk(battleId: String, protocolText: String): List<ShowdownInterpreter.ShowdownEvent> {
        return try {
            onSimOutput(battleId, protocolText)
            ShowdownInterpreter.parse(protocolText)
        } catch (t: Throwable) {
            Cobblemon.LOGGER.debug("ShowdownTurnProtocol.interpretChunk failed: {}", t.toString())
            emptyList()
        }
    }

}

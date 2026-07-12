package com.cobblemon.mod.battle;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.cobblemon.mod.species.MonSpecies;
import com.cobblemon.mod.species.OwnedMon;
import com.cobblemon.mod.species.SpeciesHandle;
import com.cobblemon.mod.species.StatBlock;

/**
 * Active wild battle state for one player (server-side).
 * Wild mon identity is a datapack {@link #wildSpeciesId} — never only Gen1 enum.
 */
public final class BattleSession {
    public enum Phase {
        PLAYER_TURN,
        ENEMY_TURN,
        WON,
        LOST,
        RAN,
        CAUGHT
    }

    private final UUID playerId;
    private final UUID wildEntityId;
    private final String wildSpeciesId;
    private final int wildLevel;
    private int wildHp;
    private final int wildMaxHp;

    private int partySlot;
    private OwnedMon playerMon;
    private boolean playerFocus;
    private boolean enemyFocus;
    private int playerDefBoost;
    private int enemyDefBoost;
    private int playerAtkDrop;
    private int enemyAtkDrop;

    private Phase phase = Phase.PLAYER_TURN;
    private final List<String> log = new ArrayList<>();
    private int turnCount;
    private com.cobblemon.mod.species.MonStatus wildStatus = com.cobblemon.mod.species.MonStatus.NONE;

    public BattleSession(
            UUID playerId,
            UUID wildEntityId,
            String wildSpeciesId,
            int wildLevel,
            int partySlot,
            OwnedMon playerMon
    ) {
        this.playerId = playerId;
        this.wildEntityId = wildEntityId;
        SpeciesHandle handle = SpeciesHandle.of(wildSpeciesId);
        this.wildSpeciesId = handle.id();
        this.wildLevel = Math.max(1, Math.min(100, wildLevel));
        this.wildMaxHp = Math.max(1, StatBlock.compute(handle, this.wildLevel,
                com.cobblemon.mod.species.Nature.HARDY,
                com.cobblemon.mod.species.MonForm.NORMAL).hp());
        this.wildHp = wildMaxHp;
        this.partySlot = partySlot;
        this.playerMon = playerMon;
        log.add("A wild " + handle.displayName().getString() + " (Lv." + this.wildLevel + ") appeared!");
        log.add("Go, " + safeName(playerMon) + "!");
    }

    public UUID playerId() {
        return playerId;
    }

    public UUID wildEntityId() {
        return wildEntityId;
    }

    /** Real datapack species id (e.g. {@code sigilyph}). */
    public String wildSpeciesId() {
        return wildSpeciesId;
    }

    public SpeciesHandle wildHandle() {
        return SpeciesHandle.of(wildSpeciesId);
    }

    /** Gen1 enum stand-in only — prefer {@link #wildSpeciesId()} for names/moves/stats. */
    public MonSpecies wildSpecies() {
        return wildHandle().asEnumOrFallback();
    }

    public String wildDisplayName() {
        return wildHandle().displayName().getString();
    }

    public int wildLevel() {
        return wildLevel;
    }

    public int wildHp() {
        return wildHp;
    }

    public int wildMaxHp() {
        return wildMaxHp;
    }

    public float wildHpRatio() {
        return wildMaxHp <= 0 ? 0f : (float) wildHp / wildMaxHp;
    }

    public int partySlot() {
        return partySlot;
    }

    public OwnedMon playerMon() {
        return playerMon;
    }

    public void setPlayerMon(OwnedMon mon, int slot) {
        this.playerMon = mon;
        this.partySlot = slot;
        this.playerFocus = false;
        this.playerDefBoost = 0;
        this.playerAtkDrop = 0;
        log.add("Go, " + safeName(mon) + "!");
    }

    /** Replace active mon data without a switch announcement (e.g. PP spend). */
    public void updatePlayerMon(OwnedMon mon) {
        if (mon != null) {
            this.playerMon = mon;
        }
    }

    private static String safeName(OwnedMon mon) {
        if (mon == null) {
            return "???";
        }
        String s = mon.displayName().getString();
        if (s == null || s.isBlank() || s.contains(".")) {
            return mon.handle().displayName().getString();
        }
        return s;
    }

    public Phase phase() {
        return phase;
    }

    public void setPhase(Phase phase) {
        this.phase = phase;
    }

    public int turnCount() {
        return turnCount;
    }

    public void bumpTurn() {
        turnCount++;
    }

    public com.cobblemon.mod.species.MonStatus wildStatus() {
        return wildStatus == null ? com.cobblemon.mod.species.MonStatus.NONE : wildStatus;
    }

    public void setWildStatus(com.cobblemon.mod.species.MonStatus status) {
        this.wildStatus = status == null ? com.cobblemon.mod.species.MonStatus.NONE : status;
    }

    public List<String> log() {
        return List.copyOf(log);
    }

    public List<String> recentLog(int max) {
        int from = Math.max(0, log.size() - max);
        return List.copyOf(log.subList(from, log.size()));
    }

    public void addLog(String line) {
        log.add(line);
        if (log.size() > 40) {
            log.remove(0);
        }
    }

    public boolean isOver() {
        return phase == Phase.WON || phase == Phase.LOST || phase == Phase.RAN || phase == Phase.CAUGHT;
    }

    public void damageWild(int amount) {
        wildHp = Math.max(0, wildHp - amount);
    }

    public void healWild(int amount) {
        wildHp = Math.min(wildMaxHp, wildHp + amount);
    }

    public void setPlayerMonHp(int hp) {
        playerMon = playerMon.withHp(hp);
    }

    public boolean playerFocus() {
        return playerFocus;
    }

    public void setPlayerFocus(boolean v) {
        playerFocus = v;
    }

    public boolean enemyFocus() {
        return enemyFocus;
    }

    public void setEnemyFocus(boolean v) {
        enemyFocus = v;
    }

    public int playerDefBoost() {
        return playerDefBoost;
    }

    public void addPlayerDefBoost(int v) {
        playerDefBoost = Math.min(3, playerDefBoost + v);
    }

    public int enemyDefBoost() {
        return enemyDefBoost;
    }

    public void addEnemyDefBoost(int v) {
        enemyDefBoost = Math.min(3, enemyDefBoost + v);
    }

    public int playerAtkDrop() {
        return playerAtkDrop;
    }

    public void addPlayerAtkDrop(int v) {
        playerAtkDrop = Math.min(3, playerAtkDrop + v);
    }

    public int enemyAtkDrop() {
        return enemyAtkDrop;
    }

    public void addEnemyAtkDrop(int v) {
        enemyAtkDrop = Math.min(3, enemyAtkDrop + v);
    }
}

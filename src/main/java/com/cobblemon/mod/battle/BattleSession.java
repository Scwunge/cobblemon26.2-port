package com.cobblemon.mod.battle;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.cobblemon.mod.species.MonElement;
import com.cobblemon.mod.species.MonSpecies;
import com.cobblemon.mod.species.OwnedMon;
import com.cobblemon.mod.species.SpeciesHandle;
import com.cobblemon.mod.species.StatBlock;

/**
 * Active wild / trainer battle state for one player (server-side).
 * Wild mon identity is a datapack {@link #wildSpeciesId} — never only Gen1 enum.
 * <p>
 * Supports optional {@link Format#DOUBLES} with a second active mon on each side (B3).
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

    public enum Format {
        SINGLES,
        DOUBLES
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

    /** Full −6…+6 stages for both sides. */
    private final BattleStages playerStages = new BattleStages();
    private final BattleStages wildStages = new BattleStages();
    /** Weather, terrain, entry hazards. */
    private final BattleFieldEffects field = new BattleFieldEffects();

    private Phase phase = Phase.PLAYER_TURN;
    private Format format = Format.SINGLES;
    private final List<String> log = new ArrayList<>();
    private int turnCount;
    private com.cobblemon.mod.species.MonStatus wildStatus = com.cobblemon.mod.species.MonStatus.NONE;
    /** Last move names for UI (enemy moves were easy to miss in a tiny log). */
    private String lastPlayerMoveName = "";
    private String lastWildMoveName = "";
    /** Player's sent-out mon entity during this battle (in-world presentation). */
    private UUID companionEntityId;
    /** Optional Showdown protocol battle id (null if sim offline). */
    private String showdownBattleId;
    /**
     * When true, the last turn's HP/status came from Showdown events (B1).
     * Native calc should not re-apply damage that turn.
     */
    private boolean simAuthoritative;
    /** Gym badge id awarded on win (N3); blank if none. */
    private String rewardBadgeId = "";
    /** B4 — opponent player UUID when this is a PvP challenge battle. */
    private UUID pvpOpponentId;

    // --- Doubles second slot (B3) ---
    private UUID wildEntityId2;
    private String wildSpeciesId2 = "";
    private int wildLevel2;
    private int wildHp2;
    private int wildMaxHp2;
    private int partySlot2 = -1;
    private OwnedMon playerMon2;
    private UUID companionEntityId2;

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

    /** Configure a second wild + second party mon for doubles (B3). */
    public void enableDoubles(
            UUID wild2Id,
            String wild2Species,
            int wild2Level,
            int party2Slot,
            OwnedMon mon2
    ) {
        this.format = Format.DOUBLES;
        this.wildEntityId2 = wild2Id;
        SpeciesHandle h = SpeciesHandle.of(wild2Species);
        this.wildSpeciesId2 = h.id();
        this.wildLevel2 = Math.max(1, Math.min(100, wild2Level));
        this.wildMaxHp2 = Math.max(1, StatBlock.compute(h, this.wildLevel2,
                com.cobblemon.mod.species.Nature.HARDY,
                com.cobblemon.mod.species.MonForm.NORMAL).hp());
        this.wildHp2 = wildMaxHp2;
        this.partySlot2 = party2Slot;
        this.playerMon2 = mon2;
        log.add("A wild " + h.displayName().getString() + " (Lv." + this.wildLevel2 + ") joined the fray!");
        if (mon2 != null) {
            log.add("Go, " + safeName(mon2) + "!");
        }
        log.add("§bDouble battle!");
    }

    public UUID playerId() {
        return playerId;
    }

    public UUID wildEntityId() {
        return wildEntityId;
    }

    public UUID companionEntityId() {
        return companionEntityId;
    }

    public void setCompanionEntityId(UUID id) {
        this.companionEntityId = id;
    }

    public String showdownBattleId() {
        return showdownBattleId;
    }

    public void setShowdownBattleId(String id) {
        this.showdownBattleId = id;
    }

    public boolean isSimAuthoritative() {
        return simAuthoritative;
    }

    public void setSimAuthoritative(boolean v) {
        this.simAuthoritative = v;
    }

    public Format format() {
        return format == null ? Format.SINGLES : format;
    }

    public boolean isDoubles() {
        return format() == Format.DOUBLES;
    }

    public String rewardBadgeId() {
        return rewardBadgeId == null ? "" : rewardBadgeId;
    }

    public void setRewardBadgeId(String id) {
        this.rewardBadgeId = id == null ? "" : id;
    }

    public UUID pvpOpponentId() {
        return pvpOpponentId;
    }

    public void setPvpOpponentId(UUID id) {
        this.pvpOpponentId = id;
    }

    public boolean isPvp() {
        return pvpOpponentId != null;
    }

    public UUID wildEntityId2() {
        return wildEntityId2;
    }

    public String wildSpeciesId2() {
        return wildSpeciesId2 == null ? "" : wildSpeciesId2;
    }

    public int wildHp2() {
        return wildHp2;
    }

    public int wildMaxHp2() {
        return wildMaxHp2;
    }

    public int wildLevel2() {
        return wildLevel2;
    }

    public void damageWild2(int amount) {
        wildHp2 = Math.max(0, wildHp2 - amount);
    }

    public int partySlot2() {
        return partySlot2;
    }

    public OwnedMon playerMon2() {
        return playerMon2;
    }

    public void setPlayerMon2(OwnedMon mon, int slot) {
        this.playerMon2 = mon;
        this.partySlot2 = slot;
    }

    public void setPlayerMon2Hp(int hp) {
        if (playerMon2 != null) {
            playerMon2 = playerMon2.withHp(hp);
        }
    }

    public UUID companionEntityId2() {
        return companionEntityId2;
    }

    public void setCompanionEntityId2(UUID id) {
        this.companionEntityId2 = id;
    }

    public boolean bothWildsFainted() {
        if (!isDoubles()) {
            return wildHp <= 0;
        }
        return wildHp <= 0 && wildHp2 <= 0;
    }

    public String wildSpeciesId() {
        return wildSpeciesId;
    }

    public SpeciesHandle wildHandle() {
        return SpeciesHandle.of(wildSpeciesId);
    }

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

    public BattleFieldEffects field() {
        return field;
    }

    public void setPlayerMon(OwnedMon mon, int slot) {
        this.playerMon = mon;
        this.partySlot = slot;
        this.playerFocus = false;
        this.playerStages.clear(); // switching resets your stages
        log.add("Go, " + safeName(mon) + "!");
        applyEntryHazardsToPlayer();
    }

    /** Entry hazards when the player mon switches in (not on every HP update). */
    private void applyEntryHazardsToPlayer() {
        if (playerMon == null) {
            return;
        }
        int haz = field.onSwitchInDamage(true, playerMon.handle(), playerMon.maxHp());
        if (haz > 0) {
            playerMon = playerMon.withHp(Math.max(0, playerMon.hp() - haz));
            log.add(safeName(playerMon) + " is hurt by entry hazards! (" + haz + ")");
        }
        int tsp = field.toxicSpikesLayers(true);
        if (tsp > 0 && playerMon.status().isNone()
                && playerMon.primaryType() != MonElement.POISON
                && playerMon.secondaryType().orElse(null) != MonElement.POISON) {
            playerMon = playerMon.withStatus(com.cobblemon.mod.species.MonStatus.POISON);
            log.add(safeName(playerMon) + " was poisoned by Toxic Spikes!");
        }
        if (field.hasStickyWeb(true)) {
            playerStages.add(StatKind.SPE, -1);
            log.add(safeName(playerMon) + " was caught in a sticky web!");
        }
    }

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
        if (log.size() > 48) {
            log.remove(0);
        }
    }

    public String lastPlayerMoveName() {
        return lastPlayerMoveName == null ? "" : lastPlayerMoveName;
    }

    public String lastWildMoveName() {
        return lastWildMoveName == null ? "" : lastWildMoveName;
    }

    public void setLastPlayerMoveName(String name) {
        this.lastPlayerMoveName = name == null ? "" : name;
    }

    public void setLastWildMoveName(String name) {
        this.lastWildMoveName = name == null ? "" : name;
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

    public BattleStages playerStages() {
        return playerStages;
    }

    public BattleStages wildStages() {
        return wildStages;
    }

    /**
     * Apply boosts to a side and log. {@code toPlayer} true = player mon, false = wild.
     * @return true if any stage actually changed
     */
    public boolean applyBoosts(boolean toPlayer, List<ShowdownMoveDex.StatBoost> boosts, String actorName) {
        if (boosts == null || boosts.isEmpty()) {
            return false;
        }
        BattleStages stages = toPlayer ? playerStages : wildStages;
        boolean any = false;
        for (ShowdownMoveDex.StatBoost b : boosts) {
            int before = stages.get(b.getKind());
            int after = stages.add(b.getKind(), b.getStages());
            if (after != before) {
                any = true;
                String who = toPlayer ? (playerMon != null ? safeName(playerMon) : "Your mon") : ("Wild " + wildDisplayName());
                addLog(who + "'s " + BattleStages.statName(b.getKind()) + " " + BattleStages.stageLabel(b.getStages()));
            }
        }
        return any;
    }

    // --- Back-compat shims for any leftover callers ---

    public int playerDefBoost() {
        return Math.max(0, playerStages.get(StatKind.DEF));
    }

    public void addPlayerDefBoost(int v) {
        playerStages.add(StatKind.DEF, v);
    }

    public int enemyDefBoost() {
        return Math.max(0, wildStages.get(StatKind.DEF));
    }

    public void addEnemyDefBoost(int v) {
        wildStages.add(StatKind.DEF, v);
    }

    public int playerAtkDrop() {
        return Math.max(0, -playerStages.get(StatKind.ATK));
    }

    public void addPlayerAtkDrop(int v) {
        playerStages.add(StatKind.ATK, -v);
    }

    public int enemyAtkDrop() {
        return Math.max(0, -wildStages.get(StatKind.ATK));
    }

    public void addEnemyAtkDrop(int v) {
        wildStages.add(StatKind.ATK, -v);
    }
}

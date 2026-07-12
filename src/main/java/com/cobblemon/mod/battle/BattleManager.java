package com.cobblemon.mod.battle;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.cobblemon.mod.entity.WildMonEntity;
import com.cobblemon.mod.item.BallContext;
import com.cobblemon.mod.item.CatchCalc;
import com.cobblemon.mod.item.CubeBallTier;
import com.cobblemon.mod.network.BattleUpdatePayload;
import com.cobblemon.mod.party.PartyHelper;
import com.cobblemon.mod.party.PlayerParty;
import com.cobblemon.mod.species.MonSpecies;
import com.cobblemon.mod.species.OwnedMon;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Server-side wild battle controller.
 */
public final class BattleManager {
    private static final Map<UUID, BattleSession> SESSIONS = new ConcurrentHashMap<>();
    /** Master Ball etc. — next catch roll always succeeds. */
    private static final java.util.Set<UUID> GUARANTEED_CATCH = ConcurrentHashMap.newKeySet();

    /**
     * Max center-to-center distance to start a wild battle (attack / right-click / auto).
     * Kept a bit snug so oversized hitboxes don't pull you into fights from far away.
     * Cobblemon's default wild battle max is higher (~12); we intentionally stay closer.
     */
    public static final double MAX_ENGAGE_DISTANCE = 3.0;
    public static final double MAX_ENGAGE_DISTANCE_SQR = MAX_ENGAGE_DISTANCE * MAX_ENGAGE_DISTANCE;

    private BattleManager() {}

    public static Optional<BattleSession> get(ServerPlayer player) {
        return Optional.ofNullable(SESSIONS.get(player.getUUID()));
    }

    public static boolean inBattle(ServerPlayer player) {
        BattleSession s = SESSIONS.get(player.getUUID());
        return s != null && !s.isOver();
    }

    /** Whether the player is close enough to engage this mon. */
    public static boolean inEngageRange(ServerPlayer player, WildMonEntity wild) {
        return player.distanceToSqr(wild) <= MAX_ENGAGE_DISTANCE_SQR;
    }

    public static boolean tryStart(ServerPlayer player, WildMonEntity wild) {
        if (wild.isCompanion() || wild.isRemoved()) {
            return false;
        }
        // Too far — don't start (avoids "I wasn't even close" from big hitboxes / long reach)
        if (!inEngageRange(player, wild)) {
            return false;
        }
        if (inBattle(player)) {
            player.sendSystemMessage(Component.translatable("message.cobblemon.already_battling"));
            return false;
        }
        PlayerParty party = PartyHelper.get(player);
        if (party.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.cobblemon.party_empty"));
            return false;
        }
        // First conscious mon
        int slot = -1;
        OwnedMon mon = null;
        for (int i = 0; i < party.size(); i++) {
            OwnedMon m = party.get(i).orElseThrow();
            if (!m.isFainted()) {
                slot = i;
                mon = m;
                break;
            }
        }
        if (mon == null) {
            player.sendSystemMessage(Component.translatable("message.cobblemon.all_fainted"));
            return false;
        }

        BattleSession session = new BattleSession(
                player.getUUID(),
                wild.getUUID(),
                wild.getSpeciesId(),
                wild.getMonLevel(),
                slot,
                mon
        );
        SESSIONS.put(player.getUUID(), session);
        PartyHelper.markSeen(player, wild.getSpeciesId());
        // In-world presentation: send out ally, park both mons, face, cry
        try {
            BattlePresentation.setupField(player, wild, mon, session);
        } catch (Throwable t) {
            // Still allow menu battle if presentation fails
            wild.getNavigation().stop();
            wild.setNoAi(true);
        }
        // Showdown protocol stream with packed teams (non-fatal if sim offline)
        try {
            java.util.List<String> wMoves = wildMoves(session).stream()
                    .map(BattleMove::getId).toList();
            String battleId = com.cobblemon.mod.battle.graal.ShowdownTurnProtocol.tryStartWildProtocolFromOwned(
                    mon, wild.getSpeciesId(), wild.getMonLevel(), wMoves
            );
            if (battleId != null) {
                session.setShowdownBattleId(battleId);
                // B1 harden: pull start-of-battle protocol lines immediately
                com.cobblemon.mod.battle.graal.ShowdownTurnProtocol.drainAndApply(battleId, session);
            }
        } catch (Throwable ignored) {
            // native battle continues regardless
        }
        sync(player, session);
        return true;
    }

    public static void end(ServerPlayer player, BattleSession.Phase endPhase) {
        BattleSession s = SESSIONS.get(player.getUUID());
        if (s == null) {
            return;
        }
        s.setPhase(endPhase);
        boolean wildAlive = endPhase != BattleSession.Phase.WON && endPhase != BattleSession.Phase.CAUGHT;
        try {
            BattlePresentation.teardown(player, s, wildAlive);
        } catch (Throwable ignored) {
        }
        sync(player, s);
        SESSIONS.remove(player.getUUID());
    }

    public static void forceClear(UUID playerId) {
        BattleSession s = SESSIONS.remove(playerId);
        GUARANTEED_CATCH.remove(playerId);
        if (s != null) {
            // Best-effort: no player reference — discard companion if we can find a level later
        }
    }

    public static void markNextCatchGuaranteed(ServerPlayer player) {
        if (player != null) {
            GUARANTEED_CATCH.add(player.getUUID());
        }
    }

    public static void handleMove(ServerPlayer player, int moveIndex) {
        BattleSession s = SESSIONS.get(player.getUUID());
        if (s == null || s.phase() != BattleSession.Phase.PLAYER_TURN) {
            return;
        }
        List<BattleMove> moves = s.playerMon().battleMoves();
        if (moveIndex < 0 || moveIndex >= moves.size()) {
            return;
        }
        BattleMove move = moves.get(moveIndex);

        // B1: when a full Showdown protocol battle is live, try sim-authoritative resolution first
        if (trySimAuthoritativeTurn(player, s, move, moveIndex)) {
            if (s.isOver()) {
                finishIfNeeded(player, s);
                return;
            }
            // Doubles partner auto-attack (native) after sim resolved primary pair
            if (s.isDoubles()) {
                runDoublesPartnerActions(player, s);
            }
            if (s.isOver()) {
                finishIfNeeded(player, s);
                return;
            }
            s.setPhase(BattleSession.Phase.PLAYER_TURN);
            sync(player, s);
            return;
        }

        // Native path (always available fallback)
        s.setSimAuthoritative(false);
        boolean playerFirst = goesFirst(s, move);
        if (playerFirst) {
            runPlayerMove(player, s, move, moveIndex);
            if (s.isOver()) {
                finishIfNeeded(player, s);
                return;
            }
            s.setPhase(BattleSession.Phase.ENEMY_TURN);
            runEnemyTurn(player, s);
        } else {
            s.setPhase(BattleSession.Phase.ENEMY_TURN);
            runEnemyTurn(player, s);
            if (s.isOver()) {
                finishIfNeeded(player, s);
                return;
            }
            if (s.playerMon().hp() > 0) {
                runPlayerMove(player, s, move, moveIndex);
            }
        }
        if (s.isDoubles() && !s.isOver()) {
            runDoublesPartnerActions(player, s);
        }
        if (s.isOver()) {
            finishIfNeeded(player, s);
            return;
        }
        s.setPhase(BattleSession.Phase.PLAYER_TURN);
        sync(player, s);
    }

    /**
     * B1 — run one full turn via Showdown; HP/status come from protocol events.
     * @return true if sim handled the turn (caller must not double-apply native damage)
     */
    private static boolean trySimAuthoritativeTurn(
            ServerPlayer player, BattleSession s, BattleMove playerMove, int moveIndex
    ) {
        if (s.showdownBattleId() == null) {
            return false;
        }
        if (!com.cobblemon.mod.battle.graal.ShowdownSim.canRunProtocolBattles()) {
            return false;
        }
        try {
            // PP still spent on our side (Showdown tracks its own PP)
            OwnedMon mon = s.playerMon();
            if (moveIndex >= 0 && !mon.hasPp(moveIndex)) {
                s.addLog(playerMove.englishName() + " has no PP left!");
                persistPlayer(player, s);
                return true; // handled as no-op turn
            }
            if (moveIndex >= 0) {
                mon = mon.consumePp(moveIndex);
                s.updatePlayerMon(mon);
            }
            s.setLastPlayerMoveName(playerMove.englishName());
            s.addLog(mon.displayName().getString() + " used " + playerMove.englishName() + "!");
            s.addLog("§8(Showdown sim resolving…)");

            List<BattleMove> foeMoves = wildMoves(s);
            int wildSlot = 0;
            if (!foeMoves.isEmpty()) {
                BattleMove foe = pickEnemyMove(s, foeMoves, player.level().getRandom());
                s.setLastWildMoveName(foe.englishName());
                wildSlot = Math.max(0, foeMoves.indexOf(foe));
            }

            int beforePlayerHp = s.playerMon().hp();
            int beforeWildHp = s.wildHp();
            boolean ok = com.cobblemon.mod.battle.graal.ShowdownTurnProtocol.runAuthoritativeTurn(
                    s, moveIndex, wildSlot
            );
            if (!ok) {
                s.addLog("§7Sim had no events — falling back to native damage.");
                s.setSimAuthoritative(false);
                return false;
            }
            // Hit FX if HP changed
            if (s.playerMon().hp() != beforePlayerHp || s.wildHp() != beforeWildHp) {
                try {
                    BattlePresentation.playHitReaction(player, s, s.wildHp() < beforeWildHp);
                } catch (Throwable ignored) {
                }
            }
            if (s.wildHp() <= 0 && (!s.isDoubles() || s.wildHp2() <= 0)) {
                s.setPhase(BattleSession.Phase.WON);
                s.addLog("Wild " + s.wildDisplayName() + " fainted!");
                removeWild(player, s);
                applyExp(player, s, BattleCalc.expForWin(s.playerMon().level(), s.wildLevel(), s.wildHandle().stage()));
            } else if (s.playerMon().hp() <= 0) {
                s.addLog(s.playerMon().displayName().getString() + " fainted!");
                persistPlayer(player, s);
                autoSwitchOrBlackout(player, s);
            } else {
                persistPlayer(player, s);
            }
            return true;
        } catch (Throwable t) {
            s.setSimAuthoritative(false);
            return false;
        }
    }

    /** B3 — second mon + second wild act once with native calc (doubles). */
    private static void runDoublesPartnerActions(ServerPlayer player, BattleSession s) {
        if (!s.isDoubles() || s.isOver()) {
            return;
        }
        // Partner mon attacks wild 2 (or wild 1 if wild2 already down)
        OwnedMon partner = s.playerMon2();
        if (partner != null && partner.hp() > 0) {
            List<BattleMove> pMoves = partner.battleMoves();
            if (!pMoves.isEmpty()) {
                BattleMove m = pMoves.get(player.level().getRandom().nextInt(pMoves.size()));
                s.addLog(partner.displayName().getString() + " used " + m.englishName() + "!");
                boolean target2 = s.wildHp2() > 0;
                OwnedMon wildDef = OwnedMon.createWild(
                        target2 ? s.wildSpeciesId2() : s.wildSpeciesId(),
                        target2 ? s.wildLevel2() : s.wildLevel(),
                        player.level().getRandom()
                ).withHp(target2 ? s.wildHp2() : s.wildHp()).withStatus(s.wildStatus());
                BattleCalc.DamageResult r = BattleCalc.useMove(
                        m, partner, wildDef, s.playerStages(), s.wildStages(),
                        player.level().getRandom(), false, s.field()
                );
                if (!r.missed() && !r.immune() && r.damage() > 0) {
                    if (target2) {
                        s.damageWild2(r.damage());
                        s.addLog("Dealt " + r.damage() + " to the second foe!");
                    } else {
                        s.damageWild(r.damage());
                        s.addLog("Dealt " + r.damage() + " damage!");
                    }
                }
            }
        }
        // Second wild attacks player (or partner)
        if (s.wildHp2() > 0) {
            List<BattleMove> foeMoves = com.cobblemon.mod.battle.DatapackLearnsets.battleMovesKnownAtLevel(
                    s.wildSpeciesId2(), s.wildLevel2()
            ).stream().map(bm -> BattleMove.resolve(bm.getId())).toList();
            if (foeMoves.isEmpty()) {
                foeMoves = List.of(BattleMove.of(MonMove.TACKLE));
            }
            BattleMove m = foeMoves.get(player.level().getRandom().nextInt(foeMoves.size()));
            s.addLog("Foe " + com.cobblemon.mod.species.SpeciesHandle.of(s.wildSpeciesId2()).displayName().getString()
                    + " used " + m.englishName() + "!");
            OwnedMon target = (s.playerMon2() != null && s.playerMon2().hp() > 0 && player.level().getRandom().nextBoolean())
                    ? s.playerMon2() : s.playerMon();
            boolean hitPartner = target == s.playerMon2();
            OwnedMon wildAtk = OwnedMon.createWild(s.wildSpeciesId2(), s.wildLevel2(), player.level().getRandom())
                    .withHp(s.wildHp2());
            BattleCalc.DamageResult r = BattleCalc.useMove(
                    m, wildAtk, target, s.wildStages(), s.playerStages(),
                    player.level().getRandom(), false, s.field()
            );
            if (!r.missed() && !r.immune() && r.damage() > 0) {
                int nh = Math.max(0, target.hp() - r.damage());
                if (hitPartner) {
                    s.setPlayerMon2Hp(nh);
                } else {
                    s.setPlayerMonHp(nh);
                }
                s.addLog(target.displayName().getString() + " took " + r.damage() + " damage!");
            }
        }
        if (s.bothWildsFainted()) {
            s.setPhase(BattleSession.Phase.WON);
            s.addLog("Both wild Pokémon fainted!");
            removeWild(player, s);
            applyExp(player, s, BattleCalc.expForWin(s.playerMon().level(), s.wildLevel(), s.wildHandle().stage()) * 2);
        } else if (s.playerMon().hp() <= 0
                && (s.playerMon2() == null || s.playerMon2().hp() <= 0)) {
            s.addLog("All active Pokémon fainted…");
            autoSwitchOrBlackout(player, s);
        }
        persistPlayer(player, s);
        if (s.playerMon2() != null && s.partySlot2() >= 0) {
            PlayerParty party = PartyHelper.get(player).copy();
            if (s.partySlot2() < party.size()) {
                party.set(s.partySlot2(), s.playerMon2());
                PartyHelper.set(player, party);
            }
        }
    }

    /**
     * B3 — start a double battle with two nearby wilds and two healthy party mons.
     * @return true if doubles started
     */
    public static boolean tryStartDoubles(ServerPlayer player, WildMonEntity wildA, WildMonEntity wildB) {
        if (wildA == null || wildB == null || wildA == wildB) {
            return false;
        }
        if (wildA.isCompanion() || wildB.isCompanion() || wildA.isRemoved() || wildB.isRemoved()) {
            return false;
        }
        if (!inEngageRange(player, wildA) || player.distanceToSqr(wildB) > MAX_ENGAGE_DISTANCE_SQR * 4) {
            return false;
        }
        if (inBattle(player)) {
            player.sendSystemMessage(Component.translatable("message.cobblemon.already_battling"));
            return false;
        }
        PlayerParty party = PartyHelper.get(player);
        int slot1 = -1, slot2 = -1;
        OwnedMon mon1 = null, mon2 = null;
        for (int i = 0; i < party.size(); i++) {
            OwnedMon m = party.get(i).orElse(null);
            if (m == null || m.isFainted()) {
                continue;
            }
            if (slot1 < 0) {
                slot1 = i;
                mon1 = m;
            } else if (slot2 < 0) {
                slot2 = i;
                mon2 = m;
                break;
            }
        }
        if (mon1 == null || mon2 == null) {
            player.sendSystemMessage(Component.literal("§cNeed 2 healthy Pokémon for a double battle."));
            return false;
        }
        BattleSession session = new BattleSession(
                player.getUUID(), wildA.getUUID(), wildA.getSpeciesId(), wildA.getMonLevel(), slot1, mon1
        );
        session.enableDoubles(wildB.getUUID(), wildB.getSpeciesId(), wildB.getMonLevel(), slot2, mon2);
        SESSIONS.put(player.getUUID(), session);
        PartyHelper.markSeen(player, wildA.getSpeciesId());
        PartyHelper.markSeen(player, wildB.getSpeciesId());
        try {
            BattlePresentation.setupField(player, wildA, mon1, session);
            wildB.getNavigation().stop();
            wildB.setNoAi(true);
        } catch (Throwable ignored) {
            wildA.setNoAi(true);
            wildB.setNoAi(true);
        }
        try {
            java.util.List<String> wMoves = wildMoves(session).stream().map(BattleMove::getId).toList();
            String battleId = com.cobblemon.mod.battle.graal.ShowdownTurnProtocol.tryStartWildProtocolFromOwned(
                    mon1, wildA.getSpeciesId(), wildA.getMonLevel(), wMoves
            );
            if (battleId != null) {
                session.setShowdownBattleId(battleId);
            }
        } catch (Throwable ignored) {
        }
        player.sendSystemMessage(Component.literal("§bDouble battle! §7Two foes at once."));
        sync(player, session);
        return true;
    }

    /** True if player acts before wild this turn. */
    private static boolean goesFirst(BattleSession s, BattleMove playerMove) {
        List<BattleMove> foeMoves = wildMoves(s);
        BattleMove enemyMove = foeMoves.isEmpty() ? BattleMove.of(MonMove.TACKLE) : foeMoves.get(0);
        int pPri = playerMove.getPriority();
        int ePri = enemyMove.getPriority();
        if (pPri != ePri) {
            return pPri > ePri;
        }
        OwnedMon wildProxy = OwnedMon.createWild(s.wildSpeciesId(), s.wildLevel(), net.minecraft.util.RandomSource.create())
                .withHp(s.wildHp())
                .withStatus(s.wildStatus());
        int pSpe = BattleCalc.speedStat(s.playerMon(), s.playerStages());
        int eSpe = BattleCalc.speedStat(wildProxy, s.wildStages());
        return pSpe >= eSpe;
    }

    private static List<BattleMove> wildMoves(BattleSession s) {
        return DatapackLearnsets.battleMovesKnownAtLevel(s.wildSpeciesId(), s.wildLevel());
    }

    public static void handleSwitch(ServerPlayer player, int partySlot) {
        BattleSession s = SESSIONS.get(player.getUUID());
        if (s == null || s.phase() != BattleSession.Phase.PLAYER_TURN) {
            return;
        }
        PlayerParty party = PartyHelper.get(player);
        if (partySlot < 0 || partySlot >= party.size()) {
            s.addLog("Can't switch there!");
            sync(player, s);
            return;
        }
        if (partySlot == s.partySlot()) {
            s.addLog("Already in battle!");
            sync(player, s);
            return;
        }
        OwnedMon next = party.get(partySlot).orElse(null);
        if (next == null || next.isFainted()) {
            s.addLog("That mon can't battle!");
            sync(player, s);
            return;
        }
        // Persist current mon HP first
        persistPlayer(player, s);
        s.setPlayerMon(next, partySlot);
        s.addLog("Go! " + next.displayName().getString() + "!");
        try {
            BattlePresentation.switchCompanion(player, s, next);
        } catch (Throwable ignored) {
        }
        // Switching loses the turn → enemy acts
        s.setPhase(BattleSession.Phase.ENEMY_TURN);
        runEnemyTurn(player, s);
        if (s.isOver()) {
            finishIfNeeded(player, s);
            return;
        }
        s.setPhase(BattleSession.Phase.PLAYER_TURN);
        sync(player, s);
    }

    public static void handleRun(ServerPlayer player) {
        BattleSession s = SESSIONS.get(player.getUUID());
        if (s == null || s.isOver()) {
            return;
        }
        // Escape chance based on levels
        float chance = 0.5f + (s.playerMon().level() - s.wildLevel()) * 0.05f;
        chance = Math.max(0.25f, Math.min(0.95f, chance));
        if (player.level().getRandom().nextFloat() <= chance) {
            s.addLog("Got away safely!");
            s.setPhase(BattleSession.Phase.RAN);
            finishIfNeeded(player, s);
        } else {
            s.addLog("Couldn't escape!");
            s.setPhase(BattleSession.Phase.ENEMY_TURN);
            runEnemyTurn(player, s);
            if (s.isOver()) {
                finishIfNeeded(player, s);
            } else {
                s.setPhase(BattleSession.Phase.PLAYER_TURN);
                sync(player, s);
            }
        }
    }

    public static void handleCatch(ServerPlayer player, CubeBallTier tier) {
        BattleSession s = SESSIONS.get(player.getUUID());
        if (s == null || s.phase() != BattleSession.Phase.PLAYER_TURN) {
            return;
        }
        if (PartyHelper.get(player).isFull()) {
            s.addLog("Party is full — can't catch!");
            sync(player, s);
            return;
        }

        float hpRatio = s.wildHpRatio();
        boolean guaranteed = GUARANTEED_CATCH.remove(player.getUUID());
        com.cobblemon.mod.species.MonStatus wildStatus = s.wildStatus();
        boolean alreadyCaught = PartyHelper.getDex(player).hasCaught(s.wildSpeciesId());
        BallContext ballCtx = BallContext.ofWild(
                player, s.wildSpeciesId(), s.wildLevel(), hpRatio, s.turnCount(), alreadyCaught
        );
        com.cobblemon.mod.item.CatchCalc.Result roll = guaranteed
                ? com.cobblemon.mod.item.CatchCalc.Result.guaranteed()
                : com.cobblemon.mod.item.CatchCalc.roll(
                        hpRatio, s.wildSpeciesId(), tier, wildStatus, player.level().getRandom(), ballCtx);

        float effMult = ballCtx.multiplierFor(tier);
        s.addLog("Threw a " + tier.englishName()
                + (guaranteed ? " (Master!)" : " (×" + String.format("%.1f", effMult)
                + (roll.caught() ? "" : " · broke after " + roll.shakesSucceeded() + " shake(s)") + ")")
                + "!");
        if (roll.caught()) {
            // Prefer wild entity identity so shiny/form/IVs from spawn are preserved
            OwnedMon caught = null;
            if (player.level() instanceof ServerLevel level) {
                Entity e = level.getEntity(s.wildEntityId());
                if (e instanceof WildMonEntity wild && !wild.isRemoved()) {
                    caught = wild.toOwnedMon();
                }
            }
            if (caught == null) {
                caught = OwnedMon.createWild(s.wildSpeciesId(), s.wildLevel(), player.level().getRandom());
            }
            int hp = Math.max(1, (int) (caught.maxHp() * hpRatio));
            caught = caught.withHp(hp);
            caught = CatchCalc.applyCatchEffects(caught, tier);
            if (PartyHelper.addMon(player, caught)) {
                String shinyNote = caught.isShiny() ? " §e★Shiny!§r" : "";
                s.addLog("Gotcha! " + caught.displayName().getString() + " was caught!"
                        + shinyNote
                        + (guaranteed ? "" : " (" + roll.shakesSucceeded() + " shakes)"));
                s.setPhase(BattleSession.Phase.CAUGHT);
                removeWild(player, s);
                applyExp(player, s, BattleCalc.expForWin(s.playerMon().level(), s.wildLevel(), s.wildHandle().stage()) / 2);
                player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 1.2f);
                finishIfNeeded(player, s);
                return;
            }
        }
        s.addLog("Oh no! It broke free after " + roll.shakesSucceeded() + " shake(s)!");
        s.setPhase(BattleSession.Phase.ENEMY_TURN);
        runEnemyTurn(player, s);
        if (s.isOver()) {
            finishIfNeeded(player, s);
        } else {
            s.setPhase(BattleSession.Phase.PLAYER_TURN);
            sync(player, s);
        }
    }

    private static void runPlayerMove(ServerPlayer player, BattleSession s, BattleMove move, int moveIndex) {
        OwnedMon mon = s.playerMon();
        int slot = moveIndex;
        if (slot < 0) {
            slot = mon.moveIds().indexOf(move.getId());
        }
        if (slot >= 0 && !mon.hasPp(slot)) {
            s.addLog(move.englishName() + " has no PP left!");
            persistPlayer(player, s);
            return;
        }
        if (slot >= 0) {
            mon = mon.consumePp(slot);
            s.updatePlayerMon(mon);
        }
        s.setLastPlayerMoveName(move.englishName());
        s.addLog(mon.displayName().getString() + " used " + move.englishName() + "!");

        boolean focused = s.playerFocus();
        s.setPlayerFocus(false);

        if (mon.status().skipTurnChance() > 0f
                && player.level().getRandom().nextFloat() < mon.status().skipTurnChance()) {
            s.addLog(mon.displayName().getString() + " is " + mon.status().english() + " and can't move!");
            // Still tick residuals after a skipped turn
            applyEndTurnResidual(s);
            persistPlayer(player, s);
            return;
        }

        OwnedMon wildMon = OwnedMon.createWild(s.wildSpeciesId(), s.wildLevel(), player.level().getRandom())
                .withHp(s.wildHp())
                .withStatus(s.wildStatus());
        BattleCalc.DamageResult result = BattleCalc.useMove(
                move, mon, wildMon, s.playerStages(), s.wildStages(), player.level().getRandom(), focused, s.field()
        );

        if (result.missed()) {
            s.addLog("But it missed!");
            persistPlayer(player, s);
            return;
        }
        if (result.immune()) {
            s.addLog("It had no effect…");
            persistPlayer(player, s);
            return;
        }

        applyMoveResult(player, s, result, true, player.level().getRandom(), slot);

        // Life Orb recoil
        mon = s.playerMon();
        float recoil = com.cobblemon.mod.species.HeldItems.lifeOrbRecoil(mon.heldItem());
        if (recoil > 0f && result.damage() > 0) {
            int self = Math.max(1, Math.round(mon.maxHp() * recoil));
            s.setPlayerMonHp(Math.max(0, mon.hp() - self));
            s.addLog(mon.displayName().getString() + " is hurt by Life Orb!");
        }

        if (s.wildHp() <= 0) {
            s.addLog("Wild " + s.wildDisplayName() + " fainted!");
            s.setPhase(BattleSession.Phase.WON);
            removeWild(player, s);
            applyExp(player, s, BattleCalc.expForWin(s.playerMon().level(), s.wildLevel(), s.wildHandle().stage()));
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 1.0f);
        } else {
            applyEndTurnResidual(s);
            if (s.wildHp() <= 0 || s.phase() == BattleSession.Phase.WON) {
                s.setPhase(BattleSession.Phase.WON);
                if (s.wildHp() <= 0) {
                    // residual already logged faint
                }
                removeWild(player, s);
                applyExp(player, s, BattleCalc.expForWin(s.playerMon().level(), s.wildLevel(), s.wildHandle().stage()));
            } else if (s.playerMon().hp() <= 0 && !s.isOver()) {
                // Recoil / residual can faint the player mon after their own attack
                s.addLog(s.playerMon().displayName().getString() + " fainted!");
                persistPlayer(player, s);
                autoSwitchOrBlackout(player, s);
            }
        }
        persistPlayer(player, s);
    }

    private static void runEnemyTurn(ServerPlayer player, BattleSession s) {
        if (s.wildHp() <= 0) {
            return;
        }
        // Skip if wild status locks them
        if (s.wildStatus().skipTurnChance() > 0f
                && player.level().getRandom().nextFloat() < s.wildStatus().skipTurnChance()) {
            s.addLog("Wild " + s.wildDisplayName() + " is " + s.wildStatus().english() + " and can't move!");
            applyEndTurnResidual(s);
            persistPlayer(player, s);
            return;
        }

        s.bumpTurn();
        List<BattleMove> foeMoves = wildMoves(s);
        if (foeMoves.isEmpty()) {
            foeMoves = List.of(BattleMove.of(MonMove.TACKLE));
        }
        BattleMove move = pickEnemyMove(s, foeMoves, player.level().getRandom());
        s.setLastWildMoveName(move.englishName());
        s.addLog("Foe " + s.wildDisplayName() + " used " + move.englishName() + "!");

        boolean focused = s.enemyFocus();
        s.setEnemyFocus(false);

        OwnedMon mon = s.playerMon();
        OwnedMon wildAtk = OwnedMon.createWild(s.wildSpeciesId(), s.wildLevel(), player.level().getRandom())
                .withHp(s.wildHp())
                .withStatus(s.wildStatus());
        BattleCalc.DamageResult result = BattleCalc.useMove(
                move, wildAtk, mon, s.wildStages(), s.playerStages(), player.level().getRandom(), focused, s.field()
        );

        if (result.missed()) {
            s.addLog("It missed!");
            applyEndTurnResidual(s);
            persistPlayer(player, s);
            return;
        }
        if (result.immune()) {
            s.addLog("It had no effect…");
            applyEndTurnResidual(s);
            persistPlayer(player, s);
            return;
        }

        applyMoveResult(player, s, result, false, player.level().getRandom(), -1);
        applyEndTurnResidual(s);

        // Foe can faint to residual burn/poison after their own turn
        if (s.phase() == BattleSession.Phase.WON || s.wildHp() <= 0) {
            if (!s.isOver() || s.phase() == BattleSession.Phase.WON) {
                s.setPhase(BattleSession.Phase.WON);
                s.addLog("Wild " + s.wildDisplayName() + " fainted!");
                removeWild(player, s);
                applyExp(player, s, BattleCalc.expForWin(s.playerMon().level(), s.wildLevel(), s.wildHandle().stage()));
            }
            return;
        }

        int newHp = s.playerMon().hp();
        if (newHp <= 0) {
            s.addLog(mon.displayName().getString() + " fainted!");
            persistPlayer(player, s);
            autoSwitchOrBlackout(player, s);
        } else {
            persistPlayer(player, s);
        }
    }

    /**
     * After the active mon faints: send out the next healthy party member
     * (with a visible world-entity swap), or blackout if none remain.
     */
    private static void autoSwitchOrBlackout(ServerPlayer player, BattleSession s) {
        PlayerParty party = PartyHelper.get(player);
        int next = -1;
        for (int i = 0; i < party.size(); i++) {
            if (i == s.partySlot()) {
                continue;
            }
            OwnedMon m = party.get(i).orElse(null);
            if (m != null && !m.isFainted()) {
                next = i;
                break;
            }
        }
        if (next < 0) {
            s.addLog("All Cobblemon fainted…");
            s.setPhase(BattleSession.Phase.LOST);
            handleBlackout(player, s);
            return;
        }
        OwnedMon nextMon = party.get(next).orElseThrow();
        s.setPlayerMon(nextMon, next);
        s.addLog("Go! " + nextMon.displayName().getString() + "!");
        try {
            BattlePresentation.faintAndSwitch(player, s, nextMon);
        } catch (Throwable t) {
            try {
                BattlePresentation.switchCompanion(player, s, nextMon);
            } catch (Throwable ignored) {
            }
        }
    }

    /**
     * Apply damage, self/target boosts, secondary status, drain heals.
     * @param playerUsed true if the player used the move (attacker = player)
     * @param playerMoveIndex 0-based party move slot when playerUsed, else -1
     */
    private static void applyMoveResult(
            ServerPlayer player,
            BattleSession s,
            BattleCalc.DamageResult result,
            boolean playerUsed,
            net.minecraft.util.RandomSource random,
            int playerMoveIndex
    ) {
        // Self boosts always (Swords Dance etc.)
        if (result.selfBoosts() != null && !result.selfBoosts().isEmpty()) {
            s.applyBoosts(playerUsed, result.selfBoosts(), null);
            // Focus energy-style: treat pure ATK+ as focus for crit help
            if (playerUsed && result.move().isStatus()) {
                for (var b : result.selfBoosts()) {
                    if (b.getKind() == StatKind.ATK && b.getStages() > 0) {
                        s.setPlayerFocus(true);
                        break;
                    }
                }
            }
        }

        int dmg = result.damage();
        if (dmg > 0) {
            if (playerUsed) {
                s.damageWild(dmg);
                if (result.hits() > 1) {
                    s.addLog("Hit " + result.hits() + " times!");
                }
                if (result.critical()) {
                    s.addLog("A critical hit!");
                }
                s.addLog("Dealt " + dmg + " damage!");
            } else {
                int newHp = Math.max(0, s.playerMon().hp() - dmg);
                s.setPlayerMonHp(newHp);
                if (result.hits() > 1) {
                    s.addLog("Hit " + result.hits() + " times!");
                }
                if (result.critical()) {
                    s.addLog("A critical hit!");
                }
                s.addLog(s.playerMon().displayName().getString() + " took " + dmg + " damage!");
            }
            String eff = TypeChart.label(result.typeMult());
            if (!eff.isEmpty()) {
                s.addLog(eff);
            }
        }
        // C3/C4 — type particles + SFX (always; hit reaction included when damaging)
        try {
            var el = result.move() != null ? result.move().getElement() : com.cobblemon.mod.species.MonElement.NORMAL;
            String mid = result.move() != null ? result.move().getId() : "";
            BattlePresentation.playMoveEffects(player, s, playerUsed, mid, el, dmg > 0);
        } catch (Throwable ignored) {
            if (dmg > 0) {
                try {
                    BattlePresentation.playHitReaction(player, s, playerUsed);
                } catch (Throwable ignored2) {
                }
            }
        }

        // Status-move heal (drainHeal == -1 means 25% max)
        if (result.drainHeal() == -1) {
            if (playerUsed) {
                int heal = Math.max(1, s.playerMon().maxHp() / 4);
                s.setPlayerMonHp(Math.min(s.playerMon().maxHp(), s.playerMon().hp() + heal));
                s.addLog(s.playerMon().displayName().getString() + " recovered HP!");
            } else {
                s.healWild(Math.max(1, s.wildMaxHp() / 4));
                s.addLog("Wild " + s.wildDisplayName() + " recovered HP!");
            }
        } else if (result.drainHeal() > 0 && dmg > 0) {
            if (playerUsed) {
                s.setPlayerMonHp(Math.min(s.playerMon().maxHp(), s.playerMon().hp() + result.drainHeal()));
                s.addLog(s.playerMon().displayName().getString() + " drained " + result.drainHeal() + " HP!");
            } else {
                s.healWild(result.drainHeal());
                s.addLog("Wild " + s.wildDisplayName() + " drained HP!");
            }
        }

        // Target boosts (already chance-rolled in BattleCalc)
        if (result.targetBoosts() != null && !result.targetBoosts().isEmpty()) {
            s.applyBoosts(!playerUsed, result.targetBoosts(), null);
        }

        // Primary / secondary status on defender
        if (result.inflictStatus() != null && !result.inflictStatus().isNone()) {
            if (playerUsed) {
                if (s.wildStatus().isNone()) {
                    s.setWildStatus(result.inflictStatus());
                    s.addLog("Wild " + s.wildDisplayName() + " was " + result.inflictStatus().english() + "!");
                }
            } else if (s.playerMon().status().isNone()) {
                // updatePlayerMon — not setPlayerMon (would re-apply entry hazards)
                s.updatePlayerMon(s.playerMon().withStatus(result.inflictStatus()));
                s.addLog(s.playerMon().displayName().getString() + " was " + result.inflictStatus().english() + "!");
            }
        }

        // Field effects (weather / terrain / hazards) from status-style moves
        if (result.move() != null && result.move().isStatus()) {
            String fieldLog = s.field().applyFieldMove(result.move().getId(), playerUsed);
            if (fieldLog != null) {
                s.addLog(fieldLog);
            }
        }
    }

    /**
     * End-of-turn residuals for BOTH sides: burn/poison, leftovers on player,
     * weather/terrain residual, field duration ticks.
     * Called once after a full turn exchange (or after a skipped move).
     */
    private static void applyEndTurnResidual(BattleSession s) {
        // Weather / terrain residual + duration
        try {
            int pField = s.field().residualDamage(true, s.playerMon(), s.wildSpeciesId(), s.wildMaxHp(), s.wildHp());
            if (pField > 0 && s.playerMon() != null && !s.playerMon().isFainted()) {
                s.setPlayerMonHp(Math.max(0, s.playerMon().hp() - pField));
                s.addLog(s.playerMon().displayName().getString() + " is buffeted by the weather!");
            } else if (pField < 0 && s.playerMon() != null && !s.playerMon().isFainted()) {
                s.setPlayerMonHp(Math.min(s.playerMon().maxHp(), s.playerMon().hp() - pField));
                s.addLog(s.playerMon().displayName().getString() + " restored HP from the terrain!");
            }
            int wField = s.field().residualDamage(false, s.playerMon(), s.wildSpeciesId(), s.wildMaxHp(), s.wildHp());
            if (wField > 0 && s.wildHp() > 0) {
                s.damageWild(wField);
                s.addLog("Wild " + s.wildDisplayName() + " is buffeted by the weather!");
            } else if (wField < 0 && s.wildHp() > 0) {
                s.healWild(-wField);
                s.addLog("Wild " + s.wildDisplayName() + " restored HP from the terrain!");
            }
            for (String line : s.field().endTurnTick()) {
                s.addLog(line);
            }
        } catch (Throwable ignored) {
        }

        // Player side
        OwnedMon mon = s.playerMon();
        if (mon != null && !mon.isFainted()) {
            BattleBerry.Result berry = BattleBerry.tryConsume(mon);
            if (berry.consumed()) {
                s.setPlayerMon(berry.mon(), s.partySlot());
                if (berry.message() != null) {
                    s.addLog(berry.message());
                }
                mon = s.playerMon();
            }
            float hold = com.cobblemon.mod.species.HeldItems.residualHealFraction(mon.heldItem(), mon.primaryType());
            if (hold > 0f) {
                int heal = Math.max(1, Math.round(mon.maxHp() * hold));
                s.setPlayerMonHp(Math.min(mon.maxHp(), mon.hp() + heal));
                s.addLog(mon.displayName().getString() + " restored HP with its held item!");
            } else if (hold < 0f) {
                int dmg = Math.max(1, Math.round(mon.maxHp() * -hold));
                s.setPlayerMonHp(Math.max(0, mon.hp() - dmg));
                s.addLog(mon.displayName().getString() + " is hurt by Black Sludge!");
            }
            float statusFrac = mon.status().residualFraction();
            if (statusFrac > 0f && mon.hp() > 0) {
                int dmg = Math.max(1, Math.round(mon.maxHp() * statusFrac));
                s.setPlayerMonHp(Math.max(0, mon.hp() - dmg));
                s.addLog(mon.displayName().getString() + " is hurt by " + mon.status().english() + "!");
            }
        }

        // Wild side — burn / poison residual
        if (s.wildHp() > 0) {
            float wFrac = s.wildStatus().residualFraction();
            if (wFrac > 0f) {
                int dmg = Math.max(1, Math.round(s.wildMaxHp() * wFrac));
                s.damageWild(dmg);
                s.addLog("Wild " + s.wildDisplayName() + " is hurt by " + s.wildStatus().english() + "!");
                if (s.wildHp() <= 0) {
                    s.addLog("Wild " + s.wildDisplayName() + " fainted!");
                    // Mark win; caller may still need to award exp — set phase if not already over
                    if (!s.isOver()) {
                        s.setPhase(BattleSession.Phase.WON);
                    }
                }
            }
        }
    }

    private static void persistPlayer(ServerPlayer player, BattleSession s) {
        PlayerParty party = PartyHelper.get(player).copy();
        if (s.partySlot() >= 0 && s.partySlot() < party.size()) {
            party.set(s.partySlot(), s.playerMon());
            PartyHelper.set(player, party);
        }
    }

    private static void applyExp(ServerPlayer player, BattleSession s, int amount) {
        persistPlayer(player, s);
        OwnedMon before = s.playerMon();
        int oldLevel = before.level();
        OwnedMon.ExpResult result = before.addExp(amount);
        s.setPlayerMon(result.mon().withHp(result.mon().hp()), s.partySlot());
        // re-read after set
        PlayerParty party = PartyHelper.get(player).copy();
        party.set(s.partySlot(), result.mon());
        // Apply learned moves from level-ups
        OwnedMon mon = result.mon();
        if (result.leveled()) {
            for (int lv = oldLevel + 1; lv <= mon.level(); lv++) {
                MonMove learned = Learnsets.learnAt(mon.speciesId(), lv);
                if (learned != null && !mon.knowsMove(learned)) {
                    mon = mon.learnMove(learned);
                    s.addLog(mon.displayName().getString() + " learned " + learned.englishName() + "!");
                    player.sendSystemMessage(Component.translatable(
                            "message.cobblemon.learned_move",
                            mon.displayName(),
                            learned.displayName()
                    ));
                }
            }
            party.set(s.partySlot(), mon);
            s.setPlayerMon(mon, s.partySlot());
            player.sendSystemMessage(Component.translatable(
                    "message.cobblemon.leveled_up", mon.displayName(), mon.level()
            ));
            s.addLog(mon.displayName().getString() + " grew to Lv." + mon.level() + "!");
        }
        if (result.evolved()) {
            player.sendSystemMessage(Component.translatable(
                    "message.cobblemon.evolved", before.displayName(), mon.displayName()
            ));
            s.addLog(before.displayName().getString() + " evolved into " + mon.displayName().getString() + "!");
            // Refresh moves after evo for new species basics
            mon = mon.withMoves(Learnsets.movesKnownAtLevel(mon.speciesId(), mon.level()));
            party.set(s.partySlot(), mon);
            s.setPlayerMon(mon, s.partySlot());
        }
        // EV yield lite: +1–3 to a stat based on foe primary type
        mon = applyEvYield(mon, s.wildSpeciesId(), player.level().getRandom());
        party.set(s.partySlot(), mon);
        s.setPlayerMon(mon, s.partySlot());
        PartyHelper.set(player, party);
        s.addLog("Gained " + amount + " EXP!");
    }

    /**
     * Blackout (intentional design): soft-heal party to 1 HP each, clear status,
     * restore a little PP — <b>player stays in place</b> (no bed/spawn teleport).
     * See README “Blackout behaviour”.
     */
    private static void handleBlackout(ServerPlayer player, BattleSession s) {
        persistPlayer(player, s);
        try {
            BattlePresentation.teardown(player, s, true);
        } catch (Throwable ignored) {
        }
        PlayerParty party = PartyHelper.get(player).copy();
        for (int i = 0; i < party.size(); i++) {
            OwnedMon m = party.get(i).orElse(null);
            if (m == null) {
                continue;
            }
            // 1 HP, cured, some PP restored — not free full heal; no world teleport
            OwnedMon next = m.withHp(1).withStatus(com.cobblemon.mod.species.MonStatus.NONE).restorePp(-1, 5);
            party.set(i, next);
        }
        PartyHelper.set(player, party);
        player.sendSystemMessage(Component.translatable("message.cobblemon.blackout"));
        player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_HURT, SoundSource.PLAYERS, 0.8f, 0.6f);
    }

    /**
     * Smarter wild AI: prefer super-effective attacks, heal when low, status when healthy.
     */
    private static BattleMove pickEnemyMove(BattleSession s, List<BattleMove> moves, net.minecraft.util.RandomSource random) {
        if (moves.size() == 1) {
            return moves.get(0);
        }
        OwnedMon player = s.playerMon();
        float wildHp = s.wildHpRatio();
        BattleMove best = moves.get(0);
        float bestScore = -1f;
        for (BattleMove m : moves) {
            float score = 1f + random.nextFloat() * 0.4f;
            if (m.isStatus()) {
                String mid = ShowdownMoveDex.canonicalize(m.getId());
                if ((mid.contains("rest") || mid.contains("recover") || mid.contains("synthesis")) && wildHp < 0.4f) {
                    score += 4f;
                } else if (wildHp > 0.55f) {
                    score += 1.2f;
                } else {
                    score -= 0.5f;
                }
            } else {
                float mult = TypeChart.multiplier(m.getElement(), player.primaryType());
                if (player.secondaryType().isPresent()) {
                    mult *= TypeChart.multiplier(m.getElement(), player.secondaryType().get());
                }
                score += mult * 2.5f;
                score += m.getPower() / 50f;
                if (m.getPriority() > 0 && player.hpRatio() < 0.25f) {
                    score += 1.5f;
                }
            }
            if (score > bestScore) {
                bestScore = score;
                best = m;
            }
        }
        return best;
    }

    /** Small EV gains after a win so training works without vitamins. */
    private static OwnedMon applyEvYield(OwnedMon mon, String foeSpeciesId, net.minecraft.util.RandomSource random) {
        var type = com.cobblemon.mod.species.SpeciesHandle.of(foeSpeciesId).primaryType();
        int amt = 1 + random.nextInt(3);
        return switch (type) {
            case FIGHTING -> mon.addEvs(0, amt, 0, 0, 0, 0);
            case ROCK, STEEL -> mon.addEvs(0, 0, amt, 0, 0, 0);
            case PSYCHIC, FAIRY -> mon.addEvs(0, 0, 0, amt, 0, 0);
            case GHOST, DARK -> mon.addEvs(0, 0, 0, 0, amt, 0);
            case ELECTRIC, FLYING -> mon.addEvs(0, 0, 0, 0, 0, amt);
            case NORMAL -> mon.addEvs(amt, 0, 0, 0, 0, 0);
            default -> mon.addEvs(0, amt > 1 ? 1 : 0, 0, 0, 0, amt > 2 ? 1 : 0);
        };
    }

    private static void removeWild(ServerPlayer player, BattleSession s) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        Entity e = level.getEntity(s.wildEntityId());
        if (e != null) {
            e.discard();
        }
        if (s.wildEntityId2() != null) {
            Entity e2 = level.getEntity(s.wildEntityId2());
            if (e2 != null) {
                e2.discard();
            }
        }
    }

    private static void finishIfNeeded(ServerPlayer player, BattleSession s) {
        if (s.isOver()) {
            boolean wildAlive = s.phase() != BattleSession.Phase.WON && s.phase() != BattleSession.Phase.CAUGHT;
            try {
                BattlePresentation.teardown(player, s, wildAlive);
            } catch (Throwable ignored) {
            }
            // N3 — award gym badge on win when session carries a reward id
            if (s.phase() == BattleSession.Phase.WON
                    && s.rewardBadgeId() != null
                    && !s.rewardBadgeId().isBlank()) {
                try {
                    if (com.cobblemon.mod.party.PlayerBadges.award(player, s.rewardBadgeId())) {
                        player.sendSystemMessage(Component.translatable(
                                "message.cobblemon.badge_earned",
                                com.cobblemon.mod.party.PlayerBadges.displayName(s.rewardBadgeId())
                        ));
                        player.level().playSound(null, player.blockPosition(),
                                SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.9f, 1.0f);
                    }
                } catch (Throwable ignored) {
                }
            }
        }
        sync(player, s);
        if (s.isOver()) {
            // B4 — sync PvP HP to opponent
            try {
                PvpChallengeManager.onBattleEnd(player, s);
            } catch (Throwable ignored) {
            }
            // Close Showdown stream if open
            try {
                if (s.showdownBattleId() != null) {
                    com.cobblemon.mod.battle.graal.ShowdownSim.endBattle(s.showdownBattleId());
                }
            } catch (Throwable ignored) {
            }
            SESSIONS.remove(player.getUUID());
            PvpChallengeManager.clear(player.getUUID());
        }
    }

    public static void sync(ServerPlayer player, BattleSession s) {
        if (!s.isOver()) {
            try {
                BattlePresentation.holdField(player, s);
            } catch (Throwable ignored) {
            }
        }
        List<String> moveIds = s.playerMon().moveIds();
        PacketDistributor.sendToPlayer(player, new BattleUpdatePayload(
                s.phase().name(),
                s.playerMon().speciesId(),
                s.playerMon().level(),
                s.playerMon().hp(),
                s.playerMon().maxHp(),
                s.playerMon().displayName().getString(),
                s.wildSpeciesId(),
                s.wildLevel(),
                s.wildHp(),
                s.wildMaxHp(),
                moveIds,
                s.recentLog(16),
                s.isOver(),
                s.lastPlayerMoveName(),
                s.lastWildMoveName()
        ));
    }
}

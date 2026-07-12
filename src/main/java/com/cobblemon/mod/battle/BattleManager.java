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
        // Soft-freeze wild: zero speed while battling
        wild.getNavigation().stop();
        PartyHelper.markSeen(player, wild.getSpeciesId());
        sync(player, session);
        return true;
    }

    public static void end(ServerPlayer player, BattleSession.Phase endPhase) {
        BattleSession s = SESSIONS.get(player.getUUID());
        if (s == null) {
            return;
        }
        s.setPhase(endPhase);
        sync(player, s);
        SESSIONS.remove(player.getUUID());
    }

    public static void forceClear(UUID playerId) {
        SESSIONS.remove(playerId);
        GUARANTEED_CATCH.remove(playerId);
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
        List<MonMove> moves = s.playerMon().moves();
        if (moveIndex < 0 || moveIndex >= moves.size()) {
            return;
        }
        MonMove move = moves.get(moveIndex);
        // Speed / priority: if enemy is faster, they may strike first when player didn't use priority
        boolean playerFirst = goesFirst(s, move);
        if (playerFirst) {
            runPlayerMove(player, s, move);
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
            // Only use move if player mon still up
            if (s.playerMon().hp() > 0) {
                runPlayerMove(player, s, move);
            }
        }
        if (s.isOver()) {
            finishIfNeeded(player, s);
            return;
        }
        s.setPhase(BattleSession.Phase.PLAYER_TURN);
        sync(player, s);
    }

    /** True if player acts before wild this turn. */
    private static boolean goesFirst(BattleSession s, MonMove playerMove) {
        List<MonMove> foeMoves = Learnsets.movesKnownAtLevel(s.wildSpeciesId(), s.wildLevel());
        MonMove enemyMove = foeMoves.isEmpty() ? MonMove.TACKLE : foeMoves.get(0);
        int pPri = playerMove.priority();
        int ePri = enemyMove.priority();
        if (pPri != ePri) {
            return pPri > ePri;
        }
        int pSpe = BattleCalc.speedStat(s.playerMon());
        // wild approximate speed
        int eSpe = BattleCalc.speedStat(
                OwnedMon.createWild(s.wildSpeciesId(), s.wildLevel(), net.minecraft.util.RandomSource.create())
                        .withHp(s.wildHp())
        );
        // Paralysis halves speed
        if (s.playerMon().status() == com.cobblemon.mod.species.MonStatus.PARALYSIS) {
            pSpe = Math.max(1, pSpe / 2);
        }
        return pSpe >= eSpe;
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
            OwnedMon caught = OwnedMon.createWild(s.wildSpeciesId(), s.wildLevel(), player.level().getRandom());
            int hp = Math.max(1, (int) (caught.maxHp() * hpRatio));
            caught = caught.withHp(hp);
            caught = CatchCalc.applyCatchEffects(caught, tier);
            if (PartyHelper.addMon(player, caught)) {
                s.addLog("Gotcha! " + caught.displayName().getString() + " was caught!"
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

    private static void runPlayerMove(ServerPlayer player, BattleSession s, MonMove move) {
        OwnedMon mon = s.playerMon();
        // PP check
        int slot = mon.moveIds().indexOf(move.id());
        if (slot >= 0 && !mon.hasPp(slot)) {
            s.addLog(move.englishName() + " has no PP left!");
            persistPlayer(player, s);
            return;
        }
        if (slot >= 0) {
            mon = mon.consumePp(slot);
            s.updatePlayerMon(mon);
        }
        s.addLog(mon.displayName().getString() + " used " + move.englishName() + "!");

        boolean focused = s.playerFocus();
        s.setPlayerFocus(false);

        // Status conditions can skip the turn
        if (mon.status().skipTurnChance() > 0f
                && player.level().getRandom().nextFloat() < mon.status().skipTurnChance()) {
            s.addLog(mon.displayName().getString() + " is " + mon.status().english() + " and can't move!");
            persistPlayer(player, s);
            return;
        }

        OwnedMon wildMon = OwnedMon.createWild(s.wildSpeciesId(), s.wildLevel(), player.level().getRandom())
                .withHp(s.wildHp());
        BattleCalc.DamageResult result = BattleCalc.useMove(
                move, mon, wildMon, player.level().getRandom(), focused
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

        // Pure status moves (no damage) apply and end the action
        if (result.status() != null && result.damage() <= 0 && result.move().isStatus()) {
            applyStatus(s, result, true);
            persistPlayer(player, s);
            return;
        }

        int dmg = result.damage();
        // atk drops / def boosts
        if (s.playerAtkDrop() > 0) {
            dmg = Math.max(1, (int) (dmg * (1f - 0.15f * s.playerAtkDrop())));
        }
        if (s.enemyDefBoost() > 0) {
            dmg = Math.max(1, (int) (dmg * (1f - 0.12f * s.enemyDefBoost())));
        }
        if (dmg > 0) {
            s.damageWild(dmg);
            if (result.unused() > 1) {
                s.addLog("Hit " + result.unused() + " times!");
            }
            if (result.critical()) {
                s.addLog("A critical hit!");
            }
            s.addLog("Dealt " + dmg + " damage!");
            String eff = TypeChart.label(result.typeMult());
            if (!eff.isEmpty()) {
                s.addLog(eff);
            }
        }

        // Secondary status after damage (Showdown-style)
        if (result.status() != null && dmg > 0) {
            applyStatus(s, result, true);
        }

        // Life Orb recoil
        float recoil = com.cobblemon.mod.species.HeldItems.lifeOrbRecoil(mon.heldItem());
        if (recoil > 0f && dmg > 0) {
            int self = Math.max(1, Math.round(mon.maxHp() * recoil));
            s.setPlayerMonHp(Math.max(0, mon.hp() - self));
            s.addLog(mon.displayName().getString() + " is hurt by Life Orb!");
        }

        if (s.wildHp() <= 0) {
            s.addLog("Wild " + s.wildDisplayName() + " fainted!");
            s.setPhase(BattleSession.Phase.WON);
            removeWild(player, s);
            applyExp(player, s, BattleCalc.expForWin(mon.level(), s.wildLevel(), s.wildHandle().stage()));
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 1.0f);
        } else {
            applyEndTurnResidual(s, true);
        }
        persistPlayer(player, s);
    }

    private static void runEnemyTurn(ServerPlayer player, BattleSession s) {
        if (s.wildHp() <= 0) {
            return;
        }
        s.bumpTurn();
        List<MonMove> foeMoves = Learnsets.movesKnownAtLevel(s.wildSpeciesId(), s.wildLevel());
        if (foeMoves.isEmpty()) {
            foeMoves = List.of(MonMove.TACKLE);
        }
        MonMove move = pickEnemyMove(s, foeMoves, player.level().getRandom());
        s.addLog("Wild " + s.wildDisplayName() + " used " + move.englishName() + "!");

        boolean focused = s.enemyFocus();
        s.setEnemyFocus(false);

        OwnedMon mon = s.playerMon();
        OwnedMon wildAtk = OwnedMon.createWild(s.wildSpeciesId(), s.wildLevel(), player.level().getRandom())
                .withHp(s.wildHp());
        BattleCalc.DamageResult result = BattleCalc.useMove(
                move, wildAtk, mon, player.level().getRandom(), focused
        );

        if (result.missed()) {
            s.addLog("It missed!");
            applyEndTurnResidual(s, false);
            return;
        }
        if (result.immune()) {
            s.addLog("It had no effect…");
            applyEndTurnResidual(s, false);
            return;
        }
        if (result.status() != null && result.damage() <= 0 && result.move().isStatus()) {
            applyStatus(s, result, false);
            applyPrimaryStatusFromMove(s, move, false);
            persistPlayer(player, s);
            applyEndTurnResidual(s, false);
            return;
        }

        int dmg = result.damage();
        if (s.enemyAtkDrop() > 0) {
            dmg = Math.max(1, (int) (dmg * (1f - 0.15f * s.enemyAtkDrop())));
        }
        if (s.playerDefBoost() > 0) {
            dmg = Math.max(1, (int) (dmg * (1f - 0.12f * s.playerDefBoost())));
        }
        int newHp = mon.hp();
        if (dmg > 0) {
            newHp = Math.max(0, mon.hp() - dmg);
            s.setPlayerMonHp(newHp);
            if (result.unused() > 1) {
                s.addLog("Hit " + result.unused() + " times!");
            }
            if (result.critical()) {
                s.addLog("A critical hit!");
            }
            s.addLog(mon.displayName().getString() + " took " + dmg + " damage!");
            String eff = TypeChart.label(result.typeMult());
            if (!eff.isEmpty()) {
                s.addLog(eff);
            }
        }
        if (result.status() != null && dmg > 0) {
            applyStatus(s, result, false);
        }

        applyEndTurnResidual(s, false);

        if (newHp <= 0) {
            s.addLog(mon.displayName().getString() + " fainted!");
            persistPlayer(player, s);
            // Try next mon
            PlayerParty party = PartyHelper.get(player);
            int next = -1;
            for (int i = 0; i < party.size(); i++) {
                if (i == s.partySlot()) {
                    continue;
                }
                OwnedMon m = party.get(i).orElseThrow();
                if (!m.isFainted()) {
                    next = i;
                    break;
                }
            }
            if (next < 0) {
                s.addLog("All Cobblemon fainted…");
                s.setPhase(BattleSession.Phase.LOST);
                handleBlackout(player, s);
            } else {
                OwnedMon nextMon = party.get(next).orElseThrow();
                s.setPlayerMon(nextMon, next);
            }
        } else {
            persistPlayer(player, s);
        }
    }

    private static void applyStatus(BattleSession s, BattleCalc.DamageResult result, boolean playerUsed) {
        switch (result.status()) {
            case HEAL_SELF -> {
                if (playerUsed) {
                    int heal = Math.max(1, s.playerMon().maxHp() / 4);
                    s.setPlayerMonHp(Math.min(s.playerMon().maxHp(), s.playerMon().hp() + heal));
                    s.addLog(s.playerMon().displayName().getString() + " recovered HP!");
                } else {
                    s.healWild(Math.max(1, s.wildMaxHp() / 4));
                    s.addLog("Wild mon recovered HP!");
                }
            }
            case DEF_UP -> {
                if (playerUsed) {
                    s.addPlayerDefBoost(1);
                    s.addLog(s.playerMon().displayName().getString() + "'s Defense rose!");
                } else {
                    s.addEnemyDefBoost(1);
                    s.addLog("Foe's Defense rose!");
                }
            }
            case FOCUS -> {
                if (playerUsed) {
                    s.setPlayerFocus(true);
                    s.addLog(s.playerMon().displayName().getString() + " is focusing!");
                } else {
                    s.setEnemyFocus(true);
                    s.addLog("Foe is focusing!");
                }
            }
            case ATK_DOWN -> {
                if (playerUsed) {
                    s.addEnemyAtkDrop(1);
                    s.addLog("Foe's Attack fell!");
                } else {
                    s.addPlayerAtkDrop(1);
                    s.addLog(s.playerMon().displayName().getString() + "'s Attack fell!");
                }
            }
            case BURN, POISON, PARALYZE, SLEEP, FREEZE -> {
                // Primary status only applies to the defender of the move
                com.cobblemon.mod.species.MonStatus st = switch (result.status()) {
                    case BURN -> com.cobblemon.mod.species.MonStatus.BURN;
                    case POISON -> com.cobblemon.mod.species.MonStatus.POISON;
                    case PARALYZE -> com.cobblemon.mod.species.MonStatus.PARALYSIS;
                    case SLEEP -> com.cobblemon.mod.species.MonStatus.SLEEP;
                    case FREEZE -> com.cobblemon.mod.species.MonStatus.FREEZE;
                    default -> com.cobblemon.mod.species.MonStatus.NONE;
                };
                if (st.isNone()) {
                    break;
                }
                if (playerUsed) {
                    if (s.wildStatus().isNone()) {
                        s.setWildStatus(st);
                        s.addLog("Wild " + s.wildDisplayName() + " was " + st.english() + "!");
                    }
                } else if (s.playerMon().status().isNone()) {
                    s.setPlayerMon(s.playerMon().withStatus(st), s.partySlot());
                    s.addLog(s.playerMon().displayName().getString() + " was " + st.english() + "!");
                }
            }
            case null, default -> {
            }
        }
    }

    /** Leftovers / berries / burn / poison residual on the player mon. */
    private static void applyEndTurnResidual(BattleSession s, boolean afterPlayerMove) {
        OwnedMon mon = s.playerMon();
        if (mon == null || mon.isFainted()) {
            return;
        }
        // Held berry auto-use (pinch / status / leppa)
        BattleBerry.Result berry = BattleBerry.tryConsume(mon);
        if (berry.consumed()) {
            s.setPlayerMon(berry.mon(), s.partySlot());
            if (berry.message() != null) {
                s.addLog(berry.message());
            }
            mon = s.playerMon();
            persistPlayerFromSession(s);
        }
        mon = s.playerMon();
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

    private static void persistPlayerFromSession(BattleSession s) {
        // Soft mark — full persist happens via persistPlayer(player, s) at action ends
    }

    private static void applyPrimaryStatusFromMove(BattleSession s, MonMove move, boolean playerUsed) {
        if (playerUsed || !s.playerMon().status().isNone() || !move.isStatus()) {
            return;
        }
        com.cobblemon.mod.species.MonStatus st = switch (move.element()) {
            case ELECTRIC -> com.cobblemon.mod.species.MonStatus.PARALYSIS;
            case POISON -> com.cobblemon.mod.species.MonStatus.POISON;
            case FIRE -> com.cobblemon.mod.species.MonStatus.BURN;
            case ICE -> com.cobblemon.mod.species.MonStatus.FREEZE;
            case PSYCHIC, GRASS -> com.cobblemon.mod.species.MonStatus.SLEEP;
            default -> com.cobblemon.mod.species.MonStatus.NONE;
        };
        if (!st.isNone()) {
            s.setPlayerMon(s.playerMon().withStatus(st), s.partySlot());
            s.addLog(s.playerMon().displayName().getString() + " was " + st.english() + "!");
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
     * Blackout: soft-heal party to 1 HP each, clear status/PP partially, and warp to
     * bed / world spawn (Pokémon-style "returned to safety").
     */
    private static void handleBlackout(ServerPlayer player, BattleSession s) {
        persistPlayer(player, s);
        PlayerParty party = PartyHelper.get(player).copy();
        for (int i = 0; i < party.size(); i++) {
            OwnedMon m = party.get(i).orElse(null);
            if (m == null) {
                continue;
            }
            // 1 HP, cured, some PP restored — not free full heal
            OwnedMon next = m.withHp(1).withStatus(com.cobblemon.mod.species.MonStatus.NONE).restorePp(-1, 5);
            party.set(i, next);
        }
        PartyHelper.set(player, party);

        // Warp to bed / respawn anchor if set, else world spawn
        try {
            var transition = player.findRespawnPositionAndUseSpawnBlock(
                    false,
                    net.minecraft.world.level.portal.TeleportTransition.DO_NOTHING
            );
            if (transition != null) {
                player.teleport(transition);
                player.sendSystemMessage(Component.literal(
                        "§cBlacked out! §7You scurried back to safety. Your team was restored to 1 HP."));
            } else if (player.level() instanceof ServerLevel sl) {
                var spawn = sl.getRespawnData().pos();
                player.teleportTo(sl, spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5,
                        java.util.Set.of(), player.getYRot(), player.getXRot(), false);
                player.sendSystemMessage(Component.literal(
                        "§cBlacked out! §7You woke up at world spawn. Your team was restored to 1 HP."));
            }
        } catch (Exception ex) {
            if (player.level() instanceof ServerLevel sl) {
                var spawn = sl.getRespawnData().pos();
                player.teleportTo(sl, spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5,
                        java.util.Set.of(), player.getYRot(), player.getXRot(), false);
            }
            player.sendSystemMessage(Component.literal(
                    "§cBlacked out! §7Your team was restored to 1 HP."));
        }
        player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_HURT, SoundSource.PLAYERS, 0.8f, 0.6f);
    }

    /**
     * Smarter wild AI: prefer super-effective attacks, heal when low, status when healthy.
     */
    private static MonMove pickEnemyMove(BattleSession s, List<MonMove> moves, net.minecraft.util.RandomSource random) {
        if (moves.size() == 1) {
            return moves.get(0);
        }
        OwnedMon player = s.playerMon();
        float wildHp = s.wildHpRatio();
        MonMove best = moves.get(0);
        float bestScore = -1f;
        for (MonMove m : moves) {
            float score = 1f + random.nextFloat() * 0.4f;
            if (m.isStatus()) {
                if (m == MonMove.REST_SOFT && wildHp < 0.4f) {
                    score += 4f;
                } else if (wildHp > 0.55f) {
                    score += 1.2f;
                } else {
                    score -= 0.5f;
                }
            } else {
                float mult = TypeChart.multiplier(m.element(), player.primaryType());
                if (player.secondaryType().isPresent()) {
                    mult *= TypeChart.multiplier(m.element(), player.secondaryType().get());
                }
                score += mult * 2.5f;
                score += m.power() / 50f;
                if (m.priority() > 0 && player.hpRatio() < 0.25f) {
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
    }

    private static void finishIfNeeded(ServerPlayer player, BattleSession s) {
        sync(player, s);
        if (s.isOver()) {
            SESSIONS.remove(player.getUUID());
        }
    }

    public static void sync(ServerPlayer player, BattleSession s) {
        List<String> moveIds = s.playerMon().moves().stream().map(MonMove::id).toList();
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
                s.recentLog(8),
                s.isOver()
        ));
    }
}

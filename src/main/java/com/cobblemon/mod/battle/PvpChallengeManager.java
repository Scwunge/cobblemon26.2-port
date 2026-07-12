package com.cobblemon.mod.battle;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.cobblemon.mod.entity.ModEntities;
import com.cobblemon.mod.entity.WildMonEntity;
import com.cobblemon.mod.network.BattleChallengeNotifyPayload;
import com.cobblemon.mod.party.PartyHelper;
import com.cobblemon.mod.party.PlayerParty;
import com.cobblemon.mod.species.OwnedMon;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * B4 — player↔player battle challenges (mirror of trade request flow).
 * <p>
 * On accept, challenger fights a temporary entity carrying the opponent's lead mon.
 * End-of-battle HP is written back to both parties.
 */
public final class PvpChallengeManager {
    private static final double MAX_RANGE = 16.0;
    /** targetUUID → fromUUID */
    private static final Map<UUID, UUID> PENDING = new ConcurrentHashMap<>();
    /** battle player UUID → opponent UUID (for HP sync on end) */
    private static final Map<UUID, UUID> PVP_OPPONENTS = new ConcurrentHashMap<>();
    /** battle player UUID → opponent party slot that was cloned */
    private static final Map<UUID, Integer> PVP_OPPONENT_SLOTS = new ConcurrentHashMap<>();

    private PvpChallengeManager() {}

    public static boolean request(ServerPlayer from, ServerPlayer target) {
        if (from == null || target == null) {
            return false;
        }
        if (from.getUUID().equals(target.getUUID())) {
            from.sendSystemMessage(Component.literal("§cYou can't challenge yourself."));
            return false;
        }
        if (from.distanceTo(target) > MAX_RANGE) {
            from.sendSystemMessage(Component.literal("§cToo far away to challenge."));
            return false;
        }
        if (BattleManager.inBattle(from) || BattleManager.inBattle(target)) {
            from.sendSystemMessage(Component.literal("§cOne of you is already in a battle."));
            return false;
        }
        if (PartyHelper.get(from).isEmpty() || PartyHelper.get(target).isEmpty()) {
            from.sendSystemMessage(Component.literal("§cBoth trainers need a party."));
            return false;
        }
        PENDING.put(target.getUUID(), from.getUUID());
        from.sendSystemMessage(Component.literal(
                "§cBattle challenge sent to §f" + target.getScoreboardName() + "§c!"));
        PacketDistributor.sendToPlayer(target, new BattleChallengeNotifyPayload(
                from.getUUID(), from.getScoreboardName()
        ));
        target.sendSystemMessage(Component.literal(
                "§c" + from.getScoreboardName() + " §7challenged you to a battle!"));
        return true;
    }

    public static boolean accept(ServerPlayer target) {
        UUID fromId = PENDING.remove(target.getUUID());
        if (fromId == null) {
            target.sendSystemMessage(Component.literal("§cNo pending battle challenge."));
            return false;
        }
        var server = target.level().getServer();
        if (server == null) {
            return false;
        }
        ServerPlayer from = server.getPlayerList().getPlayer(fromId);
        if (from == null) {
            target.sendSystemMessage(Component.literal("§cThe challenger left."));
            return false;
        }
        return startPvp(from, target);
    }

    public static void decline(ServerPlayer target) {
        UUID fromId = PENDING.remove(target.getUUID());
        if (fromId == null) {
            target.sendSystemMessage(Component.literal("§cNo pending battle challenge."));
            return;
        }
        target.sendSystemMessage(Component.literal("§7Declined the battle challenge."));
        var server = target.level().getServer();
        if (server != null) {
            ServerPlayer from = server.getPlayerList().getPlayer(fromId);
            if (from != null) {
                from.sendSystemMessage(Component.literal(
                        "§7" + target.getScoreboardName() + " declined your challenge."));
            }
        }
    }

    /**
     * Challenger (from) fights a clone of acceptor's (target) lead mon.
     */
    public static boolean startPvp(ServerPlayer from, ServerPlayer target) {
        if (!(from.level() instanceof ServerLevel level)) {
            return false;
        }
        if (BattleManager.inBattle(from) || BattleManager.inBattle(target)) {
            return false;
        }
        PlayerParty targetParty = PartyHelper.get(target);
        int foeSlot = -1;
        OwnedMon foeMon = null;
        for (int i = 0; i < targetParty.size(); i++) {
            OwnedMon m = targetParty.get(i).orElse(null);
            if (m != null && !m.isFainted()) {
                foeSlot = i;
                foeMon = m;
                break;
            }
        }
        if (foeMon == null) {
            from.sendSystemMessage(Component.literal("§cOpponent has no healthy Pokémon."));
            target.sendSystemMessage(Component.literal("§cYou have no healthy Pokémon to battle with."));
            return false;
        }

        WildMonEntity foe = ModEntities.WILD_MON.get().create(level, EntitySpawnReason.TRIGGERED);
        if (foe == null) {
            return false;
        }
        Vec3 spawn = from.position().add(from.getLookAngle().normalize().scale(2.0));
        foe.setPos(spawn.x, from.getY(), spawn.z);
        foe.applyIdentity(foeMon);
        foe.setCustomName(Component.literal(target.getScoreboardName() + "'s " + foeMon.displayName().getString()));
        foe.setCustomNameVisible(true);
        level.addFreshEntity(foe);

        from.sendSystemMessage(Component.literal(
                "§cPvP vs §f" + target.getScoreboardName() + "§c!"));
        target.sendSystemMessage(Component.literal(
                "§c" + from.getScoreboardName() + " §7is battling your "
                        + foeMon.displayName().getString() + "!"));

        boolean ok = BattleManager.tryStart(from, foe);
        if (!ok) {
            foe.discard();
            return false;
        }
        PVP_OPPONENTS.put(from.getUUID(), target.getUUID());
        PVP_OPPONENT_SLOTS.put(from.getUUID(), foeSlot);
        BattleManager.get(from).ifPresent(s -> s.setPvpOpponentId(target.getUUID()));
        return true;
    }

    /** Called when a battle ends for the challenger — sync HP to opponent's mon. */
    public static void onBattleEnd(ServerPlayer challenger, BattleSession session) {
        if (challenger == null || session == null) {
            return;
        }
        UUID oppId = PVP_OPPONENTS.remove(challenger.getUUID());
        Integer slot = PVP_OPPONENT_SLOTS.remove(challenger.getUUID());
        if (oppId == null || slot == null) {
            // also check session flag
            if (session.pvpOpponentId() != null) {
                oppId = session.pvpOpponentId();
            } else {
                return;
            }
        }
        var server = challenger.level().getServer();
        if (server == null) {
            return;
        }
        ServerPlayer opponent = server.getPlayerList().getPlayer(oppId);
        if (opponent == null) {
            return;
        }
        // Apply remaining wild HP back onto opponent's mon
        PlayerParty party = PartyHelper.get(opponent).copy();
        int s = slot != null ? slot : 0;
        if (s >= 0 && s < party.size()) {
            OwnedMon mon = party.get(s).orElse(null);
            if (mon != null) {
                int newHp = Math.min(mon.maxHp(), Math.max(0, session.wildHp()));
                // Scale if max HP differ slightly
                if (session.wildMaxHp() > 0 && mon.maxHp() != session.wildMaxHp()) {
                    newHp = Math.round(session.wildHp() * (float) mon.maxHp() / session.wildMaxHp());
                    newHp = Math.min(mon.maxHp(), Math.max(0, newHp));
                }
                party.set(s, mon.withHp(newHp).withStatus(session.wildStatus()));
                PartyHelper.set(opponent, party);
            }
        }
        if (session.phase() == BattleSession.Phase.WON) {
            challenger.sendSystemMessage(Component.literal(
                    "§aYou won the battle against §f" + opponent.getScoreboardName() + "§a!"));
            opponent.sendSystemMessage(Component.literal(
                    "§cYou lost the battle against §f" + challenger.getScoreboardName() + "§c."));
        } else if (session.phase() == BattleSession.Phase.LOST) {
            challenger.sendSystemMessage(Component.literal(
                    "§cYou lost the battle against §f" + opponent.getScoreboardName() + "§c."));
            opponent.sendSystemMessage(Component.literal(
                    "§aYou won the battle against §f" + challenger.getScoreboardName() + "§a!"));
        } else {
            opponent.sendSystemMessage(Component.literal(
                    "§7The battle with " + challenger.getScoreboardName() + " ended."));
        }
    }

    public static void clear(UUID playerId) {
        PENDING.entrySet().removeIf(e -> e.getKey().equals(playerId) || e.getValue().equals(playerId));
        PVP_OPPONENTS.remove(playerId);
        PVP_OPPONENT_SLOTS.remove(playerId);
    }
}

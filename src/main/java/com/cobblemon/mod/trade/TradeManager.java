package com.cobblemon.mod.trade;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.cobblemon.mod.Cobblemon;
import com.cobblemon.mod.network.OpenTradePayload;
import com.cobblemon.mod.party.PartyHelper;
import com.cobblemon.mod.party.PlayerParty;
import com.cobblemon.mod.species.OwnedMon;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Server-side player↔player trade — Cobblemon-style (no Trade Machine block).
 * <p>
 * Flow:
 * <ol>
 *   <li>A requests trade with B (R on player / interact) → pending offer</li>
 *   <li>B accepts → both open Trade GUI</li>
 *   <li>Each selects a party slot and confirms → swap mons</li>
 * </ol>
 * Trade evolutions without a partner use the {@code link_cable} item.
 */
public final class TradeManager {
    private static final double MAX_RANGE = 12.0;
    private static final Map<UUID, Session> SESSIONS = new ConcurrentHashMap<>();
    /** targetUUID → fromUUID pending offers */
    private static final Map<UUID, UUID> PENDING_OFFERS = new ConcurrentHashMap<>();

    private TradeManager() {}

    public record Session(
            UUID a,
            UUID b,
            int slotA,
            int slotB,
            boolean confirmA,
            boolean confirmB
    ) {
        Session withSlotA(int s) {
            return new Session(a, b, s, slotB, false, false);
        }

        Session withSlotB(int s) {
            return new Session(a, b, slotA, s, false, false);
        }

        Session withConfirmA(boolean v) {
            return new Session(a, b, slotA, slotB, v, confirmB);
        }

        Session withConfirmB(boolean v) {
            return new Session(a, b, slotA, slotB, confirmA, v);
        }
    }

    public static Session get(UUID playerId) {
        return SESSIONS.get(playerId);
    }

    public static void cancel(UUID playerId) {
        Session s = SESSIONS.remove(playerId);
        if (s != null) {
            SESSIONS.remove(s.a());
            SESSIONS.remove(s.b());
        }
        PENDING_OFFERS.entrySet().removeIf(e ->
                e.getKey().equals(playerId) || e.getValue().equals(playerId));
    }

    /**
     * Cobblemon-style: send a trade request to {@code target}. They accept/decline.
     */
    public static boolean requestTrade(ServerPlayer from, ServerPlayer target) {
        if (from == null || target == null) {
            return false;
        }
        if (from.getUUID().equals(target.getUUID())) {
            return open(from, from);
        }
        if (from.distanceTo(target) > MAX_RANGE) {
            from.sendSystemMessage(Component.literal("§cToo far away to trade."));
            return false;
        }
        if (SESSIONS.containsKey(from.getUUID()) || SESSIONS.containsKey(target.getUUID())) {
            from.sendSystemMessage(Component.literal("§cOne of you is already trading."));
            return false;
        }
        PENDING_OFFERS.put(target.getUUID(), from.getUUID());
        from.sendSystemMessage(Component.literal(
                "§aTrade request sent to §f" + target.getScoreboardName() + "§a."));
        // Notify target client with accept/decline screen
        PacketDistributor.sendToPlayer(target, new com.cobblemon.mod.network.TradeOfferNotifyPayload(
                from.getUUID(),
                from.getScoreboardName()
        ));
        target.sendSystemMessage(Component.literal(
                "§e" + from.getScoreboardName() + " §7wants to trade. Open the prompt or use §f/tradeaccept§7."));
        return true;
    }

    public static boolean acceptOffer(ServerPlayer target) {
        UUID fromId = PENDING_OFFERS.remove(target.getUUID());
        if (fromId == null) {
            target.sendSystemMessage(Component.literal("§cNo pending trade request."));
            return false;
        }
        var server = target.level().getServer();
        if (server == null) {
            return false;
        }
        ServerPlayer from = server.getPlayerList().getPlayer(fromId);
        if (from == null) {
            target.sendSystemMessage(Component.literal("§cThe other player left."));
            return false;
        }
        return open(from, target);
    }

    public static void declineOffer(ServerPlayer target) {
        UUID fromId = PENDING_OFFERS.remove(target.getUUID());
        if (fromId == null) {
            return;
        }
        target.sendSystemMessage(Component.literal("§7Trade declined."));
        var server = target.level().getServer();
        if (server != null) {
            ServerPlayer from = server.getPlayerList().getPlayer(fromId);
            if (from != null) {
                from.sendSystemMessage(Component.literal(
                        "§c" + target.getScoreboardName() + " declined the trade."));
            }
        }
    }

    /** Start trade UI between two players (or debug self-trade if partner == player). */
    public static boolean open(ServerPlayer player, ServerPlayer partner) {
        if (player == null) {
            return false;
        }
        if (partner == null || partner.getUUID().equals(player.getUUID())) {
            // Debug same-player
            openUi(player, player, true);
            Session s = new Session(player.getUUID(), player.getUUID(), -1, -1, false, false);
            SESSIONS.put(player.getUUID(), s);
            return true;
        }
        if (player.distanceTo(partner) > MAX_RANGE) {
            player.sendSystemMessage(Component.literal("§cPartner is too far away to trade."));
            return false;
        }
        cancel(player.getUUID());
        cancel(partner.getUUID());
        Session s = new Session(player.getUUID(), partner.getUUID(), -1, -1, false, false);
        SESSIONS.put(player.getUUID(), s);
        SESSIONS.put(partner.getUUID(), s);
        openUi(player, partner, false);
        openUi(partner, player, false);
        player.sendSystemMessage(Component.literal("§aTrade opened with §f" + partner.getScoreboardName()));
        partner.sendSystemMessage(Component.literal("§aTrade opened with §f" + player.getScoreboardName()));
        return true;
    }

    public static void selectSlot(ServerPlayer player, int slot) {
        Session s = SESSIONS.get(player.getUUID());
        if (s == null) {
            return;
        }
        PlayerParty party = PartyHelper.get(player);
        if (!party.isValidSlot(slot)) {
            player.sendSystemMessage(Component.literal("§cInvalid party slot."));
            return;
        }
        boolean isA = player.getUUID().equals(s.a());
        Session next = isA ? s.withSlotA(slot) : s.withSlotB(slot);
        SESSIONS.put(s.a(), next);
        SESSIONS.put(s.b(), next);
        player.sendSystemMessage(Component.literal(
                "§7Offering: §f" + party.get(slot).map(m -> m.displayName().getString()).orElse("?")
        ));
    }

    /**
     * Confirm current selection. When both sides confirmed, execute swap.
     */
    public static void confirm(ServerPlayer player) {
        Session s = SESSIONS.get(player.getUUID());
        if (s == null) {
            player.sendSystemMessage(Component.literal("§cNo active trade."));
            return;
        }
        boolean isA = player.getUUID().equals(s.a());
        boolean debug = s.a().equals(s.b());
        Session next = isA ? s.withConfirmA(true) : s.withConfirmB(true);
        // Debug: single player must confirm twice concept → one confirm enough if debug
        if (debug) {
            next = next.withConfirmA(true).withConfirmB(true);
        }
        SESSIONS.put(s.a(), next);
        SESSIONS.put(s.b(), next);

        if (!next.confirmA() || !next.confirmB()) {
            player.sendSystemMessage(Component.literal("§eWaiting for partner to confirm…"));
            return;
        }
        execute(player, next);
    }

    private static void execute(ServerPlayer any, Session s) {
        var server = any.level().getServer();
        if (server == null) {
            return;
        }
        ServerPlayer a = server.getPlayerList().getPlayer(s.a());
        ServerPlayer b = server.getPlayerList().getPlayer(s.b());
        if (a == null || b == null) {
            cancel(s.a());
            return;
        }
        // Debug same-player: swap two of own slots
        if (s.a().equals(s.b())) {
            PlayerParty party = PartyHelper.get(a).copy();
            if (!party.isValidSlot(s.slotA()) || !party.isValidSlot(s.slotB()) || s.slotA() == s.slotB()) {
                a.sendSystemMessage(Component.literal("§cSelect two different party slots to swap (debug trade)."));
                cancel(s.a());
                return;
            }
            party.swap(s.slotA(), s.slotB());
            PartyHelper.set(a, party);
            a.sendSystemMessage(Component.literal("§aDebug trade: swapped party slots "
                    + (s.slotA() + 1) + " ↔ " + (s.slotB() + 1)));
            cancel(s.a());
            return;
        }
        if (s.slotA() < 0 || s.slotB() < 0) {
            a.sendSystemMessage(Component.literal("§cBoth sides must select a mon."));
            b.sendSystemMessage(Component.literal("§cBoth sides must select a mon."));
            return;
        }
        PlayerParty partyA = PartyHelper.get(a).copy();
        PlayerParty partyB = PartyHelper.get(b).copy();
        if (!partyA.isValidSlot(s.slotA()) || !partyB.isValidSlot(s.slotB())) {
            a.sendSystemMessage(Component.literal("§cTrade failed — invalid selection."));
            b.sendSystemMessage(Component.literal("§cTrade failed — invalid selection."));
            cancel(s.a());
            return;
        }
        OwnedMon monA = partyA.get(s.slotA()).orElse(null);
        OwnedMon monB = partyB.get(s.slotB()).orElse(null);
        if (monA == null || monB == null) {
            cancel(s.a());
            return;
        }
        // Apply trade evolutions on both sides (official Cobblemon behavior)
        var evoA = monA.tryTradeEvolve();
        var evoB = monB.tryTradeEvolve();
        OwnedMon monAFinal = evoA.evolved() ? evoA.mon() : monA;
        OwnedMon monBFinal = evoB.evolved() ? evoB.mon() : monB;

        partyA.set(s.slotA(), monBFinal);
        partyB.set(s.slotB(), monAFinal);
        PartyHelper.set(a, partyA);
        PartyHelper.set(b, partyB);
        PartyHelper.markCaught(a, monBFinal.speciesId());
        PartyHelper.markCaught(b, monAFinal.speciesId());
        a.sendSystemMessage(Component.literal(
                "§aTrade complete! You received §f" + monBFinal.displayName().getString()
                        + " §afor §f" + monA.displayName().getString()
                        + (evoB.evolved() ? " §d(it evolved!)" : "")));
        b.sendSystemMessage(Component.literal(
                "§aTrade complete! You received §f" + monAFinal.displayName().getString()
                        + " §afor §f" + monB.displayName().getString()
                        + (evoA.evolved() ? " §d(it evolved!)" : "")));
        Cobblemon.LOGGER.info("Trade {} ↔ {}: {} for {}",
                a.getScoreboardName(), b.getScoreboardName(),
                monA.speciesId(), monB.speciesId());
        cancel(s.a());
    }

    private static void openUi(ServerPlayer self, ServerPlayer partner, boolean debug) {
        PlayerParty selfParty = PartyHelper.get(self);
        PlayerParty partnerParty = PartyHelper.get(partner);
        PacketDistributor.sendToPlayer(self, new OpenTradePayload(
                self.getUUID(),
                self.getScoreboardName(),
                partner.getUUID(),
                partner.getScoreboardName() + (debug ? " (you)" : ""),
                monLabels(selfParty),
                monLabels(partnerParty),
                debug
        ));
    }

    public static java.util.List<String> monLabels(PlayerParty party) {
        java.util.ArrayList<String> out = new java.util.ArrayList<>();
        if (party == null) {
            return out;
        }
        for (int i = 0; i < party.size(); i++) {
            out.add(party.get(i)
                    .map(m -> m.displayName().getString() + " Lv." + m.level())
                    .orElse("—"));
        }
        return out;
    }
}

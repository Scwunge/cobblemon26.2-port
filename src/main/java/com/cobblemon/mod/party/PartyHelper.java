package com.cobblemon.mod.party;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import com.cobblemon.mod.species.OwnedMon;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

/**
 * Server-side helpers for reading/mutating a player's party attachment.
 * Always {@link Player#setData} after mutation so sync + persistence fire correctly.
 */
public final class PartyHelper {
    private PartyHelper() {}

    public static PlayerParty get(Player player) {
        return player.getData(ModAttachments.PARTY);
    }

    public static void set(Player player, PlayerParty party) {
        player.setData(ModAttachments.PARTY, party);
    }

    public static PlayerParty update(Player player, UnaryOperator<PlayerParty> mutator) {
        PlayerParty next = mutator.apply(get(player).copy());
        set(player, next);
        return next;
    }

    public static boolean addMon(ServerPlayer player, OwnedMon mon) {
        PlayerParty party = get(player).copy();
        if (!party.add(mon)) {
            return false;
        }
        set(player, party);
        if (mon != null) {
            markCaught(player, mon.speciesId());
        }
        return true;
    }

    public static PlayerPokedex getDex(Player player) {
        return player.getData(ModAttachments.POKEDEX);
    }

    public static void markSeen(ServerPlayer player, String speciesId) {
        PlayerPokedex dex = getDex(player).copy();
        if (dex.markSeen(speciesId)) {
            player.setData(ModAttachments.POKEDEX, dex);
        }
    }

    public static void markCaught(ServerPlayer player, String speciesId) {
        PlayerPokedex dex = getDex(player).copy();
        if (dex.markCaught(speciesId)) {
            player.setData(ModAttachments.POKEDEX, dex);
            player.sendSystemMessage(Component.literal(
                    "Pokédex: registered " + speciesId + "! (" + dex.caughtCount() + " caught)"
            ));
        }
    }

    public static boolean swap(ServerPlayer player, int a, int b) {
        PlayerParty party = get(player).copy();
        if (!party.swap(a, b)) {
            return false;
        }
        set(player, party);
        return true;
    }

    public static boolean makeLead(ServerPlayer player, int index) {
        PlayerParty party = get(player).copy();
        if (!party.makeLead(index)) {
            return false;
        }
        set(player, party);
        player.sendSystemMessage(Component.translatable("message.cobblemon.lead_set",
                party.lead().map(OwnedMon::displayName).orElse(Component.literal("?"))));
        return true;
    }

    public static Optional<OwnedMon> release(ServerPlayer player, int index) {
        PlayerParty party = get(player).copy();
        Optional<OwnedMon> removed = party.release(index);
        if (removed.isEmpty()) {
            return Optional.empty();
        }
        set(player, party);
        player.sendSystemMessage(Component.translatable("message.cobblemon.released", removed.get().displayName()));
        return removed;
    }

    public static void healAll(ServerPlayer player) {
        PlayerParty party = get(player).copy();
        party.healAll();
        set(player, party);
        player.sendSystemMessage(Component.translatable("message.cobblemon.healed"));
    }

    /**
     * Apply a transform to the lead mon (slot 0). Returns empty if no party / transform no-ops.
     */
    public static Optional<OwnedMon> mapLead(ServerPlayer player, Function<OwnedMon, OwnedMon> fn) {
        PlayerParty party = get(player).copy();
        Optional<OwnedMon> lead = party.lead();
        if (lead.isEmpty()) {
            return Optional.empty();
        }
        OwnedMon before = lead.get();
        OwnedMon after = fn.apply(before);
        if (after == before || after == null) {
            return Optional.empty();
        }
        party.set(0, after);
        set(player, party);
        return Optional.of(after);
    }

    public static boolean hasParty(ServerPlayer player) {
        return !get(player).isEmpty();
    }

    public static PlayerPc getPc(Player player) {
        return player.getData(ModAttachments.PC);
    }

    public static void setPc(Player player, PlayerPc pc) {
        player.setData(ModAttachments.PC, pc);
    }

    /** Move mon from party slot into PC (first empty in box, or any empty). */
    public static boolean depositToPc(ServerPlayer player, int partySlot, int preferredBox) {
        PlayerParty party = get(player).copy();
        PlayerPc pc = getPc(player).copy();
        Optional<OwnedMon> mon = party.release(partySlot);
        if (mon.isEmpty()) {
            return false;
        }
        boolean ok = preferredBox >= 0
                ? pc.depositToBox(preferredBox, mon.get()) || pc.deposit(mon.get())
                : pc.deposit(mon.get());
        if (!ok) {
            // put back
            party.add(mon.get());
            set(player, party);
            return false;
        }
        set(player, party);
        setPc(player, pc);
        return true;
    }

    /** Move mon from PC global index into party. */
    public static boolean withdrawFromPc(ServerPlayer player, int pcGlobalIndex) {
        PlayerParty party = get(player).copy();
        if (party.isFull()) {
            return false;
        }
        PlayerPc pc = getPc(player).copy();
        Optional<OwnedMon> mon = pc.withdraw(pcGlobalIndex);
        if (mon.isEmpty()) {
            return false;
        }
        if (!party.add(mon.get())) {
            pc.setGlobal(pcGlobalIndex, mon.get());
            setPc(player, pc);
            return false;
        }
        set(player, party);
        setPc(player, pc);
        return true;
    }

    /** Grant EXP to a party mon; handles level-up + level evolutions. */
    public static boolean grantExp(ServerPlayer player, int partySlot, int amount) {
        PlayerParty party = get(player).copy();
        Optional<OwnedMon> opt = party.get(partySlot);
        if (opt.isEmpty() || amount <= 0) {
            return false;
        }
        OwnedMon before = opt.get();
        OwnedMon.ExpResult result = before.addExp(amount);
        party.set(partySlot, result.mon());
        set(player, party);

        if (result.leveled()) {
            player.sendSystemMessage(Component.translatable(
                    "message.cobblemon.leveled_up",
                    result.mon().displayName(),
                    result.mon().level()
            ));
        }
        for (var move : result.learnedMoves()) {
            player.sendSystemMessage(Component.translatable(
                    "message.cobblemon.learned_move",
                    result.mon().displayName(),
                    move.displayName()
            ));
        }
        if (result.evolved()) {
            player.sendSystemMessage(Component.translatable(
                    "message.cobblemon.evolved",
                    before.displayName(),
                    result.mon().displayName()
            ));
            player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 0.7f);
        }
        return true;
    }

    /** Replace a party slot mon (after command edits). */
    public static boolean setSlot(ServerPlayer player, int partySlot, OwnedMon mon) {
        PlayerParty party = get(player).copy();
        if (!party.set(partySlot, mon)) {
            return false;
        }
        set(player, party);
        return true;
    }

    public static void clearParty(ServerPlayer player) {
        set(player, PlayerParty.empty());
        player.sendSystemMessage(Component.literal("§7Party cleared."));
    }

    public static void clearPc(ServerPlayer player) {
        setPc(player, PlayerPc.empty());
        player.sendSystemMessage(Component.literal("§7PC cleared."));
    }

    /**
     * Official {@code /levelup} behavior: grant exactly enough EXP to gain one level.
     * @param partySlot 0-based
     */
    public static boolean levelUpOne(ServerPlayer player, int partySlot) {
        PlayerParty party = get(player).copy();
        Optional<OwnedMon> opt = party.get(partySlot);
        if (opt.isEmpty()) {
            return false;
        }
        OwnedMon mon = opt.get();
        if (mon.level() >= OwnedMon.MAX_LEVEL) {
            return false;
        }
        int need = Math.max(1, mon.expToNextLevel() - mon.exp());
        return grantExp(player, partySlot, need);
    }

    /** Force-check level evolution on a party slot (e.g. after manual level set). */
    public static boolean tryLevelEvolve(ServerPlayer player, int partySlot) {
        PlayerParty party = get(player).copy();
        Optional<OwnedMon> opt = party.get(partySlot);
        if (opt.isEmpty()) {
            return false;
        }
        OwnedMon before = opt.get();
        OwnedMon.OptionalEvo evo = before.tryLevelEvolve();
        if (!evo.evolved()) {
            return false;
        }
        party.set(partySlot, evo.mon());
        set(player, party);
        player.sendSystemMessage(Component.translatable(
                "message.cobblemon.evolved",
                before.displayName(),
                evo.into().displayName()
        ));
        player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 0.7f);
        return true;
    }
}

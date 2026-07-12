package com.cobblemon.mod.network;

import com.cobblemon.mod.Cobblemon;
import com.cobblemon.mod.block.entity.PastureBlockEntity;
import com.cobblemon.mod.party.PartyHelper;
import com.cobblemon.mod.party.PlayerParty;
import com.cobblemon.mod.species.OwnedMon;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client → server ranch actions for a pasture block.
 * <ul>
 *   <li>{@link Action#DEPOSIT} — party slot {@code a} → pasture</li>
 *   <li>{@link Action#WITHDRAW} — pasture index {@code a} → party</li>
 *   <li>{@link Action#BREED} — force egg attempt if a pair is compatible</li>
 *   <li>{@link Action#REFRESH} — re-sync open GUI state</li>
 * </ul>
 */
public record PastureActionPayload(Action action, BlockPos pos, int a) implements CustomPacketPayload {
    public static final Type<PastureActionPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Cobblemon.MOD_ID, "pasture_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PastureActionPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, p -> p.action.ordinal(),
                    ByteBufCodecs.VAR_INT, p -> p.pos.getX(),
                    ByteBufCodecs.VAR_INT, p -> p.pos.getY(),
                    ByteBufCodecs.VAR_INT, p -> p.pos.getZ(),
                    ByteBufCodecs.VAR_INT, PastureActionPayload::a,
                    (ord, x, y, z, a) -> new PastureActionPayload(
                            Action.byOrdinal(ord), new BlockPos(x, y, z), a
                    )
            );

    public enum Action {
        DEPOSIT,
        WITHDRAW,
        BREED,
        REFRESH;

        public static Action byOrdinal(int o) {
            Action[] v = values();
            return o >= 0 && o < v.length ? v[o] : REFRESH;
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PastureActionPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        Level level = player.level();
        BlockPos pos = payload.pos();
        if (pos == null || player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 64.0) {
            return;
        }
        if (!(level.getBlockEntity(pos) instanceof PastureBlockEntity pasture)) {
            player.sendSystemMessage(Component.translatable("message.cobblemon.pasture_missing"));
            return;
        }

        switch (payload.action()) {
            case DEPOSIT -> deposit(player, pasture, pos, payload.a());
            case WITHDRAW -> withdraw(player, pasture, pos, payload.a());
            case BREED -> {
                if (!pasture.hasCompatiblePair()) {
                    player.sendSystemMessage(Component.translatable("message.cobblemon.pasture_no_pair"));
                } else if (pasture.forceBreedAttempt(player.getRandom())) {
                    level.playSound(null, pos, SoundEvents.CHICKEN_EGG, SoundSource.BLOCKS, 0.8f, 1.0f);
                    player.sendSystemMessage(Component.translatable("message.cobblemon.pasture_breed_ok"));
                } else {
                    player.sendSystemMessage(Component.translatable("message.cobblemon.pasture_breed_fail"));
                }
            }
            case REFRESH -> {
                // fall through to re-open
            }
        }
        OpenPasturePayload.send(player, pasture);
    }

    private static void deposit(ServerPlayer player, PastureBlockEntity pasture, BlockPos pos, int partySlot) {
        if (pasture.isFull()) {
            player.sendSystemMessage(Component.translatable("message.cobblemon.pasture_full"));
            return;
        }
        PlayerParty party = PartyHelper.get(player).copy();
        if (party.size() <= 1) {
            player.sendSystemMessage(Component.translatable("message.cobblemon.pasture_keep_one"));
            return;
        }
        if (partySlot < 0 || partySlot >= party.size()) {
            return;
        }
        var monOpt = party.release(partySlot);
        if (monOpt.isEmpty()) {
            return;
        }
        OwnedMon mon = monOpt.get();
        if (!pasture.deposit(mon)) {
            party.add(mon);
            PartyHelper.set(player, party);
            player.sendSystemMessage(Component.translatable("message.cobblemon.pasture_full"));
            return;
        }
        PartyHelper.set(player, party);
        player.sendSystemMessage(Component.translatable("message.cobblemon.pasture_deposit", mon.displayName()));
        player.level().playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 0.7f, 1.0f);
    }

    private static void withdraw(ServerPlayer player, PastureBlockEntity pasture, BlockPos pos, int pastureIndex) {
        if (PartyHelper.get(player).isFull()) {
            player.sendSystemMessage(Component.translatable("message.cobblemon.party_full"));
            return;
        }
        var monOpt = pasture.withdraw(pastureIndex);
        if (monOpt.isEmpty()) {
            return;
        }
        OwnedMon mon = monOpt.get();
        if (!PartyHelper.addMon(player, mon)) {
            // Put back if party rejected
            pasture.deposit(mon);
            player.sendSystemMessage(Component.translatable("message.cobblemon.party_full"));
            return;
        }
        player.sendSystemMessage(Component.translatable("message.cobblemon.pasture_withdraw", mon.displayName()));
        player.level().playSound(null, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 0.7f, 1.1f);
    }
}

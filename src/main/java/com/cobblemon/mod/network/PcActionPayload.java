package com.cobblemon.mod.network;

import com.cobblemon.mod.Cobblemon;
import com.cobblemon.mod.party.PartyHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client → server PC transfers.
 * DEPOSIT: partySlot → preferredBox (-1 = any)
 * WITHDRAW: pcGlobalIndex → party
 */
public record PcActionPayload(Action action, int a, int b) implements CustomPacketPayload {
    public static final Type<PcActionPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Cobblemon.MOD_ID, "pc_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PcActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, p -> p.action.ordinal(),
            ByteBufCodecs.VAR_INT, PcActionPayload::a,
            ByteBufCodecs.VAR_INT, PcActionPayload::b,
            (ord, a, b) -> new PcActionPayload(Action.byOrdinal(ord), a, b)
    );

    public enum Action {
        DEPOSIT,
        WITHDRAW;

        public static Action byOrdinal(int o) {
            Action[] v = values();
            return o >= 0 && o < v.length ? v[o] : DEPOSIT;
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PcActionPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        switch (payload.action()) {
            case DEPOSIT -> {
                if (!PartyHelper.depositToPc(player, payload.a(), payload.b())) {
                    player.sendSystemMessage(Component.translatable("message.cobblemon.pc_deposit_fail"));
                } else {
                    player.sendSystemMessage(Component.translatable("message.cobblemon.pc_deposited"));
                }
            }
            case WITHDRAW -> {
                if (!PartyHelper.withdrawFromPc(player, payload.a())) {
                    player.sendSystemMessage(Component.translatable("message.cobblemon.pc_withdraw_fail"));
                } else {
                    player.sendSystemMessage(Component.translatable("message.cobblemon.pc_withdrawn"));
                }
            }
        }
    }
}

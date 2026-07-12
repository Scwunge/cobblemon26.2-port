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
 * Client → server party operations from the party screen / keybind UI.
 */
public record PartyActionPayload(Action action, int slotA, int slotB) implements CustomPacketPayload {
    public static final Type<PartyActionPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Cobblemon.MOD_ID, "party_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PartyActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, p -> p.action.ordinal(),
            ByteBufCodecs.VAR_INT, PartyActionPayload::slotA,
            ByteBufCodecs.VAR_INT, PartyActionPayload::slotB,
            (actionOrdinal, slotA, slotB) -> new PartyActionPayload(Action.byOrdinal(actionOrdinal), slotA, slotB)
    );

    public enum Action {
        SWAP,
        LEAD,
        RELEASE,
        HEAL;

        public static Action byOrdinal(int ordinal) {
            Action[] values = values();
            if (ordinal < 0 || ordinal >= values.length) {
                return HEAL;
            }
            return values[ordinal];
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PartyActionPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        switch (payload.action()) {
            case SWAP -> {
                if (!PartyHelper.swap(player, payload.slotA(), payload.slotB())) {
                    player.sendSystemMessage(Component.translatable("message.cobblemon.party_action_failed"));
                }
            }
            case LEAD -> {
                if (!PartyHelper.makeLead(player, payload.slotA())) {
                    player.sendSystemMessage(Component.translatable("message.cobblemon.party_action_failed"));
                }
            }
            case RELEASE -> {
                if (PartyHelper.release(player, payload.slotA()).isEmpty()) {
                    player.sendSystemMessage(Component.translatable("message.cobblemon.party_action_failed"));
                }
            }
            case HEAL -> PartyHelper.healAll(player);
        }
    }
}

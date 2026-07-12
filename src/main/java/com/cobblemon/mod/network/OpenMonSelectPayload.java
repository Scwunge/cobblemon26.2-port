package com.cobblemon.mod.network;

import com.cobblemon.mod.Cobblemon;
import com.cobblemon.mod.client.ClientHooks;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server → client: open party mon picker for item use. */
public record OpenMonSelectPayload(int hand, String itemId) implements CustomPacketPayload {
    public static final Type<OpenMonSelectPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Cobblemon.MOD_ID, "open_mon_select"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenMonSelectPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, OpenMonSelectPayload::hand,
            ByteBufCodecs.STRING_UTF8, OpenMonSelectPayload::itemId,
            OpenMonSelectPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenMonSelectPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientHooks.openMonSelect(payload.hand(), payload.itemId()));
    }
}

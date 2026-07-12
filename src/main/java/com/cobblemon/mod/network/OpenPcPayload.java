package com.cobblemon.mod.network;

import com.cobblemon.mod.Cobblemon;
import com.cobblemon.mod.client.ClientHooks;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server → client: open the PC UI. */
public record OpenPcPayload() implements CustomPacketPayload {
    public static final Type<OpenPcPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Cobblemon.MOD_ID, "open_pc"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenPcPayload> STREAM_CODEC =
            StreamCodec.unit(new OpenPcPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenPcPayload payload, IPayloadContext context) {
        context.enqueueWork(ClientHooks::openPcScreen);
    }
}

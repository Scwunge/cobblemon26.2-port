package com.cobblemon.mod.network;

import com.cobblemon.mod.Cobblemon;
import com.cobblemon.mod.client.ClientHooks;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server → client: open the starter select UI. */
public record OpenStarterPayload() implements CustomPacketPayload {
    public static final Type<OpenStarterPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Cobblemon.MOD_ID, "open_starter"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenStarterPayload> STREAM_CODEC =
            StreamCodec.unit(new OpenStarterPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenStarterPayload payload, IPayloadContext context) {
        context.enqueueWork(ClientHooks::openStarterScreen);
    }
}

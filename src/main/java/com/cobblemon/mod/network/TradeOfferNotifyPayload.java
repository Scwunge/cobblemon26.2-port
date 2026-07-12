package com.cobblemon.mod.network;

import java.util.UUID;

import com.cobblemon.mod.Cobblemon;
import com.cobblemon.mod.client.ClientHooks;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server → client: someone wants to trade with you. */
public record TradeOfferNotifyPayload(UUID fromId, String fromName) implements CustomPacketPayload {
    public static final Type<TradeOfferNotifyPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Cobblemon.MOD_ID, "trade_offer_notify"));

    public static final StreamCodec<ByteBuf, TradeOfferNotifyPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                UUIDUtil.STREAM_CODEC.encode(buf, p.fromId != null ? p.fromId : new UUID(0, 0));
                ByteBufCodecs.STRING_UTF8.encode(buf, p.fromName == null ? "" : p.fromName);
            },
            buf -> new TradeOfferNotifyPayload(
                    UUIDUtil.STREAM_CODEC.decode(buf),
                    ByteBufCodecs.STRING_UTF8.decode(buf)
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(TradeOfferNotifyPayload payload, IPayloadContext context) {
        context.enqueueWork(() ->
                ClientHooks.openTradeRequestScreen(payload.fromId(), payload.fromName()));
    }
}

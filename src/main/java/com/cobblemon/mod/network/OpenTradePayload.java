package com.cobblemon.mod.network;

import java.util.ArrayList;
import java.util.List;
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

/**
 * Server → client: open {@link com.cobblemon.mod.client.screen.TradeScreen} with two party lists.
 */
public record OpenTradePayload(
        UUID selfId,
        String selfName,
        UUID partnerId,
        String partnerName,
        List<String> selfMons,
        List<String> partnerMons,
        boolean debugSamePlayer
) implements CustomPacketPayload {
    public static final Type<OpenTradePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Cobblemon.MOD_ID, "open_trade"));

    public static final StreamCodec<ByteBuf, OpenTradePayload> STREAM_CODEC = StreamCodec.of(
            OpenTradePayload::encode,
            OpenTradePayload::decode
    );

    private static void encode(ByteBuf buf, OpenTradePayload p) {
        UUIDUtil.STREAM_CODEC.encode(buf, p.selfId != null ? p.selfId : new UUID(0, 0));
        ByteBufCodecs.STRING_UTF8.encode(buf, p.selfName == null ? "" : p.selfName);
        UUIDUtil.STREAM_CODEC.encode(buf, p.partnerId != null ? p.partnerId : new UUID(0, 0));
        ByteBufCodecs.STRING_UTF8.encode(buf, p.partnerName == null ? "" : p.partnerName);
        writeStringList(buf, p.selfMons);
        writeStringList(buf, p.partnerMons);
        buf.writeBoolean(p.debugSamePlayer);
    }

    private static OpenTradePayload decode(ByteBuf buf) {
        UUID selfId = UUIDUtil.STREAM_CODEC.decode(buf);
        String selfName = ByteBufCodecs.STRING_UTF8.decode(buf);
        UUID partnerId = UUIDUtil.STREAM_CODEC.decode(buf);
        String partnerName = ByteBufCodecs.STRING_UTF8.decode(buf);
        List<String> selfMons = readStringList(buf);
        List<String> partnerMons = readStringList(buf);
        boolean debug = buf.readBoolean();
        return new OpenTradePayload(selfId, selfName, partnerId, partnerName, selfMons, partnerMons, debug);
    }

    private static void writeStringList(ByteBuf buf, List<String> list) {
        ByteBufCodecs.VAR_INT.encode(buf, list == null ? 0 : list.size());
        if (list != null) {
            for (String s : list) {
                ByteBufCodecs.STRING_UTF8.encode(buf, s == null ? "" : s);
            }
        }
    }

    private static List<String> readStringList(ByteBuf buf) {
        int n = ByteBufCodecs.VAR_INT.decode(buf);
        List<String> out = new ArrayList<>(Math.max(0, n));
        for (int i = 0; i < n; i++) {
            out.add(ByteBufCodecs.STRING_UTF8.decode(buf));
        }
        return out;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenTradePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientHooks.openTradeScreen(payload));
    }
}

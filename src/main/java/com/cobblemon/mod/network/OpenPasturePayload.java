package com.cobblemon.mod.network;

import java.util.ArrayList;
import java.util.List;

import com.cobblemon.mod.Cobblemon;
import com.cobblemon.mod.block.entity.PastureBlockEntity;
import com.cobblemon.mod.client.ClientHooks;
import com.cobblemon.mod.species.OwnedMon;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server → client: open / refresh the pasture ranch GUI with current stored mons.
 */
public record OpenPasturePayload(
        BlockPos pos,
        int capacity,
        int breedTicksLeft,
        boolean hasCompatiblePair,
        List<OwnedMon> mons
) implements CustomPacketPayload {
    public static final Type<OpenPasturePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Cobblemon.MOD_ID, "open_pasture"));

    public static final StreamCodec<ByteBuf, OpenPasturePayload> STREAM_CODEC = StreamCodec.of(
            OpenPasturePayload::encode,
            OpenPasturePayload::decode
    );

    public static void send(ServerPlayer player, PastureBlockEntity pasture) {
        if (player == null || pasture == null) {
            return;
        }
        PacketDistributor.sendToPlayer(player, new OpenPasturePayload(
                pasture.getBlockPos(),
                PastureBlockEntity.CAPACITY,
                pasture.breedTicksRemaining(),
                pasture.hasCompatiblePair(),
                pasture.mons()
        ));
    }

    private static void encode(ByteBuf buf, OpenPasturePayload p) {
        buf.writeInt(p.pos.getX());
        buf.writeInt(p.pos.getY());
        buf.writeInt(p.pos.getZ());
        ByteBufCodecs.VAR_INT.encode(buf, p.capacity);
        ByteBufCodecs.VAR_INT.encode(buf, p.breedTicksLeft);
        buf.writeBoolean(p.hasCompatiblePair);
        List<OwnedMon> list = p.mons == null ? List.of() : p.mons;
        ByteBufCodecs.VAR_INT.encode(buf, list.size());
        for (OwnedMon mon : list) {
            OwnedMon.STREAM_CODEC.encode(buf, mon);
        }
    }

    private static OpenPasturePayload decode(ByteBuf buf) {
        BlockPos pos = new BlockPos(buf.readInt(), buf.readInt(), buf.readInt());
        int capacity = ByteBufCodecs.VAR_INT.decode(buf);
        int breedLeft = ByteBufCodecs.VAR_INT.decode(buf);
        boolean pair = buf.readBoolean();
        int n = ByteBufCodecs.VAR_INT.decode(buf);
        List<OwnedMon> mons = new ArrayList<>(Math.max(0, n));
        for (int i = 0; i < n; i++) {
            mons.add(OwnedMon.STREAM_CODEC.decode(buf));
        }
        return new OpenPasturePayload(pos, capacity, breedLeft, pair, mons);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenPasturePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientHooks.openPastureScreen(payload));
    }
}

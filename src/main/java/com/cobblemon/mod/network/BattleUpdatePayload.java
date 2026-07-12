package com.cobblemon.mod.network;

import java.util.ArrayList;
import java.util.List;

import com.cobblemon.mod.Cobblemon;
import com.cobblemon.mod.client.ClientBattle;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server → client battle state snapshot.
 */
public record BattleUpdatePayload(
        String phase,
        String playerSpeciesId,
        int playerLevel,
        int playerHp,
        int playerMaxHp,
        String playerName,
        String wildSpeciesId,
        int wildLevel,
        int wildHp,
        int wildMaxHp,
        List<String> moveIds,
        List<String> logLines,
        boolean ended
) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<BattleUpdatePayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Cobblemon.MOD_ID, "battle_update"));

    public static final StreamCodec<ByteBuf, BattleUpdatePayload> STREAM_CODEC = StreamCodec.of(
            BattleUpdatePayload::encode,
            BattleUpdatePayload::decode
    );

    private static void encode(ByteBuf buf, BattleUpdatePayload p) {
        ByteBufCodecs.STRING_UTF8.encode(buf, p.phase);
        ByteBufCodecs.STRING_UTF8.encode(buf, p.playerSpeciesId);
        ByteBufCodecs.VAR_INT.encode(buf, p.playerLevel);
        ByteBufCodecs.VAR_INT.encode(buf, p.playerHp);
        ByteBufCodecs.VAR_INT.encode(buf, p.playerMaxHp);
        ByteBufCodecs.STRING_UTF8.encode(buf, p.playerName);
        ByteBufCodecs.STRING_UTF8.encode(buf, p.wildSpeciesId);
        ByteBufCodecs.VAR_INT.encode(buf, p.wildLevel);
        ByteBufCodecs.VAR_INT.encode(buf, p.wildHp);
        ByteBufCodecs.VAR_INT.encode(buf, p.wildMaxHp);
        writeStringList(buf, p.moveIds);
        writeStringList(buf, p.logLines);
        buf.writeBoolean(p.ended);
    }

    private static BattleUpdatePayload decode(ByteBuf buf) {
        String phase = ByteBufCodecs.STRING_UTF8.decode(buf);
        String playerSpeciesId = ByteBufCodecs.STRING_UTF8.decode(buf);
        int playerLevel = ByteBufCodecs.VAR_INT.decode(buf);
        int playerHp = ByteBufCodecs.VAR_INT.decode(buf);
        int playerMaxHp = ByteBufCodecs.VAR_INT.decode(buf);
        String playerName = ByteBufCodecs.STRING_UTF8.decode(buf);
        String wildSpeciesId = ByteBufCodecs.STRING_UTF8.decode(buf);
        int wildLevel = ByteBufCodecs.VAR_INT.decode(buf);
        int wildHp = ByteBufCodecs.VAR_INT.decode(buf);
        int wildMaxHp = ByteBufCodecs.VAR_INT.decode(buf);
        List<String> moveIds = readStringList(buf);
        List<String> logLines = readStringList(buf);
        boolean ended = buf.readBoolean();
        return new BattleUpdatePayload(
                phase, playerSpeciesId, playerLevel, playerHp, playerMaxHp, playerName,
                wildSpeciesId, wildLevel, wildHp, wildMaxHp, moveIds, logLines, ended
        );
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

    public static void handle(BattleUpdatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientBattle.apply(payload));
    }
}

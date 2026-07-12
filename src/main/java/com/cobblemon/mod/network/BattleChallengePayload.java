package com.cobblemon.mod.network;

import java.util.UUID;

import com.cobblemon.mod.Cobblemon;
import com.cobblemon.mod.battle.PvpChallengeManager;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client → server PvP challenge actions. */
public record BattleChallengePayload(int action, UUID otherId, String otherName) implements CustomPacketPayload {
    public static final Type<BattleChallengePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Cobblemon.MOD_ID, "battle_challenge"));

    public static final StreamCodec<ByteBuf, BattleChallengePayload> STREAM_CODEC = StreamCodec.of(
            BattleChallengePayload::encode,
            BattleChallengePayload::decode
    );

    public static final int ACTION_REQUEST = 1;
    public static final int ACTION_ACCEPT = 2;
    public static final int ACTION_DECLINE = 3;

    private static void encode(ByteBuf buf, BattleChallengePayload p) {
        ByteBufCodecs.VAR_INT.encode(buf, p.action);
        UUIDUtil.STREAM_CODEC.encode(buf, p.otherId != null ? p.otherId : new UUID(0, 0));
        ByteBufCodecs.STRING_UTF8.encode(buf, p.otherName == null ? "" : p.otherName);
    }

    private static BattleChallengePayload decode(ByteBuf buf) {
        return new BattleChallengePayload(
                ByteBufCodecs.VAR_INT.decode(buf),
                UUIDUtil.STREAM_CODEC.decode(buf),
                ByteBufCodecs.STRING_UTF8.decode(buf)
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(BattleChallengePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            switch (payload.action()) {
                case ACTION_REQUEST -> {
                    var server = player.level().getServer();
                    if (server == null) return;
                    ServerPlayer target = server.getPlayerList().getPlayer(payload.otherId());
                    if (target == null) {
                        player.sendSystemMessage(Component.literal("§cPlayer not found."));
                        return;
                    }
                    PvpChallengeManager.request(player, target);
                }
                case ACTION_ACCEPT -> PvpChallengeManager.accept(player);
                case ACTION_DECLINE -> PvpChallengeManager.decline(player);
                default -> {
                }
            }
        });
    }
}

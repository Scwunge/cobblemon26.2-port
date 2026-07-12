package com.cobblemon.mod.network;

import java.util.UUID;

import com.cobblemon.mod.Cobblemon;
import com.cobblemon.mod.trade.TradeManager;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client → server trade actions.
 * <ul>
 *   <li>{@link #ACTION_OPEN_DEBUG} — open trade UI (self or nearest player)</li>
 *   <li>{@link #ACTION_SELECT} — select party slot ({@code selfSlot})</li>
 *   <li>{@link #ACTION_CONFIRM} — lock in selection / complete when both ready</li>
 *   <li>{@link #ACTION_CANCEL} — cancel session</li>
 *   <li>{@link #ACTION_OPEN_WITH} — open with partner UUID in partnerSlot as… use selfSlot unused; partner encoded in partnerSlot? 
 *       Better: partnerSlot unused; we use ACTION_OPEN_DEBUG with server finding nearest</li>
 * </ul>
 */
public record TradeOfferPayload(int action, int selfSlot, int partnerSlot) implements CustomPacketPayload {
    public static final Type<TradeOfferPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Cobblemon.MOD_ID, "trade_offer"));

    public static final StreamCodec<ByteBuf, TradeOfferPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, TradeOfferPayload::action,
            ByteBufCodecs.VAR_INT, TradeOfferPayload::selfSlot,
            ByteBufCodecs.VAR_INT, TradeOfferPayload::partnerSlot,
            TradeOfferPayload::new
    );

    public static final int ACTION_OPEN_DEBUG = 0;
    public static final int ACTION_CONFIRM = 1;
    public static final int ACTION_CANCEL = 2;
    public static final int ACTION_SELECT = 3;

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(TradeOfferPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            switch (payload.action()) {
                case ACTION_OPEN_DEBUG -> openNearestOrSelf(player);
                case ACTION_SELECT -> TradeManager.selectSlot(player, payload.selfSlot());
                case ACTION_CONFIRM -> {
                    // If slots sent with confirm, select first
                    if (payload.selfSlot() >= 0) {
                        TradeManager.selectSlot(player, payload.selfSlot());
                    }
                    if (payload.partnerSlot() >= 0 && TradeManager.get(player.getUUID()) != null
                            && player.getUUID().equals(TradeManager.get(player.getUUID()).a())
                            && player.getUUID().equals(TradeManager.get(player.getUUID()).b())) {
                        // debug: partner slot is the other self slot
                        TradeManager.selectSlot(player, payload.partnerSlot());
                    }
                    TradeManager.confirm(player);
                }
                case ACTION_CANCEL -> {
                    TradeManager.cancel(player.getUUID());
                    player.sendSystemMessage(Component.literal("§7Trade cancelled."));
                }
                default -> {
                }
            }
        });
    }

    private static void openNearestOrSelf(ServerPlayer player) {
        ServerPlayer partner = findNearestPlayer(player, 12.0);
        TradeManager.open(player, partner);
    }

    private static ServerPlayer findNearestPlayer(ServerPlayer player, double range) {
        ServerPlayer best = null;
        double bestD = range * range;
        for (ServerPlayer other : player.level().players()) {
            if (other == player || other.isSpectator()) {
                continue;
            }
            double d = other.distanceToSqr(player);
            if (d < bestD) {
                bestD = d;
                best = other;
            }
        }
        return best;
    }
}

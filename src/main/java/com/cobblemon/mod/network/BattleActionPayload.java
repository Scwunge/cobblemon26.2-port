package com.cobblemon.mod.network;

import com.cobblemon.mod.Cobblemon;
import com.cobblemon.mod.battle.BattleManager;
import com.cobblemon.mod.item.CubeBallTier;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client → server battle command.
 * action: 0=move, 1=run, 2=catch, 3=switch party slot
 */
public record BattleActionPayload(int action, int moveIndex) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<BattleActionPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Cobblemon.MOD_ID, "battle_action"));

    public static final StreamCodec<ByteBuf, BattleActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, BattleActionPayload::action,
            ByteBufCodecs.VAR_INT, BattleActionPayload::moveIndex,
            BattleActionPayload::new
    );

    public static final int ACTION_MOVE = 0;
    public static final int ACTION_RUN = 1;
    public static final int ACTION_CATCH = 2;
    public static final int ACTION_SWITCH = 3;

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(BattleActionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            switch (payload.action()) {
                case ACTION_MOVE -> BattleManager.handleMove(player, payload.moveIndex());
                case ACTION_RUN -> BattleManager.handleRun(player);
                case ACTION_CATCH -> {
                    CubeBallTier tier = findAndConsumeBall(player);
                    if (tier == null) {
                        player.sendSystemMessage(Component.translatable("message.cobblemon.no_ball"));
                        BattleManager.get(player).ifPresent(s -> {
                            s.addLog("No Poké Balls left!");
                            BattleManager.sync(player, s);
                        });
                        return;
                    }
                    BattleManager.handleCatch(player, tier);
                }
                case ACTION_SWITCH -> BattleManager.handleSwitch(player, payload.moveIndex());
                default -> {
                }
            }
        });
    }

    /**
     * Consume the strongest available catch ball in inventory.
     * Uses the exact ball type (Master, Ultra, Great, …) for catch mult + log name.
     */
    private static CubeBallTier findAndConsumeBall(ServerPlayer player) {
        int bestSlot = -1;
        CubeBallTier bestTier = null;
        float bestMul = -1f;

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            CubeBallTier tier = resolveBall(stack);
            if (tier == null) {
                continue;
            }
            // Prefer guaranteed balls immediately
            if (tier.catchMultiplier() >= 100f) {
                bestSlot = i;
                bestTier = tier;
                break;
            }
            if (tier.catchMultiplier() > bestMul) {
                bestMul = tier.catchMultiplier();
                bestTier = tier;
                bestSlot = i;
            }
        }

        if (bestSlot < 0 || bestTier == null) {
            return null;
        }
        ItemStack stack = player.getInventory().getItem(bestSlot);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        if (bestTier.catchMultiplier() >= 100f) {
            BattleManager.markNextCatchGuaranteed(player);
        }
        return bestTier;
    }

    private static CubeBallTier resolveBall(ItemStack stack) {
        Item item = stack.getItem();
        if (item instanceof com.cobblemon.mod.item.CubeBallItem ball) {
            return ball.tier();
        }
        Identifier key = BuiltInRegistries.ITEM.getKey(item);
        if (key == null || !Cobblemon.MOD_ID.equals(key.getNamespace())) {
            return null;
        }
        return CubeBallTier.optional(key.getPath()).orElse(null);
    }
}

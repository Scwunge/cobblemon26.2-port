package com.cobblemon.mod.item;

import com.cobblemon.mod.network.OpenMonSelectPayload;
import com.cobblemon.mod.party.PartyHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Opens the mon-picker UI on the client so the player can choose a party slot.
 */
public final class MonTargetingItem {
    private MonTargetingItem() {}

    public static InteractionResult openPicker(Level level, Player player, InteractionHand hand, ItemStack stack) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer sp)) {
            return InteractionResult.PASS;
        }
        if (!PartyHelper.hasParty(sp)) {
            sp.sendSystemMessage(Component.translatable("message.cobblemon.party_empty"));
            return InteractionResult.FAIL;
        }
        int handId = hand == InteractionHand.OFF_HAND ? 1 : 0;
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        PacketDistributor.sendToPlayer(sp, new OpenMonSelectPayload(handId, id));
        return InteractionResult.SUCCESS;
    }
}

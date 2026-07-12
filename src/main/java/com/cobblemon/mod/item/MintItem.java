package com.cobblemon.mod.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class MintItem extends Item {
    private final String contentId;

    public MintItem(Properties properties, String contentId) {
        super(properties);
        this.contentId = contentId;
    }

    public String contentId() {
        return contentId;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        return MonTargetingItem.openPicker(level, player, hand, player.getItemInHand(hand));
    }
}

package com.cobblemon.mod.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/** Berry: sneak to place crop; otherwise open mon picker (heal/status). */
public class BerryFoodItem extends BlockItem {
    private final String contentId;

    public BerryFoodItem(Block block, Properties properties, String contentId) {
        super(block, properties.food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.2f).alwaysEdible().build()));
        this.contentId = contentId;
    }

    public String contentId() {
        return contentId;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player.isShiftKeyDown()) {
            return super.use(level, player, hand);
        }
        return MonTargetingItem.openPicker(level, player, hand, player.getItemInHand(hand));
    }
}

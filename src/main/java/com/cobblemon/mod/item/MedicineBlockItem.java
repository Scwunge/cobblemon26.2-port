package com.cobblemon.mod.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/** Potion-style: place when sneaking, else open mon picker. */
public class MedicineBlockItem extends BlockItem {
    private final String contentId;

    public MedicineBlockItem(Block block, Properties properties, String contentId) {
        super(block, properties);
        this.contentId = contentId;
    }

    public String contentId() {
        return contentId;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player.isShiftKeyDown()) {
            return InteractionResult.PASS; // allow place via useOn
        }
        return MonTargetingItem.openPicker(level, player, hand, player.getItemInHand(hand));
    }
}

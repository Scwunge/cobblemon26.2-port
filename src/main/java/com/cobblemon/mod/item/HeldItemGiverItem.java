package com.cobblemon.mod.item;

import com.cobblemon.mod.species.HeldItems;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Generic held-item (choice band, leftovers, etc.) — assign via mon picker. */
public class HeldItemGiverItem extends Item {
    private final String contentId;

    public HeldItemGiverItem(Properties properties, String contentId) {
        super(properties);
        this.contentId = contentId;
    }

    public String contentId() {
        return contentId;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!HeldItems.canHold(contentId)) {
            return InteractionResult.PASS;
        }
        return MonTargetingItem.openPicker(level, player, hand, player.getItemInHand(hand));
    }
}

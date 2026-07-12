package com.cobblemon.mod.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/**
 * Inventory item that opens the party screen (handled on the client).
 * Keybind {@code P} also opens the party without this item.
 */
public class PartyBadgeItem extends Item {
    public PartyBadgeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        // Screen open is client-only (see CobblemonClient). Server just acknowledges the use.
        return InteractionResult.SUCCESS;
    }
}

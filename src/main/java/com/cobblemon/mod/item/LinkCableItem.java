package com.cobblemon.mod.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/**
 * Official Cobblemon-style Link Cable — evolves trade-required Pokémon without
 * a second player (not a Trade Machine; Cobblemon has no trade machine block).
 */
public class LinkCableItem extends Item {
    public LinkCableItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        return MonTargetingItem.openPicker(level, player, hand, player.getItemInHand(hand));
    }
}

package com.cobblemon.mod.item;

import com.cobblemon.mod.client.ClientHooks;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/**
 * One of the 7 real Pokédex colours. Right-click opens the Pokédex UI.
 * Animation pose item ids ({@code *_model*}) are not registered as items.
 */
public class PokedexItem extends Item {
    private final String color;

    public PokedexItem(Properties properties, String color) {
        super(properties);
        this.color = color == null ? "red" : color;
    }

    public String color() {
        return color;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            ClientHooks.openPokedexScreen(color);
        }
        player.getCooldowns().addCooldown(player.getItemInHand(hand), 10);
        return InteractionResult.SUCCESS;
    }
}

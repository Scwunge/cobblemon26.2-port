package com.cobblemon.mod.item;

import com.cobblemon.mod.party.PartyHelper;
import com.cobblemon.mod.party.PlayerParty;
import com.cobblemon.mod.species.EvolutionMethod;
import com.cobblemon.mod.species.OwnedMon;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Evolution gems (stones) — evolve the first eligible party mon matching this method.
 * Supports multi-branch species (e.g. Eevee) by matching the gem type.
 */
public class EvolutionGemItem extends Item {
    private final EvolutionMethod method;

    public EvolutionGemItem(Properties properties, EvolutionMethod method) {
        super(properties);
        this.method = method;
    }

    public EvolutionMethod method() {
        return method;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }

        PlayerParty party = PartyHelper.get(serverPlayer).copy();
        for (int i = 0; i < party.size(); i++) {
            OwnedMon mon = party.get(i).orElseThrow();
            OwnedMon.OptionalEvo evo = mon.tryGemEvolve(method);
            if (evo.evolved()) {
                party.set(i, evo.mon());
                PartyHelper.set(serverPlayer, party);
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                serverPlayer.sendSystemMessage(Component.translatable(
                        "message.cobblemon.evolved",
                        mon.displayName(),
                        evo.into().displayName()
                ));
                level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 0.8f);
                return InteractionResult.SUCCESS;
            }
        }

        serverPlayer.sendSystemMessage(Component.translatable(failKey(method)));
        return InteractionResult.FAIL;
    }

    private static String failKey(EvolutionMethod method) {
        return switch (method) {
            case FIRE_GEM -> "message.cobblemon.no_fire_gem_evo";
            case WATER_GEM -> "message.cobblemon.no_water_gem_evo";
            case THUNDER_GEM -> "message.cobblemon.no_thunder_gem_evo";
            case LEAF_GEM -> "message.cobblemon.no_leaf_gem_evo";
            case MOON_GEM -> "message.cobblemon.no_moon_gem_evo";
            default -> "message.cobblemon.no_fire_gem_evo";
        };
    }
}

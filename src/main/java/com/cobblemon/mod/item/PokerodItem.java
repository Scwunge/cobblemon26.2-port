package com.cobblemon.mod.item;

import com.cobblemon.mod.entity.ModEntities;
import com.cobblemon.mod.entity.WildMonEntity;
import com.cobblemon.mod.species.MonElement;
import com.cobblemon.mod.species.MonSpecies;
import com.cobblemon.mod.species.OwnedMon;
import com.cobblemon.mod.species.SpawnEntry;
import com.cobblemon.mod.species.SpawnRules;
import com.cobblemon.mod.species.SpeciesHandle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Cobblemon pokerod — cast into water to fish up a water mon.
 * Higher-tier rods (name contains great/ultra/master/super) improve odds and level.
 */
public class PokerodItem extends Item {
    public PokerodItem(Properties properties) {
        super(properties);
    }

    private static int tierFromStack(ItemStack stack) {
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().toLowerCase();
        if (id.contains("master") || id.contains("origin")) return 4;
        if (id.contains("ultra") || id.contains("super")) return 3;
        if (id.contains("great") || id.contains("good")) return 2;
        if (id.contains("old") || id.contains("bamboo")) return 0;
        return 1;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel server)) {
            return InteractionResult.PASS;
        }

        HitResult hit = player.pick(28.0, 0f, true);
        if (hit.getType() != HitResult.Type.BLOCK) {
            player.sendSystemMessage(Component.literal("§7Cast into water."));
            return InteractionResult.FAIL;
        }
        BlockPos pos = ((BlockHitResult) hit).getBlockPos();
        if (!server.getFluidState(pos).is(Fluids.WATER)
                && !server.getFluidState(pos.above()).is(Fluids.WATER)) {
            player.sendSystemMessage(Component.literal("§7Need a water block."));
            return InteractionResult.FAIL;
        }

        RandomSource random = server.getRandom();
        int tier = tierFromStack(stack);

        SpawnRules.Pick pick = SpawnRules.pick(server, pos, random);
        String speciesId = pick.speciesId();
        SpeciesHandle handle = SpeciesHandle.of(speciesId);

        // Bias strongly to water types / submerged entries
        boolean waterOk = handle.primaryType() == MonElement.WATER
                || handle.secondaryType().orElse(null) == MonElement.WATER
                || pick.position() == SpawnEntry.Position.SUBMERGED
                || pick.position() == SpawnEntry.Position.SURFACE;
        if (!waterOk || random.nextFloat() > 0.35f + tier * 0.1f) {
            MonSpecies[] waterish = {
                    MonSpecies.MAGIKARP, MonSpecies.PSYDUCK, MonSpecies.TENTACOOL,
                    MonSpecies.GOLDEEN, MonSpecies.STARYU, MonSpecies.HORSEA, MonSpecies.SQUIRTLE,
                    MonSpecies.SLOWPOKE, MonSpecies.SHELLDER, MonSpecies.KRABBY, MonSpecies.LAPRAS
            };
            // Higher tier: chance at rarer water mon
            if (tier >= 3 && random.nextFloat() < 0.2f) {
                speciesId = random.nextBoolean() ? "gyarados" : "lapras";
            } else if (tier >= 2 && random.nextFloat() < 0.25f) {
                speciesId = "staryu";
            } else {
                speciesId = waterish[random.nextInt(waterish.length)].id();
            }
        }

        int monLevel = Math.max(3, pick.level() + tier * 2 + random.nextInt(4));
        monLevel = Math.min(70, monLevel);

        // Treasure chance on higher tiers
        if (tier >= 2 && random.nextFloat() < 0.08f + tier * 0.04f) {
            String[] treasure = {
                    "deep_sea_scale", "deep_sea_tooth", "prism_scale", "dragon_scale", "kings_rock",
                    "pearl", "big_pearl", "heart_scale"
            };
            String t = treasure[random.nextInt(treasure.length)];
            var def = com.cobblemon.mod.content.ContentItems.BY_ID.get(t);
            if (def != null) {
                player.addItem(new ItemStack(def.get()));
                player.sendSystemMessage(Component.literal("§bFished up treasure: " + t.replace('_', ' ')));
            }
        }

        WildMonEntity mon = ModEntities.WILD_MON.get().create(server, EntitySpawnReason.EVENT);
        if (mon == null) {
            return InteractionResult.FAIL;
        }
        double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 2;
        double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 2;
        mon.setPos(x, pos.getY() + 0.5, z);
        mon.applyIdentity(OwnedMon.createWild(speciesId, monLevel, random));
        server.addFreshEntity(mon);

        server.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.FISHING_BOBBER_RETRIEVE, SoundSource.PLAYERS, 0.8f, 1.0f);
        player.awardStat(Stats.ITEM_USED.get(this));
        int cd = Math.max(20, 50 - tier * 8);
        player.getCooldowns().addCooldown(stack, cd);
        player.sendSystemMessage(Component.literal(
                "§bA wild " + mon.getDisplayName().getString() + " bit!"));
        return InteractionResult.SUCCESS;
    }
}

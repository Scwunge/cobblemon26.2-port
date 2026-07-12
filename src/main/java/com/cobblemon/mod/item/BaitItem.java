package com.cobblemon.mod.item;

import com.cobblemon.mod.entity.ModEntities;
import com.cobblemon.mod.entity.WildMonEntity;
import com.cobblemon.mod.species.OwnedMon;
import com.cobblemon.mod.species.SpawnRules;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * Poke bait / lure — pulls nearby wilds and can force a spawn if empty.
 */
public class BaitItem extends Item {
    private final String contentId;

    public BaitItem(Properties properties, String contentId) {
        super(properties);
        this.contentId = contentId;
    }

    public String contentId() {
        return contentId;
    }

    private int radius() {
        String id = contentId == null ? "" : contentId.toLowerCase();
        if (id.contains("great") || id.contains("super")) return 48;
        if (id.contains("ultra") || id.contains("max")) return 64;
        return 36;
    }

    private int pullCount() {
        String id = contentId == null ? "" : contentId.toLowerCase();
        if (id.contains("great") || id.contains("super")) return 3;
        if (id.contains("ultra") || id.contains("max")) return 5;
        return 2;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer sp) || !(level instanceof ServerLevel server)) {
            return InteractionResult.PASS;
        }

        int r = radius();
        AABB box = player.getBoundingBox().inflate(r);
        int pulled = 0;
        for (WildMonEntity mon : level.getEntitiesOfClass(WildMonEntity.class, box,
                e -> !e.isCompanion() && e.isAlive())) {
            mon.getNavigation().moveTo(player, 1.35);
            pulled++;
            if (pulled >= pullCount()) {
                break;
            }
        }

        if (pulled > 0) {
            sp.sendSystemMessage(Component.literal(
                    "§e" + pulled + " wild mon(s) drawn by the bait!"));
        } else {
            // Force spawn near player
            BlockPos around = player.blockPosition().offset(
                    sp.getRandom().nextInt(9) - 4,
                    0,
                    sp.getRandom().nextInt(9) - 4
            );
            int surfaceY = server.getHeight(
                    net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    around.getX(), around.getZ()
            );
            BlockPos feet = new BlockPos(around.getX(), surfaceY, around.getZ());
            var pick = SpawnRules.pick(server, feet, server.getRandom());
            WildMonEntity mon = ModEntities.WILD_MON.get().create(server, EntitySpawnReason.EVENT);
            if (mon != null) {
                mon.setPos(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5);
                mon.applyIdentity(OwnedMon.createWild(pick.speciesId(), pick.level(), server.getRandom()));
                server.addFreshEntity(mon);
                mon.getNavigation().moveTo(player, 1.2);
                sp.sendSystemMessage(Component.literal(
                        "§eA wild " + mon.getDisplayName().getString() + " appeared for the bait!"));
            } else {
                sp.sendSystemMessage(Component.literal("§eThe bait stirs the grass…"));
            }
        }

        level.playSound(null, player.blockPosition(), SoundEvents.BONE_MEAL_USE, SoundSource.PLAYERS, 0.8f, 1.1f);
        sp.getCooldowns().addCooldown(stack, 30);
        if (!sp.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResult.SUCCESS;
    }
}

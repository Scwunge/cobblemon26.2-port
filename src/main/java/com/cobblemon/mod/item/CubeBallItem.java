package com.cobblemon.mod.item;

import com.cobblemon.mod.battle.BattleManager;
import com.cobblemon.mod.entity.CubeBallEntity;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileItem;
import net.minecraft.world.level.Level;

/**
 * Throwable Cobblemon Poké Ball (3D cube model in world, flat icon in GUI).
 * In battle, used as catch action instead of throwing a projectile.
 */
public class CubeBallItem extends Item implements ProjectileItem {
    private final CubeBallTier tier;

    public CubeBallItem(Properties properties, CubeBallTier tier) {
        super(properties);
        this.tier = tier != null ? tier : CubeBallTier.POKE;
    }

    public CubeBallTier tier() {
        return tier;
    }

    /** Exact ball item id (e.g. {@code master_ball}). */
    public String ballId() {
        return tier.itemId();
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // In battle: catch attempt via battle manager (don't throw projectile)
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer
                && BattleManager.inBattle(serverPlayer)) {
            BattleManager.handleCatch(serverPlayer, tier);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            player.awardStat(Stats.ITEM_USED.get(this));
            return InteractionResult.SUCCESS;
        }

        // Client: play sound only; server spawns entity
        if (level.isClientSide()) {
            level.playSound(player, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.SNOWBALL_THROW, SoundSource.NEUTRAL,
                    0.75f, 0.55f / (level.getRandom().nextFloat() * 0.4f + 0.8f));
            return InteractionResult.SUCCESS;
        }

        if (level instanceof ServerLevel serverLevel) {
            ItemStack thrown = stack.copyWithCount(1);
            CubeBallEntity ball = new CubeBallEntity(serverLevel, player, thrown, tier);
            ball.placeInFrontOf(player);
            ball.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.5F, 0.4F);
            // Re-assert item after shoot (some projectile paths clear stacks)
            ball.setItem(thrown);
            ball.setTier(tier);
            serverLevel.addFreshEntity(ball);

            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.SNOWBALL_THROW, SoundSource.NEUTRAL,
                    0.75f, 0.55f / (level.getRandom().nextFloat() * 0.4f + 0.8f));
        }

        player.awardStat(Stats.ITEM_USED.get(this));
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public Projectile asProjectile(Level level, Position position, ItemStack itemStack, Direction direction) {
        CubeBallEntity ball = new CubeBallEntity(level, position.x(), position.y(), position.z(), itemStack, tier);
        ball.setItem(itemStack.copyWithCount(1));
        ball.setTier(tier);
        return ball;
    }
}

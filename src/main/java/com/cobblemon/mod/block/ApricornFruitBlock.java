package com.cobblemon.mod.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Hanging apricorn fruit (age 0–3). Grows on random ticks when next to leaves/log;
 * harvest at age 3 drops the fruit item and resets to age 0 (or breaks).
 */
public class ApricornFruitBlock extends Block {
    public static final MapCodec<ApricornFruitBlock> CODEC = simpleCodec(ApricornFruitBlock::new);
    public static final IntegerProperty AGE = IntegerProperty.create("age", 0, 3);
    public static final int MAX_AGE = 3;

    private static final VoxelShape SHAPE = Block.box(4, 2, 4, 12, 14, 12);

    public ApricornFruitBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(AGE, 0));
    }

    @Override
    public MapCodec<? extends ApricornFruitBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return state.getValue(AGE) < MAX_AGE;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(AGE) >= MAX_AGE) {
            return;
        }
        if (level.getRawBrightness(pos, 0) < 8) {
            return;
        }
        if (random.nextFloat() < 0.28f) {
            level.setBlock(pos, state.setValue(AGE, state.getValue(AGE) + 1), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        // Hang under/beside leaves or log (Cobblemon-style canopy fruit)
        for (Direction d : Direction.values()) {
            BlockState n = level.getBlockState(pos.relative(d));
            String path = BuiltInRegistries.BLOCK.getKey(n.getBlock()).getPath();
            if (path.contains("apricorn_leaves") || path.contains("apricorn_log") || path.contains("leaves")) {
                return true;
            }
            if (n.is(net.minecraft.tags.BlockTags.LEAVES) || n.is(net.minecraft.tags.BlockTags.LOGS)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (state.getValue(AGE) < MAX_AGE) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            ItemStack drop = new ItemStack(this.asItem());
            if (!player.getInventory().add(drop)) {
                player.drop(drop, false);
            }
            level.setBlock(pos, state.setValue(AGE, 0), Block.UPDATE_CLIENTS);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void spawnAfterBreak(BlockState state, ServerLevel level, BlockPos pos, ItemStack tool, boolean dropXp) {
        // default loot handles item drops if tables exist
        super.spawnAfterBreak(state, level, pos, tool, dropXp);
    }

    /** Force ripe fruit for worldgen trees. */
    public BlockState ripe() {
        return this.defaultBlockState().setValue(AGE, MAX_AGE);
    }
}

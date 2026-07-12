package com.cobblemon.mod.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Cobblemon mint plant: ages 0–7 (matches official blockstates / mint_stage models).
 */
public class MintCropBlock extends Block implements BonemealableBlock {
    public static final MapCodec<MintCropBlock> CODEC = simpleCodec(MintCropBlock::new);
    public static final IntegerProperty AGE = IntegerProperty.create("age", 0, 7);
    public static final int MAX_AGE = 7;

    private static final VoxelShape[] SHAPES = {
            Block.box(5, 0, 5, 11, 4, 11),
            Block.box(5, 0, 5, 11, 6, 11),
            Block.box(4, 0, 4, 12, 8, 12),
            Block.box(4, 0, 4, 12, 10, 12),
            Block.box(3, 0, 3, 13, 12, 13),
            Block.box(3, 0, 3, 13, 14, 13),
            Block.box(2, 0, 2, 14, 15, 14),
            Block.box(2, 0, 2, 14, 16, 14)
    };

    public MintCropBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(AGE, 0));
    }

    @Override
    public MapCodec<? extends MintCropBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[Math.min(MAX_AGE, state.getValue(AGE))];
    }

    public static boolean isValidSoil(BlockState below) {
        if (below.is(BlockTags.DIRT) || below.is(Blocks.FARMLAND) || below.is(Blocks.GRASS_BLOCK)
                || below.is(Blocks.PODZOL) || below.is(Blocks.MOSS_BLOCK) || below.is(Blocks.ROOTED_DIRT)
                || below.is(Blocks.COARSE_DIRT) || below.is(Blocks.MUD)) {
            return true;
        }
        String name = BuiltInRegistries.BLOCK.getKey(below.getBlock()).getPath();
        return name.contains("mulch") || name.contains("farmland");
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return isValidSoil(level.getBlockState(pos.below()));
    }

    @Override
    protected BlockState updateShape(
            BlockState state,
            LevelReader level,
            ScheduledTickAccess ticks,
            BlockPos pos,
            Direction directionToNeighbour,
            BlockPos neighbourPos,
            BlockState neighbourState,
            RandomSource random
    ) {
        if (!state.canSurvive(level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
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
        if (level.getRawBrightness(pos, 0) < 9) {
            return;
        }
        if (!isValidSoil(level.getBlockState(pos.below()))) {
            level.destroyBlock(pos, true);
            return;
        }
        float chance = 0.28f + BerryCropBlock.mulchBonus(level, pos);
        if (random.nextFloat() < chance) {
            level.setBlock(pos, state.setValue(AGE, state.getValue(AGE) + 1), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return state.getValue(AGE) < MAX_AGE;
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        int age = Math.min(MAX_AGE, state.getValue(AGE) + 1 + random.nextInt(2));
        level.setBlock(pos, state.setValue(AGE, age), Block.UPDATE_CLIENTS);
    }
}

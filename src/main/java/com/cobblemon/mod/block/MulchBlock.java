package com.cobblemon.mod.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Floor mulch that speeds berry growth when under a berry crop. */
public class MulchBlock extends Block {
    public static final MapCodec<MulchBlock> CODEC = simpleCodec(MulchBlock::new);
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 1, 16);

    public MulchBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<? extends MulchBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}

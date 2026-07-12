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
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Hanging apricorn fruit (age 0–3 + horizontal facing).
 * <p>
 * Porter models attach the stem on the <strong>south</strong> face by default;
 * {@code facing} rotates the model so the stem points into adjacent leaves
 * (same as official Cobblemon blockstates).
 */
public class ApricornFruitBlock extends Block {
    public static final MapCodec<ApricornFruitBlock> CODEC = simpleCodec(ApricornFruitBlock::new);
    public static final IntegerProperty AGE = IntegerProperty.create("age", 0, 3);
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final int MAX_AGE = 3;

    private static final VoxelShape SHAPE = Block.box(4, 2, 4, 12, 14, 12);

    public ApricornFruitBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(
                this.stateDefinition.any()
                        .setValue(AGE, 0)
                        .setValue(FACING, Direction.SOUTH)
        );
    }

    @Override
    public MapCodec<? extends ApricornFruitBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE, FACING);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction prefer = context.getClickedFace();
        if (!prefer.getAxis().isHorizontal()) {
            prefer = context.getHorizontalDirection().getOpposite();
        }
        // Stem points toward the clicked block (attachment)
        Direction facing = prefer.getOpposite();
        if (!facing.getAxis().isHorizontal()) {
            facing = Direction.SOUTH;
        }
        BlockState state = defaultBlockState().setValue(FACING, facing);
        if (canSurvive(state, context.getLevel(), context.getClickedPos())) {
            return state;
        }
        // Try each horizontal attachment
        for (Direction d : Direction.Plane.HORIZONTAL) {
            BlockState tryState = defaultBlockState().setValue(FACING, d);
            if (canSurvive(tryState, context.getLevel(), context.getClickedPos())) {
                return tryState;
            }
        }
        return defaultBlockState();
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
        // Prefer attachment in facing direction (stem into leaf/log)
        Direction facing = state.getValue(FACING);
        if (isSupport(level.getBlockState(pos.relative(facing)))) {
            return true;
        }
        // Fallback: any adjacent canopy support
        for (Direction d : Direction.values()) {
            if (isSupport(level.getBlockState(pos.relative(d)))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isSupport(BlockState n) {
        String path = BuiltInRegistries.BLOCK.getKey(n.getBlock()).getPath();
        if (path.contains("apricorn_leaves") || path.contains("apricorn_log") || path.contains("leaves")) {
            return true;
        }
        return n.is(net.minecraft.tags.BlockTags.LEAVES) || n.is(net.minecraft.tags.BlockTags.LOGS);
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
        if (!canSurvive(state, level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return state;
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
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    /** Ripe fruit, stem facing {@code attachment} (direction toward leaf). */
    public BlockState ripe(Direction attachment) {
        Direction facing = attachment.getAxis().isHorizontal() ? attachment : Direction.SOUTH;
        return this.defaultBlockState().setValue(AGE, MAX_AGE).setValue(FACING, facing);
    }

    /** Force ripe fruit for worldgen trees (default south attachment). */
    public BlockState ripe() {
        return ripe(Direction.SOUTH);
    }
}

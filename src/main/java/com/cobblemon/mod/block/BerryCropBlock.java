package com.cobblemon.mod.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Cobblemon-style berry bush: ages 0–3, soil rules, mulch growth bonus, harvest.
 */
public class BerryCropBlock extends Block implements BonemealableBlock {
    public static final MapCodec<BerryCropBlock> CODEC = simpleCodec(BerryCropBlock::new);
    public static final IntegerProperty AGE = IntegerProperty.create("age", 0, 3);
    public static final int MAX_AGE = 3;

    public static final TagKey<Block> BERRY_SOIL = TagKey.create(
            Registries.BLOCK, Identifier.fromNamespaceAndPath("cobblemon", "berry_soil"));
    public static final TagKey<Block> BERRY_WILD_SOIL = TagKey.create(
            Registries.BLOCK, Identifier.fromNamespaceAndPath("cobblemon", "berry_wild_soil"));

    private static final VoxelShape[] SHAPES = {
            Block.box(4, 0, 4, 12, 6, 12),
            Block.box(3, 0, 3, 13, 10, 13),
            Block.box(2, 0, 2, 14, 14, 14),
            Block.box(1, 0, 1, 15, 16, 15)
    };

    public BerryCropBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(AGE, 0));
    }

    @Override
    public MapCodec<? extends BerryCropBlock> codec() {
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
        if (below.is(BERRY_SOIL) || below.is(BERRY_WILD_SOIL)) {
            return true;
        }
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
        float chance = 0.35f + mulchBonus(level, pos);
        if (random.nextFloat() < chance) {
            level.setBlock(pos, state.setValue(AGE, state.getValue(AGE) + 1), Block.UPDATE_CLIENTS);
        }
    }

    public static float mulchBonus(Level level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        Block b = below.getBlock();
        String name = BuiltInRegistries.BLOCK.getKey(b).getPath();
        if (name.contains("peat_mulch") || name.contains("loamy_mulch") || name.contains("rich_mulch")) {
            return 0.35f;
        }
        if (name.contains("growth_mulch") || name.contains("surprise_mulch")) {
            return 0.25f;
        }
        if (name.contains("mulch") || name.contains("farmland") || b == Blocks.FARMLAND) {
            return 0.15f;
        }
        return 0f;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (state.getValue(AGE) < MAX_AGE) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            int count = 2 + level.getRandom().nextInt(2);
            ItemStack drop = new ItemStack(this.asItem(), count);
            popResource(level, pos, drop);
            level.setBlock(pos, state.setValue(AGE, 1), Block.UPDATE_CLIENTS);
        }
        return InteractionResult.SUCCESS;
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

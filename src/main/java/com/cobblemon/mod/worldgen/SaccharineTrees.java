package com.cobblemon.mod.worldgen;

import com.cobblemon.mod.Cobblemon;
import com.cobblemon.mod.content.ContentBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.DeferredBlock;

/** Places a short saccharine tree (log + leaves canopy). */
public final class SaccharineTrees {
    private SaccharineTrees() {}

    private static Block block(String path) {
        DeferredBlock<? extends Block> def = ContentBlocks.BY_ID.get(path);
        if (def != null) {
            return def.get();
        }
        return BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath(Cobblemon.MOD_ID, path));
    }

    public static boolean place(WorldGenLevel level, BlockPos origin, RandomSource random) {
        Block logBlock = block("saccharine_log");
        Block leafBlock = block("saccharine_leaves");
        if (logBlock == Blocks.AIR || leafBlock == Blocks.AIR) {
            return false;
        }
        BlockPos ground = origin.below();
        if (!level.getBlockState(ground).is(net.minecraft.tags.BlockTags.DIRT)
                && !level.getBlockState(ground).is(Blocks.GRASS_BLOCK)
                && !level.getBlockState(ground).is(Blocks.PODZOL)
                && !level.getBlockState(ground).is(Blocks.MOSS_BLOCK)) {
            return false;
        }
        int height = 5 + random.nextInt(3);
        for (int y = 0; y < height; y++) {
            BlockPos p = origin.above(y);
            if (!level.isEmptyBlock(p) && !level.getBlockState(p).canBeReplaced()
                    && !level.getBlockState(p).is(Blocks.SHORT_GRASS)
                    && !level.getBlockState(p).is(Blocks.TALL_GRASS)) {
                if (y == 0) {
                    // allow replacing sapling
                    String path = BuiltInRegistries.BLOCK.getKey(level.getBlockState(p).getBlock()).getPath();
                    if (!path.contains("sapling")) {
                        return false;
                    }
                } else {
                    return false;
                }
            }
        }
        BlockState log = logBlock.defaultBlockState();
        if (log.hasProperty(RotatedPillarBlock.AXIS)) {
            log = log.setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
        }
        BlockState leaf = leafBlock.defaultBlockState();
        if (leaf.hasProperty(LeavesBlock.PERSISTENT)) {
            leaf = leaf.setValue(LeavesBlock.PERSISTENT, true);
        }
        if (leaf.hasProperty(LeavesBlock.DISTANCE)) {
            leaf = leaf.setValue(LeavesBlock.DISTANCE, 1);
        }
        for (int y = 0; y < height; y++) {
            level.setBlock(origin.above(y), log, Block.UPDATE_CLIENTS);
        }
        BlockPos top = origin.above(height - 1);
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = 0; dy <= 2; dy++) {
                    if (Math.abs(dx) == 2 && Math.abs(dz) == 2 && dy != 1) {
                        continue;
                    }
                    if (dx == 0 && dz == 0 && dy < 2) {
                        continue;
                    }
                    BlockPos p = top.offset(dx, dy, dz);
                    if (level.isEmptyBlock(p) || level.getBlockState(p).canBeReplaced()) {
                        level.setBlock(p, leaf, Block.UPDATE_CLIENTS);
                    }
                }
            }
        }
        return true;
    }
}

package com.cobblemon.mod.worldgen;

import com.cobblemon.mod.Cobblemon;
import com.cobblemon.mod.block.ApricornFruitBlock;
import com.cobblemon.mod.content.ContentBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.registries.DeferredBlock;

/**
 * Places Cobblemon-style apricorn trees: short log trunk, leaf canopy, hanging fruit.
 * Shared by sapling growth and worldgen feature.
 */
public final class ApricornTrees {
    public static final String[] COLORS = {
            "black", "blue", "green", "pink", "red", "white", "yellow"
    };

    private ApricornTrees() {}

    public static Block log() {
        return block("apricorn_log");
    }

    public static Block leaves() {
        return block("apricorn_leaves");
    }

    public static Block fruit(String color) {
        return block(color + "_apricorn");
    }

    public static Block sapling(String color) {
        return block(color + "_apricorn_sapling");
    }

    private static Block block(String path) {
        DeferredBlock<? extends Block> def = ContentBlocks.BY_ID.get(path);
        if (def != null) {
            return def.get();
        }
        return BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath(Cobblemon.MOD_ID, path));
    }

    /**
     * Grow a tree at sapling position (replaces sapling).
     * @return true if placed
     */
    public static boolean grow(ServerLevel level, BlockPos saplingPos, String color, RandomSource random, boolean fromSapling) {
        if (!level.getBlockState(saplingPos.below()).is(net.minecraft.tags.BlockTags.DIRT)
                && !level.getBlockState(saplingPos.below()).is(Blocks.GRASS_BLOCK)
                && !level.getBlockState(saplingPos.below()).is(Blocks.PODZOL)
                && !level.getBlockState(saplingPos.below()).is(Blocks.MOSS_BLOCK)
                && !level.getBlockState(saplingPos.below()).is(Blocks.FARMLAND)) {
            return false;
        }
        // Clear sapling first
        if (fromSapling) {
            level.setBlock(saplingPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        return place(level, saplingPos, color, random);
    }

    /**
     * Place tree with base at {@code origin} (block above dirt/grass).
     */
    public static boolean place(WorldGenLevel level, BlockPos origin, String color, RandomSource random) {
        Block logBlock = log();
        Block leafBlock = leaves();
        Block fruitBlock = fruit(color);
        if (logBlock == Blocks.AIR || leafBlock == Blocks.AIR || fruitBlock == Blocks.AIR) {
            Cobblemon.LOGGER.warn("Apricorn tree blocks missing for color {}", color);
            return false;
        }

        // Only on plantable soil — avoids forests of trees on sand/stone/snow surfaces
        BlockPos ground = origin.below();
        if (!isPlantableGround(level.getBlockState(ground))) {
            return false;
        }

        // Need clear space for trunk (5 high) + canopy
        for (int y = 0; y < 6; y++) {
            BlockPos p = origin.above(y);
            if (!level.isEmptyBlock(p) && !level.getBlockState(p).canBeReplaced()) {
                // Allow replacing saplings/grass
                BlockState s = level.getBlockState(p);
                String path = BuiltInRegistries.BLOCK.getKey(s.getBlock()).getPath();
                if (!path.contains("sapling") && !s.is(Blocks.SHORT_GRASS) && !s.is(Blocks.TALL_GRASS)
                        && !s.is(Blocks.FERN) && !s.is(Blocks.SNOW)) {
                    return false;
                }
            }
        }

        // Vertical trunk (axis=y). Blockstates must map axis=y to the upright log model.
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

        BlockState fruitState = fruitBlock.defaultBlockState();
        if (fruitBlock instanceof ApricornFruitBlock fruit) {
            fruitState = fruit.ripe();
        } else if (fruitState.hasProperty(ApricornFruitBlock.AGE)) {
            fruitState = fruitState.setValue(ApricornFruitBlock.AGE, ApricornFruitBlock.MAX_AGE);
        }

        // Trunk height 4–5
        int height = 4 + random.nextInt(2);
        for (int y = 0; y < height; y++) {
            set(level, origin.above(y), log);
        }

        // Canopy around top of trunk
        BlockPos top = origin.above(height - 1);
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = 0; dy <= 2; dy++) {
                    if (Math.abs(dx) == 2 && Math.abs(dz) == 2 && dy == 0) {
                        continue; // cut corners on bottom layer
                    }
                    if (Math.abs(dx) == 2 && Math.abs(dz) == 2 && dy == 2) {
                        continue;
                    }
                    if (dx == 0 && dz == 0 && dy < 2) {
                        continue; // trunk occupies center
                    }
                    BlockPos p = top.offset(dx, dy, dz);
                    if (level.isEmptyBlock(p) || level.getBlockState(p).canBeReplaced()) {
                        set(level, p, leaf);
                    }
                }
            }
        }
        // Cap leaf
        set(level, top.above(2), leaf);

        // Hang 2–5 fruit under outer leaves
        int fruits = 2 + random.nextInt(4);
        int placed = 0;
        int attempts = 0;
        while (placed < fruits && attempts++ < 24) {
            int dx = random.nextInt(5) - 2;
            int dz = random.nextInt(5) - 2;
            if (dx == 0 && dz == 0) {
                continue;
            }
            BlockPos leafPos = top.offset(dx, 1 + random.nextInt(2), dz);
            BlockPos hang = leafPos.below();
            if (level.getBlockState(leafPos).is(leafBlock)
                    && (level.isEmptyBlock(hang) || level.getBlockState(hang).canBeReplaced())) {
                set(level, hang, fruitState);
                placed++;
            }
        }
        // Also attach fruit sideways on canopy edges
        for (Direction d : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
            if (random.nextFloat() < 0.55f) {
                BlockPos side = top.relative(d, 2).above();
                if (level.isEmptyBlock(side) || level.getBlockState(side).canBeReplaced()) {
                    // prefer under a leaf
                    BlockPos underLeaf = top.relative(d).above().below();
                    if (level.isEmptyBlock(underLeaf) || level.getBlockState(underLeaf).canBeReplaced()) {
                        if (level.getBlockState(underLeaf.above()).is(leafBlock)
                                || level.getBlockState(underLeaf.relative(d.getOpposite())).is(leafBlock)) {
                            set(level, underLeaf, fruitState);
                        }
                    }
                }
            }
        }

        return true;
    }

    private static void set(WorldGenLevel level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state, Block.UPDATE_CLIENTS);
    }

    private static boolean isPlantableGround(BlockState state) {
        return state.is(net.minecraft.tags.BlockTags.DIRT)
                || state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.PODZOL)
                || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.ROOTED_DIRT)
                || state.is(Blocks.MOSS_BLOCK)
                || state.is(Blocks.FARMLAND);
    }

    /** Find surface dirt and try to plant a random-color tree. */
    public static boolean tryPlaceAtSurface(WorldGenLevel level, BlockPos col, RandomSource random) {
        int y = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, col.getX(), col.getZ());
        BlockPos surface = new BlockPos(col.getX(), y, col.getZ());
        BlockPos ground = surface.below();
        if (!isPlantableGround(level.getBlockState(ground))) {
            return false;
        }
        if (!level.isEmptyBlock(surface) && !level.getBlockState(surface).canBeReplaced()
                && !level.getBlockState(surface).is(Blocks.SHORT_GRASS)
                && !level.getBlockState(surface).is(Blocks.TALL_GRASS)
                && !level.getBlockState(surface).is(Blocks.SNOW)) {
            return false;
        }
        String color = COLORS[random.nextInt(COLORS.length)];
        return place(level, surface, color, random);
    }
}

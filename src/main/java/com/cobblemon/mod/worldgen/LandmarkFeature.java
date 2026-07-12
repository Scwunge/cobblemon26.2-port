package com.cobblemon.mod.worldgen;

import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * S1 — safe landmark stand-ins for Cobblemon structures (center / tower / dig site).
 * Uses the feature registry only — never the structure set/tag system (avoids unbound freezes).
 */
public class LandmarkFeature extends Feature<NoneFeatureConfiguration> {
    public enum Kind {
        CENTER,
        TOWER,
        DIG_SITE
    }

    private final Kind kind;

    public LandmarkFeature(Codec<NoneFeatureConfiguration> codec, Kind kind) {
        super(codec);
        this.kind = kind;
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        return placeAt(context.level(), context.origin(), context.random(), kind);
    }

    public static boolean placeAt(WorldGenLevel level, BlockPos origin, RandomSource random, Kind kind) {
        BlockPos ground = origin.below();
        if (!level.getBlockState(ground).isSolid()) {
            return false;
        }
        return switch (kind) {
            case CENTER -> placeCenter(level, origin, random);
            case TOWER -> placeTower(level, origin, random);
            case DIG_SITE -> placeDigSite(level, origin, random);
        };
    }

    /** Small white/red "Pokémon Center" pad with heal-machine-looking quartz. */
    private static boolean placeCenter(WorldGenLevel level, BlockPos origin, RandomSource random) {
        // MC 26.2: dyed blocks live on ColorCollection (Blocks.CONCRETE.white(), …)
        BlockState floor = Blocks.CONCRETE.white().defaultBlockState();
        BlockState accent = Blocks.CONCRETE.red().defaultBlockState();
        BlockState pillar = Blocks.QUARTZ_PILLAR.defaultBlockState();
        int r = 3;
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx * dx + dz * dz > r * r + 1) {
                    continue;
                }
                BlockPos p = origin.offset(dx, -1, dz);
                level.setBlock(p, (Math.abs(dx) == r || Math.abs(dz) == r) ? accent : floor, 2);
                clearAbove(level, origin.offset(dx, 0, dz), 3);
            }
        }
        // Center counter
        level.setBlock(origin, pillar, 2);
        level.setBlock(origin.above(), Blocks.STAINED_GLASS.red().defaultBlockState(), 2);
        // Corner posts
        for (int[] c : new int[][]{{-2, -2}, {-2, 2}, {2, -2}, {2, 2}}) {
            level.setBlock(origin.offset(c[0], 0, c[1]), floor, 2);
            level.setBlock(origin.offset(c[0], 1, c[1]), accent, 2);
        }
        // Loot — Cobblemon pokecenter / ruin tables (not vanilla village)
        if (random.nextFloat() < 0.75f) {
            BlockPos chest = origin.offset(1, 0, 0);
            level.setBlock(chest, Blocks.CHEST.defaultBlockState(), 2);
            if (level.getBlockEntity(chest) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity be) {
                be.setLootTable(CobblemonLoot.pickCenter(random));
                be.setLootTableSeed(random.nextLong());
            }
        }
        return true;
    }

    /** 1×1 to 3-high stone brick lookout (tower stub). */
    private static boolean placeTower(WorldGenLevel level, BlockPos origin, RandomSource random) {
        BlockState wall = Blocks.STONE_BRICKS.defaultBlockState();
        BlockState moss = Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
        int h = 4 + random.nextInt(3);
        for (int y = 0; y < h; y++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (Math.abs(dx) != 1 && Math.abs(dz) != 1) {
                        continue; // hollow
                    }
                    if (y > 0 && random.nextFloat() < 0.08f) {
                        continue;
                    }
                    level.setBlock(origin.offset(dx, y, dz), random.nextBoolean() ? wall : moss, 2);
                }
            }
        }
        // Floor + top lantern
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                level.setBlock(origin.offset(dx, -1, dz), Blocks.COBBLESTONE.defaultBlockState(), 2);
            }
        }
        level.setBlock(origin.above(h), Blocks.LANTERN.defaultBlockState(), 2);
        if (random.nextFloat() < 0.55f) {
            BlockPos chest = origin;
            level.setBlock(chest, Blocks.CHEST.defaultBlockState(), 2);
            if (level.getBlockEntity(chest) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity be) {
                be.setLootTable(CobblemonLoot.pickTower(random));
                be.setLootTableSeed(random.nextLong());
            }
        }
        return true;
    }

    /** Sand/gravel dig pit with fossil chance. */
    private static boolean placeDigSite(WorldGenLevel level, BlockPos origin, RandomSource random) {
        BlockState sand = Blocks.SAND.defaultBlockState();
        BlockState gravel = Blocks.GRAVEL.defaultBlockState();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (dx * dx + dz * dz > 6) {
                    continue;
                }
                int depth = 1 + random.nextInt(2);
                for (int y = 0; y < depth; y++) {
                    level.setBlock(origin.offset(dx, -1 - y, dz), random.nextBoolean() ? sand : gravel, 2);
                }
                clearAbove(level, origin.offset(dx, 0, dz), 2);
            }
        }
        // Bone block marker + Cobblemon fossil-site chest loot
        level.setBlock(origin.below(), Blocks.BONE_BLOCK.defaultBlockState(), 2);
        if (random.nextFloat() < 0.7f) {
            BlockPos chest = origin.offset(0, 0, 1);
            level.setBlock(chest, Blocks.CHEST.defaultBlockState(), 2);
            if (level.getBlockEntity(chest) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity be) {
                be.setLootTable(CobblemonLoot.pickDigSite(random));
                be.setLootTableSeed(random.nextLong());
            }
        } else if (random.nextFloat() < 0.5f) {
            level.setBlock(origin.offset(0, 0, 1), Blocks.SUSPICIOUS_SAND.defaultBlockState(), 2);
        }
        return true;
    }

    private static void clearAbove(WorldGenLevel level, BlockPos base, int height) {
        for (int y = 0; y < height; y++) {
            BlockPos p = base.above(y);
            BlockState st = level.getBlockState(p);
            if (st.canBeReplaced() || st.is(Blocks.SHORT_GRASS) || st.is(Blocks.TALL_GRASS)
                    || st.is(Blocks.FERN) || st.is(Blocks.DEAD_BUSH)) {
                level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
            }
        }
    }
}

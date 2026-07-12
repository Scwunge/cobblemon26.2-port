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
 * Safe stand-in for Cobblemon structure ruins: small mossy stone ring + chest chance.
 * Does NOT use the structure registry (avoids unbound structure freezes).
 */
public class MiniRuinFeature extends Feature<NoneFeatureConfiguration> {
    public MiniRuinFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        return placeAt(context.level(), context.origin(), context.random());
    }

    public static boolean placeAt(WorldGenLevel level, BlockPos origin, RandomSource random) {
        BlockPos ground = origin.below();
        if (!level.getBlockState(ground).is(net.minecraft.tags.BlockTags.DIRT)
                && !level.getBlockState(ground).is(Blocks.GRASS_BLOCK)
                && !level.getBlockState(ground).is(Blocks.STONE)
                && !level.getBlockState(ground).is(Blocks.SAND)
                && !level.getBlockState(ground).is(Blocks.GRAVEL)) {
            return false;
        }

        BlockState floor = random.nextBoolean() ? Blocks.MOSSY_COBBLESTONE.defaultBlockState()
                : Blocks.COBBLESTONE.defaultBlockState();
        BlockState wall = Blocks.MOSSY_STONE_BRICKS.defaultBlockState();

        // 5x5 floor
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                BlockPos p = origin.offset(dx, -1, dz);
                level.setBlock(p, floor, 2);
                // clear plants above
                BlockPos a = origin.offset(dx, 0, dz);
                if (level.getBlockState(a).canBeReplaced() || level.getBlockState(a).is(Blocks.SHORT_GRASS)
                        || level.getBlockState(a).is(Blocks.TALL_GRASS)) {
                    level.setBlock(a, Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
        // broken walls
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (Math.abs(dx) != 2 && Math.abs(dz) != 2) {
                    continue;
                }
                if (random.nextFloat() < 0.35f) {
                    continue; // gaps
                }
                int h = 1 + random.nextInt(2);
                for (int y = 0; y < h; y++) {
                    level.setBlock(origin.offset(dx, y, dz), wall, 2);
                }
            }
        }
        // Center loot chest + chance to drop a fossil item into the world
        if (random.nextFloat() < 0.65f) {
            BlockPos chest = origin;
            level.setBlock(chest, Blocks.CHEST.defaultBlockState(), 2);
            if (level.getBlockEntity(chest) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity be) {
                be.setLootTable(net.minecraft.world.level.storage.loot.BuiltInLootTables.SIMPLE_DUNGEON);
                be.setLootTableSeed(random.nextLong());
            }
        }
        // Fossil dig fragment near ruins
        if (random.nextFloat() < 0.35f) {
            String[] fossils = {
                    "helix_fossil", "dome_fossil", "old_amber_fossil", "root_fossil", "claw_fossil",
                    "skull_fossil", "armor_fossil", "cover_fossil", "plume_fossil", "jaw_fossil",
                    "sail_fossil", "fossilized_bird", "fossilized_fish", "fossilized_drake", "fossilized_dino"
            };
            String f = fossils[random.nextInt(fossils.length)];
            var def = com.cobblemon.mod.content.ContentItems.BY_ID.get(f);
            if (def != null && level instanceof net.minecraft.server.level.ServerLevel sl) {
                BlockPos drop = origin.offset(random.nextInt(3) - 1, 1, random.nextInt(3) - 1);
                net.minecraft.world.entity.item.ItemEntity ie = new net.minecraft.world.entity.item.ItemEntity(
                        sl, drop.getX() + 0.5, drop.getY(), drop.getZ() + 0.5,
                        new net.minecraft.world.item.ItemStack(def.get())
                );
                ie.setDefaultPickUpDelay();
                sl.addFreshEntity(ie);
            }
        }

        // Variant: pillar shrine or small tower for visual variety
        if (random.nextFloat() < 0.4f) {
            int pillars = 2 + random.nextInt(3);
            for (int i = 0; i < pillars; i++) {
                int dx = random.nextBoolean() ? -2 : 2;
                int dz = random.nextInt(5) - 2;
                if (random.nextBoolean()) {
                    int t = dx;
                    dx = dz;
                    dz = t;
                }
                int h = 2 + random.nextInt(3);
                for (int y = 0; y < h; y++) {
                    level.setBlock(origin.offset(dx, y, dz),
                            random.nextBoolean() ? Blocks.STONE_BRICKS.defaultBlockState()
                                    : Blocks.MOSSY_STONE_BRICKS.defaultBlockState(), 2);
                }
                if (random.nextFloat() < 0.3f) {
                    level.setBlock(origin.offset(dx, h, dz), Blocks.TORCH.defaultBlockState(), 2);
                }
            }
        }
        // Rare: fossil fragment as armor stand-in (drop gold + bone block marker)
        if (random.nextFloat() < 0.12f) {
            level.setBlock(origin.offset(1, 0, 1), Blocks.BONE_BLOCK.defaultBlockState(), 2);
        }
        return true;
    }
}

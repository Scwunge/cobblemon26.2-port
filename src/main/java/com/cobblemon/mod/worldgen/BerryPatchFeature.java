package com.cobblemon.mod.worldgen;

import com.mojang.serialization.Codec;

import com.cobblemon.mod.block.BerryCropBlock;
import com.cobblemon.mod.content.ContentBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.neoforge.registries.DeferredBlock;

/**
 * Small wild berry clumps on grass/dirt.
 */
public class BerryPatchFeature extends Feature<NoneFeatureConfiguration> {
    public BerryPatchFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        return placeAt(context.level(), context.origin(), context.random());
    }

    /** Static entry for composite worldgen features. */
    public static boolean placeAt(WorldGenLevel level, BlockPos origin, RandomSource random) {
        // Pick a registered berry crop block
        Block berry = pickBerry(random);
        if (berry == null) {
            return false;
        }
        BlockState ripe = berry.defaultBlockState();
        if (ripe.hasProperty(BerryCropBlock.AGE)) {
            ripe = ripe.setValue(BerryCropBlock.AGE, BerryCropBlock.MAX_AGE);
        }

        int placed = 0;
        int count = 3 + random.nextInt(5);
        for (int i = 0; i < count; i++) {
            BlockPos p = origin.offset(random.nextInt(5) - 2, 0, random.nextInt(5) - 2);
            BlockPos ground = p.below();
            if (!com.cobblemon.mod.block.BerryCropBlock.isValidSoil(level.getBlockState(ground))) {
                continue;
            }
            if (!level.isEmptyBlock(p) && !level.getBlockState(p).canBeReplaced()
                    && !level.getBlockState(p).is(Blocks.SHORT_GRASS)
                    && !level.getBlockState(p).is(Blocks.TALL_GRASS)) {
                continue;
            }
            level.setBlock(p, ripe, Block.UPDATE_CLIENTS);
            placed++;
        }
        return placed > 0;
    }

    private static Block pickBerry(RandomSource random) {
        // Prefer common berries from content registry
        String[] prefer = {
                "oran_berry", "pecha_berry", "cheri_berry", "rawst_berry",
                "aspear_berry", "chesto_berry", "leppa_berry", "sitrus_berry"
        };
        for (int i = 0; i < prefer.length; i++) {
            String id = prefer[random.nextInt(prefer.length)];
            DeferredBlock<? extends Block> def = ContentBlocks.BY_ID.get(id);
            if (def != null) {
                return def.get();
            }
        }
        // Fallback: any berry crop in content
        for (var e : ContentBlocks.BY_ID.entrySet()) {
            if (e.getKey().endsWith("_berry") && e.getValue().get() instanceof BerryCropBlock) {
                return e.getValue().get();
            }
        }
        return null;
    }
}

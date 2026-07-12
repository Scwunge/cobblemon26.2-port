package com.cobblemon.mod.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import com.cobblemon.mod.block.BerryCropBlock;
import com.cobblemon.mod.block.MintCropBlock;
import com.cobblemon.mod.content.ContentBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.neoforged.neoforge.registries.DeferredBlock;

/**
 * Places a small patch of a content plant block (mint, herb, leek, etc.).
 */
public class SimplePlantPatchFeature extends Feature<SimplePlantPatchFeature.Config> {
    public SimplePlantPatchFeature(Codec<Config> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> context) {
        Config cfg = context.config();
        return placeAt(context.level(), context.origin(), cfg.blockId(), cfg.tries(), cfg.spread(), context.random());
    }

    public static boolean placeAt(
            WorldGenLevel level, BlockPos origin, String blockId, int tries, int spread, RandomSource random
    ) {
        DeferredBlock<? extends Block> def = ContentBlocks.BY_ID.get(blockId);
        if (def == null) {
            return false;
        }
        Block plant = def.get();
        BlockState state = plant.defaultBlockState();
        if (state.hasProperty(MintCropBlock.AGE)) {
            // Wild mints spawn fully grown
            state = state.setValue(MintCropBlock.AGE, MintCropBlock.MAX_AGE);
        } else if (state.hasProperty(BerryCropBlock.AGE)) {
            state = state.setValue(BerryCropBlock.AGE, BerryCropBlock.MAX_AGE);
        }

        int placed = 0;
        tries = Math.max(1, tries);
        for (int i = 0; i < tries; i++) {
            BlockPos p = origin.offset(
                    random.nextInt(spread * 2 + 1) - spread,
                    random.nextInt(3) - 1,
                    random.nextInt(spread * 2 + 1) - spread
            );
            BlockPos ground = p.below();
            if (!BerryCropBlock.isValidSoil(level.getBlockState(ground))
                    && !level.getBlockState(ground).is(Blocks.SAND)
                    && !level.getBlockState(ground).is(net.minecraft.tags.BlockTags.SAND)) {
                continue;
            }
            if (!level.isEmptyBlock(p) && !level.getBlockState(p).canBeReplaced()
                    && !level.getBlockState(p).is(Blocks.SHORT_GRASS)
                    && !level.getBlockState(p).is(Blocks.TALL_GRASS)
                    && !level.getBlockState(p).is(Blocks.MOSS_CARPET)) {
                continue;
            }
            level.setBlock(p, state, Block.UPDATE_CLIENTS);
            placed++;
        }
        return placed > 0;
    }

    public record Config(String blockId, int tries, int spread) implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("block").forGetter(Config::blockId),
                Codec.INT.optionalFieldOf("tries", 8).forGetter(Config::tries),
                Codec.INT.optionalFieldOf("spread", 3).forGetter(Config::spread)
        ).apply(i, Config::new));
    }
}

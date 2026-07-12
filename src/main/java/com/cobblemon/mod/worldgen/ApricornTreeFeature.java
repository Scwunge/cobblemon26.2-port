package com.cobblemon.mod.worldgen;

import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.BlockStateConfiguration;

/**
 * Worldgen feature: place one apricorn tree; fruit color taken from config state block id
 * (e.g. {@code cobblemon:red_apricorn}).
 */
public class ApricornTreeFeature extends Feature<BlockStateConfiguration> {
    public ApricornTreeFeature(Codec<BlockStateConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<BlockStateConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();
        BlockState fruitCfg = context.config().state;
        String path = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(fruitCfg.getBlock()).getPath();
        String color = "red";
        if (path.endsWith("_apricorn")) {
            color = path.substring(0, path.length() - "_apricorn".length());
        } else if (path.endsWith("_apricorn_sapling")) {
            color = path.substring(0, path.length() - "_apricorn_sapling".length());
        }
        // Config state may be fruit at origin; place tree with base at origin
        return ApricornTrees.place(level, origin, color, random);
    }
}

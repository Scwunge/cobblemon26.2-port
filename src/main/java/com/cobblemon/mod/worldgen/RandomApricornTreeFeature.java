package com.cobblemon.mod.worldgen;

import com.mojang.serialization.Codec;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** Picks a random apricorn color and places one tree — avoids selector feature-order cycles. */
public class RandomApricornTreeFeature extends Feature<NoneFeatureConfiguration> {
    public RandomApricornTreeFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        RandomSource random = context.random();
        String color = ApricornTrees.COLORS[random.nextInt(ApricornTrees.COLORS.length)];
        return ApricornTrees.place(context.level(), context.origin(), color, random);
    }
}

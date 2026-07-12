package com.cobblemon.mod.worldgen;

import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Single vegetal feature so biome modifiers only add ONE placed feature per step.
 * Avoids Minecraft "Feature order cycle" crashes from multi-feature add lists.
 */
public class OverworldDecorationFeature extends Feature<NoneFeatureConfiguration> {
    public OverworldDecorationFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();
        boolean any = false;

        int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, origin.getX(), origin.getZ());
        BlockPos surface = new BlockPos(origin.getX(), surfaceY, origin.getZ());

        // ~1/56 per placement attempt × count=3 ≈ ~1 tree / ~19 chunks (special resource trees)
        if (random.nextInt(56) == 0) {
            String color = ApricornTrees.COLORS[random.nextInt(ApricornTrees.COLORS.length)];
            if (ApricornTrees.place(level, surface, color, random)) {
                any = true;
            }
        }
        // ~1/80 saccharine (rarer than apricorn)
        if (random.nextInt(80) == 0) {
            if (SaccharineTrees.place(level, surface, random)) {
                any = true;
            }
        }
        // ~1/12 berry patch nearby
        if (random.nextInt(12) == 0) {
            BlockPos patch = surface.offset(random.nextInt(5) - 2, 0, random.nextInt(5) - 2);
            if (BerryPatchFeature.placeAt(level, patch, random)) {
                any = true;
            }
        }
        // ~1/18 mint / herb
        if (random.nextInt(18) == 0) {
            String[] plants = {"red_mint", "revival_herb", "blue_mint", "medicinal_leek"};
            String plant = plants[random.nextInt(plants.length)];
            if (SimplePlantPatchFeature.placeAt(level, surface, plant, 6, 3, random)) {
                any = true;
            }
        }
        // ~1/36 mini ruin
        if (random.nextInt(36) == 0) {
            if (MiniRuinFeature.placeAt(level, surface, random)) {
                any = true;
            }
        }
        // S1 safe landmarks (rare — feature registry only, never structure tags)
        if (random.nextInt(90) == 0) {
            if (LandmarkFeature.placeAt(level, surface, random, LandmarkFeature.Kind.CENTER)) {
                any = true;
            }
        }
        if (random.nextInt(110) == 0) {
            if (LandmarkFeature.placeAt(level, surface, random, LandmarkFeature.Kind.TOWER)) {
                any = true;
            }
        }
        if (random.nextInt(95) == 0) {
            if (LandmarkFeature.placeAt(level, surface, random, LandmarkFeature.Kind.DIG_SITE)) {
                any = true;
            }
        }
        return any;
    }
}

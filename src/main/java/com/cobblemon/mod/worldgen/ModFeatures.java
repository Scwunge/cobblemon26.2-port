package com.cobblemon.mod.worldgen;

import com.cobblemon.mod.Cobblemon;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.BlockStateConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Cobblemon vegetal features (apricorn trees, berry patches).
 */
public final class ModFeatures {
    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(Registries.FEATURE, Cobblemon.MOD_ID);

    public static final DeferredHolder<Feature<?>, Feature<BlockStateConfiguration>> APRICORN_TREE =
            FEATURES.register("apricorn_tree", () -> new ApricornTreeFeature(BlockStateConfiguration.CODEC));

    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> RANDOM_APRICORN_TREE =
            FEATURES.register("random_apricorn_tree", () -> new RandomApricornTreeFeature(NoneFeatureConfiguration.CODEC));

    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> BERRY_PATCH =
            FEATURES.register("berry_patch", () -> new BerryPatchFeature(NoneFeatureConfiguration.CODEC));

    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> SACCHARINE_TREE =
            FEATURES.register("saccharine_tree", () -> new SaccharineTreeFeature(NoneFeatureConfiguration.CODEC));

    public static final DeferredHolder<Feature<?>, Feature<SimplePlantPatchFeature.Config>> SIMPLE_PLANT =
            FEATURES.register("simple_plant", () -> new SimplePlantPatchFeature(SimplePlantPatchFeature.Config.CODEC));

    /** Safe mini-ruins (feature, not structure registry). */
    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> MINI_RUIN =
            FEATURES.register("mini_ruin", () -> new MiniRuinFeature(NoneFeatureConfiguration.CODEC));

    /** Code-driven evo-stone veins (reliable overworld fill). */
    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> COBBLEMON_ORE =
            FEATURES.register("cobblemon_ore", () -> new CobblemonOreFeature(NoneFeatureConfiguration.CODEC));

    /**
     * One vegetal feature for biome modifiers — avoids feature-order cycles
     * from adding many features to every overworld biome.
     */
    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> OVERWORLD_DECORATION =
            FEATURES.register("overworld_decoration", () -> new OverworldDecorationFeature(NoneFeatureConfiguration.CODEC));

    private ModFeatures() {}
}

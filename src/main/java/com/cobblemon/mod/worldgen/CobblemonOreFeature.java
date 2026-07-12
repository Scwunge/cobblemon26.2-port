package com.cobblemon.mod.worldgen;

import com.mojang.serialization.Codec;

import com.cobblemon.mod.Cobblemon;
import com.cobblemon.mod.content.ContentBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
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
 * Reliable code-driven ore veins — places random evo-stone ores in stone/deepslate.
 * Used when datapack biome modifiers fail or as denser overworld fill.
 */
public class CobblemonOreFeature extends Feature<NoneFeatureConfiguration> {
    private static final String[] STONES = {
            "fire_stone", "water_stone", "thunder_stone", "leaf_stone", "ice_stone",
            "moon_stone", "dawn_stone", "dusk_stone", "sun_stone", "shiny_stone"
    };

    public CobblemonOreFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();

        BlockState below = level.getBlockState(origin);
        if (!below.is(BlockTags.STONE_ORE_REPLACEABLES) && !below.is(BlockTags.DEEPSLATE_ORE_REPLACEABLES)
                && !below.is(Blocks.STONE) && !below.is(Blocks.DEEPSLATE)
                && !below.is(Blocks.ANDESITE) && !below.is(Blocks.DIORITE) && !below.is(Blocks.GRANITE)
                && !below.is(Blocks.TUFF)) {
            return false;
        }

        String stone = STONES[random.nextInt(STONES.length)];
        boolean deep = below.is(BlockTags.DEEPSLATE_ORE_REPLACEABLES) || below.is(Blocks.DEEPSLATE)
                || origin.getY() < 0;
        Block ore = resolve(deep ? "deepslate_" + stone + "_ore" : stone + "_ore");
        if (ore == Blocks.AIR) {
            ore = resolve(stone + "_ore");
        }
        if (ore == Blocks.AIR) {
            return false;
        }
        BlockState oreState = ore.defaultBlockState();

        // Small blob vein (size ~4-8)
        int size = 4 + random.nextInt(5);
        int placed = 0;
        BlockPos.MutableBlockPos cursor = origin.mutable();
        for (int i = 0; i < size * 3 && placed < size; i++) {
            BlockState cur = level.getBlockState(cursor);
            if (cur.is(BlockTags.STONE_ORE_REPLACEABLES) || cur.is(BlockTags.DEEPSLATE_ORE_REPLACEABLES)
                    || cur.is(Blocks.STONE) || cur.is(Blocks.DEEPSLATE)
                    || cur.is(Blocks.ANDESITE) || cur.is(Blocks.DIORITE) || cur.is(Blocks.GRANITE)
                    || cur.is(Blocks.TUFF)) {
                level.setBlock(cursor, oreState, 2);
                placed++;
            }
            cursor.move(
                    random.nextInt(3) - 1,
                    random.nextInt(3) - 1,
                    random.nextInt(3) - 1
            );
        }
        return placed > 0;
    }

    private static Block resolve(String path) {
        DeferredBlock<? extends Block> def = ContentBlocks.BY_ID.get(path);
        if (def != null) {
            return def.get();
        }
        return BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath(Cobblemon.MOD_ID, path));
    }
}

package com.cobblemon.mod.item;

import com.cobblemon.mod.block.MintCropBlock;
import com.cobblemon.mod.content.ContentBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.DeferredBlock;

/**
 * Plants a color mint crop (red_mint_seeds → red_mint, etc.).
 */
public class MintSeedsItem extends Item {
    private final String plantBlockId;

    public MintSeedsItem(Properties properties, String seedId) {
        super(properties);
        // red_mint_seeds → red_mint
        this.plantBlockId = seedId.endsWith("_seeds")
                ? seedId.substring(0, seedId.length() - "_seeds".length())
                : seedId;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos soil = context.getClickedPos();
        BlockPos plantPos = soil.above();
        // Allow planting on top of soil when clicking the soil face, or replacing air above
        if (!MintCropBlock.isValidSoil(level.getBlockState(soil))) {
            // Clicked the plant space? try soil below
            if (MintCropBlock.isValidSoil(level.getBlockState(soil.below())) && level.getBlockState(soil).isAir()) {
                plantPos = soil;
                soil = soil.below();
            } else {
                return InteractionResult.PASS;
            }
        }
        if (!level.getBlockState(plantPos).isAir()) {
            return InteractionResult.PASS;
        }
        DeferredBlock<? extends Block> def = ContentBlocks.BY_ID.get(plantBlockId);
        if (def == null) {
            return InteractionResult.PASS;
        }
        BlockState placed = def.get().defaultBlockState();
        if (placed.hasProperty(MintCropBlock.AGE)) {
            placed = placed.setValue(MintCropBlock.AGE, 0);
        }
        if (!level.isClientSide()) {
            level.setBlock(plantPos, placed, Block.UPDATE_ALL);
            level.playSound(null, plantPos, SoundEvents.CROP_PLANTED, SoundSource.BLOCKS, 1f, 1f);
            if (context.getPlayer() != null && !context.getPlayer().getAbilities().instabuild) {
                context.getItemInHand().shrink(1);
            }
        }
        return InteractionResult.SUCCESS;
    }
}

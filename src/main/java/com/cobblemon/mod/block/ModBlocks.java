package com.cobblemon.mod.block;

import com.cobblemon.mod.Cobblemon;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Cobblemon.MOD_ID);

    /**
     * Must use {@code registerBlock} so NeoForge/MC 26.1 can call {@code Properties#setId}
     * before the block is constructed (plain {@code register} leaves id null → crash).
     */
    public static final DeferredBlock<PcBlock> PC = BLOCKS.registerBlock(
            "pc",
            PcBlock::new,
            props -> props
                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(3.5F, 8.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()
                    .requiresCorrectToolForDrops()
    );

    private ModBlocks() {}
}

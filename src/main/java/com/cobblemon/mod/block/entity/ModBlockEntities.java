package com.cobblemon.mod.block.entity;

import java.util.Set;

import com.cobblemon.mod.Cobblemon;
import com.cobblemon.mod.content.ContentBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Cobblemon.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PastureBlockEntity>> PASTURE =
            BLOCK_ENTITY_TYPES.register("pasture", () ->
                    new BlockEntityType<>(
                            PastureBlockEntity::new,
                            Set.of(block("pasture"))
                    ));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FossilMachineBlockEntity>> FOSSIL_MACHINE =
            BLOCK_ENTITY_TYPES.register("fossil_machine", () ->
                    new BlockEntityType<>(
                            FossilMachineBlockEntity::new,
                            Set.of(block("fossil_analyzer"), block("restoration_tank"))
                    ));

    private static Block block(String id) {
        var def = ContentBlocks.BY_ID.get(id);
        if (def == null) {
            throw new IllegalStateException("Missing content block: " + id);
        }
        return def.get();
    }

    private ModBlockEntities() {}
}

package com.cobblemon.mod.item;

import com.cobblemon.mod.Cobblemon;
import com.cobblemon.mod.block.ModBlocks;
import com.cobblemon.mod.entity.ModEntities;
import com.cobblemon.mod.species.EvolutionMethod;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Core gameplay items. Catch balls live under ContentItems as poke_ball / great_ball / ultra_ball
 * (cube balls were removed).
 */
public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Cobblemon.MOD_ID);

    public static final DeferredItem<PartyBadgeItem> PARTY_BADGE = ITEMS.registerItem(
            "party_badge",
            PartyBadgeItem::new,
            props -> props.stacksTo(1)
    );

    public static final DeferredItem<BlockItem> PC = ITEMS.registerSimpleBlockItem(
            "pc",
            ModBlocks.PC
    );

    public static final DeferredItem<EvolutionGemItem> FIRE_GEM = ITEMS.registerItem(
            "fire_gem",
            props -> new EvolutionGemItem(props, EvolutionMethod.FIRE_GEM),
            props -> props.stacksTo(16)
    );

    public static final DeferredItem<EvolutionGemItem> WATER_GEM = ITEMS.registerItem(
            "water_gem",
            props -> new EvolutionGemItem(props, EvolutionMethod.WATER_GEM),
            props -> props.stacksTo(16)
    );

    public static final DeferredItem<EvolutionGemItem> THUNDER_GEM = ITEMS.registerItem(
            "thunder_gem",
            props -> new EvolutionGemItem(props, EvolutionMethod.THUNDER_GEM),
            props -> props.stacksTo(16)
    );

    public static final DeferredItem<EvolutionGemItem> LEAF_GEM = ITEMS.registerItem(
            "leaf_gem",
            props -> new EvolutionGemItem(props, EvolutionMethod.LEAF_GEM),
            props -> props.stacksTo(16)
    );

    public static final DeferredItem<EvolutionGemItem> MOON_GEM = ITEMS.registerItem(
            "moon_gem",
            props -> new EvolutionGemItem(props, EvolutionMethod.MOON_GEM),
            props -> props.stacksTo(16)
    );

    public static final DeferredItem<SpawnEggItem> WILD_MON_SPAWN_EGG = ITEMS.registerItem(
            "wild_mon_spawn_egg",
            SpawnEggItem::new,
            props -> props.spawnEgg(ModEntities.WILD_MON.get())
    );

    /** Pasture breeding product — hatches into a Lv.1 {@link com.cobblemon.mod.species.OwnedMon}. */
    public static final DeferredItem<PokemonEggItem> POKEMON_EGG = ITEMS.registerItem(
            "pokemon_egg",
            PokemonEggItem::new,
            props -> props.stacksTo(1)
    );

    private ModItems() {}
}

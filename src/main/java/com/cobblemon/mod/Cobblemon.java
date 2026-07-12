package com.cobblemon.mod;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import com.cobblemon.mod.block.ModBlocks;
import com.cobblemon.mod.block.entity.ModBlockEntities;
import com.cobblemon.mod.command.CobblemonCommands;
import com.cobblemon.mod.content.ContentBlocks;
import com.cobblemon.mod.content.ContentItems;
import com.cobblemon.mod.content.ContentKind;
import com.cobblemon.mod.entity.ModEntities;
import com.cobblemon.mod.entity.ModSpawn;
import com.cobblemon.mod.entity.WildMonEntity;
import com.cobblemon.mod.entity.WildMonSpawner;
import com.cobblemon.mod.item.ModItems;
import com.cobblemon.mod.party.ModAttachments;
import com.cobblemon.mod.sound.ModSounds;
import com.cobblemon.mod.species.HeldItems;
import com.cobblemon.mod.species.MonSpecies;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(Cobblemon.MOD_ID)
public class Cobblemon {
    public static final String MOD_ID = "cobblemon";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> BLOCKS_TAB = CREATIVE_TABS.register(
            "blocks",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.cobblemon.blocks"))
                    .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
                    .icon(() -> iconOr("apricorn_planks", () -> ModItems.PC.get().getDefaultInstance()))
                    .displayItems((params, output) -> {
                        for (var e : ContentBlocks.BY_ID.entrySet()) {
                            var item = ContentItems.BY_ID.get(e.getKey());
                            if (item != null) {
                                output.accept(item.get());
                            }
                        }
                        output.accept(ModItems.PC.get());
                    })
                    .build()
    );

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> UTILITY_TAB = CREATIVE_TABS.register(
            "utility_item",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.cobblemon.utility_item"))
                    .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
                    .icon(() -> iconOr("poke_ball", () -> new ItemStack(com.cobblemon.mod.item.CubeBallTier.POKE.item())))
                    .displayItems((params, output) -> {
                        output.accept(ModItems.PARTY_BADGE.get());
                        output.accept(ModItems.WILD_MON_SPAWN_EGG.get());
                        output.accept(ModItems.POKEMON_EGG.get());
                        for (var e : ContentItems.BY_ID.entrySet()) {
                            String id = e.getKey();
                            if (ContentBlocks.BY_ID.containsKey(id)) {
                                continue;
                            }
                            if (isBall(id) || isUtility(id)) {
                                output.accept(e.getValue().get());
                            }
                        }
                    })
                    .build()
    );

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> POKE_BALLS_TAB = CREATIVE_TABS.register(
            "poke_balls",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.cobblemon.poke_balls"))
                    .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
                    .icon(() -> iconOr("poke_ball", () -> new ItemStack(com.cobblemon.mod.item.CubeBallTier.POKE.item())))
                    .displayItems((params, output) -> {
                        for (var e : ContentItems.BY_ID.entrySet()) {
                            if (isBall(e.getKey())) {
                                output.accept(e.getValue().get());
                            }
                        }
                    })
                    .build()
    );

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> AGRICULTURE_TAB = CREATIVE_TABS.register(
            "agriculture",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.cobblemon.agriculture"))
                    .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
                    .icon(() -> iconOr("oran_berry", () -> new ItemStack(com.cobblemon.mod.item.CubeBallTier.POKE.item())))
                    .displayItems((params, output) -> {
                        for (var e : ContentItems.BY_ID.entrySet()) {
                            String id = e.getKey();
                            if (id.contains("berry") || id.contains("mulch") || id.contains("apricorn")
                                    || id.contains("mint") || id.contains("seed") || id.contains("sprout")
                                    || id.contains("vivichoke") || id.contains("galarica")
                                    || id.contains("hearty") || id.contains("bugwort") || id.contains("revival_herb")) {
                                output.accept(e.getValue().get());
                            }
                        }
                    })
                    .build()
    );

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> CONSUMABLES_TAB = CREATIVE_TABS.register(
            "consumables",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.cobblemon.consumables"))
                    .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
                    .icon(() -> iconOr("potion", () -> new ItemStack(com.cobblemon.mod.item.CubeBallTier.POKE.item())))
                    .displayItems((params, output) -> {
                        for (var e : ContentItems.BY_ID.entrySet()) {
                            String id = e.getKey();
                            if (ContentBlocks.BY_ID.containsKey(id) || id.contains("berry")) {
                                continue;
                            }
                            ContentKind.ItemRole role = ContentKind.itemRole(id);
                            if (role == ContentKind.ItemRole.MEDICINE
                                    || role == ContentKind.ItemRole.CANDY
                                    || id.contains("potion") || id.contains("ether") || id.contains("elixir")
                                    || id.contains("revive") || id.contains("heal") || id.contains("remedy")
                                    || id.contains("mochi") || id.contains("juice") || id.contains("candy")
                                    || id.contains("protein") || id.contains("calcium") || id.contains("zinc")
                                    || id.contains("carbos") || id.contains("hp_up") || id.contains("pp_up")
                                    || id.contains("pp_max") || id.contains("rare_candy") || id.contains("exp_candy")) {
                                output.accept(e.getValue().get());
                            }
                        }
                    })
                    .build()
    );

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> HELD_TAB = CREATIVE_TABS.register(
            "held_item",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.cobblemon.held_item"))
                    .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
                    .icon(() -> iconOr("leftovers", () -> new ItemStack(com.cobblemon.mod.item.CubeBallTier.POKE.item())))
                    .displayItems((params, output) -> {
                        for (var e : ContentItems.BY_ID.entrySet()) {
                            String id = e.getKey();
                            if (ContentBlocks.BY_ID.containsKey(id) || isBall(id)) {
                                continue;
                            }
                            if (HeldItems.canHold(id) || isLikelyHeld(id)) {
                                output.accept(e.getValue().get());
                            }
                        }
                    })
                    .build()
    );

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> EVOLUTION_TAB = CREATIVE_TABS.register(
            "evolution_item",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.cobblemon.evolution_item"))
                    .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
                    .icon(() -> iconOr("fire_stone", () -> ModItems.FIRE_GEM.get().getDefaultInstance()))
                    .displayItems((params, output) -> {
                        output.accept(ModItems.FIRE_GEM.get());
                        output.accept(ModItems.WATER_GEM.get());
                        output.accept(ModItems.THUNDER_GEM.get());
                        output.accept(ModItems.LEAF_GEM.get());
                        output.accept(ModItems.MOON_GEM.get());
                        for (var e : ContentItems.BY_ID.entrySet()) {
                            String id = e.getKey();
                            if (ContentBlocks.BY_ID.containsKey(id)) {
                                continue;
                            }
                            if (ContentKind.itemRole(id) == ContentKind.ItemRole.EVOLUTION_STONE
                                    || id.endsWith("_stone") || id.contains("dragon_scale") || id.contains("metal_coat")
                                    || id.contains("upgrade") || id.contains("dubious") || id.contains("protector")
                                    || id.contains("reaper") || id.contains("sachet") || id.contains("whipped")
                                    || id.contains("_sweet") || id.contains("link_cable") || id.contains("oval_stone")
                                    || id.contains("electirizer") || id.contains("magmarizer") || id.contains("prism_scale")
                                    || id.contains("deep_sea") || id.contains("metal_alloy") || id.contains("scroll_of")
                                    || id.contains("black_augurite") || id.contains("peat_block") || id.contains("galarica_cuff")
                                    || id.contains("galarica_wreath") || id.contains("chipped_pot") || id.contains("cracked_pot")
                                    || id.contains("kings_rock") || id.contains("razor_claw") || id.contains("razor_fang")) {
                                output.accept(e.getValue().get());
                            }
                        }
                    })
                    .build()
    );

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ARCHAEOLOGY_TAB = CREATIVE_TABS.register(
            "archaeology",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.cobblemon.archaeology"))
                    .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
                    .icon(() -> iconOr("helix_fossil", () -> new ItemStack(com.cobblemon.mod.item.CubeBallTier.POKE.item())))
                    .displayItems((params, output) -> {
                        for (var e : ContentItems.BY_ID.entrySet()) {
                            String id = e.getKey();
                            if (id.contains("fossil") || id.contains("fossilized")) {
                                output.accept(e.getValue().get());
                            }
                        }
                    })
                    .build()
    );

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> PLANTS_TAB = CREATIVE_TABS.register(
            "plants",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.cobblemon.plants"))
                    .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
                    .icon(() -> iconOr("pep_up_flower", () -> new ItemStack(com.cobblemon.mod.item.CubeBallTier.POKE.item())))
                    .displayItems((params, output) -> {
                        for (var e : ContentItems.BY_ID.entrySet()) {
                            String id = e.getKey();
                            if (id.contains("flower") || id.contains("sapling") || id.contains("leaves")
                                    || id.contains("log") || id.contains("wood") || id.contains("planks")
                                    || id.contains("saccharine") || id.contains("tumblestone")) {
                                output.accept(e.getValue().get());
                            }
                        }
                    })
                    .build()
    );

    private static boolean isBall(String id) {
        return id.contains("ball") && !id.contains("model") && !id.contains("block")
                && !id.equals("iron_ball") && !id.equals("light_ball") && !id.equals("smoke_ball")
                && !id.contains("snowball");
    }

    private static boolean isUtility(String id) {
        return id.contains("rod")
                || ContentKind.isPokedexItem(id)
                || id.contains("link_cable")
                || id.contains("ability_capsule") || id.contains("ability_patch") || id.contains("relic_coin")
                || id.contains("scatter_bang") || id.contains("sticky_glob") || id.contains("npc_editor")
                || id.contains("smithing_template");
    }

    private static boolean isLikelyHeld(String id) {
        return id.contains("band") || id.contains("scarf") || id.contains("specs")
                || id.contains("glasses") || id.contains("leftovers") || id.contains("life_orb")
                || id.contains("expert_belt") || id.contains("assault") || id.contains("vest")
                || id.contains("sludge") || id.contains("shell_bell") || id.contains("muscle")
                || id.contains("wise_") || id.equals("iron_ball") || id.equals("light_ball")
                || id.equals("smoke_ball") || id.contains("plate") || id.contains("booster")
                || id.contains("choice_") || id.contains("focus_") || id.contains("scope_")
                || id.contains("wide_lens") || id.contains("zoom_lens") || id.contains("metronome")
                || id.contains("rocky_helmet") || id.contains("eviolite") || id.contains("weakness")
                || id.contains("power_");
    }

    private static ItemStack iconOr(String contentId, java.util.function.Supplier<ItemStack> fallback) {
        var def = ContentItems.BY_ID.get(contentId);
        if (def != null) {
            return def.get().getDefaultInstance();
        }
        return fallback.get();
    }

    public Cobblemon(IEventBus modEventBus) {
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::registerAttributes);
        ModSpawn.register(modEventBus);

        ModBlocks.BLOCKS.register(modEventBus);
        ModEntities.ENTITY_TYPES.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModAttachments.ATTACHMENT_TYPES.register(modEventBus);
        ModSounds.register(modEventBus);

        ContentBlocks.BLOCKS.register(modEventBus);
        ContentItems.ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITY_TYPES.register(modEventBus);
        com.cobblemon.mod.worldgen.ModFeatures.FEATURES.register(modEventBus);

        CREATIVE_TABS.register(modEventBus);

        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.register(WildMonSpawner.class);
        NeoForge.EVENT_BUS.register(com.cobblemon.mod.ride.RideEvents.class);
        NeoForge.EVENT_BUS.addListener(Cobblemon::onServerStarting);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // Load Cobblemon datapack indexes (species stats + world spawn pools)
        com.cobblemon.mod.species.SpeciesRegistry.bootstrap();
        com.cobblemon.mod.species.SpeciesAssets.bootstrap();
        com.cobblemon.mod.species.DisabledSpecies.bootstrap(); // before spawns — filters no-model mons
        com.cobblemon.mod.species.SpawnPoolLoader.bootstrap();
        com.cobblemon.mod.species.EvolutionLookup.bootstrap();
        com.cobblemon.mod.species.ItemEvolutionLookup.bootstrapClasspath();
        com.cobblemon.mod.species.FossilRecipes.bootstrapClasspath();
        com.cobblemon.mod.worldgen.StructureBootstrap.bootstrap();
        com.cobblemon.mod.species.StarterCatalog.bootstrap();
        com.cobblemon.mod.battle.ShowdownMoveDex.bootstrap();
        // GraalVM JS spike (Java 25) — never throws; logs SUCCESS/FAILURE only.
        com.cobblemon.mod.battle.graal.GraalSpike.tryBootstrap();
        // Showdown runner v0 — optional JS context + move probe; never throws.
        com.cobblemon.mod.battle.graal.ShowdownRunner.bootstrap();
        // Showdown combat bridge v1 — JS damage formula + zip extract; native fallback.
        com.cobblemon.mod.battle.graal.ShowdownCombatBridge.bootstrap();
        // Full Showdown CommonJS sim (index.js) — best-effort; may fail without polyfills.
        com.cobblemon.mod.battle.graal.ShowdownSim.bootstrap();
        // MoLang scaffold — scan data/cobblemon/molang + tiny expression eval; never throws.
        com.cobblemon.mod.molang.MoLangBootstrap.bootstrap();
        // Ride settings JSON (data/cobblemon/ride_settings) — safe defaults if missing.
        com.cobblemon.mod.ride.RideSettingsLoader.bootstrap();
        LOGGER.info("{}", com.cobblemon.mod.battle.BattleEngine.describe());
        com.cobblemon.mod.kotlin.CobblemonKotlin.bootstrap();
        LOGGER.info(
                "Cobblemon 26.2 — runtime: {} · datapack: {} · assets: {} · playable spawns: {} · disabled (no model): {} · items: {} · blocks: {} · fossils: {} · showdown moves: {} · battle: {}",
                MonSpecies.count(),
                com.cobblemon.mod.species.SpeciesRegistry.size(),
                com.cobblemon.mod.species.SpeciesAssets.size(),
                com.cobblemon.mod.species.SpawnPoolLoader.entries().size(),
                com.cobblemon.mod.species.DisabledSpecies.size(),
                ContentItems.BY_ID.size(),
                ContentBlocks.BY_ID.size(),
                com.cobblemon.mod.species.FossilRecipes.speciesForFossilItem("old_amber_fossil").isPresent() ? "loaded" : "fallback",
                com.cobblemon.mod.battle.ShowdownMoveDex.size(),
                com.cobblemon.mod.battle.BattleEngine.preferredMode()
        );
        LOGGER.info("Cobblemon ready — M starter · P party · R send out · apricorn trees + berry patches in forests/plains/jungle");
    }

    private void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.WILD_MON.get(), WildMonEntity.createAttributes().build());
        event.put(ModEntities.TRAINER_NPC.get(), com.cobblemon.mod.entity.TrainerNpcEntity.createAttributes().build());
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        CobblemonCommands.register(event.getDispatcher());
    }

    private static void onServerStarting(net.neoforged.neoforge.event.server.ServerStartingEvent event) {
        try {
            com.cobblemon.mod.dialogue.DialogueManager.bootstrap(event.getServer().getResourceManager());
        } catch (Throwable t) {
            LOGGER.warn("Dialogue bootstrap failed: {}", t.toString());
        }
    }
}

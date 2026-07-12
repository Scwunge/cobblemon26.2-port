package com.cobblemon.mod.content;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.cobblemon.mod.Cobblemon;
import com.cobblemon.mod.item.BaitItem;
import com.cobblemon.mod.item.BerryFoodItem;
import com.cobblemon.mod.item.CandyItem;
import com.cobblemon.mod.item.CubeBallItem;
import com.cobblemon.mod.item.CubeBallTier;
import com.cobblemon.mod.item.EvolutionStoneItem;
import com.cobblemon.mod.item.HeldItemGiverItem;
import com.cobblemon.mod.item.MedicineBlockItem;
import com.cobblemon.mod.item.MedicineItem;
import com.cobblemon.mod.item.MintItem;
import com.cobblemon.mod.item.MintSeedsItem;
import com.cobblemon.mod.item.PokedexItem;
import com.cobblemon.mod.item.PokerodItem;
import com.cobblemon.mod.item.VitaminItem;
import com.cobblemon.mod.species.HeldItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registers Cobblemon items with Cobblemon-aligned behavior classes.
 */
public final class ContentItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Cobblemon.MOD_ID);

    public static final Map<String, DeferredItem<? extends Item>> BY_ID = new LinkedHashMap<>();
    public static final List<DeferredItem<? extends Item>> ALL = new ArrayList<>();

    static {
        // --- Block items (every content block gets a placeable item) ---
        for (var e : ContentBlocks.BY_ID.entrySet()) {
            String id = e.getKey();
            DeferredBlock<? extends Block> block = e.getValue();
            ContentKind.BlockRole role = ContentKind.blockRole(id);
            DeferredItem<? extends Item> bi;

            if (role == ContentKind.BlockRole.MEDICINE_DISPLAY || ContentKind.isMedicine(id)) {
                bi = ITEMS.registerItem(
                        id,
                        props -> new MedicineBlockItem(block.get(), props, id),
                        props -> props.stacksTo(64)
                );
            } else if (role == ContentKind.BlockRole.BERRY_CROP || id.endsWith("_berry")) {
                bi = ITEMS.registerItem(
                        id,
                        props -> new BerryFoodItem(block.get(), props, id),
                        props -> props.stacksTo(64)
                );
            } else {
                bi = ITEMS.registerSimpleBlockItem(id, block);
            }
            BY_ID.put(id, bi);
            ALL.add(bi);
        }

        // --- Pure items (not also blocks) ---
        for (String id : ContentLists.items()) {
            String regId = id.replace('/', '_');
            if (BY_ID.containsKey(regId)) {
                continue;
            }
            // Skip animation / 3D-only meshes (e.g. pokedex_red_model_scanning)
            if (ContentKind.isModelOnlyItem(regId)) {
                continue;
            }
            ContentKind.ItemRole role = ContentKind.itemRole(regId);
            DeferredItem<? extends Item> item = switch (role) {
                case BALL -> {
                    CubeBallTier tier = ContentKind.ballTier(regId);
                    yield ITEMS.registerItem(
                            regId,
                            props -> new CubeBallItem(props, tier),
                            props -> props.stacksTo(64)
                    );
                }
                case MEDICINE -> ITEMS.registerItem(
                        regId,
                        props -> new MedicineItem(props, regId),
                        props -> props.stacksTo(64)
                );
                case CANDY -> ITEMS.registerItem(
                        regId,
                        props -> new CandyItem(props, regId),
                        props -> props.stacksTo(64)
                );
                case EVOLUTION_STONE -> ITEMS.registerItem(
                        regId,
                        props -> new EvolutionStoneItem(props, regId),
                        props -> props.stacksTo(64)
                );
                case MINT -> ITEMS.registerItem(
                        regId,
                        props -> new MintItem(props, regId),
                        props -> props.stacksTo(64)
                );
                case MINT_SEEDS -> ITEMS.registerItem(
                        regId,
                        props -> new MintSeedsItem(props, regId),
                        props -> props.stacksTo(64)
                );
                case BERRY -> ITEMS.registerItem(
                        regId,
                        props -> new MedicineItem(props, regId),
                        props -> props.stacksTo(64)
                );
                case HELD -> ITEMS.registerItem(
                        regId,
                        props -> new HeldItemGiverItem(props, regId),
                        props -> props.stacksTo(64)
                );
                case BAIT -> ITEMS.registerItem(
                        regId,
                        props -> new BaitItem(props, regId),
                        props -> props.stacksTo(64)
                );
                case VITAMIN -> ITEMS.registerItem(
                        regId,
                        props -> new VitaminItem(props, regId),
                        props -> props.stacksTo(64)
                );
                case POKEDEX -> {
                    String color = regId.startsWith("pokedex_")
                            ? regId.substring("pokedex_".length())
                            : "red";
                    yield ITEMS.registerItem(
                            regId,
                            props -> new PokedexItem(props, color),
                            props -> props.stacksTo(1)
                    );
                }
                default -> {
                    // Official Cobblemon trade-evo item (not a Trade Machine block)
                    if (regId.equals("link_cable")) {
                        yield ITEMS.registerItem(
                                regId,
                                com.cobblemon.mod.item.LinkCableItem::new,
                                props -> props.stacksTo(64)
                        );
                    }
                    if (regId.contains("rod") && !regId.contains("cast")) {
                        yield ITEMS.registerItem(regId, PokerodItem::new, props -> props.stacksTo(1));
                    }
                    if (HeldItems.canHold(regId) && isLikelyHeldItem(regId)) {
                        yield ITEMS.registerItem(
                                regId,
                                props -> new HeldItemGiverItem(props, regId),
                                props -> props.stacksTo(64)
                        );
                    }
                    yield ITEMS.registerItem(regId, Item::new, props -> props.stacksTo(stacksFor(regId)));
                }
            };
            BY_ID.put(regId, item);
            ALL.add(item);
        }
        Cobblemon.LOGGER.info("ContentItems: {} items with Cobblemon-style roles", BY_ID.size());
    }

    private static int stacksFor(String id) {
        if (id.contains("rod") || id.contains("badge") || id.contains("key")) {
            return 1;
        }
        return 64;
    }

    private static boolean isLikelyHeldItem(String id) {
        return id.contains("band") || id.contains("scarf") || id.contains("specs")
                || id.contains("glasses") || id.contains("leftovers") || id.contains("life_orb")
                || id.contains("expert_belt") || id.contains("assault") || id.contains("vest")
                || id.contains("sludge") || id.contains("shell_bell") || id.contains("muscle")
                || id.contains("wise_") || id.equals("iron_ball") || id.equals("light_ball");
    }

    private ContentItems() {}
}

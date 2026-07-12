package com.cobblemon.mod.content;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import com.cobblemon.mod.Cobblemon;
import com.cobblemon.mod.block.ApricornFruitBlock;
import com.cobblemon.mod.block.ApricornSaplingBlock;
import com.cobblemon.mod.block.BerryCropBlock;
import com.cobblemon.mod.block.MachineBlock;
import com.cobblemon.mod.block.MintCropBlock;
import com.cobblemon.mod.block.MulchBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registers Cobblemon blocks with role-appropriate Block subclasses
 * (machines, woodset, leaves, ores, crops as generic for now).
 */
public final class ContentBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Cobblemon.MOD_ID);

    public static final Set<String> MACHINE_IDS = Set.of(
            "healing_machine", "pasture", "fossil_analyzer",
            "restoration_tank", "monitor", "display_case"
    );

    public static final Map<String, DeferredBlock<? extends Block>> BY_ID = new LinkedHashMap<>();

    /** Planks used as stair base (registered first pass). */
    private static DeferredBlock<? extends Block> APRICORN_PLANKS;

    static {
        // First pass: planks so stairs can reference them
        for (String id : ContentLists.blocks()) {
            String regId = id.replace('/', '_');
            if (ContentKind.blockRole(regId) == ContentKind.BlockRole.WOOD_PLANKS) {
                DeferredBlock<? extends Block> planks = BLOCKS.registerBlock(
                        regId,
                        Block::new,
                        props -> wood(props)
                );
                BY_ID.put(regId, planks);
                if ("apricorn_planks".equals(regId)) {
                    APRICORN_PLANKS = planks;
                }
            }
        }

        for (String id : ContentLists.blocks()) {
            String regId = id.replace('/', '_');
            if (BY_ID.containsKey(regId)) {
                continue;
            }
            ContentKind.BlockRole role = ContentKind.blockRole(regId);
            DeferredBlock<? extends Block> def = switch (role) {
                case MACHINE -> {
                    MachineBlock.Kind kind = MachineBlock.kindForId(regId);
                    MachineBlock.Kind k = kind != null ? kind : MachineBlock.Kind.GENERIC;
                    yield BLOCKS.registerBlock(regId, props -> new MachineBlock(props, k), ContentBlocks::machine);
                }
                case WOOD_LOG -> BLOCKS.registerBlock(regId, RotatedPillarBlock::new, ContentBlocks::wood);
                case WOOD_SLAB -> BLOCKS.registerBlock(regId, SlabBlock::new, ContentBlocks::wood);
                case WOOD_STAIRS -> {
                    DeferredBlock<? extends Block> base = APRICORN_PLANKS != null
                            ? APRICORN_PLANKS
                            : BY_ID.values().stream().findFirst().orElse(null);
                    if (base != null) {
                        yield BLOCKS.registerBlock(
                                regId,
                                props -> new StairBlock(base.get().defaultBlockState(), props),
                                ContentBlocks::wood
                        );
                    }
                    yield BLOCKS.registerBlock(regId, Block::new, ContentBlocks::wood);
                }
                case WOOD_FENCE -> BLOCKS.registerBlock(regId, FenceBlock::new, ContentBlocks::wood);
                case WOOD_FENCE_GATE -> BLOCKS.registerBlock(
                        regId,
                        props -> new FenceGateBlock(WoodType.OAK, props),
                        ContentBlocks::wood
                );
                case WOOD_DOOR -> BLOCKS.registerBlock(
                        regId,
                        props -> new DoorBlock(BlockSetType.OAK, props),
                        p -> wood(p).noOcclusion().pushReaction(PushReaction.DESTROY)
                );
                case WOOD_TRAPDOOR -> BLOCKS.registerBlock(
                        regId,
                        props -> new TrapDoorBlock(BlockSetType.OAK, props),
                        p -> wood(p).noOcclusion()
                );
                case WOOD_BUTTON -> BLOCKS.registerBlock(
                        regId,
                        props -> new ButtonBlock(BlockSetType.OAK, 30, props),
                        p -> wood(p).noCollision().strength(0.5F)
                );
                case WOOD_PRESSURE -> BLOCKS.registerBlock(
                        regId,
                        props -> new PressurePlateBlock(BlockSetType.OAK, props),
                        p -> wood(p).noCollision().strength(0.5F)
                );
                case LEAVES -> BLOCKS.registerBlock(
                        regId,
                        Block::new,
                        p -> p.mapColor(MapColor.PLANT).strength(0.2F).sound(SoundType.GRASS)
                                .noOcclusion().isSuffocating((s, g, pos) -> false)
                                .isViewBlocking((s, g, pos) -> false)
                                .ignitedByLava().pushReaction(PushReaction.DESTROY)
                );
                case BERRY_CROP -> BLOCKS.registerBlock(
                        regId,
                        BerryCropBlock::new,
                        p -> p.mapColor(MapColor.PLANT).strength(0.2F).sound(SoundType.GRASS)
                                .noOcclusion().noCollision().randomTicks()
                                .pushReaction(PushReaction.DESTROY)
                );
                case MULCH -> BLOCKS.registerBlock(
                        regId,
                        MulchBlock::new,
                        p -> p.mapColor(MapColor.DIRT).strength(0.3F).sound(SoundType.GRAVEL)
                );
                case SAPLING -> {
                    if ("saccharine_sapling".equals(regId)) {
                        yield BLOCKS.registerBlock(
                                regId,
                                com.cobblemon.mod.block.SaccharineSaplingBlock::new,
                                p -> p.mapColor(MapColor.PLANT).strength(0.0F).sound(SoundType.GRASS)
                                        .noOcclusion().noCollision().randomTicks()
                                        .pushReaction(PushReaction.DESTROY)
                        );
                    }
                    String color = colorFromSaplingId(regId);
                    yield BLOCKS.registerBlock(
                            regId,
                            props -> new ApricornSaplingBlock(color, props),
                            p -> p.mapColor(MapColor.PLANT).strength(0.0F).sound(SoundType.GRASS)
                                    .noOcclusion().noCollision().randomTicks()
                                    .pushReaction(PushReaction.DESTROY)
                    );
                }
                case APRICORN -> BLOCKS.registerBlock(
                        regId,
                        ApricornFruitBlock::new,
                        p -> p.mapColor(MapColor.COLOR_ORANGE).strength(0.2F).sound(SoundType.WOOD)
                                .noOcclusion().noCollision().randomTicks()
                                .pushReaction(PushReaction.DESTROY)
                );
                case CROP -> {
                    // Color mints use official age=0–7 + mint_stage models
                    boolean mintPlant = regId.equals("red_mint") || regId.equals("blue_mint")
                            || regId.equals("cyan_mint") || regId.equals("green_mint")
                            || regId.equals("pink_mint") || regId.equals("white_mint");
                    if (mintPlant) {
                        yield BLOCKS.registerBlock(
                                regId,
                                MintCropBlock::new,
                                p -> p.mapColor(MapColor.PLANT).strength(0.0F).sound(SoundType.GRASS)
                                        .noOcclusion().noCollision().randomTicks()
                                        .pushReaction(PushReaction.DESTROY)
                        );
                    }
                    yield BLOCKS.registerBlock(
                            regId,
                            Block::new,
                            p -> p.mapColor(MapColor.PLANT).strength(0.2F).sound(SoundType.GRASS)
                                    .noOcclusion().noCollision().randomTicks()
                                    .pushReaction(PushReaction.DESTROY)
                    );
                }
                case ORE -> BLOCKS.registerBlock(
                        regId,
                        Block::new,
                        p -> p.mapColor(MapColor.STONE).strength(3.0F, 3.0F).sound(SoundType.STONE)
                                .requiresCorrectToolForDrops()
                );
                case MEDICINE_DISPLAY -> BLOCKS.registerBlock(
                        regId,
                        Block::new,
                        p -> p.mapColor(MapColor.COLOR_RED).strength(0.5F).sound(SoundType.WOOD).noOcclusion()
                );
                default -> BLOCKS.registerBlock(regId, Block::new, ContentBlocks::stone);
            };
            BY_ID.put(regId, def);
        }
        Cobblemon.LOGGER.info("ContentBlocks: {} blocks (machines + woodset + crops + ores)", BY_ID.size());
    }

    private static BlockBehaviour.Properties machine(BlockBehaviour.Properties p) {
        return p.mapColor(MapColor.METAL).strength(3.5F, 8.0F).sound(SoundType.METAL)
                .noOcclusion().requiresCorrectToolForDrops();
    }

    private static BlockBehaviour.Properties wood(BlockBehaviour.Properties p) {
        return p.mapColor(MapColor.WOOD).strength(2.0F, 3.0F).sound(SoundType.WOOD).ignitedByLava();
    }

    private static BlockBehaviour.Properties stone(BlockBehaviour.Properties p) {
        return p.mapColor(MapColor.STONE).strength(1.5F, 6.0F).sound(SoundType.STONE);
    }

    /** {@code red_apricorn_sapling} → {@code red}; non-apricorn saplings default red. */
    private static String colorFromSaplingId(String regId) {
        if (regId.endsWith("_apricorn_sapling")) {
            return regId.substring(0, regId.length() - "_apricorn_sapling".length());
        }
        // potted_* or other saplings — still need a color for grower
        for (String c : new String[]{"black", "blue", "green", "pink", "red", "white", "yellow"}) {
            if (regId.contains(c)) {
                return c;
            }
        }
        return "red";
    }

    private ContentBlocks() {}
}

package com.cobblemon.mod.block;

import org.jspecify.annotations.Nullable;

import com.mojang.serialization.MapCodec;

import com.cobblemon.mod.block.entity.FossilMachineBlockEntity;
import com.cobblemon.mod.block.entity.ModBlockEntities;
import com.cobblemon.mod.block.entity.PastureBlockEntity;
import com.cobblemon.mod.party.PartyHelper;
import com.cobblemon.mod.party.PlayerParty;
import com.cobblemon.mod.species.OwnedMon;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Functional Cobblemon machines: healing, pasture storage, fossil processing.
 * Pasture: sneak-use deposits lead mon; use withdraws last mon.
 * Fossil: use with fossil item inserts; empty hand checks progress / takes result.
 */
public class MachineBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public enum Kind {
        HEAL, PASTURE, FOSSIL, RESTORATION, MONITOR, DISPLAY, GENERIC
    }

    public static final MapCodec<MachineBlock> CODEC = simpleCodec(props -> new MachineBlock(props, Kind.GENERIC));

    private static final VoxelShape SHAPE_FULL = Block.box(0, 0, 0, 16, 16, 16);
    private static final VoxelShape SHAPE_HEAL = Shapes.or(
            Block.box(0, 0, 0, 16, 10, 16),
            Block.box(1, 10, 0, 15, 14, 16)
    );
    private static final VoxelShape SHAPE_TALL = Block.box(1, 0, 1, 15, 16, 15);

    private final Kind kind;

    public MachineBlock(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public Kind kind() {
        return kind;
    }

    @Override
    public MapCodec<? extends MachineBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (kind) {
            case HEAL -> SHAPE_HEAL;
            case PASTURE, FOSSIL, RESTORATION, MONITOR -> SHAPE_TALL;
            default -> SHAPE_FULL;
        };
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return switch (kind) {
            case PASTURE -> new PastureBlockEntity(pos, state);
            case FOSSIL, RESTORATION -> new FossilMachineBlockEntity(pos, state);
            default -> null;
        };
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type
    ) {
        if (kind == Kind.FOSSIL || kind == Kind.RESTORATION) {
            // Client ticks progress so the floating countdown animates between server syncs
            return createTickerHelper(type, ModBlockEntities.FOSSIL_MACHINE.get(), FossilMachineBlockEntity::tick);
        }
        if (level.isClientSide()) {
            return null;
        }
        if (kind == Kind.PASTURE) {
            return createTickerHelper(type, ModBlockEntities.PASTURE.get(), (lvl, p, st, be) -> {
                if (be instanceof PastureBlockEntity pasture) {
                    pasture.serverTickHeal();
                }
            });
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> actual, BlockEntityType<E> expected, BlockEntityTicker<? super E> ticker
    ) {
        return expected == actual ? (BlockEntityTicker<A>) ticker : null;
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit
    ) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer sp)) {
            return InteractionResult.SUCCESS;
        }
        return switch (kind) {
            case HEAL -> {
                // Center-style: free full heal if a PC is adjacent; otherwise charge 3s cooldown via use
                boolean center = hasAdjacentPc(level, pos);
                if (!center && !canUseStandaloneHealer(sp)) {
                    long left = healerCooldownLeft(sp);
                    sp.sendSystemMessage(Component.literal(
                            "§eHealing machine recharging… §f" + left + "s §7(place next to a PC for free heals)."));
                    yield InteractionResult.FAIL;
                }
                PartyHelper.healAll(sp);
                if (!center) {
                    markStandaloneHealerUsed(sp);
                }
                level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.6f, 1.2f);
                sp.sendSystemMessage(Component.literal(center
                        ? "§aPokémon Center heal complete!"
                        : "§aHealed! §7(standalone — 60s cooldown, or place beside a PC)"));
                yield InteractionResult.SUCCESS;
            }
            case PASTURE -> {
                // Sneak = deposit lead; bare use = list, second use withdraws last
                if (player.isShiftKeyDown()) {
                    yield pastureDeposit(level, pos, sp);
                }
                if (level.getBlockEntity(pos) instanceof PastureBlockEntity pasture) {
                    if (pasture.size() == 0) {
                        pasture.sendListing(sp);
                        yield InteractionResult.SUCCESS;
                    }
                    // List first; if player is holding nothing special and pasture has mons, withdraw
                    if (!player.getMainHandItem().isEmpty()) {
                        pasture.sendListing(sp);
                        yield InteractionResult.SUCCESS;
                    }
                    yield pastureWithdraw(level, pos, sp);
                }
                yield InteractionResult.FAIL;
            }
            case FOSSIL, RESTORATION -> fossilEmptyHand(level, pos, sp);
            case MONITOR -> {
                // Field scanner: party + nearby wild density
                PlayerParty party = PartyHelper.get(sp);
                sp.sendSystemMessage(Component.literal("§b§lPokémon Monitor"));
                sp.sendSystemMessage(Component.literal("Party: " + party.size() + "/6"
                        + (party.isEmpty() ? " (empty)" : "")));
                if (!party.isEmpty()) {
                    party.get(0).ifPresent(lead -> sp.sendSystemMessage(Component.literal(
                            "Lead: " + lead.displayName().getString()
                                    + " Lv." + lead.level()
                                    + " HP " + lead.hp() + "/" + lead.maxHp()
                    )));
                }
                int wild = 0;
                for (var e : level.getEntitiesOfClass(
                        com.cobblemon.mod.entity.WildMonEntity.class,
                        player.getBoundingBox().inflate(48)
                )) {
                    wild++;
                }
                sp.sendSystemMessage(Component.literal("Wild mons nearby (48 blocks): " + wild));
                sp.sendSystemMessage(Component.literal("PC storage: use a PC block for boxes."));
                level.playSound(null, pos, SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.BLOCKS, 0.5f, 1.4f);
                yield InteractionResult.SUCCESS;
            }
            case DISPLAY -> {
                // Showcase lead mon summary on the case
                PlayerParty party = PartyHelper.get(sp);
                if (party.isEmpty()) {
                    sp.sendSystemMessage(Component.literal("Display Case: empty — need a mon in party."));
                } else {
                    party.get(0).ifPresent(lead -> {
                        sp.sendSystemMessage(Component.literal("§e§lDisplay Case"));
                        sp.sendSystemMessage(Component.literal(
                                lead.displayName().getString() + "  ·  Lv." + lead.level()
                                        + "  ·  " + lead.primaryType().englishName()
                        ));
                        sp.sendSystemMessage(Component.literal(
                                "HP " + lead.hp() + "/" + lead.maxHp()
                                        + "  ·  Nature " + lead.nature().getSerializedName()
                                        + (lead.hasHeldItem() ? "  ·  Held " + lead.heldItem() : "")
                        ));
                        StringBuilder moves = new StringBuilder("Moves: ");
                        var ids = lead.moveIds();
                        for (int i = 0; i < ids.size(); i++) {
                            if (i > 0) moves.append(", ");
                            moves.append(ids.get(i))
                                    .append(" (")
                                    .append(lead.movePp(i)).append("/").append(lead.moveMaxPp(i))
                                    .append(")");
                        }
                        sp.sendSystemMessage(Component.literal(moves.toString()));
                    });
                }
                level.playSound(null, pos, SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.4f, 1.2f);
                yield InteractionResult.SUCCESS;
            }
            default -> {
                sp.sendSystemMessage(Component.translatable("message.cobblemon.machine_generic"));
                yield InteractionResult.SUCCESS;
            }
        };
    }

    @Override
    protected InteractionResult useItemOn(
            ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit
    ) {
        if (level.isClientSide()) {
            // Server handles real logic; still swing so the click feels responsive
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer sp)) {
            return InteractionResult.PASS;
        }
        if (kind == Kind.FOSSIL || kind == Kind.RESTORATION) {
            // Insert fossil / organic if held; otherwise collect result / show status.
            if (!stack.isEmpty()) {
                String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
                if (id.contains("fossil") && !id.contains("analyzer") && !id.contains("machine")) {
                    InteractionResult inserted = fossilInsert(level, pos, sp, stack);
                    if (inserted.consumesAction()) {
                        return inserted;
                    }
                }
                if (level.getBlockEntity(pos) instanceof FossilMachineBlockEntity machine) {
                    int organicVal = FossilMachineBlockEntity.organicValue(stack);
                    if (organicVal > 0) {
                        int took = machine.insertOrganic(stack);
                        if (took > 0) {
                            if (!sp.getAbilities().instabuild) {
                                stack.shrink(took);
                            }
                            sp.sendSystemMessage(Component.literal(
                                    "§aOrganic +" + (took * organicVal) + " §7(" + machine.organic() + "/"
                                            + FossilMachineBlockEntity.ORGANIC_NEEDED + ")"));
                            level.playSound(null, pos, SoundEvents.COMPOSTER_FILL, SoundSource.BLOCKS, 0.7f, 1.0f);
                            return InteractionResult.SUCCESS;
                        }
                    }
                }
            }
            return fossilEmptyHand(level, pos, sp);
        }
        // Let empty-hand path run for heal / pasture / monitor / display
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    private InteractionResult pastureDeposit(Level level, BlockPos pos, ServerPlayer sp) {
        if (!(level.getBlockEntity(pos) instanceof PastureBlockEntity pasture)) {
            return InteractionResult.FAIL;
        }
        if (pasture.isFull()) {
            sp.sendSystemMessage(Component.translatable("message.cobblemon.pasture_full"));
            return InteractionResult.FAIL;
        }
        PlayerParty party = PartyHelper.get(sp).copy();
        if (party.size() <= 1) {
            sp.sendSystemMessage(Component.translatable("message.cobblemon.pasture_keep_one"));
            return InteractionResult.FAIL;
        }
        var monOpt = party.release(0);
        if (monOpt.isEmpty() || !pasture.deposit(monOpt.get())) {
            monOpt.ifPresent(party::add);
            PartyHelper.set(sp, party);
            return InteractionResult.FAIL;
        }
        PartyHelper.set(sp, party);
        sp.sendSystemMessage(Component.translatable("message.cobblemon.pasture_deposit", monOpt.get().displayName()));
        level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 0.7f, 1.0f);
        return InteractionResult.SUCCESS;
    }

    private InteractionResult pastureWithdraw(Level level, BlockPos pos, ServerPlayer sp) {
        if (!(level.getBlockEntity(pos) instanceof PastureBlockEntity pasture)) {
            return InteractionResult.FAIL;
        }
        if (pasture.size() == 0) {
            sp.sendSystemMessage(pasture.statusLine());
            return InteractionResult.SUCCESS;
        }
        if (PartyHelper.get(sp).isFull()) {
            sp.sendSystemMessage(Component.translatable("message.cobblemon.party_full"));
            return InteractionResult.FAIL;
        }
        var monOpt = pasture.withdraw(pasture.size() - 1);
        if (monOpt.isEmpty() || !PartyHelper.addMon(sp, monOpt.get())) {
            monOpt.ifPresent(pasture::deposit);
            return InteractionResult.FAIL;
        }
        sp.sendSystemMessage(Component.translatable("message.cobblemon.pasture_withdraw", monOpt.get().displayName()));
        level.playSound(null, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 0.7f, 1.0f);
        return InteractionResult.SUCCESS;
    }

    private InteractionResult fossilEmptyHand(Level level, BlockPos pos, ServerPlayer sp) {
        if (!(level.getBlockEntity(pos) instanceof FossilMachineBlockEntity machine)) {
            return InteractionResult.FAIL;
        }
        if (machine.hasResult()) {
            if (PartyHelper.get(sp).isFull()) {
                sp.sendSystemMessage(Component.translatable("message.cobblemon.party_full"));
                return InteractionResult.FAIL;
            }
            OwnedMon mon = machine.takeResult();
            if (mon == null) {
                sp.sendSystemMessage(Component.literal("§cFossil machine had no mon — try again."));
                return InteractionResult.FAIL;
            }
            if (!PartyHelper.addMon(sp, mon)) {
                machine.forceResult(mon);
                sp.sendSystemMessage(Component.translatable("message.cobblemon.party_full"));
                return InteractionResult.FAIL;
            }
            sp.sendSystemMessage(Component.translatable("message.cobblemon.fossil_ready", mon.displayName()));
            sp.sendSystemMessage(Component.literal(
                    "§a+" + mon.displayName().getString() + " §7Lv." + mon.level() + " added to party!"
            ));
            level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 0.8f, 1.0f);
            return InteractionResult.SUCCESS;
        }
        if (machine.hasFossils()) {
            if (!machine.canProcess()) {
                if (machine.isMultiBlock() && machine.organic() < FossilMachineBlockEntity.ORGANIC_NEEDED) {
                    sp.sendSystemMessage(Component.literal(
                            "§eNeed organic materials: §f" + machine.organic() + "/"
                                    + FossilMachineBlockEntity.ORGANIC_NEEDED
                                    + " §7(bones, crops, meat, kelp…)"));
                } else if (machine.fossilIds().size() == 1
                        && machine.fossilIds().get(0).contains("fossilized")) {
                    sp.sendSystemMessage(Component.literal(
                            "§eGalar fossil: need a second fossilized piece (bird/drake/fish/dino)."));
                } else {
                    sp.sendSystemMessage(Component.literal("§eMachine not ready yet."));
                }
                return InteractionResult.SUCCESS;
            }
            int remain = machine.remainingTicks();
            int sec = (int) Math.ceil(remain / 20.0);
            int pct = (int) (100f * machine.progress() / Math.max(1, machine.maxProgress()));
            sp.sendSystemMessage(Component.literal(
                    (machine.isMultiBlock() ? "§bMulti-block lab · " : "§eSolo analyzer · ")
                            + "Restoring… §f" + (sec / 60) + ":" + String.format("%02d", sec % 60)
                            + " §7(" + pct + "%) · fossils " + machine.fossilIds().size()
            ));
            return InteractionResult.SUCCESS;
        }
        sp.sendSystemMessage(Component.literal(
                "§7Insert a fossil" + (machine.isMultiBlock()
                        ? " + organic materials (multi-block: analyzer + tank)."
                        : ".")
                        + " Galar pairs need two fossilized pieces."));
        return InteractionResult.SUCCESS;
    }

    private InteractionResult fossilInsert(Level level, BlockPos pos, ServerPlayer sp, ItemStack stack) {
        if (!(level.getBlockEntity(pos) instanceof FossilMachineBlockEntity machine)) {
            return InteractionResult.PASS;
        }
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        if (!id.contains("fossil")) {
            return InteractionResult.PASS;
        }
        if (machine.insertFossil(id)) {
            if (!sp.getAbilities().instabuild) {
                stack.shrink(1);
            }
            sp.sendSystemMessage(Component.translatable("message.cobblemon.fossil_inserted"));
            if (id.contains("fossilized") && machine.fossilIds().size() < 2) {
                sp.sendSystemMessage(Component.literal("§7Insert matching fossilized piece for Galar combo."));
            } else {
                sp.sendSystemMessage(Component.literal("§7Right-click when timer hits §aReady!§7."));
            }
            level.playSound(null, pos, SoundEvents.UI_STONECUTTER_TAKE_RESULT, SoundSource.BLOCKS, 0.7f, 1.0f);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.FAIL;
    }

    private static final java.util.Map<java.util.UUID, Long> HEALER_COOLDOWN = new java.util.concurrent.ConcurrentHashMap<>();
    private static final long HEALER_COOLDOWN_MS = 60_000L;

    private static boolean hasAdjacentPc(Level level, BlockPos pos) {
        for (Direction d : Direction.values()) {
            var id = BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos.relative(d)).getBlock());
            if (id != null && "cobblemon".equals(id.getNamespace()) && id.getPath().equals("pc")) {
                return true;
            }
        }
        // Also accept monitor as "center desk"
        for (Direction d : Direction.values()) {
            var id = BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos.relative(d)).getBlock());
            if (id != null && "cobblemon".equals(id.getNamespace())
                    && (id.getPath().equals("monitor") || id.getPath().contains("healing"))) {
                return true;
            }
        }
        return false;
    }

    private static boolean canUseStandaloneHealer(ServerPlayer sp) {
        Long last = HEALER_COOLDOWN.get(sp.getUUID());
        if (last == null) {
            return true;
        }
        return System.currentTimeMillis() - last >= HEALER_COOLDOWN_MS;
    }

    private static long healerCooldownLeft(ServerPlayer sp) {
        Long last = HEALER_COOLDOWN.get(sp.getUUID());
        if (last == null) {
            return 0;
        }
        long left = HEALER_COOLDOWN_MS - (System.currentTimeMillis() - last);
        return Math.max(0, (left + 999) / 1000);
    }

    private static void markStandaloneHealerUsed(ServerPlayer sp) {
        HEALER_COOLDOWN.put(sp.getUUID(), System.currentTimeMillis());
    }

    public static Kind kindForId(String id) {
        if (id == null) return null;
        return switch (id) {
            case "healing_machine" -> Kind.HEAL;
            case "pasture" -> Kind.PASTURE;
            case "fossil_analyzer" -> Kind.FOSSIL;
            case "restoration_tank" -> Kind.RESTORATION;
            case "monitor" -> Kind.MONITOR;
            case "display_case" -> Kind.DISPLAY;
            default -> null;
        };
    }
}

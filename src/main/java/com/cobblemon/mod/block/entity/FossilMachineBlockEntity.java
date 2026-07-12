package com.cobblemon.mod.block.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.cobblemon.mod.species.FossilRecipes;
import com.cobblemon.mod.species.MonSpecies;
import com.cobblemon.mod.species.OwnedMon;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Fossil analyzer / restoration tank.
 * <p>
 * Full Cobblemon multi-block: Analyzer + Restoration Tank + Monitor adjacent.
 * Organic fill (value 64), 1–2 fossils (Galar pairs), then incubate.
 */
public class FossilMachineBlockEntity extends BlockEntity {
    /** Full multi-block process ~3 minutes; solo analyzer stays 30s for accessibility. */
    public static final int PROCESS_TICKS_MULTI = 20 * 180;
    public static final int PROCESS_TICKS_SOLO = 20 * 30;
    public static final int ORGANIC_NEEDED = 64;

    private final List<String> fossilIds = new ArrayList<>();
    private int organic;
    private int progress;
    private OwnedMon result;
    private boolean multiBlock;

    public FossilMachineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FOSSIL_MACHINE.get(), pos, state);
    }

    public boolean hasFossils() {
        return !fossilIds.isEmpty();
    }

    public boolean hasResult() {
        return result != null;
    }

    public int progress() {
        return progress;
    }

    public int maxProgress() {
        return multiBlock ? PROCESS_TICKS_MULTI : PROCESS_TICKS_SOLO;
    }

    public int organic() {
        return organic;
    }

    public int organicNeeded() {
        return multiBlock ? ORGANIC_NEEDED : 0;
    }

    public boolean isMultiBlock() {
        return multiBlock;
    }

    public List<String> fossilIds() {
        return List.copyOf(fossilIds);
    }

    public int remainingTicks() {
        if (hasResult() || !hasFossils() || !canProcess()) {
            return 0;
        }
        return Math.max(0, maxProgress() - progress);
    }

    public boolean canProcess() {
        if (fossilIds.isEmpty() || hasResult()) {
            return false;
        }
        if (multiBlock && organic < ORGANIC_NEEDED) {
            return false;
        }
        // Galar dual-fossil recipes need both pieces
        if (needsSecondFossil() && fossilIds.size() < 2) {
            return false;
        }
        return true;
    }

    private boolean needsSecondFossil() {
        if (fossilIds.isEmpty()) {
            return false;
        }
        String a = fossilIds.get(0).toLowerCase(Locale.ROOT);
        return a.contains("fossilized");
    }

    /** Insert organic material; returns amount consumed. */
    public int insertOrganic(ItemStack stack) {
        if (stack.isEmpty() || hasResult()) {
            return 0;
        }
        int value = organicValue(stack);
        if (value <= 0) {
            return 0;
        }
        int room = ORGANIC_NEEDED - organic;
        if (room <= 0) {
            return 0;
        }
        int need = Math.max(1, (room + value - 1) / value); // stacks needed at least 1
        int take = Math.min(stack.getCount(), Math.max(1, room / Math.max(1, value)));
        if (take <= 0) {
            take = 1;
        }
        take = Math.min(take, stack.getCount());
        organic = Math.min(ORGANIC_NEEDED, organic + take * value);
        markUpdated();
        return take;
    }

    public static int organicValue(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        // Cobblemon / vanilla organics
        if (id.contains("bone") || id.contains("rotten") || id.contains("flesh")) {
            return 8;
        }
        if (id.contains("kelp") || id.contains("seagrass") || id.contains("moss") || id.contains("leaves")) {
            return 4;
        }
        if (id.contains("sapling") || id.contains("seed") || id.contains("wheat") || id.contains("carrot")
                || id.contains("potato") || id.contains("beet") || id.contains("berry") || id.contains("apple")
                || id.contains("melon") || id.contains("pumpkin") || id.contains("sugar_cane") || id.contains("cactus")
                || id.contains("mushroom") || id.contains("nether_wart") || id.contains("cocoa")
                || id.contains("sweet_berries") || id.contains("glow_berries")) {
            return 2;
        }
        if (id.contains("organic") || id.contains("compost") || id.contains("mulch") || id.contains("herb")
                || id.contains("medicinal") || id.contains("vivichoke") || id.contains("leek")) {
            return 8;
        }
        // Meat
        if (id.contains("beef") || id.contains("pork") || id.contains("chicken") || id.contains("mutton")
                || id.contains("rabbit") || id.contains("cod") || id.contains("salmon") || id.contains("fish")) {
            return 6;
        }
        return 0;
    }

    public boolean insertFossil(String itemId) {
        if (hasResult() || itemId == null || !itemId.toLowerCase(Locale.ROOT).contains("fossil")) {
            return false;
        }
        String id = itemId.toLowerCase(Locale.ROOT);
        if (id.contains("analyzer") || id.contains("machine")) {
            return false;
        }
        if (fossilIds.size() >= 2) {
            return false;
        }
        // Don't duplicate same fossil
        if (fossilIds.contains(id)) {
            return false;
        }
        // If first is non-galar single fossil, only one slot
        if (fossilIds.size() == 1 && !needsSecondFossil() && !id.contains("fossilized")) {
            return false;
        }
        fossilIds.add(id);
        progress = 0;
        markUpdated();
        return true;
    }

    public OwnedMon takeResult() {
        OwnedMon out = result;
        result = null;
        fossilIds.clear();
        progress = 0;
        // keep leftover organic for next run (half waste)
        organic = Math.max(0, organic / 2);
        markUpdated();
        return out;
    }

    public void forceResult(OwnedMon mon) {
        this.result = mon;
        markUpdated();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, FossilMachineBlockEntity be) {
        be.multiBlock = be.detectMultiBlock(level, pos);
        if (!be.hasFossils() || be.hasResult()) {
            return;
        }
        if (!be.canProcess()) {
            return;
        }
        be.progress++;
        if (level.isClientSide()) {
            if (be.progress > be.maxProgress()) {
                be.progress = be.maxProgress();
            }
            return;
        }
        if (be.progress >= be.maxProgress()) {
            String species = be.resolveSpecies();
            OwnedMon mon = OwnedMon.createWild(species, 20 + level.getRandom().nextInt(11), level.getRandom());
            be.result = mon;
            be.fossilIds.clear();
            be.progress = 0;
            be.organic = Math.max(0, be.organic - ORGANIC_NEEDED);
            be.markUpdated();
        } else if (be.progress % 20 == 0) {
            be.markUpdated();
        }
    }

    private String resolveSpecies() {
        if (fossilIds.size() >= 2) {
            String a = fossilIds.get(0);
            String b = fossilIds.get(1);
            String pair = galarPair(a, b);
            if (pair != null) {
                return pair;
            }
        }
        if (!fossilIds.isEmpty()) {
            var opt = FossilRecipes.speciesForFossilItem(fossilIds.get(0));
            if (opt.isPresent()) {
                return opt.get();
            }
        }
        return speciesForFossilFallback(fossilIds.isEmpty() ? "" : fossilIds.get(0), RandomSource.create()).id();
    }

    /** Bird+Drake=Dracozolt, Fish+Drake=Dracovish, Dino+Bird=Arctozolt, Fish+Dino=Arctovish */
    private static @Nullable String galarPair(String a, String b) {
        String x = a.toLowerCase(Locale.ROOT);
        String y = b.toLowerCase(Locale.ROOT);
        boolean bird = x.contains("bird") || y.contains("bird");
        boolean fish = x.contains("fish") || y.contains("fish");
        boolean drake = x.contains("drake") || y.contains("drake");
        boolean dino = x.contains("dino") || y.contains("dino");
        if (bird && drake) return "dracozolt";
        if (fish && drake) return "dracovish";
        if (dino && bird) return "arctozolt";
        if (fish && dino) return "arctovish";
        return null;
    }

    private boolean detectMultiBlock(Level level, BlockPos pos) {
        boolean hasTank = false;
        boolean hasAnalyzer = false;
        boolean hasMonitor = false;
        for (Direction d : Direction.values()) {
            BlockPos n = pos.relative(d);
            BlockState st = level.getBlockState(n);
            var id = BuiltInRegistries.BLOCK.getKey(st.getBlock());
            if (id == null || !"cobblemon".equals(id.getNamespace())) {
                continue;
            }
            String p = id.getPath();
            if (p.equals("restoration_tank")) hasTank = true;
            if (p.equals("fossil_analyzer")) hasAnalyzer = true;
            if (p.equals("monitor") || p.equals("data_monitor")) hasMonitor = true;
        }
        // Also check self kind
        var selfId = BuiltInRegistries.BLOCK.getKey(getBlockState().getBlock());
        if (selfId != null) {
            String p = selfId.getPath();
            if (p.equals("restoration_tank")) hasTank = true;
            if (p.equals("fossil_analyzer")) hasAnalyzer = true;
            if (p.equals("monitor")) hasMonitor = true;
        }
        // Full multi = analyzer + tank (monitor optional but preferred)
        return hasTank && hasAnalyzer;
    }

    private static MonSpecies speciesForFossilFallback(String fossilId, RandomSource random) {
        String id = fossilId == null ? "" : fossilId.toLowerCase(Locale.ROOT);
        if (id.contains("helix") || id.contains("dome")) return MonSpecies.OMANYTE;
        if (id.contains("old_amber") || id.contains("amber")) return MonSpecies.AERODACTYL;
        if (id.contains("kabuto") || id.contains("root") || id.contains("claw")) return MonSpecies.KABUTO;
        MonSpecies[] gen = {MonSpecies.OMANYTE, MonSpecies.KABUTO, MonSpecies.AERODACTYL};
        return gen[random.nextInt(gen.length)];
    }

    private void markUpdated() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            BlockState st = getBlockState();
            level.sendBlockUpdated(worldPosition, st, st, 3);
        }
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveCustomOnly(registries);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("Organic", organic);
        output.putInt("Progress", progress);
        output.putBoolean("Multi", multiBlock);
        output.putBoolean("HasResult", result != null);
        // Fossils as comma-separated for simplicity
        output.putString("Fossils", String.join(",", fossilIds));
        // Legacy single field
        output.putString("FossilId", fossilIds.isEmpty() ? "" : fossilIds.get(0));
        if (result != null) {
            OwnedMon.CODEC.encodeStart(NbtOps.INSTANCE, result).result().ifPresent(tag -> {
                if (tag instanceof CompoundTag ct) {
                    output.store("Result", CompoundTag.CODEC, ct);
                }
            });
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        organic = input.getIntOr("Organic", 0);
        progress = input.getIntOr("Progress", 0);
        multiBlock = input.getBooleanOr("Multi", false);
        fossilIds.clear();
        String multi = input.getStringOr("Fossils", "");
        if (!multi.isBlank()) {
            for (String s : multi.split(",")) {
                if (!s.isBlank()) {
                    fossilIds.add(s.trim());
                }
            }
        } else {
            String single = input.getStringOr("FossilId", "");
            if (!single.isBlank()) {
                fossilIds.add(single);
            }
        }
        result = null;
        input.read("Result", CompoundTag.CODEC).ifPresent(ct ->
                OwnedMon.CODEC.parse(NbtOps.INSTANCE, ct).result().ifPresent(m -> result = m));
    }
}

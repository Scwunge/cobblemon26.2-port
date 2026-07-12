package com.cobblemon.mod.block.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.cobblemon.mod.species.OwnedMon;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Pasture storage — up to {@link #CAPACITY} mons. Heals slowly while stored. */
public class PastureBlockEntity extends BlockEntity {
    public static final int CAPACITY = 24;
    private final List<OwnedMon> mons = new ArrayList<>();
    private int healTicks;

    public PastureBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PASTURE.get(), pos, state);
    }

    public List<OwnedMon> mons() {
        return List.copyOf(mons);
    }

    public int size() {
        return mons.size();
    }

    public boolean isFull() {
        return mons.size() >= CAPACITY;
    }

    public boolean deposit(OwnedMon mon) {
        if (isFull() || mon == null) {
            return false;
        }
        mons.add(mon);
        setChanged();
        return true;
    }

    public Optional<OwnedMon> withdraw(int index) {
        if (index < 0 || index >= mons.size()) {
            return Optional.empty();
        }
        OwnedMon mon = mons.remove(index);
        setChanged();
        return Optional.of(mon);
    }

    private int breedTicks;

    /** Called from server tick — heal stored mons every few seconds; breed compatible pairs. */
    public void serverTickHeal() {
        if (mons.isEmpty()) {
            return;
        }
        healTicks++;
        if (healTicks >= 40) {
            healTicks = 0;
            boolean changed = false;
            for (int i = 0; i < mons.size(); i++) {
                OwnedMon m = mons.get(i);
                if (m.isFainted()) {
                    mons.set(i, m.withHp(1).withStatus(com.cobblemon.mod.species.MonStatus.NONE));
                    changed = true;
                } else if (m.hp() < m.maxHp() || !m.status().isNone()) {
                    int heal = Math.max(1, m.maxHp() / 16);
                    OwnedMon next = m.withHp(Math.min(m.maxHp(), m.hp() + heal))
                            .withStatus(com.cobblemon.mod.species.MonStatus.NONE)
                            .restoreAllPp();
                    mons.set(i, next);
                    changed = true;
                } else {
                    OwnedMon topped = m.restoreAllPp();
                    if (topped != m) {
                        mons.set(i, topped);
                        changed = true;
                    }
                }
            }
            if (changed) {
                setChanged();
            }
        }
        // Breeding: every ~2 minutes with 2+ compatible opposite-gender mons
        breedTicks++;
        if (breedTicks >= 20 * 120 && !isFull() && mons.size() >= 2) {
            breedTicks = 0;
            tryBreed(net.minecraft.util.RandomSource.create());
        }
    }

    /**
     * If two opposite-gender mons of the same species (or both Ditto-adjacent) sit in the pasture,
     * produce an egg-level offspring at Lv.1 with mixed IVs.
     */
    private void tryBreed(net.minecraft.util.RandomSource random) {
        for (int i = 0; i < mons.size(); i++) {
            for (int j = i + 1; j < mons.size(); j++) {
                OwnedMon a = mons.get(i);
                OwnedMon b = mons.get(j);
                if (!canBreed(a, b)) {
                    continue;
                }
                if (random.nextFloat() > 0.35f) {
                    continue; // chance per check
                }
                // Offspring species = mother (female) when possible
                OwnedMon mother = a.gender() == com.cobblemon.mod.species.MonGender.FEMALE ? a
                        : (b.gender() == com.cobblemon.mod.species.MonGender.FEMALE ? b : a);
                // createWild rolls IVs; breedIvs mixes parents for a second roll preference
                OwnedMon baby = OwnedMon.createWild(mother.speciesId(), 1, random)
                        .withNickname("")
                        .withIvs(OwnedMon.breedIvs(a.ivs(), b.ivs(), random));
                if (deposit(baby)) {
                    return;
                }
            }
        }
    }

    private static boolean canBreed(OwnedMon a, OwnedMon b) {
        if (a == null || b == null) {
            return false;
        }
        // Opposite gender or genderless ditto-style (same species only for now)
        boolean genderOk =
                (a.gender() == com.cobblemon.mod.species.MonGender.MALE
                        && b.gender() == com.cobblemon.mod.species.MonGender.FEMALE)
                || (a.gender() == com.cobblemon.mod.species.MonGender.FEMALE
                        && b.gender() == com.cobblemon.mod.species.MonGender.MALE);
        if (!genderOk) {
            return false;
        }
        return a.speciesId().equalsIgnoreCase(b.speciesId());
    }

    public Component statusLine() {
        return Component.translatable("message.cobblemon.pasture_status", mons.size(), CAPACITY);
    }

    /** List stored mons for chat UI (up to 12 lines). */
    public void sendListing(net.minecraft.server.level.ServerPlayer player) {
        player.sendSystemMessage(statusLine());
        if (mons.isEmpty()) {
            player.sendSystemMessage(Component.literal("  (empty — sneak+use to deposit lead mon)"));
            return;
        }
        int show = Math.min(12, mons.size());
        for (int i = 0; i < show; i++) {
            OwnedMon m = mons.get(i);
            player.sendSystemMessage(Component.literal(
                    "  #" + (i + 1) + " " + m.displayName().getString()
                            + " Lv." + m.level()
                            + " HP " + m.hp() + "/" + m.maxHp()
            ));
        }
        if (mons.size() > show) {
            player.sendSystemMessage(Component.literal("  …and " + (mons.size() - show) + " more"));
        }
        player.sendSystemMessage(Component.literal("Use: withdraw last · Sneak+use: deposit lead"));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ListTag list = new ListTag();
        for (OwnedMon mon : mons) {
            OwnedMon.CODEC.encodeStart(NbtOps.INSTANCE, mon).result().ifPresent(list::add);
        }
        CompoundTag wrap = new CompoundTag();
        wrap.put("Mons", list);
        // store as nested compound via string for simplicity if no list API
        output.store("PastureData", CompoundTag.CODEC, wrap);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        mons.clear();
        input.read("PastureData", CompoundTag.CODEC).ifPresent(wrap -> {
            ListTag list = wrap.getListOrEmpty("Mons");
            for (int i = 0; i < list.size(); i++) {
                Tag t = list.get(i);
                OwnedMon.CODEC.parse(NbtOps.INSTANCE, t).result().ifPresent(mons::add);
            }
        });
    }
}

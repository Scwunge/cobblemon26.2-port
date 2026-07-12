package com.cobblemon.mod.item;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.cobblemon.mod.party.PartyHelper;
import com.cobblemon.mod.species.MonForm;
import com.cobblemon.mod.species.OwnedMon;
import com.cobblemon.mod.species.SpeciesHandle;
import com.cobblemon.mod.species.StatBlock;
import com.cobblemon.mod.util.DataKeys;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * Pokémon egg — holds species + IV seeds + optional moves + hatch timer.
 * Hatches while sitting in a player inventory, or reports progress on right-click.
 */
public class PokemonEggItem extends Item {
    /** ~10 minutes of inventory time at 20 tps. */
    public static final int DEFAULT_HATCH_TICKS = 20 * 60 * 10;

    private static final String TAG_SPECIES = DataKeys.POKEMON_SPECIES_IDENTIFIER;
    private static final String TAG_HATCH = "HatchTicks";
    private static final String TAG_IVS = DataKeys.POKEMON_IVS;
    private static final String TAG_MOVES = DataKeys.POKEMON_PROPERTIES_MOVES;
    private static final String TAG_FORM = DataKeys.POKEMON_FORM_ID;
    private static final String TAG_SHINY = DataKeys.POKEMON_SHINY;
    private static final String TAG_MAX_HATCH = "MaxHatchTicks";

    public PokemonEggItem(Properties properties) {
        super(properties);
    }

    /** Build an egg stack from a pre-rolled Lv.1 offspring (pasture / debug). */
    public static ItemStack createFromOffspring(OwnedMon baby, int hatchTicks) {
        ItemStack stack = new ItemStack(ModItems.POKEMON_EGG.get());
        writeEggData(
                stack,
                baby.speciesId(),
                Math.max(20, hatchTicks),
                baby.ivs(),
                baby.moveIds(),
                baby.form().id(),
                baby.isShiny()
        );
        return stack;
    }

    /**
     * Breed helper: mother species, mixed IVs, inherited egg/level moves (E3);
     * form/shiny rolled at egg creation (1/4096).
     */
    public static ItemStack createFromBreed(OwnedMon parentA, OwnedMon parentB, RandomSource random) {
        OwnedMon mother = parentA.gender() == com.cobblemon.mod.species.MonGender.FEMALE ? parentA
                : (parentB.gender() == com.cobblemon.mod.species.MonGender.FEMALE ? parentB : parentA);
        int[] ivs = OwnedMon.breedIvs(parentA.ivs(), parentB.ivs(), random);
        MonForm form = MonForm.rollWild(random);
        List<String> moves = com.cobblemon.mod.battle.EggMoveInheritance.inherit(
                parentA, parentB, mother.speciesId(), random
        );
        ItemStack stack = new ItemStack(ModItems.POKEMON_EGG.get());
        writeEggData(
                stack,
                mother.speciesId(),
                DEFAULT_HATCH_TICKS,
                ivs,
                moves,
                form.id(),
                form == MonForm.SHINY
        );
        return stack;
    }

    public static void writeEggData(
            ItemStack stack,
            String speciesId,
            int hatchTicks,
            int[] ivs,
            List<String> moves,
            String formId,
            boolean shiny
    ) {
        CompoundTag tag = new CompoundTag();
        tag.putString(TAG_SPECIES, speciesId == null || speciesId.isBlank() ? "rattata" : speciesId);
        int hatch = Math.max(0, hatchTicks);
        tag.putInt(TAG_HATCH, hatch);
        tag.putInt(TAG_MAX_HATCH, Math.max(hatch, 1));
        int[] normalized = StatBlock.normalizeSix(ivs, 0, StatBlock.IV_MAX, 15);
        tag.putIntArray(TAG_IVS, normalized);
        if (moves != null && !moves.isEmpty()) {
            ListTag moveList = new ListTag();
            for (String m : moves) {
                if (m != null && !m.isBlank()) {
                    moveList.add(StringTag.valueOf(m));
                }
            }
            if (!moveList.isEmpty()) {
                tag.put(TAG_MOVES, moveList);
            }
        }
        if (formId != null && !formId.isBlank()) {
            tag.putString(TAG_FORM, formId);
        }
        if (shiny) {
            tag.putBoolean(TAG_SHINY, true);
        }
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static Optional<CompoundTag> eggTag(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return Optional.empty();
        }
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return Optional.empty();
        }
        CompoundTag tag = data.copyTag();
        if (tag.isEmpty()) {
            return Optional.empty();
        }
        // Accept modern Species or legacy species key
        if (!tag.contains(TAG_SPECIES) && !tag.contains("species")) {
            return Optional.empty();
        }
        return Optional.of(tag);
    }

    private static String readString(CompoundTag tag, String modern, String legacy, String fallback) {
        if (tag.contains(modern)) {
            return tag.getStringOr(modern, fallback);
        }
        if (tag.contains(legacy)) {
            return tag.getStringOr(legacy, fallback);
        }
        return fallback;
    }

    private static boolean readBool(CompoundTag tag, String modern, String legacy) {
        if (tag.contains(modern)) {
            return tag.getBooleanOr(modern, false);
        }
        if (tag.contains(legacy)) {
            return tag.getBooleanOr(legacy, false);
        }
        return false;
    }

    public static String speciesId(ItemStack stack) {
        return eggTag(stack)
                .map(t -> readString(t, TAG_SPECIES, "species", "rattata"))
                .orElse("rattata");
    }

    public static int hatchTicksRemaining(ItemStack stack) {
        return eggTag(stack).map(t -> t.getIntOr(TAG_HATCH, DEFAULT_HATCH_TICKS)).orElse(DEFAULT_HATCH_TICKS);
    }

    public static int maxHatchTicks(ItemStack stack) {
        return eggTag(stack).map(t -> t.getIntOr(TAG_MAX_HATCH, DEFAULT_HATCH_TICKS)).orElse(DEFAULT_HATCH_TICKS);
    }

    /** 0..1 progress toward hatch. */
    public static float hatchProgress(ItemStack stack) {
        int max = Math.max(1, maxHatchTicks(stack));
        int left = Math.max(0, hatchTicksRemaining(stack));
        return 1.0f - (left / (float) max);
    }

    private static void setHatchTicks(ItemStack stack, int ticks) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt(TAG_HATCH, Math.max(0, ticks)));
    }

    public static int[] readIvs(CompoundTag tag) {
        int[] out = new int[]{15, 15, 15, 15, 15, 15};
        if (tag == null) {
            return out;
        }
        int[] src = null;
        if (tag.contains(TAG_IVS)) {
            src = tag.getIntArray(TAG_IVS).orElse(null);
        }
        if (src == null && tag.contains("ivs")) {
            src = tag.getIntArray("ivs").orElse(null);
        }
        if (src != null) {
            for (int i = 0; i < 6 && i < src.length; i++) {
                out[i] = Math.max(0, Math.min(StatBlock.IV_MAX, src[i]));
            }
        }
        return out;
    }

    public static List<String> readMoves(CompoundTag tag) {
        List<String> out = new ArrayList<>();
        if (tag == null) {
            return out;
        }
        ListTag list = null;
        if (tag.contains(TAG_MOVES)) {
            list = tag.getListOrEmpty(TAG_MOVES);
        } else if (tag.contains("moves")) {
            list = tag.getListOrEmpty("moves");
        }
        if (list == null) {
            return out;
        }
        for (int i = 0; i < list.size() && out.size() < OwnedMon.MAX_MOVES; i++) {
            // ListTag string slots return Optional in 1.21.5+
            String id = list.getString(i).orElse("");
            if (!id.isBlank()) {
                out.add(id);
            }
        }
        return out;
    }

    /** Build the Lv.1 mon from egg data (does not consume the stack). */
    public static OwnedMon hatchMon(ItemStack stack, RandomSource random) {
        CompoundTag tag = eggTag(stack).orElseGet(CompoundTag::new);
        String species = speciesId(stack);
        int[] ivs = readIvs(tag);
        List<String> moves = readMoves(tag);
        String formId = readString(tag, TAG_FORM, "form", MonForm.NORMAL.id());
        boolean shiny = readBool(tag, TAG_SHINY, "shiny")
                || MonForm.SHINY.id().equalsIgnoreCase(formId);
        OwnedMon mon = OwnedMon.createWild(species, 1, random).withIvs(ivs).withNickname("");
        if (shiny) {
            mon = mon.withShiny(true);
        } else if (formId != null && !formId.isBlank() && !"normal".equalsIgnoreCase(formId)) {
            mon = mon.withForm(MonForm.byId(formId));
        }
        if (!moves.isEmpty()) {
            List<com.cobblemon.mod.battle.MonMove> resolved = new ArrayList<>();
            for (String id : moves) {
                resolved.add(com.cobblemon.mod.battle.MonMove.byIdOrDefault(id));
            }
            mon = mon.withMoves(resolved);
        }
        return mon;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }
        if (eggTag(stack).isEmpty()) {
            writeEggData(stack, "rattata", DEFAULT_HATCH_TICKS, StatBlock.rollIvs(level.getRandom()),
                    List.of(), MonForm.NORMAL.id(), false);
        }
        int left = hatchTicksRemaining(stack);
        String name = SpeciesHandle.of(speciesId(stack)).displayName().getString();
        if (left <= 0) {
            tryHatch(serverPlayer, stack);
            return InteractionResult.SUCCESS;
        }
        int pct = Math.round(hatchProgress(stack) * 100f);
        int seconds = left / 20;
        int min = seconds / 60;
        int sec = seconds % 60;
        serverPlayer.sendSystemMessage(Component.translatable(
                "message.cobblemon.egg_progress",
                name,
                pct,
                min,
                sec
        ));
        return InteractionResult.SUCCESS;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot slot) {
        if (!(entity instanceof ServerPlayer player)) {
            return;
        }
        if (eggTag(stack).isEmpty()) {
            return;
        }
        // Skip pure armor slots if the egg somehow lands there
        if (slot != null) {
            try {
                if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
                    return;
                }
            } catch (Throwable ignored) {
                // Type enum names may differ slightly across snapshots
            }
        }
        int left = hatchTicksRemaining(stack);
        if (left <= 0) {
            tryHatch(player, stack);
            return;
        }
        setHatchTicks(stack, left - 1);
        if (left - 1 <= 0) {
            tryHatch(player, stack);
        }
    }

    private static void tryHatch(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof PokemonEggItem)) {
            return;
        }
        OwnedMon mon = hatchMon(stack, player.getRandom());
        boolean stored = PartyHelper.addMonOrPc(player, mon);
        if (!stored) {
            player.sendSystemMessage(Component.translatable("message.cobblemon.egg_no_space", mon.displayName()));
            setHatchTicks(stack, 0);
            return;
        }
        stack.shrink(1);
        String shiny = mon.isShiny() ? " ★Shiny!" : "";
        player.sendSystemMessage(Component.translatable(
                "message.cobblemon.egg_hatched",
                mon.displayName()
        ).append(Component.literal(shiny)));
        player.level().playSound(null, player.blockPosition(), SoundEvents.TURTLE_EGG_HATCH,
                SoundSource.PLAYERS, 0.8f, 1.1f);
        player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP,
                SoundSource.PLAYERS, 0.5f, 1.4f);
    }

    @Override
    public Component getName(ItemStack stack) {
        if (eggTag(stack).isEmpty()) {
            return Component.translatable(this.getDescriptionId());
        }
        String name = SpeciesHandle.of(speciesId(stack)).displayName().getString();
        return Component.translatable("item.cobblemon.pokemon_egg_named", name);
    }
}

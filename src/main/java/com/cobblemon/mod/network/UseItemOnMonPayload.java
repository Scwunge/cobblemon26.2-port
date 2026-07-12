package com.cobblemon.mod.network;

import com.cobblemon.mod.Cobblemon;
import com.cobblemon.mod.content.ContentKind;
import com.cobblemon.mod.item.BerryFoodItem;
import com.cobblemon.mod.item.CandyItem;
import com.cobblemon.mod.item.EvolutionStoneItem;
import com.cobblemon.mod.item.MedicineBlockItem;
import com.cobblemon.mod.item.MedicineItem;
import com.cobblemon.mod.item.MintItem;
import com.cobblemon.mod.item.VitaminItem;
import com.cobblemon.mod.party.PartyHelper;
import com.cobblemon.mod.party.PlayerParty;
import com.cobblemon.mod.species.HeldItems;
import com.cobblemon.mod.species.MonStatus;
import com.cobblemon.mod.species.OwnedMon;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client → server: apply held item / medicine / candy / etc. to a specific party slot.
 */
public record UseItemOnMonPayload(int hand, int partySlot) implements CustomPacketPayload {
    public static final Type<UseItemOnMonPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Cobblemon.MOD_ID, "use_item_on_mon"));

    public static final StreamCodec<RegistryFriendlyByteBuf, UseItemOnMonPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, UseItemOnMonPayload::hand,
            ByteBufCodecs.VAR_INT, UseItemOnMonPayload::partySlot,
            UseItemOnMonPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(UseItemOnMonPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        InteractionHand h = payload.hand() == 1 ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack stack = player.getItemInHand(h);
        if (stack.isEmpty()) {
            return;
        }
        Item item = stack.getItem();
        String itemId = BuiltInRegistries.ITEM.getKey(item).getPath();

        PlayerParty party = PartyHelper.get(player).copy();
        if (!party.isValidSlot(payload.partySlot())) {
            player.sendSystemMessage(Component.translatable("message.cobblemon.party_action_failed"));
            return;
        }
        OwnedMon mon = party.get(payload.partySlot()).orElse(null);
        if (mon == null) {
            return;
        }

        OwnedMon after = apply(item, itemId, mon, player);
        if (after == mon) {
            player.sendSystemMessage(Component.translatable("message.cobblemon.medicine_no_effect"));
            return;
        }
        party.set(payload.partySlot(), after);
        PartyHelper.set(player, party);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 0.7f, 1.1f);
        player.sendSystemMessage(Component.translatable("message.cobblemon.medicine_used", after.displayName()));
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
    }

    /** Apply item effect to a mon. Returns same instance if no effect. */
    public static OwnedMon apply(Item item, String itemId, OwnedMon mon, ServerPlayer player) {
        // Medicine (flat / revive / status)
        if (item instanceof MedicineItem || item instanceof MedicineBlockItem || item instanceof BerryFoodItem
                || ContentKind.itemRole(itemId) == ContentKind.ItemRole.MEDICINE
                || ContentKind.itemRole(itemId) == ContentKind.ItemRole.BERRY) {
            return applyMedicine(itemId, mon);
        }
        if (item instanceof CandyItem || ContentKind.itemRole(itemId) == ContentKind.ItemRole.CANDY) {
            int levels = ContentKind.candyLevels(itemId);
            if (mon.level() >= OwnedMon.MAX_LEVEL) return mon;
            return mon.withLevel(mon.level() + levels);
        }
        if (item instanceof MintItem || ContentKind.itemRole(itemId) == ContentKind.ItemRole.MINT) {
            var nature = ContentKind.mintNature(itemId);
            if (mon.nature() == nature) return mon;
            return mon.withNature(nature);
        }
        if (item instanceof EvolutionStoneItem || ContentKind.itemRole(itemId) == ContentKind.ItemRole.EVOLUTION_STONE) {
            // Prefer datapack item_interact (eevee stones, etc.), then Gen1 gem enum
            var itemEvo = mon.tryItemEvolve(itemId);
            if (itemEvo.evolved()) {
                return itemEvo.mon();
            }
            var method = ContentKind.stoneMethod(itemId);
            var evo = mon.tryGemEvolve(method);
            return evo.evolved() ? evo.mon() : mon;
        }
        // Link Cable — trade evolutions without a second player (official Cobblemon)
        if (item instanceof com.cobblemon.mod.item.LinkCableItem
                || "link_cable".equalsIgnoreCase(itemId)) {
            var tradeEvo = mon.tryTradeEvolve();
            if (tradeEvo.evolved()) {
                player.sendSystemMessage(Component.literal(
                        "§dThe Link Cable glowed! §f" + mon.displayName().getString()
                                + " §devolved into §f" + tradeEvo.mon().displayName().getString() + "§d!"));
                return tradeEvo.mon();
            }
            return mon;
        }
        if (item instanceof VitaminItem || ContentKind.itemRole(itemId) == ContentKind.ItemRole.VITAMIN) {
            if (VitaminItem.isPpVitamin(itemId)) {
                // PP Up / PP Max: restore and bump via full restore all PP as stand-in
                OwnedMon next = itemId.toLowerCase().contains("max")
                        ? mon.restoreAllPp()
                        : mon.restorePp(-1, 5);
                if (next == mon) {
                    return mon;
                }
                player.sendSystemMessage(Component.literal(
                        "§a" + mon.displayName().getString() + "§7's moves gained PP!"));
                return next;
            }
            int[] g = VitaminItem.evGain(itemId);
            // Refuse if that EV is already maxed
            for (int i = 0; i < 6; i++) {
                if (g[i] > 0 && mon.ev(i) >= 252) {
                    player.sendSystemMessage(Component.literal("§cThat EV is already maxed (252)."));
                    return mon;
                }
            }
            OwnedMon next = mon.addEvs(g[0], g[1], g[2], g[3], g[4], g[5]);
            if (next == mon || java.util.Arrays.equals(next.evs(), mon.evs())) {
                player.sendSystemMessage(Component.literal("§cNo EV change (cap reached)."));
                return mon;
            }
            player.sendSystemMessage(Component.literal(
                    "§aEVs raised for " + mon.displayName().getString()
                            + " §7(HP " + next.ev(0) + " Atk " + next.ev(1) + " Def " + next.ev(2)
                            + " SpA " + next.ev(3) + " SpD " + next.ev(4) + " Spe " + next.ev(5)
                            + " · total " + (next.ev(0) + next.ev(1) + next.ev(2) + next.ev(3) + next.ev(4) + next.ev(5))
                            + "/510)"));
            return next;
        }
        // Give as held item
        if (item instanceof com.cobblemon.mod.item.HeldItemGiverItem || HeldItems.canHold(itemId)
                || ContentKind.itemRole(itemId) == ContentKind.ItemRole.HELD) {
            return mon.withHeldItem(itemId);
        }
        return mon;
    }

    public static OwnedMon applyMedicine(String itemId, OwnedMon mon) {
        if (itemId == null || mon == null) {
            return mon;
        }
        String id = itemId.toLowerCase();
        int code = ContentKind.medicineHealAmount(id);
        OwnedMon before = mon;
        OwnedMon cur = mon;

        // Revive items only work on fainted
        if (code == -2 || code == -3) {
            if (!cur.isFainted()) {
                return mon;
            }
            return code == -3 ? cur.revive(true) : cur.revive(false);
        }
        // Non-revive cannot target fainted (except max potions won't work on fainted)
        if (cur.isFainted() && code != -2 && code != -3) {
            return mon;
        }

        // Status cure first
        boolean cured = false;
        if (!cur.status().isNone() && cur.status().curedBy(id)) {
            cur = cur.withStatus(MonStatus.NONE);
            cured = true;
        } else if (id.equals("full_heal") || id.equals("full_restore") || id.equals("heal_powder")
                || id.equals("lum_berry") || id.equals("remedy")) {
            if (!cur.status().isNone()) {
                cur = cur.withStatus(MonStatus.NONE);
                cured = true;
            }
        }

        // Berry-specific heals
        if (id.equals("oran_berry")) {
            cur = cur.healBy(10);
        } else if (id.equals("sitrus_berry")) {
            cur = cur.healFraction(0.25f);
        } else if (id.equals("leppa_berry")) {
            cur = cur.restorePp(-1, 10);
        } else {
            cur = switch (code) {
                case -1 -> cur.healed();
                case -4 -> cur.healFraction(0.25f);
                case -10 -> cur.restorePp(-1, 10);
                case -11 -> cur.restoreOneMoveFullPp();
                case -12 -> {
                    OwnedMon m = cur;
                    for (int i = 0; i < m.moveIds().size(); i++) {
                        m = m.restorePp(i, 10);
                    }
                    yield m;
                }
                case -13 -> cur.restoreAllPp();
                case 0 -> cur;
                default -> code > 0 ? cur.healBy(code) : cur;
            };
        }

        if (id.equals("full_restore") && !cur.isFainted()) {
            cur = cur.healed();
        }
        // No-op if nothing changed (full HP + no status + full PP for pure cures)
        if (cur == before || (cur.hp() == before.hp() && cur.status() == before.status()
                && cur.movePpList().equals(before.movePpList()) && !cured)) {
            // still allow if cured flag or heal happened via new instance
            if (!cured && cur.hp() == before.hp() && cur.status() == before.status()) {
                boolean ppSame = true;
                for (int i = 0; i < before.moveIds().size() && i < cur.moveIds().size(); i++) {
                    if (before.movePp(i) != cur.movePp(i)) {
                        ppSame = false;
                        break;
                    }
                }
                if (ppSame && cur.status() == before.status() && cur.hp() == before.hp()) {
                    return mon;
                }
            }
        }
        return cur;
    }
}

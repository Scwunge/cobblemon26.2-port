package com.cobblemon.mod.network;

import com.cobblemon.mod.Cobblemon;
import com.cobblemon.mod.item.ModItems;
import com.cobblemon.mod.party.PartyHelper;
import com.cobblemon.mod.species.OwnedMon;
import com.cobblemon.mod.species.StarterCatalog;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ChooseStarterPayload(String speciesId) implements CustomPacketPayload {
    public static final Type<ChooseStarterPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Cobblemon.MOD_ID, "choose_starter"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ChooseStarterPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, ChooseStarterPayload::speciesId,
            ChooseStarterPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ChooseStarterPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        if (!PartyHelper.get(player).isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.cobblemon.already_started"));
            return;
        }
        String id = payload.speciesId() == null ? "" : payload.speciesId().trim().toLowerCase();
        if (!StarterCatalog.isValidStarter(id)) {
            player.sendSystemMessage(Component.translatable("message.cobblemon.invalid_starter"));
            return;
        }
        OwnedMon mon = OwnedMon.createWild(id, 5, RandomSource.create());
        // Starters always get ≥1 attack + ≥1 defensive 0-power move
        mon = mon.withMoveIds(com.cobblemon.mod.species.StarterMoves.forStarter(id, 5));
        PartyHelper.addMon(player, mon);
        // Starter kit — real Poké Balls when content pack registered, else cube balls
        giveBall(player, "poke_ball", 12);
        giveBall(player, "great_ball", 5);
        giveBall(player, "ultra_ball", 2);
        giveItem(player, "potion", 5);
        giveItem(player, "antidote", 3);
        player.getInventory().add(new ItemStack(ModItems.PARTY_BADGE.get(), 1));
        player.sendSystemMessage(Component.translatable("message.cobblemon.starter", mon.displayName()));
        player.sendSystemMessage(Component.translatable("message.cobblemon.starter_kit"));
    }

    private static void giveBall(ServerPlayer player, String id, int count) {
        var def = com.cobblemon.mod.content.ContentItems.BY_ID.get(id);
        if (def != null) {
            player.getInventory().add(new ItemStack(def.get(), count));
            return;
        }
        // fallback cube tiers
        if (id.contains("ultra")) {
            player.getInventory().add(new ItemStack(com.cobblemon.mod.item.CubeBallTier.ULTRA.item(), count));
        } else if (id.contains("great")) {
            player.getInventory().add(new ItemStack(com.cobblemon.mod.item.CubeBallTier.GREAT.item(), count));
        } else {
            player.getInventory().add(new ItemStack(com.cobblemon.mod.item.CubeBallTier.POKE.item(), count));
        }
    }

    private static void giveItem(ServerPlayer player, String id, int count) {
        var def = com.cobblemon.mod.content.ContentItems.BY_ID.get(id);
        if (def != null) {
            player.getInventory().add(new ItemStack(def.get(), count));
        }
    }
}

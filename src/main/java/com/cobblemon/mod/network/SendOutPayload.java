package com.cobblemon.mod.network;

import java.util.UUID;

import com.cobblemon.mod.Cobblemon;
import com.cobblemon.mod.entity.ModEntities;
import com.cobblemon.mod.entity.WildMonEntity;
import com.cobblemon.mod.party.PartyHelper;
import com.cobblemon.mod.party.PlayerParty;
import com.cobblemon.mod.species.OwnedMon;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client pressed R — send out / recall lead Cobblemon, and dismiss the tip client-side.
 */
public record SendOutPayload() implements CustomPacketPayload {
    public static final Type<SendOutPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Cobblemon.MOD_ID, "send_out"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SendOutPayload> STREAM_CODEC =
            StreamCodec.unit(new SendOutPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SendOutPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        // Don't recall mid-battle — battle presentation owns the companion entity
        if (com.cobblemon.mod.battle.BattleManager.inBattle(player)) {
            player.sendSystemMessage(Component.literal("§7Can't recall during battle — use Run or finish the fight."));
            return;
        }

        WildMonEntity existing = findCompanion(player);
        if (existing != null) {
            existing.discard();
            player.sendSystemMessage(Component.translatable("message.cobblemon.recalled", existing.getDisplayName()));
            return;
        }

        PlayerParty party = PartyHelper.get(player);
        if (party.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.cobblemon.party_empty"));
            return;
        }
        OwnedMon lead = party.lead().orElseThrow();

        WildMonEntity mon = ModEntities.WILD_MON.get().create(level, EntitySpawnReason.TRIGGERED);
        if (mon == null) {
            return;
        }
        mon.setPos(
                player.getX() + player.getLookAngle().x * 1.5,
                player.getY(),
                player.getZ() + player.getLookAngle().z * 1.5
        );
        mon.applyIdentity(lead);
        mon.setCompanionOwner(player.getUUID());
        mon.setCustomName(lead.displayName());
        mon.setCustomNameVisible(true);
        level.addFreshEntity(mon);

        player.sendSystemMessage(Component.translatable("message.cobblemon.sent_out", lead.displayName()));
    }

    /** Default search radius for recall / general companion lookup. */
    public static final double COMPANION_RANGE = 48.0;

    public static WildMonEntity findCompanion(ServerPlayer player) {
        return findCompanion(player, COMPANION_RANGE);
    }

    /**
     * Nearest owned companion within {@code range} blocks (center distance).
     */
    public static WildMonEntity findCompanion(ServerPlayer player, double range) {
        if (!(player.level() instanceof ServerLevel level)) {
            return null;
        }
        AABB box = player.getBoundingBox().inflate(range);
        WildMonEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (Entity e : level.getEntities(player, box, ent -> ent instanceof WildMonEntity)) {
            if (e instanceof WildMonEntity mon) {
                UUID owner = mon.getCompanionOwner();
                if (owner != null && owner.equals(player.getUUID())) {
                    double d = mon.distanceTo(player);
                    if (d <= range && d < bestDist) {
                        bestDist = d;
                        best = mon;
                    }
                }
            }
        }
        return best;
    }
}

package com.cobblemon.mod.network;

import com.cobblemon.mod.Cobblemon;
import com.cobblemon.mod.entity.WildMonEntity;
import com.cobblemon.mod.species.SpeciesHandle;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Mount / dismount the player's sent-out companion (riding).
 */
public record RidePayload() implements CustomPacketPayload {
    public static final Type<RidePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Cobblemon.MOD_ID, "ride"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RidePayload> STREAM_CODEC =
            StreamCodec.unit(new RidePayload());

    /** Max distance (blocks) to mount a sent-out companion. */
    public static final double MOUNT_RANGE = 4.0;

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RidePayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        // Already riding something — dismount
        if (player.isPassenger()) {
            player.stopRiding();
            player.sendSystemMessage(Component.literal("§7Dismounted."));
            return;
        }
        WildMonEntity companion = SendOutPayload.findCompanion(player, MOUNT_RANGE);
        if (companion == null) {
            player.sendSystemMessage(Component.literal(
                    "§cSend out a mon first (R), stand close (≤" + (int) MOUNT_RANGE + " blocks), then ride (V)."));
            return;
        }
        if (companion.distanceTo(player) > MOUNT_RANGE) {
            player.sendSystemMessage(Component.literal("§cToo far — walk closer to your mon to ride."));
            return;
        }
        if (companion.getMonLevel() < 5) {
            player.sendSystemMessage(Component.literal("§cMon must be at least Lv.5 to ride."));
            return;
        }
        SpeciesHandle handle = SpeciesHandle.of(companion.getSpeciesId());
        if (!handle.isRideable()) {
            player.sendSystemMessage(Component.literal(
                    "§c" + handle.displayName().getString() + " is too small to ride."));
            return;
        }
        // Mount companion
        boolean ok = player.startRiding(companion, true, true);
        if (ok) {
            player.sendSystemMessage(Component.literal(
                    "§aRiding " + companion.getDisplayName().getString() + " §7— V again to dismount."));
            var speed = companion.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
            if (speed != null) {
                speed.setBaseValue(0.28 + Math.min(0.20, companion.getMonLevel() * 0.004));
            }
        } else {
            player.sendSystemMessage(Component.literal("§cCouldn't mount."));
        }
    }
}

package com.cobblemon.mod.ride;

import com.cobblemon.mod.entity.WildMonEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityMountEvent;

/**
 * Keep Shift from dismounting mon mounts — Shift is dive / descend / land.
 * Players dismount with V (RidePayload) or intentional stopRiding.
 */
public final class RideEvents {
    private RideEvents() {}

    @SubscribeEvent
    public static void onMount(EntityMountEvent event) {
        if (!event.isDismounting()) {
            return;
        }
        if (!(event.getEntityBeingMounted() instanceof WildMonEntity mon)) {
            return;
        }
        if (!(event.getEntityMounting() instanceof Player player)) {
            return;
        }
        // Allow V-key / command dismounts
        if (RideController.isIntentionalDismount()) {
            return;
        }
        // Block vanilla Shift-to-dismount while on our mounts
        if (player.isShiftKeyDown()) {
            event.setCanceled(true);
            // Ensure dive input still works this tick (flight/swim already read shift)
            return;
        }
        // Also block accidental dismount if still controlling companion
        if (mon.isCompanion() && mon.getControllingPassenger() == player) {
            // Only cancel shift-style; other dismounts (death, teleport) may not set shift
            // Leave non-shift dismounts alone so death/eject still work
        }
    }
}

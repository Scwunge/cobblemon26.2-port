package com.cobblemon.mod.party;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * N3 deepen — soft progression gates based on gym badge count.
 * Not hard locks: messages + mild limits so early players aren't soft-blocked forever.
 */
public final class BadgeGates {
    private BadgeGates() {}

    /** Max wild level that is "fair" for this trainer's badge count. */
    public static int recommendedWildLevelCap(ServerPlayer player) {
        int badges = PlayerBadges.get(player).count();
        // 0 badges → 15, each badge +10, cap 100
        return Math.min(100, 15 + badges * 10);
    }

    /** True if catching this level is allowed (always true if under cap+10). */
    public static boolean canCatchLevel(ServerPlayer player, int monLevel) {
        int cap = recommendedWildLevelCap(player);
        // Soft: allow up to cap+15 so gates aren't brutal
        return monLevel <= cap + 15;
    }

    public static void warnIfOverleveled(ServerPlayer player, int monLevel) {
        int cap = recommendedWildLevelCap(player);
        if (monLevel > cap) {
            player.sendSystemMessage(Component.literal(
                    "§7This wild is above your badge level (cap ~" + cap
                            + "). Earn more gym badges for smoother fights!"
            ));
        }
    }

    /**
     * Friendship / high-level evolution soft gate: need N badges for level ≥ threshold.
     * Returns true if evolution is allowed.
     */
    public static boolean canLevelEvolve(ServerPlayer player, int intoLevel) {
        if (player == null) {
            return true;
        }
        int badges = PlayerBadges.get(player).count();
        if (intoLevel >= 50 && badges < 4) {
            return false;
        }
        if (intoLevel >= 36 && badges < 2) {
            return false;
        }
        return true;
    }

    public static Component evoBlockedMessage(int intoLevel) {
        if (intoLevel >= 50) {
            return Component.literal("§eNeeds 4 gym badges to evolve past Lv.50.");
        }
        return Component.literal("§eNeeds 2 gym badges to evolve past Lv.36.");
    }
}

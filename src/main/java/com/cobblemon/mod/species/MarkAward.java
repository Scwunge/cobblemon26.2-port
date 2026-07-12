package com.cobblemon.mod.species;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

/**
 * C2 — award catch/context marks onto a mon.
 * Mark ids match datapack {@code data/cobblemon/marks/*.json} filenames without extension.
 */
public final class MarkAward {
    private MarkAward() {}

    /**
     * Roll a mark for a freshly caught mon based on context.
     * @return mark id or empty string
     */
    public static String rollOnCatch(ServerPlayer player, OwnedMon mon, RandomSource random) {
        if (player == null || mon == null || random == null) {
            return "";
        }
        Level level = player.level();
        // Rare destiny mark
        if (random.nextFloat() < 0.01f) {
            return "mark_destiny";
        }
        // Time-of-day (night-ish by overworld clock)
        long dayTime = level.getOverworldClockTime() % 24000L;
        if (dayTime >= 13000L && dayTime < 23000L && random.nextFloat() < 0.04f) {
            return "mark_alpha"; // stand-in for night-ish rare mark
        }
        if (player.isInWater() && random.nextFloat() < 0.05f) {
            return "mark_fishing";
        }
        if (mon.isShiny() && random.nextFloat() < 0.15f) {
            return "mark_destiny";
        }
        if (random.nextFloat() < 0.03f) {
            return "mark_gourmand";
        }
        return "";
    }

    public static String displayName(String markId) {
        if (markId == null || markId.isBlank()) {
            return "";
        }
        String s = markId.startsWith("mark_") ? markId.substring(5) : markId;
        if (s.isEmpty()) {
            return markId;
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1) + " Mark";
    }
}

package com.cobblemon.mod.battle;

import com.cobblemon.mod.species.MonStatus;
import com.cobblemon.mod.species.OwnedMon;

/**
 * Held-berry auto-use during battle (pinch heals, status cures).
 */
public final class BattleBerry {
    private BattleBerry() {}

    public record Result(OwnedMon mon, boolean consumed, String message) {
        static Result none(OwnedMon mon) {
            return new Result(mon, false, null);
        }
    }

    /**
     * Try to consume the mon's held berry if conditions match.
     * Clears held item when used.
     */
    public static Result tryConsume(OwnedMon mon) {
        if (mon == null || !mon.hasHeldItem()) {
            return Result.none(mon);
        }
        String id = mon.heldItem().toLowerCase();
        if (!id.endsWith("_berry")) {
            return Result.none(mon);
        }

        // Status cures
        if (!mon.status().isNone() && mon.status().curedBy(id)) {
            return new Result(
                    mon.withStatus(MonStatus.NONE).withHeldItem(""),
                    true,
                    mon.displayName().getString() + " ate its " + pretty(id) + " and was cured!"
            );
        }
        if (id.equals("lum_berry") && !mon.status().isNone()) {
            return new Result(
                    mon.withStatus(MonStatus.NONE).withHeldItem(""),
                    true,
                    mon.displayName().getString() + " ate its Lum Berry and was cured!"
            );
        }

        // Pinch heals at ≤25% HP (Sitrus 25%, Oran flat 10)
        float ratio = mon.hpRatio();
        if (ratio > 0f && ratio <= 0.25f && !mon.isFainted()) {
            if (id.equals("sitrus_berry")) {
                OwnedMon next = mon.healFraction(0.25f).withHeldItem("");
                return new Result(next, true,
                        mon.displayName().getString() + " restored HP with its Sitrus Berry!");
            }
            if (id.equals("oran_berry")) {
                OwnedMon next = mon.healBy(10).withHeldItem("");
                return new Result(next, true,
                        mon.displayName().getString() + " restored HP with its Oran Berry!");
            }
            if (id.equals("figy_berry") || id.equals("wiki_berry") || id.equals("mago_berry")
                    || id.equals("aguav_berry") || id.equals("iapapa_berry")) {
                OwnedMon next = mon.healFraction(0.33f).withHeldItem("");
                return new Result(next, true,
                        mon.displayName().getString() + " restored HP with its " + pretty(id) + "!");
            }
        }

        // Leppa: restore PP when a move is empty
        if (id.equals("leppa_berry")) {
            for (int i = 0; i < mon.moveIds().size(); i++) {
                if (mon.movePp(i) <= 0) {
                    OwnedMon next = mon.restorePp(i, 10).withHeldItem("");
                    return new Result(next, true,
                            mon.displayName().getString() + " restored PP with its Leppa Berry!");
                }
            }
        }

        return Result.none(mon);
    }

    private static String pretty(String id) {
        String[] p = id.replace('_', ' ').split(" ");
        StringBuilder sb = new StringBuilder();
        for (String s : p) {
            if (s.isEmpty()) continue;
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(Character.toUpperCase(s.charAt(0)));
            if (s.length() > 1) sb.append(s.substring(1));
        }
        return sb.toString();
    }
}

package com.cobblemon.mod.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * EV vitamins / feathers — open mon picker (UseItemOnMonPayload applies EVs).
 */
public class VitaminItem extends Item {
    private final String contentId;

    public VitaminItem(Properties properties, String contentId) {
        super(properties);
        this.contentId = contentId;
    }

    public String contentId() {
        return contentId;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        return MonTargetingItem.openPicker(level, player, hand, player.getItemInHand(hand));
    }

    /** EV deltas for a vitamin id: hp, atk, def, spa, spd, spe */
    public static int[] evGain(String id) {
        if (id == null) {
            return new int[6];
        }
        String s = id.toLowerCase();
        // Wings = +1, vitamins = +10, power items = +8
        int amt = s.contains("wing") || s.contains("feather") ? 1
                : (s.contains("power_") ? 8 : 10);
        return switch (s) {
            case "hp_up", "health_feather", "health_wing", "power_weight" -> new int[]{amt == 8 ? 8 : (amt == 1 ? 1 : 10), 0, 0, 0, 0, 0};
            case "protein", "muscle_feather", "muscle_wing", "power_bracer" -> new int[]{0, amt == 8 ? 8 : (amt == 1 ? 1 : 10), 0, 0, 0, 0};
            case "iron", "resist_feather", "resist_wing", "power_belt" -> new int[]{0, 0, amt == 8 ? 8 : (amt == 1 ? 1 : 10), 0, 0, 0};
            case "calcium", "genius_feather", "genius_wing", "power_lens" -> new int[]{0, 0, 0, amt == 8 ? 8 : (amt == 1 ? 1 : 10), 0, 0};
            case "zinc", "clever_feather", "clever_wing", "power_band" -> new int[]{0, 0, 0, 0, amt == 8 ? 8 : (amt == 1 ? 1 : 10), 0};
            case "carbos", "swift_feather", "swift_wing", "power_anklet" -> new int[]{0, 0, 0, 0, 0, amt == 8 ? 8 : (amt == 1 ? 1 : 10)};
            case "pp_up" -> new int[]{-1, 0, 0, 0, 0, 0}; // special: PP max +1 (handled separately)
            case "pp_max" -> new int[]{-2, 0, 0, 0, 0, 0};
            default -> {
                if (s.contains("health") || s.contains("hp")) yield new int[]{10, 0, 0, 0, 0, 0};
                if (s.contains("muscle") || s.contains("protein") || s.contains("power_bracer")) yield new int[]{0, 10, 0, 0, 0, 0};
                if (s.contains("resist") || s.equals("iron") || s.contains("power_belt")) yield new int[]{0, 0, 10, 0, 0, 0};
                if (s.contains("genius") || s.contains("calcium") || s.contains("power_lens")) yield new int[]{0, 0, 0, 10, 0, 0};
                if (s.contains("clever") || s.contains("zinc") || s.contains("power_band")) yield new int[]{0, 0, 0, 0, 10, 0};
                if (s.contains("swift") || s.contains("carbos") || s.contains("anklet")) yield new int[]{0, 0, 0, 0, 0, 10};
                yield new int[]{4, 0, 0, 0, 0, 0};
            }
        };
    }

    public static boolean isPpVitamin(String id) {
        if (id == null) return false;
        String s = id.toLowerCase();
        return s.equals("pp_up") || s.equals("pp_max");
    }
}

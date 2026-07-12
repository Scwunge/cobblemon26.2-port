package com.cobblemon.mod.item;

import java.util.Locale;
import java.util.Optional;

import com.cobblemon.mod.Cobblemon;
import com.cobblemon.mod.content.ContentItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Every throwable Cobblemon Poké Ball — real item ids, catch mults, and 3D cube models.
 * Inventory icons stay 2D; world / throw use {@code models/item/*_ball_model.json}.
 */
public enum CubeBallTier implements StringRepresentable {
    // Standard
    POKE("poke_ball", 1.0f, "Poké Ball", true),
    GREAT("great_ball", 1.5f, "Great Ball", true),
    ULTRA("ultra_ball", 2.0f, "Ultra Ball", true),
    MASTER("master_ball", 255.0f, "Master Ball", true),
    SAFARI("safari_ball", 1.5f, "Safari Ball", true),
    // Gen 2 apricorn
    FAST("fast_ball", 4.0f, "Fast Ball", true),
    LEVEL("level_ball", 2.0f, "Level Ball", true),
    LURE("lure_ball", 3.0f, "Lure Ball", true),
    HEAVY("heavy_ball", 1.5f, "Heavy Ball", true),
    LOVE("love_ball", 8.0f, "Love Ball", true),
    FRIEND("friend_ball", 1.0f, "Friend Ball", true),
    MOON("moon_ball", 4.0f, "Moon Ball", true),
    SPORT("sport_ball", 1.5f, "Sport Ball", true),
    // Gen 3+
    NET("net_ball", 3.0f, "Net Ball", true),
    DIVE("dive_ball", 3.5f, "Dive Ball", true),
    NEST("nest_ball", 3.0f, "Nest Ball", true),
    REPEAT("repeat_ball", 3.0f, "Repeat Ball", true),
    TIMER("timer_ball", 3.0f, "Timer Ball", true),
    LUXURY("luxury_ball", 1.0f, "Luxury Ball", true),
    PREMIER("premier_ball", 1.0f, "Premier Ball", true),
    DUSK("dusk_ball", 3.0f, "Dusk Ball", true),
    HEAL("heal_ball", 1.0f, "Heal Ball", true),
    QUICK("quick_ball", 4.0f, "Quick Ball", true),
    // Gen 5+
    DREAM("dream_ball", 1.0f, "Dream Ball", true),
    BEAST("beast_ball", 5.0f, "Beast Ball", true),
    CHERISH("cherish_ball", 1.0f, "Cherish Ball", true),
    PARK("park_ball", 255.0f, "Park Ball", true),
    // Cobblemon apricorn dyes
    AZURE("azure_ball", 1.0f, "Azure Ball", true),
    CITRINE("citrine_ball", 1.0f, "Citrine Ball", true),
    ROSEATE("roseate_ball", 1.0f, "Roseate Ball", true),
    SLATE("slate_ball", 1.0f, "Slate Ball", true),
    VERDANT("verdant_ball", 1.0f, "Verdant Ball", true),
    // Ancient / Hisui
    ANCIENT_POKE("ancient_poke_ball", 1.0f, "Ancient Poké Ball", true),
    ANCIENT_GREAT("ancient_great_ball", 1.5f, "Ancient Great Ball", true),
    ANCIENT_ULTRA("ancient_ultra_ball", 2.0f, "Ancient Ultra Ball", true),
    ANCIENT_HEAVY("ancient_heavy_ball", 1.5f, "Ancient Heavy Ball", true),
    ANCIENT_LEADEN("ancient_leaden_ball", 1.5f, "Ancient Leaden Ball", true),
    ANCIENT_GIGATON("ancient_gigaton_ball", 2.0f, "Ancient Gigaton Ball", true),
    ANCIENT_FEATHER("ancient_feather_ball", 1.0f, "Ancient Feather Ball", true),
    ANCIENT_WING("ancient_wing_ball", 1.5f, "Ancient Wing Ball", true),
    ANCIENT_JET("ancient_jet_ball", 2.0f, "Ancient Jet Ball", true),
    ANCIENT_ORIGIN("ancient_origin_ball", 255.0f, "Ancient Origin Ball", true),
    ANCIENT_AZURE("ancient_azure_ball", 1.0f, "Ancient Azure Ball", true),
    ANCIENT_CITRINE("ancient_citrine_ball", 1.0f, "Ancient Citrine Ball", true),
    ANCIENT_ROSEATE("ancient_roseate_ball", 1.0f, "Ancient Roseate Ball", true),
    ANCIENT_SLATE("ancient_slate_ball", 1.0f, "Ancient Slate Ball", true),
    ANCIENT_VERDANT("ancient_verdant_ball", 1.0f, "Ancient Verdant Ball", true),
    ANCIENT_IVORY("ancient_ivory_ball", 1.0f, "Ancient Ivory Ball", true);

    private final String itemId;
    private final float catchMultiplier;
    private final String englishName;
    private final boolean has3dModel;

    CubeBallTier(String itemId, float catchMultiplier, String englishName, boolean has3dModel) {
        this.itemId = itemId;
        this.catchMultiplier = catchMultiplier;
        this.englishName = englishName;
        this.has3dModel = has3dModel;
    }

    public String id() {
        return itemId;
    }

    public String itemId() {
        return itemId;
    }

    public float catchMultiplier() {
        return catchMultiplier;
    }

    public boolean has3dModel() {
        return has3dModel;
    }

    /** Resource path for the 3D cube model (no namespace). */
    public String modelPath() {
        return "item/" + itemId + "_model";
    }

    public MutableComponent displayName() {
        return Component.translatable("item.cobblemon." + itemId);
    }

    public String englishName() {
        return englishName;
    }

    /** Resolve the registered item for this exact ball id. */
    public Item item() {
        var def = ContentItems.BY_ID.get(itemId);
        if (def != null) {
            return def.get();
        }
        Identifier key = Identifier.fromNamespaceAndPath(Cobblemon.MOD_ID, itemId);
        Item item = BuiltInRegistries.ITEM.getValue(key);
        return item != null && item != Items.AIR ? item : Items.SNOWBALL;
    }

    public ItemStack stack(int count) {
        return new ItemStack(item(), count);
    }

    @Override
    public String getSerializedName() {
        return itemId;
    }

    public static Optional<CubeBallTier> optional(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        String s = id.toLowerCase(Locale.ROOT).trim();
        // strip namespace
        int colon = s.indexOf(':');
        if (colon >= 0) {
            s = s.substring(colon + 1);
        }
        // held-item "balls" that are not throwables
        if (s.equals("iron_ball") || s.equals("light_ball") || s.equals("smoke_ball")) {
            return Optional.empty();
        }
        for (CubeBallTier t : values()) {
            if (t.itemId.equals(s)) {
                return Optional.of(t);
            }
        }
        // legacy cube ids
        return switch (s) {
            case "cube_ball", "cube", "bind_orb" -> Optional.of(POKE);
            case "good_cube_ball", "good" -> Optional.of(GREAT);
            case "ultra_cube_ball", "ultra" -> Optional.of(ULTRA);
            default -> Optional.empty();
        };
    }

    public static CubeBallTier byId(String id) {
        return optional(id).orElse(POKE);
    }

    /** True if this item id is a throwable catch ball. */
    public static boolean isCatchBall(String id) {
        return optional(id).isPresent();
    }
}

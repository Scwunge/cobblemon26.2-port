package com.cobblemon.mod.client;

import com.cobblemon.mod.Cobblemon;
import com.cobblemon.mod.species.MonElement;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

/**
 * Official Cobblemon GUI textures + helpers.
 * <p>
 * Draw at native texture pixel sizes (GUI units). Prefer {@link #blitNative}
 * over stretching panels into arbitrary boxes.
 */
public final class UiTextures {
    private UiTextures() {}

    public static Identifier gui(String path) {
        return com.cobblemon.mod.util.CobblemonResource.guiTexture(path);
    }

    // --- Battle (native sizes) ---
    public static final Identifier BATTLE_BACK = gui("battle/battle_back.png");
    public static final Identifier BATTLE_INFO = gui("battle/battle_info_base.png"); // 140x40
    public static final Identifier BATTLE_INFO_FLIP = gui("battle/battle_info_base_flipped.png");
    public static final Identifier BATTLE_LOG = gui("battle/battle_log.png"); // 169x55
    public static final Identifier BATTLE_MOVE = gui("battle/battle_move.png"); // 92x48
    public static final Identifier BATTLE_MOVE_OVERLAY = gui("battle/battle_move_overlay.png");
    public static final Identifier BATTLE_MENU_FIGHT = gui("battle/battle_menu_fight.png"); // 90x52
    public static final Identifier BATTLE_MENU_BAG = gui("battle/battle_menu_bag.png");
    public static final Identifier BATTLE_MENU_SWITCH = gui("battle/battle_menu_switch.png");
    public static final Identifier BATTLE_MENU_RUN = gui("battle/battle_menu_run.png");
    public static final Identifier BATTLE_MENU_FORFEIT = gui("battle/battle_menu_forfeit.png");
    public static final Identifier SELECTION_UNDERLAY = gui("battle/selection_underlay.png");
    public static final Identifier PARTY_SELECT = gui("battle/party_select.png"); // 94x58
    public static final Identifier BATTLE_OWNED = gui("battle/battle_owned_indicator.png");

    public static final int BATTLE_INFO_W = 140, BATTLE_INFO_H = 40;
    public static final int BATTLE_LOG_W = 169, BATTLE_LOG_H = 55;
    public static final int BATTLE_MOVE_W = 92, BATTLE_MOVE_H = 48;
    public static final int BATTLE_MENU_W = 90, BATTLE_MENU_H = 52;
    public static final int PARTY_SELECT_W = 94, PARTY_SELECT_H = 58;

    // --- Party ---
    public static final Identifier PARTY_SLOT = gui("party/party_slot.png"); // 62x30
    public static final Identifier PARTY_SLOT_ACTIVE = gui("party/party_slot_active.png");
    public static final Identifier PARTY_SLOT_FAINTED = gui("party/party_slot_fainted.png");
    public static final Identifier PARTY_SLOT_FAINTED_ACTIVE = gui("party/party_slot_fainted_active.png");
    public static final Identifier PARTY_SLOT_COLLAPSED = gui("party/party_slot_collapsed.png");
    public static final Identifier PARTY_PORTRAIT = gui("party/party_slot_portrait_background.png");
    public static final Identifier PARTY_GENDER_M = gui("party/party_gender_male.png");
    public static final Identifier PARTY_GENDER_F = gui("party/party_gender_female.png");

    public static final int PARTY_SLOT_W = 62, PARTY_SLOT_H = 30;

    // --- PC ---
    public static final Identifier PC_BASE = gui("pc/pc_base.png"); // 349x205
    public static final Identifier PC_PARTY_PANEL = gui("pc/party_panel.png"); // 82x169
    public static final Identifier PC_GRID = gui("pc/pc_screen_grid.png"); // 160x133
    public static final Identifier PC_SLOT = gui("pc/pc_slot_overlay.png"); // 25x25
    public static final Identifier PC_PORTRAIT = gui("pc/portrait_background.png"); // 66x66
    public static final Identifier PC_ARROW_PREV = gui("pc/pc_arrow_previous.png"); // 14x28
    public static final Identifier PC_ARROW_NEXT = gui("pc/pc_arrow_next.png");
    public static final Identifier PC_RELEASE = gui("pc/pc_release_button.png");
    public static final Identifier PC_SPACER_TOP = gui("pc/pc_spacer_top.png");
    public static final Identifier PC_SPACER_BOTTOM = gui("pc/pc_spacer_bottom.png");

    public static final int PC_BASE_W = 349, PC_BASE_H = 205;
    public static final int PC_PARTY_W = 82, PC_PARTY_H = 169;
    public static final int PC_GRID_W = 160, PC_GRID_H = 133;
    public static final int PC_SLOT_S = 25;

    // --- Summary ---
    public static final Identifier SUMMARY_BASE = gui("summary/summary_base.png"); // 331x161
    public static final Identifier SUMMARY_INFO = gui("summary/summary_info_base.png"); // 134x148
    public static final Identifier SUMMARY_STATS = gui("summary/summary_stats_other_base.png");
    public static final Identifier SUMMARY_STATS_CHART = gui("summary/summary_stats_chart_base.png");
    public static final Identifier SUMMARY_MOVES = gui("summary/summary_moves_base.png");
    public static final Identifier SUMMARY_MOVE = gui("summary/summary_move.png"); // 108x44
    public static final Identifier SUMMARY_MOVE_OVERLAY = gui("summary/summary_move_overlay.png");
    public static final Identifier SUMMARY_PARTY_SLOT = gui("summary/summary_party_slot.png"); // 46x54
    public static final Identifier SUMMARY_PARTY_EMPTY = gui("summary/summary_party_slot_empty.png");
    public static final Identifier SUMMARY_PARTY_FAINTED = gui("summary/summary_party_slot_fainted.png");
    public static final Identifier SUMMARY_PORTRAIT = gui("summary/portrait_background.png"); // 66x66
    public static final Identifier SUMMARY_TAB = gui("summary/summary_tab.png");
    public static final Identifier SUMMARY_TAB_INFO = gui("summary/summary_tab_icon_info.png");
    public static final Identifier SUMMARY_TAB_STATS = gui("summary/summary_tab_icon_stats.png");
    public static final Identifier SUMMARY_TAB_MOVES = gui("summary/summary_tab_icon_moves.png");

    public static final int SUMMARY_BASE_W = 331, SUMMARY_BASE_H = 161;
    public static final int SUMMARY_SIDE_W = 134, SUMMARY_SIDE_H = 148;
    public static final int SUMMARY_MOVE_W = 108, SUMMARY_MOVE_H = 44;
    public static final int SUMMARY_PARTY_W = 46, SUMMARY_PARTY_H = 54;
    public static final int SUMMARY_PORTRAIT_S = 66;

    // --- Starter ---
    public static final Identifier STARTER_BASE = gui("starterselection/starterselection_base.png"); // 800x700
    public static final Identifier STARTER_FRAME = gui("starterselection/starterselection_base_frame.png");
    public static final Identifier STARTER_UNDERLAY = gui("starterselection/starterselection_base_underlay.png");
    public static final Identifier STARTER_SLOT = gui("starterselection/starterselection_slot.png"); // 202x60
    public static final Identifier STARTER_BUTTON = gui("starterselection/starterselection_button.png"); // 224x48
    public static final Identifier STARTER_EXIT = gui("starterselection/starterselection_exit.png");
    public static final Identifier STARTER_ARROW_L = gui("starterselection/starterselection_arrow_left.png");
    public static final Identifier STARTER_ARROW_R = gui("starterselection/starterselection_arrow_right.png");
    public static final Identifier STARTER_TYPE1 = gui("starterselection/starterselection_type_slot1.png");
    public static final Identifier STARTER_TYPE2 = gui("starterselection/starterselection_type_slot2.png");
    public static final Identifier STARTER_TYPE3 = gui("starterselection/starterselection_type_slot3.png");

    public static final int STARTER_BASE_W = 800, STARTER_BASE_H = 700;
    public static final int STARTER_SLOT_W = 202, STARTER_SLOT_H = 60;
    public static final int STARTER_BTN_W = 224, STARTER_BTN_H = 48;

    // --- Pokédex ---
    public static final Identifier POKEDEX_BASE = gui("pokedex/pokedex_base_red.png"); // 345x207
    public static final Identifier POKEDEX_SCREEN = gui("pokedex/pokedex_screen.png");
    public static final Identifier POKEDEX_SLOT = gui("pokedex/pokedex_slot.png"); // 25x25
    public static final Identifier POKEDEX_SLOT_UNKNOWN = gui("pokedex/pokedex_slot_unknown.png");
    public static final Identifier POKEDEX_SLOT_SELECT = gui("pokedex/slot_select.png");
    public static final Identifier POKEDEX_CAUGHT = gui("pokedex/caught_icon.png");
    public static final Identifier POKEDEX_SEARCH = gui("pokedex/search_icon.png");
    public static final Identifier POKEDEX_TYPE_BAR = gui("pokedex/type_bar.png");

    public static final int POKEDEX_W = 345, POKEDEX_H = 207;
    public static final int POKEDEX_SLOT_S = 25;

    // --- Common ---
    public static final Identifier BACK_BUTTON = gui("common/back_button.png"); // 26x26
    public static final Identifier TYPES = gui("types.png"); // 648x36 — 18×36
    public static final Identifier TYPES_SMALL = gui("types_small.png"); // 324x18

    /**
     * Cobblemon types.png order (left → right).
     * Matches official atlas, not our MonElement enum ordinal.
     */
    private static final MonElement[] TYPE_ATLAS = {
            MonElement.NORMAL, MonElement.FIRE, MonElement.WATER, MonElement.GRASS,
            MonElement.ELECTRIC, MonElement.ICE, MonElement.FIGHTING, MonElement.POISON,
            MonElement.GROUND, MonElement.FLYING, MonElement.PSYCHIC, MonElement.BUG,
            MonElement.ROCK, MonElement.GHOST, MonElement.DRAGON, MonElement.DARK,
            MonElement.STEEL, MonElement.FAIRY
    };

    /** Draw full texture at native size (or scaled). */
    public static void blitNative(GuiGraphicsExtractor g, Identifier tex, int x, int y, int w, int h) {
        try {
            g.blit(tex, x, y, x + w, y + h, 0f, 1f, 0f, 1f);
        } catch (Exception ignored) {
            g.fill(x, y, x + w, y + h, 0xFF555555);
        }
    }

    public static void blitNative(GuiGraphicsExtractor g, Identifier tex, int x, int y, int nativeW, int nativeH, float scale) {
        int w = Math.max(1, Math.round(nativeW * scale));
        int h = Math.max(1, Math.round(nativeH * scale));
        blitNative(g, tex, x, y, w, h);
    }

    public static void blit(GuiGraphicsExtractor g, Identifier tex, int x0, int y0, int x1, int y1) {
        try {
            g.blit(tex, x0, y0, x1, y1, 0f, 1f, 0f, 1f);
        } catch (Exception ignored) {
        }
    }

    public static void blitOrFill(GuiGraphicsExtractor g, Identifier tex, int x0, int y0, int x1, int y1, int fillArgb) {
        try {
            g.blit(tex, x0, y0, x1, y1, 0f, 1f, 0f, 1f);
        } catch (Exception e) {
            g.fill(x0, y0, x1, y1, fillArgb);
        }
    }

    public static void blitUv(
            GuiGraphicsExtractor g,
            Identifier tex,
            int x, int y, int w, int h,
            float u0, float v0, float u1, float v1
    ) {
        try {
            g.blit(tex, x, y, x + w, y + h, u0, u1, v0, v1);
        } catch (Exception ignored) {
            g.fill(x, y, x + w, y + h, 0xFF888888);
        }
    }

    public static int typeAtlasIndex(MonElement el) {
        if (el == null) {
            return 0;
        }
        for (int i = 0; i < TYPE_ATLAS.length; i++) {
            if (TYPE_ATLAS[i] == el) {
                return i;
            }
        }
        return 0;
    }

    /** 36×36 type icon from types.png. */
    public static void typeIcon(GuiGraphicsExtractor g, MonElement el, int x, int y, int size) {
        int idx = typeAtlasIndex(el);
        float u0 = idx / 18f;
        float u1 = (idx + 1) / 18f;
        blitUv(g, TYPES, x, y, size, size, u0, 0f, u1, 1f);
    }

    /** 18×18 type icon from types_small.png. */
    public static void typeIconSmall(GuiGraphicsExtractor g, MonElement el, int x, int y, int size) {
        int idx = typeAtlasIndex(el);
        float u0 = idx / 18f;
        float u1 = (idx + 1) / 18f;
        blitUv(g, TYPES_SMALL, x, y, size, size, u0, 0f, u1, 1f);
    }

    public static void hpBar(GuiGraphicsExtractor g, int x, int y, int w, int h, int hp, int maxHp) {
        g.fill(x, y, x + w, y + h, 0xFF202028);
        float ratio = maxHp <= 0 ? 0f : Math.max(0f, Math.min(1f, (float) hp / maxHp));
        int fill = Math.max(0, Math.round((w - 2) * ratio));
        int color = ratio > 0.5f ? 0xFF3DDC84 : (ratio > 0.2f ? 0xFFFFD166 : 0xFFEF476F);
        if (fill > 0) {
            g.fill(x + 1, y + 1, x + 1 + fill, y + h - 1, color);
        }
    }

    public static boolean hit(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    /** Scale so a base panel fits inside the screen with padding. */
    public static float fitScale(int baseW, int baseH, int screenW, int screenH, float maxFrac) {
        float sx = (screenW * maxFrac) / baseW;
        float sy = (screenH * maxFrac) / baseH;
        return Math.min(1.25f, Math.min(sx, sy));
    }
}

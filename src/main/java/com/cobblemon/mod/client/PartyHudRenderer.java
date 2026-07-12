package com.cobblemon.mod.client;

import com.cobblemon.mod.party.PlayerParty;
import com.cobblemon.mod.species.OwnedMon;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;

/**
 * World HUD strip + inventory docked party panel.
 */
public final class PartyHudRenderer {
    private static final int MARGIN_X = 6;
    private static final int MARGIN_Y = 40;

    /** Compact docked rows (not full 62×30 world-HUD slots — those overflow creative height). */
    private static final int INV_PANEL_W = 86;
    private static final int INV_PAD = 5;
    private static final int INV_TITLE_H = 16;
    private static final int INV_ROW_H = 22;
    private static final int INV_ROW_GAP = 2;
    private static final int INV_ICON = 18;

    private PartyHudRenderer() {}

    /** Height of the docked panel: title + 6 rows (PC is world-only via block / command). */
    private static int inventoryPanelH() {
        return INV_PAD + INV_TITLE_H
                + PlayerParty.MAX_SIZE * (INV_ROW_H + INV_ROW_GAP)
                + INV_PAD;
    }

    public static int hitTestInventorySlot(AbstractContainerScreen<?> screen, int mouseX, int mouseY) {
        int panelX = inventoryPanelX(screen);
        int panelY = screen.getTopPos();
        int slotStartY = panelY + INV_PAD + INV_TITLE_H;
        int sx = panelX + INV_PAD;
        int rowW = INV_PANEL_W - INV_PAD * 2;

        for (int i = 0; i < PlayerParty.MAX_SIZE; i++) {
            int sy = slotStartY + i * (INV_ROW_H + INV_ROW_GAP);
            if (mouseX >= sx && mouseX < sx + rowW && mouseY >= sy && mouseY < sy + INV_ROW_H) {
                return i;
            }
        }
        return -1;
    }

    /** Prefer left of inv; if clipped, put on the right of inv. */
    private static int inventoryPanelX(AbstractContainerScreen<?> screen) {
        int left = screen.getLeftPos() - INV_PANEL_W - 4;
        if (left < 2) {
            return screen.getLeftPos() + screen.getImageWidth() + 4;
        }
        return left;
    }

    public static void renderWorldHud(GuiGraphicsExtractor graphics, int screenW, int screenH) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gui.hud.isHidden() || mc.gui.screen() != null) {
            return;
        }
        PlayerParty party = mc.player.getData(com.cobblemon.mod.party.ModAttachments.PARTY);
        if (ClientPartySettings.hudVisible && !party.isEmpty()) {
            renderWorldPartyStrip(graphics, party, screenW);
        }
        if (ClientPartySettings.showThrowTip && !party.isEmpty()) {
            renderThrowTip(graphics, screenW);
        }
        if (party.isEmpty()) {
            ActiveTextCollector text = graphics.textRenderer(GuiGraphicsExtractor.HoveredTextEffects.NONE);
            text.accept(TextAlignment.CENTER, screenW / 2, screenH - 52,
                    Component.translatable("hud.cobblemon.choose_starter").withStyle(ChatFormatting.GOLD));
        }
    }

    /**
     * Always draw when inventory/creative is open (independent of world HUD toggle).
     */
    public static void renderInventoryParty(
            GuiGraphicsExtractor graphics,
            AbstractContainerScreen<?> screen,
            PlayerParty party,
            int mouseX,
            int mouseY
    ) {
        int panelX = inventoryPanelX(screen);
        int panelY = screen.getTopPos();
        int panelH = inventoryPanelH();
        drawInventoryPartyPanel(graphics, party, panelX, panelY, INV_PANEL_W, panelH, mouseX, mouseY);
        InventoryPartyMenu.render(graphics, party, mouseX, mouseY);
    }

    private static void drawInventoryPartyPanel(
            GuiGraphicsExtractor graphics,
            PlayerParty party,
            int x,
            int y,
            int w,
            int h,
            int mouseX,
            int mouseY
    ) {
        ActiveTextCollector text = graphics.textRenderer(GuiGraphicsExtractor.HoveredTextEffects.NONE);

        // Official PC party panel chrome (scaled to compact inventory dock)
        UiTextures.blitNative(graphics, UiTextures.PC_PARTY_PANEL, x, y, w, h);

        text.accept(x + INV_PAD, y + INV_PAD + 2, Component.literal("Party").withStyle(ChatFormatting.GOLD));
        text.accept(x + w - INV_PAD - 20, y + INV_PAD + 2,
                Component.literal(party.size() + "/6").withStyle(ChatFormatting.DARK_GRAY));

        int slotStartY = y + INV_PAD + INV_TITLE_H;
        int rowW = w - INV_PAD * 2;
        // Scale official 62×30 party slots into compact dock rows
        int slotDrawW = rowW;
        int slotDrawH = INV_ROW_H;

        for (int i = 0; i < PlayerParty.MAX_SIZE; i++) {
            int sx = x + INV_PAD;
            int sy = slotStartY + i * (INV_ROW_H + INV_ROW_GAP);
            boolean hover = mouseX >= sx && mouseX < sx + rowW
                    && mouseY >= sy && mouseY < sy + INV_ROW_H;
            boolean filled = i < party.size();

            if (!filled) {
                UiTextures.blitNative(graphics, UiTextures.PARTY_SLOT_COLLAPSED, sx, sy, slotDrawW, slotDrawH);
                if (hover) {
                    graphics.fill(sx, sy, sx + slotDrawW, sy + slotDrawH, 0x22FFFFFF);
                }
                continue;
            }

            OwnedMon mon = party.get(i).orElseThrow();
            boolean lead = i == 0;
            var tex = mon.isFainted()
                    ? (lead ? UiTextures.PARTY_SLOT_FAINTED_ACTIVE : UiTextures.PARTY_SLOT_FAINTED)
                    : (lead ? UiTextures.PARTY_SLOT_ACTIVE : UiTextures.PARTY_SLOT);
            UiTextures.blitNative(graphics, tex, sx, sy, slotDrawW, slotDrawH);
            if (hover) {
                graphics.fill(sx, sy, sx + slotDrawW, sy + slotDrawH, 0x28FFFFFF);
            }

            MonSpriteDrawer.draw(graphics, sx + 3, sy + 2, INV_ICON - 2, mon);

            String name = mon.displayName().getString();
            if (name.length() > 8) {
                name = name.substring(0, 7) + "…";
            }
            text.accept(sx + INV_ICON + 4, sy + 2,
                    Component.literal(name).withStyle(lead ? ChatFormatting.YELLOW : ChatFormatting.WHITE));
            text.accept(sx + INV_ICON + 4, sy + 11,
                    Component.literal("Lv." + mon.level()).withStyle(ChatFormatting.GRAY));
            UiTextures.hpBar(graphics, sx + rowW - 28, sy + INV_ROW_H - 6, 24, 3, mon.hp(), mon.maxHp());
        }
    }

    private static void renderThrowTip(GuiGraphicsExtractor graphics, int screenW) {
        int tipW = 150;
        int tipH = 34;
        int x = screenW - tipW - 8;
        int y = 8;

        graphics.fill(x, y, x + tipW, y + tipH, 0xE0181820);
        graphics.fill(x, y, x + tipW, y + 1, 0xFF5A5A68);
        graphics.fill(x, y + tipH - 1, x + tipW, y + tipH, 0xFF5A5A68);
        graphics.fill(x, y, x + 1, y + tipH, 0xFF5A5A68);
        graphics.fill(x + tipW - 1, y, x + tipW, y + tipH, 0xFF5A5A68);

        drawBall(graphics, x + 8, y + 9, 14, 0xFFE85555, 0xFFF0F0F0);

        ActiveTextCollector text = graphics.textRenderer(GuiGraphicsExtractor.HoveredTextEffects.NONE);
        text.accept(x + 28, y + 7,
                Component.translatable("hud.cobblemon.throw_title").withStyle(ChatFormatting.WHITE));
        text.accept(x + 28, y + 18,
                Component.translatable("hud.cobblemon.throw_hint").withStyle(ChatFormatting.GRAY));
    }

    private static void renderWorldPartyStrip(GuiGraphicsExtractor graphics, PlayerParty party, int screenW) {
        boolean right = ClientPartySettings.hudOnRight;
        int startY = MARGIN_Y;
        int slotW = UiTextures.PARTY_SLOT_W;
        int slotH = UiTextures.PARTY_SLOT_H;
        int gap = 2;

        ActiveTextCollector text = graphics.textRenderer(GuiGraphicsExtractor.HoveredTextEffects.TOOLTIP_ONLY);

        // Only draw filled slots in the world HUD (empty collapsed bars clutter the edge)
        for (int i = 0; i < party.size() && i < PlayerParty.MAX_SIZE; i++) {
            int cy = startY + i * (slotH + gap);
            boolean lead = i == 0;
            int cx = right ? screenW - slotW - MARGIN_X : MARGIN_X;

            OwnedMon mon = party.get(i).orElseThrow();
            var tex = mon.isFainted()
                    ? (lead ? UiTextures.PARTY_SLOT_FAINTED_ACTIVE : UiTextures.PARTY_SLOT_FAINTED)
                    : (lead ? UiTextures.PARTY_SLOT_ACTIVE : UiTextures.PARTY_SLOT);
            UiTextures.blitNative(graphics, tex, cx, cy, slotW, slotH);
            MonSpriteDrawer.draw(graphics, cx + 3, cy + 3, 20, mon);

            String name = mon.displayName().getString();
            if (name.length() > 8) {
                name = name.substring(0, 7) + "…";
            }
            text.accept(cx + 26, cy + 4,
                    Component.literal(name).withStyle(lead ? ChatFormatting.YELLOW : ChatFormatting.WHITE));
            text.accept(cx + 26, cy + 14,
                    Component.literal("Lv." + mon.level()).withStyle(ChatFormatting.GRAY));
            UiTextures.hpBar(graphics, cx + 26, cy + 23, 32, 4, mon.hp(), mon.maxHp());
        }
    }

    private static void drawBall(GuiGraphicsExtractor graphics, int x, int y, int size, int topColor, int bottomColor) {
        graphics.fill(x, y + 2, x + size, y + size - 2, 0xFF1A1A22);
        graphics.fill(x + 2, y, x + size - 2, y + size, 0xFF1A1A22);

        int inset = 2;
        int ix = x + inset;
        int iy = y + inset;
        int is = size - inset * 2;
        int mid = iy + is / 2;

        graphics.fill(ix, iy, ix + is, mid, topColor);
        graphics.fill(ix, mid, ix + is, iy + is, bottomColor);
        graphics.fill(ix, mid - 1, ix + is, mid + 2, 0xFF101018);
        int cx = ix + is / 2 - 2;
        int cy = mid - 2;
        graphics.fill(cx, cy, cx + 4, cy + 4, 0xFFE8E8F0);
        graphics.fill(cx + 1, cy + 1, cx + 3, cy + 3, 0xFF303038);
    }
}

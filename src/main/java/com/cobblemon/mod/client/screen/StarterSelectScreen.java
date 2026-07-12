package com.cobblemon.mod.client.screen;

import java.util.List;

import com.cobblemon.mod.client.MonSpriteDrawer;
import com.cobblemon.mod.client.UiTextures;
import com.cobblemon.mod.network.ChooseStarterPayload;
import com.cobblemon.mod.species.MonSpecies;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * Starter picker using official Cobblemon <em>pieces</em> (slot / button / exit),
 * not the full 800×700 multi-region shell (that chrome is for config-driven
 * category browsing and looks empty/broken with only 3 Kanto starters).
 */
public class StarterSelectScreen extends Screen {
    private static final int SLOT_W = UiTextures.STARTER_SLOT_W; // 202
    private static final int SLOT_H = UiTextures.STARTER_SLOT_H; // 60
    private static final int BTN_W = UiTextures.STARTER_BTN_W;   // 224
    private static final int BTN_H = UiTextures.STARTER_BTN_H;   // 48
    private static final int EXIT_W = 32;
    private static final int EXIT_H = 24;
    private static final int PORTRAIT = 96;
    private static final int PANEL_PAD = 20;
    private static final int SLOT_GAP = 8;

    private final List<MonSpecies> starters = MonSpecies.starters();
    private int selected = 0;

    private int panelX, panelY, panelW, panelH;
    private int slotsLeft, slotsTop;
    private int portraitX, portraitY;
    private int btnX, btnY;
    private int exitX, exitY;

    public StarterSelectScreen() {
        super(Component.translatable("screen.cobblemon.starter"));
    }

    @Override
    protected void init() {
        int n = Math.max(1, starters.size());
        int slotsBlockW = n * SLOT_W + Math.max(0, n - 1) * SLOT_GAP;
        int contentW = Math.max(slotsBlockW, Math.max(BTN_W, PORTRAIT + 40));
        panelW = contentW + PANEL_PAD * 2;
        // title + portrait + gap + slots + gap + selected line + button + pad
        panelH = 28 + 16 + PORTRAIT + 16 + SLOT_H + 14 + 14 + BTN_H + PANEL_PAD + 8;

        panelX = (this.width - panelW) / 2;
        panelY = Math.max(12, (this.height - panelH) / 2);

        portraitX = panelX + (panelW - PORTRAIT) / 2;
        portraitY = panelY + 44;

        slotsLeft = panelX + (panelW - slotsBlockW) / 2;
        slotsTop = portraitY + PORTRAIT + 16;

        btnX = panelX + (panelW - BTN_W) / 2;
        btnY = slotsTop + SLOT_H + 28;

        exitX = panelX + panelW - EXIT_W - 8;
        exitY = panelY + 8;
    }

    private int slotX(int index) {
        return slotsLeft + index * (SLOT_W + SLOT_GAP);
    }

    private void confirm() {
        if (selected < 0 || selected >= starters.size()) {
            return;
        }
        ClientPacketDistributor.sendToServer(new ChooseStarterPayload(starters.get(selected).id()));
        com.cobblemon.mod.client.ClientPartySettings.showThrowTipAfterStarter();
        this.onClose();
    }

    private static String prettyName(MonSpecies sp) {
        String name = sp.displayName().getString();
        if (name.contains(".") || name.startsWith("cobblemon")) {
            String id = sp.id();
            return Character.toUpperCase(id.charAt(0)) + id.substring(1);
        }
        return name;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) {
            return super.mouseClicked(event, doubleClick);
        }
        double mx = event.x();
        double my = event.y();

        if (UiTextures.hit(mx, my, exitX, exitY, EXIT_W, EXIT_H)) {
            onClose();
            return true;
        }
        if (UiTextures.hit(mx, my, btnX, btnY, BTN_W, BTN_H)) {
            confirm();
            return true;
        }
        for (int i = 0; i < starters.size(); i++) {
            if (UiTextures.hit(mx, my, slotX(i), slotsTop, SLOT_W, SLOT_H)) {
                selected = i;
                if (doubleClick) {
                    confirm();
                }
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        this.extractTransparentBackground(graphics);
        // Dim world — do NOT draw the full 800×700 multi-panel shell
        graphics.fill(0, 0, this.width, this.height, 0xD0080C18);

        // Solid panel frame
        graphics.fill(panelX - 3, panelY - 3, panelX + panelW + 3, panelY + panelH + 3, 0xFF2A3040);
        graphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xF0181E2C);
        // Title bar
        graphics.fill(panelX, panelY, panelX + panelW, panelY + 36, 0xF0242C3C);
        graphics.fill(panelX, panelY + 36, panelX + panelW, panelY + 37, 0xFF3A4560);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        ActiveTextCollector text = graphics.textRenderer(GuiGraphicsExtractor.HoveredTextEffects.NONE);

        text.accept(TextAlignment.CENTER, this.width / 2, panelY + 10,
                Component.translatable("screen.cobblemon.starter_title").withStyle(ChatFormatting.GOLD));
        text.accept(TextAlignment.CENTER, this.width / 2, panelY + 22,
                Component.translatable("screen.cobblemon.starter_subtitle").withStyle(ChatFormatting.GRAY));

        // Exit
        UiTextures.blitNative(graphics, UiTextures.STARTER_EXIT, exitX, exitY, EXIT_W, EXIT_H);

        // Large preview of selected mon
        if (selected >= 0 && selected < starters.size()) {
            MonSpecies pick = starters.get(selected);
            // Portrait plate
            graphics.fill(portraitX - 4, portraitY - 4, portraitX + PORTRAIT + 4, portraitY + PORTRAIT + 4, 0xFF0C1018);
            graphics.fill(portraitX, portraitY, portraitX + PORTRAIT, portraitY + PORTRAIT, 0xFF1A2230);
            // Type accent ring
            graphics.fill(portraitX, portraitY, portraitX + PORTRAIT, portraitY + 3, pick.element().argb(0xFF));
            MonSpriteDrawer.draw(graphics, portraitX + 8, portraitY + 8, PORTRAIT - 16, pick);
        }

        // Starter slots (official texture, native size)
        for (int i = 0; i < starters.size(); i++) {
            MonSpecies sp = starters.get(i);
            int sx = slotX(i);
            int sy = slotsTop;
            boolean on = i == selected;
            boolean hover = UiTextures.hit(mouseX, mouseY, sx, sy, SLOT_W, SLOT_H);

            UiTextures.blitNative(graphics, UiTextures.STARTER_SLOT, sx, sy, SLOT_W, SLOT_H);
            if (on) {
                graphics.fill(sx + 2, sy + 2, sx + SLOT_W - 2, sy + SLOT_H - 2, 0x35FFD166);
            } else if (hover) {
                graphics.fill(sx + 2, sy + 2, sx + SLOT_W - 2, sy + SLOT_H - 2, 0x22FFFFFF);
            }

            // Sprite inside left of slot
            MonSpriteDrawer.draw(graphics, sx + 6, sy + 6, 48, sp);

            String name = prettyName(sp);
            text.accept(sx + 60, sy + 12,
                    Component.literal(name).withStyle(on ? ChatFormatting.YELLOW : ChatFormatting.WHITE));
            text.accept(sx + 60, sy + 28,
                    Component.literal(sp.element().englishName()).withStyle(ChatFormatting.GRAY));
            UiTextures.typeIconSmall(graphics, sp.element(), sx + SLOT_W - 24, sy + 10, 16);
        }

        // Selected label
        if (selected >= 0 && selected < starters.size()) {
            text.accept(TextAlignment.CENTER, this.width / 2, btnY - 14,
                    Component.translatable("screen.cobblemon.starter_picked", prettyName(starters.get(selected)))
                            .withStyle(ChatFormatting.AQUA));
        }

        // Confirm button (official texture)
        boolean btnHover = UiTextures.hit(mouseX, mouseY, btnX, btnY, BTN_W, BTN_H);
        UiTextures.blitNative(graphics, UiTextures.STARTER_BUTTON, btnX, btnY, BTN_W, BTN_H);
        if (btnHover) {
            graphics.fill(btnX, btnY, btnX + BTN_W, btnY + BTN_H, 0x28FFFFFF);
        }
        text.accept(TextAlignment.CENTER, btnX + BTN_W / 2, btnY + BTN_H / 2 - 4,
                Component.translatable("screen.cobblemon.starter_confirm").withStyle(ChatFormatting.WHITE));

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

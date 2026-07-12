package com.cobblemon.mod.client.screen;

import com.cobblemon.mod.client.MonSpriteDrawer;
import com.cobblemon.mod.network.UseItemOnMonPayload;
import com.cobblemon.mod.party.ModAttachments;
import com.cobblemon.mod.party.PlayerParty;
import com.cobblemon.mod.species.OwnedMon;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/** Pick a party mon for bag-item use. */
public class MonSelectScreen extends Screen {
    private static final int PANEL_W = 260;
    private static final int SLOT_H = 28;

    private final int hand;
    private final String itemId;
    private int panelLeft;
    private int panelTop;
    private int slotsTop;

    public MonSelectScreen(int hand, String itemId) {
        super(Component.translatable("screen.cobblemon.select_mon"));
        this.hand = hand;
        this.itemId = itemId == null ? "" : itemId;
    }

    private PlayerParty party() {
        if (minecraft == null || minecraft.player == null) {
            return PlayerParty.empty();
        }
        return minecraft.player.getData(ModAttachments.PARTY);
    }

    @Override
    protected void init() {
        clearWidgets();
        int panelH = 40 + PlayerParty.MAX_SIZE * SLOT_H + 36;
        panelLeft = (width - PANEL_W) / 2;
        panelTop = Math.max(12, (height - panelH) / 2);
        slotsTop = panelTop + 36;
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> onClose())
                .bounds(panelLeft + PANEL_W / 2 - 40, panelTop + panelH - 28, 80, 20).build());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        extractTransparentBackground(graphics);
        graphics.fill(0, 0, width, height, 0xB0080C18);
        int panelBottom = slotsTop + PlayerParty.MAX_SIZE * SLOT_H + 4;
        graphics.fill(panelLeft - 10, panelTop - 10, panelLeft + PANEL_W + 10, panelBottom + 36, 0xE0101428);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        ActiveTextCollector text = graphics.textRenderer(GuiGraphicsExtractor.HoveredTextEffects.TOOLTIP_AND_CURSOR);
        int centerX = width / 2;
        text.accept(TextAlignment.CENTER, centerX, panelTop, title.copy().withStyle(ChatFormatting.GOLD));
        if (!itemId.isEmpty()) {
            text.accept(TextAlignment.CENTER, centerX, panelTop + 14,
                    Component.literal(itemId).withStyle(ChatFormatting.GRAY));
        }

        PlayerParty party = party();
        for (int i = 0; i < PlayerParty.MAX_SIZE; i++) {
            int y = slotsTop + i * SLOT_H;
            boolean hover = mouseX >= panelLeft && mouseX < panelLeft + PANEL_W
                    && mouseY >= y && mouseY < y + SLOT_H - 2;
            graphics.fill(panelLeft, y, panelLeft + PANEL_W, y + SLOT_H - 2, hover ? 0xE0305088 : 0xC0141828);
            if (i < party.size()) {
                OwnedMon mon = party.get(i).orElseThrow();
                MonSpriteDrawer.draw(graphics, panelLeft + 8, y + 4, 18, mon);
                Component line = Component.empty()
                        .append(mon.displayName())
                        .append(Component.literal("  Lv." + mon.level()).withStyle(ChatFormatting.YELLOW))
                        .append(Component.literal("  HP " + mon.hp() + "/" + mon.maxHp())
                                .withStyle(mon.isFainted() ? ChatFormatting.RED : ChatFormatting.GREEN));
                if (!mon.status().isNone()) {
                    line = line.copy().append(Component.literal("  [" + mon.status().english() + "]")
                            .withStyle(ChatFormatting.LIGHT_PURPLE));
                }
                text.accept(panelLeft + 32, y + 8, line);
            } else {
                text.accept(panelLeft + 14, y + 8,
                        Component.translatable("screen.cobblemon.empty_slot").withStyle(ChatFormatting.DARK_GRAY));
            }
        }
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }
        if (event.button() != 0) {
            return false;
        }
        PlayerParty party = party();
        for (int i = 0; i < party.size(); i++) {
            int y = slotsTop + i * SLOT_H;
            if (event.x() >= panelLeft && event.x() < panelLeft + PANEL_W
                    && event.y() >= y && event.y() < y + SLOT_H - 2) {
                ClientPacketDistributor.sendToServer(new UseItemOnMonPayload(hand, i));
                onClose();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

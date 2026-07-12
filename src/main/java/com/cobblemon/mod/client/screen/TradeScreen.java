package com.cobblemon.mod.client.screen;

import java.util.ArrayList;
import java.util.List;

import com.cobblemon.mod.network.OpenTradePayload;
import com.cobblemon.mod.network.TradeOfferPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * Trade UI — select a mon on each side and confirm.
 * Real multiplayer trade when partner is nearby; same-player debug swaps two of your slots.
 */
public class TradeScreen extends Screen {
    private static final int PANEL_W = 200;
    private static final int SLOT_H = 18;
    private static final int GAP = 24;

    private final OpenTradePayload data;
    private final List<String> selfMons;
    private final List<String> partnerMons;

    private int selfSelected = -1;
    private int partnerSelected = -1;
    private int leftX;
    private int rightX;
    private int listTop;
    private int btnY;

    public TradeScreen(OpenTradePayload data) {
        super(Component.literal("Trade"));
        this.data = data;
        this.selfMons = data == null || data.selfMons() == null
                ? List.of() : new ArrayList<>(data.selfMons());
        this.partnerMons = data == null || data.partnerMons() == null
                ? List.of() : new ArrayList<>(data.partnerMons());
    }

    @Override
    protected void init() {
        int totalW = PANEL_W * 2 + GAP;
        leftX = (width - totalW) / 2;
        rightX = leftX + PANEL_W + GAP;
        listTop = height / 2 - 70;
        btnY = listTop + 6 * SLOT_H + 28;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        extractTransparentBackground(graphics);
        graphics.fill(0, 0, width, height, 0xB0080C18);
        // Two panels
        graphics.fill(leftX - 8, listTop - 28, leftX + PANEL_W + 8, btnY + 40, 0xE0101428);
        graphics.fill(rightX - 8, listTop - 28, rightX + PANEL_W + 8, btnY + 40, 0xE0101428);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        ActiveTextCollector text = graphics.textRenderer(GuiGraphicsExtractor.HoveredTextEffects.TOOLTIP_AND_CURSOR);
        text.accept(TextAlignment.CENTER, width / 2, listTop - 40,
                Component.literal("Pokémon Trade").withStyle(ChatFormatting.GOLD));
        if (data != null && data.debugSamePlayer()) {
            text.accept(TextAlignment.CENTER, width / 2, listTop - 28,
                    Component.literal("Debug / same-player mode").withStyle(ChatFormatting.DARK_GRAY));
        }

        String selfTitle = data != null && data.selfName() != null ? data.selfName() : "You";
        String partnerTitle = data != null && data.partnerName() != null ? data.partnerName() : "Partner";
        text.accept(leftX + 4, listTop - 14,
                Component.literal(selfTitle).withStyle(ChatFormatting.AQUA));
        text.accept(rightX + 4, listTop - 14,
                Component.literal(partnerTitle).withStyle(ChatFormatting.LIGHT_PURPLE));

        drawList(graphics, text, leftX, selfMons, selfSelected, mouseX, mouseY);
        drawList(graphics, text, rightX, partnerMons, partnerSelected, mouseX, mouseY);

        // Confirm
        boolean confirmHover = hitBtn(mouseX, mouseY, width / 2 - 50, btnY, 100, 20);
        graphics.fill(width / 2 - 50, btnY, width / 2 + 50, btnY + 20,
                confirmHover ? 0xF0408060 : 0xE0285040);
        text.accept(TextAlignment.CENTER, width / 2, btnY + 6,
                Component.literal("Confirm").withStyle(ChatFormatting.WHITE));

        // Cancel
        boolean cancelHover = hitBtn(mouseX, mouseY, width / 2 - 50, btnY + 24, 100, 20);
        graphics.fill(width / 2 - 50, btnY + 24, width / 2 + 50, btnY + 44,
                cancelHover ? 0xF0804040 : 0xE0502828);
        text.accept(TextAlignment.CENTER, width / 2, btnY + 30,
                Component.literal("Cancel").withStyle(ChatFormatting.WHITE));

        text.accept(TextAlignment.CENTER, width / 2, btnY + 52,
                Component.literal(data != null && data.debugSamePlayer()
                                ? "Debug: pick two of your slots, Confirm swaps them"
                                : "Select your offer · partner selects theirs · both Confirm")
                        .withStyle(ChatFormatting.GRAY));

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void drawList(
            GuiGraphicsExtractor graphics,
            ActiveTextCollector text,
            int x,
            List<String> mons,
            int selected,
            int mouseX,
            int mouseY
    ) {
        int rows = Math.max(6, mons.size());
        for (int i = 0; i < rows; i++) {
            int y = listTop + i * SLOT_H;
            boolean hover = mouseX >= x && mouseX < x + PANEL_W && mouseY >= y && mouseY < y + SLOT_H - 1;
            boolean sel = i == selected;
            int bg = sel ? 0xE0406088 : (hover ? 0xE0305080 : 0xC0141828);
            graphics.fill(x, y, x + PANEL_W, y + SLOT_H - 1, bg);
            if (i < mons.size()) {
                text.accept(x + 6, y + 5,
                        Component.literal((i + 1) + ". " + mons.get(i)).withStyle(ChatFormatting.WHITE));
            } else {
                text.accept(x + 6, y + 5,
                        Component.literal("— empty —").withStyle(ChatFormatting.DARK_GRAY));
            }
        }
    }

    private static boolean hitBtn(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }
        if (event.button() != 0) {
            return false;
        }
        double mx = event.x();
        double my = event.y();

        // Self list
        for (int i = 0; i < Math.max(6, selfMons.size()); i++) {
            int y = listTop + i * SLOT_H;
            if (mx >= leftX && mx < leftX + PANEL_W && my >= y && my < y + SLOT_H - 1) {
                if (i < selfMons.size()) {
                    selfSelected = i;
                    ClientPacketDistributor.sendToServer(
                            new TradeOfferPayload(TradeOfferPayload.ACTION_SELECT, i, -1));
                }
                return true;
            }
        }
        // Partner list — in debug mode this is your other slot; multiplayer: view-only
        for (int i = 0; i < Math.max(6, partnerMons.size()); i++) {
            int y = listTop + i * SLOT_H;
            if (mx >= rightX && mx < rightX + PANEL_W && my >= y && my < y + SLOT_H - 1) {
                if (i < partnerMons.size()) {
                    partnerSelected = i;
                    if (data != null && data.debugSamePlayer()) {
                        ClientPacketDistributor.sendToServer(
                                new TradeOfferPayload(TradeOfferPayload.ACTION_SELECT, i, -1));
                    }
                }
                return true;
            }
        }

        if (hitBtn(mx, my, width / 2 - 50, btnY, 100, 20)) {
            int a = selfSelected >= 0 ? selfSelected : 0;
            int b = partnerSelected >= 0 ? partnerSelected : 0;
            ClientPacketDistributor.sendToServer(
                    new TradeOfferPayload(TradeOfferPayload.ACTION_CONFIRM, a, b));
            return true;
        }
        if (hitBtn(mx, my, width / 2 - 50, btnY + 24, 100, 20)) {
            ClientPacketDistributor.sendToServer(
                    new TradeOfferPayload(TradeOfferPayload.ACTION_CANCEL, 0, 0));
            onClose();
            return true;
        }
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

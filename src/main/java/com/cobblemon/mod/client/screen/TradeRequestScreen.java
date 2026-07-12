package com.cobblemon.mod.client.screen;

import java.util.UUID;

import com.cobblemon.mod.network.TradeRequestPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/** Incoming trade offer: Accept / Decline. */
public class TradeRequestScreen extends Screen {
    private final UUID fromId;
    private final String fromName;
    private int cx, cy;

    public TradeRequestScreen(UUID fromId, String fromName) {
        super(Component.literal("Trade Request"));
        this.fromId = fromId;
        this.fromName = fromName == null ? "Someone" : fromName;
    }

    @Override
    protected void init() {
        cx = width / 2;
        cy = height / 2;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        extractTransparentBackground(graphics);
        graphics.fill(cx - 110, cy - 45, cx + 110, cy + 55, 0xE0101428);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        ActiveTextCollector text = graphics.textRenderer(GuiGraphicsExtractor.HoveredTextEffects.NONE);
        text.accept(TextAlignment.CENTER, cx, cy - 30,
                Component.literal(fromName + " wants to trade!").withStyle(ChatFormatting.YELLOW));

        boolean acceptH = mouseX >= cx - 100 && mouseX < cx - 10 && mouseY >= cy && mouseY < cy + 22;
        boolean declineH = mouseX >= cx + 10 && mouseX < cx + 100 && mouseY >= cy && mouseY < cy + 22;
        graphics.fill(cx - 100, cy, cx - 10, cy + 22, acceptH ? 0xF0408060 : 0xE0285040);
        graphics.fill(cx + 10, cy, cx + 100, cy + 22, declineH ? 0xF0804040 : 0xE0502828);
        text.accept(TextAlignment.CENTER, cx - 55, cy + 7,
                Component.literal("Accept").withStyle(ChatFormatting.WHITE));
        text.accept(TextAlignment.CENTER, cx + 55, cy + 7,
                Component.literal("Decline").withStyle(ChatFormatting.WHITE));
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) {
            return super.mouseClicked(event, doubleClick);
        }
        double mx = event.x();
        double my = event.y();
        if (mx >= cx - 100 && mx < cx - 10 && my >= cy && my < cy + 22) {
            ClientPacketDistributor.sendToServer(new TradeRequestPayload(
                    TradeRequestPayload.ACTION_ACCEPT, fromId, fromName
            ));
            onClose();
            return true;
        }
        if (mx >= cx + 10 && mx < cx + 100 && my >= cy && my < cy + 22) {
            ClientPacketDistributor.sendToServer(new TradeRequestPayload(
                    TradeRequestPayload.ACTION_DECLINE, fromId, fromName
            ));
            onClose();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

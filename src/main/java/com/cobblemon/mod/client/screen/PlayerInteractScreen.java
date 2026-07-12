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

/**
 * Cobblemon-style player interact (R on player): Battle / Trade / Cancel.
 */
public class PlayerInteractScreen extends Screen {
    private final UUID targetId;
    private final String targetName;
    private int cx, cy;

    public PlayerInteractScreen(UUID targetId, String targetName) {
        super(Component.literal("Interact"));
        this.targetId = targetId;
        this.targetName = targetName == null ? "Player" : targetName;
    }

    @Override
    protected void init() {
        cx = width / 2;
        cy = height / 2;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        extractTransparentBackground(graphics);
        graphics.fill(cx - 90, cy - 70, cx + 90, cy + 75, 0xE0101428);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        ActiveTextCollector text = graphics.textRenderer(GuiGraphicsExtractor.HoveredTextEffects.NONE);
        text.accept(TextAlignment.CENTER, cx, cy - 58,
                Component.literal(targetName).withStyle(ChatFormatting.GOLD));
        text.accept(TextAlignment.CENTER, cx, cy - 46,
                Component.literal("What would you like to do?").withStyle(ChatFormatting.GRAY));

        drawBtn(graphics, text, mouseX, mouseY, cy - 24, "Battle", 0xE0502828, 0xF0A04040);
        drawBtn(graphics, text, mouseX, mouseY, cy + 2, "Trade", 0xE0285040, 0xF0408060);
        drawBtn(graphics, text, mouseX, mouseY, cy + 28, "Cancel", 0xE0404040, 0xF0606060);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void drawBtn(
            GuiGraphicsExtractor g, ActiveTextCollector text,
            int mx, int my, int y, String label, int idle, int hover
    ) {
        boolean hov = mx >= cx - 60 && mx < cx + 60 && my >= y && my < y + 20;
        g.fill(cx - 60, y, cx + 60, y + 20, hov ? hover : idle);
        text.accept(TextAlignment.CENTER, cx, y + 6,
                Component.literal(label).withStyle(ChatFormatting.WHITE));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) {
            return super.mouseClicked(event, doubleClick);
        }
        double mx = event.x();
        double my = event.y();
        if (mx >= cx - 60 && mx < cx + 60 && my >= cy - 24 && my < cy - 4) {
            ClientPacketDistributor.sendToServer(new com.cobblemon.mod.network.BattleChallengePayload(
                    com.cobblemon.mod.network.BattleChallengePayload.ACTION_REQUEST, targetId, targetName
            ));
            onClose();
            return true;
        }
        if (mx >= cx - 60 && mx < cx + 60 && my >= cy + 2 && my < cy + 22) {
            ClientPacketDistributor.sendToServer(new TradeRequestPayload(
                    TradeRequestPayload.ACTION_REQUEST, targetId, targetName
            ));
            onClose();
            return true;
        }
        if (mx >= cx - 60 && mx < cx + 60 && my >= cy + 28 && my < cy + 48) {
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

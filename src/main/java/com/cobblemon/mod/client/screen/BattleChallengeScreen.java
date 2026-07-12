package com.cobblemon.mod.client.screen;

import java.util.UUID;

import com.cobblemon.mod.network.BattleChallengePayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/** Accept / decline a PvP battle challenge. */
public class BattleChallengeScreen extends Screen {
    private final UUID fromId;
    private final String fromName;
    private int cx, cy;

    public BattleChallengeScreen(UUID fromId, String fromName) {
        super(Component.literal("Battle Challenge"));
        this.fromId = fromId;
        this.fromName = fromName == null ? "Player" : fromName;
    }

    @Override
    protected void init() {
        cx = width / 2;
        cy = height / 2;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        extractTransparentBackground(graphics);
        graphics.fill(cx - 100, cy - 55, cx + 100, cy + 55, 0xE0281018);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        ActiveTextCollector text = graphics.textRenderer(GuiGraphicsExtractor.HoveredTextEffects.NONE);
        text.accept(TextAlignment.CENTER, cx, cy - 40,
                Component.literal(fromName).withStyle(ChatFormatting.RED));
        text.accept(TextAlignment.CENTER, cx, cy - 28,
                Component.literal("challenged you to a battle!").withStyle(ChatFormatting.GRAY));
        drawBtn(graphics, text, mouseX, mouseY, cy - 2, "Accept", 0xE0285028, 0xF040A040);
        drawBtn(graphics, text, mouseX, mouseY, cy + 24, "Decline", 0xE0502828, 0xF0804040);
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
        if (mx >= cx - 60 && mx < cx + 60 && my >= cy - 2 && my < cy + 18) {
            ClientPacketDistributor.sendToServer(new BattleChallengePayload(
                    BattleChallengePayload.ACTION_ACCEPT, fromId, fromName
            ));
            onClose();
            return true;
        }
        if (mx >= cx - 60 && mx < cx + 60 && my >= cy + 24 && my < cy + 44) {
            ClientPacketDistributor.sendToServer(new BattleChallengePayload(
                    BattleChallengePayload.ACTION_DECLINE, fromId, fromName
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

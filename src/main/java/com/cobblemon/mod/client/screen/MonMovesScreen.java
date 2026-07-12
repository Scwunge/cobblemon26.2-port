package com.cobblemon.mod.client.screen;

import java.util.List;

import com.cobblemon.mod.battle.MonMove;
import com.cobblemon.mod.client.UiTextures;
import com.cobblemon.mod.species.OwnedMon;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * Official Cobblemon moves panel using summary_moves chrome.
 */
public class MonMovesScreen extends Screen {
    private final Screen parent;
    private final OwnedMon mon;
    private int baseX, baseY;

    public MonMovesScreen(Screen parent, OwnedMon mon) {
        super(Component.translatable("screen.cobblemon.moves"));
        this.parent = parent;
        this.mon = mon;
    }

    @Override
    protected void init() {
        int w = UiTextures.SUMMARY_SIDE_W + 16 + UiTextures.SUMMARY_MOVE_W;
        int h = UiTextures.SUMMARY_SIDE_H + 24;
        baseX = (this.width - w) / 2;
        baseY = (this.height - h) / 2;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0 && UiTextures.hit(event.x(), event.y(),
                baseX + UiTextures.SUMMARY_SIDE_W + UiTextures.SUMMARY_MOVE_W - 10,
                baseY - 4, 26, 26)) {
            if (minecraft != null) {
                minecraft.gui.setScreen(parent);
            }
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        this.extractTransparentBackground(graphics);
        graphics.fill(0, 0, this.width, this.height, 0xC0101828);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        ActiveTextCollector text = graphics.textRenderer(GuiGraphicsExtractor.HoveredTextEffects.NONE);

        UiTextures.blitNative(graphics, UiTextures.SUMMARY_MOVES, baseX, baseY,
                UiTextures.SUMMARY_SIDE_W, UiTextures.SUMMARY_SIDE_H);

        text.accept(baseX + 10, baseY + 8,
                mon.displayName().copy().append(Component.literal(" — Moves")).withStyle(ChatFormatting.WHITE));

        List<String> moves = mon.moveIds();
        int mx = baseX + UiTextures.SUMMARY_SIDE_W + 8;
        int my = baseY;
        for (int i = 0; i < 4; i++) {
            int y = my + i * (UiTextures.SUMMARY_MOVE_H + 4);
            UiTextures.blitNative(graphics, UiTextures.SUMMARY_MOVE, mx, y,
                    UiTextures.SUMMARY_MOVE_W, UiTextures.SUMMARY_MOVE_H);
            if (i < moves.size()) {
                MonMove m = MonMove.byIdOrDefault(moves.get(i));
                text.accept(mx + 8, y + 8,
                        Component.literal(m.englishName()).withStyle(ChatFormatting.WHITE));
                text.accept(mx + 8, y + 22,
                        Component.literal(m.element().englishName() + " · Pwr " + m.power() + " · Acc " + m.accuracy())
                                .withStyle(ChatFormatting.GRAY));
                UiTextures.typeIconSmall(graphics, m.element(), mx + UiTextures.SUMMARY_MOVE_W - 22, y + 6, 16);
            } else {
                text.accept(mx + 8, y + 16,
                        Component.literal("— empty —").withStyle(ChatFormatting.DARK_GRAY));
            }
        }

        UiTextures.blitNative(graphics, UiTextures.BACK_BUTTON,
                mx + UiTextures.SUMMARY_MOVE_W - 10, baseY - 4, 26, 26);

        text.accept(TextAlignment.CENTER, this.width / 2, baseY + UiTextures.SUMMARY_SIDE_H + 12,
                Component.literal("Back").withStyle(ChatFormatting.DARK_GRAY));

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

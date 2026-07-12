package com.cobblemon.mod.client.screen;

import java.util.ArrayList;
import java.util.List;

import com.cobblemon.mod.network.DialogueChoicePayload;
import com.cobblemon.mod.network.OpenDialoguePayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/** Simple multi-line dialogue with option buttons (N2). */
public class DialogueScreen extends Screen {
    private final String dialogueId;
    private final String pageId;
    private final List<String> lines;
    private final List<String> optionLabels;
    private final List<String> optionActions;
    private final int speakerEntityId;

    private int boxX, boxY, boxW, boxH;
    private int optY;

    public DialogueScreen(OpenDialoguePayload data) {
        super(Component.literal("Dialogue"));
        this.dialogueId = data == null ? "" : data.dialogueId();
        this.pageId = data == null ? "" : data.pageId();
        this.lines = data == null || data.lines() == null ? List.of() : new ArrayList<>(data.lines());
        this.optionLabels = data == null || data.optionLabels() == null ? List.of() : new ArrayList<>(data.optionLabels());
        this.optionActions = data == null || data.optionActions() == null ? List.of() : new ArrayList<>(data.optionActions());
        this.speakerEntityId = data == null ? -1 : data.speakerEntityId();
    }

    @Override
    protected void init() {
        boxW = Math.min(360, width - 40);
        boxH = 120 + optionLabels.size() * 22;
        boxX = (width - boxW) / 2;
        boxY = height - boxH - 24;
        optY = boxY + 70;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        extractTransparentBackground(graphics);
        graphics.fill(boxX, boxY, boxX + boxW, boxY + boxH, 0xE0101428);
        graphics.fill(boxX, boxY, boxX + boxW, boxY + 1, 0xFF5A7A9A);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        ActiveTextCollector text = graphics.textRenderer(GuiGraphicsExtractor.HoveredTextEffects.NONE);
        int ly = boxY + 12;
        for (String line : lines) {
            text.accept(boxX + 12, ly, Component.literal(line == null ? "" : line).withStyle(ChatFormatting.WHITE));
            ly += 12;
        }
        for (int i = 0; i < optionLabels.size(); i++) {
            int y = optY + i * 22;
            boolean hover = mouseX >= boxX + 16 && mouseX < boxX + boxW - 16 && mouseY >= y && mouseY < y + 18;
            graphics.fill(boxX + 16, y, boxX + boxW - 16, y + 18, hover ? 0xF0406088 : 0xE0283848);
            text.accept(TextAlignment.CENTER, width / 2, y + 5,
                    Component.literal(optionLabels.get(i)).withStyle(ChatFormatting.AQUA));
        }
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) {
            return super.mouseClicked(event, doubleClick);
        }
        double mx = event.x();
        double my = event.y();
        for (int i = 0; i < optionLabels.size(); i++) {
            int y = optY + i * 22;
            if (mx >= boxX + 16 && mx < boxX + boxW - 16 && my >= y && my < y + 18) {
                String action = i < optionActions.size() ? optionActions.get(i) : "close";
                if (action != null && (action.equalsIgnoreCase("close") || action.isBlank())) {
                    onClose();
                    return true;
                }
                ClientPacketDistributor.sendToServer(new DialogueChoicePayload(
                        dialogueId, pageId, i, speakerEntityId
                ));
                // Stay open for next: page; close for battle after send
                if (action != null && action.toLowerCase().startsWith("battle")) {
                    onClose();
                }
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

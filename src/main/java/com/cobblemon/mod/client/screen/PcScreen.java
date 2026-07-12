package com.cobblemon.mod.client.screen;

import com.cobblemon.mod.client.MonSpriteDrawer;
import com.cobblemon.mod.client.UiTextures;
import com.cobblemon.mod.network.PcActionPayload;
import com.cobblemon.mod.party.ModAttachments;
import com.cobblemon.mod.party.PlayerParty;
import com.cobblemon.mod.party.PlayerPc;
import com.cobblemon.mod.species.OwnedMon;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * Official Cobblemon PC: {@code pc_base} + party panel + box grid at native sizes.
 */
public class PcScreen extends Screen {
    private static final int COLS = 6;
    private static final int ROWS = 5;
    private static final int SLOT = UiTextures.PC_SLOT_S;

    private int currentBox = 0;
    private int baseX, baseY;
    private int partyX, partyY;
    private int gridX, gridY;
    private int prevX, nextX, navY;

    public PcScreen() {
        super(Component.translatable("screen.cobblemon.pc"));
    }

    private PlayerParty party() {
        return this.minecraft.player.getData(ModAttachments.PARTY);
    }

    private PlayerPc pc() {
        return this.minecraft.player.getData(ModAttachments.PC);
    }

    @Override
    protected void init() {
        baseX = (this.width - UiTextures.PC_BASE_W) / 2;
        baseY = (this.height - UiTextures.PC_BASE_H) / 2 - 6;
        // Layout matches official PC chrome proportions
        partyX = baseX + 12;
        partyY = baseY + 28;
        gridX = baseX + 110;
        gridY = baseY + 36;
        prevX = gridX - 2;
        nextX = gridX + UiTextures.PC_GRID_W - 12;
        navY = baseY + 10;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) {
            return super.mouseClicked(event, doubleClick);
        }
        int mx = (int) event.x();
        int my = (int) event.y();

        if (UiTextures.hit(mx, my, prevX, navY, 14, 28)) {
            currentBox = (currentBox + PlayerPc.BOX_COUNT - 1) % PlayerPc.BOX_COUNT;
            return true;
        }
        if (UiTextures.hit(mx, my, nextX, navY, 14, 28)) {
            currentBox = (currentBox + 1) % PlayerPc.BOX_COUNT;
            return true;
        }
        if (UiTextures.hit(mx, my, baseX + UiTextures.PC_BASE_W - 30, baseY + UiTextures.PC_BASE_H - 28, 26, 26)) {
            onClose();
            return true;
        }

        // Party column — deposit
        for (int i = 0; i < PlayerParty.MAX_SIZE; i++) {
            int sx = partyX + 8;
            int sy = partyY + 10 + i * (SLOT + 2);
            if (UiTextures.hit(mx, my, sx, sy, SLOT, SLOT)) {
                if (i < party().size()) {
                    ClientPacketDistributor.sendToServer(new PcActionPayload(
                            PcActionPayload.Action.DEPOSIT, i, currentBox));
                }
                return true;
            }
        }

        // Box grid — withdraw
        for (int i = 0; i < PlayerPc.BOX_SIZE; i++) {
            int col = i % COLS;
            int row = i / COLS;
            int sx = gridX + 4 + col * (SLOT + 1);
            int sy = gridY + 4 + row * (SLOT + 1);
            if (UiTextures.hit(mx, my, sx, sy, SLOT, SLOT)) {
                int global = PlayerPc.globalIndex(currentBox, i);
                if (pc().getGlobal(global).isPresent()) {
                    ClientPacketDistributor.sendToServer(new PcActionPayload(
                            PcActionPayload.Action.WITHDRAW, global, 0));
                }
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        this.extractTransparentBackground(graphics);
        graphics.fill(0, 0, this.width, this.height, 0xC0081018);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        ActiveTextCollector text = graphics.textRenderer(GuiGraphicsExtractor.HoveredTextEffects.NONE);

        UiTextures.blitNative(graphics, UiTextures.PC_BASE, baseX, baseY,
                UiTextures.PC_BASE_W, UiTextures.PC_BASE_H);

        // Wallpaper wash behind grid
        int wp = PlayerPc.wallpaperColor(pc().wallpaper(currentBox));
        graphics.fill(gridX, gridY, gridX + UiTextures.PC_GRID_W, gridY + UiTextures.PC_GRID_H, wp);

        UiTextures.blitNative(graphics, UiTextures.PC_PARTY_PANEL, partyX, partyY,
                UiTextures.PC_PARTY_W, UiTextures.PC_PARTY_H);
        UiTextures.blitNative(graphics, UiTextures.PC_GRID, gridX, gridY,
                UiTextures.PC_GRID_W, UiTextures.PC_GRID_H);

        UiTextures.blitNative(graphics, UiTextures.PC_ARROW_PREV, prevX, navY, 14, 28);
        UiTextures.blitNative(graphics, UiTextures.PC_ARROW_NEXT, nextX, navY, 14, 28);

        text.accept(TextAlignment.CENTER, baseX + UiTextures.PC_BASE_W / 2, baseY + 8,
                Component.literal(pc().boxName(currentBox) + "  (" + (currentBox + 1) + "/" + PlayerPc.BOX_COUNT + ")")
                        .withStyle(ChatFormatting.DARK_GRAY));

        PlayerParty party = party();
        for (int i = 0; i < PlayerParty.MAX_SIZE; i++) {
            int sx = partyX + 8;
            int sy = partyY + 10 + i * (SLOT + 2);
            UiTextures.blitNative(graphics, UiTextures.PC_SLOT, sx, sy, SLOT, SLOT);
            if (i < party.size()) {
                MonSpriteDrawer.draw(graphics, sx + 2, sy + 2, SLOT - 4, party.get(i).orElseThrow());
            }
        }

        PlayerPc storage = pc();
        for (int i = 0; i < PlayerPc.BOX_SIZE; i++) {
            int col = i % COLS;
            int row = i / COLS;
            int sx = gridX + 4 + col * (SLOT + 1);
            int sy = gridY + 4 + row * (SLOT + 1);
            UiTextures.blitNative(graphics, UiTextures.PC_SLOT, sx, sy, SLOT, SLOT);
            int global = PlayerPc.globalIndex(currentBox, i);
            storage.getGlobal(global).ifPresent(mon ->
                    MonSpriteDrawer.draw(graphics, sx + 2, sy + 2, SLOT - 4, mon));
        }

        text.accept(TextAlignment.CENTER, this.width / 2, baseY + UiTextures.PC_BASE_H + 6,
                Component.translatable("screen.cobblemon.pc_hint").withStyle(ChatFormatting.DARK_GRAY));
        text.accept(TextAlignment.CENTER, this.width / 2, baseY + UiTextures.PC_BASE_H + 16,
                Component.translatable("screen.cobblemon.pc_storage", storage.count(), PlayerPc.TOTAL_SLOTS)
                        .withStyle(ChatFormatting.GRAY));

        UiTextures.blitNative(graphics, UiTextures.BACK_BUTTON,
                baseX + UiTextures.PC_BASE_W - 30, baseY + UiTextures.PC_BASE_H - 28, 26, 26);

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

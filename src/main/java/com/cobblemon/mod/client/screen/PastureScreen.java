package com.cobblemon.mod.client.screen;

import java.util.ArrayList;
import java.util.List;

import com.cobblemon.mod.client.MonSpriteDrawer;
import com.cobblemon.mod.client.UiTextures;
import com.cobblemon.mod.network.OpenPasturePayload;
import com.cobblemon.mod.network.PastureActionPayload;
import com.cobblemon.mod.party.ModAttachments;
import com.cobblemon.mod.party.PlayerParty;
import com.cobblemon.mod.species.OwnedMon;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * E4 ranch GUI — party column + pasture grid, deposit / withdraw / force-breed.
 * Reuses PC chrome textures for a familiar layout.
 */
public class PastureScreen extends Screen {
    private static final int COLS = 6;
    private static final int ROWS = 4;
    private static final int SLOT = UiTextures.PC_SLOT_S;
    private static final int VISIBLE_SLOTS = COLS * ROWS;

    private final BlockPos pos;
    private int capacity;
    private int breedTicksLeft;
    private boolean hasCompatiblePair;
    private final List<OwnedMon> pastureMons = new ArrayList<>();

    private int baseX, baseY;
    private int partyX, partyY;
    private int gridX, gridY;
    private int breedBtnX, breedBtnY, breedBtnW, breedBtnH;
    private int page;
    private int selectedPasture = -1;
    private int selectedParty = -1;

    public PastureScreen(OpenPasturePayload data) {
        super(Component.translatable("screen.cobblemon.pasture"));
        this.pos = data.pos();
        apply(data);
    }

    public boolean isSameBlock(BlockPos other) {
        return other != null && other.equals(this.pos);
    }

    /** Refresh contents after a server action re-sends {@link OpenPasturePayload}. */
    public void apply(OpenPasturePayload data) {
        this.capacity = data.capacity();
        this.breedTicksLeft = data.breedTicksLeft();
        this.hasCompatiblePair = data.hasCompatiblePair();
        this.pastureMons.clear();
        if (data.mons() != null) {
            this.pastureMons.addAll(data.mons());
        }
        int maxPage = Math.max(0, (pastureMons.size() - 1) / VISIBLE_SLOTS);
        if (page > maxPage) {
            page = maxPage;
        }
        if (selectedPasture >= pastureMons.size()) {
            selectedPasture = -1;
        }
    }

    private PlayerParty party() {
        return this.minecraft.player.getData(ModAttachments.PARTY);
    }

    @Override
    protected void init() {
        baseX = (this.width - UiTextures.PC_BASE_W) / 2;
        baseY = (this.height - UiTextures.PC_BASE_H) / 2 - 6;
        partyX = baseX + 12;
        partyY = baseY + 28;
        gridX = baseX + 110;
        gridY = baseY + 36;
        breedBtnW = 72;
        breedBtnH = 18;
        breedBtnX = baseX + UiTextures.PC_BASE_W - breedBtnW - 36;
        breedBtnY = baseY + UiTextures.PC_BASE_H - 30;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) {
            return super.mouseClicked(event, doubleClick);
        }
        int mx = (int) event.x();
        int my = (int) event.y();

        // Close
        if (UiTextures.hit(mx, my, baseX + UiTextures.PC_BASE_W - 30, baseY + UiTextures.PC_BASE_H - 28, 26, 26)) {
            onClose();
            return true;
        }

        // Breed button
        if (UiTextures.hit(mx, my, breedBtnX, breedBtnY, breedBtnW, breedBtnH)) {
            ClientPacketDistributor.sendToServer(new PastureActionPayload(
                    PastureActionPayload.Action.BREED, pos, 0));
            return true;
        }

        // Page arrows
        int prevX = gridX - 2;
        int nextX = gridX + UiTextures.PC_GRID_W - 12;
        int navY = baseY + 10;
        if (UiTextures.hit(mx, my, prevX, navY, 14, 28)) {
            if (page > 0) {
                page--;
            }
            return true;
        }
        if (UiTextures.hit(mx, my, nextX, navY, 14, 28)) {
            int maxPage = Math.max(0, (Math.max(pastureMons.size(), 1) - 1) / VISIBLE_SLOTS);
            if (page < maxPage) {
                page++;
            }
            return true;
        }

        // Party → deposit
        for (int i = 0; i < PlayerParty.MAX_SIZE; i++) {
            int sx = partyX + 8;
            int sy = partyY + 10 + i * (SLOT + 2);
            if (UiTextures.hit(mx, my, sx, sy, SLOT, SLOT)) {
                selectedParty = i;
                selectedPasture = -1;
                if (i < party().size()) {
                    ClientPacketDistributor.sendToServer(new PastureActionPayload(
                            PastureActionPayload.Action.DEPOSIT, pos, i));
                }
                return true;
            }
        }

        // Pasture grid → withdraw
        int start = page * VISIBLE_SLOTS;
        for (int i = 0; i < VISIBLE_SLOTS; i++) {
            int col = i % COLS;
            int row = i / COLS;
            int sx = gridX + 4 + col * (SLOT + 1);
            int sy = gridY + 4 + row * (SLOT + 1);
            if (UiTextures.hit(mx, my, sx, sy, SLOT, SLOT)) {
                int idx = start + i;
                selectedPasture = idx < pastureMons.size() ? idx : -1;
                selectedParty = -1;
                if (idx < pastureMons.size()) {
                    ClientPacketDistributor.sendToServer(new PastureActionPayload(
                            PastureActionPayload.Action.WITHDRAW, pos, idx));
                }
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        this.extractTransparentBackground(graphics);
        graphics.fill(0, 0, this.width, this.height, 0xC0102010);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        ActiveTextCollector text = graphics.textRenderer(GuiGraphicsExtractor.HoveredTextEffects.NONE);

        UiTextures.blitNative(graphics, UiTextures.PC_BASE, baseX, baseY,
                UiTextures.PC_BASE_W, UiTextures.PC_BASE_H);

        // Soft green wash for ranch
        graphics.fill(gridX, gridY, gridX + UiTextures.PC_GRID_W, gridY + UiTextures.PC_GRID_H, 0x55204018);

        UiTextures.blitNative(graphics, UiTextures.PC_PARTY_PANEL, partyX, partyY,
                UiTextures.PC_PARTY_W, UiTextures.PC_PARTY_H);
        UiTextures.blitNative(graphics, UiTextures.PC_GRID, gridX, gridY,
                UiTextures.PC_GRID_W, UiTextures.PC_GRID_H);

        int prevX = gridX - 2;
        int nextX = gridX + UiTextures.PC_GRID_W - 12;
        int navY = baseY + 10;
        UiTextures.blitNative(graphics, UiTextures.PC_ARROW_PREV, prevX, navY, 14, 28);
        UiTextures.blitNative(graphics, UiTextures.PC_ARROW_NEXT, nextX, navY, 14, 28);

        text.accept(TextAlignment.CENTER, baseX + UiTextures.PC_BASE_W / 2, baseY + 8,
                Component.translatable("screen.cobblemon.pasture_title", pastureMons.size(), capacity)
                        .withStyle(ChatFormatting.DARK_GREEN));

        // Party column
        PlayerParty party = party();
        for (int i = 0; i < PlayerParty.MAX_SIZE; i++) {
            int sx = partyX + 8;
            int sy = partyY + 10 + i * (SLOT + 2);
            UiTextures.blitNative(graphics, UiTextures.PC_SLOT, sx, sy, SLOT, SLOT);
            if (i == selectedParty) {
                graphics.fill(sx - 1, sy - 1, sx + SLOT + 1, sy + SLOT + 1, 0x80FFFF55);
            }
            if (i < party.size()) {
                MonSpriteDrawer.draw(graphics, sx + 2, sy + 2, SLOT - 4, party.get(i).orElseThrow());
            }
        }

        // Pasture grid
        int start = page * VISIBLE_SLOTS;
        for (int i = 0; i < VISIBLE_SLOTS; i++) {
            int col = i % COLS;
            int row = i / COLS;
            int sx = gridX + 4 + col * (SLOT + 1);
            int sy = gridY + 4 + row * (SLOT + 1);
            UiTextures.blitNative(graphics, UiTextures.PC_SLOT, sx, sy, SLOT, SLOT);
            int idx = start + i;
            if (idx == selectedPasture) {
                graphics.fill(sx - 1, sy - 1, sx + SLOT + 1, sy + SLOT + 1, 0x8055FF88);
            }
            if (idx < pastureMons.size()) {
                MonSpriteDrawer.draw(graphics, sx + 2, sy + 2, SLOT - 4, pastureMons.get(idx));
            }
        }

        // Breed button
        int breedColor = hasCompatiblePair ? 0xFF3A8A3A : 0xFF444444;
        graphics.fill(breedBtnX, breedBtnY, breedBtnX + breedBtnW, breedBtnY + breedBtnH, breedColor);
        graphics.fill(breedBtnX + 1, breedBtnY + 1, breedBtnX + breedBtnW - 1, breedBtnY + breedBtnH - 1,
                hasCompatiblePair ? 0xFF56B856 : 0xFF555555);
        text.accept(TextAlignment.CENTER, breedBtnX + breedBtnW / 2, breedBtnY + 5,
                Component.translatable("screen.cobblemon.pasture_breed")
                        .withStyle(hasCompatiblePair ? ChatFormatting.WHITE : ChatFormatting.DARK_GRAY));

        int sec = Math.max(0, breedTicksLeft / 20);
        String pairNote = hasCompatiblePair
                ? Component.translatable("screen.cobblemon.pasture_pair_ready").getString()
                : Component.translatable("screen.cobblemon.pasture_pair_none").getString();
        text.accept(TextAlignment.CENTER, this.width / 2, baseY + UiTextures.PC_BASE_H + 4,
                Component.literal(pairNote + "  ·  auto " + (sec / 60) + "m " + (sec % 60) + "s")
                        .withStyle(ChatFormatting.GRAY));
        text.accept(TextAlignment.CENTER, this.width / 2, baseY + UiTextures.PC_BASE_H + 14,
                Component.translatable("screen.cobblemon.pasture_hint").withStyle(ChatFormatting.DARK_GRAY));

        UiTextures.blitNative(graphics, UiTextures.BACK_BUTTON,
                baseX + UiTextures.PC_BASE_W - 30, baseY + UiTextures.PC_BASE_H - 28, 26, 26);

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

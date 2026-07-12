package com.cobblemon.mod.client.screen;

import com.cobblemon.mod.client.ClientPartySettings;
import com.cobblemon.mod.client.LangNames;
import com.cobblemon.mod.client.MonSpriteDrawer;
import com.cobblemon.mod.client.UiTextures;
import com.cobblemon.mod.network.PartyActionPayload;
import com.cobblemon.mod.party.ModAttachments;
import com.cobblemon.mod.party.PlayerParty;
import com.cobblemon.mod.species.OwnedMon;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * Official Cobblemon party UI — {@code party_slot} chrome + summary-style side actions.
 */
public class PartyScreen extends Screen {
    private static final int SLOT_W = UiTextures.PARTY_SLOT_W;
    private static final int SLOT_H = UiTextures.PARTY_SLOT_H;
    private static final int SLOT_GAP = 2;
    private static final int BTN_H = 16;
    private static final int BTN_GAP = 3;

    private int selected = -1;
    private int swapFrom = -1;
    private boolean releaseArmed;

    private int originX;
    private int originY;
    private int actionX;
    private int actionY;

    public PartyScreen() {
        super(Component.translatable("screen.cobblemon.party"));
    }

    private PlayerParty party() {
        if (this.minecraft == null || this.minecraft.player == null) {
            return PlayerParty.empty();
        }
        return this.minecraft.player.getData(ModAttachments.PARTY);
    }

    @Override
    protected void init() {
        int stackH = PlayerParty.MAX_SIZE * (SLOT_H + SLOT_GAP) - SLOT_GAP;
        int totalW = SLOT_W + 12 + 100;
        this.originX = (this.width - totalW) / 2;
        this.originY = (this.height - stackH) / 2 - 12;
        this.actionX = originX + SLOT_W + 12;
        this.actionY = originY + 8;
    }

    private int slotY(int index) {
        return originY + index * (SLOT_H + SLOT_GAP);
    }

    private boolean isOverSlot(double mx, double my, int index) {
        return UiTextures.hit(mx, my, originX, slotY(index), SLOT_W, SLOT_H);
    }

    private record ActionBtn(String label, int x, int y, int w, int h, Runnable run) {
        boolean hit(double mx, double my) {
            return UiTextures.hit(mx, my, x, y, w, h);
        }
    }

    private ActionBtn[] actions() {
        int x = actionX;
        int y = actionY;
        int w = 96;
        return new ActionBtn[]{
                new ActionBtn("Lead", x, y, w, BTN_H, this::onMakeLead),
                new ActionBtn("Swap", x, y + (BTN_H + BTN_GAP), w, BTN_H, this::onStartSwap),
                new ActionBtn(releaseArmed ? "Confirm!" : "Release", x, y + 2 * (BTN_H + BTN_GAP), w, BTN_H, this::onRelease),
                new ActionBtn("Heal", x, y + 3 * (BTN_H + BTN_GAP), w, BTN_H, () -> {
                    send(PartyActionPayload.Action.HEAL, 0, 0);
                    releaseArmed = false;
                    swapFrom = -1;
                }),
                new ActionBtn("Summary", x, y + 4 * (BTN_H + BTN_GAP), w, BTN_H, () -> {
                    if (selected >= 0 && selected < party().size() && minecraft != null) {
                        minecraft.gui.setScreen(new MonSummaryScreen(this, party().get(selected).orElseThrow(), false));
                    }
                }),
                new ActionBtn("Moves", x, y + 5 * (BTN_H + BTN_GAP), w, BTN_H, () -> {
                    if (selected >= 0 && selected < party().size() && minecraft != null) {
                        minecraft.gui.setScreen(new MonMovesScreen(this, party().get(selected).orElseThrow()));
                    }
                }),
                new ActionBtn(ClientPartySettings.hudOnRight ? "HUD: Right" : "HUD: Left",
                        x, y + 6 * (BTN_H + BTN_GAP), w, BTN_H, ClientPartySettings::toggleSide),
                new ActionBtn("Done", x, y + 7 * (BTN_H + BTN_GAP), w, BTN_H, this::onClose),
        };
    }

    private void onMakeLead() {
        if (selected < 0) return;
        send(PartyActionPayload.Action.LEAD, selected, 0);
        selected = 0;
        swapFrom = -1;
        releaseArmed = false;
    }

    private void onStartSwap() {
        if (selected < 0) return;
        swapFrom = selected;
        releaseArmed = false;
    }

    private void onRelease() {
        if (selected < 0) return;
        if (!releaseArmed) {
            releaseArmed = true;
            swapFrom = -1;
            return;
        }
        send(PartyActionPayload.Action.RELEASE, selected, 0);
        selected = -1;
        swapFrom = -1;
        releaseArmed = false;
    }

    private void send(PartyActionPayload.Action action, int a, int b) {
        ClientPacketDistributor.sendToServer(new PartyActionPayload(action, a, b));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) {
            return super.mouseClicked(event, doubleClick);
        }
        double mx = event.x();
        double my = event.y();
        PlayerParty party = party();

        for (int i = 0; i < PlayerParty.MAX_SIZE; i++) {
            if (!isOverSlot(mx, my, i)) continue;
            if (i >= party.size()) {
                selected = -1;
                swapFrom = -1;
                releaseArmed = false;
                return true;
            }
            if (swapFrom >= 0 && swapFrom != i) {
                send(PartyActionPayload.Action.SWAP, swapFrom, i);
                selected = i;
                swapFrom = -1;
                releaseArmed = false;
                return true;
            }
            selected = i;
            releaseArmed = false;
            if (doubleClick && minecraft != null) {
                minecraft.gui.setScreen(new MonSummaryScreen(this, party.get(i).orElseThrow(), false));
            }
            return true;
        }

        for (ActionBtn b : actions()) {
            if (b.hit(mx, my)) {
                b.run().run();
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        this.extractTransparentBackground(graphics);
        graphics.fill(0, 0, this.width, this.height, 0xB0101828);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        ActiveTextCollector text = graphics.textRenderer(GuiGraphicsExtractor.HoveredTextEffects.NONE);
        PlayerParty party = party();

        text.accept(TextAlignment.CENTER, this.width / 2, originY - 14,
                Component.translatable("screen.cobblemon.party").withStyle(ChatFormatting.GOLD));
        text.accept(TextAlignment.CENTER, this.width / 2, originY - 4,
                Component.literal(party.size() + " / " + PlayerParty.MAX_SIZE).withStyle(ChatFormatting.GRAY));

        for (int i = 0; i < PlayerParty.MAX_SIZE; i++) {
            int sx = originX;
            int sy = slotY(i);
            boolean hover = isOverSlot(mouseX, mouseY, i);
            boolean isSel = i == selected || i == swapFrom;
            boolean filled = i < party.size();
            boolean fainted = filled && party.get(i).orElseThrow().isFainted();

            Identifier tex = slotTex(filled, fainted, isSel || hover);
            UiTextures.blitNative(graphics, tex, sx, sy, SLOT_W, SLOT_H);

            if (filled) {
                OwnedMon mon = party.get(i).orElseThrow();
                // Portrait sits left inside the official slot
                MonSpriteDrawer.draw(graphics, sx + 3, sy + 3, 20, mon);
                String name = LangNames.species(mon).getString();
                if (name.length() > 9) {
                    name = name.substring(0, 8) + "…";
                }
                ChatFormatting nameCol = i == 0 ? ChatFormatting.YELLOW : ChatFormatting.WHITE;
                text.accept(sx + 26, sy + 4,
                        Component.literal((i == 0 ? "★" : "") + name).withStyle(nameCol));
                text.accept(sx + 26, sy + 14,
                        Component.literal("Lv." + mon.level()).withStyle(ChatFormatting.GRAY));
                UiTextures.hpBar(graphics, sx + 26, sy + 23, 32, 4, mon.hp(), mon.maxHp());
            }
        }

        // Side action buttons styled like Cobblemon panels
        for (ActionBtn b : actions()) {
            boolean hover = b.hit(mouseX, mouseY);
            boolean danger = b.label().startsWith("Confirm") || b.label().equals("Release");
            int bg = hover ? 0xF0405068 : 0xE0283048;
            if (danger && releaseArmed) {
                bg = hover ? 0xF0A04040 : 0xE0803030;
            }
            graphics.fill(b.x(), b.y(), b.x() + b.w(), b.y() + b.h(), bg);
            graphics.fill(b.x(), b.y(), b.x() + b.w(), b.y() + 1, 0xFF5A6A80);
            text.accept(TextAlignment.CENTER, b.x() + b.w() / 2, b.y() + 4,
                    Component.literal(b.label()).withStyle(ChatFormatting.WHITE));
        }

        Component hint;
        if (releaseArmed) {
            hint = Component.translatable("screen.cobblemon.hint_release_confirm").withStyle(ChatFormatting.RED);
        } else if (swapFrom >= 0) {
            hint = Component.literal("Select another slot to swap").withStyle(ChatFormatting.YELLOW);
        } else if (selected >= 0) {
            hint = Component.literal("Selected · double-click for summary").withStyle(ChatFormatting.AQUA);
        } else {
            hint = Component.literal("Select a Pokémon").withStyle(ChatFormatting.GRAY);
        }
        text.accept(TextAlignment.CENTER, this.width / 2, originY + PlayerParty.MAX_SIZE * (SLOT_H + SLOT_GAP) + 8, hint);

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private static Identifier slotTex(boolean filled, boolean fainted, boolean active) {
        if (!filled) {
            return UiTextures.PARTY_SLOT;
        }
        if (fainted) {
            return active ? UiTextures.PARTY_SLOT_FAINTED_ACTIVE : UiTextures.PARTY_SLOT_FAINTED;
        }
        return active ? UiTextures.PARTY_SLOT_ACTIVE : UiTextures.PARTY_SLOT;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

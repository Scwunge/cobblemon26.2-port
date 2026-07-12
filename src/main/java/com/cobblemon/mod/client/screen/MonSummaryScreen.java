package com.cobblemon.mod.client.screen;

import com.cobblemon.mod.client.MonSpriteDrawer;
import com.cobblemon.mod.client.UiTextures;
import com.cobblemon.mod.species.MonElement;
import com.cobblemon.mod.species.OwnedMon;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * Official Cobblemon summary layout: base + side panel + party strip + tabs.
 */
public class MonSummaryScreen extends Screen {
    private enum Tab { INFO, STATS, MOVES }

    private final Screen parent;
    private final OwnedMon mon;
    private Tab tab;

    private int baseX, baseY;
    private int sideX, sideY;
    private int partyX, partyY;
    private int tabY;

    public MonSummaryScreen(Screen parent, OwnedMon mon, boolean statsFocus) {
        super(Component.translatable("screen.cobblemon.summary"));
        this.parent = parent;
        this.mon = mon;
        this.tab = statsFocus ? Tab.STATS : Tab.INFO;
    }

    @Override
    protected void init() {
        int totalW = UiTextures.SUMMARY_BASE_W + 8 + UiTextures.SUMMARY_SIDE_W + 8 + UiTextures.SUMMARY_PARTY_W;
        int totalH = Math.max(UiTextures.SUMMARY_BASE_H, UiTextures.SUMMARY_SIDE_H) + 24;
        baseX = (this.width - totalW) / 2;
        baseY = (this.height - totalH) / 2;
        sideX = baseX + UiTextures.SUMMARY_BASE_W + 6;
        sideY = baseY + 8;
        partyX = sideX + UiTextures.SUMMARY_SIDE_W + 6;
        partyY = baseY + 8;
        tabY = baseY - 18;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) {
            return super.mouseClicked(event, doubleClick);
        }
        double mx = event.x();
        double my = event.y();

        // Tabs over base
        int tabW = 28;
        for (int i = 0; i < 3; i++) {
            int tx = baseX + 12 + i * (tabW + 4);
            if (UiTextures.hit(mx, my, tx, tabY, tabW, 16)) {
                tab = Tab.values()[i];
                if (tab == Tab.MOVES && minecraft != null) {
                    minecraft.gui.setScreen(new MonMovesScreen(this, mon));
                    return true;
                }
                return true;
            }
        }

        // Back button
        if (UiTextures.hit(mx, my, baseX + UiTextures.SUMMARY_BASE_W - 30, baseY + 4, 26, 26)) {
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
        MonElement type = mon.primaryType();

        // Tabs
        int tabW = 28;
        Tab[] tabs = {Tab.INFO, Tab.STATS, Tab.MOVES};
        net.minecraft.resources.Identifier[] tabIcons = {
                UiTextures.SUMMARY_TAB_INFO, UiTextures.SUMMARY_TAB_STATS, UiTextures.SUMMARY_TAB_MOVES
        };
        for (int i = 0; i < 3; i++) {
            int tx = baseX + 12 + i * (tabW + 4);
            boolean on = tab == tabs[i];
            UiTextures.blitNative(graphics, UiTextures.SUMMARY_TAB, tx, tabY, tabW, 16);
            UiTextures.blitNative(graphics, tabIcons[i], tx + 6, tabY, 16, 16);
            if (on) {
                graphics.fill(tx, tabY + 14, tx + tabW, tabY + 16, 0xFFFFD166);
            }
        }

        // Main base
        UiTextures.blitNative(graphics, UiTextures.SUMMARY_BASE, baseX, baseY,
                UiTextures.SUMMARY_BASE_W, UiTextures.SUMMARY_BASE_H);

        // Portrait
        int por = UiTextures.SUMMARY_PORTRAIT_S;
        int px = baseX + 14;
        int py = baseY + 28;
        UiTextures.blitNative(graphics, UiTextures.SUMMARY_PORTRAIT, px, py, por, por);
        MonSpriteDrawer.draw(graphics, px + 5, py + 5, por - 10, mon);

        // Name / meta on base
        int tx = baseX + 90;
        text.accept(tx, baseY + 18, mon.displayName().copy().withStyle(ChatFormatting.WHITE));
        text.accept(tx, baseY + 30,
                Component.literal("Lv." + mon.level() + "  " + mon.gender().symbol())
                        .withStyle(ChatFormatting.GRAY));
        if (mon.hasMark()) {
            text.accept(tx, baseY + 40,
                    Component.literal("★ " + com.cobblemon.mod.species.MarkAward.displayName(mon.mark()))
                            .withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        UiTextures.typeIconSmall(graphics, type, tx, baseY + 42, 14);
        mon.secondaryType().ifPresent(t -> UiTextures.typeIconSmall(graphics, t, tx + 16, baseY + 42, 14));

        UiTextures.hpBar(graphics, tx, baseY + 60, 120, 7, mon.hp(), mon.maxHp());
        text.accept(tx, baseY + 70,
                Component.literal(mon.hp() + " / " + mon.maxHp() + " HP").withStyle(ChatFormatting.DARK_GRAY));

        text.accept(tx, baseY + 88,
                Component.literal("Nature  " + mon.nature().displayName().getString()).withStyle(ChatFormatting.GRAY));
        text.accept(tx, baseY + 100,
                Component.literal("Ability " + mon.ability().displayName().getString()).withStyle(ChatFormatting.GRAY));
        if (mon.heldItem() != null && !mon.heldItem().isBlank()) {
            text.accept(tx, baseY + 112,
                    Component.literal("Held  " + mon.heldItem()).withStyle(ChatFormatting.GOLD));
        }

        // Side panel
        net.minecraft.resources.Identifier sideTex = switch (tab) {
            case STATS -> UiTextures.SUMMARY_STATS_CHART;
            case MOVES -> UiTextures.SUMMARY_MOVES;
            default -> UiTextures.SUMMARY_INFO;
        };
        UiTextures.blitNative(graphics, sideTex, sideX, sideY, UiTextures.SUMMARY_SIDE_W, UiTextures.SUMMARY_SIDE_H);

        if (tab == Tab.STATS || tab == Tab.INFO) {
            var st = mon.stats();
            int sy = sideY + 16;
            int lx = sideX + 10;
            text.accept(lx, sy, Component.literal("HP   " + st.hp()).withStyle(ChatFormatting.RED));
            text.accept(lx, sy + 14, Component.literal("Atk  " + st.atk()).withStyle(ChatFormatting.GOLD));
            text.accept(lx, sy + 28, Component.literal("Def  " + st.def()).withStyle(ChatFormatting.YELLOW));
            text.accept(lx, sy + 42, Component.literal("SpA  " + st.spAtk()).withStyle(ChatFormatting.LIGHT_PURPLE));
            text.accept(lx, sy + 56, Component.literal("SpD  " + st.spDef()).withStyle(ChatFormatting.AQUA));
            text.accept(lx, sy + 70, Component.literal("Spe  " + st.speed()).withStyle(ChatFormatting.GREEN));
            text.accept(lx, sy + 92,
                    Component.literal("EXP " + mon.exp()).withStyle(ChatFormatting.DARK_GRAY));
        }

        // Party strip (visual only — current mon highlighted by sprite)
        UiTextures.blitNative(graphics, UiTextures.SUMMARY_PARTY_SLOT, partyX, partyY,
                UiTextures.SUMMARY_PARTY_W, UiTextures.SUMMARY_PARTY_H);
        MonSpriteDrawer.draw(graphics, partyX + 7, partyY + 10, 32, mon);

        // Back
        UiTextures.blitNative(graphics, UiTextures.BACK_BUTTON,
                baseX + UiTextures.SUMMARY_BASE_W - 30, baseY + 4, 26, 26);

        text.accept(TextAlignment.CENTER, this.width / 2, baseY + UiTextures.SUMMARY_BASE_H + 10,
                Component.literal("Click tabs · back button to return").withStyle(ChatFormatting.DARK_GRAY));

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

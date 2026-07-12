package com.cobblemon.mod.client.screen;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import com.cobblemon.mod.client.MonSpriteDrawer;
import com.cobblemon.mod.client.UiTextures;
import com.cobblemon.mod.party.ModAttachments;
import com.cobblemon.mod.party.PlayerPokedex;
import com.cobblemon.mod.species.SpeciesHandle;
import com.cobblemon.mod.species.SpeciesRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * Official Cobblemon Pokédex shell (red base + screen + slots).
 */
public class PokedexScreen extends Screen {
    private static final int COLS = 6;
    private static final int ROWS = 5;
    private static final int SLOT = UiTextures.POKEDEX_SLOT_S;

    private EditBox search;
    private int scroll;
    private Filter filter = Filter.ALL;
    private List<String> filtered = List.of();
    private int selected = -1;

    private int baseX, baseY;
    private int gridX, gridY;
    private int detailX, detailY;
    private int filterAllX, filterSeenX, filterCaughtX, filterY;

    public PokedexScreen() {
        super(Component.literal("Pokédex"));
    }

    private PlayerPokedex dex() {
        if (minecraft == null || minecraft.player == null) {
            return PlayerPokedex.empty();
        }
        return minecraft.player.getData(ModAttachments.POKEDEX);
    }

    @Override
    protected void init() {
        baseX = (this.width - UiTextures.POKEDEX_W) / 2;
        baseY = (this.height - UiTextures.POKEDEX_H) / 2 - 10;
        gridX = baseX + 18;
        gridY = baseY + 48;
        detailX = baseX + 190;
        detailY = baseY + 40;
        filterY = baseY + 22;
        filterAllX = baseX + 18;
        filterSeenX = baseX + 58;
        filterCaughtX = baseX + 108;

        search = new EditBox(this.font, baseX + 18, baseY + UiTextures.POKEDEX_H - 28, 120, 16,
                Component.literal("Search"));
        search.setHint(Component.literal("Search…"));
        search.setResponder(s -> {
            scroll = 0;
            rebuild();
        });
        addRenderableWidget(search);
        rebuild();
    }

    private void rebuild() {
        PlayerPokedex d = dex();
        String q = search != null ? search.getValue().toLowerCase(Locale.ROOT).trim() : "";
        List<String> all = new ArrayList<>(SpeciesRegistry.all().keySet());
        all.sort(Comparator.comparingInt(s -> SpeciesHandle.of(s).nationalNumber()));
        List<String> out = new ArrayList<>();
        for (String id : all) {
            boolean seen = d.hasSeen(id);
            boolean caught = d.hasCaught(id);
            if (filter == Filter.SEEN && !seen) continue;
            if (filter == Filter.CAUGHT && !caught) continue;
            if (!q.isEmpty()) {
                String name = SpeciesHandle.of(id).displayName().getString().toLowerCase(Locale.ROOT);
                if (!id.contains(q) && !name.contains(q)) continue;
            }
            out.add(id);
        }
        filtered = out;
        if (selected >= filtered.size()) {
            selected = filtered.isEmpty() ? -1 : 0;
        }
    }

    private int pageSize() {
        return COLS * ROWS;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) {
            return super.mouseClicked(event, doubleClick);
        }
        double mx = event.x();
        double my = event.y();

        if (UiTextures.hit(mx, my, filterAllX, filterY, 36, 14)) {
            filter = Filter.ALL;
            scroll = 0;
            rebuild();
            return true;
        }
        if (UiTextures.hit(mx, my, filterSeenX, filterY, 44, 14)) {
            filter = Filter.SEEN;
            scroll = 0;
            rebuild();
            return true;
        }
        if (UiTextures.hit(mx, my, filterCaughtX, filterY, 50, 14)) {
            filter = Filter.CAUGHT;
            scroll = 0;
            rebuild();
            return true;
        }
        if (UiTextures.hit(mx, my, baseX + UiTextures.POKEDEX_W - 28, baseY + 8, 20, 20)) {
            onClose();
            return true;
        }

        for (int i = 0; i < pageSize(); i++) {
            int col = i % COLS;
            int row = i / COLS;
            int sx = gridX + col * (SLOT + 2);
            int sy = gridY + row * (SLOT + 2);
            if (UiTextures.hit(mx, my, sx, sy, SLOT, SLOT)) {
                int idx = scroll + i;
                if (idx < filtered.size()) {
                    selected = idx;
                }
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int step = COLS;
        int max = Math.max(0, filtered.size() - pageSize());
        scroll = Math.max(0, Math.min(scroll - (int) Math.signum(scrollY) * step, max));
        // snap to row
        scroll = (scroll / COLS) * COLS;
        return true;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        this.extractTransparentBackground(graphics);
        graphics.fill(0, 0, this.width, this.height, 0xC0080C18);
        UiTextures.blitNative(graphics, UiTextures.POKEDEX_BASE, baseX, baseY,
                UiTextures.POKEDEX_W, UiTextures.POKEDEX_H);
        UiTextures.blitNative(graphics, UiTextures.POKEDEX_SCREEN, baseX, baseY,
                UiTextures.POKEDEX_W, UiTextures.POKEDEX_H);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        ActiveTextCollector text = graphics.textRenderer(GuiGraphicsExtractor.HoveredTextEffects.NONE);
        PlayerPokedex d = dex();

        text.accept(baseX + 18, baseY + 8,
                Component.literal("Pokédex").withStyle(ChatFormatting.WHITE));
        text.accept(baseX + 80, baseY + 8,
                Component.literal(d.seenCount() + " seen · " + d.caughtCount() + " caught")
                        .withStyle(ChatFormatting.GRAY));

        // Filter chips
        drawChip(graphics, text, filterAllX, filterY, 36, "All", filter == Filter.ALL);
        drawChip(graphics, text, filterSeenX, filterY, 44, "Seen", filter == Filter.SEEN);
        drawChip(graphics, text, filterCaughtX, filterY, 50, "Caught", filter == Filter.CAUGHT);

        for (int i = 0; i < pageSize(); i++) {
            int col = i % COLS;
            int row = i / COLS;
            int sx = gridX + col * (SLOT + 2);
            int sy = gridY + row * (SLOT + 2);
            int idx = scroll + i;
            UiTextures.blitNative(graphics, UiTextures.POKEDEX_SLOT, sx, sy, SLOT, SLOT);
            if (idx < filtered.size()) {
                String id = filtered.get(idx);
                boolean seen = d.hasSeen(id);
                boolean caught = d.hasCaught(id);
                if (seen) {
                    MonSpriteDrawer.draw(graphics, sx + 2, sy + 2, SLOT - 4, id);
                } else {
                    UiTextures.blitNative(graphics, UiTextures.POKEDEX_SLOT_UNKNOWN, sx + 8, sy + 7, 8, 10);
                }
                if (caught) {
                    UiTextures.blitNative(graphics, UiTextures.POKEDEX_CAUGHT, sx + SLOT - 10, sy + 1, 10, 10);
                }
                if (idx == selected) {
                    graphics.fill(sx, sy, sx + SLOT, sy + SLOT, 0x50FFD166);
                }
            }
        }

        // Detail panel
        if (selected >= 0 && selected < filtered.size()) {
            String id = filtered.get(selected);
            boolean seen = d.hasSeen(id);
            boolean caught = d.hasCaught(id);
            SpeciesHandle h = SpeciesHandle.of(id);
            if (seen) {
                MonSpriteDrawer.draw(graphics, detailX + 20, detailY, 64, id);
                text.accept(detailX, detailY + 70, h.displayName().copy().withStyle(ChatFormatting.WHITE));
                text.accept(detailX, detailY + 84,
                        Component.literal("No. " + h.nationalNumber()).withStyle(ChatFormatting.GRAY));
                UiTextures.typeIconSmall(graphics, h.primaryType(), detailX, detailY + 98, 16);
                h.secondaryType().ifPresent(t -> UiTextures.typeIconSmall(graphics, t, detailX + 18, detailY + 98, 16));
                text.accept(detailX, detailY + 118,
                        Component.literal(caught ? "Caught" : "Seen")
                                .withStyle(caught ? ChatFormatting.GREEN : ChatFormatting.YELLOW));
            } else {
                text.accept(detailX + 20, detailY + 40,
                        Component.literal("???").withStyle(ChatFormatting.DARK_GRAY));
                text.accept(detailX, detailY + 70,
                        Component.literal("Not seen yet").withStyle(ChatFormatting.GRAY));
            }
        } else {
            text.accept(detailX, detailY + 40,
                    Component.literal("Select an entry").withStyle(ChatFormatting.GRAY));
        }

        UiTextures.blitNative(graphics, UiTextures.BACK_BUTTON,
                baseX + UiTextures.POKEDEX_W - 28, baseY + 8, 20, 20);

        text.accept(TextAlignment.CENTER, this.width / 2, baseY + UiTextures.POKEDEX_H + 8,
                Component.literal("Scroll wheel to page").withStyle(ChatFormatting.DARK_GRAY));

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private static void drawChip(GuiGraphicsExtractor g, ActiveTextCollector text,
                                 int x, int y, int w, String label, boolean on) {
        g.fill(x, y, x + w, y + 14, on ? 0xF0C04040 : 0xC0303040);
        text.accept(TextAlignment.CENTER, x + w / 2, y + 3,
                Component.literal(label).withStyle(ChatFormatting.WHITE));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private enum Filter { ALL, SEEN, CAUGHT }
}

package com.cobblemon.mod.client.screen;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.cobblemon.mod.client.MonSpriteDrawer;
import com.cobblemon.mod.client.UiTextures;
import com.cobblemon.mod.party.ModAttachments;
import com.cobblemon.mod.party.PlayerPokedex;
import com.cobblemon.mod.species.SpeciesHandle;
import com.cobblemon.mod.species.SpeciesRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * Pokédex UI using official Cobblemon shells from porter
 * ({@code pokedex_base_<color>} + {@code pokedex_screen} + slots/icons).
 */
public class PokedexScreen extends Screen {
    private static final int COLS = 6;
    private static final int ROWS = 5;
    private static final int SLOT = UiTextures.POKEDEX_SLOT_S; // 25
    private static final Set<String> COLORS = Set.of(
            "red", "blue", "green", "yellow", "pink", "black", "white"
    );

    private final String color;
    private EditBox search;
    private int scroll;
    private Filter filter = Filter.ALL;
    private List<String> filtered = List.of();
    private int selected = -1;

    private int baseX, baseY;
    private int gridX, gridY;
    private int detailX, detailY;

    public PokedexScreen() {
        this("red");
    }

    public PokedexScreen(String color) {
        super(Component.translatable("item.cobblemon.pokedex_" + safeColor(color)));
        this.color = safeColor(color);
    }

    private static String safeColor(String c) {
        if (c == null) {
            return "red";
        }
        String s = c.toLowerCase(Locale.ROOT);
        return COLORS.contains(s) ? s : "red";
    }

    private PlayerPokedex dex() {
        if (minecraft == null || minecraft.player == null) {
            return PlayerPokedex.empty();
        }
        return minecraft.player.getData(ModAttachments.POKEDEX);
    }

    private Identifier baseTex() {
        return UiTextures.gui("pokedex/pokedex_base_" + color + ".png");
    }

    @Override
    protected void init() {
        baseX = (this.width - UiTextures.POKEDEX_W) / 2;
        baseY = (this.height - UiTextures.POKEDEX_H) / 2 - 8;
        // Layout tuned to official 345×207 shell
        gridX = baseX + 14;
        gridY = baseY + 42;
        detailX = baseX + 188;
        detailY = baseY + 38;

        search = new EditBox(this.font, baseX + 14, baseY + UiTextures.POKEDEX_H - 26, 150, 14,
                Component.literal("Search"));
        search.setHint(Component.literal("Search…"));
        search.setBordered(true);
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
        if (all.isEmpty()) {
            // Fallback: Gen1 enum if index missing
            for (var sp : com.cobblemon.mod.species.MonSpecies.values()) {
                all.add(sp.id());
            }
        }
        all.sort(Comparator
                .comparingInt((String s) -> {
                    int n = SpeciesHandle.of(s).nationalNumber();
                    return n <= 0 ? 99999 : n;
                })
                .thenComparing(s -> s));

        List<String> out = new ArrayList<>();
        for (String id : all) {
            var progress = d.progress(id);
            boolean seen = progress.isSeen();
            boolean caught = progress.isCaught();
            if (filter == Filter.SEEN && !seen) {
                continue;
            }
            if (filter == Filter.CAUGHT && !caught) {
                continue;
            }
            if (!q.isEmpty()) {
                String name = SpeciesHandle.of(id).displayName().getString().toLowerCase(Locale.ROOT);
                String key = Component.translatable("cobblemon.species." + id + ".name")
                        .getString().toLowerCase(Locale.ROOT);
                if (!id.contains(q) && !name.contains(q) && !key.contains(q)
                        && !String.valueOf(SpeciesHandle.of(id).nationalNumber()).contains(q)) {
                    continue;
                }
            }
            out.add(id);
        }
        filtered = out;
        if (selected >= filtered.size()) {
            selected = filtered.isEmpty() ? -1 : Math.min(selected, filtered.size() - 1);
        }
        if (selected < 0 && !filtered.isEmpty()) {
            selected = 0;
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

        // Filters
        if (UiTextures.hit(mx, my, baseX + 14, baseY + 22, 34, 12)) {
            filter = Filter.ALL;
            scroll = 0;
            rebuild();
            return true;
        }
        if (UiTextures.hit(mx, my, baseX + 52, baseY + 22, 40, 12)) {
            filter = Filter.SEEN;
            scroll = 0;
            rebuild();
            return true;
        }
        if (UiTextures.hit(mx, my, baseX + 96, baseY + 22, 48, 12)) {
            filter = Filter.CAUGHT;
            scroll = 0;
            rebuild();
            return true;
        }
        // Close
        if (UiTextures.hit(mx, my, baseX + UiTextures.POKEDEX_W - 26, baseY + 6, 18, 18)) {
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
        int max = Math.max(0, filtered.size() - pageSize());
        scroll = Math.max(0, Math.min(scroll - (int) Math.signum(scrollY) * COLS, max));
        scroll = (scroll / COLS) * COLS;
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (key == 265) { // up
            selected = Math.max(0, selected - COLS);
            ensureSelectedVisible();
            return true;
        }
        if (key == 264) { // down
            selected = Math.min(filtered.size() - 1, selected + COLS);
            ensureSelectedVisible();
            return true;
        }
        if (key == 263) { // left
            selected = Math.max(0, selected - 1);
            ensureSelectedVisible();
            return true;
        }
        if (key == 262) { // right
            selected = Math.min(filtered.size() - 1, selected + 1);
            ensureSelectedVisible();
            return true;
        }
        return super.keyPressed(event);
    }

    private void ensureSelectedVisible() {
        if (selected < 0) {
            return;
        }
        if (selected < scroll) {
            scroll = (selected / COLS) * COLS;
        }
        if (selected >= scroll + pageSize()) {
            scroll = ((selected - pageSize() + COLS) / COLS) * COLS;
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        this.extractTransparentBackground(graphics);
        graphics.fill(0, 0, this.width, this.height, 0xB0000000);
        // Official coloured shell + screen overlay from porter
        UiTextures.blitNative(graphics, baseTex(), baseX, baseY, UiTextures.POKEDEX_W, UiTextures.POKEDEX_H);
        UiTextures.blitNative(graphics, UiTextures.POKEDEX_SCREEN, baseX, baseY,
                UiTextures.POKEDEX_W, UiTextures.POKEDEX_H);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        ActiveTextCollector text = graphics.textRenderer(GuiGraphicsExtractor.HoveredTextEffects.NONE);
        PlayerPokedex d = dex();

        text.accept(baseX + 14, baseY + 8,
                Component.literal("Pokédex").withStyle(ChatFormatting.WHITE));
        text.accept(baseX + 70, baseY + 8,
                Component.literal(d.seenCount() + " seen · " + d.caughtCount() + " caught · "
                                + SpeciesRegistry.size() + " total")
                        .withStyle(ChatFormatting.GRAY));

        drawChip(graphics, text, baseX + 14, baseY + 22, 34, "All", filter == Filter.ALL);
        drawChip(graphics, text, baseX + 52, baseY + 22, 40, "Seen", filter == Filter.SEEN);
        drawChip(graphics, text, baseX + 96, baseY + 22, 48, "Caught", filter == Filter.CAUGHT);

        for (int i = 0; i < pageSize(); i++) {
            int col = i % COLS;
            int row = i / COLS;
            int sx = gridX + col * (SLOT + 2);
            int sy = gridY + row * (SLOT + 2);
            int idx = scroll + i;
            UiTextures.blitNative(graphics, UiTextures.POKEDEX_SLOT, sx, sy, SLOT, SLOT);
            if (idx < filtered.size()) {
                String sid = filtered.get(idx);
                boolean seen = d.hasSeen(sid);
                boolean caught = d.hasCaught(sid);
                if (seen) {
                    MonSpriteDrawer.draw(graphics, sx + 2, sy + 2, SLOT - 4, sid);
                } else {
                    UiTextures.blitNative(graphics, UiTextures.POKEDEX_SLOT_UNKNOWN, sx + 7, sy + 6, 10, 12);
                }
                if (caught) {
                    UiTextures.blitNative(graphics, UiTextures.POKEDEX_CAUGHT, sx + SLOT - 9, sy + 1, 9, 9);
                }
                if (idx == selected) {
                    UiTextures.blitNative(graphics, UiTextures.POKEDEX_SLOT_SELECT, sx - 1, sy - 1, SLOT + 2, SLOT + 2);
                }
            }
        }

        // Detail panel (right of grid)
        if (selected >= 0 && selected < filtered.size()) {
            String sid = filtered.get(selected);
            boolean seen = d.hasSeen(sid);
            boolean caught = d.hasCaught(sid);
            SpeciesHandle h = SpeciesHandle.of(sid);

            graphics.fill(detailX - 4, detailY - 4, detailX + 140, detailY + 150, 0x66000000);

            if (seen) {
                MonSpriteDrawer.draw(graphics, detailX + 30, detailY, 72, sid);
                Component name = Component.translatable("cobblemon.species." + sid + ".name");
                if (name.getString().startsWith("cobblemon.species.")) {
                    name = h.displayName();
                }
                text.accept(detailX, detailY + 78, name.copy().withStyle(ChatFormatting.WHITE));
                int num = h.nationalNumber();
                text.accept(detailX, detailY + 90,
                        Component.literal(num > 0 ? String.format("No. %04d", num) : "No. ????")
                                .withStyle(ChatFormatting.GRAY));
                UiTextures.typeIconSmall(graphics, h.primaryType(), detailX, detailY + 102, 14);
                h.secondaryType().ifPresent(t ->
                        UiTextures.typeIconSmall(graphics, t, detailX + 16, detailY + 102, 14));
                text.accept(detailX, detailY + 120,
                        Component.literal(caught ? "Caught" : "Seen")
                                .withStyle(caught ? ChatFormatting.GREEN : ChatFormatting.YELLOW));
                drawWrapped(text, Component.translatable("cobblemon.species." + sid + ".desc"),
                        detailX, detailY + 132, 130, 3);
            } else {
                UiTextures.blitNative(graphics, UiTextures.POKEDEX_SLOT_UNKNOWN, detailX + 50, detailY + 30, 24, 28);
                text.accept(detailX + 30, detailY + 70,
                        Component.literal("???").withStyle(ChatFormatting.DARK_GRAY));
                text.accept(detailX, detailY + 90,
                        Component.literal("Not seen yet").withStyle(ChatFormatting.GRAY));
            }
        } else {
            text.accept(detailX, detailY + 40,
                    Component.literal("Select an entry").withStyle(ChatFormatting.GRAY));
        }

        UiTextures.blitNative(graphics, UiTextures.BACK_BUTTON,
                baseX + UiTextures.POKEDEX_W - 26, baseY + 6, 18, 18);

        int page = pageSize() <= 0 ? 1 : (scroll / pageSize()) + 1;
        int pages = pageSize() <= 0 ? 1 : Math.max(1, (filtered.size() + pageSize() - 1) / pageSize());
        text.accept(TextAlignment.CENTER, this.width / 2, baseY + UiTextures.POKEDEX_H + 6,
                Component.literal("Scroll · " + filtered.size() + " shown · page " + page + "/" + pages)
                        .withStyle(ChatFormatting.DARK_GRAY));

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void drawWrapped(ActiveTextCollector text, Component msg, int x, int y, int maxW, int maxLines) {
        String raw = msg.getString();
        if (raw.startsWith("cobblemon.species.")) {
            return;
        }
        var font = Minecraft.getInstance().font;
        int lineH = 8;
        int line = 0;
        StringBuilder cur = new StringBuilder();
        for (String word : raw.split(" ")) {
            String trial = cur.isEmpty() ? word : cur + " " + word;
            if (font.width(trial) > maxW && !cur.isEmpty()) {
                text.accept(x, y + line * lineH,
                        Component.literal(cur.toString()).withStyle(ChatFormatting.GRAY));
                line++;
                if (line >= maxLines) {
                    return;
                }
                cur = new StringBuilder(word);
            } else {
                cur = new StringBuilder(trial);
            }
        }
        if (!cur.isEmpty() && line < maxLines) {
            String t = cur.toString();
            if (line == maxLines - 1 && font.width(t) > maxW) {
                while (t.length() > 3 && font.width(t + "…") > maxW) {
                    t = t.substring(0, t.length() - 1);
                }
                t = t + "…";
            }
            text.accept(x, y + line * lineH, Component.literal(t).withStyle(ChatFormatting.GRAY));
        }
    }

    private static void drawChip(GuiGraphicsExtractor g, ActiveTextCollector text,
                                 int x, int y, int w, String label, boolean on) {
        g.fill(x, y, x + w, y + 12, on ? 0xF0C04040 : 0xC0282838);
        text.accept(TextAlignment.CENTER, x + w / 2, y + 2,
                Component.literal(label).withStyle(ChatFormatting.WHITE));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private enum Filter { ALL, SEEN, CAUGHT }
}

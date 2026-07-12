package com.cobblemon.mod.client.screen;

import java.util.List;

import com.cobblemon.mod.client.MonSpriteDrawer;
import com.cobblemon.mod.client.UiTextures;
import com.cobblemon.mod.network.ChooseStarterPayload;
import com.cobblemon.mod.species.MonElement;
import com.cobblemon.mod.species.SpeciesHandle;
import com.cobblemon.mod.species.StarterCatalog;
import com.cobblemon.mod.util.Lang;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * Starter selection UI matching official Cobblemon layout and behaviour:
 * type-tinted shell, region list, dex header, large preview, choose button,
 * bottom row of the three region starters + arrows.
 * <p>
 * Shell colour = selected mon primary type (Grass green / Fire red / Water blue…).
 */
public class StarterSelectScreen extends Screen {
    private static final int BASE_W = 200;
    private static final int BASE_H = 175;

    private static final int RAIL_W = 54;

    private static final int CAT_X = 2;
    private static final int CAT_Y = 10;
    private static final int CAT_W = 50;
    private static final int CAT_H = 15;
    private static final int CAT_GAP = 2;

    private static final int PREV_X = 72;
    private static final int PREV_Y = 46;
    private static final int PREV_W = 112;
    private static final int PREV_H = 70;

    private static final int BTN_X = 100;
    private static final int BTN_Y = 122;
    private static final int BTN_W = 56;
    private static final int BTN_H = 12;

    private static final int SLOT_Y = 147;
    private static final int SLOT_S = 22;
    private static final int[] SLOT_X = {90, 116, 142};

    private static final int ARROW_L_X = 72;
    private static final int ARROW_R_X = 182;
    private static final int ARROW_Y = 150;
    private static final int ARROW_W = 9;
    private static final int ARROW_H = 14;

    private static final int EXIT_X = 180;
    private static final int EXIT_Y = 3;
    private static final int EXIT_W = 16;
    private static final int EXIT_H = 12;

    private final List<StarterCatalog.Category> categories = StarterCatalog.categories();
    private int categoryIndex;
    private int starterIndex;
    private int baseX;
    private int baseY;

    public StarterSelectScreen() {
        super(Lang.ui("starter.title"));
    }

    private StarterCatalog.Category category() {
        return categories.get(Math.floorMod(categoryIndex, categories.size()));
    }

    private List<String> starters() {
        return category().speciesIds();
    }

    private String selectedId() {
        List<String> s = starters();
        return s.get(Math.floorMod(starterIndex, s.size()));
    }

    private int bx(int x) {
        return baseX + x;
    }

    private int by(int y) {
        return baseY + y;
    }

    /** Bright panel fill from type colour (official-style tint). */
    private int typePanelColor() {
        int rgb = SpeciesHandle.of(selectedId()).primaryType().rgb();
        // Keep saturation/brightness high so Fire reads red, not brown-grey
        return 0xFF000000 | lift(rgb, 1.15f);
    }

    private int typeHeaderColor() {
        int rgb = SpeciesHandle.of(selectedId()).primaryType().rgb();
        return 0xFF000000 | lift(rgb, 0.75f);
    }

    private static int lift(int rgb, float mul) {
        int r = Math.min(255, Math.round(((rgb >> 16) & 0xFF) * mul));
        int g = Math.min(255, Math.round(((rgb >> 8) & 0xFF) * mul));
        int b = Math.min(255, Math.round((rgb & 0xFF) * mul));
        return (r << 16) | (g << 8) | b;
    }

    @Override
    protected void init() {
        baseX = (this.width - BASE_W) / 2;
        baseY = (this.height - BASE_H) / 2;
    }

    private void confirm() {
        String id = selectedId();
        if (!StarterCatalog.isValidStarter(id)) {
            return;
        }
        ClientPacketDistributor.sendToServer(new ChooseStarterPayload(id));
        com.cobblemon.mod.client.ClientPartySettings.showThrowTipAfterStarter();
        onClose();
    }

    private void cycleStarter(int d) {
        List<String> s = starters();
        if (!s.isEmpty()) {
            starterIndex = Math.floorMod(starterIndex + d, s.size());
        }
    }

    private void selectCategory(int i) {
        if (i >= 0 && i < categories.size()) {
            categoryIndex = i;
            starterIndex = 0;
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) {
            return super.mouseClicked(event, doubleClick);
        }
        double mx = event.x();
        double my = event.y();

        if (UiTextures.hit(mx, my, bx(EXIT_X), by(EXIT_Y), EXIT_W, EXIT_H)) {
            onClose();
            return true;
        }
        for (int i = 0; i < categories.size(); i++) {
            int cy = by(CAT_Y) + i * (CAT_H + CAT_GAP);
            if (cy + CAT_H > by(BASE_H - 4)) {
                break;
            }
            if (UiTextures.hit(mx, my, bx(CAT_X), cy, CAT_W, CAT_H)) {
                selectCategory(i);
                return true;
            }
        }
        if (UiTextures.hit(mx, my, bx(ARROW_L_X), by(ARROW_Y), ARROW_W, ARROW_H)) {
            cycleStarter(-1);
            return true;
        }
        if (UiTextures.hit(mx, my, bx(ARROW_R_X), by(ARROW_Y), ARROW_W, ARROW_H)) {
            cycleStarter(1);
            return true;
        }
        List<String> s = starters();
        for (int i = 0; i < s.size() && i < 3; i++) {
            if (UiTextures.hit(mx, my, bx(SLOT_X[i]), by(SLOT_Y), SLOT_S, SLOT_S)) {
                starterIndex = i;
                if (doubleClick) {
                    confirm();
                }
                return true;
            }
        }
        if (UiTextures.hit(mx, my, bx(BTN_X), by(BTN_Y), BTN_W, BTN_H)) {
            confirm();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (key == 262 || key == 68) {
            cycleStarter(1);
            return true;
        }
        if (key == 263 || key == 65) {
            cycleStarter(-1);
            return true;
        }
        if (key == 265 || key == 87) {
            selectCategory(Math.max(0, categoryIndex - 1));
            return true;
        }
        if (key == 264 || key == 83) {
            selectCategory(Math.min(categories.size() - 1, categoryIndex + 1));
            return true;
        }
        if (key == 257 || key == 32) {
            confirm();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        this.extractTransparentBackground(graphics);
        graphics.fill(0, 0, this.width, this.height, 0x99000000);

        int panel = typePanelColor();
        int header = typeHeaderColor();

        // Outer silver border
        graphics.fill(baseX - 1, baseY - 1, baseX + BASE_W + 1, baseY + BASE_H + 1, 0xFFB8B8B8);
        graphics.fill(baseX, baseY, baseX + BASE_W, baseY + BASE_H, 0xFF1E2228);

        // Left category rail (always dark grey)
        graphics.fill(bx(0), by(0), bx(RAIL_W), by(BASE_H), 0xFF2C3038);

        // Right type-tinted shell — solid so it always shows (textures alone stay grey)
        graphics.fill(bx(RAIL_W), by(0), bx(BASE_W), by(BASE_H), panel);
        graphics.fill(bx(RAIL_W), by(0), bx(BASE_W), by(18), header);

        // Divider
        graphics.fill(bx(RAIL_W), by(0), bx(RAIL_W + 1), by(BASE_H), 0xFF101418);

        // Built-in header wells (name strip + desc box) — dark translucent
        graphics.fill(bx(RAIL_W + 4), by(20), bx(RAIL_W + 46), by(42), 0xCC14181C);
        graphics.fill(bx(RAIL_W + 48), by(20), bx(BASE_W - 4), by(42), 0xCC14181C);

        // Preview well + bottom slots (black)
        graphics.fill(bx(PREV_X), by(PREV_Y), bx(PREV_X + PREV_W), by(PREV_Y + PREV_H), 0xFF000000);
        for (int sx : SLOT_X) {
            graphics.fill(bx(sx), by(SLOT_Y), bx(sx + SLOT_S), by(SLOT_Y + SLOT_S), 0xFF000000);
        }

        // Light frame lines for polish
        outline(graphics, bx(0), by(0), BASE_W, BASE_H, 0xFFD0D0D0);
        outline(graphics, bx(PREV_X), by(PREV_Y), PREV_W, PREV_H, 0xFF3A3A3A);
    }

    private static void outline(GuiGraphicsExtractor g, int x, int y, int w, int h, int argb) {
        g.fill(x, y, x + w, y + 1, argb);
        g.fill(x, y + h - 1, x + w, y + h, argb);
        g.fill(x, y, x + 1, y + h, argb);
        g.fill(x + w - 1, y, x + w, y + h, argb);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        ActiveTextCollector text = graphics.textRenderer(GuiGraphicsExtractor.HoveredTextEffects.NONE);
        String id = selectedId();
        SpeciesHandle handle = SpeciesHandle.of(id);
        List<String> s = starters();

        // Title + exit
        text.accept(TextAlignment.CENTER, bx(RAIL_W + (BASE_W - RAIL_W) / 2), by(5),
                Lang.ui("starter.title").withStyle(ChatFormatting.WHITE));
        UiTextures.blitNative(graphics, UiTextures.STARTER_EXIT, bx(EXIT_X), by(EXIT_Y), EXIT_W, EXIT_H);

        // Categories
        for (int i = 0; i < categories.size(); i++) {
            int cy = by(CAT_Y) + i * (CAT_H + CAT_GAP);
            if (cy + CAT_H > by(BASE_H - 4)) {
                break;
            }
            int cx = bx(CAT_X);
            boolean on = i == categoryIndex;
            boolean hover = UiTextures.hit(mouseX, mouseY, cx, cy, CAT_W, CAT_H);
            int face = on ? 0xFF4A5568 : (hover ? 0xFF3A4250 : 0xFF323840);
            graphics.fill(cx, cy, cx + CAT_W, cy + CAT_H, face);
            outline(graphics, cx, cy, CAT_W, CAT_H, on ? 0xFF5AC8FF : 0xFF1A1E24);
            if (on) {
                graphics.fill(cx, cy, cx + 2, cy + CAT_H, 0xFF5AC8FF);
            }
            text.accept(cx + 5, cy + 4,
                    Component.literal(categories.get(i).displayName())
                            .withStyle(on ? ChatFormatting.WHITE : ChatFormatting.GRAY));
        }

        // Name (full, not clipped into a tiny well)
        Component name = Lang.speciesName(id);
        text.accept(bx(RAIL_W + 6), by(22), name.copy().withStyle(ChatFormatting.WHITE));

        // Type icons under name
        MonElement primary = handle.primaryType();
        UiTextures.typeIconSmall(graphics, primary, bx(RAIL_W + 6), by(31), 10);
        handle.secondaryType().ifPresent(sec ->
                UiTextures.typeIconSmall(graphics, sec, bx(RAIL_W + 18), by(31), 10));

        // Description in the larger header well
        drawWrapped(text,
                Lang.speciesDesc(id),
                bx(RAIL_W + 50), by(21), 92, 3);

        // Large mon only (no overlapping side sprites)
        int monS = 54;
        MonSpriteDrawer.draw(graphics,
                bx(PREV_X) + (PREV_W - monS) / 2,
                by(PREV_Y) + 6,
                monS, id);

        // Choose button
        int btnX = bx(BTN_X);
        int btnY = by(BTN_Y);
        boolean btnHover = UiTextures.hit(mouseX, mouseY, btnX, btnY, BTN_W, BTN_H);
        UiTextures.blitNative(graphics, UiTextures.STARTER_BUTTON, btnX, btnY, BTN_W, BTN_H);
        if (btnHover) {
            graphics.fill(btnX, btnY, btnX + BTN_W, btnY + BTN_H, 0x33FFFFFF);
        }
        text.accept(TextAlignment.CENTER, btnX + BTN_W / 2, btnY + 2,
                Lang.ui("starter.choosebutton").withStyle(ChatFormatting.DARK_GRAY));

        // Bottom three starters of this region
        for (int i = 0; i < 3; i++) {
            int sx = bx(SLOT_X[i]);
            int sy = by(SLOT_Y);
            if (i < s.size()) {
                boolean on = i == starterIndex;
                if (on) {
                    outline(graphics, sx - 1, sy - 1, SLOT_S + 2, SLOT_S + 2, 0xFF5AC8FF);
                }
                MonSpriteDrawer.draw(graphics, sx + 1, sy + 1, SLOT_S - 2, s.get(i));
            }
        }

        UiTextures.blitNative(graphics, UiTextures.STARTER_ARROW_L, bx(ARROW_L_X), by(ARROW_Y), ARROW_W, ARROW_H);
        UiTextures.blitNative(graphics, UiTextures.STARTER_ARROW_R, bx(ARROW_R_X), by(ARROW_Y), ARROW_W, ARROW_H);

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void drawWrapped(ActiveTextCollector text, Component msg, int x, int y, int maxW, int maxLines) {
        String raw = msg.getString();
        if (raw.startsWith("cobblemon.species.")) {
            raw = "A Pokémon.";
        }
        var font = Minecraft.getInstance().font;
        int lineH = 7;
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

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

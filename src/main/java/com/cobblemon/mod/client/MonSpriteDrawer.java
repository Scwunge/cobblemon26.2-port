package com.cobblemon.mod.client;

import com.cobblemon.mod.Cobblemon;
import com.cobblemon.mod.species.BodyShape;
import com.cobblemon.mod.species.MonElement;
import com.cobblemon.mod.species.MonSpecies;
import com.cobblemon.mod.species.OwnedMon;
import com.cobblemon.mod.species.SpeciesHandle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;

/**
 * UI portraits for party / battle / summary.
 * <p>
 * Never blits 3D model UV atlases (those look like "unfolded textures").
 * Prefers authored {@code textures/entity/sprites/front/<id>.png}, else a
 * type-colored chibi silhouette.
 */
public final class MonSpriteDrawer {
    private MonSpriteDrawer() {}

    public static Identifier frontSprite(String speciesId) {
        String id = speciesId == null ? "rattata" : speciesId.toLowerCase();
        return Identifier.fromNamespaceAndPath(
                Cobblemon.MOD_ID,
                "textures/entity/sprites/front/" + id + ".png"
        );
    }

    public static Identifier frontSprite(MonSpecies species) {
        return frontSprite(species == null ? "rattata" : species.id());
    }

    /** True only if a dedicated 2D portrait exists (not a model atlas). */
    public static boolean hasAuthoredSprite(String speciesId) {
        ResourceManager rm = Minecraft.getInstance().getResourceManager();
        return rm.getResource(frontSprite(speciesId)).isPresent();
    }

    public static void draw(GuiGraphicsExtractor g, int x, int y, int size, OwnedMon mon) {
        if (mon == null) {
            return;
        }
        draw(g, x, y, size, mon.speciesId());
    }

    public static void draw(GuiGraphicsExtractor g, int x, int y, int size, MonSpecies species) {
        if (species == null) {
            return;
        }
        draw(g, x, y, size, species.id());
    }

    public static void draw(GuiGraphicsExtractor g, int x, int y, int size, String speciesId) {
        if (size < 8) {
            return;
        }
        // No plate fill — starter / party chrome already provides the background

        String id = speciesId == null || speciesId.isBlank() ? "rattata" : speciesId.toLowerCase();

        // Authored 2D portrait only — never SpeciesAssets model UV textures
        if (hasAuthoredSprite(id)) {
            try {
                g.blit(frontSprite(id), x, y, x + size, y + size, 0f, 1f, 0f, 1f);
                return;
            } catch (Exception ignored) {
                // fall through to procedural
            }
        }

        drawProcedural(g, x, y, size, SpeciesHandle.of(id));
    }

    private static void drawProcedural(GuiGraphicsExtractor g, int x, int y, int size, SpeciesHandle handle) {
        MonSpecies enumSp = handle.asEnum().orElse(null);
        MonElement el = handle.primaryType();
        MonElement el2 = handle.secondaryType().orElse(null);

        int p = enumSp != null
                ? argb(enumSp.primaryColor())
                : el.argb(0xFF);
        int s = enumSp != null
                ? argb(enumSp.secondaryColor())
                : (el2 != null ? el2.argb(0xFF) : darken(p));
        int outline = 0xFF1A1420;
        int eye = 0xFF101018;
        int shine = 0xEEFFFFFF;
        int accent = el.argb(0xFF);
        int stage = Math.max(1, Math.min(3, handle.stage()));
        BodyShape shape = enumSp != null ? enumSp.bodyShape() : shapeForType(el, el2);

        // Legendary accents when enum match
        if (enumSp == MonSpecies.MEW || enumSp == MonSpecies.MEWTWO || enumSp == MonSpecies.MOLTRES) {
            drawGrok(g, x, y, size, enumSp, p, s, outline, eye, shine, accent, stage);
            int chip = Math.max(2, size / 6);
            g.fill(x + size - chip - 1, y + 1, x + size - 1, y + 1 + chip, accent);
            return;
        }

        switch (shape) {
            case AVIAN -> drawAvian(g, x, y, size, p, s, outline, eye, shine, accent, stage);
            case AQUATIC -> drawAquatic(g, x, y, size, p, s, outline, eye, shine, accent, stage);
            case SERPENT -> drawSerpent(g, x, y, size, p, s, outline, eye, shine, accent, stage);
            case INSECTOID -> drawInsect(g, x, y, size, p, s, outline, eye, shine, accent, stage);
            case AMORPH -> drawAmorph(g, x, y, size, p, s, outline, eye, shine, accent, stage);
            case ARMORED -> drawArmored(g, x, y, size, p, s, outline, eye, shine, accent, stage);
            case FEY -> drawFey(g, x, y, size, p, s, outline, eye, shine, accent, stage);
            case BIPED -> drawBiped(g, x, y, size, p, s, outline, eye, shine, accent, stage);
            default -> drawQuad(g, x, y, size, p, s, outline, eye, shine, accent, stage);
        }

        // type corner chip
        int chip = Math.max(2, size / 6);
        g.fill(x + size - chip - 1, y + 1, x + size - 1, y + 1 + chip, accent);
        g.fill(x + size - chip, y + 2, x + size - 2, y + chip, 0x55FFFFFF);

        // stage pips
        for (int i = 0; i < stage; i++) {
            int px = x + 1 + i * (chip);
            g.fill(px, y + size - chip, px + chip - 1, y + size - 1, 0xFF2A2A32);
            g.fill(px + 1, y + size - chip + 1, px + chip - 2, y + size - 2, accent);
        }
    }

    private static BodyShape shapeForType(MonElement primary, MonElement secondary) {
        MonElement t = primary != null ? primary : MonElement.NORMAL;
        MonElement t2 = secondary;
        if (t == MonElement.FLYING || t2 == MonElement.FLYING) return BodyShape.AVIAN;
        if (t == MonElement.WATER || t2 == MonElement.WATER) return BodyShape.AQUATIC;
        if (t == MonElement.BUG) return BodyShape.INSECTOID;
        if (t == MonElement.GHOST || t == MonElement.PSYCHIC) return BodyShape.AMORPH;
        if (t == MonElement.ROCK || t == MonElement.STEEL || t2 == MonElement.ROCK || t2 == MonElement.STEEL) {
            return BodyShape.ARMORED;
        }
        if (t == MonElement.DRAGON || t == MonElement.POISON) return BodyShape.SERPENT;
        if (t == MonElement.FAIRY) return BodyShape.FEY;
        if (t == MonElement.FIGHTING || t == MonElement.FIRE && t2 == MonElement.FLYING) return BodyShape.BIPED;
        if (t == MonElement.FIRE || t == MonElement.FIGHTING) return BodyShape.BIPED;
        return BodyShape.QUAD;
    }

    private static int darken(int argb) {
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        r = Math.max(0, r * 55 / 100);
        g = Math.max(0, g * 55 / 100);
        b = Math.max(0, b * 55 / 100);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static void drawQuad(
            GuiGraphicsExtractor g, int x, int y, int size,
            int p, int s, int o, int eye, int shine, int accent, int stage
    ) {
        int head = size * 11 / 16;
        int hx = x + (size - head) / 2;
        int hy = y + size / 10;
        int bw = size * 10 / 16;
        int bh = size * 6 / 16;
        int bx = x + (size - bw) / 2;
        int by = y + size - bh - size / 8;
        ovalish(g, bx, by, bw, bh, p, o);
        int lw = Math.max(2, size / 8);
        g.fill(bx + 1, by + bh - 1, bx + 1 + lw, y + size - 1, s);
        g.fill(bx + bw - lw - 1, by + bh - 1, bx + bw - 1, y + size - 1, s);
        ovalish(g, hx, hy, head, head, p, o);
        int ew = Math.max(2, size / 7);
        g.fill(hx + 1, hy - ew + 1, hx + 1 + ew, hy + 2, s);
        g.fill(hx + head - ew - 1, hy - ew + 1, hx + head - 1, hy + 2, s);
        g.fill(hx + 2, hy - ew + 2, hx + ew, hy + 1, accent);
        g.fill(hx + head - ew, hy - ew + 2, hx + head - 2, hy + 1, accent);
        g.fill(hx + head / 2 - size / 10, hy + head * 2 / 3, hx + head / 2 + size / 10, hy + head - 1, s);
        eyes(g, hx, hy, head, eye, shine, stage >= 2);
        cheek(g, hx, hy, head, accent);
        g.fill(bx + bw - 1, by + bh / 3, bx + bw + size / 8, by + bh / 3 + size / 8, accent);
    }

    private static void drawBiped(
            GuiGraphicsExtractor g, int x, int y, int size,
            int p, int s, int o, int eye, int shine, int accent, int stage
    ) {
        int head = size * 10 / 16;
        int hx = x + (size - head) / 2;
        int hy = y + 2;
        int bw = size * 7 / 16;
        int bh = size * 6 / 16;
        int bx = x + (size - bw) / 2;
        int by = hy + head - 2;
        ovalish(g, bx, by, bw, bh, p, o);
        int aw = Math.max(2, size / 10);
        g.fill(bx - aw, by + 2, bx + 1, by + bh - 1, s);
        g.fill(bx + bw - 1, by + 2, bx + bw + aw, by + bh - 1, s);
        int lw = Math.max(2, size / 8);
        g.fill(bx + 1, by + bh - 1, bx + 1 + lw, y + size - 1, s);
        g.fill(bx + bw - lw - 1, by + bh - 1, bx + bw - 1, y + size - 1, s);
        if (stage >= 2) {
            g.fill(hx + head / 2 - 1, hy - size / 6, hx + head / 2 + 2, hy + 2, accent);
        }
        ovalish(g, hx, hy, head, head, p, o);
        eyes(g, hx, hy, head, eye, shine, true);
        g.fill(bx + 1, by + bh / 2, bx + bw - 1, by + bh / 2 + 2, accent);
    }

    private static void drawAvian(
            GuiGraphicsExtractor g, int x, int y, int size,
            int p, int s, int o, int eye, int shine, int accent, int stage
    ) {
        int head = size * 9 / 16;
        int hx = x + (size - head) / 2;
        int hy = y + size / 8;
        int ww = size * 5 / 16;
        int wh = size * 4 / 16;
        g.fill(x + 1, hy + head / 2, x + 1 + ww, hy + head / 2 + wh, s);
        g.fill(x + size - 1 - ww, hy + head / 2, x + size - 1, hy + head / 2 + wh, s);
        g.fill(x + 2, hy + head / 2 + 1, x + ww, hy + head / 2 + wh - 1, accent);
        g.fill(x + size - ww, hy + head / 2 + 1, x + size - 2, hy + head / 2 + wh - 1, accent);
        int bw = size * 6 / 16;
        int bh = size * 5 / 16;
        int bx = x + (size - bw) / 2;
        int by = hy + head - 2;
        ovalish(g, bx, by, bw, bh, p, o);
        g.fill(bx + 1, by + bh - 1, bx + bw / 2, y + size - 1, 0xFFE8B060);
        g.fill(bx + bw / 2, by + bh - 1, bx + bw - 1, y + size - 1, 0xFFE8B060);
        ovalish(g, hx, hy, head, head, p, o);
        g.fill(hx + head / 2 - 1, hy + head * 2 / 3, hx + head / 2 + 2, hy + head, 0xFFFFC857);
        eyes(g, hx, hy, head, eye, shine, stage >= 2);
        if (stage >= 3) {
            g.fill(hx + head / 2 - 1, hy - 3, hx + head / 2 + 2, hy + 1, accent);
        }
    }

    private static void drawAquatic(
            GuiGraphicsExtractor g, int x, int y, int size,
            int p, int s, int o, int eye, int shine, int accent, int stage
    ) {
        int bodyW = size * 11 / 16;
        int bodyH = size * 8 / 16;
        int bx = x + (size - bodyW) / 2;
        int by = y + (size - bodyH) / 2;
        ovalish(g, bx, by, bodyW, bodyH, p, o);
        g.fill(bx + bodyW / 2 - 1, y + 2, bx + bodyW / 2 + 2, by + 2, accent);
        g.fill(bx + bodyW - 2, by + bodyH / 3, x + size - 1, by + bodyH * 2 / 3, s);
        g.fill(bx + bodyW - 1, by + bodyH / 3 + 1, x + size - 2, by + bodyH * 2 / 3 - 1, accent);
        g.fill(bx + 3, by + bodyH / 2, bx + bodyW - 3, by + bodyH - 2, s);
        eyes(g, bx + 2, by, bodyW * 3 / 4, eye, shine, true);
        g.fill(bx + bodyW - 5, by + bodyH / 2, bx + bodyW - 2, by + bodyH / 2 + 3, 0x88FFFFFF);
        if (stage >= 2) {
            g.fill(bx + 2, by + 2, bx + 4, by + 4, accent);
        }
    }

    private static void drawSerpent(
            GuiGraphicsExtractor g, int x, int y, int size,
            int p, int s, int o, int eye, int shine, int accent, int stage
    ) {
        int seg = Math.max(3, size / 5);
        for (int i = 0; i < 3 + stage; i++) {
            int sx = x + 2 + (i % 2) * (size / 6);
            int sy = y + size - 4 - i * (seg - 1);
            g.fill(sx, sy, sx + size - 4 - i, sy + seg, i % 2 == 0 ? p : s);
            g.fill(sx, sy, sx + size - 4 - i, sy + 1, o);
        }
        int head = size * 8 / 16;
        int hx = x + (size - head) / 2;
        int hy = y + 2;
        ovalish(g, hx, hy, head, head, p, o);
        g.fill(hx + 2, hy - 2, hx + 4, hy + 2, accent);
        g.fill(hx + head - 4, hy - 2, hx + head - 2, hy + 2, accent);
        eyes(g, hx, hy, head, eye, shine, true);
    }

    private static void drawInsect(
            GuiGraphicsExtractor g, int x, int y, int size,
            int p, int s, int o, int eye, int shine, int accent, int stage
    ) {
        int head = size * 7 / 16;
        int hx = x + (size - head) / 2;
        int hy = y + size / 6;
        g.fill(x + 1, hy, x + size / 3, hy + size / 3, 0xAAE8FFF0);
        g.fill(x + size - size / 3, hy, x + size - 1, hy + size / 3, 0xAAE8FFF0);
        int bw = size * 8 / 16;
        int bh = size * 6 / 16;
        int bx = x + (size - bw) / 2;
        int by = hy + head - 2;
        ovalish(g, bx, by, bw, bh, p, o);
        g.fill(bx + 2, by + 2, bx + bw - 2, by + 3, accent);
        g.fill(bx + 2, by + bh / 2, bx + bw - 2, by + bh / 2 + 1, accent);
        ovalish(g, hx, hy, head, head, s, o);
        g.fill(hx + 1, hy - 3, hx + 2, hy + 1, o);
        g.fill(hx + head - 2, hy - 3, hx + head - 1, hy + 1, o);
        g.fill(hx, hy - 4, hx + 3, hy - 2, accent);
        g.fill(hx + head - 3, hy - 4, hx + head, hy - 2, accent);
        eyes(g, hx, hy, head, eye, shine, stage >= 2);
    }

    private static void drawAmorph(
            GuiGraphicsExtractor g, int x, int y, int size,
            int p, int s, int o, int eye, int shine, int accent, int stage
    ) {
        int body = size * 12 / 16;
        int bx = x + (size - body) / 2;
        int by = y + (size - body) / 2 - 1;
        g.fill(bx - 1, by - 1, bx + body + 1, by + body + 1, 0x33FFFFFF & accent | 0x33000000);
        ovalish(g, bx, by, body, body, p, o);
        g.fill(bx + 2, by + body - 2, bx + 4, y + size - 1, s);
        g.fill(bx + body / 2, by + body - 1, bx + body / 2 + 2, y + size - 1, s);
        g.fill(bx + body - 4, by + body - 2, bx + body - 2, y + size - 1, s);
        eyes(g, bx + 2, by + 2, body - 4, eye, shine, true);
        g.fill(bx + body - 3, by + 2, bx + body - 1, by + 4, accent);
        if (stage >= 2) {
            g.fill(bx + 1, by + body / 3, bx + 3, by + body / 3 + 2, shine);
        }
    }

    private static void drawArmored(
            GuiGraphicsExtractor g, int x, int y, int size,
            int p, int s, int o, int eye, int shine, int accent, int stage
    ) {
        int body = size * 11 / 16;
        int bx = x + (size - body) / 2;
        int by = y + size / 5;
        g.fill(bx, by, bx + body, by + body, o);
        g.fill(bx + 1, by + 1, bx + body - 1, by + body - 1, p);
        g.fill(bx + 3, by + 3, bx + body - 3, by + body - 3, s);
        g.fill(bx + body / 2 - 1, by - size / 8, bx + body / 2 + 2, by + 2, accent);
        int lw = Math.max(2, size / 7);
        g.fill(bx + 2, by + body - 1, bx + 2 + lw, y + size - 1, s);
        g.fill(bx + body - lw - 2, by + body - 1, bx + body - 2, y + size - 1, s);
        eyes(g, bx + 2, by + 2, body - 4, eye, shine, stage >= 2);
        g.fill(bx + 2, by + body / 2, bx + body - 2, by + body / 2 + 1, o);
    }

    private static void drawFey(
            GuiGraphicsExtractor g, int x, int y, int size,
            int p, int s, int o, int eye, int shine, int accent, int stage
    ) {
        int head = size * 11 / 16;
        int hx = x + (size - head) / 2;
        int hy = y + size / 8;
        g.fill(x + 2, y + 2, x + 4, y + 4, accent);
        g.fill(x + size - 4, y + 3, x + size - 2, y + 5, shine);
        g.fill(x + 3, y + size - 4, x + 5, y + size - 2, accent);
        int bw = size * 7 / 16;
        int bh = size * 5 / 16;
        int bx = x + (size - bw) / 2;
        int by = hy + head - 3;
        ovalish(g, bx, by, bw, bh, p, o);
        ovalish(g, hx, hy, head, head, p, o);
        g.fill(hx, hy + 2, hx + 2, hy + head / 2, accent);
        g.fill(hx + head - 2, hy + 2, hx + head, hy + head / 2, accent);
        eyes(g, hx, hy, head, eye, shine, true);
        cheek(g, hx, hy, head, 0xFFFF8FB8);
        g.fill(hx + head / 2 - 1, hy - 2, hx + head / 2 + 2, hy + 1, shine);
    }

    private static void ovalish(GuiGraphicsExtractor g, int x, int y, int w, int h, int fill, int outline) {
        g.fill(x + 1, y, x + w - 1, y + h, outline);
        g.fill(x, y + 1, x + w, y + h - 1, outline);
        g.fill(x + 2, y + 1, x + w - 2, y + h - 1, fill);
        g.fill(x + 1, y + 2, x + w - 1, y + h - 2, fill);
        g.fill(x + 3, y + 2, x + w / 2, y + 3, 0x55FFFFFF);
    }

    private static void eyes(GuiGraphicsExtractor g, int hx, int hy, int head, int eye, int shine, boolean big) {
        int eyeY = hy + head * 5 / 12;
        int eyeH = big ? Math.max(2, head / 5) : Math.max(1, head / 7);
        int eyeW = Math.max(2, head / 6);
        int left = hx + head / 4;
        int right = hx + head - head / 4 - eyeW;
        g.fill(left, eyeY, left + eyeW, eyeY + eyeH, eye);
        g.fill(right, eyeY, right + eyeW, eyeY + eyeH, eye);
        g.fill(left + 1, eyeY, left + 2, eyeY + 1, shine);
        g.fill(right + 1, eyeY, right + 2, eyeY + 1, shine);
    }

    private static void cheek(GuiGraphicsExtractor g, int hx, int hy, int head, int color) {
        int cy = hy + head * 2 / 3;
        int cw = Math.max(2, head / 7);
        g.fill(hx + 2, cy, hx + 2 + cw, cy + Math.max(1, head / 10), color);
        g.fill(hx + head - 2 - cw, cy, hx + head - 2, cy + Math.max(1, head / 10), color);
    }

    private static int argb(int rgb) {
        return 0xFF000000 | (rgb & 0xFFFFFF);
    }

    private static void drawGrok(
            GuiGraphicsExtractor g, int x, int y, int size, MonSpecies species,
            int p, int s, int o, int eye, int shine, int accent, int stage
    ) {
        int cyan = 0xFF00E5FF;
        int violet = 0xFF7B61FF;
        int gold = 0xFFFFE566;

        g.fill(x + 2, y + 2, x + 3, y + 3, cyan);
        g.fill(x + size - 4, y + 3, x + size - 3, y + 4, violet);
        g.fill(x + 3, y + size - 4, x + 4, y + size - 3, shine);

        if (species == MonSpecies.MOLTRES || species == MonSpecies.MEW) {
            for (int i = 0; i < 4; i++) {
                int sx = x + 2 + (i % 2) * (size / 8);
                int sy = y + size - 5 - i * (size / 6);
                int sw = size - 4 - i * 2;
                g.fill(sx, sy, sx + sw, sy + size / 7, i % 2 == 0 ? p : s);
            }
            int head = size * 9 / 16;
            int hx = x + (size - head) / 2;
            int hy = y + 2;
            ovalish(g, hx, hy, head, head, p, o);
            g.fill(hx + head / 2 - 2, hy - 3, hx + head / 2 + 3, hy + 1, gold);
            int ex = hx + head / 2 - 2;
            int ey = hy + head / 2 - 1;
            g.fill(ex, ey, ex + 5, ey + 4, cyan);
            g.fill(ex + 1, ey + 1, ex + 4, ey + 3, eye);
            g.fill(ex + 2, ey + 1, ex + 3, ey + 2, shine);
            g.fill(hx + head / 3, hy + head * 3 / 4, hx + head * 2 / 3, hy + head * 3 / 4 + 1, cyan);
            return;
        }

        if (species == MonSpecies.MEWTWO) {
            int head = size * 10 / 16;
            int hx = x + (size - head) / 2;
            int hy = y + 2;
            g.fill(hx - 2, hy + 2, hx + 1, hy + head - 2, violet);
            g.fill(hx + head - 1, hy + 2, hx + head + 2, hy + head - 2, violet);
            g.fill(hx + head / 2 - 2, hy - 3, hx + head / 2 + 3, hy + 2, cyan);
            int bw = size * 7 / 16;
            int bh = size * 6 / 16;
            int bx = x + (size - bw) / 2;
            int by = hy + head - 3;
            ovalish(g, bx, by, bw, bh, p, o);
            g.fill(bx + bw - 1, by + 2, bx + bw + size / 8, by + bh, 0xFF1A1030);
            g.fill(bx - size / 10, by + 2, bx + 1, by + bh - 1, s);
            g.fill(bx + bw - 1, by + 2, bx + bw + size / 10, by + bh - 1, s);
            g.fill(bx + 1, by + bh - 1, bx + bw / 2 - 1, y + size - 1, p);
            g.fill(bx + bw / 2 + 1, by + bh - 1, bx + bw - 1, y + size - 1, p);
            ovalish(g, hx, hy, head, head, p, o);
            int ex = hx + head / 2 - 3;
            int ey = hy + head * 5 / 12;
            g.fill(ex, ey, ex + 7, ey + 5, cyan);
            g.fill(ex + 1, ey + 1, ex + 6, ey + 4, eye);
            g.fill(ex + 2, ey + 1, ex + 3, ey + 2, shine);
            g.fill(hx + head / 3, hy + head * 3 / 4, hx + head * 2 / 3 + 1, hy + head * 3 / 4 + 2, cyan);
            g.fill(bx + bw / 2 - 1, by + bh / 3, bx + bw / 2 + 2, by + bh / 3 + 3, gold);
            return;
        }

        int head = size * 11 / 16;
        int hx = x + (size - head) / 2;
        int hy = y + size / 8;
        int bw = size * 8 / 16;
        int bh = size * 5 / 16;
        int bx = x + (size - bw) / 2;
        int by = y + size - bh - size / 10;
        ovalish(g, bx, by, bw, bh, p, o);
        g.fill(bx + bw - 1, by + bh / 3, bx + bw + size / 7, by + bh / 3 + size / 8, cyan);
        g.fill(bx + 1, by + bh - 1, bx + 1 + size / 8, y + size - 1, s);
        g.fill(bx + bw - size / 8 - 1, by + bh - 1, bx + bw - 1, y + size - 1, s);
        g.fill(hx + 1, hy - size / 8, hx + 3, hy + 2, violet);
        g.fill(hx + head - 3, hy - size / 8, hx + head - 1, hy + 2, violet);
        ovalish(g, hx, hy, head, head, p, o);
        int ex = hx + head / 2 - 3;
        int ey = hy + head / 2 - 2;
        g.fill(ex, ey, ex + 7, ey + 5, cyan);
        g.fill(ex + 1, ey + 1, ex + 6, ey + 4, eye);
        g.fill(ex + 2, ey + 1, ex + 4, ey + 3, shine);
        g.fill(hx + head / 2 - 2, hy + head * 3 / 4, hx + head / 2 + 3, hy + head * 3 / 4 + 1, cyan);
    }
}

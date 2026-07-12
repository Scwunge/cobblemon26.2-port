package com.cobblemon.mod.client.screen;

import java.util.List;

import com.cobblemon.mod.battle.MonMove;
import com.cobblemon.mod.client.ClientBattle;
import com.cobblemon.mod.client.MonSpriteDrawer;
import com.cobblemon.mod.client.UiTextures;
import com.cobblemon.mod.network.BattleActionPayload;
import com.cobblemon.mod.network.BattleUpdatePayload;
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
 * Cobblemon battle UI.
 * <p>
 * Menu / move textures are <b>vertical 2-state sheets</b> (top = idle, bottom = active).
 * Only one half is drawn at a time — drawing the full PNG shows both states stacked
 * (the double Fight/Catch row bug).
 */
public class BattleScreen extends Screen {
    private enum Mode { MAIN, FIGHT, SWITCH }

    /** Drawn size of one menu button (half of 90×52 sheet). */
    private static final int MENU_W = 90;
    private static final int MENU_H = 26;
    private static final int MENU_GAP = 6;

    /** Drawn size of one move tile (half of 92×48 sheet). */
    private static final int MOVE_W = 92;
    private static final int MOVE_H = 24;
    private static final int MOVE_GAP_X = 8;
    private static final int MOVE_GAP_Y = 6;

    private static final int INFO_W = UiTextures.BATTLE_INFO_W;
    private static final int INFO_H = UiTextures.BATTLE_INFO_H;
    private static final int LOG_W = UiTextures.BATTLE_LOG_W;
    private static final int LOG_H = UiTextures.BATTLE_LOG_H;
    private static final int PORTRAIT = 40;

    private Mode mode = Mode.MAIN;
    private boolean waiting;
    private String lastPhase = "";

    private int wildInfoX, wildInfoY;
    private int playerInfoX, playerInfoY;
    private int wildSpriteX, wildSpriteY;
    private int playerSpriteX, playerSpriteY;
    private int logX, logY;
    private int menuX, menuY;
    private int moveOriginX, moveOriginY;

    public BattleScreen() {
        super(Component.translatable("screen.cobblemon.battle"));
    }

    @Override
    protected void init() {
        layout();
    }

    private void layout() {
        // Opponent — top right
        wildInfoX = this.width - INFO_W - 16;
        wildInfoY = 16;
        wildSpriteX = wildInfoX - PORTRAIT - 6;
        wildSpriteY = wildInfoY;

        // Player — bottom left, above menu
        int bottomStack = MENU_H + 10 + LOG_H + 8 + INFO_H + 8;
        playerInfoX = 16;
        playerInfoY = this.height - bottomStack + LOG_H + 8;
        playerSpriteX = playerInfoX + INFO_W + 6;
        playerSpriteY = playerInfoY - 4;

        // Log — center, above menu
        logX = (this.width - LOG_W) / 2;
        logY = this.height - MENU_H - 14 - LOG_H - 6;

        // Main menu — bottom center
        int menuTotalW = MENU_W * 4 + MENU_GAP * 3;
        menuX = (this.width - menuTotalW) / 2;
        menuY = this.height - MENU_H - 12;

        // Move grid — bottom center
        int moveTotalW = MOVE_W * 2 + MOVE_GAP_X;
        int moveTotalH = MOVE_H * 2 + MOVE_GAP_Y;
        moveOriginX = (this.width - moveTotalW) / 2;
        moveOriginY = this.height - moveTotalH - 28;
    }

    private static String sanitize(String name, String fallback) {
        if (name == null || name.isBlank() || name.contains(".")) {
            return fallback;
        }
        return name;
    }

    private static String sanitizeLog(String line) {
        if (line == null) return "";
        String out = replaceKeyPattern(line, "species\\.cobblemon\\.([a-z0-9_]+)", false);
        out = replaceKeyPattern(out, "move\\.cobblemon\\.([a-z0-9_]+)", true);
        out = replaceKeyPattern(out, "cobblemon\\.species\\.([a-z0-9_]+)\\.name", false);
        out = replaceKeyPattern(out, "cobblemon\\.move\\.([a-z0-9_]+)", true);
        return out;
    }

    private static String replaceKeyPattern(String input, String regex, boolean splitUnderscores) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(regex).matcher(input);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String id = m.group(1);
            String pretty;
            if (splitUnderscores) {
                String[] parts = id.split("_");
                StringBuilder name = new StringBuilder();
                for (String p : parts) {
                    if (p.isEmpty()) continue;
                    if (!name.isEmpty()) name.append(' ');
                    name.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
                }
                pretty = name.toString();
            } else {
                pretty = Character.toUpperCase(id.charAt(0)) + id.substring(1).replace('_', ' ');
            }
            m.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(pretty));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /** Draw top (idle) or bottom (active/colored) half of a 2-state vertical sheet. */
    private static void blitHalf(
            GuiGraphicsExtractor g,
            Identifier tex,
            int x, int y, int w, int h,
            boolean active
    ) {
        float v0 = active ? 0.5f : 0f;
        float v1 = active ? 1f : 0.5f;
        UiTextures.blitUv(g, tex, x, y, w, h, 0f, v0, 1f, v1);
    }

    private void send(int action, int arg) {
        if (waiting) return;
        waiting = true;
        mode = Mode.MAIN;
        ClientPacketDistributor.sendToServer(new BattleActionPayload(action, arg));
    }

    @Override
    public void tick() {
        super.tick();
        BattleUpdatePayload s = ClientBattle.state();
        if (s == null) return;
        if (s.ended()) {
            this.onClose();
            return;
        }
        lastPhase = s.phase();
        if ("PLAYER_TURN".equals(s.phase()) && waiting) {
            waiting = false;
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) {
            return super.mouseClicked(event, doubleClick);
        }
        BattleUpdatePayload s = ClientBattle.state();
        if (s == null || s.ended() || waiting || !"PLAYER_TURN".equals(s.phase())) {
            return super.mouseClicked(event, doubleClick);
        }

        double mx = event.x();
        double my = event.y();

        if (mode == Mode.MAIN) {
            for (int i = 0; i < 4; i++) {
                int bx = menuX + i * (MENU_W + MENU_GAP);
                if (UiTextures.hit(mx, my, bx, menuY, MENU_W, MENU_H)) {
                    switch (i) {
                        case 0 -> mode = Mode.FIGHT;
                        case 1 -> send(BattleActionPayload.ACTION_CATCH, 0);
                        case 2 -> mode = Mode.SWITCH;
                        case 3 -> send(BattleActionPayload.ACTION_RUN, 0);
                    }
                    return true;
                }
            }
        } else if (mode == Mode.FIGHT) {
            List<String> moves = s.moveIds();
            for (int i = 0; i < 4; i++) {
                int col = i % 2;
                int row = i / 2;
                int bx = moveOriginX + col * (MOVE_W + MOVE_GAP_X);
                int by = moveOriginY + row * (MOVE_H + MOVE_GAP_Y);
                if (UiTextures.hit(mx, my, bx, by, MOVE_W, MOVE_H)) {
                    if (i < moves.size()) {
                        send(BattleActionPayload.ACTION_MOVE, i);
                    }
                    return true;
                }
            }
            if (UiTextures.hit(mx, my, moveOriginX, moveOriginY + MOVE_H * 2 + MOVE_GAP_Y + 6, 70, 14)) {
                mode = Mode.MAIN;
                return true;
            }
        } else {
            int cardW = 90;
            int cardH = 28;
            int gridW = cardW * 3 + 8;
            int ox = (this.width - gridW) / 2;
            int oy = this.height - cardH * 2 - 40;
            for (int i = 0; i < 6; i++) {
                int col = i % 3;
                int row = i / 3;
                int bx = ox + col * (cardW + 4);
                int by = oy + row * (cardH + 4);
                if (UiTextures.hit(mx, my, bx, by, cardW, cardH)) {
                    send(BattleActionPayload.ACTION_SWITCH, i);
                    return true;
                }
            }
            if (UiTextures.hit(mx, my, ox, oy + cardH * 2 + 8, 70, 14)) {
                mode = Mode.MAIN;
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // Keep the world visible (battle is “in place”) — light dim only
        this.extractTransparentBackground(graphics);
        graphics.fill(0, 0, this.width, this.height, 0x60081018);
        // Soft bar behind menu (1px-wide underlay stretched)
        int stripY = this.height - 48;
        UiTextures.blit(graphics, UiTextures.SELECTION_UNDERLAY, 0, stripY, this.width, this.height);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        BattleUpdatePayload s = ClientBattle.state();
        if (s == null) {
            super.extractRenderState(graphics, mouseX, mouseY, partialTick);
            return;
        }
        ActiveTextCollector text = graphics.textRenderer(GuiGraphicsExtractor.HoveredTextEffects.NONE);

        String wildName = com.cobblemon.mod.species.SpeciesHandle.of(s.wildSpeciesId())
                .displayName().getString();
        String myName = sanitize(s.playerName(),
                com.cobblemon.mod.species.SpeciesHandle.of(s.playerSpeciesId())
                        .displayName().getString());

        // --- Wild (top-right) ---
        graphics.fill(wildSpriteX - 2, wildSpriteY - 2, wildSpriteX + PORTRAIT + 2, wildSpriteY + PORTRAIT + 2, 0xA0101018);
        MonSpriteDrawer.draw(graphics, wildSpriteX, wildSpriteY, PORTRAIT, s.wildSpeciesId());
        UiTextures.blitNative(graphics, UiTextures.BATTLE_INFO_FLIP, wildInfoX, wildInfoY, INFO_W, INFO_H);
        text.accept(wildInfoX + 28, wildInfoY + 6,
                Component.literal(wildName).withStyle(ChatFormatting.WHITE));
        text.accept(wildInfoX + 28, wildInfoY + 16,
                Component.literal("Lv." + s.wildLevel()).withStyle(ChatFormatting.DARK_GRAY));
        UiTextures.hpBar(graphics, wildInfoX + 28, wildInfoY + 28, INFO_W - 40, 5, s.wildHp(), s.wildMaxHp());

        // --- Player (bottom-left) ---
        graphics.fill(playerSpriteX - 2, playerSpriteY - 2, playerSpriteX + PORTRAIT + 2, playerSpriteY + PORTRAIT + 2, 0xA0101018);
        MonSpriteDrawer.draw(graphics, playerSpriteX, playerSpriteY, PORTRAIT, s.playerSpeciesId());
        UiTextures.blitNative(graphics, UiTextures.BATTLE_INFO, playerInfoX, playerInfoY, INFO_W, INFO_H);
        text.accept(playerInfoX + 28, playerInfoY + 6,
                Component.literal(myName).withStyle(ChatFormatting.WHITE));
        text.accept(playerInfoX + 28, playerInfoY + 16,
                Component.literal("Lv." + s.playerLevel()).withStyle(ChatFormatting.DARK_GRAY));
        UiTextures.hpBar(graphics, playerInfoX + 28, playerInfoY + 28, INFO_W - 40, 5, s.playerHp(), s.playerMaxHp());

        // --- Log ---
        UiTextures.blitNative(graphics, UiTextures.BATTLE_LOG, logX, logY, LOG_W, LOG_H);
        List<String> lines = s.logLines();
        int start = Math.max(0, lines.size() - 3);
        for (int i = start; i < lines.size(); i++) {
            text.accept(logX + 10, logY + 10 + (i - start) * 12,
                    Component.literal(sanitizeLog(lines.get(i))).withStyle(ChatFormatting.WHITE));
        }

        boolean canAct = "PLAYER_TURN".equals(s.phase()) && !waiting;

        if (mode == Mode.MAIN) {
            Identifier[] menus = {
                    UiTextures.BATTLE_MENU_FIGHT,
                    UiTextures.BATTLE_MENU_BAG,
                    UiTextures.BATTLE_MENU_SWITCH,
                    UiTextures.BATTLE_MENU_RUN
            };
            String[] labels = {"Fight", "Catch", "Switch", "Run"};
            for (int i = 0; i < 4; i++) {
                int bx = menuX + i * (MENU_W + MENU_GAP);
                boolean hover = canAct && UiTextures.hit(mouseX, mouseY, bx, menuY, MENU_W, MENU_H);
                // Always the colored half only (never full 90×52 sheet = double row)
                blitHalf(graphics, menus[i], bx, menuY, MENU_W, MENU_H, true);
                if (hover) {
                    graphics.fill(bx, menuY, bx + MENU_W, menuY + MENU_H, 0x28FFFFFF);
                } else if (!canAct) {
                    graphics.fill(bx, menuY, bx + MENU_W, menuY + MENU_H, 0x66000000);
                }
                text.accept(bx + 10, menuY + 9,
                        Component.literal(labels[i]).withStyle(ChatFormatting.WHITE));
            }
            if (canAct) {
                text.accept(TextAlignment.CENTER, this.width / 2, menuY - 12,
                        Component.translatable("screen.cobblemon.battle_what_do").withStyle(ChatFormatting.AQUA));
            } else {
                text.accept(TextAlignment.CENTER, this.width / 2, menuY - 12,
                        Component.translatable("screen.cobblemon.battle_waiting").withStyle(ChatFormatting.YELLOW));
            }
        } else if (mode == Mode.FIGHT) {
            List<String> moves = s.moveIds();
            for (int i = 0; i < 4; i++) {
                int col = i % 2;
                int row = i / 2;
                int bx = moveOriginX + col * (MOVE_W + MOVE_GAP_X);
                int by = moveOriginY + row * (MOVE_H + MOVE_GAP_Y);
                boolean hover = UiTextures.hit(mouseX, mouseY, bx, by, MOVE_W, MOVE_H);
                // Top half idle, bottom half pressed
                blitHalf(graphics, UiTextures.BATTLE_MOVE, bx, by, MOVE_W, MOVE_H, hover);
                if (i < moves.size()) {
                    MonMove m = MonMove.byIdOrDefault(moves.get(i));
                    String label = m.englishName();
                    if (label.length() > 12) {
                        label = label.substring(0, 11) + "…";
                    }
                    text.accept(bx + 6, by + 4,
                            Component.literal(label).withStyle(ChatFormatting.DARK_GRAY));
                    text.accept(bx + 6, by + 14,
                            Component.literal(m.power() + " PWR").withStyle(ChatFormatting.GRAY));
                    UiTextures.typeIconSmall(graphics, m.element(), bx + MOVE_W - 18, by + 4, 14);
                } else {
                    text.accept(bx + 8, by + 8, Component.literal("—").withStyle(ChatFormatting.DARK_GRAY));
                }
            }
            text.accept(moveOriginX, moveOriginY + MOVE_H * 2 + MOVE_GAP_Y + 6,
                    Component.literal("« Back").withStyle(ChatFormatting.YELLOW));
        } else {
            PlayerParty party = minecraft != null && minecraft.player != null
                    ? minecraft.player.getData(ModAttachments.PARTY) : PlayerParty.empty();
            int cardW = 90;
            int cardH = 28;
            int gridW = cardW * 3 + 8;
            int ox = (this.width - gridW) / 2;
            int oy = this.height - cardH * 2 - 40;
            for (int i = 0; i < 6; i++) {
                int col = i % 3;
                int row = i / 3;
                int bx = ox + col * (cardW + 4);
                int by = oy + row * (cardH + 4);
                boolean hover = UiTextures.hit(mouseX, mouseY, bx, by, cardW, cardH);
                // Compact party cards (avoid oversized party_select 94×58 sheet)
                graphics.fill(bx, by, bx + cardW, by + cardH, hover ? 0xE0405068 : 0xE0283040);
                graphics.fill(bx, by, bx + cardW, by + 1, 0xFF5A6A80);
                if (i < party.size()) {
                    OwnedMon mon = party.get(i).orElseThrow();
                    MonSpriteDrawer.draw(graphics, bx + 4, by + 4, 20, mon);
                    String nm = mon.displayName().getString();
                    if (nm.length() > 8) nm = nm.substring(0, 7) + "…";
                    text.accept(bx + 28, by + 5,
                            Component.literal(nm).withStyle(ChatFormatting.WHITE));
                    text.accept(bx + 28, by + 15,
                            Component.literal("Lv." + mon.level()).withStyle(ChatFormatting.GRAY));
                    UiTextures.hpBar(graphics, bx + 28, by + 22, 56, 3, mon.hp(), mon.maxHp());
                } else {
                    text.accept(bx + 10, by + 10,
                            Component.literal("(empty)").withStyle(ChatFormatting.DARK_GRAY));
                }
            }
            text.accept(ox, oy + cardH * 2 + 10,
                    Component.literal("« Back").withStyle(ChatFormatting.YELLOW));
        }

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        BattleUpdatePayload s = ClientBattle.state();
        if (s != null && !s.ended()) {
            ClientPacketDistributor.sendToServer(
                    new BattleActionPayload(BattleActionPayload.ACTION_RUN, 0));
        }
        ClientBattle.clear();
        super.onClose();
    }
}

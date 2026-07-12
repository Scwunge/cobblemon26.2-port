package com.cobblemon.mod.client;

import com.cobblemon.mod.client.screen.MonMovesScreen;
import com.cobblemon.mod.client.screen.MonSummaryScreen;
import com.cobblemon.mod.network.PartyActionPayload;
// LangNames is same package
import com.cobblemon.mod.party.ModAttachments;
import com.cobblemon.mod.party.PlayerParty;
import com.cobblemon.mod.species.OwnedMon;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * Cobblemon-style right-click menu on inventory party slots:
 * Summary / Moves / Stats / Release.
 */
public final class InventoryPartyMenu {
    private static final int MENU_W = 80;
    private static final int ROW_H = 14;
    private static final int PAD = 3;
    /** Header + 4 actions. */
    private static final int ROWS = 5;

    private static boolean open;
    private static int slotIndex = -1;
    private static int menuX;
    private static int menuY;

    private InventoryPartyMenu() {}

    public static boolean isOpen() {
        return open;
    }

    public static void close() {
        open = false;
        slotIndex = -1;
    }

    public static void openAt(int slot, int mouseX, int mouseY) {
        open = true;
        slotIndex = slot;
        menuX = mouseX + 2;
        menuY = mouseY + 2;
    }

    public static int menuHeight() {
        return PAD * 2 + ROWS * ROW_H;
    }

    public static void render(GuiGraphicsExtractor graphics, PlayerParty party, int mouseX, int mouseY) {
        if (!open || slotIndex < 0 || slotIndex >= party.size()) {
            return;
        }
        OwnedMon mon = party.get(slotIndex).orElse(null);
        if (mon == null) {
            close();
            return;
        }

        int h = menuHeight();
        int bg = 0xF0C6C6C6;
        int edgeL = 0xFFFFFFFF;
        int edgeD = 0xFF373737;

        graphics.fill(menuX, menuY, menuX + MENU_W, menuY + h, bg);
        graphics.fill(menuX, menuY, menuX + MENU_W, menuY + 1, edgeL);
        graphics.fill(menuX, menuY, menuX + 1, menuY + h, edgeL);
        graphics.fill(menuX, menuY + h - 1, menuX + MENU_W, menuY + h, edgeD);
        graphics.fill(menuX + MENU_W - 1, menuY, menuX + MENU_W, menuY + h, edgeD);

        ActiveTextCollector text = graphics.textRenderer(GuiGraphicsExtractor.HoveredTextEffects.NONE);

        // Header = mon name (never raw lang keys)
        String name = LangNames.species(mon).getString();
        if (name.length() > 12) {
            name = name.substring(0, 11) + "…";
        }
        text.accept(menuX + PAD + 2, menuY + PAD + 2,
                Component.literal(name).withStyle(ChatFormatting.DARK_GRAY));
        // header separator
        graphics.fill(menuX + 2, menuY + PAD + ROW_H, menuX + MENU_W - 2, menuY + PAD + ROW_H + 1, 0xFF8B8B8B);

        String[] labels = {
                "", // header
                "Summary",
                "Moves",
                "Stats",
                "Release"
        };
        for (int i = 1; i < ROWS; i++) {
            int ry = menuY + PAD + i * ROW_H;
            boolean hover = mouseX >= menuX && mouseX < menuX + MENU_W
                    && mouseY >= ry && mouseY < ry + ROW_H;
            if (hover) {
                graphics.fill(menuX + 1, ry, menuX + MENU_W - 1, ry + ROW_H, 0xFF9A9A9A);
            }
            // button face
            graphics.fill(menuX + 3, ry + 1, menuX + MENU_W - 3, ry + ROW_H - 1, hover ? 0xFFB0B0B0 : 0xFFA0A0A0);
            graphics.fill(menuX + 3, ry + 1, menuX + MENU_W - 3, ry + 2, 0xFFD0D0D0);
            graphics.fill(menuX + 3, ry + ROW_H - 2, menuX + MENU_W - 3, ry + ROW_H - 1, 0xFF606060);

            ChatFormatting color = i == 4 ? ChatFormatting.RED : ChatFormatting.DARK_GRAY;
            text.accept(menuX + PAD + 6, ry + 3, Component.literal(labels[i]).withStyle(color));
        }
    }

    /**
     * @return true if the click was consumed by the menu or opening a menu
     */
    public static boolean handleClick(AbstractContainerScreen<?> screen, int button, double mouseX, double mouseY) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return false;
        }
        PlayerParty party = mc.player.getData(ModAttachments.PARTY);
        int mx = (int) mouseX;
        int my = (int) mouseY;

        // Click while menu open
        if (open) {
            int h = menuHeight();
            boolean inside = mx >= menuX && mx < menuX + MENU_W && my >= menuY && my < menuY + h;
            if (!inside) {
                close();
                return button == 1; // swallow right-click outside to avoid confusion
            }
            if (button == 0) {
                int relY = my - menuY - PAD;
                int row = relY / ROW_H;
                if (row >= 1 && row <= 4 && slotIndex >= 0 && slotIndex < party.size()) {
                    OwnedMon mon = party.get(slotIndex).orElseThrow();
                    int slot = slotIndex;
                    close();
                    switch (row) {
                        case 1 -> mc.gui.setScreen(new MonSummaryScreen(screen, mon, false));
                        case 2 -> mc.gui.setScreen(new MonMovesScreen(screen, mon));
                        case 3 -> mc.gui.setScreen(new MonSummaryScreen(screen, mon, true));
                        case 4 -> ClientPacketDistributor.sendToServer(
                                new PartyActionPayload(PartyActionPayload.Action.RELEASE, slot, 0));
                    }
                    return true;
                }
            }
            return true;
        }

        // Right-click a filled party slot to open menu
        if (button == 1) {
            int slot = PartyHudRenderer.hitTestInventorySlot(screen, mx, my);
            if (slot >= 0 && slot < party.size()) {
                openAt(slot, mx, my);
                return true;
            }
        }

        return false;
    }
}

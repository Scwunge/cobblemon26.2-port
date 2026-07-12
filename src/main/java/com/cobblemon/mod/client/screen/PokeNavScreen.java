package com.cobblemon.mod.client.screen;

import com.cobblemon.mod.client.ClientHooks;
import com.cobblemon.mod.client.ClientParty;
import com.cobblemon.mod.network.RidePayload;
import com.cobblemon.mod.network.SendOutPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * P1 — radial-style interaction wheel / PokeNav hub.
 * Opens party, dex, send-out, ride, badges list.
 */
public class PokeNavScreen extends Screen {
    private static final String[] LABELS = {
            "Party", "Pokédex", "Send Out", "Ride", "Badges", "Close"
    };
    private int cx, cy;

    public PokeNavScreen() {
        super(Component.literal("PokéNav"));
    }

    @Override
    protected void init() {
        cx = width / 2;
        cy = height / 2;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        extractTransparentBackground(graphics);
        graphics.fill(0, 0, width, height, 0xA0081020);
        graphics.fill(cx - 100, cy - 110, cx + 100, cy + 110, 0xE0182038);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        ActiveTextCollector text = graphics.textRenderer(GuiGraphicsExtractor.HoveredTextEffects.NONE);
        text.accept(TextAlignment.CENTER, cx, cy - 98,
                Component.literal("PokéNav").withStyle(ChatFormatting.AQUA));
        text.accept(TextAlignment.CENTER, cx, cy - 86,
                Component.literal("Quick actions").withStyle(ChatFormatting.DARK_GRAY));

        for (int i = 0; i < LABELS.length; i++) {
            int y = cy - 60 + i * 28;
            boolean hov = mouseX >= cx - 70 && mouseX < cx + 70 && mouseY >= y && mouseY < y + 22;
            graphics.fill(cx - 70, y, cx + 70, y + 22, hov ? 0xF04080A0 : 0xE0284058);
            text.accept(TextAlignment.CENTER, cx, y + 7,
                    Component.literal(LABELS[i]).withStyle(ChatFormatting.WHITE));
        }
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) {
            return super.mouseClicked(event, doubleClick);
        }
        double mx = event.x();
        double my = event.y();
        for (int i = 0; i < LABELS.length; i++) {
            int y = cy - 60 + i * 28;
            if (mx >= cx - 70 && mx < cx + 70 && my >= y && my < y + 22) {
                runAction(i);
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void runAction(int index) {
        switch (index) {
            case 0 -> {
                onClose();
                ClientParty.openScreen();
            }
            case 1 -> {
                onClose();
                ClientHooks.openPokedexScreen();
            }
            case 2 -> {
                ClientPacketDistributor.sendToServer(new SendOutPayload());
                onClose();
            }
            case 3 -> {
                ClientPacketDistributor.sendToServer(new RidePayload());
                onClose();
            }
            case 4 -> {
                // Show badge count via chat (server will answer /badges if typed; client-local summary)
                if (minecraft != null && minecraft.player != null) {
                    var badges = minecraft.player.getData(com.cobblemon.mod.party.ModAttachments.BADGES);
                    if (badges.count() == 0) {
                        minecraft.player.sendSystemMessage(
                                Component.literal("§7No gym badges yet."));
                    } else {
                        StringBuilder sb = new StringBuilder("§eBadges: §f");
                        for (String id : badges.ordered()) {
                            sb.append(com.cobblemon.mod.party.PlayerBadges.displayName(id)).append(" ");
                        }
                        minecraft.player.sendSystemMessage(Component.literal(sb.toString()));
                    }
                }
                onClose();
            }
            default -> onClose();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

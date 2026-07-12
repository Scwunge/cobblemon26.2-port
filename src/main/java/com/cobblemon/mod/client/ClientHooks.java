package com.cobblemon.mod.client;

import com.cobblemon.mod.client.screen.DialogueScreen;
import com.cobblemon.mod.client.screen.MonSelectScreen;
import com.cobblemon.mod.client.screen.PastureScreen;
import com.cobblemon.mod.client.screen.PcScreen;
import com.cobblemon.mod.client.screen.PokedexScreen;
import com.cobblemon.mod.client.screen.StarterSelectScreen;
import com.cobblemon.mod.client.screen.TradeScreen;
import com.cobblemon.mod.network.OpenDialoguePayload;
import com.cobblemon.mod.network.OpenPasturePayload;
import com.cobblemon.mod.network.OpenTradePayload;
import net.minecraft.client.Minecraft;

/**
 * Client-only entrypoints called from common code on the client thread.
 */
public final class ClientHooks {
    private ClientHooks() {}

    public static void openPcScreen() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.gui.setScreen(new PcScreen());
        }
    }

    public static void openStarterScreen() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.gui.setScreen(new StarterSelectScreen());
        }
    }

    public static void openPokedexScreen() {
        openPokedexScreen("red");
    }

    public static void openPokedexScreen(String color) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.gui.setScreen(new PokedexScreen(color == null ? "red" : color));
        }
    }

    public static void openMonSelect(int hand, String itemId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.gui.setScreen(new MonSelectScreen(hand, itemId));
        }
    }

    public static void openTradeScreen(OpenTradePayload payload) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && payload != null) {
            mc.gui.setScreen(new TradeScreen(payload));
        }
    }

    public static void openDialogueScreen(OpenDialoguePayload payload) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && payload != null) {
            mc.gui.setScreen(new DialogueScreen(payload));
        }
    }

    public static void openTradeRequestScreen(java.util.UUID fromId, String fromName) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.gui.setScreen(new com.cobblemon.mod.client.screen.TradeRequestScreen(fromId, fromName));
        }
    }

    public static void openPlayerInteract(java.util.UUID targetId, String targetName) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.gui.setScreen(new com.cobblemon.mod.client.screen.PlayerInteractScreen(targetId, targetName));
        }
    }

    public static void openPastureScreen(OpenPasturePayload payload) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || payload == null) {
            return;
        }
        if (mc.gui.screen() instanceof PastureScreen existing && existing.isSameBlock(payload.pos())) {
            existing.apply(payload);
            return;
        }
        mc.gui.setScreen(new PastureScreen(payload));
    }

    public static void openBattleChallengeScreen(java.util.UUID fromId, String fromName) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.gui.setScreen(new com.cobblemon.mod.client.screen.BattleChallengeScreen(fromId, fromName));
        }
    }

    public static void openPokeNav() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.gui.setScreen(new com.cobblemon.mod.client.screen.PokeNavScreen());
        }
    }
}


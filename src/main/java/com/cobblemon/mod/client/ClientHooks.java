package com.cobblemon.mod.client;

import com.cobblemon.mod.client.screen.MonSelectScreen;
import com.cobblemon.mod.client.screen.PcScreen;
import com.cobblemon.mod.client.screen.PokedexScreen;
import com.cobblemon.mod.client.screen.StarterSelectScreen;
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
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.gui.setScreen(new PokedexScreen());
        }
    }

    public static void openMonSelect(int hand, String itemId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.gui.setScreen(new MonSelectScreen(hand, itemId));
        }
    }
}


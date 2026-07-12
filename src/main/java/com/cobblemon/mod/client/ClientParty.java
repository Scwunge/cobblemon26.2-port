package com.cobblemon.mod.client;

import com.cobblemon.mod.client.screen.PartyScreen;
import com.cobblemon.mod.client.screen.StarterSelectScreen;
import com.cobblemon.mod.party.ModAttachments;
import net.minecraft.client.Minecraft;

/**
 * Client-only helpers for opening party / starter UIs.
 */
public final class ClientParty {
    private ClientParty() {}

    public static void openScreen() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        if (mc.player.getData(ModAttachments.PARTY).isEmpty()) {
            mc.gui.setScreen(new StarterSelectScreen());
        } else {
            mc.gui.setScreen(new PartyScreen());
        }
    }

    public static void openPartyOnly() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        mc.gui.setScreen(new PartyScreen());
    }

    public static void openStarter() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        if (!mc.player.getData(ModAttachments.PARTY).isEmpty()) {
            return;
        }
        mc.gui.setScreen(new StarterSelectScreen());
    }
}

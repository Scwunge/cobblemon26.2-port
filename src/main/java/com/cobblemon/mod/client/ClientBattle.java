package com.cobblemon.mod.client;

import com.cobblemon.mod.client.screen.BattleScreen;
import com.cobblemon.mod.network.BattleUpdatePayload;
import net.minecraft.client.Minecraft;

/**
 * Client-side battle state mirror.
 * <p>
 * While open, {@link BattleCamera} takes over the view (third-person framing of
 * both in-world mons) and {@link com.cobblemon.mod.client.screen.BattleScreen}
 * draws a transparent overlay on top.
 */
public final class ClientBattle {
    private static BattleUpdatePayload state;
    private static boolean open;

    private ClientBattle() {}

    public static void apply(BattleUpdatePayload payload) {
        state = payload;
        Minecraft mc = Minecraft.getInstance();
        if (payload.ended()) {
            open = false;
            BattleMusic.stop();
            if (mc.gui.screen() instanceof BattleScreen) {
                mc.gui.setScreen(null);
            }
            return;
        }
        open = true;
        if (!(mc.gui.screen() instanceof BattleScreen)) {
            mc.gui.setScreen(new BattleScreen());
        }
    }

    public static BattleUpdatePayload state() {
        return state;
    }

    public static boolean isOpen() {
        return open && state != null && !state.ended();
    }

    public static void clear() {
        state = null;
        open = false;
        BattleMusic.stop();
    }
}

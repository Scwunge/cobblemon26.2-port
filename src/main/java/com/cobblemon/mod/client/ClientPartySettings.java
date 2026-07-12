package com.cobblemon.mod.client;

/**
 * Client-only HUD prefs. Default rail matches Cobblemon-style LEFT party strip.
 */
public final class ClientPartySettings {
    /** When true, rail sits on the right; screenshot reference uses left. */
    public static boolean hudOnRight = false;
    /** In-world party rail visibility. */
    public static boolean hudVisible = true;
    /**
     * Post-starter tip ("throw out your Cobblemon / Press R").
     * Hidden when the player presses R (and when they dismiss it permanently this session).
     */
    public static boolean showThrowTip = false;

    private ClientPartySettings() {}

    public static void toggleSide() {
        hudOnRight = !hudOnRight;
    }

    public static void toggleVisible() {
        hudVisible = !hudVisible;
    }

    public static void showThrowTipAfterStarter() {
        showThrowTip = true;
    }

    public static void dismissThrowTip() {
        showThrowTip = false;
    }
}

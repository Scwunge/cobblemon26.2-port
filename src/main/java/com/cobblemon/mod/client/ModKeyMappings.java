package com.cobblemon.mod.client;

import com.mojang.blaze3d.platform.InputConstants;

import com.cobblemon.mod.Cobblemon;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;

@EventBusSubscriber(modid = Cobblemon.MOD_ID, value = Dist.CLIENT)
public final class ModKeyMappings {
    public static final KeyMapping.Category CATEGORY =
            new KeyMapping.Category(Identifier.fromNamespaceAndPath(Cobblemon.MOD_ID, "main"));

    public static final KeyMapping OPEN_PARTY = new KeyMapping(
            "key.cobblemon.open_party",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_P,
            CATEGORY
    );

    public static final KeyMapping OPEN_STARTER = new KeyMapping(
            "key.cobblemon.open_starter",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_M,
            CATEGORY
    );

    /** Send out / recall lead mon + dismiss throw tip (screenshot reference). */
    public static final KeyMapping SEND_OUT = new KeyMapping(
            "key.cobblemon.send_out",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_R,
            CATEGORY
    );

    public static final KeyMapping TOGGLE_HUD_SIDE = new KeyMapping(
            "key.cobblemon.toggle_hud_side",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_H,
            CATEGORY
    );

    public static final KeyMapping TOGGLE_HUD = new KeyMapping(
            "key.cobblemon.toggle_hud",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_O,
            CATEGORY
    );

    public static final KeyMapping OPEN_POKEDEX = new KeyMapping(
            "key.cobblemon.open_pokedex",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_J,
            CATEGORY
    );

    public static final KeyMapping RIDE = new KeyMapping(
            "key.cobblemon.ride",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_V,
            CATEGORY
    );

    private ModKeyMappings() {}

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.registerCategory(CATEGORY);
        event.register(OPEN_PARTY);
        event.register(OPEN_STARTER);
        event.register(SEND_OUT);
        event.register(TOGGLE_HUD_SIDE);
        event.register(TOGGLE_HUD);
        event.register(OPEN_POKEDEX);
        event.register(RIDE);
    }
}

package com.cobblemon.mod.client;

import com.cobblemon.mod.Cobblemon;
import com.cobblemon.mod.block.entity.ModBlockEntities;
import com.cobblemon.mod.entity.ModEntities;
import com.cobblemon.mod.item.ModItems;
import com.cobblemon.mod.client.screen.PokedexScreen;
import com.cobblemon.mod.network.RidePayload;
import com.cobblemon.mod.network.SendOutPayload;
import com.cobblemon.mod.party.ModAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@Mod(value = Cobblemon.MOD_ID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = Cobblemon.MOD_ID, value = Dist.CLIENT)
public class CobblemonClient {
    public CobblemonClient(ModContainer container) {
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        Cobblemon.LOGGER.info("Cobblemon ready — M starter · R send out · P party · H flip HUD");
    }

    @SubscribeEvent
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.WILD_MON.get(), WildMonRenderer::new);
        event.registerEntityRenderer(ModEntities.CUBE_BALL.get(), CubeBallRenderer::new);
        event.registerEntityRenderer(ModEntities.TRAINER_NPC.get(), TrainerNpcRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.FOSSIL_MACHINE.get(), FossilMachineRenderer::new);
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        if (mc.gui.screen() == null) {
            while (ModKeyMappings.OPEN_PARTY.consumeClick()) {
                ClientParty.openScreen();
            }
            while (ModKeyMappings.OPEN_STARTER.consumeClick()) {
                if (mc.player.getData(ModAttachments.PARTY).isEmpty()) {
                    ClientParty.openStarter();
                } else {
                    ClientParty.openPartyOnly();
                }
            }
            while (ModKeyMappings.SEND_OUT.consumeClick()) {
                ClientPartySettings.dismissThrowTip();
                // Cobblemon-style: R on another player → interact (trade), else send out
                if (tryOpenPlayerInteract(mc)) {
                    // opened trade/interact UI
                } else {
                    ClientPacketDistributor.sendToServer(new SendOutPayload());
                }
            }
            while (ModKeyMappings.TOGGLE_HUD_SIDE.consumeClick()) {
                ClientPartySettings.toggleSide();
                mc.player.sendSystemMessage(
                        Component.translatable(ClientPartySettings.hudOnRight
                                ? "message.cobblemon.hud_right"
                                : "message.cobblemon.hud_left")
                );
            }
            while (ModKeyMappings.TOGGLE_HUD.consumeClick()) {
                ClientPartySettings.toggleVisible();
                mc.player.sendSystemMessage(
                        Component.translatable(ClientPartySettings.hudVisible
                                ? "message.cobblemon.hud_on"
                                : "message.cobblemon.hud_off")
                );
            }
            while (ModKeyMappings.OPEN_POKEDEX.consumeClick()) {
                // Prefer colour of a held Pokédex item; default red
                String color = "red";
                var main = mc.player.getMainHandItem();
                var off = mc.player.getOffhandItem();
                String mainId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(main.getItem()).getPath();
                String offId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(off.getItem()).getPath();
                if (mainId.startsWith("pokedex_")) {
                    color = mainId.substring("pokedex_".length());
                } else if (offId.startsWith("pokedex_")) {
                    color = offId.substring("pokedex_".length());
                }
                ClientHooks.openPokedexScreen(color);
            }
            while (ModKeyMappings.OPEN_POKENAV.consumeClick()) {
                ClientHooks.openPokeNav();
            }
            while (ModKeyMappings.RIDE.consumeClick()) {
                ClientPacketDistributor.sendToServer(new RidePayload());
            }
        }
    }

    /** If crosshair is on a player, open interact menu (trade). Returns true if handled. */
    private static boolean tryOpenPlayerInteract(Minecraft mc) {
        if (mc.hitResult == null || mc.level == null || mc.player == null) {
            return false;
        }
        if (!(mc.hitResult instanceof net.minecraft.world.phys.EntityHitResult ehr)) {
            return false;
        }
        if (!(ehr.getEntity() instanceof net.minecraft.world.entity.player.Player other)) {
            return false;
        }
        if (other == mc.player) {
            return false;
        }
        ClientHooks.openPlayerInteract(other.getUUID(), other.getScoreboardName());
        return true;
    }

    @SubscribeEvent
    static void onRenderHud(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getWindow() == null) {
            return;
        }
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();
        PartyHudRenderer.renderWorldHud(event.getGuiGraphics(), w, h);
    }

    @SubscribeEvent
    static void onScreenRender(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> container)) {
            return;
        }
        // Survival + creative inventory
        if (!(container instanceof InventoryScreen) && !(container instanceof CreativeModeInventoryScreen)) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        PartyHudRenderer.renderInventoryParty(
                event.getGuiGraphics(),
                container,
                mc.player.getData(ModAttachments.PARTY),
                event.getMouseX(),
                event.getMouseY()
        );
    }

    @SubscribeEvent
    static void onScreenMouse(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> container)
                || (!(container instanceof InventoryScreen) && !(container instanceof CreativeModeInventoryScreen))) {
            InventoryPartyMenu.close();
            return;
        }
        if (InventoryPartyMenu.handleClick(container, event.getButton(), event.getMouseX(), event.getMouseY())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!event.getLevel().isClientSide()) {
            return;
        }
        if (event.getItemStack().is(ModItems.PARTY_BADGE.get())) {
            ClientParty.openScreen();
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }
}

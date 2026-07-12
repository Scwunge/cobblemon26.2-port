package com.cobblemon.mod.client.model;

import com.cobblemon.mod.Cobblemon;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = Cobblemon.MOD_ID, value = Dist.CLIENT)
public final class ModModelLayers {
    public static final ModelLayerLocation MON =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(Cobblemon.MOD_ID, "mon"), "main");
    public static final ModelLayerLocation SUBSHAH =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(Cobblemon.MOD_ID, "subshah"), "main");
    public static final ModelLayerLocation BILLBOARD =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(Cobblemon.MOD_ID, "mon_billboard"), "main");

    private ModModelLayers() {}

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(MON, CobblemonModel::createBodyLayer);
        event.registerLayerDefinition(SUBSHAH, SubshahModel::createBodyLayer);
        event.registerLayerDefinition(BILLBOARD, MonBillboardModel::createBodyLayer);
    }
}

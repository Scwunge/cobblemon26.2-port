package com.cobblemon.mod.client;

import com.mojang.blaze3d.vertex.PoseStack;

import com.cobblemon.mod.client.model.MonRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;

/**
 * Intentionally empty. A full-model translucent/emissive accent pass made mons
 * visible through solid blocks on the MC 26.1 submit pipeline. Coloring is done
 * via {@link WildMonRenderer#getModelTint} on the opaque cutout body instead.
 */
public class MonAccentLayer extends RenderLayer<MonRenderState, EntityModel<? super MonRenderState>> {
    public MonAccentLayer(RenderLayerParent<MonRenderState, EntityModel<? super MonRenderState>> parent) {
        super(parent);
    }

    @Override
    public void submit(
            PoseStack poseStack,
            SubmitNodeCollector collector,
            int packedLight,
            MonRenderState state,
            float yRot,
            float xRot
    ) {
        // no-op — do not draw translucent full-body overlays
    }
}

package com.cobblemon.mod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import com.cobblemon.mod.entity.CubeBallEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.ThrownItemRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Thrown ball renderer — Cobblemon 3D cube models ({@code *_ball_model}).
 * Item defs select GUI=flat icon, GROUND/hand=3D cube.
 */
public class CubeBallRenderer extends EntityRenderer<CubeBallEntity, CubeBallRenderer.BallState> {
    private final ItemModelResolver itemModelResolver;
    private final float scale;

    public CubeBallRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemModelResolver = context.getItemModelResolver();
        this.scale = 1.05f;
    }

    @Override
    public BallState createRenderState() {
        return new BallState();
    }

    @Override
    public void extractRenderState(CubeBallEntity entity, BallState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.capturing = entity.getPhase() == 1;
        state.age = entity.tickCount + partialTick;
        ItemStack stack = entity.getItem();
        if (stack == null || stack.isEmpty()) {
            stack = entity.getDefaultRenderStack();
        }
        // GROUND display → 3D cube model (items/*.json select fallback)
        this.itemModelResolver.updateForNonLiving(
                state.item,
                stack,
                ItemDisplayContext.GROUND,
                entity
        );
    }

    @Override
    public void submit(
            BallState state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera
    ) {
        poseStack.pushPose();
        poseStack.scale(this.scale, this.scale, this.scale);

        if (state.capturing) {
            // Grounded shake — keep upright-ish so the cube is readable
            float rock = Mth.sin(state.age * 0.22f) * 14.0f;
            float bob = Math.abs(Mth.sin(state.age * 0.22f)) * 0.05f;
            poseStack.translate(0.0, 0.12 + bob, 0.0);
            // Face camera lightly so the button is visible
            poseStack.mulPose(Axis.YP.rotationDegrees(camera.yRot));
            poseStack.mulPose(Axis.ZP.rotationDegrees(rock));
        } else {
            // In flight: tumble the cube
            poseStack.translate(0.0, 0.12, 0.0);
            poseStack.mulPose(Axis.YP.rotationDegrees(state.age * 28.0f));
            poseStack.mulPose(Axis.XP.rotationDegrees(state.age * 36.0f));
            poseStack.mulPose(Axis.ZP.rotationDegrees(state.age * 18.0f));
        }

        state.item.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }

    public static class BallState extends ThrownItemRenderState {
        public boolean capturing;
        public float age;
    }
}

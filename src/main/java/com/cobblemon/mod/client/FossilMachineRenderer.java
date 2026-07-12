package com.cobblemon.mod.client;

import com.mojang.blaze3d.vertex.PoseStack;

import com.cobblemon.mod.block.entity.FossilMachineBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Floating countdown / "Ready!" popup above the fossil analyzer / restoration tank.
 */
public class FossilMachineRenderer implements BlockEntityRenderer<FossilMachineBlockEntity, FossilMachineRenderer.State> {
    public FossilMachineRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(
            FossilMachineBlockEntity be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTicks, cameraPosition, breakProgress);
        state.show = false;
        state.label = null;
        if (be.hasResult()) {
            state.show = true;
            state.label = Component.literal("§a§lReady!");
        } else if (be.hasFossils() && be.canProcess()) {
            int remaining = be.remainingTicks();
            float rem = Math.max(0f, remaining - partialTicks);
            int totalSec = (int) Math.ceil(rem / 20.0);
            int min = totalSec / 60;
            int sec = totalSec % 60;
            state.show = true;
            state.label = Component.literal(String.format("§e§l%d:%02d", min, sec));
        } else if (be.hasFossils()) {
            state.show = true;
            if (be.isMultiBlock() && be.organic() < be.organicNeeded()) {
                state.label = Component.literal("§6Organic " + be.organic() + "/" + be.organicNeeded());
            } else {
                state.label = Component.literal("§6Need 2nd fossil");
            }
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.show || state.label == null) {
            return;
        }
        poseStack.pushPose();
        // Center above the block — nametag-style popup
        poseStack.translate(0.5, 1.35, 0.5);
        collector.submitNameTag(
                poseStack,
                null,
                0,
                state.label,
                true,
                state.lightCoords,
                camera
        );
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(FossilMachineBlockEntity blockEntity) {
        BlockPos pos = blockEntity.getBlockPos();
        return new AABB(
                pos.getX() - 0.5, pos.getY(), pos.getZ() - 0.5,
                pos.getX() + 1.5, pos.getY() + 2.5, pos.getZ() + 1.5
        );
    }

    @Override
    public int getViewDistance() {
        return 48;
    }

    public static class State extends BlockEntityRenderState {
        public boolean show;
        public @Nullable Component label;
    }
}

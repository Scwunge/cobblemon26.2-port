package com.cobblemon.mod.client.model;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.Identifier;

/**
 * Entity model backed by Cobblemon Bedrock geo + animation clips.
 */
public class CobblemonGeoModel extends EntityModel<MonRenderState> {
    private Identifier animId;

    public CobblemonGeoModel(ModelPart root) {
        super(root);
    }

    public void bindAnimation(Identifier animationResource) {
        this.animId = animationResource;
    }

    @Override
    public void setupAnim(MonRenderState state) {
        this.resetPose();
        if (animId == null) {
            return;
        }
        BedrockAnimationLoader.AnimationFile file = BedrockAnimationLoader.getOrLoad(animId);
        // Switch to walk only when clearly moving (hysteresis-friendly threshold).
        // Do NOT time-scale age by walk speed — Cobblemon clips already encode gait timing;
        // multiplying age made every mon look sped-up while moving.
        float walkAmt = Math.max(state.walkAnimationSpeed, state.limbSwingAmount);
        boolean walking = walkAmt > 0.12f;
        BedrockAnimationLoader.Clip clip = walking ? file.findWalk() : file.findIdle();
        if (clip == null) {
            clip = file.findIdle();
        }
        if (clip != null) {
            // Natural tick clock → seconds inside Clip.apply (age/20).
            // Walk uses the same real-time base so footfalls stay at authored speed.
            clip.apply(this.root, state.ageInTicks);
        }
    }
}

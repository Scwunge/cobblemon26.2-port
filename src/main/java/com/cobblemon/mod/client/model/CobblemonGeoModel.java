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
        float walkAmt = Math.max(state.walkAnimationSpeed, state.limbSwingAmount);
        boolean walking = walkAmt > 0.12f;

        BedrockAnimationLoader.Clip clip = null;
        // Hold-Space flight: full air_fly / air_idle (not sparse ride_air_* T-poses)
        if (state.isRidingFlight) {
            if (state.riderPitch > 30f) {
                clip = file.findAirDive();
            }
            if (clip == null && walking) {
                clip = file.findAirFly();
            }
            if (clip == null) {
                // Hovering in place while holding Space
                clip = file.findAirIdle();
            }
            if (clip == null) {
                clip = file.findAirFly();
            }
        } else if (state.isRidden && walking) {
            clip = file.findRideGround();
        }

        if (clip == null) {
            clip = walking ? file.findWalk() : file.findIdle();
        }
        if (clip == null) {
            clip = file.findIdle();
        }
        if (clip != null) {
            // Natural tick clock → seconds inside Clip.apply (age/20).
            clip.apply(this.root, state.ageInTicks);
        }
    }
}

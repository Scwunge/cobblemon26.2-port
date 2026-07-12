package com.cobblemon.mod.client.model;

import com.cobblemon.mod.species.BodyShape;
import com.cobblemon.mod.species.MonForm;
import com.cobblemon.mod.species.MonSpecies;
import net.minecraft.util.Mth;

/**
 * Shared animation state machine for creature poses (idle / walk / hover / battle-ready).
 * Models call {@link #apply} from setupAnim.
 */
public final class MonAnimController {
    public enum Pose {
        IDLE,
        WALK,
        HOVER,
        BATTLE
    }

    private MonAnimController() {}

    public static Pose detect(MonRenderState state) {
        if (state.walkAnimationSpeed > 0.15f) {
            return Pose.WALK;
        }
        BodyShape shape = state.bodyShape;
        if (shape == BodyShape.AVIAN || shape == BodyShape.AMORPH || shape == BodyShape.FEY
                || shape == BodyShape.AQUATIC) {
            return Pose.HOVER;
        }
        return Pose.IDLE;
    }

    /**
     * @param bobTarget model part y to bob (caller adds to base after resetPose)
     * @return bob delta for root/hip
     */
    public static float bob(MonRenderState state, Pose pose) {
        float t = state.ageInTicks;
        return switch (pose) {
            case HOVER -> Mth.sin(t * 0.12F) * 0.55F;
            case WALK -> Mth.sin(t * 0.25F) * 0.15F * state.walkAnimationSpeed;
            case BATTLE -> Mth.sin(t * 0.2F) * 0.2F;
            default -> Mth.sin(t * 0.08F) * 0.12F;
        };
    }

    public static float walkLimb(MonRenderState state, boolean left) {
        float swing = state.walkAnimationPos;
        float amount = state.walkAnimationSpeed;
        float phase = left ? 0f : (float) Math.PI;
        return Mth.cos(swing * 0.6662F + phase) * 1.1F * amount;
    }

    public static float formScale(MonSpecies species, MonForm form) {
        float s = species.sizeScale();
        if (form == MonForm.ALPHA) {
            s *= 1.2f;
        } else if (form == MonForm.SHINY) {
            s *= 1.02f;
        }
        return s;
    }

    public static void apply(MonRenderState state) {
        // State is filled by renderer; models read pose helpers above.
        state.animPose = detect(state);
    }
}

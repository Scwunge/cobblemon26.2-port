package com.cobblemon.mod.client.model;

import com.cobblemon.mod.species.BodyShape;
import com.cobblemon.mod.species.MonElement;
import com.cobblemon.mod.species.MonSpecies;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/**
 * Client render state for wild / party mon previews.
 */
public class MonRenderState extends LivingEntityRenderState {
    public MonSpecies species = MonSpecies.RATTATA;
    /** Datapack species id (may be beyond Gen1 enum). */
    public String speciesId = "rattata";
    public MonElement element = MonElement.FIRE;
    public BodyShape bodyShape = BodyShape.QUAD;
    public int stage = 1;
    public float modelScale = 1.0f;
    public MonAnimController.Pose animPose = MonAnimController.Pose.IDLE;
    public String formId = "normal";
    public String genderId = "genderless";
    /** Walk limb swing for leg animation. */
    public float limbSwing;
    public float limbSwingAmount;
}

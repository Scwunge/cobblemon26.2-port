package com.cobblemon.mod.client.model;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * Original cube recreation of the free Meshy "Subshah" look (reference only — not their mesh).
 * Target silhouette: psychic mantis-shrimp — wide hood, dual crest orbs, cyan eyes,
 * cream underbelly V, multi-joint raptorial claws, trailing tentacles with bulb tips.
 */
public class SubshahModel extends EntityModel<MonRenderState> {
    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart thorax;
    private final ModelPart abdomen;
    private final ModelPart head;
    private final ModelPart facePlate;
    private final ModelPart jaw;
    private final ModelPart hoodL;
    private final ModelPart hoodR;
    private final ModelPart hoodMid;
    private final ModelPart hornL;
    private final ModelPart hornR;
    private final ModelPart orbL;
    private final ModelPart orbR;
    private final ModelPart eyeL;
    private final ModelPart eyeR;
    private final ModelPart pupilL;
    private final ModelPart pupilR;
    private final ModelPart brow;
    // Left claw chain
    private final ModelPart shoulderL;
    private final ModelPart armL1;
    private final ModelPart armL2;
    private final ModelPart pincerL;
    private final ModelPart bladeL;
    // Right claw chain
    private final ModelPart shoulderR;
    private final ModelPart armR1;
    private final ModelPart armR2;
    private final ModelPart pincerR;
    private final ModelPart bladeR;
    // Mid/low arms
    private final ModelPart midArmL;
    private final ModelPart midArmR;
    private final ModelPart lowArmL;
    private final ModelPart lowArmR;
    // Tentacles
    private final ModelPart t1;
    private final ModelPart t1b;
    private final ModelPart t2;
    private final ModelPart t2b;
    private final ModelPart t3;
    private final ModelPart t3b;
    private final ModelPart t4;
    private final ModelPart t4b;
    private final ModelPart t5;
    private final ModelPart t5b;

    public SubshahModel(ModelPart root) {
        super(root);
        this.root = root.getChild("root");
        this.body = this.root.getChild("body");
        this.thorax = this.body.getChild("thorax");
        this.abdomen = this.body.getChild("abdomen");
        this.head = this.body.getChild("head");
        this.facePlate = this.head.getChild("face_plate");
        this.jaw = this.head.getChild("jaw");
        this.hoodL = this.head.getChild("hood_l");
        this.hoodR = this.head.getChild("hood_r");
        this.hoodMid = this.head.getChild("hood_mid");
        this.hornL = this.head.getChild("horn_l");
        this.hornR = this.head.getChild("horn_r");
        this.orbL = this.hornL.getChild("orb_l");
        this.orbR = this.hornR.getChild("orb_r");
        this.eyeL = this.head.getChild("eye_l");
        this.eyeR = this.head.getChild("eye_r");
        this.pupilL = this.eyeL.getChild("pupil_l");
        this.pupilR = this.eyeR.getChild("pupil_r");
        this.brow = this.head.getChild("brow");
        this.shoulderL = this.body.getChild("shoulder_l");
        this.armL1 = this.shoulderL.getChild("arm_l1");
        this.armL2 = this.armL1.getChild("arm_l2");
        this.pincerL = this.armL2.getChild("pincer_l");
        this.bladeL = this.pincerL.getChild("blade_l");
        this.shoulderR = this.body.getChild("shoulder_r");
        this.armR1 = this.shoulderR.getChild("arm_r1");
        this.armR2 = this.armR1.getChild("arm_r2");
        this.pincerR = this.armR2.getChild("pincer_r");
        this.bladeR = this.pincerR.getChild("blade_r");
        this.midArmL = this.body.getChild("mid_arm_l");
        this.midArmR = this.body.getChild("mid_arm_r");
        this.lowArmL = this.body.getChild("low_arm_l");
        this.lowArmR = this.body.getChild("low_arm_r");
        this.t1 = this.body.getChild("t1");
        this.t1b = this.t1.getChild("t1b");
        this.t2 = this.body.getChild("t2");
        this.t2b = this.t2.getChild("t2b");
        this.t3 = this.body.getChild("t3");
        this.t3b = this.t3.getChild("t3b");
        this.t4 = this.body.getChild("t4");
        this.t4b = this.t4.getChild("t4b");
        this.t5 = this.body.getChild("t5");
        this.t5b = this.t5.getChild("t5b");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition mr = mesh.getRoot();
        CubeDeformation soft = new CubeDeformation(0.45F);
        CubeDeformation mid = new CubeDeformation(0.25F);
        CubeDeformation sm = new CubeDeformation(0.12F);
        CubeDeformation none = CubeDeformation.NONE;

        // UV regions (match mon_subshah.png atlas):
        // 0,0 head  | 16,0 cream face | 32,0 eyes cyan | 48,0 gold orbs
        // 0,16 body | 16,16 belly    | 32,16 claws    | 48,16 arms
        // 0,32 hood | 32,32 horns    | 48,32 tents
        // 0,48 shell

        PartDefinition root = mr.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0, 24, 0));

        // Core body — slightly tapered floating thorax
        PartDefinition body = root.addOrReplaceChild(
                "body",
                CubeListBuilder.create().texOffs(0, 16)
                        .addBox(-2.8F, -7.5F, -2.4F, 5.6F, 6.5F, 5.0F, soft),
                PartPose.offset(0, -7.5F, 0)
        );
        body.addOrReplaceChild(
                "thorax",
                CubeListBuilder.create().texOffs(0, 48)
                        .addBox(-3.4F, -2.2F, -1.2F, 6.8F, 3.5F, 3.8F, mid),
                PartPose.offset(0, -5.8F, 1.4F)
        );
        // Cream underbelly V-plate (signature of the ref)
        body.addOrReplaceChild(
                "abdomen",
                CubeListBuilder.create().texOffs(16, 16)
                        .addBox(-2.6F, -1.0F, -1.5F, 5.2F, 5.5F, 3.0F, mid)
                        .texOffs(16, 16)
                        .addBox(-1.8F, 3.5F, -1.8F, 3.6F, 2.2F, 2.5F, sm),
                PartPose.offsetAndRotation(0, -2.0F, -2.2F, 0.2F, 0, 0)
        );

        // ===== HEAD =====
        PartDefinition head = body.addOrReplaceChild(
                "head",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-3.6F, -5.8F, -3.6F, 7.2F, 5.8F, 6.8F, soft),
                PartPose.offset(0, -8.0F, 0.2F)
        );
        // Cream/peach face plate
        head.addOrReplaceChild(
                "face_plate",
                CubeListBuilder.create().texOffs(16, 0)
                        .addBox(-2.8F, -3.2F, -1.2F, 5.6F, 4.0F, 2.0F, mid),
                PartPose.offset(0, -1.5F, -3.4F)
        );
        head.addOrReplaceChild(
                "jaw",
                CubeListBuilder.create().texOffs(16, 0)
                        .addBox(-2.0F, 0.0F, -1.8F, 4.0F, 1.6F, 2.6F, sm),
                PartPose.offset(0, -0.4F, -3.0F)
        );
        head.addOrReplaceChild(
                "brow",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-3.0F, -0.5F, -0.6F, 6.0F, 1.0F, 1.2F, sm),
                PartPose.offset(0, -4.2F, -3.2F)
        );

        // Wide winged hood (the big horizontal crest)
        head.addOrReplaceChild(
                "hood_mid",
                CubeListBuilder.create().texOffs(0, 32)
                        .addBox(-4.0F, -1.5F, -1.5F, 8.0F, 2.5F, 4.0F, mid),
                PartPose.offset(0, -5.5F, 0.8F)
        );
        head.addOrReplaceChild(
                "hood_l",
                CubeListBuilder.create().texOffs(0, 32)
                        .addBox(0.0F, -1.2F, -1.5F, 5.5F, 2.2F, 3.5F, mid),
                PartPose.offsetAndRotation(3.5F, -5.2F, 0.5F, 0.05F, 0.15F, 0.35F)
        );
        head.addOrReplaceChild(
                "hood_r",
                CubeListBuilder.create().texOffs(0, 32).mirror()
                        .addBox(-5.5F, -1.2F, -1.5F, 5.5F, 2.2F, 3.5F, mid),
                PartPose.offsetAndRotation(-3.5F, -5.2F, 0.5F, 0.05F, -0.15F, -0.35F)
        );

        // Dual crest horns + yellow tip orbs
        PartDefinition hornL = head.addOrReplaceChild(
                "horn_l",
                CubeListBuilder.create().texOffs(32, 32)
                        .addBox(-0.85F, -5.8F, -0.85F, 1.7F, 6.2F, 1.7F, mid),
                PartPose.offsetAndRotation(2.6F, -5.8F, 0.6F, 0.25F, 0.1F, 0.38F)
        );
        hornL.addOrReplaceChild(
                "orb_l",
                CubeListBuilder.create().texOffs(48, 0)
                        .addBox(-1.4F, -1.4F, -1.4F, 2.8F, 2.8F, 2.8F, soft),
                PartPose.offset(0, -6.4F, 0)
        );
        PartDefinition hornR = head.addOrReplaceChild(
                "horn_r",
                CubeListBuilder.create().texOffs(32, 32)
                        .addBox(-0.85F, -5.8F, -0.85F, 1.7F, 6.2F, 1.7F, mid),
                PartPose.offsetAndRotation(-2.6F, -5.8F, 0.6F, 0.25F, -0.1F, -0.38F)
        );
        hornR.addOrReplaceChild(
                "orb_r",
                CubeListBuilder.create().texOffs(48, 0)
                        .addBox(-1.4F, -1.4F, -1.4F, 2.8F, 2.8F, 2.8F, soft),
                PartPose.offset(0, -6.4F, 0)
        );

        // Bright cyan eyes (big, slightly protruding)
        PartDefinition eyeL = head.addOrReplaceChild(
                "eye_l",
                CubeListBuilder.create().texOffs(32, 0)
                        .addBox(-1.5F, -1.5F, -0.7F, 3.0F, 3.0F, 1.4F, none),
                PartPose.offset(1.95F, -3.3F, -3.65F)
        );
        eyeL.addOrReplaceChild(
                "pupil_l",
                CubeListBuilder.create().texOffs(40, 0)
                        .addBox(-0.75F, -0.75F, -0.45F, 1.5F, 1.5F, 0.7F, none),
                PartPose.offset(0.2F, 0.1F, -0.4F)
        );
        PartDefinition eyeR = head.addOrReplaceChild(
                "eye_r",
                CubeListBuilder.create().texOffs(32, 0)
                        .addBox(-1.5F, -1.5F, -0.7F, 3.0F, 3.0F, 1.4F, none),
                PartPose.offset(-1.95F, -3.3F, -3.65F)
        );
        eyeR.addOrReplaceChild(
                "pupil_r",
                CubeListBuilder.create().texOffs(40, 0)
                        .addBox(-0.75F, -0.75F, -0.45F, 1.5F, 1.5F, 0.7F, none),
                PartPose.offset(-0.2F, 0.1F, -0.4F)
        );

        // ===== RAPTORTIAL CLAWS (3 joints each, scythe tips) =====
        PartDefinition shL = body.addOrReplaceChild(
                "shoulder_l",
                CubeListBuilder.create().texOffs(48, 16)
                        .addBox(-1.2F, -1.5F, -1.5F, 3.0F, 3.0F, 3.0F, mid),
                PartPose.offsetAndRotation(3.0F, -5.5F, 0.2F, 0.1F, 0.35F, 0.4F)
        );
        PartDefinition aL1 = shL.addOrReplaceChild(
                "arm_l1",
                CubeListBuilder.create().texOffs(32, 16)
                        .addBox(0.0F, -1.4F, -1.6F, 5.2F, 2.8F, 3.2F, mid),
                PartPose.offsetAndRotation(1.5F, 0.2F, 0, 0.05F, 0.25F, 0.5F)
        );
        PartDefinition aL2 = aL1.addOrReplaceChild(
                "arm_l2",
                CubeListBuilder.create().texOffs(32, 16)
                        .addBox(0.0F, -1.2F, -1.3F, 4.8F, 2.4F, 2.6F, mid),
                PartPose.offsetAndRotation(4.8F, 0.15F, 0, 0, 0.1F, 0.7F)
        );
        PartDefinition pinL = aL2.addOrReplaceChild(
                "pincer_l",
                CubeListBuilder.create().texOffs(32, 16)
                        .addBox(0.0F, -1.0F, -1.0F, 3.8F, 2.0F, 2.0F, sm),
                PartPose.offsetAndRotation(4.4F, 0.1F, 0, 0, 0, 0.55F)
        );
        pinL.addOrReplaceChild(
                "blade_l",
                CubeListBuilder.create().texOffs(32, 16)
                        .addBox(0.0F, -0.7F, -0.55F, 3.5F, 1.4F, 1.1F, none),
                PartPose.offsetAndRotation(3.5F, 0, 0, 0, 0, 0.4F)
        );

        PartDefinition shR = body.addOrReplaceChild(
                "shoulder_r",
                CubeListBuilder.create().texOffs(48, 16).mirror()
                        .addBox(-1.8F, -1.5F, -1.5F, 3.0F, 3.0F, 3.0F, mid),
                PartPose.offsetAndRotation(-3.0F, -5.5F, 0.2F, 0.1F, -0.35F, -0.4F)
        );
        PartDefinition aR1 = shR.addOrReplaceChild(
                "arm_r1",
                CubeListBuilder.create().texOffs(32, 16).mirror()
                        .addBox(-5.2F, -1.4F, -1.6F, 5.2F, 2.8F, 3.2F, mid),
                PartPose.offsetAndRotation(-1.5F, 0.2F, 0, 0.05F, -0.25F, -0.5F)
        );
        PartDefinition aR2 = aR1.addOrReplaceChild(
                "arm_r2",
                CubeListBuilder.create().texOffs(32, 16).mirror()
                        .addBox(-4.8F, -1.2F, -1.3F, 4.8F, 2.4F, 2.6F, mid),
                PartPose.offsetAndRotation(-4.8F, 0.15F, 0, 0, -0.1F, -0.7F)
        );
        PartDefinition pinR = aR2.addOrReplaceChild(
                "pincer_r",
                CubeListBuilder.create().texOffs(32, 16).mirror()
                        .addBox(-3.8F, -1.0F, -1.0F, 3.8F, 2.0F, 2.0F, sm),
                PartPose.offsetAndRotation(-4.4F, 0.1F, 0, 0, 0, -0.55F)
        );
        pinR.addOrReplaceChild(
                "blade_r",
                CubeListBuilder.create().texOffs(32, 16).mirror()
                        .addBox(-3.5F, -0.7F, -0.55F, 3.5F, 1.4F, 1.1F, none),
                PartPose.offsetAndRotation(-3.5F, 0, 0, 0, 0, -0.4F)
        );

        // Secondary limb pairs
        body.addOrReplaceChild(
                "mid_arm_l",
                CubeListBuilder.create().texOffs(48, 16)
                        .addBox(0, -0.9F, -0.9F, 4.8F, 1.8F, 1.8F, sm),
                PartPose.offsetAndRotation(2.6F, -2.8F, 0.6F, 0.25F, 0.3F, 0.85F)
        );
        body.addOrReplaceChild(
                "mid_arm_r",
                CubeListBuilder.create().texOffs(48, 16).mirror()
                        .addBox(-4.8F, -0.9F, -0.9F, 4.8F, 1.8F, 1.8F, sm),
                PartPose.offsetAndRotation(-2.6F, -2.8F, 0.6F, 0.25F, -0.3F, -0.85F)
        );
        body.addOrReplaceChild(
                "low_arm_l",
                CubeListBuilder.create().texOffs(48, 16)
                        .addBox(0, -0.7F, -0.7F, 3.6F, 1.4F, 1.4F, sm),
                PartPose.offsetAndRotation(2.0F, -0.8F, 0.3F, 0.45F, 0.2F, 1.0F)
        );
        body.addOrReplaceChild(
                "low_arm_r",
                CubeListBuilder.create().texOffs(48, 16).mirror()
                        .addBox(-3.6F, -0.7F, -0.7F, 3.6F, 1.4F, 1.4F, sm),
                PartPose.offsetAndRotation(-2.0F, -0.8F, 0.3F, 0.45F, -0.2F, -1.0F)
        );

        // ===== TENTACLES (segment + bulb tip) =====
        addTentacle(body, "t1", "t1b", 1.6F, -0.2F, 2.0F, 0.7F, 0.3F, 0.15F, 6.5F);
        addTentacle(body, "t2", "t2b", -1.6F, -0.2F, 2.0F, 0.75F, -0.3F, -0.15F, 7.0F);
        addTentacle(body, "t3", "t3b", 2.5F, -1.0F, 1.0F, 0.5F, 0.55F, 0.5F, 5.5F);
        addTentacle(body, "t4", "t4b", -2.5F, -1.0F, 1.0F, 0.5F, -0.55F, -0.5F, 5.5F);
        addTentacle(body, "t5", "t5b", 0.0F, 0.2F, 2.3F, 0.95F, 0.0F, 0.0F, 5.0F);

        return LayerDefinition.create(mesh, 64, 64);
    }

    private static void addTentacle(
            PartDefinition body, String name, String tipName,
            float ox, float oy, float oz,
            float xRot, float yRot, float zRot, float length
    ) {
        CubeDeformation mid = new CubeDeformation(0.2F);
        CubeDeformation soft = new CubeDeformation(0.35F);
        PartDefinition t = body.addOrReplaceChild(
                name,
                CubeListBuilder.create().texOffs(48, 32)
                        .addBox(-0.65F, 0, -0.65F, 1.3F, length, 1.3F, mid),
                PartPose.offsetAndRotation(ox, oy, oz, xRot, yRot, zRot)
        );
        t.addOrReplaceChild(
                tipName,
                CubeListBuilder.create().texOffs(48, 32)
                        .addBox(-1.05F, 0, -1.05F, 2.1F, 2.1F, 2.1F, soft),
                PartPose.offset(0, length - 0.2F, 0)
        );
    }

    @Override
    public void setupAnim(MonRenderState state) {
        float t = state.ageInTicks;
        float w = state.walkAnimationSpeed;

        // IMPORTANT: resetPose() restores PartPose, then we ADD animation.
        // Never assign y = bob alone — that wiped the root offset (y=24) and hid the model underground.
        this.root.yRot = state.yRot * ((float) Math.PI / 180F) * 0.3F;
        this.root.y += Mth.sin(t * 0.1F) * 0.85F;

        this.head.yRot = state.yRot * ((float) Math.PI / 180F) * 0.28F;
        this.head.xRot = state.xRot * ((float) Math.PI / 180F) * 0.22F;
        this.body.xRot = 0.08F + Mth.sin(t * 0.07F) * 0.04F;

        // Hood slight flap (additive on base zRot from PartPose)
        this.hoodL.zRot += Mth.sin(t * 0.12F) * 0.04F;
        this.hoodR.zRot += Mth.sin(t * 0.12F) * -0.04F;

        // Crest orbs pulse
        float p = 1f + Mth.sin(t * 0.3F) * 0.1F;
        this.orbL.xScale = this.orbL.yScale = this.orbL.zScale = p;
        this.orbR.xScale = this.orbR.yScale = this.orbR.zScale = p;

        // Claw animation — open/close menace (additive on posed joints)
        float claw = Mth.sin(t * 0.13F);
        this.shoulderL.zRot += claw * 0.08F + w * 0.15F;
        this.shoulderR.zRot += -claw * 0.08F - w * 0.15F;
        this.armL1.zRot += claw * 0.1F;
        this.armR1.zRot += -claw * 0.1F;
        this.armL2.zRot += Mth.sin(t * 0.17F) * 0.12F;
        this.armR2.zRot += -Mth.sin(t * 0.17F) * 0.12F;
        this.pincerL.zRot += Mth.sin(t * 0.2F) * 0.1F;
        this.pincerR.zRot += -Mth.sin(t * 0.2F) * 0.1F;
        this.bladeL.zRot += Mth.sin(t * 0.25F) * 0.08F;
        this.bladeR.zRot += -Mth.sin(t * 0.25F) * 0.08F;

        // Secondary arms breathe
        this.midArmL.zRot += Mth.sin(t * 0.11F) * 0.06F;
        this.midArmR.zRot += -Mth.sin(t * 0.11F) * 0.06F;

        // Tentacle sway
        this.t1.xRot += Mth.sin(t * 0.16F) * 0.2F;
        this.t2.xRot += Mth.sin(t * 0.16F + 1.2F) * 0.2F;
        this.t3.xRot += Mth.sin(t * 0.14F + 0.5F) * 0.16F;
        this.t4.xRot += Mth.sin(t * 0.14F + 1.9F) * 0.16F;
        this.t5.xRot += Mth.sin(t * 0.12F + 0.9F) * 0.12F;
        this.t1b.xRot += Mth.sin(t * 0.22F) * 0.15F;
        this.t2b.xRot += Mth.sin(t * 0.22F + 1F) * 0.15F;
        this.t3b.xRot += Mth.sin(t * 0.2F + 0.4F) * 0.12F;
        this.t4b.xRot += Mth.sin(t * 0.2F + 1.6F) * 0.12F;

        // Jaw
        this.jaw.xRot += Math.max(0f, Mth.sin(t * 0.18F)) * 0.14F;

        // Eye look (pupils start near 0 offset from pose)
        float look = Mth.clamp(state.yRot / 45f, -0.4f, 0.4f);
        this.pupilL.x += look * 0.35F;
        this.pupilR.x += look * 0.35F;
    }
}

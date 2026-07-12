package com.cobblemon.mod.client.model;

import com.cobblemon.mod.species.BodyShape;
import com.cobblemon.mod.species.MonElement;
import com.cobblemon.mod.species.MonSpecies;
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
 * High-detail shared Cobblemon rig. Many small volumes + per-species toggles
 * so silhouettes read as creatures, not single crates.
 */
public class CobblemonModel extends EntityModel<MonRenderState> {
    private final ModelPart root;
    private final ModelPart hip;
    private final ModelPart body;
    private final ModelPart chest;
    private final ModelPart belly;
    private final ModelPart collar;
    private final ModelPart head;
    private final ModelPart snout;
    private final ModelPart jaw;
    private final ModelPart brow;
    private final ModelPart eyeL;
    private final ModelPart eyeR;
    private final ModelPart pupilL;
    private final ModelPart pupilR;
    private final ModelPart cheekL;
    private final ModelPart cheekR;
    private final ModelPart earLeft;
    private final ModelPart earRight;
    private final ModelPart earInnerL;
    private final ModelPart earInnerR;
    private final ModelPart crest;
    private final ModelPart crest2;
    private final ModelPart crest3;
    private final ModelPart leaf;
    private final ModelPart hornLeft;
    private final ModelPart hornRight;
    private final ModelPart maneL;
    private final ModelPart maneR;
    private final ModelPart maneBack;
    private final ModelPart leftArm;
    private final ModelPart leftHand;
    private final ModelPart rightArm;
    private final ModelPart rightHand;
    private final ModelPart leftLeg;
    private final ModelPart leftFoot;
    private final ModelPart rightLeg;
    private final ModelPart rightFoot;
    private final ModelPart tail;
    private final ModelPart tailMid;
    private final ModelPart tailTip;
    private final ModelPart wingLeft;
    private final ModelPart wingLeftInner;
    private final ModelPart wingRight;
    private final ModelPart wingRightInner;
    private final ModelPart fin;
    private final ModelPart finSideL;
    private final ModelPart finSideR;
    private final ModelPart shell;
    private final ModelPart shellRidge;
    private final ModelPart cape;
    private final ModelPart chestStar;
    private final ModelPart eyeRing;
    private final ModelPart antennaL;
    private final ModelPart antennaR;
    private final ModelPart orb;

    public CobblemonModel(ModelPart root) {
        super(root);
        this.root = root;
        this.hip = root.getChild("hip");
        this.body = this.hip.getChild("body");
        this.chest = this.body.getChild("chest");
        this.belly = this.body.getChild("belly");
        this.collar = this.body.getChild("collar");
        this.head = this.body.getChild("head");
        this.snout = this.head.getChild("snout");
        this.jaw = this.head.getChild("jaw");
        this.brow = this.head.getChild("brow");
        this.eyeL = this.head.getChild("eye_l");
        this.eyeR = this.head.getChild("eye_r");
        this.pupilL = this.eyeL.getChild("pupil_l");
        this.pupilR = this.eyeR.getChild("pupil_r");
        this.cheekL = this.head.getChild("cheek_l");
        this.cheekR = this.head.getChild("cheek_r");
        this.earLeft = this.head.getChild("ear_left");
        this.earRight = this.head.getChild("ear_right");
        this.earInnerL = this.earLeft.getChild("ear_inner_l");
        this.earInnerR = this.earRight.getChild("ear_inner_r");
        this.crest = this.head.getChild("crest");
        this.crest2 = this.head.getChild("crest2");
        this.crest3 = this.head.getChild("crest3");
        this.leaf = this.head.getChild("leaf");
        this.hornLeft = this.head.getChild("horn_left");
        this.hornRight = this.head.getChild("horn_right");
        this.maneL = this.head.getChild("mane_l");
        this.maneR = this.head.getChild("mane_r");
        this.maneBack = this.head.getChild("mane_back");
        this.eyeRing = this.head.getChild("eye_ring");
        this.antennaL = this.head.getChild("antenna_l");
        this.antennaR = this.head.getChild("antenna_r");
        this.leftArm = this.body.getChild("left_arm");
        this.leftHand = this.leftArm.getChild("left_hand");
        this.rightArm = this.body.getChild("right_arm");
        this.rightHand = this.rightArm.getChild("right_hand");
        this.leftLeg = this.hip.getChild("left_leg");
        this.leftFoot = this.leftLeg.getChild("left_foot");
        this.rightLeg = this.hip.getChild("right_leg");
        this.rightFoot = this.rightLeg.getChild("right_foot");
        this.tail = this.hip.getChild("tail");
        this.tailMid = this.tail.getChild("tail_mid");
        this.tailTip = this.tailMid.getChild("tail_tip");
        this.wingLeft = this.body.getChild("wing_left");
        this.wingLeftInner = this.wingLeft.getChild("wing_left_inner");
        this.wingRight = this.body.getChild("wing_right");
        this.wingRightInner = this.wingRight.getChild("wing_right_inner");
        this.fin = this.body.getChild("fin");
        this.finSideL = this.body.getChild("fin_side_l");
        this.finSideR = this.body.getChild("fin_side_r");
        this.shell = this.body.getChild("shell");
        this.shellRidge = this.shell.getChild("shell_ridge");
        this.cape = this.body.getChild("cape");
        this.chestStar = this.chest.getChild("chest_star");
        this.orb = this.body.getChild("orb");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        CubeDeformation soft = new CubeDeformation(0.4F);
        CubeDeformation mid = new CubeDeformation(0.25F);
        CubeDeformation sm = new CubeDeformation(0.12F);
        CubeDeformation none = CubeDeformation.NONE;

        // Hip anchors whole body (bob from here)
        PartDefinition hip = root.addOrReplaceChild(
                "hip",
                CubeListBuilder.create().texOffs(0, 40)
                        .addBox(-2.5F, -1.5F, -2.0F, 5.0F, 3.0F, 4.0F, mid),
                PartPose.offset(0.0F, 18.5F, 0.0F)
        );

        PartDefinition body = hip.addOrReplaceChild(
                "body",
                CubeListBuilder.create().texOffs(0, 16)
                        .addBox(-3.2F, -5.5F, -2.6F, 6.4F, 6.0F, 5.2F, soft),
                PartPose.offset(0.0F, -1.0F, 0.0F)
        );

        PartDefinition chest = body.addOrReplaceChild(
                "chest",
                CubeListBuilder.create().texOffs(28, 16)
                        .addBox(-2.8F, -2.0F, -1.5F, 5.6F, 3.2F, 3.0F, mid),
                PartPose.offset(0.0F, -3.8F, 0.2F)
        );
        chest.addOrReplaceChild(
                "chest_star",
                CubeListBuilder.create().texOffs(56, 40)
                        .addBox(-1.0F, -1.0F, -0.5F, 2.0F, 2.0F, 1.0F, sm),
                PartPose.offset(0.0F, 0.2F, -1.6F)
        );

        body.addOrReplaceChild(
                "belly",
                CubeListBuilder.create().texOffs(28, 24)
                        .addBox(-2.4F, -1.8F, -1.0F, 4.8F, 3.6F, 2.0F, mid),
                PartPose.offset(0.0F, -0.8F, -2.4F)
        );
        body.addOrReplaceChild(
                "collar",
                CubeListBuilder.create().texOffs(40, 40)
                        .addBox(-2.6F, -0.6F, -2.4F, 5.2F, 1.2F, 4.8F, sm),
                PartPose.offset(0.0F, -5.2F, 0.0F)
        );

        // Head cluster
        PartDefinition head = body.addOrReplaceChild(
                "head",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-3.4F, -6.6F, -3.4F, 6.8F, 6.8F, 6.8F, soft),
                PartPose.offset(0.0F, -5.8F, 0.0F)
        );
        head.addOrReplaceChild(
                "snout",
                CubeListBuilder.create().texOffs(24, 0)
                        .addBox(-1.6F, -1.8F, -2.4F, 3.2F, 2.4F, 2.6F, mid),
                PartPose.offset(0.0F, -1.6F, -3.4F)
        );
        head.addOrReplaceChild(
                "jaw",
                CubeListBuilder.create().texOffs(36, 0)
                        .addBox(-1.4F, 0.0F, -2.0F, 2.8F, 1.2F, 2.2F, sm),
                PartPose.offset(0.0F, -0.6F, -3.0F)
        );
        head.addOrReplaceChild(
                "brow",
                CubeListBuilder.create().texOffs(48, 0)
                        .addBox(-2.8F, -0.4F, -0.5F, 5.6F, 0.9F, 1.0F, sm),
                PartPose.offset(0.0F, -4.4F, -3.0F)
        );

        PartDefinition eyeL = head.addOrReplaceChild(
                "eye_l",
                CubeListBuilder.create().texOffs(56, 0)
                        .addBox(-1.1F, -1.1F, -0.4F, 2.2F, 2.2F, 0.8F, none),
                PartPose.offset(1.6F, -3.2F, -3.35F)
        );
        eyeL.addOrReplaceChild(
                "pupil_l",
                CubeListBuilder.create().texOffs(60, 4)
                        .addBox(-0.55F, -0.55F, -0.35F, 1.1F, 1.1F, 0.5F, none),
                PartPose.offset(0.15F, 0.1F, -0.2F)
        );
        PartDefinition eyeR = head.addOrReplaceChild(
                "eye_r",
                CubeListBuilder.create().texOffs(56, 0)
                        .addBox(-1.1F, -1.1F, -0.4F, 2.2F, 2.2F, 0.8F, none),
                PartPose.offset(-1.6F, -3.2F, -3.35F)
        );
        eyeR.addOrReplaceChild(
                "pupil_r",
                CubeListBuilder.create().texOffs(60, 4)
                        .addBox(-0.55F, -0.55F, -0.35F, 1.1F, 1.1F, 0.5F, none),
                PartPose.offset(-0.15F, 0.1F, -0.2F)
        );

        head.addOrReplaceChild(
                "cheek_l",
                CubeListBuilder.create().texOffs(48, 4)
                        .addBox(-0.9F, -0.9F, -0.9F, 1.8F, 1.8F, 1.8F, mid),
                PartPose.offset(3.0F, -2.0F, -2.2F)
        );
        head.addOrReplaceChild(
                "cheek_r",
                CubeListBuilder.create().texOffs(48, 4)
                        .addBox(-0.9F, -0.9F, -0.9F, 1.8F, 1.8F, 1.8F, mid),
                PartPose.offset(-3.0F, -2.0F, -2.2F)
        );

        PartDefinition earL = head.addOrReplaceChild(
                "ear_left",
                CubeListBuilder.create().texOffs(34, 8)
                        .addBox(-1.1F, -3.6F, -1.0F, 2.2F, 4.0F, 2.0F, mid),
                PartPose.offsetAndRotation(2.6F, -5.8F, 0.2F, 0.1F, 0.15F, 0.35F)
        );
        earL.addOrReplaceChild(
                "ear_inner_l",
                CubeListBuilder.create().texOffs(42, 8)
                        .addBox(-0.6F, -2.6F, -0.3F, 1.2F, 2.8F, 0.6F, sm),
                PartPose.offset(0.0F, -0.2F, -0.55F)
        );
        PartDefinition earR = head.addOrReplaceChild(
                "ear_right",
                CubeListBuilder.create().texOffs(34, 8).mirror()
                        .addBox(-1.1F, -3.6F, -1.0F, 2.2F, 4.0F, 2.0F, mid),
                PartPose.offsetAndRotation(-2.6F, -5.8F, 0.2F, 0.1F, -0.15F, -0.35F)
        );
        earR.addOrReplaceChild(
                "ear_inner_r",
                CubeListBuilder.create().texOffs(42, 8).mirror()
                        .addBox(-0.6F, -2.6F, -0.3F, 1.2F, 2.8F, 0.6F, sm),
                PartPose.offset(0.0F, -0.2F, -0.55F)
        );

        head.addOrReplaceChild(
                "crest",
                CubeListBuilder.create().texOffs(0, 48)
                        .addBox(-1.2F, -4.2F, -1.2F, 2.4F, 4.5F, 2.4F, mid),
                PartPose.offset(0.0F, -6.0F, 0.6F)
        );
        head.addOrReplaceChild(
                "crest2",
                CubeListBuilder.create().texOffs(10, 48)
                        .addBox(-0.9F, -3.4F, -0.9F, 1.8F, 3.6F, 1.8F, sm),
                PartPose.offsetAndRotation(1.4F, -5.6F, 0.2F, 0.0F, 0.0F, 0.4F)
        );
        head.addOrReplaceChild(
                "crest3",
                CubeListBuilder.create().texOffs(10, 48)
                        .addBox(-0.9F, -3.4F, -0.9F, 1.8F, 3.6F, 1.8F, sm),
                PartPose.offsetAndRotation(-1.4F, -5.6F, 0.2F, 0.0F, 0.0F, -0.4F)
        );
        head.addOrReplaceChild(
                "leaf",
                CubeListBuilder.create().texOffs(20, 48)
                        .addBox(-2.4F, -0.5F, 0.0F, 4.8F, 1.0F, 5.0F, mid),
                PartPose.offsetAndRotation(0.0F, -6.4F, 0.4F, -0.7F, 0.0F, 0.0F)
        );
        head.addOrReplaceChild(
                "horn_left",
                CubeListBuilder.create().texOffs(48, 12)
                        .addBox(-0.6F, -3.4F, -0.6F, 1.2F, 3.6F, 1.2F, none),
                PartPose.offsetAndRotation(1.9F, -6.0F, 0.0F, 0.2F, 0.0F, 0.35F)
        );
        head.addOrReplaceChild(
                "horn_right",
                CubeListBuilder.create().texOffs(48, 12).mirror()
                        .addBox(-0.6F, -3.4F, -0.6F, 1.2F, 3.6F, 1.2F, none),
                PartPose.offsetAndRotation(-1.9F, -6.0F, 0.0F, 0.2F, 0.0F, -0.35F)
        );

        // Data mane (Grokmon / electric / fire finals)
        head.addOrReplaceChild(
                "mane_l",
                CubeListBuilder.create().texOffs(48, 28)
                        .addBox(0.0F, -3.0F, -1.5F, 1.4F, 6.0F, 3.0F, mid),
                PartPose.offsetAndRotation(3.2F, -3.0F, 0.0F, 0.0F, 0.0F, -0.15F)
        );
        head.addOrReplaceChild(
                "mane_r",
                CubeListBuilder.create().texOffs(48, 28).mirror()
                        .addBox(-1.4F, -3.0F, -1.5F, 1.4F, 6.0F, 3.0F, mid),
                PartPose.offsetAndRotation(-3.2F, -3.0F, 0.0F, 0.0F, 0.0F, 0.15F)
        );
        head.addOrReplaceChild(
                "mane_back",
                CubeListBuilder.create().texOffs(0, 56)
                        .addBox(-2.5F, -2.0F, 0.0F, 5.0F, 5.0F, 2.5F, mid),
                PartPose.offset(0.0F, -3.5F, 2.8F)
        );

        // Grok eye-ring (geometry, not just paint)
        head.addOrReplaceChild(
                "eye_ring",
                CubeListBuilder.create().texOffs(40, 48)
                        .addBox(-2.0F, -2.0F, -0.35F, 4.0F, 4.0F, 0.7F, new CubeDeformation(0.05F)),
                PartPose.offset(0.0F, -3.1F, -3.55F)
        );
        head.addOrReplaceChild(
                "antenna_l",
                CubeListBuilder.create().texOffs(52, 48)
                        .addBox(-0.35F, -4.0F, -0.35F, 0.7F, 4.2F, 0.7F, none)
                        .texOffs(56, 48).addBox(-0.7F, -4.6F, -0.7F, 1.4F, 1.4F, 1.4F, sm),
                PartPose.offsetAndRotation(2.0F, -6.0F, 0.5F, 0.15F, 0.0F, 0.2F)
        );
        head.addOrReplaceChild(
                "antenna_r",
                CubeListBuilder.create().texOffs(52, 48)
                        .addBox(-0.35F, -4.0F, -0.35F, 0.7F, 4.2F, 0.7F, none)
                        .texOffs(56, 48).addBox(-0.7F, -4.6F, -0.7F, 1.4F, 1.4F, 1.4F, sm),
                PartPose.offsetAndRotation(-2.0F, -6.0F, 0.5F, 0.15F, 0.0F, -0.2F)
        );

        // Arms
        PartDefinition leftArm = body.addOrReplaceChild(
                "left_arm",
                CubeListBuilder.create().texOffs(36, 16)
                        .addBox(-1.0F, -0.5F, -1.0F, 2.0F, 4.2F, 2.0F, mid),
                PartPose.offset(4.0F, -4.0F, 0.0F)
        );
        leftArm.addOrReplaceChild(
                "left_hand",
                CubeListBuilder.create().texOffs(36, 24)
                        .addBox(-1.15F, 0.0F, -1.15F, 2.3F, 1.6F, 2.3F, sm),
                PartPose.offset(0.0F, 3.6F, 0.0F)
        );
        PartDefinition rightArm = body.addOrReplaceChild(
                "right_arm",
                CubeListBuilder.create().texOffs(36, 16).mirror()
                        .addBox(-1.0F, -0.5F, -1.0F, 2.0F, 4.2F, 2.0F, mid),
                PartPose.offset(-4.0F, -4.0F, 0.0F)
        );
        rightArm.addOrReplaceChild(
                "right_hand",
                CubeListBuilder.create().texOffs(36, 24).mirror()
                        .addBox(-1.15F, 0.0F, -1.15F, 2.3F, 1.6F, 2.3F, sm),
                PartPose.offset(0.0F, 3.6F, 0.0F)
        );

        // Legs + feet
        PartDefinition leftLeg = hip.addOrReplaceChild(
                "left_leg",
                CubeListBuilder.create().texOffs(24, 16)
                        .addBox(-1.4F, 0.0F, -1.4F, 2.8F, 4.0F, 2.8F, mid),
                PartPose.offset(1.8F, 1.0F, 0.0F)
        );
        leftLeg.addOrReplaceChild(
                "left_foot",
                CubeListBuilder.create().texOffs(24, 24)
                        .addBox(-1.5F, 0.0F, -2.2F, 3.0F, 1.3F, 3.4F, sm),
                PartPose.offset(0.0F, 3.7F, 0.2F)
        );
        PartDefinition rightLeg = hip.addOrReplaceChild(
                "right_leg",
                CubeListBuilder.create().texOffs(24, 16).mirror()
                        .addBox(-1.4F, 0.0F, -1.4F, 2.8F, 4.0F, 2.8F, mid),
                PartPose.offset(-1.8F, 1.0F, 0.0F)
        );
        rightLeg.addOrReplaceChild(
                "right_foot",
                CubeListBuilder.create().texOffs(24, 24).mirror()
                        .addBox(-1.5F, 0.0F, -2.2F, 3.0F, 1.3F, 3.4F, sm),
                PartPose.offset(0.0F, 3.7F, 0.2F)
        );

        // Multi-segment tail
        PartDefinition tail = hip.addOrReplaceChild(
                "tail",
                CubeListBuilder.create().texOffs(0, 28)
                        .addBox(-1.1F, -1.1F, 0.0F, 2.2F, 2.2F, 3.5F, mid),
                PartPose.offset(0.0F, -0.5F, 2.2F)
        );
        PartDefinition tailMid = tail.addOrReplaceChild(
                "tail_mid",
                CubeListBuilder.create().texOffs(12, 28)
                        .addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 3.2F, sm),
                PartPose.offset(0.0F, 0.0F, 3.2F)
        );
        tailMid.addOrReplaceChild(
                "tail_tip",
                CubeListBuilder.create().texOffs(14, 34)
                        .addBox(-1.4F, -1.4F, 0.0F, 2.8F, 2.8F, 2.6F, soft),
                PartPose.offset(0.0F, 0.0F, 2.9F)
        );

        // Layered wings
        PartDefinition wingL = body.addOrReplaceChild(
                "wing_left",
                CubeListBuilder.create().texOffs(24, 28)
                        .addBox(0.0F, -3.5F, 0.0F, 0.9F, 6.0F, 7.0F, mid),
                PartPose.offsetAndRotation(3.3F, -3.5F, 0.4F, 0.15F, 0.45F, 0.3F)
        );
        wingL.addOrReplaceChild(
                "wing_left_inner",
                CubeListBuilder.create().texOffs(40, 52)
                        .addBox(0.1F, -2.0F, 0.5F, 0.5F, 4.0F, 4.5F, sm),
                PartPose.offset(0.0F, 0.0F, 0.0F)
        );
        PartDefinition wingR = body.addOrReplaceChild(
                "wing_right",
                CubeListBuilder.create().texOffs(24, 28).mirror()
                        .addBox(-0.9F, -3.5F, 0.0F, 0.9F, 6.0F, 7.0F, mid),
                PartPose.offsetAndRotation(-3.3F, -3.5F, 0.4F, 0.15F, -0.45F, -0.3F)
        );
        wingR.addOrReplaceChild(
                "wing_right_inner",
                CubeListBuilder.create().texOffs(40, 52).mirror()
                        .addBox(-0.6F, -2.0F, 0.5F, 0.5F, 4.0F, 4.5F, sm),
                PartPose.offset(0.0F, 0.0F, 0.0F)
        );

        body.addOrReplaceChild(
                "fin",
                CubeListBuilder.create().texOffs(42, 16)
                        .addBox(-0.45F, -4.2F, -1.0F, 0.9F, 4.5F, 3.6F, mid),
                PartPose.offset(0.0F, -5.0F, 1.4F)
        );
        body.addOrReplaceChild(
                "fin_side_l",
                CubeListBuilder.create().texOffs(50, 16)
                        .addBox(0.0F, -1.5F, -1.0F, 0.6F, 3.0F, 3.0F, sm),
                PartPose.offsetAndRotation(3.0F, -1.5F, 0.0F, 0.0F, 0.4F, 0.0F)
        );
        body.addOrReplaceChild(
                "fin_side_r",
                CubeListBuilder.create().texOffs(50, 16).mirror()
                        .addBox(-0.6F, -1.5F, -1.0F, 0.6F, 3.0F, 3.0F, sm),
                PartPose.offsetAndRotation(-3.0F, -1.5F, 0.0F, 0.0F, -0.4F, 0.0F)
        );

        PartDefinition shell = body.addOrReplaceChild(
                "shell",
                CubeListBuilder.create().texOffs(40, 28)
                        .addBox(-3.8F, -3.5F, -0.5F, 7.6F, 5.0F, 3.6F, new CubeDeformation(0.35F)),
                PartPose.offset(0.0F, -3.0F, 1.6F)
        );
        shell.addOrReplaceChild(
                "shell_ridge",
                CubeListBuilder.create().texOffs(48, 56)
                        .addBox(-1.0F, -2.0F, -0.5F, 2.0F, 4.0F, 2.0F, mid),
                PartPose.offset(0.0F, -3.2F, 1.0F)
        );

        body.addOrReplaceChild(
                "cape",
                CubeListBuilder.create().texOffs(16, 56)
                        .addBox(-3.0F, 0.0F, 0.0F, 6.0F, 7.0F, 1.2F, mid),
                PartPose.offsetAndRotation(0.0F, -4.5F, 2.6F, 0.25F, 0.0F, 0.0F)
        );

        // Floating focus orb (psychic / grok)
        body.addOrReplaceChild(
                "orb",
                CubeListBuilder.create().texOffs(56, 56)
                        .addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F, soft),
                PartPose.offset(0.0F, -7.5F, 0.0F)
        );

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(MonRenderState state) {
        // Reset
        this.hip.yRot = 0;
        this.hip.xRot = 0;
        this.body.xRot = 0;
        this.head.xRot = state.xRot * ((float) Math.PI / 180F) * 0.55F;
        this.head.yRot = state.yRot * ((float) Math.PI / 180F) * 0.55F;
        this.jaw.xRot = 0;
        this.tail.xRot = 0.25F;
        this.tail.yRot = 0;
        this.tailMid.yRot = 0;
        this.tailTip.yRot = 0;
        this.leftLeg.xRot = 0;
        this.rightLeg.xRot = 0;
        this.leftArm.xRot = 0;
        this.rightArm.xRot = 0;
        this.wingLeft.zRot = 0.3F;
        this.wingRight.zRot = -0.3F;
        this.hip.y = 18.5F;
        this.orb.y = -7.5F;

        float swing = state.walkAnimationPos;
        float amount = state.walkAnimationSpeed;
        this.leftLeg.xRot = Mth.cos(swing * 0.6662F) * 1.1F * amount;
        this.rightLeg.xRot = Mth.cos(swing * 0.6662F + (float) Math.PI) * 1.1F * amount;
        this.leftArm.xRot = Mth.cos(swing * 0.6662F + (float) Math.PI) * 0.55F * amount;
        this.rightArm.xRot = Mth.cos(swing * 0.6662F) * 0.55F * amount;
        this.tail.yRot = Mth.cos(swing * 0.4F) * 0.35F * amount;
        this.tailMid.yRot = Mth.sin(state.ageInTicks * 0.12F) * 0.12F;
        this.tailTip.yRot = Mth.sin(state.ageInTicks * 0.18F + 1f) * 0.18F;
        this.wingLeft.zRot = 0.3F + Mth.cos(state.ageInTicks * 0.42F) * 0.42F;
        this.wingRight.zRot = -0.3F - Mth.cos(state.ageInTicks * 0.42F) * 0.42F;

        float bob = Mth.sin(state.ageInTicks * 0.1F) * 0.45F;
        this.hip.y += bob;
        this.head.xRot += Mth.sin(state.ageInTicks * 0.08F) * 0.03F;
        this.jaw.xRot = Math.max(0f, Mth.sin(state.ageInTicks * 0.15F)) * 0.08F;
        this.earLeft.zRot = 0.35F + Mth.sin(state.ageInTicks * 0.09F) * 0.06F;
        this.earRight.zRot = -0.35F - Mth.sin(state.ageInTicks * 0.09F) * 0.06F;
        this.orb.y = -7.5F + Mth.sin(state.ageInTicks * 0.2F) * 0.8F;
        this.cape.xRot = 0.25F + Mth.sin(state.ageInTicks * 0.1F) * 0.05F + amount * 0.1F;

        // Subtle pupil look
        float look = Mth.clamp(state.yRot / 40f, -0.4f, 0.4f);
        this.pupilL.x = 0.15F + look * 0.25F;
        this.pupilR.x = -0.15F + look * 0.25F;

        applyLook(state);
    }

    private void applyLook(MonRenderState state) {
        BodyShape shape = state.bodyShape != null ? state.bodyShape : BodyShape.QUAD;
        MonElement element = state.element != null ? state.element : MonElement.NORMAL;
        MonSpecies species = state.species != null ? state.species : MonSpecies.RATTATA;
        int stage = state.stage;
        boolean grok = species == MonSpecies.MEW || species == MonSpecies.MEWTWO || species == MonSpecies.MOLTRES;

        // Defaults: detailed terrestrial critter
        boolean wings = false;
        boolean crest = false;
        boolean crestMulti = false;
        boolean leaf = false;
        boolean fin = false;
        boolean finSides = false;
        boolean tallEars = true;
        boolean arms = true;
        boolean snout = true;
        boolean legs = true;
        boolean shell = false;
        boolean horns = false;
        boolean longTail = true;
        boolean cheeks = true;
        boolean mane = false;
        boolean cape = false;
        boolean eyeRing = false;
        boolean antenna = false;
        boolean orb = false;
        boolean chestStar = false;
        boolean brow = true;
        boolean jaw = true;

        switch (shape) {
            case QUAD -> {
                tallEars = true;
                arms = stage >= 2;
                snout = true;
            }
            case BIPED -> {
                tallEars = stage <= 1;
                arms = true;
                crest = stage >= 2 || element == MonElement.FIRE || element == MonElement.FIGHTING;
                mane = stage >= 2;
            }
            case AVIAN -> {
                wings = true;
                arms = false;
                tallEars = false;
                snout = true;
                cheeks = false;
                brow = false;
            }
            case AQUATIC -> {
                fin = true;
                finSides = true;
                tallEars = false;
                arms = false;
                longTail = true;
                this.tail.xRot = 0.05F;
                cheeks = false;
                brow = false;
            }
            case SERPENT -> {
                legs = false;
                arms = false;
                tallEars = false;
                horns = stage >= 2 || element == MonElement.DRAGON;
                longTail = true;
                this.tail.xRot = 0.55F;
                wings = element == MonElement.DRAGON && stage >= 2;
                cheeks = false;
            }
            case INSECTOID -> {
                wings = true;
                tallEars = false;
                shell = true;
                arms = false;
                cheeks = false;
                antenna = true;
            }
            case AMORPH -> {
                legs = false;
                arms = false;
                tallEars = false;
                snout = false;
                longTail = false;
                cheeks = true;
                orb = true;
                jaw = false;
            }
            case ARMORED -> {
                shell = true;
                tallEars = false;
                crest = stage >= 2;
                arms = stage >= 2;
                cheeks = false;
            }
            case FEY -> {
                wings = true;
                tallEars = true;
                cheeks = true;
                orb = stage >= 2;
            }
        }

        if (element == MonElement.GRASS) {
            leaf = true;
        }
        if (element == MonElement.FIRE && shape != BodyShape.AQUATIC) {
            crest = true;
            crestMulti = stage >= 2;
            this.tail.xRot = Math.max(this.tail.xRot, 0.5F);
        }
        if (element == MonElement.ELECTRIC) {
            crest = true;
            crestMulti = true;
            cheeks = true;
            antenna = stage >= 2;
        }
        if (element == MonElement.ICE && stage >= 2) {
            horns = true;
        }
        if (element == MonElement.PSYCHIC) {
            orb = true;
            brow = true;
        }
        if (element == MonElement.DRAGON && stage >= 2) {
            mane = true;
            horns = true;
        }

        // ===== GROK LINE — max detail =====
        if (grok) {
            eyeRing = true;
            cheeks = true;
            brow = true;
            jaw = true;
            snout = true;
            orb = true;
            if (species == MonSpecies.MEW) {
                antenna = true;
                tallEars = false;
                wings = false;
                arms = false;
                legs = true;
                mane = false;
                cape = false;
                crest = false;
                longTail = true;
                chestStar = false;
            } else if (species == MonSpecies.MEWTWO) {
                antenna = false;
                tallEars = true;
                mane = true;
                cape = true;
                arms = true;
                legs = true;
                wings = false;
                crest = true;
                crestMulti = true;
                chestStar = true;
                horns = false;
            } else { // MOLTRES
                antenna = true;
                tallEars = false;
                mane = true;
                cape = true;
                arms = false;
                legs = false;
                wings = true;
                horns = true;
                crest = true;
                crestMulti = true;
                chestStar = true;
                longTail = true;
                this.tail.xRot = 0.4F;
            }
        }

        this.leftLeg.visible = legs;
        this.rightLeg.visible = legs;
        this.leftFoot.visible = legs;
        this.rightFoot.visible = legs;
        this.wingLeft.visible = wings;
        this.wingRight.visible = wings;
        this.wingLeftInner.visible = wings;
        this.wingRightInner.visible = wings;
        this.crest.visible = crest;
        this.crest2.visible = crestMulti;
        this.crest3.visible = crestMulti;
        this.leaf.visible = leaf;
        this.fin.visible = fin;
        this.finSideL.visible = finSides;
        this.finSideR.visible = finSides;
        this.earLeft.visible = tallEars;
        this.earRight.visible = tallEars;
        this.leftArm.visible = arms;
        this.rightArm.visible = arms;
        this.snout.visible = snout;
        this.jaw.visible = jaw && snout;
        this.tail.visible = longTail;
        this.shell.visible = shell;
        this.hornLeft.visible = horns;
        this.hornRight.visible = horns;
        this.cheekL.visible = cheeks;
        this.cheekR.visible = cheeks;
        this.belly.visible = true;
        this.chest.visible = true;
        this.collar.visible = stage >= 2 || grok;
        this.maneL.visible = mane;
        this.maneR.visible = mane;
        this.maneBack.visible = mane;
        this.cape.visible = cape;
        this.eyeRing.visible = eyeRing;
        this.antennaL.visible = antenna;
        this.antennaR.visible = antenna;
        this.orb.visible = orb;
        this.chestStar.visible = chestStar;
        this.brow.visible = brow;
        this.eyeL.visible = true;
        this.eyeR.visible = true;
        // Single big eye vibe for Groknova — hide one slightly by stacking
        if (species == MonSpecies.MOLTRES) {
            this.eyeR.visible = false;
            this.eyeL.x = 0.0F;
            this.eyeRing.visible = true;
        } else {
            this.eyeL.x = 1.6F;
        }
    }
}

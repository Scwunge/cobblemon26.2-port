package com.cobblemon.mod.client.model;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * Single vertical quad — used to display pack Pokémon textures as a billboard
 * (pack provides skins for Cobblemon geo; we don't ship those meshes).
 */
public class MonBillboardModel extends EntityModel<MonRenderState> {
    private final ModelPart root;
    private final ModelPart plane;

    public MonBillboardModel(ModelPart root) {
        super(root);
        this.root = root;
        this.plane = root.getChild("plane");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // 16×16 unit plane, UV full 0–16 map of a square texture
        root.addOrReplaceChild(
                "plane",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-8.0F, 0.0F, -0.5F, 16.0F, 16.0F, 1.0F),
                PartPose.offset(0.0F, 8.0F, 0.0F)
        );
        return LayerDefinition.create(mesh, 16, 16);
    }

    @Override
    public void setupAnim(MonRenderState state) {
        // Yaw is applied in renderer (face camera); idle bob
        this.plane.yRot = 0;
        this.plane.xRot = 0;
        this.plane.zRot = 0;
        this.plane.y = 8.0F + (float) Math.sin(state.ageInTicks * 0.08f) * 0.35f;
    }
}

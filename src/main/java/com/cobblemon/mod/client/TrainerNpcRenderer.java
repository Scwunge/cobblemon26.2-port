package com.cobblemon.mod.client;

import com.cobblemon.mod.client.model.ModModelLayers;
import com.cobblemon.mod.client.model.TrainerNpcModel;
import com.cobblemon.mod.entity.TrainerNpcEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

/**
 * Placeholder humanoid renderer for trainer NPCs (Steve-like texture).
 */
public class TrainerNpcRenderer extends MobRenderer<TrainerNpcEntity, LivingEntityRenderState, TrainerNpcModel> {
    /** Vanilla zombie texture — always present; placeholder until trainer skins land. */
    private static final Identifier TEXTURE =
            Identifier.withDefaultNamespace("textures/entity/zombie/zombie.png");

    public TrainerNpcRenderer(EntityRendererProvider.Context context) {
        super(context, new TrainerNpcModel(context.bakeLayer(ModModelLayers.TRAINER_NPC)), 0.5f);
    }

    @Override
    public LivingEntityRenderState createRenderState() {
        return new LivingEntityRenderState();
    }

    @Override
    public Identifier getTextureLocation(LivingEntityRenderState state) {
        return TEXTURE;
    }

    @Override
    protected boolean shouldShowName(TrainerNpcEntity entity, double distanceToCameraSq) {
        return distanceToCameraSq < 64 * 64;
    }
}

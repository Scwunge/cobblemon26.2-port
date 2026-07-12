package com.cobblemon.mod.client;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.mojang.blaze3d.vertex.PoseStack;

import org.jspecify.annotations.Nullable;

import com.cobblemon.mod.client.model.BedrockGeoLoader;
import com.cobblemon.mod.client.model.CobblemonGeoModel;
import com.cobblemon.mod.client.model.ModModelLayers;
import com.cobblemon.mod.client.model.MonBillboardModel;
import com.cobblemon.mod.client.model.MonRenderState;
import com.cobblemon.mod.entity.WildMonEntity;
import com.cobblemon.mod.species.MonForm;
import com.cobblemon.mod.species.MonSpecies;
import com.cobblemon.mod.species.SpeciesTextures;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;

/**
 * Renders wild/companion mons with Cobblemon Bedrock geo meshes + pack rework textures.
 * Falls back to a camera-facing billboard if a geo file is missing.
 */
public class WildMonRenderer extends MobRenderer<WildMonEntity, MonRenderState, EntityModel<? super MonRenderState>> {
    private final MonBillboardModel billboard;
    /** Cache key is geoPath + "\\0" + animPath (not an Identifier — "|" is illegal in IDs). */
    private final Map<String, CobblemonGeoModel> geoModels = new ConcurrentHashMap<>();

    public WildMonRenderer(EntityRendererProvider.Context context) {
        super(context, new MonBillboardModel(context.bakeLayer(ModModelLayers.BILLBOARD)), 0.4f);
        this.billboard = (MonBillboardModel) this.getModel();
        // Animated emissive flame (Charmander / Charmeleon / Charizard / …)
        this.addLayer(new MonFlameLayer(this));
    }

    private CobblemonGeoModel geoModel(Identifier geoId, Identifier animId) {
        // Key by geo+anim so species never share a wrong animation binding
        String key = geoId.toString() + "\0" + animId.toString();
        return geoModels.computeIfAbsent(key, ignored -> {
            CobblemonGeoModel m = new CobblemonGeoModel(BedrockGeoLoader.getOrLoad(geoId));
            m.bindAnimation(animId);
            return m;
        });
    }

    @Override
    public MonRenderState createRenderState() {
        return new MonRenderState();
    }

    @Override
    public Identifier getTextureLocation(MonRenderState state) {
        String id = state.speciesId != null ? state.speciesId : (state.species != null ? state.species.id() : "rattata");
        return com.cobblemon.mod.species.SpeciesAssets.textureId(id);
    }

    @Override
    public void extractRenderState(WildMonEntity entity, MonRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        MonSpecies species = entity.getSpecies();
        state.species = species;
        state.speciesId = entity.getSpeciesId();
        var handle = com.cobblemon.mod.species.SpeciesHandle.of(entity.getSpeciesId());
        state.element = handle.primaryType();
        state.bodyShape = species.bodyShape();
        state.stage = handle.stage();
        state.formId = entity.getForm().id();
        state.genderId = entity.getGender().id();
        state.modelScale = handle.sizeScale();
        // Drive walk vs idle / ride flight animation clips
        state.limbSwing = state.walkAnimationPos;
        state.limbSwingAmount = state.walkAnimationSpeed;
        state.isRidden = entity.isVehicle();
        state.isRidingFlight = entity.isRidingFlight();
        if (state.isRidden && entity.getControllingPassenger() != null) {
            state.riderPitch = entity.getControllingPassenger().getXRot();
        } else {
            state.riderPitch = 0f;
        }
        float scale = 0.95f + handle.stage() * 0.12f;
        scale *= entity.getSizeScale();
        if (entity.getForm() == MonForm.ALPHA) {
            scale *= 1.15f;
        }
        if (species.isLegendary()) {
            scale *= 1.2f;
        }
        // Cobblemon geo is ~1 block for small mons; tune with species modelScale
        scale *= 0.85f * Math.max(0.4f, handle.sizeScale());
        state.scale = scale;
    }

    @Override
    public void submit(
            MonRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera
    ) {
        String id = state.speciesId != null ? state.speciesId : (state.species != null ? state.species.id() : "rattata");
        Identifier geoId = com.cobblemon.mod.species.SpeciesAssets.geoId(id);
        Identifier animId = com.cobblemon.mod.species.SpeciesAssets.animationId(id);
        // Prefer 3D geo + bedrock animations; billboard only if geo is missing
        this.model = BedrockGeoLoader.hasMesh(geoId) ? geoModel(geoId, animId) : this.billboard;
        super.submit(state, poseStack, collector, camera);
    }

    /**
     * LivingEntityRenderer applies {@code scale(-1,-1,1)} then {@code translate(0,-1.501)}.
     * Geo is baked Cobblemon-style (Y-down ModelPart, feet near model Y≈0).
     * Vanilla's -1.501 is for 24px-tall player models (feet at Y=24); Cobblemon feet sit
     * near Y=0, so that offset lifts every mon ~1.5 blocks. Cancel only the height nudge;
     * keep the Y flip so arm/leg animations stay correct.
     */
    @Override
    protected void scale(MonRenderState state, PoseStack poseStack) {
        poseStack.translate(0.0F, 1.501F, 0.0F);
    }

    @Override
    protected boolean shouldShowName(WildMonEntity entity, double distanceToCameraSq) {
        if (entity.isCustomNameVisible()) {
            return distanceToCameraSq < 64 * 64;
        }
        return entity.hasCustomName()
                && entity == this.entityRenderDispatcher.crosshairPickEntity
                && distanceToCameraSq < 48 * 48;
    }

    @Override
    protected @Nullable RenderType getRenderType(
            MonRenderState state,
            boolean isBodyVisible,
            boolean forceTransparent,
            boolean appearGlowing
    ) {
        if (!isBodyVisible && !forceTransparent) {
            return appearGlowing ? RenderTypes.outline(getTextureLocation(state)) : null;
        }
        return RenderTypes.entityCutout(getTextureLocation(state));
    }

    @Override
    protected int getModelTint(MonRenderState state) {
        if ("shiny".equals(state.formId)) {
            return ARGB.color(255, 255, 245, 220);
        }
        return ARGB.white(1.0f);
    }
}

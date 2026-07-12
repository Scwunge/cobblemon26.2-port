package com.cobblemon.mod.client;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.mojang.blaze3d.vertex.PoseStack;

import com.cobblemon.mod.Cobblemon;
import com.cobblemon.mod.client.model.MonRenderState;
import com.cobblemon.mod.species.SpeciesAssets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;

/**
 * Second-pass emissive flame for Charmander-line / Slugma-line / etc.
 * <p>
 * Cobblemon uses resolver {@code layers} with animated flame textures that only
 * contain the fire UV islands — re-drawing the full geo with that texture as
 * translucent-emissive lights the tail flame without x-raying the whole body.
 */
public class MonFlameLayer extends RenderLayer<MonRenderState, EntityModel<? super MonRenderState>> {
    /** Species known (or discovered) to have {@code *_flame1.png}…{@code *_flame4.png}. */
    private static final Set<String> KNOWN_FLAME = Set.of(
            "charmander", "charmeleon", "charizard", "slugma", "magcargo", "talonflame"
    );
    private static final Map<String, Boolean> HAS_FLAME = new ConcurrentHashMap<>();
    private static final int FRAME_COUNT = 4;
    private static final float FPS = 10f;

    public MonFlameLayer(RenderLayerParent<MonRenderState, EntityModel<? super MonRenderState>> parent) {
        super(parent);
    }

    @Override
    public void submit(
            PoseStack poseStack,
            SubmitNodeCollector collector,
            int packedLight,
            MonRenderState state,
            float yRot,
            float xRot
    ) {
        String id = state.speciesId != null ? state.speciesId : "rattata";
        if (!hasFlame(id)) {
            return;
        }
        Identifier tex = flameTexture(id, state.ageInTicks);
        if (tex == null) {
            return;
        }
        // Fullbright light so flame reads as glowing
        int light = 0x00F000F0;
        collector.order(1).submitModel(
                this.getParentModel(),
                state,
                poseStack,
                RenderTypes.entityTranslucentEmissive(tex),
                light,
                OverlayTexture.NO_OVERLAY,
                state.outlineColor,
                null
        );
    }

    private static boolean hasFlame(String speciesId) {
        String key = speciesId.toLowerCase();
        return HAS_FLAME.computeIfAbsent(key, id -> {
            if (KNOWN_FLAME.contains(id)) {
                return flameTexture(id, 0f) != null;
            }
            // Lazy probe for any other species with flame frames
            return flameTexture(id, 0f) != null;
        });
    }

    /**
     * {@code textures/pokemon/0004_charmander/charmander.png}
     * → {@code .../charmander_flame1.png} … {@code _flame4.png}
     */
    private static Identifier flameTexture(String speciesId, float ageInTicks) {
        Identifier base = SpeciesAssets.textureId(speciesId);
        String path = base.getPath(); // textures/pokemon/.../name.png
        if (!path.endsWith(".png")) {
            return null;
        }
        int frame = 1 + (Math.floorMod((int) (ageInTicks * FPS / 20f), FRAME_COUNT));
        String flamePath = path.substring(0, path.length() - 4) + "_flame" + frame + ".png";
        Identifier id = Identifier.fromNamespaceAndPath(Cobblemon.MOD_ID, flamePath);
        ResourceManager rm = Minecraft.getInstance().getResourceManager();
        if (rm.getResource(id).isEmpty()) {
            // shiny path fallback already handled by textureId; try non-shiny folder naming
            return null;
        }
        return id;
    }
}

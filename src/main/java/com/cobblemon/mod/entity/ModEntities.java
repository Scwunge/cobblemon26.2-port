package com.cobblemon.mod.entity;

import java.util.function.Supplier;

import com.cobblemon.mod.Cobblemon;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    public static final DeferredRegister.Entities ENTITY_TYPES = DeferredRegister.createEntities(Cobblemon.MOD_ID);

    /**
     * MISC (not CREATURE) so vanilla natural spawn loops never pack these during worldgen.
     * Wild encounters are via command / future custom spawner — natural biome spawns were
     * freezing "Loading terrain" on MC 26.1 chunk prep.
     */
    public static final Supplier<EntityType<WildMonEntity>> WILD_MON = ENTITY_TYPES.registerEntityType(
            "wild_mon",
            WildMonEntity::new,
            MobCategory.MISC,
            // Base box; per-species scale applied via Attributes.SCALE on the entity
            builder -> builder.sized(0.72f, 1.05f).eyeHeight(0.88f).clientTrackingRange(10)
    );

    public static final Supplier<EntityType<CubeBallEntity>> CUBE_BALL = ENTITY_TYPES.registerEntityType(
            "thrown_poke_ball",
            CubeBallEntity::new,
            MobCategory.MISC,
            // Wide tracking + frequent updates so throws stay visible in flight
            builder -> builder.sized(0.35f, 0.35f).clientTrackingRange(64).updateInterval(1)
    );

    private ModEntities() {}
}

package com.cobblemon.mod.entity;

import com.cobblemon.mod.Cobblemon;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;

/**
 * Placement registration only. Wild population is handled by {@link WildMonSpawner}
 * (player-near, post-load) — not vanilla creature packing (that froze Loading terrain).
 */
public final class ModSpawn {
    private ModSpawn() {}

    public static void register(IEventBus modBus) {
        modBus.addListener(ModSpawn::registerSpawnPlacements);
    }

    private static void registerSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(
                ModEntities.WILD_MON.get(),
                SpawnPlacementTypes.NO_RESTRICTIONS,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                ModSpawn::checkSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE
        );
        Cobblemon.LOGGER.info("Wild Cobblemon placement registered; live spawns via WildMonSpawner");
    }

    /** Vanilla packing stays off; custom spawner uses EVENT / command / egg. */
    public static boolean checkSpawnRules(
            EntityType<WildMonEntity> type,
            ServerLevelAccessor level,
            EntitySpawnReason reason,
            BlockPos pos,
            RandomSource random
    ) {
        return reason == EntitySpawnReason.SPAWN_ITEM_USE
                || reason == EntitySpawnReason.MOB_SUMMONED
                || reason == EntitySpawnReason.COMMAND
                || reason == EntitySpawnReason.EVENT
                || reason == EntitySpawnReason.LOAD;
    }
}

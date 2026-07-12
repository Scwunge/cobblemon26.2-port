package com.cobblemon.mod.entity;

import com.cobblemon.mod.Cobblemon;
import com.cobblemon.mod.species.MonSpecies;
import com.cobblemon.mod.species.OwnedMon;
import com.cobblemon.mod.species.SpawnEntry;
import com.cobblemon.mod.species.SpawnRules;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Cobblemon-style wild spawner — packing density, multi-attempt cycles,
 * surface / water / cave probes, and small packs for common species.
 */
public final class WildMonSpawner {
    /** Slightly slower cadence under load (was 16). */
    private static final int TICKS_BETWEEN_ATTEMPTS = 24;
    /** Soft cap of wilds in radius — keep world denser but not model-thrashy. */
    private static final int MAX_NEARBY_SOFT = 14;
    private static final int MAX_NEARBY_HARD = 20;
    private static final double CHECK_RADIUS = 80.0;
    private static final int MIN_DIST = 14;
    private static final int MAX_DIST = 64;
    private static final int MIN_ENTITY_GAP = 7;
    private static final float ATTEMPT_CHANCE = 0.78f;
    /** How many independent spawn tries per player per cycle when under soft cap. */
    private static final int SPAWNS_PER_CYCLE = 1;

    private static int tickCounter;

    private WildMonSpawner() {}

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        tickCounter++;
        if (tickCounter < TICKS_BETWEEN_ATTEMPTS) {
            return;
        }
        tickCounter = 0;

        var server = event.getServer();
        if (server == null) {
            return;
        }

        for (ServerLevel level : server.getAllLevels()) {
            if (level.players().isEmpty()) {
                continue;
            }
            var dim = level.dimension();
            if (dim != net.minecraft.world.level.Level.OVERWORLD
                    && dim != net.minecraft.world.level.Level.NETHER
                    && dim != net.minecraft.world.level.Level.END) {
                continue;
            }
            for (ServerPlayer player : level.players()) {
                int budget = SPAWNS_PER_CYCLE;
                // Night: rare second attempt only (was +1 every cycle — too dense under load)
                long day = level.getOverworldClockTime() % 24000L;
                if (day >= 13000L && day < 23000L && level.getRandom().nextFloat() < 0.35f) {
                    budget++;
                }
                for (int i = 0; i < budget; i++) {
                    if (!trySpawnNear(level, player)) {
                        break;
                    }
                }
            }
        }
    }

    private static boolean trySpawnNear(ServerLevel level, ServerPlayer player) {
        RandomSource random = level.getRandom();

        AABB box = player.getBoundingBox().inflate(CHECK_RADIUS);
        int nearby = level.getEntitiesOfClass(
                WildMonEntity.class,
                box,
                e -> !e.isCompanion() && e.isAlive()
        ).size();
        if (nearby >= MAX_NEARBY_HARD) {
            return false;
        }
        // Soft cap: reduced chance
        if (nearby >= MAX_NEARBY_SOFT && random.nextFloat() > 0.35f) {
            return false;
        }
        if (random.nextFloat() > ATTEMPT_CHANCE) {
            return false;
        }

        BlockPos playerPos = player.blockPosition();
        for (int attempt = 0; attempt < 16; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            int dist = MIN_DIST + random.nextInt(MAX_DIST - MIN_DIST + 1);
            int dx = Mth.floor(Math.cos(angle) * dist);
            int dz = Mth.floor(Math.sin(angle) * dist);
            int x = playerPos.getX() + dx;
            int z = playerPos.getZ() + dz;

            if (!level.hasChunkAt(x, z)) {
                continue;
            }

            // 55% surface, 25% water, 20% cave/underground
            float mode = random.nextFloat();
            boolean preferWater = mode < 0.25f;
            boolean preferCave = mode >= 0.25f && mode < 0.45f;
            BlockPos feet;
            if (preferWater) {
                int ySurface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                feet = findWaterColumn(level, x, z, ySurface);
                if (feet == null) {
                    feet = new BlockPos(x, ySurface, z);
                    preferWater = false;
                }
            } else if (preferCave) {
                feet = findCaveSpot(level, x, z, playerPos.getY(), random);
                if (feet == null) {
                    int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                    feet = new BlockPos(x, y, z);
                }
            } else {
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                feet = new BlockPos(x, y, z);
            }

            if (!level.getWorldBorder().isWithinBounds(feet)) {
                continue;
            }

            boolean inWater = isWater(level, feet);
            if (!inWater && !isStandableLand(level, feet)) {
                continue;
            }
            if (inWater && !isUsableWater(level, feet)) {
                continue;
            }

            AABB gap = new AABB(feet).inflate(MIN_ENTITY_GAP);
            if (!level.getEntitiesOfClass(WildMonEntity.class, gap, e -> e.isAlive()).isEmpty()) {
                continue;
            }

            SpawnRules.Pick pick;
            try {
                pick = SpawnRules.pick(level, feet, random);
            } catch (Exception e) {
                Cobblemon.LOGGER.debug("Wild spawn pick failed: {}", e.toString());
                pick = new SpawnRules.Pick(
                        inWater ? MonSpecies.MAGIKARP : MonSpecies.RATTATA,
                        3 + random.nextInt(6),
                        inWater ? SpawnEntry.Position.SUBMERGED : SpawnEntry.Position.GROUNDED
                );
            }

            if ((pick.position() == SpawnEntry.Position.SUBMERGED
                    || pick.position() == SpawnEntry.Position.SURFACE) && !inWater) {
                continue;
            }
            if (pick.position() == SpawnEntry.Position.GROUNDED && inWater) {
                continue;
            }

            // Pack size: common stage-1 often 1–3
            int pack = 1;
            SpeciesHandleLite stage = SpeciesHandleLite.of(pick.speciesId());
            if (stage.commonPack() && random.nextFloat() < 0.4f) {
                pack = 1 + random.nextInt(3);
            }

            int spawned = 0;
            for (int p = 0; p < pack; p++) {
                BlockPos at = feet.offset(
                        p == 0 ? 0 : random.nextInt(3) - 1,
                        0,
                        p == 0 ? 0 : random.nextInt(3) - 1
                );
                if (!level.hasChunkAt(at)) {
                    continue;
                }
                WildMonEntity mon = ModEntities.WILD_MON.get().create(level, EntitySpawnReason.EVENT);
                if (mon == null) {
                    break;
                }
                mon.setPos(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
                mon.setYRot(random.nextFloat() * 360f);
                mon.applyIdentity(OwnedMon.createWild(pick.speciesId(), pick.level(), random));
                if (!level.noCollision(mon) && !inWater) {
                    mon.discard();
                    continue;
                }
                level.addFreshEntity(mon);
                spawned++;
            }
            return spawned > 0;
        }
        return false;
    }

    private static BlockPos findCaveSpot(ServerLevel level, int x, int z, int playerY, RandomSource random) {
        int baseY = Mth.clamp(playerY + random.nextInt(17) - 8, level.getMinY() + 8, level.getMaxY() - 16);
        for (int i = 0; i < 8; i++) {
            int y = baseY + random.nextInt(9) - 4;
            BlockPos feet = new BlockPos(x, y, z);
            if (isStandableLand(level, feet) && level.getBrightness(LightLayer.SKY, feet) <= 7) {
                return feet;
            }
        }
        return null;
    }

    private static BlockPos findWaterColumn(ServerLevel level, int x, int z, int surfaceY) {
        for (int dy = 0; dy < 14; dy++) {
            int y = surfaceY - dy;
            BlockPos p = new BlockPos(x, y, z);
            if (isUsableWater(level, p)) {
                return p;
            }
        }
        return null;
    }

    private static boolean isWater(ServerLevel level, BlockPos pos) {
        var f = level.getFluidState(pos);
        return f.is(Fluids.WATER) || f.is(Fluids.FLOWING_WATER);
    }

    private static boolean isUsableWater(ServerLevel level, BlockPos pos) {
        if (!isWater(level, pos)) {
            return false;
        }
        return level.getFluidState(pos.above()).isEmpty() || isWater(level, pos.above());
    }

    private static boolean isStandableLand(ServerLevel level, BlockPos feet) {
        BlockPos below = feet.below();
        if (!level.getBlockState(below).isSolid()) {
            return false;
        }
        if (!level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()) {
            return false;
        }
        if (!level.getFluidState(feet).isEmpty() || !level.getFluidState(feet.above()).isEmpty()) {
            return false;
        }
        return true;
    }

    /** Tiny helper so we don't pull full SpeciesHandle for pack logic only. */
    private record SpeciesHandleLite(boolean commonPack) {
        static SpeciesHandleLite of(String id) {
            var h = com.cobblemon.mod.species.SpeciesHandle.of(id);
            // Stage 1 and high catch rate → packable
            boolean pack = h.stage() <= 1 && h.catchRate() >= 120;
            return new SpeciesHandleLite(pack);
        }
    }
}

package com.cobblemon.mod.entity;

import com.cobblemon.mod.battle.BattleManager;
import com.cobblemon.mod.item.BallContext;
import com.cobblemon.mod.item.CatchCalc;
import com.cobblemon.mod.item.CubeBallTier;
import com.cobblemon.mod.party.PartyHelper;
import com.cobblemon.mod.species.OwnedMon;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Thrown Poké Ball. Uses Cobblemon 3D cube item models in flight, seats on the ground,
 * shakes, then resolves catch with the ball's real catch multiplier.
 */
public class CubeBallEntity extends ThrowableItemProjectile {
    private static final EntityDataAccessor<String> DATA_TIER =
            SynchedEntityData.defineId(CubeBallEntity.class, EntityDataSerializers.STRING);
    /** 0 = flying, 1 = capturing (stuck on mon), 2 = resolved */
    private static final EntityDataAccessor<Integer> DATA_PHASE =
            SynchedEntityData.defineId(CubeBallEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_TARGET_ID =
            SynchedEntityData.defineId(CubeBallEntity.class, EntityDataSerializers.INT);

    /** Total capture performance on the ground (Cobblemon/Pixelmon-style). */
    private static final int SHAKE_TICKS = 80;
    /** Mainline uses 4 shake checks; we animate each. */
    private static final int SHAKES = 4;
    /** Quiet pause after landing before first rock. */
    private static final int LAND_SETTLE_TICKS = 8;

    private int captureAge;
    private int nextShakeAt;
    private int shakesDone;
    /** How many shake checks succeed before break/catch (from CatchCalc.roll). */
    private int shakesToSucceed;
    private boolean pendingSuccess;
    private int targetEntityId = -1;
    /** Ground seat for the ball while capturing (server + client). */
    private double landX;
    private double landY;
    private double landZ;
    /** Local fallback for default item before synched data is ready. */
    private CubeBallTier spawnTier = CubeBallTier.POKE;

    public CubeBallEntity(EntityType<? extends CubeBallEntity> type, Level level) {
        super(type, level);
    }

    public CubeBallEntity(Level level, LivingEntity shooter, ItemStack stack, CubeBallTier tier) {
        super(ModEntities.CUBE_BALL.get(), shooter, level, stack.copyWithCount(1));
        this.spawnTier = tier;
        setTier(tier);
        // Ensure item stack is non-empty for client render (was empty when getDefaultItem failed)
        setItem(stack.copyWithCount(1).isEmpty() ? new ItemStack(itemForTier(tier)) : stack.copyWithCount(1));
    }

    public CubeBallEntity(Level level, double x, double y, double z, ItemStack stack, CubeBallTier tier) {
        super(ModEntities.CUBE_BALL.get(), x, y, z, level, stack.copyWithCount(1));
        this.spawnTier = tier;
        setTier(tier);
        setItem(stack.copyWithCount(1).isEmpty() ? new ItemStack(itemForTier(tier)) : stack.copyWithCount(1));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        // super first (vanilla). getDefaultItem uses spawnTier field only — never DATA_TIER.
        super.defineSynchedData(builder);
        builder.define(DATA_TIER, CubeBallTier.POKE.id());
        builder.define(DATA_PHASE, 0);
        builder.define(DATA_TARGET_ID, -1);
    }

    public void setTier(CubeBallTier tier) {
        this.spawnTier = tier != null ? tier : CubeBallTier.POKE;
        this.entityData.set(DATA_TIER, this.spawnTier.id());
    }

    public CubeBallTier getTier() {
        // Prefer synched id; fall back to spawn field during construction
        try {
            return CubeBallTier.byId(this.entityData.get(DATA_TIER));
        } catch (Exception e) {
            return spawnTier != null ? spawnTier : CubeBallTier.POKE;
        }
    }

    /** Non-empty stack for client render when synched item is not ready yet. */
    public ItemStack getDefaultRenderStack() {
        return getTier().stack(1);
    }

    public int getPhase() {
        return this.entityData.get(DATA_PHASE);
    }

    public int getTargetEntityId() {
        return this.entityData.get(DATA_TARGET_ID);
    }

    private void setPhase(int phase) {
        this.entityData.set(DATA_PHASE, phase);
    }

    private static Item itemForTier(CubeBallTier tier) {
        return (tier != null ? tier : CubeBallTier.POKE).item();
    }

    /**
     * Must NOT read synched DATA_TIER — ThrowableItemProjectile.defineSynchedData
     * calls this while building entity data. Reading undefined accessors breaks the item stack.
     */
    @Override
    protected Item getDefaultItem() {
        return itemForTier(spawnTier != null ? spawnTier : CubeBallTier.POKE);
    }

    @Override
    public ItemStack getItem() {
        ItemStack stack = super.getItem();
        if (stack == null || stack.isEmpty()) {
            return new ItemStack(getDefaultItem());
        }
        return stack;
    }

    /** Vanilla hides throwables for 2 ticks when near the camera — makes throws invisible. */
    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return true;
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        if (entity == getOwner()) {
            return false;
        }
        if (entity instanceof WildMonEntity mon && mon.isCompanion()) {
            return false;
        }
        return super.canHitEntity(entity);
    }

    @Override
    public void tick() {
        if (getPhase() == 1) {
            // Sit still on the ground; gentle shake is visual only (renderer)
            this.setDeltaMovement(Vec3.ZERO);
            if (!this.level().isClientSide()) {
                // Server pins the ball to the computed ground seat
                this.setPos(landX, landY, landZ);
                tickCaptureServer();
            }
            // Client uses networked position from server — do not zero to local land*
            this.xo = this.getX();
            this.yo = this.getY();
            this.zo = this.getZ();
            return;
        }

        super.tick();

        if (!this.level().isClientSide() && this.level() instanceof ServerLevel server && getPhase() == 0) {
            if (this.tickCount % 2 == 0) {
                server.sendParticles(
                        ParticleTypes.CRIT,
                        getX(), getY(), getZ(),
                        2, 0.05, 0.05, 0.05, 0.01
                );
            }
        }
    }

    private void tickCaptureServer() {
        if (!(this.level() instanceof ServerLevel server)) {
            return;
        }
        Entity e = server.getEntity(targetEntityId);
        if (!(e instanceof WildMonEntity wild) || !wild.isAlive() || wild.isCompanion()) {
            dropAsItem();
            discard();
            return;
        }

        // Stay seated on the ground (do not hover mid-body)
        this.setPos(landX, landY, landZ);
        captureAge++;

        // Soft spaced rocks — one shake check per rock (Gen 3+ style)
        if (captureAge >= nextShakeAt && shakesDone < SHAKES) {
            shakesDone++;
            int gap = (SHAKE_TICKS - LAND_SETTLE_TICKS) / SHAKES;
            nextShakeAt = LAND_SETTLE_TICKS + shakesDone * gap;
            server.sendParticles(
                    ParticleTypes.POOF,
                    getX(), getY() + 0.15, getZ(),
                    4, 0.12, 0.05, 0.12, 0.01
            );
            level().playSound(null, getX(), getY(), getZ(),
                    SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.4f, 0.85f + shakesDone * 0.08f);

            // Broke free on this shake (didn't pass enough checks)
            if (!pendingSuccess && shakesDone > shakesToSucceed) {
                resolveCapture(server, wild);
                return;
            }
        }

        if (captureAge >= SHAKE_TICKS || (pendingSuccess && shakesDone >= SHAKES)) {
            resolveCapture(server, wild);
        }
    }

    private void resolveCapture(ServerLevel server, WildMonEntity wild) {
        setPhase(2);
        if (!(getOwner() instanceof ServerPlayer player)) {
            restoreWild(wild);
            discard();
            return;
        }

        if (pendingSuccess) {
            // toOwnedMon preserves shiny form / identity from the wild entity
            OwnedMon caught = CatchCalc.applyCatchEffects(wild.toOwnedMon(), getTier());
            // C2 — roll contextual mark on catch
            String markId = com.cobblemon.mod.species.MarkAward.rollOnCatch(player, caught, player.getRandom());
            if (markId != null && !markId.isBlank()) {
                caught = caught.withMark(markId);
            }
            if (PartyHelper.addMon(player, caught)) {
                player.sendSystemMessage(
                        Component.translatable("message.cobblemon.caught", caught.displayName(), caught.level())
                );
                if (caught.hasMark()) {
                    player.sendSystemMessage(Component.literal(
                            "§d★ " + com.cobblemon.mod.species.MarkAward.displayName(caught.mark()) + "!"));
                }
                if (caught.isShiny()) {
                    player.sendSystemMessage(Component.literal("§e★ Shiny!"));
                }
                PartyHelper.grantExp(player, 0, 10 + wild.getMonLevel() * 2);
                level().playSound(null, wild.getX(), wild.getY(), wild.getZ(),
                        SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 1.2f);
                server.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                        wild.getX(), wild.getY() + 0.5, wild.getZ(),
                        24, 0.45, 0.5, 0.45, 0.03);
                server.sendParticles(ParticleTypes.END_ROD,
                        wild.getX(), wild.getY() + 0.6, wild.getZ(),
                        12, 0.3, 0.4, 0.3, 0.02);
                wild.discard();
            } else {
                player.sendSystemMessage(Component.translatable("message.cobblemon.party_full"));
                restoreWild(wild);
                dropAsItem();
            }
        } else {
            player.sendSystemMessage(Component.translatable("message.cobblemon.catch_failed"));
            restoreWild(wild);
            server.sendParticles(ParticleTypes.CLOUD,
                    getX(), getY(), getZ(),
                    16, 0.3, 0.3, 0.3, 0.04);
            level().playSound(null, getX(), getY(), getZ(),
                    SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 0.8f, 0.7f);
            wild.hurtServer(server, damageSources().thrown(this, player), 1.0f);
        }
        discard();
    }

    private void restoreWild(WildMonEntity wild) {
        wild.setInvisible(false);
        wild.setNoAi(false);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (this.level().isClientSide() || getPhase() != 0) {
            return;
        }
        if (!(result.getEntity() instanceof WildMonEntity wild)) {
            // Pass through non-mons; keep flying
            return;
        }
        if (wild.isCompanion()) {
            return;
        }
        if (!(getOwner() instanceof ServerPlayer player)) {
            discard();
            return;
        }
        if (BattleManager.inBattle(player)) {
            player.sendSystemMessage(Component.translatable("message.cobblemon.catch_in_battle"));
            dropAsItem();
            discard();
            return;
        }
        if (PartyHelper.get(player).isFull()) {
            player.sendSystemMessage(Component.translatable("message.cobblemon.party_full"));
            dropAsItem();
            discard();
            return;
        }

        // Impact flash where the mon was
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.END_ROD,
                    wild.getX(), wild.getY() + wild.getBbHeight() * 0.4, wild.getZ(),
                    12, 0.2, 0.25, 0.2, 0.04);
            server.sendParticles(ParticleTypes.CLOUD,
                    wild.getX(), wild.getY() + 0.2, wild.getZ(),
                    10, 0.25, 0.15, 0.25, 0.02);
        }
        level().playSound(null, wild.getX(), wild.getY(), wild.getZ(),
                SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 0.75f, 1.15f);
        level().playSound(null, wild.getX(), wild.getY(), wild.getZ(),
                SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.6f, 0.5f);

        float hpRatio = wild.getHealth() / Math.max(1f, wild.getMaxHealth());
        // Prefer party status if identity carried; wild entity has no MonStatus → NONE
        com.cobblemon.mod.species.MonStatus status = com.cobblemon.mod.species.MonStatus.NONE;
        try {
            OwnedMon id = wild.toOwnedMon();
            if (id != null) {
                status = id.status();
            }
        } catch (Exception ignored) {
        }
        boolean alreadyCaught = false;
        try {
            alreadyCaught = com.cobblemon.mod.party.PartyHelper.getDex(player).hasCaught(wild.getSpeciesId());
        } catch (Exception ignored) {
        }
        BallContext ctx = BallContext.ofWild(
                player, wild.getSpeciesId(), wild.getMonLevel(), hpRatio, 0, alreadyCaught
        );
        // Prefer water/dark from impact position
        boolean inWater = !this.level().getFluidState(wild.blockPosition()).isEmpty();
        if (inWater || this.level().getBrightness(net.minecraft.world.level.LightLayer.BLOCK, wild.blockPosition()) <= 7) {
            ctx = new BallContext(
                    ctx.hpRatio(), ctx.speciesId(), ctx.wildLevel(), ctx.playerLevel(),
                    ctx.wildGender(), ctx.playerGender(),
                    inWater || ctx.inWater(),
                    this.level().getBrightness(net.minecraft.world.level.LightLayer.BLOCK, wild.blockPosition()) <= 7
                            || ctx.isDark(),
                    ctx.isNight(), ctx.battleTurns(), alreadyCaught, ctx.wildSpeedApprox()
            );
        }
        CatchCalc.Result roll = CatchCalc.roll(
                hpRatio, wild.getSpeciesId(), getTier(), status, this.level().getRandom(), ctx
        );
        pendingSuccess = roll.caught();
        shakesToSucceed = roll.shakesSucceeded();

        // Mon "sucked in" — hide in place
        wild.setNoAi(true);
        wild.setDeltaMovement(Vec3.ZERO);
        wild.setInvisible(true);

        // Seat the ball on the ground under the mon (Cobblemon/Pixelmon style)
        landX = wild.getX();
        landZ = wild.getZ();
        landY = findGroundY(wild.getX(), wild.getY(), wild.getZ()) + 0.08;
        this.setDeltaMovement(Vec3.ZERO);
        this.setPos(landX, landY, landZ);

        targetEntityId = wild.getId();
        this.entityData.set(DATA_TARGET_ID, targetEntityId);
        setPhase(1);
        captureAge = 0;
        shakesDone = 0;
        nextShakeAt = LAND_SETTLE_TICKS + (SHAKE_TICKS - LAND_SETTLE_TICKS) / SHAKES;

        if (level() instanceof ServerLevel server) {
            // Soft dust when the ball plops onto the ground
            server.sendParticles(ParticleTypes.POOF,
                    landX, landY, landZ,
                    6, 0.15, 0.02, 0.15, 0.01);
        }
        level().playSound(null, landX, landY, landZ,
                SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.45f, 0.45f);
    }

    /** Find a solid surface at/below the mon so the ball rests on the floor. */
    private double findGroundY(double x, double y, double z) {
        var level = this.level();
        int ix = Mth.floor(x);
        int iz = Mth.floor(z);
        int startY = Mth.floor(y + 0.5);
        for (int dy = 0; dy < 8; dy++) {
            int iy = startY - dy;
            var pos = new net.minecraft.core.BlockPos(ix, iy, iz);
            var below = pos.below();
            if (level.getBlockState(below).isSolid() && level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
                return iy;
            }
        }
        return y; // fallback: mon feet
    }

    @Override
    protected void onHit(HitResult result) {
        if (this.level().isClientSide() || getPhase() != 0) {
            return;
        }
        if (result.getType() == HitResult.Type.ENTITY) {
            onHitEntity((EntityHitResult) result);
            return;
        }
        // Block miss — recover ball
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.SMOKE,
                    getX(), getY(), getZ(),
                    6, 0.15, 0.15, 0.15, 0.01);
        }
        level().playSound(null, getX(), getY(), getZ(),
                SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.5f, 0.6f);
        dropAsItem();
        discard();
    }

    private void dropAsItem() {
        if (this.level().isClientSide()) {
            return;
        }
        ItemStack stack = getItem();
        if (stack.isEmpty()) {
            stack = new ItemStack(getDefaultItem());
        } else {
            stack = stack.copyWithCount(1);
        }
        ItemEntity drop = new ItemEntity(this.level(), getX(), getY(), getZ(), stack);
        drop.setPickUpDelay(10);
        this.level().addFreshEntity(drop);
    }

    /** Place slightly ahead of the thrower looking direction so the cube is visible immediately. */
    public void placeInFrontOf(LivingEntity thrower) {
        float yRot = thrower.getYRot() * ((float) Math.PI / 180F);
        float xRot = thrower.getXRot() * ((float) Math.PI / 180F);
        double dist = 0.8;
        double x = thrower.getX() - Mth.sin(yRot) * Mth.cos(xRot) * dist;
        double y = thrower.getEyeY() - 0.1 - Mth.sin(xRot) * dist;
        double z = thrower.getZ() + Mth.cos(yRot) * Mth.cos(xRot) * dist;
        this.setPos(x, y, z);
    }
}

package com.cobblemon.mod.battle;

import java.util.UUID;

import com.cobblemon.mod.entity.ModEntities;
import com.cobblemon.mod.entity.WildMonEntity;
import com.cobblemon.mod.network.SendOutPayload;
import com.cobblemon.mod.species.OwnedMon;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.Vec3;

/**
 * In-world battle staging: send out the player's mon, park both fighters
 * on opposite sides, freeze AI, face each other, play cry.
 * <p>
 * Official Cobblemon shows both Pokémon as world entities during battle;
 * this approximates that presentation for our wild-battle stack.
 */
public final class BattlePresentation {
    /** Distance from battlefield center to each mon. */
    private static final double HALF_ARENA = 2.6;
    /**
     * Player stands near the arena midpoint so the third-person battle camera
     * is centered on both mons (orbit happens client-side via yaw).
     */
    private static final double CAMERA_BACK = 0.35;
    private static final double CAMERA_SIDE = 0.2;
    private static final double CAMERA_UP = 0.15;

    private BattlePresentation() {}

    /**
     * Spawn/send-out active mon, position both fighters, freeze, face, cry.
     * Stores companion entity id on the session.
     * <p>
     * Player is parked near the field center so the client battle camera can
     * orbit around both world entities with both mons framed.
     */
    public static void setupField(ServerPlayer player, WildMonEntity wild, OwnedMon mon, BattleSession session) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        // Recall any existing free-roam companion so we don't have two
        WildMonEntity existing = SendOutPayload.findCompanion(player);
        if (existing != null && (session.companionEntityId() == null
                || !existing.getUUID().equals(session.companionEntityId()))) {
            existing.discard();
        }

        Vec3 playerPos = player.position();
        Vec3 wildPos = wild.position();
        Vec3 dir = wildPos.subtract(playerPos);
        if (dir.horizontalDistanceSqr() < 0.04) {
            dir = player.getLookAngle();
        }
        dir = new Vec3(dir.x, 0, dir.z).normalize();
        if (dir.lengthSqr() < 1.0e-6) {
            dir = new Vec3(0, 0, 1);
        }
        // Perpendicular on the horizontal plane for slight offset
        Vec3 side = new Vec3(-dir.z, 0, dir.x);

        Vec3 mid = playerPos.add(wildPos).scale(0.5);
        // Keep Y on solid footing near the midpoint of the two heights
        double groundY = Math.min(playerPos.y, wildPos.y);

        Vec3 foeSpot = mid.add(dir.scale(HALF_ARENA));
        Vec3 allySpot = mid.add(dir.scale(-HALF_ARENA));
        // Camera stand ≈ center of the fight (third-person pulls back to orbit)
        Vec3 playerSpot = mid
                .add(dir.scale(-CAMERA_BACK))
                .add(side.scale(CAMERA_SIDE))
                .add(0, CAMERA_UP, 0);
        // Look along pure side axis so both mons sit left/right of center
        Vec3 lookTarget = mid.add(side.scale(2.0)).add(0, 0.9, 0);

        // Snap wild to foe spot
        placeAndFreeze(wild, foeSpot.x, groundY, foeSpot.z, false);

        // Spawn / reuse companion at ally spot
        WildMonEntity ally = null;
        if (session.companionEntityId() != null) {
            Entity e = level.getEntity(session.companionEntityId());
            if (e instanceof WildMonEntity wm && !wm.isRemoved()) {
                ally = wm;
            }
        }
        if (ally == null) {
            ally = spawnCompanion(level, player, mon, allySpot.x, groundY, allySpot.z);
            if (ally != null) {
                session.setCompanionEntityId(ally.getUUID());
            }
        } else {
            ally.applyIdentity(mon);
            placeAndFreeze(ally, allySpot.x, groundY, allySpot.z, true);
            ally.setCompanionOwner(player.getUUID());
        }

        float lookYaw = yawToward(playerSpot, lookTarget);
        float lookPitch = pitchToward(playerSpot.add(0, player.getEyeHeight(), 0), lookTarget);
        player.teleportTo(level, playerSpot.x, groundY + CAMERA_UP, playerSpot.z,
                java.util.Set.of(),
                lookYaw,
                lookPitch,
                false);

        faceEachOther(ally, wild);
        if (ally != null) {
            ally.playCry();
        }
        wild.playCry();
    }

    /**
     * Faint the on-field ally (particles + cry), then send out {@code next}
     * so the player literally sees a new mon appear.
     */
    public static void faintAndSwitch(ServerPlayer player, BattleSession session, OwnedMon next) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        WildMonEntity old = entity(level, session.companionEntityId());
        if (old != null) {
            // Death-style burst so the faint is obvious
            double x = old.getX();
            double y = old.getY() + old.getBbHeight() * 0.5;
            double z = old.getZ();
            level.sendParticles(ParticleTypes.POOF, x, y, z, 18, 0.35, 0.4, 0.35, 0.05);
            level.sendParticles(ParticleTypes.CLOUD, x, y, z, 10, 0.25, 0.3, 0.25, 0.02);
            level.playSound(null, x, y, z, SoundEvents.FIRE_EXTINGUISH, SoundSource.NEUTRAL, 0.7f, 1.3f);
            old.playCry();
            old.setHealth(0.1f);
            old.animateHurt(player.getYRot());
            // Brief linger then remove — discard now (client still saw the flash)
            old.discard();
            session.setCompanionEntityId(null);
        }
        switchCompanion(player, session, next);
    }

    /** Replace sent-out mon when switching party slots mid-battle. */
    public static void switchCompanion(ServerPlayer player, BattleSession session, OwnedMon next) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        // Discard old companion if still present
        if (session.companionEntityId() != null) {
            Entity e = level.getEntity(session.companionEntityId());
            if (e != null) {
                e.discard();
            }
            session.setCompanionEntityId(null);
        }
        Entity wildEnt = level.getEntity(session.wildEntityId());
        WildMonEntity wild = wildEnt instanceof WildMonEntity w ? w : null;
        if (wild == null || wild.isRemoved()) {
            // Still try spawn near player
            Vec3 look = player.getLookAngle();
            WildMonEntity ally = spawnCompanion(level, player, next,
                    player.getX() + look.x * 2,
                    player.getY(),
                    player.getZ() + look.z * 2);
            if (ally != null) {
                session.setCompanionEntityId(ally.getUUID());
                playSendOut(level, ally);
            }
            return;
        }
        // Keep wild parked; re-stage ally at the ally spot without re-centering the player
        Vec3 mid = player.position().add(wild.position()).scale(0.5);
        Vec3 dir = wild.position().subtract(player.position());
        dir = new Vec3(dir.x, 0, dir.z);
        if (dir.lengthSqr() < 1.0e-4) {
            dir = player.getLookAngle();
            dir = new Vec3(dir.x, 0, dir.z);
        }
        if (dir.lengthSqr() < 1.0e-6) {
            dir = new Vec3(0, 0, 1);
        }
        dir = dir.normalize();
        double groundY = Math.min(player.getY(), wild.getY());
        Vec3 allySpot = mid.add(dir.scale(-HALF_ARENA));
        WildMonEntity ally = spawnCompanion(level, player, next, allySpot.x, groundY, allySpot.z);
        if (ally != null) {
            session.setCompanionEntityId(ally.getUUID());
            faceEachOther(ally, wild);
            playSendOut(level, ally);
        }
    }

    private static void playSendOut(ServerLevel level, WildMonEntity ally) {
        double x = ally.getX();
        double y = ally.getY() + 0.4;
        double z = ally.getZ();
        level.sendParticles(ParticleTypes.END_ROD, x, y, z, 16, 0.3, 0.5, 0.3, 0.04);
        level.sendParticles(ParticleTypes.FIREWORK, x, y + 0.3, z, 12, 0.25, 0.35, 0.25, 0.05);
        try {
            level.playSound(null, x, y, z,
                    com.cobblemon.mod.sound.ModSounds.pokeBallSendOut(),
                    SoundSource.NEUTRAL, 0.85f, 1.0f);
        } catch (Throwable t) {
            level.playSound(null, x, y, z, SoundEvents.ENDER_EYE_LAUNCH, SoundSource.NEUTRAL, 0.7f, 1.2f);
        }
        ally.playCry();
    }

    /** Keep both mons frozen + facing each other (call lightly each turn sync). */
    public static void holdField(ServerPlayer player, BattleSession session) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        WildMonEntity wild = entity(level, session.wildEntityId());
        WildMonEntity ally = entity(level, session.companionEntityId());
        if (wild != null) {
            freezeInPlace(wild);
        }
        if (ally != null) {
            freezeInPlace(ally);
            // Sync HP bar on entity to battle HP (visual feedback)
            float ratio = session.playerMon().maxHp() <= 0 ? 1f
                    : (float) session.playerMon().hp() / (float) session.playerMon().maxHp();
            float maxH = ally.getMaxHealth();
            ally.setHealth(Math.max(0.1f, maxH * Math.max(0f, ratio)));
        }
        if (wild != null && ally != null) {
            faceEachOther(ally, wild);
            // Wild HP visual
            float wr = session.wildHpRatio();
            float wMax = wild.getMaxHealth();
            wild.setHealth(Math.max(0.1f, wMax * Math.max(0f, wr)));
        }
    }

    /**
     * C3/C4 — move-type particles + SFX at the defender, then standard hit reaction if damage.
     */
    public static void playMoveEffects(
            ServerPlayer player,
            BattleSession session,
            boolean playerUsed,
            String moveId,
            com.cobblemon.mod.species.MonElement element,
            boolean damaging
    ) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        WildMonEntity target = playerUsed
                ? entity(level, session.wildEntityId())
                : entity(level, session.companionEntityId());
        WildMonEntity actor = playerUsed
                ? entity(level, session.companionEntityId())
                : entity(level, session.wildEntityId());
        if (target == null) {
            return;
        }
        double x = target.getX();
        double y = target.getY() + target.getBbHeight() * 0.55;
        double z = target.getZ();
        com.cobblemon.mod.species.MonElement el = element != null
                ? element
                : com.cobblemon.mod.species.MonElement.NORMAL;
        // Type-tinted particles
        switch (el) {
            case FIRE -> {
                level.sendParticles(ParticleTypes.FLAME, x, y, z, 18, 0.4, 0.35, 0.4, 0.02);
                level.sendParticles(ParticleTypes.LAVA, x, y, z, 4, 0.2, 0.2, 0.2, 0.0);
                level.playSound(null, x, y, z, SoundEvents.BLAZE_SHOOT, SoundSource.NEUTRAL, 0.45f, 1.2f);
            }
            case WATER -> {
                level.sendParticles(ParticleTypes.SPLASH, x, y, z, 20, 0.45, 0.35, 0.45, 0.05);
                level.sendParticles(ParticleTypes.BUBBLE, x, y, z, 12, 0.3, 0.3, 0.3, 0.02);
                level.playSound(null, x, y, z, SoundEvents.GENERIC_SPLASH, SoundSource.NEUTRAL, 0.5f, 1.1f);
            }
            case GRASS -> {
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, x, y, z, 14, 0.4, 0.3, 0.4, 0.0);
                level.sendParticles(ParticleTypes.COMPOSTER, x, y, z, 8, 0.3, 0.25, 0.3, 0.0);
                level.playSound(null, x, y, z, SoundEvents.GRASS_BREAK, SoundSource.NEUTRAL, 0.55f, 0.9f);
            }
            case ELECTRIC -> {
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y, z, 22, 0.45, 0.4, 0.45, 0.08);
                level.playSound(null, x, y, z, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.NEUTRAL, 0.35f, 1.6f);
            }
            case ICE -> {
                level.sendParticles(ParticleTypes.SNOWFLAKE, x, y, z, 18, 0.4, 0.35, 0.4, 0.02);
                level.sendParticles(ParticleTypes.ITEM_SNOWBALL, x, y, z, 6, 0.25, 0.25, 0.25, 0.05);
                level.playSound(null, x, y, z, SoundEvents.GLASS_BREAK, SoundSource.NEUTRAL, 0.4f, 1.5f);
            }
            case PSYCHIC, FAIRY -> {
                level.sendParticles(ParticleTypes.ENCHANT, x, y, z, 20, 0.5, 0.4, 0.5, 0.4);
                level.sendParticles(ParticleTypes.END_ROD, x, y, z, 8, 0.3, 0.3, 0.3, 0.02);
                level.playSound(null, x, y, z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 0.6f, 1.3f);
            }
            case GHOST, DARK -> {
                level.sendParticles(ParticleTypes.SMOKE, x, y, z, 16, 0.35, 0.3, 0.35, 0.02);
                level.sendParticles(ParticleTypes.SOUL, x, y, z, 8, 0.25, 0.25, 0.25, 0.01);
                level.playSound(null, x, y, z, SoundEvents.SCULK_CLICKING, SoundSource.NEUTRAL, 0.5f, 0.7f);
            }
            case DRAGON -> {
                level.sendParticles(ParticleTypes.FLAME, x, y, z, 10, 0.4, 0.35, 0.4, 0.02);
                level.sendParticles(ParticleTypes.PORTAL, x, y, z, 14, 0.4, 0.35, 0.4, 0.15);
                level.playSound(null, x, y, z, SoundEvents.ENDER_DRAGON_GROWL, SoundSource.NEUTRAL, 0.25f, 1.4f);
            }
            case GROUND, ROCK -> {
                level.sendParticles(ParticleTypes.POOF, x, y, z, 12, 0.4, 0.15, 0.4, 0.02);
                level.sendParticles(ParticleTypes.CLOUD, x, y, z, 6, 0.3, 0.1, 0.3, 0.01);
                level.playSound(null, x, y, z, SoundEvents.GRAVEL_BREAK, SoundSource.NEUTRAL, 0.6f, 0.8f);
            }
            case FLYING -> {
                level.sendParticles(ParticleTypes.CLOUD, x, y, z, 14, 0.5, 0.25, 0.5, 0.02);
                level.playSound(null, x, y, z, SoundEvents.PHANTOM_FLAP, SoundSource.NEUTRAL, 0.45f, 1.2f);
            }
            case POISON, BUG -> {
                level.sendParticles(ParticleTypes.ITEM_SLIME, x, y, z, 12, 0.35, 0.3, 0.35, 0.05);
                level.playSound(null, x, y, z, SoundEvents.SLIME_SQUISH, SoundSource.NEUTRAL, 0.45f, 1.1f);
            }
            case STEEL -> {
                level.sendParticles(ParticleTypes.CRIT, x, y, z, 16, 0.35, 0.3, 0.35, 0.15);
                level.playSound(null, x, y, z, SoundEvents.ANVIL_LAND, SoundSource.NEUTRAL, 0.3f, 1.5f);
            }
            default -> {
                level.sendParticles(ParticleTypes.CRIT, x, y, z, 10, 0.3, 0.3, 0.3, 0.1);
                level.playSound(null, x, y, z, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.NEUTRAL, 0.4f, 1.0f);
            }
        }
        // Actor cry on status moves / wind-up
        if (actor != null && !damaging) {
            actor.playCry();
        }
        if (damaging) {
            playHitReaction(player, session, playerUsed);
        }
    }

    /**
     * Flash / particle / cry reaction when a mon takes battle damage.
     * @param hitWild true = wild mon was hit; false = player's ally was hit
     */
    public static void playHitReaction(ServerPlayer player, BattleSession session, boolean hitWild) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        WildMonEntity target = hitWild
                ? entity(level, session.wildEntityId())
                : entity(level, session.companionEntityId());
        WildMonEntity source = hitWild
                ? entity(level, session.companionEntityId())
                : entity(level, session.wildEntityId());
        if (target == null) {
            return;
        }

        // Red hurt overlay on clients tracking this entity
        DamageSource dmg = level.damageSources().generic();
        level.broadcastDamageEvent(target, dmg);
        target.animateHurt(source != null ? source.getYRot() : player.getYRot());
        target.hurtTime = 10;
        target.hurtDuration = 10;

        // Entity HP mirrors battle HP so any health-based FX stay correct
        if (hitWild) {
            float wr = session.wildHpRatio();
            target.setHealth(Math.max(0.1f, target.getMaxHealth() * Math.max(0f, wr)));
        } else {
            float ratio = session.playerMon().maxHp() <= 0 ? 1f
                    : (float) session.playerMon().hp() / (float) session.playerMon().maxHp();
            target.setHealth(Math.max(0.1f, target.getMaxHealth() * Math.max(0f, ratio)));
        }

        // Crit-style spark burst + damage ticks
        double x = target.getX();
        double y = target.getY() + target.getBbHeight() * 0.55;
        double z = target.getZ();
        level.sendParticles(ParticleTypes.CRIT, x, y, z, 14, 0.35, 0.4, 0.35, 0.18);
        level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, x, y, z, 6, 0.25, 0.3, 0.25, 0.08);
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);

        // Impact + cry
        level.playSound(null, x, y, z, SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.NEUTRAL, 0.55f, 1.15f);
        level.playSound(null, x, y, z, SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.NEUTRAL, 0.4f, 0.9f);
        target.playCry();

        // Brief knockback away from attacker, then freeze (visual punch)
        if (source != null) {
            Vec3 push = target.position().subtract(source.position());
            if (push.horizontalDistanceSqr() < 1.0e-4) {
                push = source.getLookAngle();
            }
            push = new Vec3(push.x, 0, push.z).normalize().scale(0.28).add(0, 0.08, 0);
            target.setDeltaMovement(push);
            target.hurtMarked = true;
        }
    }

    /** End of battle: unfreeze wild if alive, recall companion. */
    public static void teardown(ServerPlayer player, BattleSession session, boolean wildStillThere) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        if (session.companionEntityId() != null) {
            Entity e = level.getEntity(session.companionEntityId());
            if (e != null) {
                e.discard();
            }
            session.setCompanionEntityId(null);
        }
        if (wildStillThere) {
            WildMonEntity wild = entity(level, session.wildEntityId());
            if (wild != null && !wild.isRemoved()) {
                wild.setNoAi(false);
                // If HP was drained to 0 in battle we already remove; else restore AI
            }
        }
    }

    private static WildMonEntity spawnCompanion(
            ServerLevel level, ServerPlayer player, OwnedMon mon,
            double x, double y, double z
    ) {
        WildMonEntity ally = ModEntities.WILD_MON.get().create(level, EntitySpawnReason.TRIGGERED);
        if (ally == null) {
            return null;
        }
        ally.applyIdentity(mon);
        ally.setCompanionOwner(player.getUUID());
        ally.setCustomName(mon.displayName());
        ally.setCustomNameVisible(true);
        placeAndFreeze(ally, x, y, z, true);
        level.addFreshEntity(ally);
        return ally;
    }

    private static void placeAndFreeze(WildMonEntity mon, double x, double y, double z, boolean companion) {
        mon.teleportTo(x, y, z);
        mon.setDeltaMovement(Vec3.ZERO);
        mon.getNavigation().stop();
        mon.setNoAi(true);
        mon.setYya(0);
        if (companion) {
            // companions already owned
        }
    }

    private static void freezeInPlace(WildMonEntity mon) {
        mon.getNavigation().stop();
        mon.setDeltaMovement(Vec3.ZERO);
        mon.setNoAi(true);
    }

    private static void faceEachOther(WildMonEntity a, WildMonEntity b) {
        if (a == null || b == null) {
            return;
        }
        float yawA = yawToward(a.position(), b.position());
        float yawB = yawToward(b.position(), a.position());
        a.setYRot(yawA);
        a.setYBodyRot(yawA);
        a.setYHeadRot(yawA);
        b.setYRot(yawB);
        b.setYBodyRot(yawB);
        b.setYHeadRot(yawB);
    }

    private static float yawToward(Vec3 from, Vec3 to) {
        double dx = to.x - from.x;
        double dz = to.z - from.z;
        return (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90f;
    }

    private static float pitchToward(Vec3 fromEye, Vec3 to) {
        double dx = to.x - fromEye.x;
        double dy = to.y - fromEye.y;
        double dz = to.z - fromEye.z;
        double horiz = Math.sqrt(dx * dx + dz * dz);
        return (float) (-(Mth.atan2(dy, horiz) * (180.0 / Math.PI)));
    }

    private static WildMonEntity entity(ServerLevel level, UUID id) {
        if (id == null) {
            return null;
        }
        Entity e = level.getEntity(id);
        return e instanceof WildMonEntity w && !w.isRemoved() ? w : null;
    }
}

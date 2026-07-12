package com.cobblemon.mod.client;

import com.cobblemon.mod.Cobblemon;
import com.cobblemon.mod.entity.WildMonEntity;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.CalculateDetachedCameraDistanceEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * Cobblemon-style battle camera: frames the midpoint of both fighters, slowly
 * orbits the field, and keeps the player model out of the shot.
 */
@EventBusSubscriber(modid = Cobblemon.MOD_ID, value = Dist.CLIENT)
public final class BattleCamera {
    /** Base third-person pull-back (blocks) — keeps both mons in frame. */
    private static final float BASE_DISTANCE = 6.4f;
    /** Extra distance pulse so the shot breathes slightly. */
    private static final float DISTANCE_PULSE = 0.55f;
    /** How quickly yaw/pitch ease toward the target. */
    private static final float ANGLE_SMOOTH = 0.18f;
    /** Look slightly above feet so bodies fill the shot. */
    private static final double LOOK_Y_OFFSET = 0.95;
    private static final double SCAN_RANGE = 28.0;

    /** Full orbit period in ticks (~50s for a full 360°). */
    private static final float ORBIT_PERIOD_TICKS = 50f * 20f;
    /** Max swing from the side-view base angle (degrees). */
    private static final float ORBIT_YAW_AMP = 28f;
    /** Gentle pitch bob (degrees). */
    private static final float ORBIT_PITCH_AMP = 3.5f;

    private static CameraType savedCameraType;
    private static boolean controlling;
    private static float smoothYaw;
    private static float smoothPitch;
    private static boolean smoothInitialized;
    private static int orbitTicks;
    /** Side-view base yaw derived from ally→foe axis (degrees). */
    private static float baseSideYaw;
    private static boolean hasBaseSideYaw;

    private BattleCamera() {}

    public static boolean isActive() {
        return ClientBattle.isOpen();
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            releaseControl(mc);
            return;
        }

        if (!isActive()) {
            releaseControl(mc);
            return;
        }

        if (!controlling) {
            savedCameraType = mc.options.getCameraType();
            controlling = true;
            smoothInitialized = false;
            hasBaseSideYaw = false;
            orbitTicks = 0;
            BattleMusic.start();
        }
        BattleMusic.ensurePlaying();

        if (mc.options.getCameraType().isFirstPerson()
                || mc.options.getCameraType().isMirrored()) {
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        }

        orbitTicks++;
        Frame frame = findFrame(mc, player);
        if (frame == null) {
            return;
        }

        // Keep player body aligned so third-person base stays coherent
        player.setYRot(frame.yaw);
        player.setXRot(frame.pitch);
        player.yRotO = frame.yaw;
        player.xRotO = frame.pitch;
        player.yHeadRot = frame.yaw;
        player.yHeadRotO = frame.yaw;
        player.yBodyRot = frame.yaw;
        player.yBodyRotO = frame.yaw;
    }

    @SubscribeEvent
    static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (!isActive() || !controlling) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }

        Frame frame = findFrame(mc, player);
        if (frame == null) {
            return;
        }

        if (!smoothInitialized) {
            smoothYaw = frame.yaw;
            smoothPitch = frame.pitch;
            smoothInitialized = true;
        } else {
            float dy = Mth.wrapDegrees(frame.yaw - smoothYaw);
            smoothYaw = smoothYaw + dy * ANGLE_SMOOTH;
            smoothPitch = Mth.lerp(ANGLE_SMOOTH, smoothPitch, frame.pitch);
        }

        event.setYaw(smoothYaw);
        event.setPitch(smoothPitch);
        event.setRoll(0f);
    }

    @SubscribeEvent
    static void onDetachedDistance(CalculateDetachedCameraDistanceEvent event) {
        if (!isActive() || !controlling) {
            return;
        }
        float pulse = (float) Math.sin(orbitTicks / 90.0) * DISTANCE_PULSE;
        event.setDistance(Math.max(event.getDistance(), BASE_DISTANCE + pulse));
    }

    @SubscribeEvent
    static void onRenderHand(RenderHandEvent event) {
        if (isActive()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void onRenderPlayer(RenderPlayerEvent.Pre<?> event) {
        if (!isActive()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && event.getRenderState().id == mc.player.getId()) {
            event.setCanceled(true);
        }
    }

    private static void releaseControl(Minecraft mc) {
        if (!controlling) {
            return;
        }
        BattleMusic.stop();
        if (mc != null && mc.options != null && savedCameraType != null) {
            mc.options.setCameraType(savedCameraType);
        }
        savedCameraType = null;
        controlling = false;
        smoothInitialized = false;
        hasBaseSideYaw = false;
        orbitTicks = 0;
    }

    /**
     * Look-at = midpoint of ally + wild (true center of the fight).
     * Yaw orbits slowly around a side-view base so both mons stay framed.
     */
    private static Frame findFrame(Minecraft mc, LocalPlayer player) {
        if (mc.level == null) {
            return null;
        }

        WildMonEntity ally = null;
        WildMonEntity wild = null;
        double bestWildDist = Double.MAX_VALUE;

        AABB box = player.getBoundingBox().inflate(SCAN_RANGE);
        for (Entity e : mc.level.getEntities(player, box, ent -> ent instanceof WildMonEntity)) {
            if (!(e instanceof WildMonEntity mon) || mon.isRemoved()) {
                continue;
            }
            if (mon.isCompanion()) {
                if (ally == null || mon.distanceToSqr(player) < ally.distanceToSqr(player)) {
                    ally = mon;
                }
            } else {
                double d = mon.distanceToSqr(player);
                if (d < bestWildDist) {
                    bestWildDist = d;
                    wild = mon;
                }
            }
        }

        Vec3 lookAt;
        if (ally != null && wild != null) {
            lookAt = ally.position().add(wild.position()).scale(0.5)
                    .add(0, LOOK_Y_OFFSET, 0);
            // Side-view base: camera looks along the axis perpendicular to ally→foe
            Vec3 axis = wild.position().subtract(ally.position());
            axis = new Vec3(axis.x, 0, axis.z);
            if (axis.lengthSqr() > 1.0e-4) {
                // Perpendicular horizontal = pure side shot of both mons
                Vec3 side = new Vec3(-axis.z, 0, axis.x).normalize();
                float sideYaw = (float) (Mth.atan2(side.z, side.x) * (180.0 / Math.PI)) - 90f;
                if (!hasBaseSideYaw) {
                    baseSideYaw = sideYaw;
                    hasBaseSideYaw = true;
                } else {
                    // Ease base if arena axis shifts slightly
                    float dBase = Mth.wrapDegrees(sideYaw - baseSideYaw);
                    baseSideYaw += dBase * 0.05f;
                }
            }
        } else if (wild != null) {
            lookAt = wild.position().add(0, LOOK_Y_OFFSET, 0);
        } else if (ally != null) {
            lookAt = ally.position().add(0, LOOK_Y_OFFSET, 0);
        } else {
            return new Frame(player.getYRot(), player.getXRot());
        }

        // Slow orbit around the side-view base (centered on the fight)
        float t = orbitTicks / ORBIT_PERIOD_TICKS * ((float) Math.PI * 2f);
        float orbitYaw = (hasBaseSideYaw ? baseSideYaw : player.getYRot())
                + (float) Math.sin(t) * ORBIT_YAW_AMP;
        float orbitPitch = 8f + (float) Math.sin(t * 0.7) * ORBIT_PITCH_AMP;

        // Blend pure side orbit with a slight look-at correction so mid stays centered
        Vec3 from = player.getEyePosition(1f);
        double dx = lookAt.x - from.x;
        double dy = lookAt.y - from.y;
        double dz = lookAt.z - from.z;
        double horiz = Math.sqrt(dx * dx + dz * dz);
        float lookYaw = (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90f;
        float lookPitch = (float) (-(Mth.atan2(dy, horiz) * (180.0 / Math.PI)));

        // Prefer orbit base, nudge toward look-at so both mons stay in the sweet spot
        float yaw = orbitYaw + Mth.wrapDegrees(lookYaw - orbitYaw) * 0.35f;
        float pitch = Mth.clamp(Mth.lerp(0.55f, orbitPitch, lookPitch + 4f), -28f, 22f);

        return new Frame(yaw, pitch);
    }

    private record Frame(float yaw, float pitch) {}
}

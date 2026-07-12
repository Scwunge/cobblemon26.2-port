package com.cobblemon.mod.ride;

import com.cobblemon.mod.entity.WildMonEntity;
import com.cobblemon.mod.species.MonElement;
import com.cobblemon.mod.species.SpeciesHandle;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Ride rules for companions — seat profile, fly/swim modes, and stat-based buffs.
 * <p>
 * Pokémon-style heuristics (not full official tables):
 * <ul>
 *   <li>Rideable only when large enough ({@link SpeciesHandle#isRideable()}).</li>
 *   <li>Flying-type (or large Dragon) → air mount.</li>
 *   <li>Water-type → boat/dolphin in water.</li>
 *   <li>High base Speed → land speed + jump boost.</li>
 * </ul>
 */
public final class RideController {
    /**
     * When true, the next dismount is intentional (V key / command) and must not be cancelled.
     * Vanilla uses Shift = dismount; we repurpose Shift for dive/descend on mon mounts.
     */
    private static final ThreadLocal<Boolean> ALLOW_DISMOUNT = ThreadLocal.withInitial(() -> false);

    private RideController() {}

    /** Run an intentional stopRiding (V key) without EntityMountEvent cancelling it. */
    public static void intentionalDismount(Player player) {
        if (player == null) {
            return;
        }
        ALLOW_DISMOUNT.set(true);
        try {
            player.stopRiding();
        } finally {
            ALLOW_DISMOUNT.set(false);
        }
    }

    public static boolean isIntentionalDismount() {
        return Boolean.TRUE.equals(ALLOW_DISMOUNT.get());
    }

    /**
     * Pick a ride_settings profile for this mon.
     */
    public static RideSettingsLoader.RideSettings resolve(WildMonEntity mon) {
        if (mon == null) {
            return RideSettingsLoader.defaultLand();
        }
        SpeciesHandle handle = SpeciesHandle.of(mon.getSpeciesId());
        if (isAirMount(handle)) {
            // Large flyers prefer bird; hover for dragon-heavy
            if (handle.primaryType() == MonElement.DRAGON
                    && handle.secondaryType().orElse(null) != MonElement.FLYING) {
                return RideSettingsLoader.getOrDefault("hover");
            }
            return RideSettingsLoader.getOrDefault("bird");
        }
        if (isWaterMount(handle)) {
            // Heavier water mons → boat; agile ones → dolphin
            if (handle.weightHg() >= 900 || handle.heightDm() >= 20) {
                return RideSettingsLoader.getOrDefault("boat");
            }
            return RideSettingsLoader.getOrDefault("dolphin");
        }
        return RideSettingsLoader.getOrDefault("horse");
    }

    /** Flying-type, or large Dragon that can take air. */
    public static boolean isAirMount(SpeciesHandle handle) {
        if (handle == null) {
            return false;
        }
        if (!handle.isRideable()) {
            return false;
        }
        if (handle.primaryType() == MonElement.FLYING
                || handle.secondaryType().orElse(null) == MonElement.FLYING) {
            return true;
        }
        // Large dragons / levitating-style (height ≥ 1.5m)
        if (handle.primaryType() == MonElement.DRAGON && handle.heightDm() >= 15) {
            return true;
        }
        return false;
    }

    public static boolean isWaterMount(SpeciesHandle handle) {
        if (handle == null || !handle.isRideable()) {
            return false;
        }
        return handle.primaryType() == MonElement.WATER
                || handle.secondaryType().orElse(null) == MonElement.WATER;
    }

    /**
     * Apply movement / jump after a successful mount.
     */
    public static RideSettingsLoader.RideSettings applyMount(WildMonEntity mon, Player rider) {
        RideSettingsLoader.RideSettings settings = resolve(mon);
        SpeciesHandle handle = SpeciesHandle.of(mon.getSpeciesId());
        float agility = agilityMult(handle);
        float bulk = bulkMult(handle);

        var speed = mon.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            float base = settings.movementSpeed(mon.getMonLevel(), speciesSpeedBase(mon));
            // Large + agile rideables get a modest land/air speed bump
            base *= agility * bulk;
            if (settings.canFly()) {
                base = Math.max(base, 0.38f); // ensure air mounts feel responsive
            }
            speed.setBaseValue(Math.min(0.75f, base));
        }

        // Land jump buff for agile rideables
        var jump = mon.getAttribute(Attributes.JUMP_STRENGTH);
        if (jump != null) {
            double j = 0.42; // vanilla-ish
            if (!settings.canFly()) {
                j *= jumpMult(handle);
            } else {
                j = 0.35; // less ground hop when primarily a flyer
            }
            jump.setBaseValue(j);
        }

        // Step height for large mounts
        var step = mon.getAttribute(Attributes.STEP_HEIGHT);
        if (step != null) {
            step.setBaseValue(handle.heightDm() >= 15 ? 1.2 : 0.8);
        }

        // Stay grounded until the player takes off (Space). Auto noGravity made Charizard fly on mount.
        mon.setNoGravity(false);
        mon.setRidingFlight(false);
        return settings;
    }

    /**
     * Hold <b>Space</b> to fly — release Space to fall / land.
     * <b>Shift</b> dives (cancels lift even while Space is held).
     */
    public static void updateFlightState(WildMonEntity mon, Player player) {
        if (mon == null || player == null) {
            return;
        }
        if (!canFly(mon)) {
            if (mon.isRidingFlight()) {
                mon.setRidingFlight(false);
                mon.setNoGravity(false);
            }
            return;
        }
        boolean diving = player.isShiftKeyDown();
        // Stay in flight mode while Space OR while diving airborne (so Shift can pull you down)
        // Flight while Space held, or Shift-diving while airborne (controlled descent)
        if (player.isJumping()) {
            mon.setRidingFlight(true);
        } else if (diving && !mon.onGround()) {
            mon.setRidingFlight(true);
        } else {
            mon.setRidingFlight(false);
        }

        if (mon.isRidingFlight()) {
            mon.setNoGravity(true);
            mon.fallDistance = 0;
            player.fallDistance = 0;
            Vec3 v = mon.getDeltaMovement();
            if (diving) {
                // Shift dive — push down, no lift
                mon.setDeltaMovement(v.x, Math.max(-0.55, v.y - 0.12), v.z);
                mon.hurtMarked = true;
            } else if (player.isJumping()) {
                // Gentle sustained lift only while Space (not diving)
                double lift = mon.onGround() ? 0.18 : 0.06;
                if (v.y < lift) {
                    mon.setDeltaMovement(v.x, Math.min(lift, v.y + 0.05), v.z);
                    mon.hurtMarked = true;
                }
            }
        } else {
            mon.setNoGravity(false);
            if (mon.onGround()) {
                mon.setDeltaMovement(mon.getDeltaMovement().multiply(1.0, 0.0, 1.0));
            } else if (diving) {
                // Fast fall when not in flight mode
                Vec3 v = mon.getDeltaMovement();
                mon.setDeltaMovement(v.x, Math.max(-0.7, v.y - 0.08), v.z);
            }
        }
    }

    /** Species base Speed stat (defaults to 50). */
    public static int speciesSpeedBase(WildMonEntity mon) {
        if (mon == null) {
            return 50;
        }
        try {
            var stats = SpeciesHandle.of(mon.getSpeciesId()).baseStats();
            if (stats != null) {
                return Math.max(1, stats.spe());
            }
        } catch (Throwable ignored) {
        }
        return 50;
    }

    /**
     * Agility factor from base Speed: 40 → ~0.95, 70 → ~1.08, 100 → ~1.18, 130 → ~1.28.
     */
    public static float agilityMult(SpeciesHandle handle) {
        int spe = 50;
        try {
            if (handle != null && handle.baseStats() != null) {
                spe = handle.baseStats().spe();
            }
        } catch (Throwable ignored) {
        }
        spe = Mth.clamp(spe, 20, 160);
        return 0.88f + (spe / 100f) * 0.32f;
    }

    /**
     * Slight bulk bonus for larger rideables (height/weight) — not huge.
     */
    public static float bulkMult(SpeciesHandle handle) {
        if (handle == null || !handle.isRideable()) {
            return 1.0f;
        }
        int h = handle.heightDm();
        int w = handle.weightHg();
        float fromH = h >= 20 ? 1.08f : (h >= 15 ? 1.05f : 1.02f);
        float fromW = w >= 1000 ? 1.04f : 1.0f;
        return fromH * fromW;
    }

    /** Jump mult for land mounts from Speed + size. */
    public static float jumpMult(SpeciesHandle handle) {
        float a = agilityMult(handle);
        // Cap so we don't moon-jump
        return Mth.clamp(0.95f + (a - 1.0f) * 1.4f, 0.95f, 1.45f);
    }

    public static void onDismount(WildMonEntity mon) {
        if (mon == null) {
            return;
        }
        mon.setRidingFlight(false);
        mon.setNoGravity(false);
        mon.setDeltaMovement(mon.getDeltaMovement().multiply(1.0, 0.35, 1.0));
        // Restore sane defaults so AI walk speed works again
        try {
            var speed = mon.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed != null) {
                // Will be overwritten by next refreshStats / tick if needed
                speed.setBaseValue(0.28);
            }
            var jump = mon.getAttribute(Attributes.JUMP_STRENGTH);
            if (jump != null) {
                jump.setBaseValue(0.42);
            }
        } catch (Throwable ignored) {
        }
    }

    public static boolean canFly(WildMonEntity mon) {
        if (mon == null) {
            return false;
        }
        SpeciesHandle h = SpeciesHandle.of(mon.getSpeciesId());
        return isAirMount(h) || resolve(mon).canFly();
    }

    public static boolean canSwimMount(WildMonEntity mon) {
        if (mon == null) {
            return false;
        }
        return isWaterMount(SpeciesHandle.of(mon.getSpeciesId()));
    }

    public static float speedFor(WildMonEntity mon) {
        int level = mon == null ? 5 : mon.getMonLevel();
        SpeciesHandle h = mon == null ? null : SpeciesHandle.of(mon.getSpeciesId());
        return resolve(mon).movementSpeed(level, speciesSpeedBase(mon))
                * agilityMult(h)
                * bulkMult(h);
    }

    /**
     * Local-space passenger seat: on the back/top of the mon, not inside the hitbox.
     * Y is relative to entity feet; scales with visual/entity height.
     */
    public static Vec3 passengerSeatOffset(WildMonEntity mon) {
        if (mon == null) {
            return new Vec3(0, 1.0, 0);
        }
        SpeciesHandle h = SpeciesHandle.of(mon.getSpeciesId());
        float bbH = mon.getBbHeight();
        float bbW = mon.getBbWidth();
        // Sit near the top of the collision box, slightly back along local Z
        double y = Math.max(0.85, bbH * 0.92);
        // Extra lift for very tall species (Charizard ~1.7m+)
        if (h.heightDm() >= 15) {
            y = Math.max(y, bbH * 0.95 + 0.15);
        }
        if (h.heightDm() >= 20) {
            y = Math.max(y, bbH * 0.98 + 0.25);
        }
        // Slight rearward seat so the camera clears the head
        double z = -bbW * 0.12;
        return new Vec3(0.0, y, z);
    }

    /**
     * Ridden strafe/forward/vertical.
     * <p>
     * Flight: 3D vertical after takeoff. Water mounts: <b>horizontal only</b> (boat-style);
     * dive/surface is handled in {@link #travelAsBoat}, not free no-gravity swim.
     */
    public static Vec3 riddenInput(WildMonEntity mon, Player player, float strafe, float forward) {
        float f = strafe * 0.45f;
        float f1 = forward;
        if (f1 <= 0.0f) {
            f1 *= 0.25f;
        }
        double y = 0.0;
        boolean inFlight = mon != null && mon.isRidingFlight() && canFly(mon);
        boolean waterMount = mon != null && canSwimMount(mon) && mon.isInWater();

        if (inFlight) {
            mon.setNoGravity(true);
            if (player.isShiftKeyDown()) {
                // Strong dive signal (applied separately in travelFlying so normalize won't kill it)
                y = -0.85;
            } else {
                y += 0.10; // hold altitude while Space
                if (f1 > 0.05f) {
                    y += (-player.getXRot() / 90.0) * 0.30;
                }
            }
        } else if (waterMount) {
            mon.setNoGravity(false);
            // Pass dive intent via Y for boat travel
            if (player.isShiftKeyDown()) {
                y = -1.0;
            } else if (player.isJumping()) {
                y = 0.5;
            }
        } else {
            mon.setNoGravity(false);
        }
        return new Vec3(f, y, f1);
    }

    public static float riddenSpeedMult(WildMonEntity mon) {
        float mult = 1.0f;
        if (mon != null && mon.isRidingFlight() && canFly(mon)) {
            mult = 1.25f;
        } else if (canSwimMount(mon) && mon.isInWater()) {
            mult = 1.0f; // boat travel uses its own fixed cruise speed
        }
        SpeciesHandle h = mon == null ? null : SpeciesHandle.of(mon.getSpeciesId());
        // Don't apply full agility mult in water — boats don't get Spe-stat rockets
        if (canSwimMount(mon) && mon != null && mon.isInWater()) {
            return mult * (0.92f + (agilityMult(h) - 1.0f) * 0.35f);
        }
        return mult * agilityMult(h);
    }

    /** Public ride speed (avoids protected LivingEntity.getRiddenSpeed). */
    public static float rideSpeed(WildMonEntity mon, Player player) {
        if (mon == null) {
            return 0.3f;
        }
        float base = (float) mon.getAttributeValue(Attributes.MOVEMENT_SPEED) * 0.95f;
        return base * riddenSpeedMult(mon);
    }

    /**
     * Custom 3D flight — horizontal from look; vertical applied <b>separately</b>
     * so Shift dive isn't wiped by normalize() with forward speed.
     */
    public static void travelFlying(WildMonEntity mon, Player player, Vec3 input) {
        mon.setNoGravity(true);
        mon.fallDistance = 0;
        player.fallDistance = 0;

        float speed = Math.min(0.42f, rideSpeed(mon, player) * 1.1f);
        double forward = input.z;
        double strafe = input.x;
        double vertical = input.y; // includes strong -0.85 when Shift

        float yaw = player.getYRot() * Mth.DEG_TO_RAD;
        float pitch = player.getXRot() * Mth.DEG_TO_RAD;

        double lookX = -Mth.sin(yaw) * Mth.cos(pitch);
        double lookY = -Mth.sin(pitch);
        double lookZ = Mth.cos(yaw) * Mth.cos(pitch);
        double rightX = Mth.cos(yaw);
        double rightZ = Mth.sin(yaw);

        // Horizontal only (normalize without eating dive)
        Vec3 horiz = new Vec3(lookX * forward + rightX * strafe, 0, lookZ * forward + rightZ * strafe);
        if (horiz.lengthSqr() > 1.0e-6) {
            horiz = horiz.normalize().scale(speed);
        }

        // Vertical: dive / climb as its own channel
        double vyTarget;
        if (player.isShiftKeyDown()) {
            vyTarget = -0.50; // clear dive
        } else {
            vyTarget = vertical * 0.35 + lookY * Math.max(0, forward) * speed * 0.5;
            vyTarget = Mth.clamp(vyTarget, -0.35, 0.22);
        }

        Vec3 cur = mon.getDeltaMovement();
        double vx = cur.x * 0.65 + horiz.x * 0.35;
        double vz = cur.z * 0.65 + horiz.z * 0.35;
        double vy = cur.y * 0.55 + vyTarget * 0.45;
        if (player.isShiftKeyDown()) {
            vy = Math.min(vy, -0.35); // guarantee downward while Shift
        }

        mon.setDeltaMovement(vx, vy, vz);
        mon.move(net.minecraft.world.entity.MoverType.SELF, mon.getDeltaMovement());
        mon.setDeltaMovement(mon.getDeltaMovement().scale(0.94));
    }

    /**
     * Water mounts: boat on the surface by default; <b>hold Shift to dive</b> (submerge),
     * release Shift to float back up. Not free 3D fly-swim.
     */
    public static void travelAsBoat(WildMonEntity mon, Player player, Vec3 input) {
        mon.fallDistance = 0;
        player.fallDistance = 0;

        boolean diving = player.isShiftKeyDown() || input.y < -0.5;
        // While diving, turn off gravity fight so we can go under; surface uses normal float
        mon.setNoGravity(diving);

        float cruise = diving ? 0.07f : 0.085f;
        cruise *= Mth.clamp(0.9f + (speciesSpeedBase(mon) - 50) * 0.002f, 0.85f, 1.12f);

        float yaw = player.getYRot() * Mth.DEG_TO_RAD;
        double forward = input.z;
        double strafe = input.x * 0.35;

        double lookX = -Mth.sin(yaw);
        double lookZ = Mth.cos(yaw);
        double rightX = Mth.cos(yaw);
        double rightZ = Mth.sin(yaw);

        double ax = (lookX * forward + rightX * strafe) * cruise;
        double az = (lookZ * forward + rightZ * strafe) * cruise;

        Vec3 cur = mon.getDeltaMovement();
        double vx = cur.x * 0.9 + ax;
        double vz = cur.z * 0.9 + az;
        double vy = cur.y;

        if (mon.isInWater() || mon.isUnderWater()) {
            if (diving) {
                // Hold Shift → sink under the surface (actual dive)
                vy = Math.min(vy, -0.18);
                vy = Math.max(-0.35, vy - 0.10);
            } else if (mon.isUnderWater()) {
                // Release Shift → float back to surface
                vy = Math.min(0.16, vy + 0.07);
            } else {
                // On surface — damp bob; Space = small hop
                vy *= 0.5;
                if (player.isJumping() || input.y > 0.25) {
                    vy = Math.min(0.16, vy + 0.05);
                }
            }
        } else {
            mon.setNoGravity(false);
            vy -= 0.04;
        }

        double horiz = Math.sqrt(vx * vx + vz * vz);
        double maxH = diving ? 0.18 : 0.22;
        if (horiz > maxH) {
            double s = maxH / horiz;
            vx *= s;
            vz *= s;
        }

        mon.setDeltaMovement(vx, vy, vz);
        mon.move(net.minecraft.world.entity.MoverType.SELF, mon.getDeltaMovement());
        if (Math.abs(forward) < 0.01 && Math.abs(strafe) < 0.01) {
            mon.setDeltaMovement(mon.getDeltaMovement().multiply(0.85, diving ? 0.98 : 0.95, 0.85));
        } else {
            mon.setDeltaMovement(mon.getDeltaMovement().multiply(0.96, 0.98, 0.96));
        }
    }

    /** @deprecated use {@link #travelAsBoat} */
    public static void travelSwimming(WildMonEntity mon, Player player, Vec3 input) {
        travelAsBoat(mon, player, input);
    }
}

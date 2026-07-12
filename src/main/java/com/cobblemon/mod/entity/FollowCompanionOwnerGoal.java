package com.cobblemon.mod.entity;

import java.util.EnumSet;
import java.util.UUID;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

/**
 * Simple follow AI for sent-out companions (no TamableAnimal required).
 */
public class FollowCompanionOwnerGoal extends Goal {
    private final WildMonEntity mon;
    private final double speed;
    private final float startDistance;
    private final float stopDistance;
    private int recalc;

    public FollowCompanionOwnerGoal(WildMonEntity mon, double speed, float startDistance, float stopDistance) {
        this.mon = mon;
        this.speed = speed;
        this.startDistance = startDistance;
        this.stopDistance = stopDistance;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        UUID ownerId = mon.getCompanionOwner();
        if (ownerId == null) {
            return false;
        }
        Player owner = mon.level().getPlayerByUUID(ownerId);
        if (owner == null || owner.isSpectator()) {
            return false;
        }
        return mon.distanceToSqr(owner) > (double) (startDistance * startDistance);
    }

    @Override
    public boolean canContinueToUse() {
        UUID ownerId = mon.getCompanionOwner();
        if (ownerId == null) {
            return false;
        }
        Player owner = mon.level().getPlayerByUUID(ownerId);
        if (owner == null) {
            return false;
        }
        return mon.distanceToSqr(owner) > (double) (stopDistance * stopDistance);
    }

    @Override
    public void start() {
        this.recalc = 0;
    }

    @Override
    public void stop() {
        mon.getNavigation().stop();
    }

    @Override
    public void tick() {
        UUID ownerId = mon.getCompanionOwner();
        if (ownerId == null) {
            return;
        }
        Player owner = mon.level().getPlayerByUUID(ownerId);
        if (owner == null) {
            return;
        }
        mon.getLookControl().setLookAt(owner, 10.0F, mon.getMaxHeadXRot());
        if (--this.recalc <= 0) {
            this.recalc = 10;
            mon.getNavigation().moveTo(owner, this.speed);
        }
        // Teleport if too far
        if (mon.distanceToSqr(owner) > 24 * 24) {
            mon.teleportTo(owner.getX(), owner.getY(), owner.getZ());
        }
    }
}

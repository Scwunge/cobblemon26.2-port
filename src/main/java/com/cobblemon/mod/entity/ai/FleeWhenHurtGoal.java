package com.cobblemon.mod.entity.ai;

import java.util.EnumSet;

import com.cobblemon.mod.entity.WildMonEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

/**
 * Wild mons with low HP try to flee the attacker (Run Away / natural survival).
 */
public class FleeWhenHurtGoal extends Goal {
    private final WildMonEntity mon;
    private final double speed;
    private LivingEntity avoid;
    private int cooldown;

    public FleeWhenHurtGoal(WildMonEntity mon, double speed) {
        this.mon = mon;
        this.speed = speed;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (mon.isCompanion()) {
            return false;
        }
        if (cooldown > 0) {
            cooldown--;
            return false;
        }
        if (mon.getHealth() / mon.getMaxHealth() > 0.35f) {
            return false;
        }
        LivingEntity last = mon.getLastHurtByMob();
        if (last == null || !last.isAlive()) {
            return false;
        }
        this.avoid = last;
        return true;
    }

    @Override
    public void start() {
        Vec3 away = DefaultRandomPos.getPosAway(mon, 12, 5, avoid.position());
        if (away != null) {
            mon.getNavigation().moveTo(away.x, away.y, away.z, speed);
        }
        cooldown = 40;
    }

    @Override
    public boolean canContinueToUse() {
        return !mon.getNavigation().isDone() && avoid != null && avoid.isAlive()
                && mon.getHealth() / mon.getMaxHealth() <= 0.4f;
    }
}

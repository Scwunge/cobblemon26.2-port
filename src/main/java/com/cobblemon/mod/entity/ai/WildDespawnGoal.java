package com.cobblemon.mod.entity.ai;

import com.cobblemon.mod.entity.WildMonEntity;
import com.cobblemon.mod.species.SpawnRules;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

/**
 * Despawns idle wild mons after species-based timeout if no player is nearby.
 */
public class WildDespawnGoal extends Goal {
    private final WildMonEntity mon;
    private long ticksAlive;

    public WildDespawnGoal(WildMonEntity mon) {
        this.mon = mon;
    }

    @Override
    public boolean canUse() {
        if (mon.isCompanion() || mon.level().isClientSide()) {
            return false;
        }
        ticksAlive++;
        if (!SpawnRules.canDespawnWild(false, ticksAlive, mon.getSpecies())) {
            return false;
        }
        // Don't despawn near any player (32 blocks); natural spawns need time to be found
        Player nearest = mon.level().getNearestPlayer(mon, 48.0);
        return nearest == null;
    }

    @Override
    public void start() {
        mon.discard();
    }

    @Override
    public boolean canContinueToUse() {
        return false;
    }
}

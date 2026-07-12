package com.cobblemon.mod.item;

import com.cobblemon.mod.species.MonElement;
import com.cobblemon.mod.species.MonGender;
import com.cobblemon.mod.species.OwnedMon;
import com.cobblemon.mod.species.SpeciesHandle;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;

/**
 * Context for ball-specific catch multipliers (Dusk, Dive, Nest, Quick, …).
 */
public record BallContext(
        float hpRatio,
        String speciesId,
        int wildLevel,
        int playerLevel,
        MonGender wildGender,
        MonGender playerGender,
        boolean inWater,
        boolean isDark,
        boolean isNight,
        int battleTurns,
        boolean alreadyCaughtSpecies,
        float wildSpeedApprox
) {
    public static BallContext ofWild(
            ServerPlayer player,
            String speciesId,
            int wildLevel,
            float hpRatio,
            int battleTurns,
            boolean alreadyCaught
    ) {
        Level level = player.level();
        BlockPos pos = player.blockPosition();
        boolean dark = level.getBrightness(LightLayer.BLOCK, pos) <= 7
                && level.getBrightness(LightLayer.SKY, pos) <= 7;
        long day = level.getOverworldClockTime() % 24000L;
        boolean night = day >= 13000L && day < 23000L;
        boolean water = !level.getFluidState(pos).isEmpty()
                || !level.getFluidState(pos.below()).isEmpty();
        int pLevel = 5;
        try {
            var party = com.cobblemon.mod.party.PartyHelper.get(player);
            if (!party.isEmpty()) {
                pLevel = party.lead().map(OwnedMon::level).orElse(5);
            }
        } catch (Exception ignored) {
        }
        SpeciesHandle h = SpeciesHandle.of(speciesId);
        float spe = h.baseStats().spe();
        return new BallContext(
                hpRatio, speciesId, wildLevel, pLevel,
                MonGender.GENDERLESS, MonGender.GENDERLESS,
                water, dark, night, battleTurns, alreadyCaught, spe
        );
    }

    /**
     * Effective catch multiplier for a ball given context (replaces flat enum mult when &gt; 0).
     */
    public float multiplierFor(CubeBallTier tier) {
        if (tier == null) {
            return 1f;
        }
        float base = tier.catchMultiplier();
        if (base >= 100f) {
            return base;
        }
        SpeciesHandle handle = SpeciesHandle.of(speciesId);
        MonElement type = handle.primaryType();
        MonElement type2 = handle.secondaryType().orElse(null);

        return switch (tier) {
            case GREAT -> 1.5f;
            case ULTRA, ANCIENT_ULTRA -> 2.0f;
            case NET -> (type == MonElement.BUG || type == MonElement.WATER
                    || type2 == MonElement.BUG || type2 == MonElement.WATER) ? 3.5f : 1.0f;
            case DIVE, LURE -> inWater ? 3.5f : 1.0f;
            case NEST -> {
                // Stronger on low-level wilds
                if (wildLevel <= 20) {
                    yield Math.min(4.0f, 3.0f + (20 - wildLevel) * 0.1f);
                }
                yield 1.0f;
            }
            case REPEAT -> alreadyCaughtSpecies ? 3.5f : 1.0f;
            case TIMER -> Math.min(4.0f, 1.0f + battleTurns * 0.3f);
            case QUICK -> battleTurns <= 1 ? 5.0f : (battleTurns <= 2 ? 3.0f : 1.0f);
            case DUSK -> (isDark || isNight) ? 3.5f : 1.0f;
            case FAST -> wildSpeedApprox >= 100 ? 4.0f : 1.0f;
            case LEVEL -> {
                int diff = playerLevel - wildLevel;
                if (diff >= 20) yield 4.0f;
                if (diff >= 10) yield 2.5f;
                if (diff >= 5) yield 2.0f;
                yield 1.0f;
            }
            case LOVE -> (wildGender != MonGender.GENDERLESS
                    && playerGender != MonGender.GENDERLESS
                    && wildGender != playerGender) ? 8.0f : 1.0f;
            case MOON -> {
                String id = speciesId.toLowerCase();
                if (id.contains("nidor") || id.contains("cleffa") || id.contains("clefairy")
                        || id.contains("jiggly") || id.contains("iggly") || id.contains("skitty")
                        || id.contains("munna") || id.contains("cresselia")) {
                    yield 4.0f;
                }
                yield 1.0f;
            }
            case HEAVY, ANCIENT_HEAVY, ANCIENT_GIGATON, ANCIENT_LEADEN -> {
                // Heavier base weight → better (approx via base HP as proxy)
                int hp = handle.baseStats().hp();
                if (hp >= 100) yield 2.5f;
                if (hp >= 70) yield 1.5f;
                yield 1.0f;
            }
            case BEAST -> {
                boolean target = false;
                var data = com.cobblemon.mod.species.SpeciesRegistry.get(handle.id()).orElse(null);
                if (data != null && data.labels() != null) {
                    for (String l : data.labels()) {
                        String s = l.toLowerCase();
                        if (s.contains("ultra_beast") || s.contains("paradox") || s.contains("beast")) {
                            target = true;
                            break;
                        }
                    }
                }
                yield target ? 5.0f : 0.1f;
            }
            case SAFARI, SPORT -> 1.5f;
            case DREAM -> 4.0f; // simplified strong catch
            case HEAL, FRIEND, LUXURY, PREMIER, CHERISH,
                 AZURE, CITRINE, ROSEATE, SLATE, VERDANT,
                 ANCIENT_POKE, ANCIENT_FEATHER, ANCIENT_AZURE, ANCIENT_CITRINE,
                 ANCIENT_ROSEATE, ANCIENT_SLATE, ANCIENT_VERDANT, ANCIENT_IVORY,
                 ANCIENT_GREAT, ANCIENT_WING, ANCIENT_JET, POKE -> base;
            case MASTER, PARK, ANCIENT_ORIGIN -> 255f;
        };
    }
}

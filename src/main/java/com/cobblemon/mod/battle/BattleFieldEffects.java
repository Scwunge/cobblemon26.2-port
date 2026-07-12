package com.cobblemon.mod.battle;

import java.util.Locale;

import com.cobblemon.mod.species.MonElement;
import com.cobblemon.mod.species.OwnedMon;
import com.cobblemon.mod.species.SpeciesHandle;

/**
 * Weather, terrain, and entry hazards for wild 1v1 battles.
 * <p>
 * Native engine support (not full Showdown parity). Turns count down at end of round.
 */
public final class BattleFieldEffects {
    public enum Weather {
        NONE, SUN, RAIN, SAND, SNOW;

        public static Weather fromMove(String moveId) {
            if (moveId == null) return NONE;
            String m = moveId.toLowerCase(Locale.ROOT).replace(" ", "").replace("-", "").replace("_", "");
            return switch (m) {
                case "sunnyday", "sun" -> SUN;
                case "raindance", "rain" -> RAIN;
                case "sandstorm", "sand" -> SAND;
                case "snowscape", "hail", "snow" -> SNOW;
                default -> NONE;
            };
        }
    }

    public enum Terrain {
        NONE, ELECTRIC, GRASSY, PSYCHIC, MISTY;

        public static Terrain fromMove(String moveId) {
            if (moveId == null) return NONE;
            String m = moveId.toLowerCase(Locale.ROOT).replace(" ", "").replace("-", "").replace("_", "");
            return switch (m) {
                case "electricterrain" -> ELECTRIC;
                case "grassyterrain" -> GRASSY;
                case "psychicterrain" -> PSYCHIC;
                case "mistyterrain" -> MISTY;
                default -> NONE;
            };
        }
    }

    private Weather weather = Weather.NONE;
    private int weatherTurns;
    private Terrain terrain = Terrain.NONE;
    private int terrainTurns;

    /** Stealth Rock layers: 0–1 (boolean as 0/1). */
    private int stealthRockPlayer;
    private int stealthRockWild;
    /** Spikes 0–3. */
    private int spikesPlayer;
    private int spikesWild;
    /** Toxic spikes 0–2. */
    private int toxicSpikesPlayer;
    private int toxicSpikesWild;
    private boolean stickyWebPlayer;
    private boolean stickyWebWild;

    public Weather weather() {
        return weather;
    }

    public Terrain terrain() {
        return terrain;
    }

    public void setWeather(Weather w, int turns) {
        this.weather = w == null ? Weather.NONE : w;
        this.weatherTurns = weather == Weather.NONE ? 0 : Math.max(1, turns);
    }

    public void setTerrain(Terrain t, int turns) {
        this.terrain = t == null ? Terrain.NONE : t;
        this.terrainTurns = terrain == Terrain.NONE ? 0 : Math.max(1, turns);
    }

    /** Try set field from a status-style move id. Returns log line or null. */
    public String applyFieldMove(String moveId, boolean usedByPlayer) {
        Weather w = Weather.fromMove(moveId);
        if (w != Weather.NONE) {
            setWeather(w, 5);
            return weatherLabel() + " for 5 turns!";
        }
        Terrain t = Terrain.fromMove(moveId);
        if (t != Terrain.NONE) {
            setTerrain(t, 5);
            return terrainLabel() + " for 5 turns!";
        }
        String m = moveId == null ? "" : moveId.toLowerCase(Locale.ROOT).replace("_", "").replace("-", "");
        // Hazards always set on the OPPOSITE side of the user
        boolean onPlayerSide = !usedByPlayer;
        return switch (m) {
            case "stealthrock" -> {
                if (onPlayerSide) stealthRockPlayer = 1;
                else stealthRockWild = 1;
                yield "Pointed stones float in the air around the foe!";
            }
            case "spikes" -> {
                if (onPlayerSide) spikesPlayer = Math.min(3, spikesPlayer + 1);
                else spikesWild = Math.min(3, spikesWild + 1);
                yield "Spikes were scattered around the foe's feet!";
            }
            case "toxicspikes" -> {
                if (onPlayerSide) toxicSpikesPlayer = Math.min(2, toxicSpikesPlayer + 1);
                else toxicSpikesWild = Math.min(2, toxicSpikesWild + 1);
                yield "Poison spikes were scattered around the foe's feet!";
            }
            case "stickyweb" -> {
                if (onPlayerSide) stickyWebPlayer = true;
                else stickyWebWild = true;
                yield "A sticky web spreads out beneath the foe!";
            }
            case "defog", "rapidspin" -> {
                clearHazards(!usedByPlayer);
                if (m.equals("defog")) {
                    setWeather(Weather.NONE, 0);
                }
                yield "The entry hazards were blown away!";
            }
            default -> null;
        };
    }

    public void clearHazards(boolean playerSide) {
        if (playerSide) {
            stealthRockPlayer = 0;
            spikesPlayer = 0;
            toxicSpikesPlayer = 0;
            stickyWebPlayer = false;
        } else {
            stealthRockWild = 0;
            spikesWild = 0;
            toxicSpikesWild = 0;
            stickyWebWild = false;
        }
    }

    /**
     * Damage multiplier for a move's element under current weather/terrain.
     */
    public float typeMultiplier(MonElement element, boolean physical) {
        float mul = 1f;
        if (weather == Weather.SUN) {
            if (element == MonElement.FIRE) mul *= 1.5f;
            if (element == MonElement.WATER) mul *= 0.5f;
        } else if (weather == Weather.RAIN) {
            if (element == MonElement.WATER) mul *= 1.5f;
            if (element == MonElement.FIRE) mul *= 0.5f;
        }
        if (terrain == Terrain.ELECTRIC && element == MonElement.ELECTRIC) {
            mul *= 1.3f;
        } else if (terrain == Terrain.GRASSY && element == MonElement.GRASS) {
            mul *= 1.3f;
        } else if (terrain == Terrain.PSYCHIC && element == MonElement.PSYCHIC) {
            mul *= 1.3f;
        } else if (terrain == Terrain.MISTY && element == MonElement.DRAGON) {
            mul *= 0.5f;
        }
        return mul;
    }

    /** End-of-turn residual from weather/terrain. Returns damage or heal amount (positive = damage). */
    public int residualDamage(boolean forPlayer, OwnedMon mon, String wildSpeciesId, int wildMaxHp, int wildHp) {
        int max = forPlayer ? mon.maxHp() : wildMaxHp;
        if (max <= 0) return 0;
        MonElement primary = forPlayer
                ? mon.primaryType()
                : SpeciesHandle.of(wildSpeciesId).primaryType();
        MonElement secondary = forPlayer
                ? mon.secondaryType().orElse(null)
                : SpeciesHandle.of(wildSpeciesId).secondaryType().orElse(null);

        if (weather == Weather.SAND) {
            if (!isRockGroundSteel(primary) && !isRockGroundSteel(secondary)) {
                return Math.max(1, max / 16);
            }
        } else if (weather == Weather.SNOW) {
            // Mild chip if not Ice (simplified hail)
            if (primary != MonElement.ICE && secondary != MonElement.ICE) {
                return Math.max(1, max / 16);
            }
        }
        if (terrain == Terrain.GRASSY) {
            // Heal 1/16
            return -Math.max(1, max / 16);
        }
        return 0;
    }

    private static boolean isRockGroundSteel(MonElement e) {
        return e != null && (e == MonElement.ROCK || e == MonElement.GROUND || e == MonElement.STEEL);
    }

    /**
     * Entry hazard damage when a mon switches in. {@code intoPlayerSide} true if the entering mon is the player's.
     * @return damage amount; toxic spikes may set status via out param style — caller applies status if return code special
     */
    public int onSwitchInDamage(boolean intoPlayerSide, SpeciesHandle handle, int maxHp) {
        int dmg = 0;
        int sr = intoPlayerSide ? stealthRockPlayer : stealthRockWild;
        int sp = intoPlayerSide ? spikesPlayer : spikesWild;
        if (sr > 0) {
            // Type-based SR: simplified 12.5% base, more if weak to Rock
            float mult = TypeChart.multiplier(MonElement.ROCK, handle.primaryType());
            if (handle.secondaryType().isPresent()) {
                mult *= TypeChart.multiplier(MonElement.ROCK, handle.secondaryType().get());
            }
            dmg += Math.max(1, Math.round(maxHp * 0.125f * mult));
        }
        if (sp > 0) {
            // 1/8, 1/6, 1/4
            float frac = sp == 1 ? 1f / 8f : sp == 2 ? 1f / 6f : 1f / 4f;
            // Flying / Levitate skip — simplified: Flying type immune
            boolean flying = handle.primaryType() == MonElement.FLYING
                    || handle.secondaryType().orElse(null) == MonElement.FLYING;
            if (!flying) {
                dmg += Math.max(1, Math.round(maxHp * frac));
            }
        }
        return dmg;
    }

    public int toxicSpikesLayers(boolean playerSide) {
        return playerSide ? toxicSpikesPlayer : toxicSpikesWild;
    }

    public boolean hasStickyWeb(boolean playerSide) {
        return playerSide ? stickyWebPlayer : stickyWebWild;
    }

    /** Tick durations at end of full turn. Returns log lines. */
    public java.util.List<String> endTurnTick() {
        java.util.ArrayList<String> lines = new java.util.ArrayList<>();
        if (weather != Weather.NONE) {
            weatherTurns--;
            if (weatherTurns <= 0) {
                lines.add("The " + weather.name().toLowerCase(Locale.ROOT) + " stopped.");
                weather = Weather.NONE;
            }
        }
        if (terrain != Terrain.NONE) {
            terrainTurns--;
            if (terrainTurns <= 0) {
                lines.add("The terrain returned to normal.");
                terrain = Terrain.NONE;
            }
        }
        return lines;
    }

    public String weatherLabel() {
        return switch (weather) {
            case SUN -> "The sunlight turned harsh";
            case RAIN -> "It started to rain";
            case SAND -> "A sandstorm kicked up";
            case SNOW -> "It started to snow";
            default -> "";
        };
    }

    public String terrainLabel() {
        return switch (terrain) {
            case ELECTRIC -> "An electric current ran across the battlefield";
            case GRASSY -> "Grass grew to cover the battlefield";
            case PSYCHIC -> "The battlefield got weird";
            case MISTY -> "Mist swirled around the battlefield";
            default -> "";
        };
    }

    public String shortSummary() {
        StringBuilder sb = new StringBuilder();
        if (weather != Weather.NONE) {
            sb.append(weather.name());
            if (weatherTurns > 0) sb.append('(').append(weatherTurns).append(')');
        }
        if (terrain != Terrain.NONE) {
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(terrain.name());
        }
        return sb.toString();
    }
}

package com.cobblemon.mod.battle;

import com.cobblemon.mod.species.MonElement;

/**
 * Simple effectiveness chart (single type).
 */
public final class TypeChart {
    private TypeChart() {}

    public static float multiplier(MonElement attack, MonElement defend) {
        if (attack == null || defend == null) {
            return 1.0f;
        }
        return switch (attack) {
            case FIRE -> switch (defend) {
                case GRASS, ICE, BUG, STEEL -> 2.0f;
                case FIRE, WATER, ROCK, DRAGON -> 0.5f;
                default -> 1.0f;
            };
            case WATER -> switch (defend) {
                case FIRE, GROUND, ROCK -> 2.0f;
                case WATER, GRASS, DRAGON -> 0.5f;
                default -> 1.0f;
            };
            case GRASS -> switch (defend) {
                case WATER, GROUND, ROCK -> 2.0f;
                case FIRE, GRASS, POISON, FLYING, BUG, DRAGON, STEEL -> 0.5f;
                default -> 1.0f;
            };
            case ELECTRIC -> switch (defend) {
                case WATER, FLYING -> 2.0f;
                case ELECTRIC, GRASS, DRAGON -> 0.5f;
                case GROUND -> 0.0f;
                default -> 1.0f;
            };
            case GROUND -> switch (defend) {
                case FIRE, ELECTRIC, POISON, ROCK, STEEL -> 2.0f;
                case GRASS, BUG -> 0.5f;
                case FLYING -> 0.0f;
                default -> 1.0f;
            };
            case FLYING -> switch (defend) {
                case GRASS, FIGHTING, BUG -> 2.0f;
                case ELECTRIC, ROCK, STEEL -> 0.5f;
                default -> 1.0f;
            };
            case FIGHTING -> switch (defend) {
                case NORMAL, ICE, ROCK, DARK, STEEL -> 2.0f;
                case POISON, FLYING, PSYCHIC, BUG, FAIRY -> 0.5f;
                case GHOST -> 0.0f;
                default -> 1.0f;
            };
            case ROCK -> switch (defend) {
                case FIRE, ICE, FLYING, BUG -> 2.0f;
                case FIGHTING, GROUND, STEEL -> 0.5f;
                default -> 1.0f;
            };
            case ICE -> switch (defend) {
                case GRASS, GROUND, FLYING, DRAGON -> 2.0f;
                case FIRE, WATER, ICE, STEEL -> 0.5f;
                default -> 1.0f;
            };
            case DRAGON -> switch (defend) {
                case DRAGON -> 2.0f;
                case STEEL -> 0.5f;
                case FAIRY -> 0.0f;
                default -> 1.0f;
            };
            case DARK -> switch (defend) {
                case PSYCHIC, GHOST -> 2.0f;
                case FIGHTING, DARK, FAIRY -> 0.5f;
                default -> 1.0f;
            };
            case PSYCHIC -> switch (defend) {
                case FIGHTING, POISON -> 2.0f;
                case PSYCHIC, STEEL -> 0.5f;
                case DARK -> 0.0f;
                default -> 1.0f;
            };
            case STEEL -> switch (defend) {
                case ICE, ROCK, FAIRY -> 2.0f;
                case FIRE, WATER, ELECTRIC, STEEL -> 0.5f;
                default -> 1.0f;
            };
            case FAIRY -> switch (defend) {
                case FIGHTING, DRAGON, DARK -> 2.0f;
                case FIRE, POISON, STEEL -> 0.5f;
                default -> 1.0f;
            };
            case BUG -> switch (defend) {
                case GRASS, PSYCHIC, DARK -> 2.0f;
                case FIRE, FIGHTING, POISON, FLYING, GHOST, STEEL, FAIRY -> 0.5f;
                default -> 1.0f;
            };
            case POISON -> switch (defend) {
                case GRASS, FAIRY -> 2.0f;
                case POISON, GROUND, ROCK, GHOST -> 0.5f;
                case STEEL -> 0.0f;
                default -> 1.0f;
            };
            case GHOST -> switch (defend) {
                case PSYCHIC, GHOST -> 2.0f;
                case DARK -> 0.5f;
                case NORMAL -> 0.0f;
                default -> 1.0f;
            };
            case NORMAL -> switch (defend) {
                case ROCK, STEEL -> 0.5f;
                case GHOST -> 0.0f;
                default -> 1.0f;
            };
        };
    }

    public static String label(float mult) {
        if (mult <= 0f) {
            return "It had no effect…";
        }
        if (mult >= 2f) {
            return "It's super effective!";
        }
        if (mult <= 0.5f) {
            return "It's not very effective…";
        }
        return "";
    }
}

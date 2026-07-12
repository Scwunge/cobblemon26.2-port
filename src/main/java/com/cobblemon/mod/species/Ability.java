package com.cobblemon.mod.species;

import java.util.Locale;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.StringRepresentable;

/**
 * Species ability — passive battle/world hooks (expand effects over time).
 */
public enum Ability implements StringRepresentable {
    BLAZE("blaze", "Fire moves strengthen when HP is low."),
    TORRENT("torrent", "Water moves strengthen when HP is low."),
    OVERGROW("overgrow", "Grass moves strengthen when HP is low."),
    STATIC("static", "Contact may jolt the attacker — speed edge in storms."),
    KEEN_EYE("keen_eye", "Accuracy cannot be lowered; better wild awareness."),
    INTIMIDATE("intimidate", "Lowers foe Attack on battle entry."),
    THICK_FAT("thick_fat", "Halves Fire and Ice damage taken."),
    LEVITATE("levitate", "Immune to Ground moves."),
    CLEAR_BODY("clear_body", "Stats cannot be lowered by foes."),
    SWIFT_SWIM("swift_swim", "Speed rises in rain / water biomes."),
    CHLOROPHYLL("chlorophyll", "Speed rises in bright daylight."),
    SAND_FORCE("sand_force", "Rock/Ground/Steel power up in deserts."),
    GUTS("guts", "Attack rises when HP is critical."),
    SHELL_ARMOR("shell_armor", "Blocks critical hits."),
    PRESSURE("pressure", "Foes spend more — wilds flee sooner."),
    SYNCHRONIZE("synchronize", "Shares nature pressure with wild encounters."),
    INNER_FOCUS("inner_focus", "Cannot flinch; steadier in battle."),
    PICKUP("pickup", "Sometimes finds items after walks."),
    RUN_AWAY("run_away", "Always escapes wild battles."),
    OWN_TEMPO("own_tempo", "Cannot be confused; even-minded."),
    MAXIMUM_TRUTH("maximum_truth", "Legendary psychic: moves ignore a little resistance."),
    RIPTIDE_MIND("riptide_mind", "Water + Psychic hybrid pressure."),
    KINDLE_COAT("kindle_coat", "Fire starter spark — tiny Fire STAB boost."),
    TRUE_HEART("true_heart", "Normal-type resolve — balanced growth."),
    NONE("none", "No special ability.");

    private final String id;
    private final String blurb;

    Ability(String id, String blurb) {
        this.id = id;
        this.blurb = blurb;
    }

    public String id() {
        return id;
    }

    public String blurb() {
        return blurb;
    }

    public MutableComponent displayName() {
        return Component.translatable("cobblemon.ability." + id);
    }

    @Override
    public String getSerializedName() {
        return id;
    }

    public static Ability byId(String id) {
        if (id == null || id.isBlank()) {
            return NONE;
        }
        String key = id.toLowerCase(Locale.ROOT);
        // Cobblemon abilities sometimes have h: prefix
        if (key.startsWith("h:")) {
            key = key.substring(2);
        }
        for (Ability a : values()) {
            if (a.id.equals(key) || a.name().equalsIgnoreCase(key)) {
                return a;
            }
        }
        // Map common Cobblemon ability names
        return switch (key) {
            case "solarpower", "flashfire" -> BLAZE;
            case "raindish", "hydration" -> TORRENT;
            case "effectspore", "leafguard" -> OVERGROW;
            case "lightningrod", "voltabsorb" -> STATIC;
            case "tangledfeet", "bigpecks" -> KEEN_EYE;
            case "sturdy", "rockhead" -> SHELL_ARMOR;
            case "arenatrap", "sandveil" -> SAND_FORCE;
            case "infiltrator", "cursedbody" -> INNER_FOCUS;
            case "marvelscale", "moxie" -> INTIMIDATE;
            case "magicguard", "trace" -> SYNCHRONIZE;
            case "noguard", "ironfist" -> GUTS;
            case "snowcloak", "icebody" -> THICK_FAT;
            case "magnetpull" -> LEVITATE;
            case "pickup", "technician" -> PICKUP;
            case "anticipation", "adaptability", "runaway" -> RUN_AWAY;
            case "cutecharm", "friendguard" -> OWN_TEMPO;
            case "pressure", "unnerve" -> PRESSURE;
            default -> NONE;
        };
    }

    /**
     * Multiplier on damage taken from {@code attackType}. 0 = immune, 1 = normal.
     */
    public float defensiveBoost(MonElement attackType) {
        return switch (this) {
            case LEVITATE -> attackType == MonElement.GROUND ? 0f : 1f;
            case THICK_FAT -> (attackType == MonElement.FIRE || attackType == MonElement.ICE) ? 0.5f : 1f;
            case SHELL_ARMOR -> 0.95f;
            default -> 1f;
        };
    }

    /**
     * Multiplier on damage dealt. {@code hpRatio} is 0..1 remaining HP.
     */
    public float offensiveBoost(MonElement moveType, float hpRatio) {
        boolean low = hpRatio <= 0.33f;
        return switch (this) {
            case BLAZE -> (moveType == MonElement.FIRE && low) ? 1.5f : (moveType == MonElement.FIRE ? 1.1f : 1f);
            case TORRENT -> (moveType == MonElement.WATER && low) ? 1.5f : (moveType == MonElement.WATER ? 1.1f : 1f);
            case OVERGROW -> (moveType == MonElement.GRASS && low) ? 1.5f : (moveType == MonElement.GRASS ? 1.1f : 1f);
            case KINDLE_COAT -> moveType == MonElement.FIRE ? 1.15f : 1f;
            case MAXIMUM_TRUTH -> moveType == MonElement.PSYCHIC ? 1.2f : 1f;
            case RIPTIDE_MIND -> (moveType == MonElement.WATER || moveType == MonElement.PSYCHIC) ? 1.15f : 1f;
            case GUTS -> low ? 1.3f : 1f;
            case SAND_FORCE -> (moveType == MonElement.ROCK || moveType == MonElement.GROUND || moveType == MonElement.STEEL)
                    ? 1.2f : 1f;
            default -> 1f;
        };
    }

    /** Map species to a default ability. */
    public static Ability fromSpecies(MonSpecies species) {
        Ability byTrait = byId(species.trait());
        if (byTrait != NONE) {
            return byTrait;
        }
        if (species == MonSpecies.MEWTWO || species == MonSpecies.MEW) {
            return MAXIMUM_TRUTH;
        }
        if (species == MonSpecies.ARTICUNO || species == MonSpecies.ZAPDOS || species == MonSpecies.MOLTRES) {
            return PRESSURE;
        }
        return switch (species.element()) {
            case FIRE -> BLAZE;
            case WATER -> TORRENT;
            case GRASS -> OVERGROW;
            case ELECTRIC -> STATIC;
            case FLYING -> KEEN_EYE;
            case ROCK, STEEL -> SHELL_ARMOR;
            case GROUND -> SAND_FORCE;
            case GHOST, DARK -> INNER_FOCUS;
            case DRAGON -> INTIMIDATE;
            case PSYCHIC -> SYNCHRONIZE;
            case FIGHTING -> GUTS;
            case ICE -> THICK_FAT;
            case NORMAL -> TRUE_HEART;
            case FAIRY -> OWN_TEMPO;
            case BUG, POISON -> RUN_AWAY;
            default -> NONE;
        };
    }
}

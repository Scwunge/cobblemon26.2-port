package com.cobblemon.mod.battle;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

import com.cobblemon.mod.species.MonElement;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.StringRepresentable;

/**
 * Cobblemon moves — power 0 = status. Accuracy 0–100.
 */
public enum MonMove implements StringRepresentable {
    // Normal
    TACKLE("tackle", MonElement.NORMAL, MoveCategory.PHYSICAL, 40, 100, 0, "A basic full-body charge."),
    GROWL("growl", MonElement.NORMAL, MoveCategory.STATUS, 0, 100, 0, "Lowers the foe's Attack briefly."),
    QUICK_STRIKE("quick_strike", MonElement.NORMAL, MoveCategory.PHYSICAL, 55, 100, 1, "A snappy jab that often goes first."),
    BODY_SLAM("body_slam", MonElement.NORMAL, MoveCategory.PHYSICAL, 85, 95, 0, "A heavy crash that may stagger."),

    // Fire
    EMBER_SNAP("ember_snap", MonElement.FIRE, MoveCategory.SPECIAL, 40, 100, 0, "A small burst of living embers."),
    FLAME_PAW("flame_paw", MonElement.FIRE, MoveCategory.PHYSICAL, 60, 100, 0, "Paws wreathed in heat."),
    HEAT_WAVE("heat_wave", MonElement.FIRE, MoveCategory.SPECIAL, 80, 90, 0, "A wash of searing air."),
    INFERNO_CRASH("inferno_crash", MonElement.FIRE, MoveCategory.PHYSICAL, 100, 85, 0, "A blazing full-power charge."),

    // Water
    SPLASH_JAB("splash_jab", MonElement.WATER, MoveCategory.PHYSICAL, 40, 100, 0, "A watery poke."),
    BUBBLE_BURST("bubble_burst", MonElement.WATER, MoveCategory.SPECIAL, 55, 100, 0, "Popping orbs of water."),
    TIDE_CRASH("tide_crash", MonElement.WATER, MoveCategory.PHYSICAL, 80, 95, 0, "A rolling wave slam."),
    ABYSS_PULSE("abyss_pulse", MonElement.WATER, MoveCategory.SPECIAL, 95, 90, 0, "Pressure from the deep."),

    // Grass
    LEAF_CLIP("leaf_clip", MonElement.GRASS, MoveCategory.PHYSICAL, 40, 100, 0, "A cutting leaf edge."),
    SEED_SHOT("seed_shot", MonElement.GRASS, MoveCategory.SPECIAL, 55, 100, 0, "Hard seeds fired like bullets."),
    VINE_LASH("vine_lash", MonElement.GRASS, MoveCategory.PHYSICAL, 75, 95, 0, "Whipping vines."),
    SOLAR_BURST("solar_burst", MonElement.GRASS, MoveCategory.SPECIAL, 95, 90, 0, "Stored sunlight released at once."),

    // Electric
    STATIC_NIBBLE("static_nibble", MonElement.ELECTRIC, MoveCategory.PHYSICAL, 40, 100, 0, "A crackling nibble."),
    SPARK_ARC("spark_arc", MonElement.ELECTRIC, MoveCategory.SPECIAL, 60, 100, 0, "An arc of blue-white spark."),
    THUNDER_PAW("thunder_paw", MonElement.ELECTRIC, MoveCategory.PHYSICAL, 80, 95, 0, "Lightning-charged claws."),
    STORM_BOLT("storm_bolt", MonElement.ELECTRIC, MoveCategory.SPECIAL, 100, 85, 0, "A full thunder strike."),

    // Flying
    GUST_FLUFF("gust_fluff", MonElement.FLYING, MoveCategory.SPECIAL, 40, 100, 0, "A soft but sharp gust."),
    WING_BUFF("wing_buff", MonElement.FLYING, MoveCategory.PHYSICAL, 60, 100, 0, "A solid wing slap."),
    SKY_DIVE("sky_dive", MonElement.FLYING, MoveCategory.PHYSICAL, 85, 90, 0, "A steep diving strike."),
    TEMPEST("tempest", MonElement.FLYING, MoveCategory.SPECIAL, 95, 90, 0, "A roaring wind funnel."),

    // Rock / Ground / Fighting / etc.
    PEBBLE_TOSS("pebble_toss", MonElement.ROCK, MoveCategory.PHYSICAL, 40, 100, 0, "Fling a sharp pebble."),
    STONE_CRASH("stone_crash", MonElement.ROCK, MoveCategory.PHYSICAL, 85, 90, 0, "A crushing rock body-blow."),
    DIRT_KICK("dirt_kick", MonElement.GROUND, MoveCategory.PHYSICAL, 45, 100, 0, "A kick that flings grit."),
    QUAKE_STOMP("quake_stomp", MonElement.GROUND, MoveCategory.PHYSICAL, 90, 90, 0, "Ground-shaking stomp."),
    QUICK_JAB("quick_jab", MonElement.FIGHTING, MoveCategory.PHYSICAL, 45, 100, 1, "A fast focused punch."),
    POWER_FIST("power_fist", MonElement.FIGHTING, MoveCategory.PHYSICAL, 90, 90, 0, "A heavy finishing punch."),
    SHADOW_NIP("shadow_nip", MonElement.DARK, MoveCategory.PHYSICAL, 45, 100, 0, "A bite from the dark."),
    NIGHT_EDGE("night_edge", MonElement.DARK, MoveCategory.PHYSICAL, 85, 95, 0, "A blade of pure night."),
    SCALE_SWIPE("scale_swipe", MonElement.DRAGON, MoveCategory.PHYSICAL, 50, 100, 0, "A sweeping claw of scale."),
    DRACO_ROAR("draco_roar", MonElement.DRAGON, MoveCategory.SPECIAL, 90, 90, 0, "A roar thick with power."),
    MIND_PING("mind_ping", MonElement.PSYCHIC, MoveCategory.SPECIAL, 45, 100, 0, "A sharp mental tap."),
    PSY_BURST("psy_burst", MonElement.PSYCHIC, MoveCategory.SPECIAL, 90, 95, 0, "A wave of psychic force."),
    FROST_NIP("frost_nip", MonElement.ICE, MoveCategory.PHYSICAL, 40, 100, 0, "A freezing nibble."),
    ICE_SHARD("ice_shard", MonElement.ICE, MoveCategory.PHYSICAL, 70, 100, 1, "A flung shard of ice."),
    GLACIER_CRASH("glacier_crash", MonElement.ICE, MoveCategory.PHYSICAL, 95, 85, 0, "A frozen mountain of force."),
    METAL_TAP("metal_tap", MonElement.STEEL, MoveCategory.PHYSICAL, 45, 100, 0, "A ringing metal poke."),
    IRON_CRASH("iron_crash", MonElement.STEEL, MoveCategory.PHYSICAL, 90, 90, 0, "Full-weight steel body."),
    SPARKLE("sparkle", MonElement.FAIRY, MoveCategory.SPECIAL, 40, 100, 0, "Dazzling motes of light."),
    MOON_DUST("moon_dust", MonElement.FAIRY, MoveCategory.SPECIAL, 80, 95, 0, "Soft powder that hits hard."),
    NIBBLE("nibble", MonElement.BUG, MoveCategory.PHYSICAL, 35, 100, 0, "A tiny chomp."),
    SILK_SHOT("silk_shot", MonElement.BUG, MoveCategory.SPECIAL, 65, 95, 0, "Sticky silk projectiles."),
    TOXIN_DAB("toxin_dab", MonElement.POISON, MoveCategory.SPECIAL, 40, 100, 0, "A touch of venom."),
    VENOM_SPIKE("venom_spike", MonElement.POISON, MoveCategory.PHYSICAL, 80, 95, 0, "Poisoned spikes."),
    PHASE_TOUCH("phase_touch", MonElement.GHOST, MoveCategory.SPECIAL, 45, 100, 0, "A chill that passes through."),
    HAUNT("haunt", MonElement.GHOST, MoveCategory.SPECIAL, 85, 95, 0, "A wave of dread energy."),
    HARDEN("harden", MonElement.NORMAL, MoveCategory.STATUS, 0, 100, 0, "Toughens the body, raising Defense."),
    FOCUS("focus", MonElement.FIGHTING, MoveCategory.STATUS, 0, 100, 0, "Steady the mind; next hit hits harder."),
    REST_SOFT("rest_soft", MonElement.NORMAL, MoveCategory.STATUS, 0, 100, 0, "Catch a breath and mend a little HP."),

    // ===== Grok line signatures — cosmic wit =====
    ASK_EVERYTHING("ask_everything", MonElement.PSYCHIC, MoveCategory.STATUS, 0, 100, 1,
            "Flood the field with questions. Raises focus; the next strike hits harder."),
    WIT_SPARK("wit_spark", MonElement.PSYCHIC, MoveCategory.SPECIAL, 55, 100, 1,
            "A lightning-fast jab of pure sass and cyan starlight."),
    TRUTH_BEAM("truth_beam", MonElement.PSYCHIC, MoveCategory.SPECIAL, 90, 100, 0,
            "A beam of unfiltered fact. Lies cannot armor against it."),
    MAXIMUM_TRUTH("maximum_truth", MonElement.PSYCHIC, MoveCategory.SPECIAL, 110, 95, 0,
            "Grokmon's creed made weapon. Reality flinches first."),
    VOID_LAUGH("void_laugh", MonElement.DARK, MoveCategory.SPECIAL, 75, 100, 0,
            "A chuckle from the abyss that rattles the foe's nerve."),
    STARFORGE_WIT("starforge_wit", MonElement.PSYCHIC, MoveCategory.SPECIAL, 120, 90, 0,
            "Groknova only. Forges a newborn star of understanding and drops it on the problem."),
    COSMIC_QUERY("cosmic_query", MonElement.PSYCHIC, MoveCategory.SPECIAL, 65, 100, 0,
            "A question so sharp it cuts through type resistance — half as a riddle, half as a comet.");

    private final String id;
    private final MonElement element;
    private final MoveCategory category;
    private final int power;
    private final int accuracy;
    private final int priority;
    private final String blurb;

    MonMove(String id, MonElement element, MoveCategory category, int power, int accuracy, int priority, String blurb) {
        this.id = id;
        this.element = element;
        this.category = category;
        this.power = power;
        this.accuracy = accuracy;
        this.priority = priority;
        this.blurb = blurb;
    }

    public String id() {
        return id;
    }

    public MonElement element() {
        return element;
    }

    public MoveCategory category() {
        return category;
    }

    public int power() {
        return power;
    }

    public int accuracy() {
        return accuracy;
    }

    public int priority() {
        return priority;
    }

    public String blurb() {
        return blurb;
    }

    public boolean isStatus() {
        return category == MoveCategory.STATUS || power <= 0;
    }

    /**
     * Default max PP (simplified mainline-style tiers).
     * Status / weak moves get more; high power gets less.
     */
    public int defaultMaxPp() {
        if (isStatus()) {
            return 20;
        }
        if (power >= 100) {
            return 8;
        }
        if (power >= 80) {
            return 12;
        }
        if (power >= 55) {
            return 16;
        }
        return 25;
    }

    public MutableComponent displayName() {
        // Cobblemon lang uses cobblemon.move.<id> — fall back to plain English
        MutableComponent fromLang = Component.translatable("cobblemon.move." + id);
        String s = fromLang.getString();
        if (s == null || s.isBlank() || s.contains(".") || s.startsWith("cobblemon.") || s.startsWith("move.")) {
            return Component.literal(englishName());
        }
        return Component.literal(s);
    }

    /** Plain English name (never a translation key). */
    public String englishName() {
        // ember_snap → Ember Snap
        String[] parts = id.replace('-', '_').split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) continue;
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(Character.toUpperCase(p.charAt(0)));
            if (p.length() > 1) sb.append(p.substring(1));
        }
        return sb.isEmpty() ? id : sb.toString();
    }

    public MutableComponent description() {
        return Component.literal(blurb);
    }

    @Override
    public String getSerializedName() {
        return id;
    }

    public static Optional<MonMove> byId(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        String key = id.toLowerCase(Locale.ROOT);
        return Arrays.stream(values()).filter(m -> m.id.equals(key)).findFirst();
    }

    public static MonMove byIdOrDefault(String id) {
        return byId(id).orElse(TACKLE);
    }
}

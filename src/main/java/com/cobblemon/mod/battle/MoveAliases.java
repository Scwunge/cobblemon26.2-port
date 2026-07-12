package com.cobblemon.mod.battle;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Maps Cobblemon / national-dex move ids onto our {@link MonMove} set.
 */
public final class MoveAliases {
    private static final Map<String, MonMove> MAP = new HashMap<>();

    static {
        // Universal
        alias("tackle", MonMove.TACKLE);
        alias("growl", MonMove.GROWL);
        alias("scratch", MonMove.TACKLE);
        alias("pound", MonMove.TACKLE);
        alias("quickattack", MonMove.QUICK_STRIKE);
        alias("quick_attack", MonMove.QUICK_STRIKE);
        alias("bodyslam", MonMove.BODY_SLAM);
        alias("body_slam", MonMove.BODY_SLAM);
        alias("takedown", MonMove.BODY_SLAM);
        alias("doubleedge", MonMove.BODY_SLAM);
        alias("slam", MonMove.BODY_SLAM);
        alias("harden", MonMove.HARDEN);
        alias("defensecurl", MonMove.HARDEN);
        alias("withdraw", MonMove.HARDEN);
        alias("rest", MonMove.REST_SOFT);
        alias("recover", MonMove.REST_SOFT);
        alias("roost", MonMove.REST_SOFT);
        alias("synthesis", MonMove.REST_SOFT);
        alias("moonlight", MonMove.REST_SOFT);
        alias("morningsun", MonMove.REST_SOFT);
        alias("focusenergy", MonMove.FOCUS);
        alias("swordsdance", MonMove.FOCUS);
        alias("nastyplot", MonMove.FOCUS);
        alias("bulkup", MonMove.FOCUS);
        alias("calmmind", MonMove.FOCUS);
        alias("agility", MonMove.FOCUS);
        alias("charm", MonMove.GROWL);
        alias("babydolleyes", MonMove.GROWL);
        alias("tailwhip", MonMove.GROWL);
        alias("leer", MonMove.GROWL);
        alias("playnice", MonMove.GROWL);
        alias("sweetkiss", MonMove.SPARKLE);
        alias("nuzzle", MonMove.STATIC_NIBBLE);

        // Fire
        alias("ember", MonMove.EMBER_SNAP);
        alias("flamethrower", MonMove.HEAT_WAVE);
        alias("fireblast", MonMove.INFERNO_CRASH);
        alias("firepunch", MonMove.FLAME_PAW);
        alias("flamewheel", MonMove.FLAME_PAW);
        alias("flareblitz", MonMove.INFERNO_CRASH);
        alias("heatwave", MonMove.HEAT_WAVE);
        alias("lavaplume", MonMove.HEAT_WAVE);
        alias("incinerate", MonMove.EMBER_SNAP);
        alias("firespin", MonMove.EMBER_SNAP);
        alias("overheat", MonMove.INFERNO_CRASH);
        alias("blastburn", MonMove.INFERNO_CRASH);
        alias("eruption", MonMove.INFERNO_CRASH);

        // Water
        alias("watergun", MonMove.SPLASH_JAB);
        alias("bubble", MonMove.BUBBLE_BURST);
        alias("bubblebeam", MonMove.BUBBLE_BURST);
        alias("surf", MonMove.TIDE_CRASH);
        alias("hydropump", MonMove.ABYSS_PULSE);
        alias("waterfall", MonMove.TIDE_CRASH);
        alias("aquatail", MonMove.TIDE_CRASH);
        alias("scald", MonMove.BUBBLE_BURST);
        alias("muddywater", MonMove.TIDE_CRASH);
        alias("hydrocannon", MonMove.ABYSS_PULSE);
        alias("liquidation", MonMove.TIDE_CRASH);
        alias("waterpulse", MonMove.BUBBLE_BURST);
        alias("brine", MonMove.BUBBLE_BURST);
        alias("whirlpool", MonMove.SPLASH_JAB);

        // Grass
        alias("vinewhip", MonMove.VINE_LASH);
        alias("razorleaf", MonMove.LEAF_CLIP);
        alias("magicalleaf", MonMove.LEAF_CLIP);
        alias("leafblade", MonMove.VINE_LASH);
        alias("seedbomb", MonMove.SEED_SHOT);
        alias("bulletseed", MonMove.SEED_SHOT);
        alias("solarbeam", MonMove.SOLAR_BURST);
        alias("energyball", MonMove.SEED_SHOT);
        alias("gigadrain", MonMove.SEED_SHOT);
        alias("megadrain", MonMove.SEED_SHOT);
        alias("absorb", MonMove.LEAF_CLIP);
        alias("petaldance", MonMove.SOLAR_BURST);
        alias("powerwhip", MonMove.VINE_LASH);
        alias("leafstorm", MonMove.SOLAR_BURST);
        alias("frenzyplant", MonMove.SOLAR_BURST);
        alias("grassknot", MonMove.VINE_LASH);
        alias("worryseed", MonMove.SEED_SHOT);
        alias("leechseed", MonMove.SEED_SHOT);
        alias("growth", MonMove.FOCUS);
        alias("sweetscent", MonMove.GROWL);
        alias("poisonpowder", MonMove.TOXIN_DAB);
        alias("sleeppowder", MonMove.TOXIN_DAB);
        alias("stunspore", MonMove.TOXIN_DAB);

        // Electric
        alias("thundershock", MonMove.STATIC_NIBBLE);
        alias("thunderbolt", MonMove.SPARK_ARC);
        alias("thunder", MonMove.STORM_BOLT);
        alias("spark", MonMove.STATIC_NIBBLE);
        alias("discharge", MonMove.SPARK_ARC);
        alias("wildcharge", MonMove.THUNDER_PAW);
        alias("volttackle", MonMove.STORM_BOLT);
        alias("thunderpunch", MonMove.THUNDER_PAW);
        alias("shockwave", MonMove.SPARK_ARC);
        alias("chargebeam", MonMove.SPARK_ARC);
        alias("electroball", MonMove.SPARK_ARC);
        alias("zingzap", MonMove.THUNDER_PAW);

        // Flying
        alias("gust", MonMove.GUST_FLUFF);
        alias("wingattack", MonMove.WING_BUFF);
        alias("aerialace", MonMove.WING_BUFF);
        alias("airslash", MonMove.SKY_DIVE);
        alias("fly", MonMove.SKY_DIVE);
        alias("bravebird", MonMove.SKY_DIVE);
        alias("hurricane", MonMove.TEMPEST);
        alias("aircutter", MonMove.GUST_FLUFF);
        alias("peck", MonMove.GUST_FLUFF);
        alias("drillpeck", MonMove.SKY_DIVE);
        alias("skyattack", MonMove.SKY_DIVE);

        // Rock / Ground / Fighting
        alias("rockthrow", MonMove.PEBBLE_TOSS);
        alias("rockslide", MonMove.STONE_CRASH);
        alias("stoneedge", MonMove.STONE_CRASH);
        alias("rockblast", MonMove.PEBBLE_TOSS);
        alias("rollout", MonMove.PEBBLE_TOSS);
        alias("mudslap", MonMove.DIRT_KICK);
        alias("mudshot", MonMove.DIRT_KICK);
        alias("earthquake", MonMove.QUAKE_STOMP);
        alias("bulldoze", MonMove.QUAKE_STOMP);
        alias("dig", MonMove.DIRT_KICK);
        alias("magnitude", MonMove.QUAKE_STOMP);
        alias("stomp", MonMove.BODY_SLAM);
        alias("karatechop", MonMove.QUICK_JAB);
        alias("lowkick", MonMove.QUICK_JAB);
        alias("brickbreak", MonMove.POWER_FIST);
        alias("closecombat", MonMove.POWER_FIST);
        alias("crosschop", MonMove.POWER_FIST);
        alias("dynamicpunch", MonMove.POWER_FIST);
        alias("machpunch", MonMove.QUICK_JAB);
        alias("focuspunch", MonMove.POWER_FIST);
        alias("superpower", MonMove.POWER_FIST);
        alias("seismictoss", MonMove.QUICK_JAB);
        alias("submission", MonMove.POWER_FIST);
        alias("revenge", MonMove.POWER_FIST);

        // Dark / Ghost / Dragon / Psychic
        alias("bite", MonMove.SHADOW_NIP);
        alias("crunch", MonMove.NIGHT_EDGE);
        alias("pursuit", MonMove.SHADOW_NIP);
        alias("thief", MonMove.SHADOW_NIP);
        alias("knockoff", MonMove.NIGHT_EDGE);
        alias("darkpulse", MonMove.NIGHT_EDGE);
        alias("foulplay", MonMove.NIGHT_EDGE);
        alias("suckerpunch", MonMove.SHADOW_NIP);
        alias("shadowball", MonMove.HAUNT);
        alias("shadowclaw", MonMove.PHASE_TOUCH);
        alias("lick", MonMove.PHASE_TOUCH);
        alias("nightshade", MonMove.HAUNT);
        alias("hex", MonMove.HAUNT);
        alias("dragonrage", MonMove.SCALE_SWIPE);
        alias("dragonclaw", MonMove.SCALE_SWIPE);
        alias("dragonpulse", MonMove.DRACO_ROAR);
        alias("outrage", MonMove.DRACO_ROAR);
        alias("dracometeor", MonMove.DRACO_ROAR);
        alias("confusion", MonMove.MIND_PING);
        alias("psybeam", MonMove.MIND_PING);
        alias("psychic", MonMove.PSY_BURST);
        alias("psyshock", MonMove.PSY_BURST);
        alias("futuresight", MonMove.PSY_BURST);
        alias("zenheadbutt", MonMove.MIND_PING);
        alias("extrasensory", MonMove.PSY_BURST);

        // Ice / Steel / Fairy / Bug / Poison
        alias("powdersnow", MonMove.FROST_NIP);
        alias("iceshard", MonMove.ICE_SHARD);
        alias("icebeam", MonMove.ICE_SHARD);
        alias("blizzard", MonMove.GLACIER_CRASH);
        alias("icywind", MonMove.FROST_NIP);
        alias("icepunch", MonMove.ICE_SHARD);
        alias("aurorabeam", MonMove.ICE_SHARD);
        alias("metalclaw", MonMove.METAL_TAP);
        alias("ironhead", MonMove.IRON_CRASH);
        alias("irontail", MonMove.IRON_CRASH);
        alias("flashcannon", MonMove.METAL_TAP);
        alias("steelwing", MonMove.METAL_TAP);
        alias("meteormash", MonMove.IRON_CRASH);
        alias("dazzlinggleam", MonMove.SPARKLE);
        alias("moonblast", MonMove.MOON_DUST);
        alias("playrough", MonMove.MOON_DUST);
        alias("fairywind", MonMove.SPARKLE);
        alias("disarmingvoice", MonMove.SPARKLE);
        alias("bugbite", MonMove.NIBBLE);
        alias("strugglebug", MonMove.NIBBLE);
        alias("xscissor", MonMove.SILK_SHOT);
        alias("megahorn", MonMove.SILK_SHOT);
        alias("uturn", MonMove.NIBBLE);
        alias("signalbeam", MonMove.SILK_SHOT);
        alias("pinmissile", MonMove.NIBBLE);
        alias("twineedle", MonMove.NIBBLE);
        alias("poisonsting", MonMove.TOXIN_DAB);
        alias("sludge", MonMove.TOXIN_DAB);
        alias("sludgebomb", MonMove.VENOM_SPIKE);
        alias("poisonjab", MonMove.VENOM_SPIKE);
        alias("toxic", MonMove.TOXIN_DAB);
        alias("acid", MonMove.TOXIN_DAB);
        alias("gunkshot", MonMove.VENOM_SPIKE);
        alias("crosspoison", MonMove.VENOM_SPIKE);

        // Misc strong normals
        alias("hyperbeam", MonMove.BODY_SLAM);
        alias("gigaimpact", MonMove.BODY_SLAM);
        alias("strength", MonMove.BODY_SLAM);
        alias("facade", MonMove.TACKLE);
        alias("return", MonMove.TACKLE);
        alias("frustration", MonMove.TACKLE);
        alias("secretpower", MonMove.TACKLE);
        alias("hiddenpower", MonMove.TACKLE);
        alias("swift", MonMove.QUICK_STRIKE);
        alias("headbutt", MonMove.BODY_SLAM);
        alias("cut", MonMove.LEAF_CLIP);
        alias("flash", MonMove.SPARKLE);
        alias("protect", MonMove.HARDEN);
        alias("detect", MonMove.HARDEN);
        alias("substitute", MonMove.HARDEN);
        alias("endure", MonMove.HARDEN);
        alias("sleeptalk", MonMove.REST_SOFT);
        alias("snore", MonMove.REST_SOFT);
    }

    private MoveAliases() {}

    private static void alias(String id, MonMove move) {
        MAP.put(normalize(id), move);
    }

    public static String normalize(String id) {
        if (id == null) {
            return "";
        }
        return id.toLowerCase(Locale.ROOT)
                .replace(" ", "")
                .replace("-", "")
                .replace("_", "")
                .replace("'", "")
                .replace(".", "");
    }

    public static MonMove resolve(String cobblemonMoveId) {
        if (cobblemonMoveId == null || cobblemonMoveId.isBlank()) {
            return MonMove.TACKLE;
        }
        String n = normalize(cobblemonMoveId);
        MonMove mapped = MAP.get(n);
        if (mapped != null) {
            return mapped;
        }
        // enum id match
        var enumMatch = MonMove.byId(cobblemonMoveId).or(() -> MonMove.byId(n));
        if (enumMatch.isPresent()) {
            return enumMatch.get();
        }
        // Infer from move name keywords (covers full datapack learnsets)
        return inferFromName(n);
    }

    /**
     * Maps unlisted Cobblemon move ids onto our typed MonMove set using name keywords
     * so species learnsets aren't collapsed to Tackle.
     */
    private static MonMove inferFromName(String n) {
        // Status / setup
        if (containsAny(n, "protect", "detect", "substitute", "endure", "safeguard", "wideguard", "quickguard")) {
            return MonMove.HARDEN;
        }
        if (containsAny(n, "rest", "recover", "roost", "softboiled", "milkdrink", "synthesis", "moonlight", "morningsun", "shoreup", "healorder")) {
            return MonMove.REST_SOFT;
        }
        if (containsAny(n, "swordsdance", "nastyplot", "dragondance", "quiverdance", "bulkup", "calmmind", "agility", "rockpolish", "autotomize", "shiftgear", "shellsmash", "growth", "workup", "honeclaws", "coil", "quiver", "focusenergy")) {
            return MonMove.FOCUS;
        }
        if (containsAny(n, "growl", "leer", "tailwhip", "babydolleyes", "charm", "tickle", "featherdance", "scaryface", "partingshot")) {
            return MonMove.GROWL;
        }
        // Types by keyword
        if (containsAny(n, "fire", "flame", "ember", "blaze", "heat", "lava", "inferno", "burn", "overheat", "flare", "sunny", "magm", "searing", "pyro", "torch", "willowi", "willowisp")) {
            return powerTier(n, MonMove.EMBER_SNAP, MonMove.HEAT_WAVE, MonMove.INFERNO_CRASH);
        }
        if (containsAny(n, "water", "aqua", "hydro", "surf", "bubble", "wave", "ocean", "soak", "brine", "liquidation", "scald", "whirlpool", "dive", "rain")) {
            return powerTier(n, MonMove.SPLASH_JAB, MonMove.BUBBLE_BURST, MonMove.ABYSS_PULSE);
        }
        if (containsAny(n, "grass", "leaf", "seed", "vine", "solar", "petal", "wood", "forest", "spore", "powder", "giga drain", "gigadrain", "mega drain", "megadrain", "absorb", "energyball", "frenzyplant")) {
            return powerTier(n, MonMove.LEAF_CLIP, MonMove.SEED_SHOT, MonMove.SOLAR_BURST);
        }
        if (containsAny(n, "thunder", "electric", "volt", "spark", "shock", "zap", "electro", "charge", "plasma", "ion", "nuzzle")) {
            return powerTier(n, MonMove.STATIC_NIBBLE, MonMove.SPARK_ARC, MonMove.STORM_BOLT);
        }
        if (containsAny(n, "wing", "aerial", "air", "fly", "sky", "hurricane", "gust", "peck", "drill", "bravebird", "acrobat", "tailwind")) {
            return powerTier(n, MonMove.GUST_FLUFF, MonMove.WING_BUFF, MonMove.SKY_DIVE);
        }
        if (containsAny(n, "rock", "stone", "geo", "ancientpower", "stealthrock", "powergem", "diamond")) {
            return powerTier(n, MonMove.PEBBLE_TOSS, MonMove.STONE_CRASH, MonMove.STONE_CRASH);
        }
        if (containsAny(n, "ground", "earth", "mud", "sand", "dig", "fissure", "magnitude", "bulldoze", "stomping", "highhorsepower")) {
            return powerTier(n, MonMove.DIRT_KICK, MonMove.QUAKE_STOMP, MonMove.QUAKE_STOMP);
        }
        if (containsAny(n, "fight", "punch", "kick", "closecombat", "brick", "aura", "forcepalm", "superpower", "crosschop", "dynamic", "seismic", "counter", "drainpunch", "focusblast", "machpunch", "bulletpunch")) {
            return powerTier(n, MonMove.QUICK_JAB, MonMove.POWER_FIST, MonMove.POWER_FIST);
        }
        if (containsAny(n, "dark", "night", "crunch", "bite", "knock", "foul", "thief", "sucker", "pursuit", "snarl", "payback", "assurance", "brutalswing")) {
            return powerTier(n, MonMove.SHADOW_NIP, MonMove.NIGHT_EDGE, MonMove.NIGHT_EDGE);
        }
        if (containsAny(n, "dragon", "draco", "outrage", "scale", "twister", "dualchop", "breaking swipe", "breakingswipe")) {
            return powerTier(n, MonMove.SCALE_SWIPE, MonMove.DRACO_ROAR, MonMove.DRACO_ROAR);
        }
        if (containsAny(n, "psychic", "psy", "mind", "zen", "future", "confusion", "extrasensory", "luster", "storedpower", "psyshock", "mirrorcoat", "teleport", "trick", "skillswap")) {
            return powerTier(n, MonMove.MIND_PING, MonMove.PSY_BURST, MonMove.PSY_BURST);
        }
        if (containsAny(n, "ice", "freeze", "frost", "blizzard", "aurora", "hail", "snow", "glacial", "cold")) {
            return powerTier(n, MonMove.FROST_NIP, MonMove.ICE_SHARD, MonMove.GLACIER_CRASH);
        }
        if (containsAny(n, "steel", "iron", "metal", "flashcannon", "meteor", "gear", "bullet", "gyro", "heavyslam", "smartstrike")) {
            return powerTier(n, MonMove.METAL_TAP, MonMove.IRON_CRASH, MonMove.IRON_CRASH);
        }
        if (containsAny(n, "fairy", "moon", "dazzling", "playrough", "disarming", "sparkling", "misty", "alluring", "drainingkiss", "geomancy")) {
            return powerTier(n, MonMove.SPARKLE, MonMove.MOON_DUST, MonMove.MOON_DUST);
        }
        if (containsAny(n, "bug", "silk", "pin", "xscissor", "megahorn", "uturn", "leechlife", "furycutter", "signal", "strugglebug", "firstimpression", "pollenpuff", "lunge")) {
            return powerTier(n, MonMove.NIBBLE, MonMove.SILK_SHOT, MonMove.SILK_SHOT);
        }
        if (containsAny(n, "poison", "toxic", "sludge", "venom", "acid", "gunk", "crosspoison", "poisonjab", "belch", "clearsmog", "smog")) {
            return powerTier(n, MonMove.TOXIN_DAB, MonMove.VENOM_SPIKE, MonMove.VENOM_SPIKE);
        }
        if (containsAny(n, "ghost", "shadow", "hex", "haunt", "phantom", "ominous", "spite", "curse", "astonish", "nightshade", "poltergeist")) {
            return powerTier(n, MonMove.PHASE_TOUCH, MonMove.HAUNT, MonMove.HAUNT);
        }
        if (containsAny(n, "hyper", "giga", "doubleedge", "thrash", "outrage", "lastresort", "facade", "return", "frustration", "strength", "bodyslam", "takedown", "skullbash", "headcharge")) {
            return MonMove.BODY_SLAM;
        }
        if (containsAny(n, "quick", "extreme", "mach", "bullet", "aqua jet", "aquajet", "iceshard", "shadowsneak", "accelerock", "firstimpression", "suckerpunch", "vacuumwave")) {
            return MonMove.QUICK_STRIKE;
        }
        return MonMove.TACKLE;
    }

    private static MonMove powerTier(String n, MonMove weak, MonMove mid, MonMove strong) {
        if (containsAny(n, "blast", "hyper", "giga", "max", "ultimate", "meteor", "blast", "cannon", "overheat", "outrage", "closecombat", "superpower", "draco", "frenzy", "hydrocannon", "blastburn", "frenzyplant", "boomburst", "eruption", "water spout", "waterspout")) {
            return strong;
        }
        if (containsAny(n, "beam", "thrower", "pulse", "slash", "crash", "quake", "storm", "dance", "fang", "punch", "kick", "tail", "claw", "wave", "ball", "bomb", "spit", "shot")) {
            return mid;
        }
        // Longer names often stronger TMs
        if (n.length() >= 12) {
            return mid;
        }
        return weak;
    }

    private static boolean containsAny(String n, String... keys) {
        for (String k : keys) {
            if (n.contains(normalize(k))) {
                return true;
            }
        }
        return false;
    }
}

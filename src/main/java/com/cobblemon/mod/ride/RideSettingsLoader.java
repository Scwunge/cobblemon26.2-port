package com.cobblemon.mod.ride;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.cobblemon.mod.Cobblemon;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Loads official-style {@code data/cobblemon/ride_settings/*.json} into a map by settings id
 * (filename without extension: {@code horse}, {@code bird}, …).
 * <p>
 * Safe defaults if files are missing. Expressions like
 * {@code q.get_ride_stats('SPEED','LAND',1.2,0.1)} are scraped for numeric bases only —
 * full MoLang ride physics is future work (R2–R5).
 */
public final class RideSettingsLoader {
    private static final String[] KNOWN_IDS = {
            "bird", "boat", "burst", "dolphin", "glider", "helicopter",
            "horse", "hover", "jet", "minekart", "rocket", "submarine", "vehicle"
    };

    private static final Pattern SPEED_NUM = Pattern.compile(
            "get_ride_stats\\(\\s*'SPEED'\\s*,\\s*'(\\w+)'\\s*,\\s*([0-9.]+)",
            Pattern.CASE_INSENSITIVE
    );

    private static Map<String, RideSettings> BY_ID = Map.of();
    private static boolean loaded;

    private RideSettingsLoader() {}

    public static void bootstrap() {
        if (loaded) {
            return;
        }
        loaded = true;
        Map<String, RideSettings> map = new LinkedHashMap<>();
        int ok = 0;
        for (String id : KNOWN_IDS) {
            RideSettings settings = loadOne(id);
            map.put(id, settings);
            if (settings.fromDatapack()) {
                ok++;
            }
        }
        BY_ID = Collections.unmodifiableMap(map);
        Cobblemon.LOGGER.info(
                "RideSettingsLoader: {} profiles ({} from datapack, {} defaults)",
                BY_ID.size(), ok, BY_ID.size() - ok
        );
    }

    public static Map<String, RideSettings> all() {
        ensure();
        return BY_ID;
    }

    public static Optional<RideSettings> get(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        ensure();
        String key = normalizeId(id);
        return Optional.ofNullable(BY_ID.get(key));
    }

    /** Prefer explicit id, else land horse defaults. */
    public static RideSettings getOrDefault(String id) {
        return get(id).orElseGet(RideSettingsLoader::defaultLand);
    }

    public static RideSettings defaultLand() {
        ensure();
        RideSettings horse = BY_ID.get("horse");
        if (horse != null) {
            return horse;
        }
        return RideSettings.defaults("horse", false);
    }

    private static void ensure() {
        if (!loaded) {
            bootstrap();
        }
    }

    private static String normalizeId(String id) {
        String s = id.toLowerCase(Locale.ROOT).trim();
        // Accept cobblemon:land/horse or land/horse
        int slash = s.lastIndexOf('/');
        if (slash >= 0 && slash < s.length() - 1) {
            s = s.substring(slash + 1);
        }
        int colon = s.lastIndexOf(':');
        if (colon >= 0 && colon < s.length() - 1) {
            s = s.substring(colon + 1);
        }
        return s;
    }

    private static RideSettings loadOne(String id) {
        String path = "data/cobblemon/ride_settings/" + id + ".json";
        try (InputStream in = open(path)) {
            if (in == null) {
                return RideSettings.defaults(id, isAirPreset(id));
            }
            JsonObject root = JsonParser.parseReader(
                    new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))
            ).getAsJsonObject();
            return parse(id, root);
        } catch (Exception ex) {
            Cobblemon.LOGGER.debug("RideSettingsLoader: failed {}: {}", id, ex.toString());
            return RideSettings.defaults(id, isAirPreset(id));
        }
    }

    private static RideSettings parse(String id, JsonObject root) {
        String speedExpr = str(root, "speedExpr");
        float speedBase = scrapeSpeedBase(speedExpr, id);
        boolean canFly = isAirPreset(id)
                || (speedExpr != null && speedExpr.toUpperCase(Locale.ROOT).contains("'AIR'"))
                || root.has("infiniteAltitude");
        boolean canJump = boolish(root, "canJump", !canFly);
        boolean canSprint = boolish(root, "canSprint", true);
        boolean infiniteStamina = boolish(root, "infiniteStamina", false);
        Map<String, String> raw = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> e : root.entrySet()) {
            raw.put(e.getKey(), e.getValue().isJsonPrimitive()
                    ? e.getValue().getAsString()
                    : e.getValue().toString());
        }
        return new RideSettings(id, speedBase, canFly, canJump, canSprint, infiniteStamina, true, raw);
    }

    private static float scrapeSpeedBase(String speedExpr, String id) {
        if (speedExpr != null && !speedExpr.isBlank()) {
            Matcher m = SPEED_NUM.matcher(speedExpr);
            if (m.find()) {
                try {
                    return Float.parseFloat(m.group(2));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return isAirPreset(id) ? 16.0f : 1.2f;
    }

    private static boolean isAirPreset(String id) {
        return switch (id) {
            case "bird", "glider", "helicopter", "hover", "jet", "rocket" -> true;
            default -> false;
        };
    }

    private static String str(JsonObject o, String key) {
        if (!o.has(key) || o.get(key).isJsonNull()) {
            return null;
        }
        try {
            return o.get(key).getAsString();
        } catch (Exception e) {
            return o.get(key).toString();
        }
    }

    private static boolean boolish(JsonObject o, String key, boolean def) {
        if (!o.has(key) || o.get(key).isJsonNull()) {
            return def;
        }
        try {
            JsonElement el = o.get(key);
            if (el.isJsonPrimitive() && el.getAsJsonPrimitive().isBoolean()) {
                return el.getAsBoolean();
            }
            String s = el.getAsString().trim().toLowerCase(Locale.ROOT);
            if ("true".equals(s) || "1".equals(s) || "yes".equals(s)) {
                return true;
            }
            if ("false".equals(s) || "0".equals(s) || "no".equals(s)) {
                return false;
            }
        } catch (Exception ignored) {
        }
        return def;
    }

    private static InputStream open(String path) {
        ClassLoader cl = RideSettingsLoader.class.getClassLoader();
        InputStream in = cl.getResourceAsStream(path);
        if (in == null) {
            in = RideSettingsLoader.class.getResourceAsStream("/" + path);
        }
        return in;
    }

    /**
     * Immutable ride profile used by [RideController] / [com.cobblemon.mod.network.RidePayload].
     */
    public record RideSettings(
            String id,
            float speedBase,
            boolean canFly,
            boolean canJump,
            boolean canSprint,
            boolean infiniteStamina,
            boolean fromDatapack,
            Map<String, String> raw
    ) {
        public RideSettings {
            raw = raw == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(raw));
        }

        public static RideSettings defaults(String id, boolean canFly) {
            return new RideSettings(
                    id,
                    canFly ? 16.0f : 1.2f,
                    canFly,
                    !canFly,
                    true,
                    false,
                    false,
                    Map.of()
            );
        }

        /**
         * Map datapack speed units into vanilla movement-speed attribute base.
         * LAND expressions use ~1–2; AIR uses ~20. Both clamp to a playable mount range.
         */
        public float movementSpeed(int monLevel) {
            return movementSpeed(monLevel, 50);
        }

        /**
         * @param speciesSpeedBase species base Speed stat (e.g. 45–160) from datapack
         */
        public float movementSpeed(int monLevel, int speciesSpeedBase) {
            float levelBonus = Math.min(0.20f, Math.max(0, monLevel) * 0.004f);
            // R5: species Speed 50 ≈ 1.0×, 100 ≈ 1.18×, 30 ≈ 0.90×
            float spe = Math.max(20, Math.min(200, speciesSpeedBase));
            float speciesMult = 0.78f + (spe / 100f) * 0.44f;
            float fromExpr;
            if (canFly || speedBase >= 8.0f) {
                // Air-ish: 20 → ~0.42
                fromExpr = 0.28f + Math.min(0.22f, speedBase * 0.007f);
            } else {
                // Land: 1.2 → ~0.30
                fromExpr = 0.24f + Math.min(0.20f, speedBase * 0.05f);
            }
            return Math.min(0.62f, (fromExpr + levelBonus) * speciesMult);
        }
    }
}

package com.cobblemon.mod.species;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import com.cobblemon.mod.Cobblemon;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

/**
 * Item-interact evolutions from species datapack ({@code requiredContext: cobblemon:fire_stone}).
 */
public final class ItemEvolutionLookup {
    /** key: "species|stoneItemPath" → into species id */
    private static Map<String, String> MAP = Map.of();
    private static boolean loaded;

    private ItemEvolutionLookup() {}

    public static void bootstrapClasspath() {
        if (loaded && !MAP.isEmpty()) {
            return;
        }
        Map<String, String> map = new HashMap<>();
        // Hardcoded classics (always available)
        put(map, "eevee", "thunder_stone", "jolteon");
        put(map, "eevee", "water_stone", "vaporeon");
        put(map, "eevee", "fire_stone", "flareon");
        put(map, "eevee", "leaf_stone", "leafeon");
        put(map, "eevee", "ice_stone", "glaceon");
        put(map, "pikachu", "thunder_stone", "raichu");
        put(map, "nidorina", "moon_stone", "nidoqueen");
        put(map, "nidorino", "moon_stone", "nidoking");
        put(map, "clefairy", "moon_stone", "clefable");
        put(map, "jigglypuff", "moon_stone", "wigglytuff");
        put(map, "gloom", "leaf_stone", "vileplume");
        put(map, "gloom", "sun_stone", "bellossom");
        put(map, "weepinbell", "leaf_stone", "victreebel");
        put(map, "exeggcute", "leaf_stone", "exeggutor");
        put(map, "growlithe", "fire_stone", "arcanine");
        put(map, "vulpix", "fire_stone", "ninetales");
        put(map, "poliwhirl", "water_stone", "poliwrath");
        put(map, "shellder", "water_stone", "cloyster");
        put(map, "staryu", "water_stone", "starmie");
        put(map, "eevee", "shiny_stone", "sylveon"); // fallback if no friendship path
        put(map, "murkrow", "dusk_stone", "honchkrow");
        put(map, "misdreavus", "dusk_stone", "mismagius");
        put(map, "lampent", "dusk_stone", "chandelure");
        put(map, "doublade", "dusk_stone", "aegislash");
        put(map, "cosplay_pikachu", "thunder_stone", "raichu");
        put(map, "happiny", "oval_stone", "chansey");
        put(map, "munchlax", "sitrus_berry", "snorlax");

        // Scan species folder for item_interact evolutions
        try {
            var cl = ItemEvolutionLookup.class.getClassLoader();
            // Content index is heavy; sample generation folders
            for (int gen = 1; gen <= 9; gen++) {
                // Can't list JAR dirs easily — rely on hardcoded + runtime ResourceManager path
            }
        } catch (Exception ignored) {
        }

        MAP = Collections.unmodifiableMap(map);
        loaded = true;
        Cobblemon.LOGGER.info("ItemEvolutionLookup: {} stone/item chains", MAP.size());
    }

    public static void bootstrap(ResourceManager resources) {
        Map<String, String> map = new HashMap<>(MAP.isEmpty() ? Map.of() : MAP);
        if (!loaded) {
            bootstrapClasspath();
            map = new HashMap<>(MAP);
        }
        try {
            Map<Identifier, Resource> found = resources.listResources(
                    "species",
                    id -> id.getNamespace().equals(Cobblemon.MOD_ID) && id.getPath().endsWith(".json")
            );
            for (var e : found.entrySet()) {
                try (var in = e.getValue().open();
                     var reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                    JsonObject o = JsonParser.parseReader(reader).getAsJsonObject();
                    String from = e.getKey().getPath();
                    int slash = from.lastIndexOf('/');
                    String species = from.substring(slash + 1).replace(".json", "").toLowerCase(Locale.ROOT);
                    if (!o.has("evolutions") || !o.get("evolutions").isJsonArray()) {
                        continue;
                    }
                    JsonArray arr = o.getAsJsonArray("evolutions");
                    for (JsonElement el : arr) {
                        if (!el.isJsonObject()) continue;
                        JsonObject ev = el.getAsJsonObject();
                        String variant = ev.has("variant") ? ev.get("variant").getAsString() : "";
                        if (!"item_interact".equalsIgnoreCase(variant)) continue;
                        if (!ev.has("result") || !ev.has("requiredContext")) continue;
                        String into = strip(ev.get("result").getAsString());
                        String stone = strip(ev.get("requiredContext").getAsString());
                        put(map, species, stone, into);
                    }
                } catch (Exception ignored) {
                }
            }
        } catch (Exception ex) {
            Cobblemon.LOGGER.warn("Item evo scan failed", ex);
        }
        MAP = Collections.unmodifiableMap(map);
        loaded = true;
        Cobblemon.LOGGER.info("ItemEvolutionLookup: {} chains (datapack scan)", MAP.size());
    }

    private static void put(Map<String, String> map, String from, String stone, String into) {
        map.put(key(from, stone), into.toLowerCase(Locale.ROOT));
    }

    private static String key(String species, String stone) {
        return species.toLowerCase(Locale.ROOT) + "|" + strip(stone);
    }

    private static String strip(String raw) {
        String s = raw.toLowerCase(Locale.ROOT).trim();
        int c = s.indexOf(':');
        return c >= 0 ? s.substring(c + 1) : s;
    }

    public static Optional<String> evolveWith(String speciesId, String stoneItemId) {
        if (!loaded) {
            bootstrapClasspath();
        }
        if (speciesId == null || stoneItemId == null) {
            return Optional.empty();
        }
        String k = key(speciesId, stoneItemId);
        String into = MAP.get(k);
        if (into != null && !DisabledSpecies.isDisabled(into)) {
            return Optional.of(into);
        }
        return Optional.empty();
    }
}

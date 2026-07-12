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
 * Loads {@code data/cobblemon/fossils/*.json} → fossil item id → result species id.
 */
public final class FossilRecipes {
    private static Map<String, String> FOSSIL_TO_SPECIES = Map.of();
    private static boolean loaded;

    private FossilRecipes() {}

    public static void bootstrap(ResourceManager resources) {
        Map<String, String> map = new HashMap<>();
        try {
            Map<Identifier, Resource> found = resources.listResources(
                    "fossils",
                    id -> id.getNamespace().equals(Cobblemon.MOD_ID) && id.getPath().endsWith(".json")
            );
            for (var e : found.entrySet()) {
                try (var in = e.getValue().open();
                     var reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                    JsonObject o = JsonParser.parseReader(reader).getAsJsonObject();
                    String result = o.has("result") ? o.get("result").getAsString() : null;
                    if (result == null) {
                        continue;
                    }
                    result = stripId(result);
                    if (o.has("fossils") && o.get("fossils").isJsonArray()) {
                        JsonArray arr = o.getAsJsonArray("fossils");
                        for (JsonElement el : arr) {
                            map.put(stripId(el.getAsString()), result);
                        }
                    }
                    // Also map fossil file name
                    String file = e.getKey().getPath();
                    int slash = file.lastIndexOf('/');
                    String base = file.substring(slash + 1).replace(".json", "");
                    map.putIfAbsent(base + "_fossil", result);
                    map.putIfAbsent(base, result);
                } catch (Exception ex) {
                    Cobblemon.LOGGER.warn("Bad fossil recipe {}", e.getKey(), ex);
                }
            }
        } catch (Exception ex) {
            Cobblemon.LOGGER.warn("Fossil recipe scan failed", ex);
        }
        FOSSIL_TO_SPECIES = Collections.unmodifiableMap(map);
        loaded = true;
        Cobblemon.LOGGER.info("FossilRecipes: {} fossil item → species mappings", FOSSIL_TO_SPECIES.size());
    }

    /** Bootstrap from classpath datapack + hard defaults. */
    public static void bootstrapClasspath() {
        if (loaded && !FOSSIL_TO_SPECIES.isEmpty()) {
            return;
        }
        Map<String, String> map = new HashMap<>();
        // Hard defaults matching Cobblemon datapack names
        map.put("old_amber_fossil", "aerodactyl");
        map.put("helix_fossil", "omanyte");
        map.put("dome_fossil", "kabuto");
        map.put("root_fossil", "lileep");
        map.put("claw_fossil", "anorith");
        map.put("skull_fossil", "cranidos");
        map.put("armor_fossil", "shieldon");
        map.put("cover_fossil", "tirtouga");
        map.put("plume_fossil", "archen");
        map.put("jaw_fossil", "tyrunt");
        map.put("sail_fossil", "amaura");
        map.put("fossilized_bird", "dracozolt");
        map.put("fossilized_fish", "arctovish");
        map.put("fossilized_drake", "dracovish");
        map.put("fossilized_dino", "arctozolt");

        // Overlay any classpath fossils/*.json
        var cl = FossilRecipes.class.getClassLoader();
        String[] names = {
                "aerodactyl", "amaura", "anorith", "archen", "arctovish", "arctozolt",
                "cranidos", "dracovish", "dracozolt", "kabuto", "lileep", "omanyte",
                "shieldon", "tirtouga", "tyrunt"
        };
        for (String name : names) {
            try (var in = cl.getResourceAsStream("data/cobblemon/fossils/" + name + ".json")) {
                if (in == null) {
                    continue;
                }
                JsonObject o = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
                String result = o.has("result") ? stripId(o.get("result").getAsString()) : name;
                if (o.has("fossils") && o.get("fossils").isJsonArray()) {
                    for (JsonElement el : o.getAsJsonArray("fossils")) {
                        map.put(stripId(el.getAsString()), result);
                    }
                }
            } catch (Exception ignored) {
            }
        }

        FOSSIL_TO_SPECIES = Collections.unmodifiableMap(map);
        loaded = true;
        Cobblemon.LOGGER.info("FossilRecipes: {} mappings", FOSSIL_TO_SPECIES.size());
    }

    public static Optional<String> speciesForFossilItem(String itemId) {
        if (!loaded) {
            bootstrapClasspath();
        }
        if (itemId == null) {
            return Optional.empty();
        }
        String id = stripId(itemId);
        String direct = FOSSIL_TO_SPECIES.get(id);
        if (direct != null) {
            return Optional.of(direct);
        }
        for (var e : FOSSIL_TO_SPECIES.entrySet()) {
            if (id.contains(e.getKey()) || e.getKey().contains(id)) {
                return Optional.of(e.getValue());
            }
        }
        return Optional.empty();
    }

    private static String stripId(String raw) {
        String s = raw.toLowerCase(Locale.ROOT);
        int c = s.indexOf(':');
        return c >= 0 ? s.substring(c + 1) : s;
    }
}

package com.cobblemon.mod.species;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import com.cobblemon.mod.Cobblemon;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Loads Cobblemon {@code spawn_pool_world} entries from
 * {@code data/cobblemon/content/spawn_index.json}.
 * Falls back to {@link Gen1SpawnPool} if the index is missing.
 */
public final class SpawnPoolLoader {
    private static List<SpawnEntry> ENTRIES = List.of();
    private static boolean loaded;

    private SpawnPoolLoader() {}

    public static void bootstrap() {
        if (loaded) {
            return;
        }
        loaded = true;
        List<SpawnEntry> list = new ArrayList<>();
        try (var in = open("data/cobblemon/content/spawn_index.json")) {
            if (in == null) {
                Cobblemon.LOGGER.warn("spawn_index.json missing — using Gen1SpawnPool fallback");
                ENTRIES = Gen1SpawnPool.ENTRIES;
                return;
            }
            JsonObject root = JsonParser.parseReader(new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)))
                    .getAsJsonObject();
            JsonArray arr = root.getAsJsonArray("entries");
            int skippedDisabled = 0;
            if (arr != null) {
                for (JsonElement el : arr) {
                    SpawnEntry e = parse(el.getAsJsonObject());
                    if (e == null) {
                        continue;
                    }
                    // No real 3D model → never wild-spawn
                    if (DisabledSpecies.isDisabled(e.speciesId)) {
                        skippedDisabled++;
                        continue;
                    }
                    list.add(e);
                }
            }
            if (list.isEmpty()) {
                Cobblemon.LOGGER.warn("spawn_index empty — using Gen1SpawnPool fallback");
                ENTRIES = Gen1SpawnPool.ENTRIES;
            } else {
                ENTRIES = Collections.unmodifiableList(list);
                Cobblemon.LOGGER.info(
                        "SpawnPoolLoader: {} wild spawn rules (skipped {} no-model species)",
                        ENTRIES.size(),
                        skippedDisabled
                );
            }
        } catch (Exception ex) {
            Cobblemon.LOGGER.error("Failed loading spawn_index.json — Gen1 fallback", ex);
            ENTRIES = Gen1SpawnPool.ENTRIES;
        }
    }

    private static java.io.InputStream open(String path) {
        var cl = SpawnPoolLoader.class.getClassLoader();
        var in = cl.getResourceAsStream(path);
        if (in == null) {
            in = SpawnPoolLoader.class.getResourceAsStream("/" + path);
        }
        return in;
    }

    private static SpawnEntry parse(JsonObject j) {
        try {
            String speciesId = j.get("speciesId").getAsString().toLowerCase(Locale.ROOT);
            SpawnRarity bucket = switch (j.has("bucket") ? j.get("bucket").getAsString() : "common") {
                case "ultra_rare" -> SpawnRarity.ULTRA_RARE;
                case "rare" -> SpawnRarity.RARE;
                case "uncommon" -> SpawnRarity.UNCOMMON;
                default -> SpawnRarity.COMMON;
            };
            SpawnEntry.Position position = switch (j.has("position") ? j.get("position").getAsString() : "grounded") {
                case "submerged" -> SpawnEntry.Position.SUBMERGED;
                case "surface" -> SpawnEntry.Position.SURFACE;
                default -> SpawnEntry.Position.GROUNDED;
            };
            String[] keywords = stringArray(j, "biomeKeywords");
            return new SpawnEntry(
                    speciesId,
                    bucket,
                    j.has("weight") ? j.get("weight").getAsFloat() : 1f,
                    j.has("levelMin") ? j.get("levelMin").getAsInt() : 5,
                    j.has("levelMax") ? j.get("levelMax").getAsInt() : 20,
                    position,
                    EMPTY,
                    keywords,
                    j.has("anyOverworld") && j.get("anyOverworld").getAsBoolean(),
                    !j.has("overworld") || j.get("overworld").getAsBoolean(),
                    j.has("nether") && j.get("nether").getAsBoolean(),
                    j.has("end") && j.get("end").getAsBoolean(),
                    EMPTY,
                    EMPTY,
                    j.has("minSkyLight") ? j.get("minSkyLight").getAsInt() : -1,
                    j.has("maxSkyLight") ? j.get("maxSkyLight").getAsInt() : -1,
                    j.has("minY") ? j.get("minY").getAsInt() : -99999,
                    j.has("maxY") ? j.get("maxY").getAsInt() : 99999,
                    j.has("canSeeSky") ? j.get("canSeeSky").getAsInt() : -1,
                    SpawnEntry.TimeMode.ANY,
                    j.has("noRain") && j.get("noRain").getAsBoolean()
            );
        } catch (Exception e) {
            return null;
        }
    }

    private static final String[] EMPTY = new String[0];

    private static String[] stringArray(JsonObject j, String key) {
        if (!j.has(key) || !j.get(key).isJsonArray()) {
            return EMPTY;
        }
        return j.getAsJsonArray(key).asList().stream()
                .filter(JsonElement::isJsonPrimitive)
                .map(JsonElement::getAsString)
                .toArray(String[]::new);
    }

    public static List<SpawnEntry> entries() {
        if (!loaded) {
            bootstrap();
        }
        return ENTRIES;
    }
}

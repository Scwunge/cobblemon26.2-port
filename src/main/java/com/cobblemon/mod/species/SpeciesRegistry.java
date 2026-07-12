package com.cobblemon.mod.species;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import com.cobblemon.mod.Cobblemon;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Runtime registry of all Cobblemon species loaded from
 * {@code data/cobblemon/content/species_index.json} (built from porter species JSONs).
 * <p>
 * Gen1 {@link MonSpecies} remains the implemented runtime set; this registry supplies
 * authoritative base stats / types / catch rates for any known species id.
 */
public final class SpeciesRegistry {
    public record SpeciesData(
            String id,
            String name,
            int nationalPokedexNumber,
            String primaryType,
            String secondaryType,
            boolean implemented,
            float maleRatio,
            int height,
            int weight,
            int catchRate,
            int baseExperienceYield,
            int baseFriendship,
            float baseScale,
            List<String> labels,
            List<String> abilities,
            CobblemonBaseStats.Six baseStats,
            List<String> levelMoves
    ) {
        public MonElement primaryElement() {
            MonElement el = MonElement.byId(primaryType);
            return el != null ? el : MonElement.NORMAL;
        }

        public Optional<MonElement> secondaryElement() {
            if (secondaryType == null || secondaryType.isBlank()) {
                return Optional.empty();
            }
            MonElement el = MonElement.byId(secondaryType);
            return Optional.ofNullable(el);
        }
    }

    private static Map<String, SpeciesData> BY_ID = Map.of();
    private static boolean loaded;

    private SpeciesRegistry() {}

    public static void bootstrap() {
        if (loaded) {
            return;
        }
        loaded = true;
        Map<String, SpeciesData> map = new LinkedHashMap<>();
        try (var in = open("data/cobblemon/content/species_index.json")) {
            if (in == null) {
                Cobblemon.LOGGER.warn("species_index.json missing — run node tools/build_data_indexes.js");
                BY_ID = Map.of();
                return;
            }
            JsonObject root = JsonParser.parseReader(new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)))
                    .getAsJsonObject();
            JsonObject species = root.getAsJsonObject("species");
            if (species != null) {
                for (Map.Entry<String, JsonElement> e : species.entrySet()) {
                    SpeciesData data = parse(e.getKey(), e.getValue().getAsJsonObject());
                    if (data != null) {
                        map.put(data.id(), data);
                    }
                }
            }
            BY_ID = Collections.unmodifiableMap(map);
            Cobblemon.LOGGER.info("SpeciesRegistry: loaded {} species from datapack index", BY_ID.size());
        } catch (Exception ex) {
            Cobblemon.LOGGER.error("Failed loading species_index.json", ex);
            BY_ID = Map.of();
        }
    }

    private static java.io.InputStream open(String path) {
        var cl = SpeciesRegistry.class.getClassLoader();
        var in = cl.getResourceAsStream(path);
        if (in == null) {
            in = SpeciesRegistry.class.getResourceAsStream("/" + path);
        }
        return in;
    }

    private static SpeciesData parse(String id, JsonObject j) {
        try {
            JsonObject stats = j.has("baseStats") ? j.getAsJsonObject("baseStats") : new JsonObject();
            CobblemonBaseStats.Six six = new CobblemonBaseStats.Six(
                    intOr(stats, "hp", 50),
                    intOr(stats, "attack", 50),
                    intOr(stats, "defence", 50),
                    intOr(stats, "special_attack", 50),
                    intOr(stats, "special_defence", 50),
                    intOr(stats, "speed", 50),
                    intOr(j, "catchRate", 45)
            );
            List<String> labels = stringList(j, "labels");
            List<String> abilities = stringList(j, "abilities");
            List<String> moves = stringList(j, "moves");
            String secondary = j.has("secondaryType") && !j.get("secondaryType").isJsonNull()
                    ? j.get("secondaryType").getAsString()
                    : null;
            return new SpeciesData(
                    id.toLowerCase(Locale.ROOT),
                    j.has("name") ? j.get("name").getAsString() : id,
                    intOr(j, "nationalPokedexNumber", 0),
                    j.has("primaryType") ? j.get("primaryType").getAsString() : "normal",
                    secondary,
                    j.has("implemented") && j.get("implemented").getAsBoolean(),
                    j.has("maleRatio") ? j.get("maleRatio").getAsFloat() : 0.5f,
                    intOr(j, "height", 10),
                    intOr(j, "weight", 100),
                    intOr(j, "catchRate", 45),
                    intOr(j, "baseExperienceYield", 50),
                    intOr(j, "baseFriendship", 50),
                    j.has("baseScale") ? j.get("baseScale").getAsFloat() : 1f,
                    labels,
                    abilities,
                    six,
                    moves
            );
        } catch (Exception e) {
            return null;
        }
    }

    private static int intOr(JsonObject o, String key, int def) {
        return o.has(key) && o.get(key).isJsonPrimitive() ? o.get(key).getAsInt() : def;
    }

    private static List<String> stringList(JsonObject o, String key) {
        if (!o.has(key) || !o.get(key).isJsonArray()) {
            return List.of();
        }
        return o.getAsJsonArray(key).asList().stream()
                .filter(JsonElement::isJsonPrimitive)
                .map(JsonElement::getAsString)
                .toList();
    }

    public static Optional<SpeciesData> get(String id) {
        if (id == null) {
            return Optional.empty();
        }
        if (!loaded) {
            bootstrap();
        }
        return Optional.ofNullable(BY_ID.get(id.toLowerCase(Locale.ROOT)));
    }

    public static CobblemonBaseStats.Six statsOrNull(String id) {
        return get(id).map(SpeciesData::baseStats).orElse(null);
    }

    public static int size() {
        if (!loaded) {
            bootstrap();
        }
        return BY_ID.size();
    }

    public static Map<String, SpeciesData> all() {
        if (!loaded) {
            bootstrap();
        }
        return BY_ID;
    }
}

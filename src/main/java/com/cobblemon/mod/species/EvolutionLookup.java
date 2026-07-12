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
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Level-up evolution chains from Cobblemon species datapack.
 */
public final class EvolutionLookup {
    public record LevelEvo(String into, int level) {}

    private static Map<String, LevelEvo> BY_FROM = Map.of();
    private static boolean loaded;

    private EvolutionLookup() {}

    public static void bootstrap() {
        if (loaded) {
            return;
        }
        loaded = true;
        Map<String, LevelEvo> map = new HashMap<>();
        try (var in = open("data/cobblemon/content/evo_index.json")) {
            if (in == null) {
                Cobblemon.LOGGER.warn("evo_index.json missing");
                BY_FROM = Map.of();
                return;
            }
            JsonObject root = JsonParser.parseReader(new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)))
                    .getAsJsonObject();
            JsonObject evo = root.getAsJsonObject("evolutions");
            if (evo != null) {
                for (Map.Entry<String, JsonElement> e : evo.entrySet()) {
                    JsonObject o = e.getValue().getAsJsonObject();
                    map.put(e.getKey().toLowerCase(Locale.ROOT), new LevelEvo(
                            o.get("into").getAsString().toLowerCase(Locale.ROOT),
                            o.get("level").getAsInt()
                    ));
                }
            }
            BY_FROM = Collections.unmodifiableMap(map);
            Cobblemon.LOGGER.info("EvolutionLookup: {} level-up chains", BY_FROM.size());
        } catch (Exception ex) {
            Cobblemon.LOGGER.error("Failed loading evo_index.json", ex);
            BY_FROM = Map.of();
        }
    }

    private static java.io.InputStream open(String path) {
        var cl = EvolutionLookup.class.getClassLoader();
        var in = cl.getResourceAsStream(path);
        if (in == null) {
            in = EvolutionLookup.class.getResourceAsStream("/" + path);
        }
        return in;
    }

    public static Optional<String> levelUpTarget(String fromSpeciesId, int level) {
        if (!loaded) {
            bootstrap();
        }
        if (fromSpeciesId == null) {
            return Optional.empty();
        }
        LevelEvo e = BY_FROM.get(fromSpeciesId.toLowerCase(Locale.ROOT));
        if (e == null) {
            return Optional.empty();
        }
        if (level >= e.level()) {
            return Optional.of(e.into());
        }
        return Optional.empty();
    }

    public static int size() {
        if (!loaded) {
            bootstrap();
        }
        return BY_FROM.size();
    }
}

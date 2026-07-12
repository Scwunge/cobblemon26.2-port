package com.cobblemon.mod.species;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import com.cobblemon.mod.Cobblemon;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Species without real Cobblemon 3D models (placeholders only).
 * Disabled from wild spawns, commands, and any other content pickup.
 */
public final class DisabledSpecies {
    private static Set<String> DISABLED = Set.of();
    private static boolean loaded;

    private DisabledSpecies() {}

    public static void bootstrap() {
        if (loaded) {
            return;
        }
        loaded = true;
        Set<String> set = new HashSet<>();
        // Primary: explicit disabled list
        loadList(set, "data/cobblemon/content/disabled_species.json", "species");
        // Secondary: any remaining assets marked placeholder
        loadPlaceholdersFromAssets(set);
        DISABLED = Collections.unmodifiableSet(set);
        Cobblemon.LOGGER.info("DisabledSpecies: {} species without real models (no spawn/give)", DISABLED.size());
    }

    private static void loadList(Set<String> set, String path, String arrayKey) {
        try (var in = open(path)) {
            if (in == null) {
                return;
            }
            JsonObject root = JsonParser.parseReader(new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)))
                    .getAsJsonObject();
            JsonArray arr = root.getAsJsonArray(arrayKey);
            if (arr == null) {
                return;
            }
            for (JsonElement el : arr) {
                if (el.isJsonPrimitive()) {
                    set.add(el.getAsString().toLowerCase(Locale.ROOT));
                }
            }
        } catch (Exception ex) {
            Cobblemon.LOGGER.warn("Failed loading {}", path, ex);
        }
    }

    private static void loadPlaceholdersFromAssets(Set<String> set) {
        try (var in = open("data/cobblemon/content/species_assets.json")) {
            if (in == null) {
                return;
            }
            JsonObject root = JsonParser.parseReader(new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)))
                    .getAsJsonObject();
            JsonObject assets = root.getAsJsonObject("assets");
            if (assets == null) {
                return;
            }
            for (var e : assets.entrySet()) {
                JsonObject o = e.getValue().getAsJsonObject();
                if (o.has("placeholder") && o.get("placeholder").getAsBoolean()) {
                    set.add(e.getKey().toLowerCase(Locale.ROOT));
                }
            }
        } catch (Exception ex) {
            Cobblemon.LOGGER.warn("Failed scanning species_assets for placeholders", ex);
        }
    }

    private static java.io.InputStream open(String path) {
        var cl = DisabledSpecies.class.getClassLoader();
        var in = cl.getResourceAsStream(path);
        if (in == null) {
            in = DisabledSpecies.class.getResourceAsStream("/" + path);
        }
        return in;
    }

    public static boolean isDisabled(String speciesId) {
        if (!loaded) {
            bootstrap();
        }
        if (speciesId == null || speciesId.isBlank()) {
            return false;
        }
        return DISABLED.contains(speciesId.toLowerCase(Locale.ROOT).trim());
    }

    /** True if this mon has real art and is allowed in the world. */
    public static boolean isPlayable(String speciesId) {
        return speciesId != null && !speciesId.isBlank() && !isDisabled(speciesId);
    }

    public static int size() {
        if (!loaded) {
            bootstrap();
        }
        return DISABLED.size();
    }
}

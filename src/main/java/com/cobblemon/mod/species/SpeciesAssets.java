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
import net.minecraft.resources.Identifier;

/**
 * Maps species id → texture / geo / animation paths from scanned Cobblemon assets.
 */
public final class SpeciesAssets {
    public record Paths(String folder, int dex, String texture, String geo, String animation) {}

    private static Map<String, Paths> BY_ID = Map.of();
    private static boolean loaded;

    private SpeciesAssets() {}

    public static void bootstrap() {
        if (loaded) {
            return;
        }
        loaded = true;
        Map<String, Paths> map = new HashMap<>();
        try (var in = open("data/cobblemon/content/species_assets.json")) {
            if (in == null) {
                Cobblemon.LOGGER.warn("species_assets.json missing — run node tools/build_species_assets.js");
                BY_ID = Map.of();
                return;
            }
            JsonObject root = JsonParser.parseReader(new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)))
                    .getAsJsonObject();
            JsonObject assets = root.getAsJsonObject("assets");
            if (assets != null) {
                for (Map.Entry<String, JsonElement> e : assets.entrySet()) {
                    JsonObject o = e.getValue().getAsJsonObject();
                    map.put(e.getKey().toLowerCase(Locale.ROOT), new Paths(
                            o.has("folder") ? o.get("folder").getAsString() : e.getKey(),
                            o.has("dex") ? o.get("dex").getAsInt() : 0,
                            o.has("texture") && !o.get("texture").isJsonNull() ? o.get("texture").getAsString() : null,
                            o.has("geo") && !o.get("geo").isJsonNull() ? o.get("geo").getAsString() : null,
                            o.has("animation") && !o.get("animation").isJsonNull() ? o.get("animation").getAsString() : null
                    ));
                }
            }
            BY_ID = Collections.unmodifiableMap(map);
            Cobblemon.LOGGER.info("SpeciesAssets: {} species asset paths", BY_ID.size());
        } catch (Exception ex) {
            Cobblemon.LOGGER.error("Failed loading species_assets.json", ex);
            BY_ID = Map.of();
        }
    }

    private static java.io.InputStream open(String path) {
        var cl = SpeciesAssets.class.getClassLoader();
        var in = cl.getResourceAsStream(path);
        if (in == null) {
            in = SpeciesAssets.class.getResourceAsStream("/" + path);
        }
        return in;
    }

    public static Optional<Paths> get(String speciesId) {
        if (!loaded) {
            bootstrap();
        }
        if (speciesId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(BY_ID.get(speciesId.toLowerCase(Locale.ROOT)));
    }

    public static Identifier textureId(String speciesId) {
        Paths p = get(speciesId).orElse(null);
        if (p != null && p.texture() != null) {
            return Identifier.fromNamespaceAndPath(Cobblemon.MOD_ID, "textures/pokemon/" + p.texture());
        }
        return SpeciesTextures.texture(speciesId);
    }

    public static Identifier geoId(String speciesId) {
        Paths p = get(speciesId).orElse(null);
        if (p != null && p.geo() != null) {
            return Identifier.fromNamespaceAndPath(Cobblemon.MOD_ID, "bedrock/pokemon/models/" + p.geo());
        }
        return SpeciesTextures.geo(speciesId);
    }

    public static Identifier animationId(String speciesId) {
        Paths p = get(speciesId).orElse(null);
        if (p != null && p.animation() != null) {
            return Identifier.fromNamespaceAndPath(Cobblemon.MOD_ID, "bedrock/pokemon/animations/" + p.animation());
        }
        return SpeciesTextures.animation(speciesId);
    }

    public static int size() {
        if (!loaded) {
            bootstrap();
        }
        return BY_ID.size();
    }
}

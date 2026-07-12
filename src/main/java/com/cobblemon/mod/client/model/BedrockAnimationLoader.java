package com.cobblemon.mod.client.model;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.cobblemon.mod.Cobblemon;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.Mth;

/**
 * Loads Cobblemon/Blockbench Bedrock {@code .animation.json} and applies idle/walk
 * bone tracks. Supports static keyframes and a practical Molang subset
 * ({@code q.anim_time}, {@code math.sin/cos} in degrees, arithmetic).
 */
public final class BedrockAnimationLoader {
    private static final Map<Identifier, AnimationFile> CACHE = new ConcurrentHashMap<>();

    private BedrockAnimationLoader() {}

    public static AnimationFile getOrLoad(Identifier animId) {
        return CACHE.computeIfAbsent(animId, BedrockAnimationLoader::load);
    }

    public static void clearCache() {
        CACHE.clear();
    }

    private static AnimationFile load(Identifier animId) {
        try {
            var opt = Minecraft.getInstance().getResourceManager().getResource(animId);
            if (opt.isEmpty()) {
                return AnimationFile.EMPTY;
            }
            Resource res = opt.get();
            try (var in = res.open(); var reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                JsonObject animations = root.getAsJsonObject("animations");
                if (animations == null) {
                    return AnimationFile.EMPTY;
                }
                Map<String, Clip> clips = new HashMap<>();
                for (var e : animations.entrySet()) {
                    clips.put(e.getKey(), Clip.parse(e.getKey(), e.getValue().getAsJsonObject()));
                }
                return new AnimationFile(clips);
            }
        } catch (Exception ex) {
            Cobblemon.LOGGER.error("Failed to load animation {}", animId, ex);
            return AnimationFile.EMPTY;
        }
    }

    public record AnimationFile(Map<String, Clip> clips) {
        static final AnimationFile EMPTY = new AnimationFile(Map.of());

        public Clip findIdle() {
            // Prefer exact ground idle — avoid matching idle_quirk / battle_idle / air_idle
            Clip c = findExact("ground_idle");
            if (c != null) {
                return c;
            }
            c = findExact("standing");
            if (c != null) {
                return c;
            }
            return findExact("battle_idle");
        }

        public Clip findWalk() {
            // Prefer walk over run — run is intentionally faster and looked "sped up"
            // when we used it as the default locomotion clip.
            Clip c = findExact("ground_walk");
            if (c != null) {
                return c;
            }
            c = findExact("walk");
            if (c != null) {
                return c;
            }
            // Last resort only
            return findExact("ground_run");
        }

        /** Air idle while hovering / held aloft (porter air_idle). */
        public Clip findAirIdle() {
            Clip c = findExact("air_idle");
            if (c != null) {
                return c;
            }
            return findExact("hover");
        }

        /**
         * Forward flight — prefer full {@code air_fly} (many bones).
         * {@code ride_air_fly} is sparse and T-poses Charizard-style geos.
         */
        public Clip findAirFly() {
            Clip c = findExact("air_fly");
            if (c != null) {
                return c;
            }
            c = findExact("air_glide");
            if (c != null) {
                return c;
            }
            return findExact("ride_air_fly");
        }

        /** Dive while flying. */
        public Clip findAirDive() {
            Clip c = findExact("air_fly"); // full wing set; ride_air_dive is sparse
            if (c != null) {
                // Prefer dedicated dive if present with real content later
            }
            Clip dive = findExact("air_dive");
            if (dive != null) {
                return dive;
            }
            dive = findExact("ride_air_dive");
            if (dive != null) {
                return dive;
            }
            return findAirFly();
        }

        /** Ground ride run (porter ride_ground_run). */
        public Clip findRideGround() {
            Clip c = findExact("ride_ground_run");
            if (c != null) {
                return c;
            }
            return findWalk();
        }

        /** Match animation.&lt;species&gt;.&lt;name&gt; exactly by suffix. */
        public Clip findExact(String poseName) {
            String suffix = "." + poseName.toLowerCase(Locale.ROOT);
            for (var e : clips.entrySet()) {
                if (e.getKey().toLowerCase(Locale.ROOT).endsWith(suffix)) {
                    return e.getValue();
                }
            }
            return null;
        }
    }

    public static final class Clip {
        final String name;
        final boolean loop;
        final float lengthSec;
        final Map<String, BoneTrack> bones;

        Clip(String name, boolean loop, float lengthSec, Map<String, BoneTrack> bones) {
            this.name = name;
            this.loop = loop;
            this.lengthSec = lengthSec <= 0f ? 1f : lengthSec;
            this.bones = bones;
        }

        static Clip parse(String name, JsonObject o) {
            boolean loop = !o.has("loop") || o.get("loop").getAsBoolean()
                    || (o.has("loop") && o.get("loop").isJsonPrimitive() && "true".equalsIgnoreCase(o.get("loop").getAsString()));
            if (o.has("loop") && o.get("loop").isJsonPrimitive() && o.get("loop").getAsJsonPrimitive().isBoolean()) {
                loop = o.get("loop").getAsBoolean();
            }
            float length = o.has("animation_length") ? o.get("animation_length").getAsFloat() : 1f;
            Map<String, BoneTrack> bones = new HashMap<>();
            if (o.has("bones")) {
                for (var e : o.getAsJsonObject("bones").entrySet()) {
                    bones.put(sanitize(e.getKey()), BoneTrack.parse(e.getValue().getAsJsonObject()));
                }
            }
            return new Clip(name, loop, length, bones);
        }

        /** Apply bone transforms onto a geo root for the given age (ticks). */
        public void apply(ModelPart root, float ageInTicks) {
            float seconds = ageInTicks / 20f;
            float t = loop ? seconds % lengthSec : Math.min(seconds, lengthSec);
            for (var e : bones.entrySet()) {
                ModelPart part = findPart(root, e.getKey());
                if (part == null) {
                    continue;
                }
                float[] rot = e.getValue().rotation.sample(t);
                float[] pos = e.getValue().position.sample(t);
                // Cobblemon BedrockAnimation: degrees → radians, no axis flips on rotation
                part.xRot += BedrockGeoLoader.boneRotX(rot[0]);
                part.yRot += BedrockGeoLoader.boneRotY(rot[1]);
                part.zRot += BedrockGeoLoader.boneRotZ(rot[2]);
                // Position: Cobblemon MolangBoneValue multiplies Y by -1 for POSITION
                // (ModelPart Y is inverted vs Bedrock after TexturedModel bake).
                // Without this, floating bodyparts (e.g. Flabébé) sit far above the stem.
                part.x += pos[0];
                part.y += -pos[1];
                part.z += pos[2];
            }
        }
    }

    private static ModelPart findPart(ModelPart root, String name) {
        if (root.hasChild(name)) {
            return root.getChild(name);
        }
        for (ModelPart p : root.getAllParts()) {
            if (p.hasChild(name)) {
                return p.getChild(name);
            }
        }
        return null;
    }

    static String sanitize(String name) {
        return name.replaceAll("[^a-zA-Z0-9_]", "_");
    }

    private static final class BoneTrack {
        final Channel rotation;
        final Channel position;

        BoneTrack(Channel rotation, Channel position) {
            this.rotation = rotation;
            this.position = position;
        }

        static BoneTrack parse(JsonObject o) {
            return new BoneTrack(
                    Channel.parse(o.get("rotation")),
                    Channel.parse(o.get("position"))
            );
        }
    }

    private static final class Channel {
        /** Per-axis live expressions (null = use keyframes). */
        final String[] expr;
        final List<Keyframe> keys;

        Channel(String[] expr, List<Keyframe> keys) {
            this.expr = expr;
            this.keys = keys;
        }

        static Channel parse(JsonElement el) {
            if (el == null || el.isJsonNull()) {
                return new Channel(new String[]{"0", "0", "0"}, List.of());
            }
            if (el.isJsonArray()) {
                JsonArray a = el.getAsJsonArray();
                return new Channel(new String[]{
                        componentToString(a, 0),
                        componentToString(a, 1),
                        componentToString(a, 2)
                }, List.of());
            }
            if (el.isJsonObject()) {
                List<Keyframe> keys = new ArrayList<>();
                for (var e : el.getAsJsonObject().entrySet()) {
                    try {
                        float time = Float.parseFloat(e.getKey());
                        keys.add(new Keyframe(time, readVecExpr(e.getValue())));
                    } catch (NumberFormatException ignored) {
                    }
                }
                keys.sort((a, b) -> Float.compare(a.time, b.time));
                return new Channel(null, keys);
            }
            return new Channel(new String[]{"0", "0", "0"}, List.of());
        }

        float[] sample(float animTimeSec) {
            if (expr != null) {
                return new float[]{
                        Molang.eval(expr[0], animTimeSec),
                        Molang.eval(expr[1], animTimeSec),
                        Molang.eval(expr[2], animTimeSec)
                };
            }
            if (keys.isEmpty()) {
                return new float[]{0, 0, 0};
            }
            if (keys.size() == 1) {
                return evalKey(keys.get(0), animTimeSec);
            }
            if (animTimeSec <= keys.get(0).time) {
                return evalKey(keys.get(0), animTimeSec);
            }
            if (animTimeSec >= keys.get(keys.size() - 1).time) {
                return evalKey(keys.get(keys.size() - 1), animTimeSec);
            }
            for (int i = 0; i < keys.size() - 1; i++) {
                Keyframe a = keys.get(i);
                Keyframe b = keys.get(i + 1);
                if (animTimeSec >= a.time && animTimeSec <= b.time) {
                    float u = (animTimeSec - a.time) / Math.max(1e-5f, b.time - a.time);
                    float[] av = evalKey(a, animTimeSec);
                    float[] bv = evalKey(b, animTimeSec);
                    return new float[]{
                            Mth.lerp(u, av[0], bv[0]),
                            Mth.lerp(u, av[1], bv[1]),
                            Mth.lerp(u, av[2], bv[2])
                    };
                }
            }
            return evalKey(keys.get(0), animTimeSec);
        }

        private static float[] evalKey(Keyframe k, float t) {
            return new float[]{
                    Molang.eval(k.x, t),
                    Molang.eval(k.y, t),
                    Molang.eval(k.z, t)
            };
        }

        private static String componentToString(JsonArray a, int i) {
            if (i >= a.size()) {
                return "0";
            }
            JsonElement e = a.get(i);
            if (e.isJsonPrimitive() && e.getAsJsonPrimitive().isNumber()) {
                return Float.toString(e.getAsFloat());
            }
            return e.getAsString();
        }

        private static String[] readVecExpr(JsonElement el) {
            if (el.isJsonArray()) {
                JsonArray a = el.getAsJsonArray();
                return new String[]{componentToString(a, 0), componentToString(a, 1), componentToString(a, 2)};
            }
            if (el.isJsonObject()) {
                JsonObject o = el.getAsJsonObject();
                JsonElement post = o.has("post") ? o.get("post") : o.get("pre");
                if (post != null && post.isJsonArray()) {
                    JsonArray a = post.getAsJsonArray();
                    return new String[]{componentToString(a, 0), componentToString(a, 1), componentToString(a, 2)};
                }
            }
            return new String[]{"0", "0", "0"};
        }
    }

    private record Keyframe(float time, String x, String y, String z) {
        Keyframe(float time, String[] v) {
            this(time, v[0], v[1], v[2]);
        }
    }

    /** Minimal Molang evaluator for Cobblemon animation expressions. */
    public static final class Molang {
        private Molang() {}

        public static float eval(String expr, float animTime) {
            if (expr == null || expr.isBlank()) {
                return 0f;
            }
            try {
                String s = expr.trim();
                s = s.replace("q.anim_time", Float.toString(animTime));
                s = s.replace("query.anim_time", Float.toString(animTime));
                s = expandMath(s, "math.sin", true);
                s = expandMath(s, "math.cos", false);
                return (float) arithmetic(s);
            } catch (Exception e) {
                return 0f;
            }
        }

        private static String expandMath(String s, String fn, boolean sin) {
            String out = s;
            int guard = 0;
            while (guard++ < 48) {
                int idx = out.toLowerCase(Locale.ROOT).indexOf(fn);
                if (idx < 0) {
                    break;
                }
                int open = out.indexOf('(', idx);
                if (open < 0) {
                    break;
                }
                int close = matchParen(out, open);
                if (close < 0) {
                    break;
                }
                String inner = out.substring(open + 1, close);
                float arg = eval(inner, 0f); // inner should already have time substituted
                // re-eval with original time: parse float from substituted string
                try {
                    arg = (float) arithmetic(inner);
                } catch (Exception ignored) {
                }
                float v = sin
                        ? (float) Math.sin(arg * Math.PI / 180.0)
                        : (float) Math.cos(arg * Math.PI / 180.0);
                out = out.substring(0, idx) + v + out.substring(close + 1);
            }
            return out;
        }

        private static int matchParen(String s, int open) {
            int d = 0;
            for (int i = open; i < s.length(); i++) {
                if (s.charAt(i) == '(') {
                    d++;
                } else if (s.charAt(i) == ')') {
                    d--;
                    if (d == 0) {
                        return i;
                    }
                }
            }
            return -1;
        }

        private static double arithmetic(String expr) {
            return new Parser(expr).parse();
        }

        private static final class Parser {
            final String expr;
            int pos = -1;
            int ch;

            Parser(String expr) {
                this.expr = expr;
            }

            void next() {
                ch = (++pos < expr.length()) ? expr.charAt(pos) : -1;
            }

            boolean eat(int c) {
                while (ch == ' ') {
                    next();
                }
                if (ch == c) {
                    next();
                    return true;
                }
                return false;
            }

            double parse() {
                next();
                double x = parseExpr();
                return x;
            }

            double parseExpr() {
                double x = parseTerm();
                for (;;) {
                    if (eat('+')) {
                        x += parseTerm();
                    } else if (eat('-')) {
                        x -= parseTerm();
                    } else {
                        return x;
                    }
                }
            }

            double parseTerm() {
                double x = parseFactor();
                for (;;) {
                    if (eat('*')) {
                        x *= parseFactor();
                    } else if (eat('/')) {
                        x /= parseFactor();
                    } else {
                        return x;
                    }
                }
            }

            double parseFactor() {
                if (eat('+')) {
                    return parseFactor();
                }
                if (eat('-')) {
                    return -parseFactor();
                }
                double x;
                int start = pos;
                if (eat('(')) {
                    x = parseExpr();
                    eat(')');
                } else if ((ch >= '0' && ch <= '9') || ch == '.') {
                    while ((ch >= '0' && ch <= '9') || ch == '.' || ch == 'e' || ch == 'E' || ch == '-') {
                        // stop on second '-' not part of exponent
                        if (ch == '-' && pos > start && expr.charAt(pos - 1) != 'e' && expr.charAt(pos - 1) != 'E') {
                            break;
                        }
                        next();
                    }
                    x = Double.parseDouble(expr.substring(start, pos));
                } else {
                    // unknown token → 0
                    next();
                    x = 0;
                }
                return x;
            }
        }
    }
}

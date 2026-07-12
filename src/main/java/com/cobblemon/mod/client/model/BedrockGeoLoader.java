package com.cobblemon.mod.client.model;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.cobblemon.mod.Cobblemon;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.Mth;

/**
 * Loads Cobblemon/Blockbench Bedrock {@code .geo.json} into a Java {@link ModelPart} tree.
 * <p>
 * Conversion matches Cobblemon's {@code TexturedModel.create()} so LivingEntity's
 * {@code scale(-1,-1,1)} works without extra Y cancels:
 * <ul>
 *   <li>Child pivot: {@code (bx-px, py-by, bz-pz)} — Y is inverted vs Bedrock</li>
 *   <li>Root bones: offset (0,0,0)</li>
 *   <li>Cube box: {@code (ox-px, -(oy-py+sizeY), oz-pz)}</li>
 *   <li>Bone/cube rotations: degrees → radians, <b>no</b> axis negation</li>
 * </ul>
 */
public final class BedrockGeoLoader {
    private static final Map<Identifier, ModelPart> CACHE = new ConcurrentHashMap<>();
    private static final Map<Identifier, Boolean> HAS_MESH = new ConcurrentHashMap<>();

    private BedrockGeoLoader() {}

    public static ModelPart getOrLoad(Identifier geoId) {
        return CACHE.computeIfAbsent(geoId, BedrockGeoLoader::load);
    }

    public static boolean hasMesh(Identifier geoId) {
        // Avoid full load on every query once we know the answer
        Boolean known = HAS_MESH.get(geoId);
        if (known != null) {
            return known;
        }
        getOrLoad(geoId);
        return HAS_MESH.getOrDefault(geoId, false);
    }

    public static void clearCache() {
        CACHE.clear();
        HAS_MESH.clear();
    }

    /**
     * Cobblemon-compatible: plain degrees → radians (no axis flips).
     * Used by both geo bake and animation application.
     */
    public static float boneRotX(float bedrockDeg) {
        return bedrockDeg * Mth.DEG_TO_RAD;
    }

    public static float boneRotY(float bedrockDeg) {
        return bedrockDeg * Mth.DEG_TO_RAD;
    }

    public static float boneRotZ(float bedrockDeg) {
        return bedrockDeg * Mth.DEG_TO_RAD;
    }

    private static ModelPart load(Identifier geoId) {
        try {
            var opt = Minecraft.getInstance().getResourceManager().getResource(geoId);
            if (opt.isEmpty()) {
                Cobblemon.LOGGER.warn("Missing geo: {}", geoId);
                HAS_MESH.put(geoId, false);
                return emptyRoot();
            }
            Resource res = opt.get();
            try (var in = res.open(); var reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                ModelPart baked = bake(root);
                HAS_MESH.put(geoId, baked.getAllParts().size() > 1);
                return baked;
            }
        } catch (Exception e) {
            Cobblemon.LOGGER.error("Failed to load geo {}", geoId, e);
            HAS_MESH.put(geoId, false);
            return emptyRoot();
        }
    }

    private static ModelPart emptyRoot() {
        return LayerDefinition.create(new MeshDefinition(), 64, 64).bakeRoot();
    }

    private static ModelPart bake(JsonObject root) {
        JsonArray geometries = root.getAsJsonArray("minecraft:geometry");
        if (geometries == null || geometries.isEmpty()) {
            return emptyRoot();
        }
        JsonObject geometry = geometries.get(0).getAsJsonObject();
        JsonObject desc = geometry.getAsJsonObject("description");
        int texW = desc.has("texture_width") ? desc.get("texture_width").getAsInt() : 64;
        int texH = desc.has("texture_height") ? desc.get("texture_height").getAsInt() : 64;

        JsonArray bones = geometry.getAsJsonArray("bones");
        if (bones == null) {
            return emptyRoot();
        }

        Map<String, BoneData> boneMap = new HashMap<>();
        List<String> order = new ArrayList<>();
        for (JsonElement el : bones) {
            JsonObject b = el.getAsJsonObject();
            String name = b.get("name").getAsString();
            String parent = b.has("parent") ? b.get("parent").getAsString() : null;
            float[] pivot = readVec3(b, "pivot", 0, 0, 0);
            float[] rot = readVec3(b, "rotation", 0, 0, 0);
            List<CubeData> cubes = new ArrayList<>();
            if (b.has("cubes")) {
                for (JsonElement ce : b.getAsJsonArray("cubes")) {
                    cubes.add(CubeData.from(ce.getAsJsonObject()));
                }
            }
            boneMap.put(name, new BoneData(name, parent, pivot, rot, cubes));
            order.add(name);
        }

        MeshDefinition mesh = new MeshDefinition();
        PartDefinition rootDef = mesh.getRoot();
        Map<String, PartDefinition> parts = new HashMap<>();
        AtomicInteger cubePartSeq = new AtomicInteger();

        int guard = 0;
        List<String> remaining = new ArrayList<>(order);
        while (!remaining.isEmpty() && guard++ < 128) {
            List<String> next = new ArrayList<>();
            for (String name : remaining) {
                BoneData bone = boneMap.get(name);
                PartDefinition parentDef;
                float px, py, pz;

                if (bone.parent == null || bone.parent.isEmpty()) {
                    // Cobblemon: root bones always at origin; world position lives in children
                    parentDef = rootDef;
                    px = 0f;
                    py = 0f;
                    pz = 0f;
                } else if (parts.containsKey(bone.parent)) {
                    parentDef = parts.get(bone.parent);
                    BoneData parentBone = boneMap.get(bone.parent);
                    // Cobblemon: (bx-px, py-by, bz-pz) — Y inverted vs Bedrock delta
                    px = bone.pivot[0] - parentBone.pivot[0];
                    py = parentBone.pivot[1] - bone.pivot[1];
                    pz = bone.pivot[2] - parentBone.pivot[2];
                } else {
                    next.add(name);
                    continue;
                }

                CubeListBuilder simple = CubeListBuilder.create();
                List<CubeData> rotated = new ArrayList<>();
                for (CubeData cube : bone.cubes) {
                    if (cube.rotation != null && nonzero(cube.rotation)) {
                        rotated.add(cube);
                        continue;
                    }
                    addCubeToBuilder(simple, cube, bone.pivot);
                }

                boolean hasBoneRot = nonzero(bone.rot);
                PartDefinition bonePart = parentDef.addOrReplaceChild(
                        sanitize(name),
                        simple,
                        hasBoneRot
                                ? PartPose.offsetAndRotation(
                                        px, py, pz,
                                        boneRotX(bone.rot[0]),
                                        boneRotY(bone.rot[1]),
                                        boneRotZ(bone.rot[2])
                                )
                                : PartPose.offset(px, py, pz)
                );
                parts.put(name, bonePart);

                // Rotated cubes as child parts (own pivot + rotation) — Cobblemon style
                for (CubeData cube : rotated) {
                    float[] cp = cube.pivot != null ? cube.pivot : bone.pivot;
                    float cpx = cp[0] - bone.pivot[0];
                    float cpy = bone.pivot[1] - cp[1]; // Y invert
                    float cpz = cp[2] - bone.pivot[2];
                    CubeListBuilder cb = CubeListBuilder.create();
                    addCubeToBuilder(cb, cube, cp);
                    String cname = sanitize(name) + "_cube" + cubePartSeq.incrementAndGet();
                    bonePart.addOrReplaceChild(
                            cname,
                            cb,
                            PartPose.offsetAndRotation(
                                    cpx, cpy, cpz,
                                    boneRotX(cube.rotation[0]),
                                    boneRotY(cube.rotation[1]),
                                    boneRotZ(cube.rotation[2])
                            )
                    );
                }
            }
            if (next.size() == remaining.size()) {
                for (String name : next) {
                    BoneData bone = boneMap.get(name);
                    PartDefinition child = rootDef.addOrReplaceChild(
                            sanitize(name),
                            CubeListBuilder.create(),
                            PartPose.offset(0, 0, 0)
                    );
                    parts.put(name, child);
                }
                break;
            }
            remaining = next;
        }

        return LayerDefinition.create(mesh, texW, texH).bakeRoot();
    }

    /**
     * Cobblemon cube formula:
     * {@code ox = origin.x - pivot.x}
     * {@code oy = -(origin.y - pivot.y + size.y)}
     * {@code oz = origin.z - pivot.z}
     */
    private static void addCubeToBuilder(CubeListBuilder builder, CubeData cube, float[] pivot) {
        // Blockbench "planes" use size 0 on one axis (Rattata tail, whiskers, many fins).
        // Java Edition drops zero-thickness faces, so they vanish from some angles.
        // Expand those axes slightly, centered on the original plane.
        float sx = cube.size[0];
        float sy = cube.size[1];
        float sz = cube.size[2];
        float ox0 = cube.origin[0];
        float oy0 = cube.origin[1];
        float oz0 = cube.origin[2];
        final float minPlane = 0.05f;
        if (Math.abs(sx) < 1e-4f) {
            sx = minPlane;
            ox0 -= minPlane * 0.5f;
        }
        if (Math.abs(sy) < 1e-4f) {
            sy = minPlane;
            oy0 -= minPlane * 0.5f;
        }
        if (Math.abs(sz) < 1e-4f) {
            sz = minPlane;
            oz0 -= minPlane * 0.5f;
        }

        float ox = ox0 - pivot[0];
        float oy = -(oy0 - pivot[1] + sy);
        float oz = oz0 - pivot[2];
        builder.texOffs((int) cube.uv[0], (int) cube.uv[1]);
        if (cube.mirror) {
            builder.mirror();
        }
        builder.addBox(ox, oy, oz, sx, sy, sz, new CubeDeformation(cube.inflate));
    }

    public static String sanitize(String name) {
        return name.replaceAll("[^a-zA-Z0-9_]", "_");
    }

    private static boolean nonzero(float[] r) {
        return Math.abs(r[0]) > 0.01f || Math.abs(r[1]) > 0.01f || Math.abs(r[2]) > 0.01f;
    }

    private static float[] readVec3(JsonObject o, String key, float dx, float dy, float dz) {
        if (!o.has(key)) {
            return new float[]{dx, dy, dz};
        }
        JsonArray a = o.getAsJsonArray(key);
        return new float[]{
                a.size() > 0 ? a.get(0).getAsFloat() : dx,
                a.size() > 1 ? a.get(1).getAsFloat() : dy,
                a.size() > 2 ? a.get(2).getAsFloat() : dz
        };
    }

    private record BoneData(String name, String parent, float[] pivot, float[] rot, List<CubeData> cubes) {}

    private static final class CubeData {
        final float[] origin;
        final float[] size;
        final float[] uv;
        final float inflate;
        final boolean mirror;
        final float[] rotation;
        final float[] pivot;

        private CubeData(float[] origin, float[] size, float[] uv, float inflate, boolean mirror,
                         float[] rotation, float[] pivot) {
            this.origin = origin;
            this.size = size;
            this.uv = uv;
            this.inflate = inflate;
            this.mirror = mirror;
            this.rotation = rotation;
            this.pivot = pivot;
        }

        static CubeData from(JsonObject c) {
            float[] origin = readArr(c, "origin", 0, 0, 0);
            float[] size = readArr(c, "size", 1, 1, 1);
            float inflate = c.has("inflate") ? c.get("inflate").getAsFloat() : 0f;
            boolean mirror = c.has("mirror") && c.get("mirror").getAsBoolean();
            float[] rotation = c.has("rotation") ? readArr(c, "rotation", 0, 0, 0) : null;
            float[] pivot = c.has("pivot") ? readArr(c, "pivot", 0, 0, 0) : null;
            float[] uv = new float[]{0, 0};
            if (c.has("uv")) {
                JsonElement u = c.get("uv");
                if (u.isJsonArray()) {
                    JsonArray a = u.getAsJsonArray();
                    uv[0] = a.get(0).getAsFloat();
                    uv[1] = a.size() > 1 ? a.get(1).getAsFloat() : 0;
                } else if (u.isJsonObject()) {
                    for (String face : List.of("north", "up", "east", "south", "west", "down")) {
                        if (u.getAsJsonObject().has(face)) {
                            JsonObject f = u.getAsJsonObject().getAsJsonObject(face);
                            if (f.has("uv")) {
                                JsonArray a = f.getAsJsonArray("uv");
                                uv[0] = a.get(0).getAsFloat();
                                uv[1] = a.get(1).getAsFloat();
                                break;
                            }
                        }
                    }
                    if (uv[0] == 0 && uv[1] == 0) {
                        for (var e : u.getAsJsonObject().entrySet()) {
                            JsonObject face = e.getValue().getAsJsonObject();
                            if (face.has("uv")) {
                                JsonArray a = face.getAsJsonArray("uv");
                                uv[0] = a.get(0).getAsFloat();
                                uv[1] = a.get(1).getAsFloat();
                                break;
                            }
                        }
                    }
                }
            }
            return new CubeData(origin, size, uv, inflate, mirror, rotation, pivot);
        }

        private static float[] readArr(JsonObject o, String key, float dx, float dy, float dz) {
            JsonArray a = o.getAsJsonArray(key);
            return new float[]{
                    a.size() > 0 ? a.get(0).getAsFloat() : dx,
                    a.size() > 1 ? a.get(1).getAsFloat() : dy,
                    a.size() > 2 ? a.get(2).getAsFloat() : dz
            };
        }
    }
}

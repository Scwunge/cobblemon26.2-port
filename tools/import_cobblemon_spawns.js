/**
 * Import Cobblemon 1.7.3 Gen 1 spawn pools + base stats into Fakemons.
 *
 * Source: C:\Users\conne\Desktop\porter
 * Output:
 *   src/main/java/ai/xai/fakemon/species/Gen1SpawnPool.java
 *   src/main/java/ai/xai/fakemon/species/CobblemonBaseStats.java
 *
 * Usage: node tools/import_cobblemon_spawns.js
 */
const fs = require("fs");
const path = require("path");

const PORTER = process.env.PORTER || "C:\\Users\\conne\\Desktop\\porter";
const OUT_DIR = path.join(__dirname, "..", "src", "main", "java", "ai", "xai", "fakemon", "species");
const SPAWN_DIR = path.join(PORTER, "11_data_spawning", "spawn_pool_world");
const BIOME_TAG_DIR = path.join(PORTER, "12_data_tags", "worldgen", "biome");
const SPECIES_DIR = path.join(PORTER, "09_data_species", "species", "generation1");

// Keyword fallbacks when a tag expands to zero vanilla biomes (mod-only tags).
const TAG_KEYWORDS = {
  is_arid: ["desert", "badland", "savanna"],
  is_badlands: ["badland"],
  is_bamboo: ["bamboo", "jungle"],
  is_beach: ["beach"],
  is_cave: ["cave", "dripstone", "lush_caves", "deep_dark"],
  is_cherry_blossom: ["cherry"],
  is_coast: ["beach", "stony_shore"],
  is_cold: ["snow", "ice", "frozen", "cold", "taiga"],
  is_cold_ocean: ["cold_ocean", "frozen_ocean", "deep_cold"],
  is_deep_dark: ["deep_dark"],
  is_deep_ocean: ["deep_ocean", "deep_cold", "deep_lukewarm", "deep_frozen"],
  is_desert: ["desert"],
  is_dripstone: ["dripstone"],
  is_end: ["end"],
  is_floral: ["flower", "meadow", "cherry", "sunflower"],
  is_forest: ["forest", "taiga", "birch", "dark_forest", "wooded", "grove"],
  is_freezing: ["snow", "ice", "frozen", "cold"],
  is_freshwater: ["river", "swamp", "mangrove"],
  is_frozen_ocean: ["frozen_ocean", "deep_frozen"],
  is_glacial: ["ice", "frozen", "snow"],
  is_grassland: ["plains", "meadow", "savanna", "sunflower"],
  is_highlands: ["windswept", "plateau", "meadow", "peak", "hill"],
  is_hills: ["hill", "windswept", "plateau"],
  is_island: ["island", "mushroom"],
  is_jungle: ["jungle", "bamboo"],
  is_lukewarm_ocean: ["lukewarm_ocean", "warm_ocean", "deep_lukewarm"],
  is_lush: ["lush", "jungle", "mangrove"],
  is_magical: ["mushroom", "dark_forest", "cherry"],
  is_mountain: ["mountain", "peak", "windswept", "stony", "meadow", "grove", "slope"],
  is_mushroom: ["mushroom"],
  is_ocean: ["ocean", "deep_ocean", "warm_ocean", "lukewarm", "cold_ocean", "frozen_ocean"],
  is_overworld: ["*overworld*"],
  is_peak: ["peaks", "frozen_peaks", "jagged_peaks", "stony_peaks", "snowy_slopes"],
  is_plains: ["plains", "sunflower", "meadow"],
  is_plateau: ["plateau", "savanna_plateau", "badlands_plateau", "wooded_badlands"],
  is_river: ["river"],
  is_sandy: ["desert", "beach", "badland"],
  is_savanna: ["savanna"],
  is_shrubland: ["savanna", "plains", "windswept"],
  is_sky: ["*open*"],
  is_snowy: ["snow", "ice", "frozen", "grove"],
  is_snowy_forest: ["snowy_taiga", "grove", "snowy"],
  is_spooky: ["dark_forest", "deep_dark", "swamp"],
  is_swamp: ["swamp", "mangrove"],
  is_taiga: ["taiga", "grove"],
  is_temperate: ["plains", "forest", "birch", "meadow", "river"],
  is_thermal: ["basalt", "nether"],
  is_tropical: ["jungle", "bamboo", "mangrove", "warm_ocean"],
  is_tundra: ["snow", "ice", "frozen", "windswept"],
  is_volcanic: ["basalt", "nether_wastes", "soul_sand", "crimson", "warped"],
  is_warm_ocean: ["warm_ocean", "lukewarm_ocean"],
};

// Vanilla minecraft biomes for expanding #minecraft: tags we know.
const VANILLA_BIOMES = {
  is_forest: [
    "forest", "flower_forest", "birch_forest", "old_growth_birch_forest",
    "dark_forest", "grove", "pale_garden",
  ],
  is_hill: [
    "windswept_hills", "windswept_forest", "windswept_gravelly_hills",
    "meadow", "cherry_grove", "grove", "snowy_slopes",
  ],
  is_mountain: [
    "meadow", "frozen_peaks", "jagged_peaks", "stony_peaks", "snowy_slopes",
    "grove", "cherry_grove",
  ],
  is_ocean: [
    "ocean", "deep_ocean", "warm_ocean", "lukewarm_ocean", "deep_lukewarm_ocean",
    "cold_ocean", "deep_cold_ocean", "frozen_ocean", "deep_frozen_ocean",
  ],
  is_river: ["river", "frozen_river"],
  is_beach: ["beach", "snowy_beach", "stony_shore"],
  is_badlands: ["badlands", "eroded_badlands", "wooded_badlands"],
  is_jungle: ["jungle", "sparse_jungle", "bamboo_jungle"],
  is_nether: [
    "nether_wastes", "soul_sand_valley", "crimson_forest", "warped_forest", "basalt_deltas",
  ],
  is_end: ["the_end", "end_highlands", "end_midlands", "end_barrens", "small_end_islands"],
  is_overworld: ["*overworld*"],
  is_savanna: ["savanna", "savanna_plateau", "windswept_savanna"],
  is_taiga: ["taiga", "old_growth_pine_taiga", "old_growth_spruce_taiga", "snowy_taiga"],
};

function walkJsonFiles(dir) {
  const out = [];
  if (!fs.existsSync(dir)) return out;
  for (const ent of fs.readdirSync(dir, { withFileTypes: true })) {
    const p = path.join(dir, ent.name);
    if (ent.isDirectory()) out.push(...walkJsonFiles(p));
    else if (ent.name.endsWith(".json")) out.push(p);
  }
  return out;
}

function tagKeyFromRef(ref) {
  // #cobblemon:is_forest -> is_forest
  // #cobblemon:nether/is_basalt -> nether/is_basalt
  // #minecraft:is_forest -> minecraft:is_forest
  if (!ref.startsWith("#")) return null;
  const body = ref.slice(1);
  if (body.startsWith("cobblemon:")) return body.slice("cobblemon:".length);
  if (body.startsWith("minecraft:")) return "minecraft:" + body.slice("minecraft:".length);
  return body; // c: etc ignored later
}

/** Load cobblemon biome tag files into map: tagPath -> raw values */
function loadBiomeTags() {
  const tags = new Map();
  for (const file of walkJsonFiles(BIOME_TAG_DIR)) {
    const rel = path.relative(BIOME_TAG_DIR, file).replace(/\\/g, "/").replace(/\.json$/, "");
    try {
      const j = JSON.parse(fs.readFileSync(file, "utf8"));
      tags.set(rel, j.values || []);
    } catch (e) {
      console.warn("bad tag", file, e.message);
    }
  }
  return tags;
}

function extractId(entry) {
  if (typeof entry === "string") return entry;
  if (entry && typeof entry.id === "string") return entry.id;
  return null;
}

/**
 * Resolve a biome ref into:
 *  - exact biome path names (no namespace) for minecraft:
 *  - special tokens *overworld*, *nether*, *end*
 *  - keyword tokens from TAG_KEYWORDS
 */
function resolveBiomeRef(ref, tags, cache, stack = new Set()) {
  if (cache.has(ref)) return cache.get(ref);
  const out = new Set();

  // Direct biome id
  if (!ref.startsWith("#")) {
    const id = ref.includes(":") ? ref.split(":")[1] : ref;
    // Only keep vanilla-ish / generic path names (skip heavy mod ids for match list,
    // but keep path token so "volcanic_peaks" style still helps via keywords)
    if (ref.startsWith("minecraft:") || !ref.includes(":")) {
      out.add(id);
    } else {
      // mod biome — keep short path for keyword-ish contains checks
      out.add(id);
    }
    cache.set(ref, out);
    return out;
  }

  const key = tagKeyFromRef(ref);
  if (!key) {
    cache.set(ref, out);
    return out;
  }

  if (stack.has(key)) {
    cache.set(ref, out);
    return out;
  }
  stack.add(key);

  // Built-in minecraft tag expansion
  if (key.startsWith("minecraft:")) {
    const short = key.slice("minecraft:".length);
    for (const b of VANILLA_BIOMES[short] || []) out.add(b);
    // also add short name as keyword token
    out.add("kw:" + short.replace(/^is_/, ""));
    cache.set(ref, out);
    stack.delete(key);
    return out;
  }

  // Skip foreign optional tags (c:, aether:, biomesoplenty as tags)
  if (key.includes(":") && !key.startsWith("cobblemon") && !key.startsWith("minecraft")) {
    cache.set(ref, out);
    stack.delete(key);
    return out;
  }

  const tagPath = key.startsWith("cobblemon:") ? key.slice("cobblemon:".length) : key;
  const values = tags.get(tagPath);
  if (values) {
    for (const v of values) {
      const id = extractId(v);
      if (!id) continue;
      for (const x of resolveBiomeRef(id, tags, cache, stack)) out.add(x);
    }
  }

  // Always attach keyword fallbacks for the tag name
  const baseName = tagPath.includes("/") ? tagPath.split("/").pop() : tagPath;
  const kws = TAG_KEYWORDS[baseName] || TAG_KEYWORDS[tagPath];
  if (kws) {
    for (const k of kws) out.add(k.startsWith("*") ? k : "kw:" + k);
  } else {
    out.add("kw:" + baseName.replace(/^is_/, ""));
  }

  cache.set(ref, out);
  stack.delete(key);
  return out;
}

function parseLevel(level) {
  if (typeof level === "number") return [level, level];
  if (typeof level !== "string") return [5, 20];
  const m = level.match(/(\d+)\s*-\s*(\d+)/);
  if (m) return [parseInt(m[1], 10), parseInt(m[2], 10)];
  const n = parseInt(level, 10);
  return Number.isFinite(n) ? [n, n] : [5, 20];
}

function normalizeBucket(b) {
  if (!b) return "common";
  const s = String(b).toLowerCase();
  if (s === "ultra-rare" || s === "ultra_rare") return "ultra_rare";
  if (s === "legendary") return "legendary";
  if (s === "rare") return "rare";
  if (s === "uncommon") return "uncommon";
  return "common";
}

function baseSpeciesName(pokemon) {
  if (!pokemon) return null;
  // "rattata alolan" -> skip forms we don't have
  const parts = String(pokemon).trim().split(/\s+/);
  if (parts.length > 1) return null; // forms / features
  return parts[0].toLowerCase().replace(/[^a-z0-9_]/g, "");
}

function mapSpeciesId(id) {
  // Cobblemon uses nidoranf / nidoranm / mrmime / farfetchd
  if (id === "nidoranf") return "nidoran_f";
  if (id === "nidoranm") return "nidoran_m";
  if (id === "mrmime") return "mr_mime";
  return id;
}

function collectMatchers(refs, tags, cache) {
  const exact = new Set();
  const keywords = new Set();
  let overworld = false;
  let nether = false;
  let end = false;

  for (const ref of refs || []) {
    for (const tok of resolveBiomeRef(ref, tags, cache)) {
      if (tok === "*overworld*") overworld = true;
      else if (tok === "*nether*") nether = true;
      else if (tok === "*end*") end = true;
      else if (tok.startsWith("kw:")) keywords.add(tok.slice(3));
      else if (tok.startsWith("*")) {
        // ignore other specials
      } else {
        exact.add(tok);
      }
    }
  }
  return { exact: [...exact].sort(), keywords: [...keywords].sort(), overworld, nether, end };
}

function importSpawns(tags) {
  const cache = new Map();
  const entries = [];
  const files = fs.readdirSync(SPAWN_DIR).filter((f) => f.endsWith(".json"));

  for (const file of files) {
    // Gen 1 only: 0001-0151 + skip herd test 0000
    const m = file.match(/^(\d{4})_/);
    if (!m) continue;
    const num = parseInt(m[1], 10);
    if (num < 1 || num > 151) continue;
    // Birds 144-146, mewtwo 150, mew 151 may be missing in pool — skip quietly

    const raw = JSON.parse(fs.readFileSync(path.join(SPAWN_DIR, file), "utf8"));
    if (raw.enabled === false) continue;

    for (const sp of raw.spawns || []) {
      if (sp.type && sp.type !== "pokemon") continue;
      if (sp.spawnablePositionType === "fishing") continue;

      const species = mapSpeciesId(baseSpeciesName(sp.pokemon));
      if (!species) continue;

      const cond = sp.condition || {};
      const anti = sp.anticondition || {};
      const pos = sp.spawnablePositionType || "grounded";
      if (pos !== "grounded" && pos !== "submerged" && pos !== "surface") continue;

      const [levelMin, levelMax] = parseLevel(sp.level || sp.levelRange);
      const bucket = normalizeBucket(sp.bucket);
      const weight = typeof sp.weight === "number" ? sp.weight : 1.0;

      const biomeMatch = collectMatchers(cond.biomes || [], tags, cache);
      const antiMatch = collectMatchers(anti.biomes || [], tags, cache);

      // Prefer vanilla biome paths for exact matches (mod paths still help via contains).
      const VANILLA = new Set([
        "plains","sunflower_plains","snowy_plains","ice_spikes","desert","swamp","mangrove_swamp",
        "forest","flower_forest","birch_forest","dark_forest","old_growth_birch_forest","old_growth_pine_taiga",
        "old_growth_spruce_taiga","taiga","snowy_taiga","savanna","savanna_plateau","windswept_hills",
        "windswept_gravelly_hills","windswept_forest","windswept_savanna","jungle","sparse_jungle",
        "bamboo_jungle","badlands","eroded_badlands","wooded_badlands","meadow","cherry_grove","grove",
        "snowy_slopes","frozen_peaks","jagged_peaks","stony_peaks","river","frozen_river","beach",
        "snowy_beach","stony_shore","warm_ocean","lukewarm_ocean","deep_lukewarm_ocean","ocean",
        "deep_ocean","cold_ocean","deep_cold_ocean","frozen_ocean","deep_frozen_ocean","mushroom_fields",
        "dripstone_caves","lush_caves","deep_dark","nether_wastes","warped_forest","crimson_forest",
        "soul_sand_valley","basalt_deltas","the_end","end_highlands","end_midlands","end_barrens",
        "small_end_islands","pale_garden",
      ]);
      const exactVanilla = biomeMatch.exact.filter((b) => VANILLA.has(b) || b.includes("/"));
      // Keep a few high-signal exacts even if modded (basalt_deltas etc already vanilla)
      const exact = exactVanilla.length > 0 ? exactVanilla : biomeMatch.exact.filter((b) => !b.includes("/")).slice(0, 24);

      const kws = biomeMatch.keywords.filter((k) => k && k !== "overworld");
      const hasSpecific = exact.length > 0 || kws.length > 0;
      const anyOverworld = biomeMatch.overworld && !hasSpecific;

      // Skip unusable entries (no biome filter at all — would spawn everywhere)
      if (!hasSpecific && !anyOverworld && !biomeMatch.nether && !biomeMatch.end) {
        continue;
      }

      const time = cond.timeRange || "any";
      let timeMode = "any";
      if (time === "night" || (typeof time === "string" && time.includes("night") && !time.includes("day"))) {
        timeMode = "night";
      } else if (time === "day" || (typeof time === "string" && time.includes("day") && !time.includes("night"))) {
        timeMode = "day";
      }

      // Dimension: only when tag explicitly nether/end or biomes are pure nether
      const netherOnly =
        biomeMatch.nether ||
        (exact.length > 0 && exact.every((b) =>
          ["nether_wastes", "basalt_deltas", "soul_sand_valley", "crimson_forest", "warped_forest"].includes(b)
        ));

      entries.push({
        species,
        bucket,
        weight,
        levelMin,
        levelMax,
        position: pos,
        exactBiomes: exact,
        biomeKeywords: kws,
        anyOverworld,
        overworld: biomeMatch.overworld || (!netherOnly && !biomeMatch.end),
        nether: netherOnly,
        end: biomeMatch.end,
        antiExact: antiMatch.exact.filter((b) => VANILLA.has(b)).slice(0, 32),
        antiKeywords: antiMatch.keywords.filter((k) => k !== "overworld").slice(0, 16),
        minSkyLight: cond.minSkyLight != null ? cond.minSkyLight : -1,
        maxSkyLight: cond.maxSkyLight != null ? cond.maxSkyLight : -1,
        minY: cond.minY != null ? cond.minY : -99999,
        maxY: cond.maxY != null ? cond.maxY : 99999,
        canSeeSky: cond.canSeeSky === undefined ? -1 : cond.canSeeSky ? 1 : 0,
        timeMode,
        noRain: cond.isRaining === false,
      });
    }
  }

  return entries;
}

function javaStringArray(arr) {
  if (!arr || arr.length === 0) return "EMPTY";
  // Cap exact biomes to keep file size sane — keywords cover the rest
  const limited = arr.slice(0, 48);
  return "arr(" + limited.map((s) => JSON.stringify(s)).join(", ") + ")";
}

function emitSpawnPoolJava(entries) {
  const lines = [];
  lines.push("package ai.xai.fakemon.species;");
  lines.push("");
  lines.push("import java.util.ArrayList;");
  lines.push("import java.util.Collections;");
  lines.push("import java.util.List;");
  lines.push("");
  lines.push("/**");
  lines.push(" * AUTO-GENERATED from Cobblemon 1.7.3 Gen 1 spawn_pool_world.");
  lines.push(" * Do not edit by hand — run: node tools/import_cobblemon_spawns.js");
  lines.push(" */");
  lines.push("public final class Gen1SpawnPool {");
  lines.push("    private Gen1SpawnPool() {}");
  lines.push("");
  lines.push("    private static final String[] EMPTY = new String[0];");
  lines.push("    private static String[] arr(String... s) { return s; }");
  lines.push("");
  lines.push("    public static final List<SpawnEntry> ENTRIES;");
  lines.push("    static {");
  lines.push("        List<SpawnEntry> list = new ArrayList<>(" + entries.length + ");");

  for (const e of entries) {
    lines.push(
      "        list.add(new SpawnEntry(" +
        JSON.stringify(e.species) + ", " +
        "SpawnRarity." + e.bucket.toUpperCase() + ", " +
        e.weight + "f, " +
        e.levelMin + ", " + e.levelMax + ", " +
        "SpawnEntry.Position." + e.position.toUpperCase() + ", " +
        javaStringArray(e.exactBiomes) + ", " +
        javaStringArray(e.biomeKeywords) + ", " +
        e.anyOverworld + ", " +
        e.overworld + ", " +
        e.nether + ", " +
        e.end + ", " +
        javaStringArray(e.antiExact) + ", " +
        javaStringArray(e.antiKeywords) + ", " +
        e.minSkyLight + ", " + e.maxSkyLight + ", " +
        e.minY + ", " + e.maxY + ", " +
        e.canSeeSky + ", " +
        "SpawnEntry.TimeMode." + e.timeMode.toUpperCase() + ", " +
        e.noRain +
        "));"
    );
  }

  lines.push("        ENTRIES = Collections.unmodifiableList(list);");
  lines.push("    }");
  lines.push("}");
  lines.push("");
  return lines.join("\n");
}

function emitSpawnEntryJava() {
  return `package ai.xai.fakemon.species;

/**
 * One Cobblemon-style wild spawn rule (Gen 1 import).
 */
public final class SpawnEntry {
    public enum Position { GROUNDED, SUBMERGED, SURFACE }
    public enum TimeMode { ANY, DAY, NIGHT }

    public final String speciesId;
    public final SpawnRarity bucket;
    public final float weight;
    public final int levelMin;
    public final int levelMax;
    public final Position position;
    public final String[] exactBiomes;
    public final String[] biomeKeywords;
    public final boolean anyOverworld;
    public final boolean overworld;
    public final boolean nether;
    public final boolean end;
    public final String[] antiExact;
    public final String[] antiKeywords;
    public final int minSkyLight; // -1 = ignore
    public final int maxSkyLight;
    public final int minY;
    public final int maxY;
    public final int canSeeSky; // -1 ignore, 0 false, 1 true
    public final TimeMode timeMode;
    public final boolean noRain;

    public SpawnEntry(
            String speciesId,
            SpawnRarity bucket,
            float weight,
            int levelMin,
            int levelMax,
            Position position,
            String[] exactBiomes,
            String[] biomeKeywords,
            boolean anyOverworld,
            boolean overworld,
            boolean nether,
            boolean end,
            String[] antiExact,
            String[] antiKeywords,
            int minSkyLight,
            int maxSkyLight,
            int minY,
            int maxY,
            int canSeeSky,
            TimeMode timeMode,
            boolean noRain
    ) {
        this.speciesId = speciesId;
        this.bucket = bucket;
        this.weight = weight;
        this.levelMin = levelMin;
        this.levelMax = levelMax;
        this.position = position;
        this.exactBiomes = exactBiomes;
        this.biomeKeywords = biomeKeywords;
        this.anyOverworld = anyOverworld;
        this.overworld = overworld;
        this.nether = nether;
        this.end = end;
        this.antiExact = antiExact;
        this.antiKeywords = antiKeywords;
        this.minSkyLight = minSkyLight;
        this.maxSkyLight = maxSkyLight;
        this.minY = minY;
        this.maxY = maxY;
        this.canSeeSky = canSeeSky;
        this.timeMode = timeMode;
        this.noRain = noRain;
    }
}
`;
}

function importBaseStats() {
  const stats = {};
  if (!fs.existsSync(SPECIES_DIR)) {
    console.warn("species dir missing", SPECIES_DIR);
    return stats;
  }
  for (const file of fs.readdirSync(SPECIES_DIR)) {
    if (!file.endsWith(".json")) continue;
    const j = JSON.parse(fs.readFileSync(path.join(SPECIES_DIR, file), "utf8"));
    let id = file.replace(/\.json$/, "").toLowerCase();
    id = mapSpeciesId(id);
    if (!j.baseStats) continue;
    const b = j.baseStats;
    stats[id] = {
      hp: b.hp || 50,
      atk: b.attack || 50,
      def: b.defence || b.defense || 50,
      spa: b.special_attack || 50,
      spd: b.special_defence || b.special_defense || 50,
      spe: b.speed || 50,
      catchRate: j.catchRate != null ? j.catchRate : 45,
      maleRatio: j.maleRatio != null ? j.maleRatio : 0.5,
    };
  }
  return stats;
}

function emitBaseStatsJava(stats) {
  const ids = Object.keys(stats).sort();
  const lines = [];
  lines.push("package ai.xai.fakemon.species;");
  lines.push("");
  lines.push("/**");
  lines.push(" * AUTO-GENERATED from Cobblemon 1.7.3 generation1 species JSON.");
  lines.push(" * Full 6-stat base stats + catch rate.");
  lines.push(" */");
  lines.push("public final class CobblemonBaseStats {");
  lines.push("    private CobblemonBaseStats() {}");
  lines.push("");
  lines.push("    public record Six(int hp, int atk, int def, int spa, int spd, int spe, int catchRate) {}");
  lines.push("");
  lines.push("    public static Six of(MonSpecies species) {");
  lines.push("        if (species == null) return null;");
  lines.push("        return ofId(species.id());");
  lines.push("    }");
  lines.push("");
  lines.push("    public static Six ofId(String id) {");
  lines.push("        if (id == null) return null;");
  lines.push("        return switch (id) {");
  for (const id of ids) {
    const s = stats[id];
    lines.push(
      `            case "${id}" -> new Six(${s.hp}, ${s.atk}, ${s.def}, ${s.spa}, ${s.spd}, ${s.spe}, ${s.catchRate});`
    );
  }
  lines.push("            default -> null;");
  lines.push("        };");
  lines.push("    }");
  lines.push("}");
  lines.push("");
  return lines.join("\n");
}

function main() {
  console.log("Loading biome tags from", BIOME_TAG_DIR);
  const tags = loadBiomeTags();
  console.log("  tags:", tags.size);

  console.log("Importing Gen1 spawns from", SPAWN_DIR);
  const entries = importSpawns(tags);
  console.log("  spawn entries:", entries.length);

  // Ensure SpawnRarity has ULTRA_RARE matching
  fs.writeFileSync(path.join(OUT_DIR, "SpawnEntry.java"), emitSpawnEntryJava(), "utf8");
  fs.writeFileSync(path.join(OUT_DIR, "Gen1SpawnPool.java"), emitSpawnPoolJava(entries), "utf8");
  console.log("Wrote Gen1SpawnPool.java + SpawnEntry.java");

  const stats = importBaseStats();
  fs.writeFileSync(path.join(OUT_DIR, "CobblemonBaseStats.java"), emitBaseStatsJava(stats), "utf8");
  console.log("Wrote CobblemonBaseStats.java for", Object.keys(stats).length, "species");

  // Quick sanity: rattata / charmander / magikarp counts
  for (const name of ["rattata", "charmander", "magikarp", "zubat", "pidgey"]) {
    const n = entries.filter((e) => e.species === name).length;
    console.log(" ", name, "entries:", n);
  }
}

main();

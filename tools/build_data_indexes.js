/**
 * Build compact runtime indexes from imported Cobblemon datapacks.
 * Produces:
 *   data/cobblemon/content/species_index.json
 *   data/cobblemon/content/spawn_index.json
 *
 * Usage: node tools/build_data_indexes.js
 */
const fs = require("fs");
const path = require("path");

const ROOT = path.join(__dirname, "..");
const DATA = path.join(ROOT, "src", "main", "resources", "data", "cobblemon");
const OUT = path.join(DATA, "content");

function walk(dir, acc = []) {
  if (!fs.existsSync(dir)) return acc;
  for (const ent of fs.readdirSync(dir, { withFileTypes: true })) {
    const p = path.join(dir, ent.name);
    if (ent.isDirectory()) walk(p, acc);
    else acc.push(p);
  }
  return acc;
}

function readJson(file) {
  let t = fs.readFileSync(file, "utf8");
  if (t.charCodeAt(0) === 0xfeff) t = t.slice(1);
  return JSON.parse(t);
}

function speciesIdFromFile(file) {
  return path.basename(file, ".json").toLowerCase();
}

function mapType(t) {
  if (!t) return null;
  return String(t).toLowerCase();
}

function buildSpeciesIndex() {
  const speciesDir = path.join(DATA, "species");
  const files = walk(speciesDir).filter((f) => f.endsWith(".json"));
  const byId = {};
  let n = 0;
  for (const file of files) {
    try {
      const j = readJson(file);
      const id = speciesIdFromFile(file);
      // skip forms folders that aren't root species (keep all — forms are useful)
      const stats = j.baseStats || {};
      byId[id] = {
        id,
        name: j.name || id,
        nationalPokedexNumber: j.nationalPokedexNumber || 0,
        primaryType: mapType(j.primaryType) || "normal",
        secondaryType: mapType(j.secondaryType),
        implemented: !!j.implemented,
        maleRatio: typeof j.maleRatio === "number" ? j.maleRatio : 0.5,
        height: j.height || 10,
        weight: j.weight || 100,
        catchRate: j.catchRate ?? 45,
        baseExperienceYield: j.baseExperienceYield ?? 50,
        baseFriendship: j.baseFriendship ?? 50,
        baseScale: j.baseScale ?? 1,
        labels: j.labels || [],
        abilities: j.abilities || [],
        baseStats: {
          hp: stats.hp ?? 50,
          attack: stats.attack ?? 50,
          defence: stats.defence ?? 50,
          special_attack: stats.special_attack ?? 50,
          special_defence: stats.special_defence ?? 50,
          speed: stats.speed ?? 50,
        },
        // first level evolution only (compact)
        evolutions: (j.evolutions || []).slice(0, 4).map((e) => ({
          id: e.id,
          variant: e.variant,
          result: typeof e.result === "string" ? e.result.split(" ")[0] : e.result,
          requirements: e.requirements || [],
        })),
        // level-up moves only (compact)
        moves: (j.moves || [])
          .filter((m) => typeof m === "string" && /^\d+:/.test(m))
          .slice(0, 40),
        // egg-move pool (ids only, no "egg:" prefix)
        eggMoves: (j.moves || [])
          .filter((m) => typeof m === "string" && /^egg:/i.test(m))
          .map((m) => String(m).replace(/^egg:/i, "").toLowerCase())
          .filter((m) => m.length > 0)
          .slice(0, 32),
      };
      n++;
    } catch (e) {
      console.warn("species fail", file, e.message);
    }
  }
  fs.mkdirSync(OUT, { recursive: true });
  const outPath = path.join(OUT, "species_index.json");
  fs.writeFileSync(outPath, JSON.stringify({ version: 1, count: n, species: byId }), "utf8");
  console.log("species_index.json:", n, "species →", outPath);
  return n;
}

function parseLevelRange(s) {
  if (!s) return [5, 20];
  if (typeof s === "number") return [s, s];
  const m = String(s).match(/(\d+)\s*-\s*(\d+)/);
  if (m) return [parseInt(m[1], 10), parseInt(m[2], 10)];
  const n = parseInt(s, 10);
  return Number.isFinite(n) ? [n, n] : [5, 20];
}

function mapBucket(b) {
  const s = String(b || "common").toLowerCase();
  if (s.includes("ultra")) return "ultra_rare";
  if (s.includes("rare")) return "rare";
  if (s.includes("uncommon")) return "uncommon";
  return "common";
}

function mapPosition(t) {
  const s = String(t || "grounded").toLowerCase();
  if (s.includes("submerg") || s === "seafloor") return "submerged";
  if (s.includes("surface") || s.includes("water")) return "surface";
  return "grounded";
}

function biomeKeywordsFromTags(biomes) {
  if (!Array.isArray(biomes)) return [];
  const keys = [];
  for (const b of biomes) {
    const s = String(b);
    // #cobblemon:is_jungle → jungle
    const m = s.match(/is_([a-z0-9_]+)/i);
    if (m) keys.push(m[1].replace(/_/g, " "));
    // minecraft:plains → plains
    const bare = s.replace(/^#?[^:]+:/, "").replace(/^is_/, "");
    if (bare && bare.length > 2) keys.push(bare.replace(/_/g, " "));
  }
  return [...new Set(keys)].slice(0, 12);
}

function buildSpawnIndex() {
  const spawnDir = path.join(DATA, "spawn_pool_world");
  const files = walk(spawnDir).filter((f) => f.endsWith(".json"));
  const entries = [];
  for (const file of files) {
    try {
      const j = readJson(file);
      if (j.enabled === false) continue;
      const spawns = j.spawns || [];
      for (const sp of spawns) {
        if (!sp || !sp.pokemon) continue;
        // pokemon can be "bulbasaur" or "rattata alolan=true"
        const speciesId = String(sp.pokemon).split(/\s+/)[0].toLowerCase();
        const [levelMin, levelMax] = parseLevelRange(sp.level);
        const cond = sp.condition || {};
        const anti = sp.anticondition || {};
        const biomes = cond.biomes || [];
        const keywords = biomeKeywordsFromTags(biomes);
        const anyOverworld =
          biomes.length === 0 ||
          biomes.some((b) => String(b).includes("is_overworld") || String(b) === "#minecraft:is_overworld");
        entries.push({
          speciesId,
          bucket: mapBucket(sp.bucket),
          weight: typeof sp.weight === "number" ? sp.weight : 1,
          levelMin,
          levelMax,
          position: mapPosition(sp.spawnablePositionType || sp.context),
          biomeKeywords: keywords,
          anyOverworld: anyOverworld || keywords.length === 0,
          overworld: true,
          nether: biomes.some((b) => String(b).includes("nether") || String(b).includes("is_nether")),
          end: biomes.some((b) => String(b).includes("is_end") || String(b).includes("the_end")),
          minSkyLight: cond.minSkyLight ?? -1,
          maxSkyLight: cond.maxSkyLight ?? -1,
          minY: cond.minY ?? -99999,
          maxY: cond.maxY ?? 99999,
          canSeeSky: cond.canSeeSky === true ? 1 : cond.canSeeSky === false ? 0 : -1,
          timeMode: "any",
          noRain: !!(cond.isRaining === false || anti.isRaining === true),
        });
      }
    } catch (e) {
      console.warn("spawn fail", path.basename(file), e.message);
    }
  }
  const outPath = path.join(OUT, "spawn_index.json");
  fs.writeFileSync(
    outPath,
    JSON.stringify({ version: 1, count: entries.length, entries }),
    "utf8"
  );
  console.log("spawn_index.json:", entries.length, "entries →", outPath);
  return entries.length;
}

function main() {
  console.log("=== Building Cobblemon data indexes ===");
  if (!fs.existsSync(DATA)) {
    console.error("Missing data/cobblemon — run import_porter_all.js first");
    process.exit(1);
  }
  buildSpeciesIndex();
  buildSpawnIndex();
  console.log("=== DONE ===");
}

main();

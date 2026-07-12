/**
 * Completes playability for every species in species_index:
 *  - Rebuild evo_index.json from all level_up evolutions
 *  - Add synthetic wild spawn rules for any species missing from spawn_index
 *
 * Usage: node tools/fill_missing_content.js
 */
const fs = require("fs");
const path = require("path");

const ROOT = path.join(__dirname, "..");
const CONTENT = path.join(ROOT, "src/main/resources/data/cobblemon/content");
const SPECIES = path.join(CONTENT, "species_index.json");
const SPAWN = path.join(CONTENT, "spawn_index.json");
const EVO = path.join(CONTENT, "evo_index.json");

/** Primary type → default biome keywords for synthetic spawns */
const TYPE_BIOMES = {
  fire: ["desert", "badlands", "savanna", "nether"],
  water: ["ocean", "river", "beach", "swamp"],
  grass: ["forest", "jungle", "plains", "swamp"],
  electric: ["plains", "savanna", "badlands"],
  ground: ["desert", "badlands", "savanna"],
  flying: ["plains", "mountains", "forest"],
  fighting: ["plains", "savanna", "village"],
  dragon: ["mountains", "hills", "end"],
  dark: ["dark forest", "swamp", "spooky"],
  psychic: ["plains", "mushroom", "end"],
  rock: ["mountains", "badlands", "desert", "stony"],
  ice: ["snowy", "frozen", "taiga", "cold"],
  steel: ["mountains", "badlands", "industrial"],
  fairy: ["flower", "meadow", "forest", "cherry"],
  bug: ["forest", "jungle", "plains", "swamp"],
  poison: ["swamp", "jungle", "dark forest"],
  ghost: ["dark forest", "swamp", "spooky", "deep dark"],
  normal: ["plains", "forest", "taiga"],
};

const WATER_TYPES = new Set(["water"]);
const FLYINGISH = new Set(["flying"]);
const LEGEND_LABELS = /legendary|mythical|ultra_beast|paradox|restricted/i;

function isLegendary(sp) {
  const labels = sp.labels || [];
  if (labels.some((l) => LEGEND_LABELS.test(l))) return true;
  // ultra-rare catch rate is a decent proxy for legendaries without labels
  return (sp.catchRate ?? 45) <= 3 && (sp.nationalPokedexNumber || 0) > 0;
}

function pickPosition(sp) {
  const t = (sp.primaryType || "").toLowerCase();
  const t2 = (sp.secondaryType || "").toLowerCase();
  if (WATER_TYPES.has(t) || WATER_TYPES.has(t2)) return "submerged";
  if (FLYINGISH.has(t) || FLYINGISH.has(t2)) return "grounded"; // still walk; no fly AI
  return "grounded";
}

function pickBiomes(sp) {
  const t = (sp.primaryType || "normal").toLowerCase();
  const base = TYPE_BIOMES[t] || TYPE_BIOMES.normal;
  // label hints
  const labels = (sp.labels || []).map((l) => String(l).toLowerCase());
  const extra = [];
  if (labels.some((l) => l.includes("nether"))) extra.push("nether");
  if (labels.some((l) => l.includes("end"))) extra.push("end");
  if (labels.some((l) => l.includes("fossil"))) extra.push("desert", "lush cave");
  return [...new Set([...base, ...extra])].slice(0, 8);
}

function syntheticSpawn(id, sp) {
  const legend = isLegendary(sp);
  const t = (sp.primaryType || "normal").toLowerCase();
  const biomes = pickBiomes(sp);
  const nether = biomes.includes("nether") || t === "fire" && legend;
  const end = biomes.includes("end") || t === "dragon" && legend;
  return {
    speciesId: id,
    bucket: legend ? "ultra_rare" : sp.catchRate >= 150 ? "common" : sp.catchRate >= 75 ? "uncommon" : "rare",
    weight: legend ? 1 : sp.catchRate >= 150 ? 10 : sp.catchRate >= 75 ? 6 : 3,
    levelMin: legend ? 50 : 5,
    levelMax: legend ? 70 : 35,
    position: pickPosition(sp),
    biomeKeywords: biomes.filter((b) => b !== "nether" && b !== "end"),
    anyOverworld: !end && !nether,
    overworld: !end,
    nether: !!nether,
    end: !!end,
    minSkyLight: -1,
    maxSkyLight: -1,
    minY: -99999,
    maxY: 99999,
    canSeeSky: -1,
    timeMode: "any",
    noRain: false,
    synthetic: true,
  };
}

function rebuildEvoIndex(speciesMap) {
  const evolutions = {};
  let count = 0;
  for (const [id, sp] of Object.entries(speciesMap)) {
    const list = sp.evolutions || [];
    for (const ev of list) {
      if (!ev || ev.variant !== "level_up") continue;
      const req = (ev.requirements || []).find(
        (r) => r && (r.variant === "level" || r.minLevel != null || r.level != null)
      );
      const level = req ? req.minLevel ?? req.level ?? 0 : 0;
      if (!level || level < 1) continue;
      let into = ev.result;
      if (typeof into !== "string") continue;
      into = into.split(/\s+/)[0].toLowerCase();
      // first level-up only (runtime supports one chain per species)
      if (evolutions[id]) continue;
      evolutions[id] = { into, level };
      count++;
    }
  }
  return { version: 2, count, evolutions };
}

function main() {
  console.log("=== Fill missing spawns + rebuild evo_index ===");
  const speciesRoot = JSON.parse(fs.readFileSync(SPECIES, "utf8"));
  const spawnRoot = JSON.parse(fs.readFileSync(SPAWN, "utf8"));
  const speciesMap = speciesRoot.species || {};
  const entries = spawnRoot.entries || [];

  const haveSpawn = new Set(entries.map((e) => String(e.speciesId).toLowerCase()));
  let added = 0;
  for (const [id, sp] of Object.entries(speciesMap)) {
    if (haveSpawn.has(id)) continue;
    entries.push(syntheticSpawn(id, sp));
    haveSpawn.add(id);
    added++;
  }

  const spawnOut = {
    version: 2,
    count: entries.length,
    entries,
  };
  fs.writeFileSync(SPAWN, JSON.stringify(spawnOut));
  console.log("Spawn entries:", entries.length, "(added synthetic:", added + ")");

  const evoOut = rebuildEvoIndex(speciesMap);
  fs.writeFileSync(EVO, JSON.stringify(evoOut));
  console.log("Evo level-up chains:", evoOut.count);

  // coverage report
  const uniqueSpawn = new Set(entries.map((e) => e.speciesId));
  const missing = Object.keys(speciesMap).filter((k) => !uniqueSpawn.has(k));
  console.log("Species with ≥1 spawn:", uniqueSpawn.size, "/", Object.keys(speciesMap).length);
  if (missing.length) console.log("Still missing spawns:", missing.join(", "));
  console.log("=== DONE ===");
}

main();

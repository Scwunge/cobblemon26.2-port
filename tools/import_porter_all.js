/**
 * Full Cobblemon porter → mod import.
 *
 * Namespace: cobblemon (kept as-is from porter; no fakemon rewrite).
 * Copies textures (incl. GUI), models, blockstates, sounds, particles,
 * recipes, loot tables, safe tags, lang; generates item definitions + content lists.
 *
 * SAFE: never copies worldgen structures / structure tags / entity AI tags
 * (those register unbound structure IDs and hang world creation).
 *
 * Usage: node tools/import_porter_all.js
 */
const fs = require("fs");
const path = require("path");

const PORTER = process.env.PORTER || "C:\\Users\\conne\\Desktop\\porter";
const ROOT = path.join(__dirname, "..");
const ASSETS = path.join(ROOT, "src", "main", "resources", "assets", "cobblemon");
const DATA = path.join(ROOT, "src", "main", "resources", "data", "cobblemon");
const GEN = path.join(DATA, "content");

// IDs already owned by core gameplay (do not re-register as generic content)
const RESERVED_IDS = new Set([
  "party_badge", "pc",
  "fire_gem", "water_gem", "thunder_gem", "leaf_gem", "moon_gem",
  "wild_mon_spawn_egg", "bind_orb",
]);

function ensureDir(d) {
  fs.mkdirSync(d, { recursive: true });
}

function walk(dir, acc = []) {
  if (!fs.existsSync(dir)) return acc;
  for (const ent of fs.readdirSync(dir, { withFileTypes: true })) {
    const p = path.join(dir, ent.name);
    if (ent.isDirectory()) walk(p, acc);
    else acc.push(p);
  }
  return acc;
}

/** Keep cobblemon namespace; strip any leftover fakemon rewrite. */
function rewriteNs(str) {
  return str
    .replace(/fakemon:/g, "cobblemon:")
    .replace(/"fakemon"/g, '"cobblemon"')
    .replace(/fakemon\//g, "cobblemon/")
    .replace(/\.fakemon\./g, ".cobblemon.")
    .replace(/itemGroup\.fakemon/g, "itemGroup.cobblemon");
}

function copyFileRewritten(src, dest) {
  ensureDir(path.dirname(dest));
  const ext = path.extname(src).toLowerCase();
  if ([".json", ".mcmeta", ".toml", ".lang", ".txt", ".glsl", ".vsh", ".fsh", ".molang", ".js"].includes(ext)) {
    let text = fs.readFileSync(src, "utf8");
    if (text.charCodeAt(0) === 0xfeff) text = text.slice(1);
    text = rewriteNs(text);
    fs.writeFileSync(dest, text, "utf8");
  } else {
    fs.copyFileSync(src, dest);
  }
}

function copyTree(srcRoot, destRoot, filterExt = null) {
  let n = 0;
  for (const file of walk(srcRoot)) {
    if (filterExt && !filterExt.some((e) => file.toLowerCase().endsWith(e))) continue;
    const rel = path.relative(srcRoot, file);
    const dest = path.join(destRoot, rel);
    copyFileRewritten(file, dest);
    n++;
  }
  return n;
}

/** Collapse multi-variant blockstates to a single default model so simple Blocks work. */
function simplifyBlockstate(obj) {
  if (obj.variants) {
    const keys = Object.keys(obj.variants);
    if (keys.length === 0) return obj;
    const pick = obj.variants[""] ?? obj.variants[keys[0]];
    const model = Array.isArray(pick) ? pick[0] : pick;
    return { variants: { "": model } };
  }
  if (obj.multipart) {
    for (const part of obj.multipart) {
      if (part.apply) {
        const model = Array.isArray(part.apply) ? part.apply[0] : part.apply;
        return { variants: { "": model } };
      }
    }
  }
  return obj;
}

function processBlockstates() {
  const dir = path.join(ASSETS, "blockstates");
  let n = 0;
  for (const file of walk(dir)) {
    if (!file.endsWith(".json")) continue;
    try {
      let text = fs.readFileSync(file, "utf8");
      if (text.charCodeAt(0) === 0xfeff) text = text.slice(1);
      const obj = JSON.parse(text);
      const simple = simplifyBlockstate(obj);
      // ensure cobblemon namespace in model paths
      fs.writeFileSync(file, rewriteNs(JSON.stringify(simple)), "utf8");
      n++;
    } catch (e) {
      console.warn("blockstate fail", file, e.message);
    }
  }
  return n;
}

function loadIds(file) {
  return fs.readFileSync(file, "utf8")
    .split(/\r?\n/)
    .map((l) => l.trim())
    .filter(Boolean)
    .map((l) => l.replace(/^cobblemon:/, "").replace(/^fakemon:/, ""))
    .filter((id) => id && !id.includes(":") && /^[a-z0-9_./-]+$/.test(id));
}

function generateItemDefinitions(itemIds) {
  const itemsDir = path.join(ASSETS, "items");
  ensureDir(itemsDir);
  let n = 0;
  for (const id of itemIds) {
    if (RESERVED_IDS.has(id)) continue;
    // MC 26 item definition → model under models/item/
    const body = {
      model: {
        type: "minecraft:model",
        model: "cobblemon:item/" + id,
      },
    };
    fs.writeFileSync(path.join(itemsDir, id + ".json"), JSON.stringify(body), "utf8");
    n++;
  }
  // Also define reserved core items if missing
  for (const id of RESERVED_IDS) {
    const defPath = path.join(itemsDir, id + ".json");
    if (!fs.existsSync(defPath)) {
      fs.writeFileSync(
        defPath,
        JSON.stringify({ model: { type: "minecraft:model", model: "cobblemon:item/" + id } }),
        "utf8"
      );
      n++;
    }
  }
  return n;
}

/** Ensure flat-texture models can resolve when nested texture is missing (fallback). */
function fixItemModelsWithMissingTextures() {
  const modelsDir = path.join(ASSETS, "models", "item");
  const texRoot = path.join(ASSETS, "textures");
  let fixed = 0;
  let missing = 0;
  const missingList = [];
  if (!fs.existsSync(modelsDir)) return { fixed, missing };

  for (const file of walk(modelsDir)) {
    if (!file.endsWith(".json")) continue;
    try {
      let text = fs.readFileSync(file, "utf8");
      if (text.charCodeAt(0) === 0xfeff) text = text.slice(1);
      text = rewriteNs(text);
      const obj = JSON.parse(text);
      if (obj.textures) {
        for (const [layer, tex] of Object.entries(obj.textures)) {
          if (typeof tex !== "string") continue;
          // cobblemon:item/foo/bar → textures/item/foo/bar.png
          const m = tex.match(/^(?:cobblemon|minecraft):(.+)$/);
          if (!m) continue;
          const rel = m[1];
          const png = path.join(texRoot, rel.replace(/\//g, path.sep) + ".png");
          if (!fs.existsSync(png) && tex.startsWith("cobblemon:")) {
            missing++;
            if (missingList.length < 40) missingList.push(tex);
          }
        }
      }
      fs.writeFileSync(file, JSON.stringify(obj), "utf8");
      fixed++;
    } catch (e) {
      /* skip bad model */
    }
  }
  return { fixed, missing, missingList };
}

function mergeLang() {
  const src = path.join(PORTER, "05_lang", "en_us.json");
  const dest = path.join(ASSETS, "lang", "en_us.json");
  ensureDir(path.dirname(dest));
  let existing = {};
  if (fs.existsSync(dest)) {
    try {
      existing = JSON.parse(fs.readFileSync(dest, "utf8").replace(/^\uFEFF/, ""));
    } catch (_) {}
  }
  let porter = {};
  try {
    porter = JSON.parse(fs.readFileSync(src, "utf8").replace(/^\uFEFF/, ""));
  } catch (e) {
    console.warn("lang parse fail", e.message);
    return 0;
  }
  let added = 0;
  for (const [k, v] of Object.entries(porter)) {
    // Keep cobblemon keys; rewrite any fakemon leftovers
    const nk = k.replace(/\.fakemon\./g, ".cobblemon.").replace(/fakemon:/g, "cobblemon:");
    existing[nk] = typeof v === "string" ? v : String(v);
    added++;
  }
  // Cobblemon creative tab titles (from porter lang + extras)
  existing["itemGroup.cobblemon.blocks"] = existing["itemGroup.cobblemon.blocks"] || "Cobblemon: Blocks";
  existing["itemGroup.cobblemon.utility_item"] = existing["itemGroup.cobblemon.utility_item"] || "Cobblemon: Utility Items";
  existing["itemGroup.cobblemon.agriculture"] = existing["itemGroup.cobblemon.agriculture"] || "Cobblemon: Agriculture";
  existing["itemGroup.cobblemon.consumables"] = existing["itemGroup.cobblemon.consumables"] || "Cobblemon: Consumables";
  existing["itemGroup.cobblemon.held_item"] = existing["itemGroup.cobblemon.held_item"] || "Cobblemon: Held Items";
  existing["itemGroup.cobblemon.evolution_item"] = existing["itemGroup.cobblemon.evolution_item"] || "Cobblemon: Evolution Items";
  existing["itemGroup.cobblemon.archaeology"] = existing["itemGroup.cobblemon.archaeology"] || "Cobblemon: Archaeology";
  existing["itemGroup.cobblemon.plants"] = existing["itemGroup.cobblemon.plants"] || "Cobblemon: Plants";
  existing["itemGroup.cobblemon.poke_balls"] = existing["itemGroup.cobblemon.poke_balls"] || "Cobblemon: Poké Balls";
  // Screens / core
  existing["screen.cobblemon.party"] = existing["screen.cobblemon.party"] || "Party";
  existing["screen.cobblemon.pc"] = existing["screen.cobblemon.pc"] || "PC";
  existing["screen.cobblemon.summary"] = existing["screen.cobblemon.summary"] || "Summary";
  existing["screen.cobblemon.battle"] = existing["screen.cobblemon.battle"] || "Battle";
  existing["screen.cobblemon.starter"] = existing["screen.cobblemon.starter"] || "Choose your starter";
  existing["key.cobblemon.party"] = existing["key.cobblemon.party"] || "Open Party";
  existing["key.categories.cobblemon"] = existing["key.categories.cobblemon"] || "Cobblemon";

  fs.writeFileSync(dest, JSON.stringify(existing, null, 2), "utf8");

  // Copy other langs as-is (rewritten)
  const langSrc = path.join(PORTER, "05_lang");
  for (const f of fs.readdirSync(langSrc)) {
    if (!f.endsWith(".json") || f === "en_us.json") continue;
    copyFileRewritten(path.join(langSrc, f), path.join(ASSETS, "lang", f));
  }
  return added;
}

function writeContentLists(items, blocks) {
  ensureDir(GEN);
  const itemOnly = items.filter((id) => !blocks.includes(id) && !RESERVED_IDS.has(id));
  const blockIds = blocks.filter((id) => !RESERVED_IDS.has(id));

  const catalog = {
    generatedAt: new Date().toISOString(),
    source: "porter Cobblemon",
    namespace: "cobblemon",
    reserved: [...RESERVED_IDS],
    items: itemOnly.sort(),
    blocks: blockIds.sort(),
    counts: {
      items: itemOnly.length,
      blocks: blockIds.length,
    },
  };
  fs.writeFileSync(path.join(GEN, "catalog.json"), JSON.stringify(catalog, null, 2), "utf8");
  fs.writeFileSync(path.join(GEN, "item_ids.txt"), itemOnly.sort().join("\n") + "\n", "utf8");
  fs.writeFileSync(path.join(GEN, "block_ids.txt"), blockIds.sort().join("\n") + "\n", "utf8");
  return catalog;
}

function importRecipesAndLoot() {
  const recipeSrc = path.join(PORTER, "10_data_recipes_loot", "recipe");
  const lootSrc = path.join(PORTER, "10_data_recipes_loot", "loot_table");
  const advSrc = path.join(PORTER, "10_data_recipes_loot", "advancement");
  let recipes = 0, loot = 0, adv = 0;
  if (fs.existsSync(recipeSrc)) {
    recipes = copyTree(recipeSrc, path.join(DATA, "recipe"));
  }
  if (fs.existsSync(lootSrc)) {
    loot = copyTree(lootSrc, path.join(DATA, "loot_table"));
  }
  if (fs.existsSync(advSrc)) {
    // Advancements may reference missing criteria — still useful; skip if they break later
    adv = copyTree(advSrc, path.join(DATA, "advancement"));
  }
  return { recipes, loot, adv };
}

function importSafeTags() {
  const tagSrc = path.join(PORTER, "12_data_tags");
  let tagCount = 0;
  for (const sub of ["block", "item"]) {
    const from = path.join(tagSrc, sub);
    if (fs.existsSync(from)) {
      tagCount += copyTree(from, path.join(DATA, "tags", sub));
    }
  }
  // biome habitat tags only (is_*)
  const biomeTags = path.join(tagSrc, "worldgen", "biome");
  if (fs.existsSync(biomeTags)) {
    for (const f of walk(biomeTags)) {
      const rel = path.relative(biomeTags, f);
      if (rel.includes("has_structure") || rel.includes("has_feature") || rel.includes("has_ore")
          || rel.includes("has_block") || rel.includes("has_density") || rel.includes("has_season")
          || rel.includes("evolution") || rel.includes("space")) {
        continue;
      }
      const base = path.basename(f);
      if (!base.startsWith("is_") && !rel.startsWith("nether" + path.sep) && !rel.startsWith("nether/")) {
        continue;
      }
      copyFileRewritten(f, path.join(DATA, "tags", "worldgen", "biome", rel));
      tagCount++;
    }
  }
  return tagCount;
}

function importCobblemonDataPacks() {
  // Species, berries, held items metadata, pokerods, fossils — useful data, not structures
  const other = path.join(PORTER, "13_data_other");
  const allow = [
    "berries", "held_items", "pokerods", "fossils", "seasonings",
    "bag_items", "cosmetic_items", "natural_materials", "painting_variant",
    "trim_pattern", "marks", "mechanics", "ride_settings", "dialogues",
    "action_effects", "behaviours", "molang", "callbacks",
    "unlockable_pc_box_wallpapers",
  ];
  let n = 0;
  for (const sub of allow) {
    const from = path.join(other, sub);
    if (fs.existsSync(from)) {
      n += copyTree(from, path.join(DATA, sub));
    }
  }
  // Species data
  const species = path.join(PORTER, "09_data_species");
  if (fs.existsSync(species)) {
    for (const sub of fs.readdirSync(species, { withFileTypes: true })) {
      if (sub.isDirectory()) {
        n += copyTree(path.join(species, sub.name), path.join(DATA, sub.name));
      }
    }
  }
  // Spawning pools (JSON data for our own spawner / future use)
  const spawn = path.join(PORTER, "11_data_spawning");
  if (fs.existsSync(spawn)) {
    for (const sub of ["spawn_pool_world", "spawn_detail_presets", "spawn_bait_effects", "spawn_rules", "spawning"]) {
      const from = path.join(spawn, sub);
      if (fs.existsSync(from)) {
        n += copyTree(from, path.join(DATA, sub));
      }
    }
  }
  return n;
}

function removeOldFakemonNamespace() {
  const oldAssets = path.join(ROOT, "src", "main", "resources", "assets", "fakemon");
  const oldData = path.join(ROOT, "src", "main", "resources", "data", "fakemon");
  for (const p of [oldAssets, oldData]) {
    if (fs.existsSync(p)) {
      fs.rmSync(p, { recursive: true, force: true });
      console.log("Removed old namespace:", p);
    }
  }
}

function main() {
  console.log("=== Cobblemon full porter import ===");
  console.log("Source:", PORTER);
  console.log("Assets:", ASSETS);
  console.log("Data:", DATA);

  ensureDir(ASSETS);
  ensureDir(DATA);

  // 1) Textures (includes gui/, item/, block/, pokemon/, poke_balls/, ...)
  console.log("Copying textures (incl. GUI)...");
  console.log("  textures:", copyTree(path.join(PORTER, "01_textures"), path.join(ASSETS, "textures")));

  // 2) Models
  console.log("Copying models...");
  console.log("  models:", copyTree(path.join(PORTER, "02_models"), path.join(ASSETS, "models")));

  // 3) Blockstates
  console.log("Copying blockstates...");
  console.log("  blockstates:", copyTree(path.join(PORTER, "04_blockstates"), path.join(ASSETS, "blockstates")));
  console.log("  simplified blockstates:", processBlockstates());

  // 4) Sounds
  console.log("Copying sounds...");
  const soundSrc = path.join(PORTER, "03_sounds");
  for (const sub of fs.readdirSync(soundSrc, { withFileTypes: true })) {
    if (sub.name === "sounds.json") continue;
    if (sub.isDirectory()) {
      const n = copyTree(path.join(soundSrc, sub.name), path.join(ASSETS, "sounds", sub.name));
      console.log("  sounds/" + sub.name + ":", n);
    }
  }
  try {
    const porterSounds = JSON.parse(
      fs.readFileSync(path.join(soundSrc, "sounds.json"), "utf8").replace(/^\uFEFF/, "")
    );
    const rewritten = JSON.parse(rewriteNs(JSON.stringify(porterSounds)));
    fs.writeFileSync(path.join(ASSETS, "sounds.json"), JSON.stringify(rewritten, null, 2), "utf8");
    console.log("  sounds.json keys:", Object.keys(rewritten).length);
  } catch (e) {
    console.warn("sounds.json failed", e.message);
  }

  // 5) Bedrock
  console.log("Copying bedrock models...");
  const bedrockSrc = path.join(PORTER, "06_bedrock_models_animations");
  for (const sub of ["poke_balls", "berries", "block_entities", "fossils", "generic", "misc", "npcs", "particles", "pokemon", "fishing"]) {
    const from = path.join(bedrockSrc, sub);
    if (fs.existsSync(from)) {
      console.log("  bedrock/" + sub + ":", copyTree(from, path.join(ASSETS, "bedrock", sub)));
    }
  }

  // 6) Particles / shaders / atlases
  console.log("Copying particles/shaders/atlases...");
  const pss = path.join(PORTER, "07_particles_shaders_atlases");
  if (fs.existsSync(pss)) {
    for (const sub of fs.readdirSync(pss, { withFileTypes: true })) {
      if (sub.isDirectory()) {
        console.log("  " + sub.name + ":", copyTree(path.join(pss, sub.name), path.join(ASSETS, sub.name)));
      }
    }
  }

  // 7) IDs + item defs + catalog
  const itemIds = loadIds(path.join(PORTER, "08_item_ids", "item_ids.txt"));
  const blockIds = loadIds(path.join(PORTER, "08_item_ids", "block_ids.txt"));
  console.log("IDs items:", itemIds.length, "blocks:", blockIds.length);
  console.log("Item definitions:", generateItemDefinitions(itemIds));
  const catalog = writeContentLists(itemIds, blockIds);
  console.log("Catalog items:", catalog.counts.items, "blocks:", catalog.counts.blocks);

  // 8) Lang
  console.log("Lang keys:", mergeLang());

  // 9) Recipes / loot / advancements
  const rla = importRecipesAndLoot();
  console.log("Recipes:", rla.recipes, "loot:", rla.loot, "advancements:", rla.adv);

  // 10) Safe tags only
  console.log("Safe tags:", importSafeTags());

  // 11) Extra cobblemon data (species, berries, etc.) — no worldgen structures
  console.log("Extra data packs:", importCobblemonDataPacks());

  // 12) Model texture audit
  const audit = fixItemModelsWithMissingTextures();
  console.log("Item models rewritten:", audit.fixed, "missing textures:", audit.missing);
  if (audit.missingList && audit.missingList.length) {
    console.log("  sample missing:", audit.missingList.slice(0, 15).join(", "));
  }

  // 13) Drop old fakemon namespace folders
  removeOldFakemonNamespace();

  // 14) Compact runtime indexes for SpeciesRegistry / SpawnPoolLoader
  try {
    require("./build_data_indexes.js");
  } catch (e) {
    console.warn("build_data_indexes failed (run separately):", e.message);
  }

  console.log("=== DONE ===");
  console.log("Namespace: cobblemon");
  console.log("GUI textures: assets/cobblemon/textures/gui/");
  console.log("Recipes: data/cobblemon/recipe/");
}

main();

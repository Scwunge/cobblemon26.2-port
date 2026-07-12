/**
 * 1:1 sync: C:\Users\conne\Desktop\porter → src/main/resources (cobblemon namespace).
 *
 * - Copies every mappable asset/datapack file from porter
 * - Overwrites when bytes differ (exact Cobblemon content)
 * - Skips only known world-breakers (structure NBTs / structure tags)
 * - Regenerates MC 26 items/*.json definitions + content id lists
 * - Does NOT simplify blockstates (keeps full age= / multipart variants)
 *
 * Usage: node tools/sync_porter_exact.js
 * Env:   PORTER=C:\path\to\porter
 */
const fs = require("fs");
const path = require("path");
const crypto = require("crypto");

const PORTER = process.env.PORTER || "C:\\Users\\conne\\Desktop\\porter";
const ROOT = path.join(__dirname, "..");
const ASSETS = path.join(ROOT, "src", "main", "resources", "assets", "cobblemon");
const DATA = path.join(ROOT, "src", "main", "resources", "data", "cobblemon");
const GEN = path.join(DATA, "content");

const RESERVED_IDS = new Set([
  "party_badge", "pc",
  "fire_gem", "water_gem", "thunder_gem", "leaf_gem", "moon_gem",
  "wild_mon_spawn_egg", "bind_orb",
]);

const stats = {
  copied: 0,
  updated: 0,
  same: 0,
  skipped: 0,
  missingSrc: 0,
};

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

function md5(file) {
  const h = crypto.createHash("md5");
  h.update(fs.readFileSync(file));
  return h.digest("hex");
}

function rewriteNs(str) {
  return str
    .replace(/fakemon:/g, "cobblemon:")
    .replace(/"fakemon"/g, '"cobblemon"')
    .replace(/fakemon\//g, "cobblemon/")
    .replace(/\.fakemon\./g, ".cobblemon.");
}

const TEXT_EXT = new Set([
  ".json", ".mcmeta", ".toml", ".lang", ".txt", ".glsl", ".vsh", ".fsh", ".molang", ".js", ".md",
]);

/**
 * Copy src → dest. Returns 'copied' | 'updated' | 'same'
 */
function syncFile(src, dest) {
  ensureDir(path.dirname(dest));
  const ext = path.extname(src).toLowerCase();
  let buf;
  if (TEXT_EXT.has(ext)) {
    let text = fs.readFileSync(src, "utf8");
    if (text.charCodeAt(0) === 0xfeff) text = text.slice(1);
    text = rewriteNs(text);
    buf = Buffer.from(text, "utf8");
  } else {
    buf = fs.readFileSync(src);
  }

  if (fs.existsSync(dest)) {
    const existing = fs.readFileSync(dest);
    if (existing.equals(buf)) {
      stats.same++;
      return "same";
    }
    fs.writeFileSync(dest, buf);
    stats.updated++;
    return "updated";
  }
  fs.writeFileSync(dest, buf);
  stats.copied++;
  return "copied";
}

function syncTree(srcRoot, destRoot, filterFn = null) {
  if (!fs.existsSync(srcRoot)) {
    stats.missingSrc++;
    console.warn("  missing source:", srcRoot);
    return;
  }
  for (const file of walk(srcRoot)) {
    const rel = path.relative(srcRoot, file);
    if (filterFn && !filterFn(rel, file)) {
      stats.skipped++;
      continue;
    }
    syncFile(file, path.join(destRoot, rel));
  }
}

function isUnsafeStructurePath(rel) {
  const r = rel.replace(/\\/g, "/").toLowerCase();
  // NBT structures + structure tags that freeze registries when unbound
  if (r.includes("/structure/") && (r.endsWith(".nbt") || r.includes("structure_set"))) return true;
  if (r.includes("tags/worldgen/structure")) return true;
  if (r.includes("tags/worldgen/structure_set")) return true;
  if (r.includes("worldgen/structure/") && r.endsWith(".json")) return true;
  if (r.includes("worldgen/structure_set/")) return true;
  if (r.includes("worldgen/template_pool/")) return true;
  if (r.includes("has_structure")) return true;
  return false;
}

function generateItemDefinitions(itemIds) {
  const itemsDir = path.join(ASSETS, "items");
  ensureDir(itemsDir);
  let n = 0;
  for (const id of itemIds) {
    if (RESERVED_IDS.has(id)) continue;
    const body = JSON.stringify({
      model: { type: "minecraft:model", model: "cobblemon:item/" + id },
    });
    const dest = path.join(itemsDir, id + ".json");
    if (fs.existsSync(dest) && fs.readFileSync(dest, "utf8") === body) {
      stats.same++;
      continue;
    }
    fs.writeFileSync(dest, body, "utf8");
    n++;
    stats.updated++;
  }
  for (const id of RESERVED_IDS) {
    const dest = path.join(itemsDir, id + ".json");
    if (!fs.existsSync(dest)) {
      fs.writeFileSync(
        dest,
        JSON.stringify({ model: { type: "minecraft:model", model: "cobblemon:item/" + id } }),
        "utf8"
      );
      n++;
      stats.copied++;
    }
  }
  return n;
}

function writeContentLists(itemIds, blockIds) {
  ensureDir(GEN);
  const itemOnly = itemIds
    .filter((id) => !blockIds.includes(id) && !RESERVED_IDS.has(id))
    .sort();
  const blocks = blockIds.filter((id) => !RESERVED_IDS.has(id)).sort();
  fs.writeFileSync(path.join(GEN, "item_ids.txt"), itemOnly.join("\n") + "\n", "utf8");
  fs.writeFileSync(path.join(GEN, "block_ids.txt"), blocks.join("\n") + "\n", "utf8");
  const catalog = {
    generatedAt: new Date().toISOString(),
    source: "porter Cobblemon exact sync",
    namespace: "cobblemon",
    reserved: [...RESERVED_IDS],
    counts: { items: itemOnly.length, blocks: blocks.length },
  };
  fs.writeFileSync(path.join(GEN, "catalog.json"), JSON.stringify(catalog, null, 2), "utf8");
  return catalog;
}

function loadPorterIds() {
  const itemFile = path.join(PORTER, "08_item_ids", "item_ids.txt");
  const blockFile = path.join(PORTER, "08_item_ids", "block_ids.txt");
  const parse = (f) => {
    if (!fs.existsSync(f)) return [];
    return fs
      .readFileSync(f, "utf8")
      .split(/\r?\n/)
      .map((l) => l.trim().replace(/^cobblemon:/, ""))
      .filter((id) => id && !id.includes(":") && /^[a-z0-9_./-]+$/.test(id));
  };
  // Prefer model-folder filenames if id lists missing
  let items = parse(itemFile);
  let blocks = parse(blockFile);
  if (items.length === 0) {
    const modelDir = path.join(PORTER, "02_models", "item");
    items = walk(modelDir)
      .filter((f) => f.endsWith(".json"))
      .map((f) => path.basename(f, ".json"));
  }
  if (blocks.length === 0) {
    const bs = path.join(PORTER, "04_blockstates");
    blocks = walk(bs)
      .filter((f) => f.endsWith(".json"))
      .map((f) => path.basename(f, ".json"));
  }
  return { items: [...new Set(items)], blocks: [...new Set(blocks)] };
}

function mergeEnUsExtras() {
  const dest = path.join(ASSETS, "lang", "en_us.json");
  if (!fs.existsSync(dest)) return;
  let lang = {};
  try {
    lang = JSON.parse(fs.readFileSync(dest, "utf8").replace(/^\uFEFF/, ""));
  } catch (_) {
    return;
  }
  // Keep our runtime-only keys if missing from porter
  const extras = {
    "screen.cobblemon.party": "Party",
    "screen.cobblemon.pc": "PC",
    "screen.cobblemon.summary": "Summary",
    "screen.cobblemon.battle": "Battle",
    "screen.cobblemon.starter": "Choose your starter",
    "screen.cobblemon.battle_what_do": "What will you do?",
    "screen.cobblemon.battle_waiting": "Waiting…",
    "key.cobblemon.party": "Open Party",
    "key.categories.cobblemon": "Cobblemon",
    "message.cobblemon.healed": "Your party was healed!",
    "message.cobblemon.party_full": "Your party is full!",
    "message.cobblemon.party_empty": "Your party is empty!",
    "message.cobblemon.released": "Released %s.",
    "message.cobblemon.lead_set": "%s is now the lead!",
    "message.cobblemon.sent_out": "Go, %s!",
    "message.cobblemon.recalled": "%s, come back!",
    "message.cobblemon.leveled_up": "%s grew to Lv.%s!",
    "message.cobblemon.learned_move": "%s learned %s!",
    "message.cobblemon.evolved": "%s evolved into %s!",
    "itemGroup.cobblemon.plants": "Cobblemon: Plants",
  };
  let added = 0;
  for (const [k, v] of Object.entries(extras)) {
    if (lang[k] == null) {
      lang[k] = v;
      added++;
    }
  }
  if (added > 0) {
    fs.writeFileSync(dest, JSON.stringify(lang, null, 2), "utf8");
    console.log("  en_us extras kept/added:", added);
  }
}

function main() {
  console.log("=== Exact Cobblemon sync from porter ===");
  console.log("PORTER:", PORTER);
  if (!fs.existsSync(PORTER)) {
    console.error("Porter folder not found:", PORTER);
    process.exit(1);
  }

  // --- ASSETS ---
  console.log("\n[assets] textures");
  syncTree(path.join(PORTER, "01_textures"), path.join(ASSETS, "textures"));

  console.log("[assets] models");
  syncTree(path.join(PORTER, "02_models"), path.join(ASSETS, "models"));

  console.log("[assets] blockstates (exact multi-variant)");
  syncTree(path.join(PORTER, "04_blockstates"), path.join(ASSETS, "blockstates"));

  console.log("[assets] sounds");
  // Porter: 03_sounds/{sounds.json, pokemon/, move/, ...}
  // MC: assets/cobblemon/sounds.json + assets/cobblemon/sounds/**
  syncTree(path.join(PORTER, "03_sounds"), path.join(ASSETS, "sounds"), (rel) => {
    return path.basename(rel).toLowerCase() !== "sounds.json";
  });
  const soundsJson = path.join(PORTER, "03_sounds", "sounds.json");
  if (fs.existsSync(soundsJson)) {
    syncFile(soundsJson, path.join(ASSETS, "sounds.json"));
  }

  console.log("[assets] lang");
  syncTree(path.join(PORTER, "05_lang"), path.join(ASSETS, "lang"));
  mergeEnUsExtras();

  console.log("[assets] bedrock models/animations");
  syncTree(path.join(PORTER, "06_bedrock_models_animations"), path.join(ASSETS, "bedrock"));

  console.log("[assets] particles / shaders / atlases / dynamiclights");
  const p7 = path.join(PORTER, "07_particles_shaders_atlases");
  if (fs.existsSync(p7)) {
    for (const sub of ["particles", "shaders", "atlases", "dynamiclights"]) {
      const from = path.join(p7, sub);
      if (fs.existsSync(from)) {
        syncTree(from, path.join(ASSETS, sub));
      }
    }
  }

  // item model jsons mirror (porter 08)
  const itemModelJsons = path.join(PORTER, "08_item_ids", "item_model_jsons");
  if (fs.existsSync(itemModelJsons)) {
    console.log("[assets] item models from 08_item_ids");
    syncTree(itemModelJsons, path.join(ASSETS, "models", "item"));
  }

  // --- DATA ---
  console.log("\n[data] species packs");
  syncTree(path.join(PORTER, "09_data_species"), DATA, (rel) => !isUnsafeStructurePath(rel));

  console.log("[data] recipes / loot / advancements");
  for (const [sub, dest] of [
    ["recipe", "recipe"],
    ["loot_table", "loot_table"],
    ["advancement", "advancement"],
  ]) {
    const from = path.join(PORTER, "10_data_recipes_loot", sub);
    if (fs.existsSync(from)) syncTree(from, path.join(DATA, dest));
  }

  console.log("[data] spawning");
  for (const sub of ["spawn_pool_world", "spawn_detail_presets", "spawn_bait_effects", "spawn_rules", "spawning"]) {
    const from = path.join(PORTER, "11_data_spawning", sub);
    if (fs.existsSync(from)) syncTree(from, path.join(DATA, sub));
  }

  console.log("[data] tags (skip structure tags)");
  syncTree(path.join(PORTER, "12_data_tags"), path.join(DATA, "tags"), (rel) => !isUnsafeStructurePath(rel));

  console.log("[data] other datapacks (skip structures)");
  syncTree(path.join(PORTER, "13_data_other"), DATA, (rel) => !isUnsafeStructurePath(rel));

  // --- IDs + item defs ---
  console.log("\n[content] ids + items/*.json");
  const { items, blocks } = loadPorterIds();
  const catalog = writeContentLists(items, blocks);
  const defs = generateItemDefinitions(items.concat(blocks));
  console.log("  items listed:", catalog.counts.items, "blocks:", catalog.counts.blocks, "item defs written:", defs);

  // Also ensure block items have items/*.json
  generateItemDefinitions(blocks);

  // Summary report
  const report = {
    at: new Date().toISOString(),
    porter: PORTER,
    stats,
    catalog: catalog.counts,
  };
  ensureDir(GEN);
  fs.writeFileSync(path.join(GEN, "sync_report.json"), JSON.stringify(report, null, 2), "utf8");

  console.log("\n=== DONE ===");
  console.log(JSON.stringify(stats, null, 2));
  console.log("Report:", path.join(GEN, "sync_report.json"));
}

main();

/**
 * 1:1 sync: C:\Users\conne\Desktop\porter → src/main/resources (cobblemon namespace).
 *
 * - Copies every mappable asset/datapack file from porter
 * - Overwrites when bytes differ (exact Cobblemon content)
 * - Skips world-breakers (structures, processor lists, porter features/biome_modifiers)
 * - Remaps minecraft:chain → minecraft:iron_chain on text (MC 26.2)
 * - Restores the safe local worldgen pack after data sync
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

/** Only these configured/placed feature ids are kept after sync. */
const SAFE_FEATURE_IDS = ["cobblemon_ore", "overworld_decoration"];
/** Only these biome_modifier files are kept after sync. */
const SAFE_BIOME_MODIFIERS = ["01_ores.json", "02_vegetation.json"];

const stats = {
  copied: 0,
  updated: 0,
  same: 0,
  skipped: 0,
  missingSrc: 0,
  worldgenRestored: 0,
  worldgenDeleted: 0,
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

/**
 * MC 26.2: block/item `minecraft:chain` was renamed to `minecraft:iron_chain`.
 * Only remap the exact resource id — do not touch chainmail / other chain* ids.
 */
function remapMc26Ids(str) {
  // minecraft:chain not followed by more resource-id characters
  return str.replace(/minecraft:chain(?![a-z0-9_])/g, "minecraft:iron_chain");
}

function rewriteNs(str) {
  return remapMc26Ids(
    str
      .replace(/fakemon:/g, "cobblemon:")
      .replace(/"fakemon"/g, '"cobblemon"')
      .replace(/fakemon\//g, "cobblemon/")
      .replace(/\.fakemon\./g, ".cobblemon.")
  );
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

/**
 * Paths that must never be synced from porter (registry freezes / feature-order crashes).
 * Covers structures + unsafe worldgen + porter biome modifiers.
 */
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
  if (isUnsafeWorldgenPath(r)) return true;
  return false;
}

/**
 * Unsafe worldgen / biome_modifier paths (porter must not overwrite local safe pack).
 * @param {string} r already lowercased, forward-slash normalized
 */
function isUnsafeWorldgenPath(r) {
  if (r.includes("worldgen/processor_list/") || r.endsWith("worldgen/processor_list")) return true;
  if (r.includes("worldgen/configured_feature/")) return true;
  if (r.includes("worldgen/placed_feature/")) return true;
  // Keep only local 01_ores + 02_vegetation — never pull ANY porter biome_modifier (incl. coded / inject_coded)
  if (r.includes("biome_modifier")) return true;
  return false;
}

/** Allow-list filter for data trees: skip unsafe structure/worldgen paths. */
function isSafeDatapackRel(rel) {
  return !isUnsafeStructurePath(rel);
}

function writeJsonIfChanged(dest, obj) {
  const body = JSON.stringify(obj, null, 2) + "\n";
  ensureDir(path.dirname(dest));
  if (fs.existsSync(dest)) {
    const existing = fs.readFileSync(dest, "utf8").replace(/\r\n/g, "\n");
    if (existing === body || existing.trim() === body.trim()) {
      stats.same++;
      return "same";
    }
    fs.writeFileSync(dest, body, "utf8");
    stats.updated++;
    stats.worldgenRestored++;
    return "updated";
  }
  fs.writeFileSync(dest, body, "utf8");
  stats.copied++;
  stats.worldgenRestored++;
  return "copied";
}

function rmRecursiveIfExists(p) {
  if (!fs.existsSync(p)) return 0;
  let n = 0;
  const st = fs.statSync(p);
  if (st.isDirectory()) {
    for (const ent of fs.readdirSync(p, { withFileTypes: true })) {
      n += rmRecursiveIfExists(path.join(p, ent.name));
    }
    try {
      fs.rmdirSync(p);
      n++;
    } catch (_) {
      /* non-empty or race — ignore */
    }
  } else {
    fs.unlinkSync(p);
    n++;
  }
  return n;
}

/**
 * Official blockstates use age/half/mutation for crops & saccharine leaves, but this
 * port often registers plain Block (no properties). Mismatched variants → purple/black.
 * Flatten to a single model that matches registration.
 */
function restoreSafePlantBlockstates() {
  console.log("[assets] restore safe plant blockstates (no age mismatch)");
  const bsDir = path.join(ASSETS, "blockstates");
  const flat = {
    saccharine_leaves: "cobblemon:block/saccharine_leaves",
    medicinal_leek: "cobblemon:block/medicinal_leek_stage_2",
    hearty_grains: "cobblemon:block/hearty_grains_stage3",
    revival_herb: "cobblemon:block/revival_herb_stage_3",
  };
  for (const [id, model] of Object.entries(flat)) {
    writeJsonIfChanged(path.join(bsDir, id + ".json"), {
      variants: { "": { model } },
    });
  }
  // Apricorn fruit: age 0–3 + facing (porter y-rotations; stem on facing side)
  const faceRot = { east: 270, north: 180, south: null, west: 90 };
  for (const color of ["black", "blue", "green", "pink", "red", "white", "yellow"]) {
    const variants = {};
    for (let age = 0; age <= 3; age++) {
      const model =
        age === 3
          ? "cobblemon:block/" + color + "_apricorn"
          : "cobblemon:block/apricorn_stage_" + age;
      for (const [face, y] of Object.entries(faceRot)) {
        const key = "age=" + age + ",facing=" + face;
        variants[key] = y == null ? { model } : { model, y };
      }
    }
    writeJsonIfChanged(path.join(bsDir, color + "_apricorn.json"), { variants });
  }
  // Berry bushes: official multipart uses age 0–5 + rooted + mulch; BerryCropBlock only has age 0–3
  if (fs.existsSync(bsDir)) {
    for (const name of fs.readdirSync(bsDir)) {
      if (!name.endsWith("_berry.json")) continue;
      const base = name.replace(/_berry\.json$/i, "");
      writeJsonIfChanged(path.join(bsDir, name), {
        variants: {
          "age=0": { model: "cobblemon:block/berries/planted" },
          "age=1": { model: "cobblemon:block/berries/" + base + "_sprout" },
          "age=2": { model: "cobblemon:block/berries/" + base + "_young" },
          "age=3": { model: "cobblemon:block/berries/" + base + "_mature" },
        },
      });
    }
  }
  // Machines: official BS uses part/on/charge; our blocks only have FACING.
  // Full single-block meshes are rotated so FACING = front toward player
  // (placement uses lookDir.getOpposite()).
  const faceOnly = (model) => ({
    "facing=north": { model },
    "facing=south": { model, y: 180 },
    "facing=west": { model, y: 270 },
    "facing=east": { model, y: 90 },
  });
  const machineModels = {
    pc: "cobblemon:block/pc",
    healing_machine: "cobblemon:block/healing_machine_0",
    fossil_analyzer: "cobblemon:block/fossil_analyzer",
    pasture: "cobblemon:block/pasture",
    restoration_tank: "cobblemon:block/restoration_tank",
    monitor: "cobblemon:block/monitor",
    display_case: "cobblemon:block/display_case",
  };
  for (const [id, model] of Object.entries(machineModels)) {
    writeJsonIfChanged(path.join(bsDir, id + ".json"), { variants: faceOnly(model) });
  }
  // Fix mint parent model texture paths: cobblemon:blocks/ → cobblemon:block/
  const modelsDir = path.join(ASSETS, "models", "block");
  if (fs.existsSync(modelsDir)) {
    for (const name of fs.readdirSync(modelsDir)) {
      if (!/^mint_stage_\d+\.json$/i.test(name)) continue;
      const p = path.join(modelsDir, name);
      let text = fs.readFileSync(p, "utf8");
      const next = text.replace(/cobblemon:blocks\//g, "cobblemon:block/");
      if (next !== text) {
        fs.writeFileSync(p, next, "utf8");
        stats.updated++;
        console.log("  fixed path in", name);
      }
    }
  }
}

/**
 * After data sync: force the safe worldgen pack so a full sync cannot re-break worlds.
 * - Two configured + two placed features only (code-backed NoneFeatureConfiguration)
 * - Two NeoForge biome modifiers (exclude deep_dark)
 * - Strip processor_list, coded modifiers, and any extra features from porter leaks
 */
function restoreSafeWorldgenPack() {
  console.log("\n[worldgen] restore safe pack");

  const cfgDir = path.join(DATA, "worldgen", "configured_feature");
  const placedDir = path.join(DATA, "worldgen", "placed_feature");
  const modDir = path.join(DATA, "neoforge", "biome_modifier");

  writeJsonIfChanged(path.join(cfgDir, "cobblemon_ore.json"), {
    type: "cobblemon:cobblemon_ore",
    config: {},
  });
  writeJsonIfChanged(path.join(cfgDir, "overworld_decoration.json"), {
    type: "cobblemon:overworld_decoration",
    config: {},
  });

  writeJsonIfChanged(path.join(placedDir, "cobblemon_ore.json"), {
    feature: "cobblemon:cobblemon_ore",
    placement: [
      { type: "minecraft:count", count: 20 },
      { type: "minecraft:in_square" },
      {
        type: "minecraft:height_range",
        height: {
          type: "minecraft:uniform",
          min_inclusive: { absolute: -56 },
          max_inclusive: { absolute: 72 },
        },
      },
      { type: "minecraft:biome" },
    ],
  });

  writeJsonIfChanged(path.join(placedDir, "overworld_decoration.json"), {
    feature: "cobblemon:overworld_decoration",
    placement: [
      { type: "minecraft:count", count: 3 },
      { type: "minecraft:in_square" },
      {
        type: "minecraft:heightmap",
        heightmap: "MOTION_BLOCKING_NO_LEAVES",
      },
      { type: "minecraft:biome" },
    ],
  });

  // Biome modifiers (exclude deep_dark) — always ensure present
  writeJsonIfChanged(path.join(modDir, "01_ores.json"), {
    type: "neoforge:add_features",
    biomes: {
      type: "neoforge:and",
      values: [
        "#minecraft:is_overworld",
        { type: "neoforge:not", value: "minecraft:deep_dark" },
      ],
    },
    features: "cobblemon:cobblemon_ore",
    step: "underground_ores",
  });
  writeJsonIfChanged(path.join(modDir, "02_vegetation.json"), {
    type: "neoforge:add_features",
    biomes: {
      type: "neoforge:and",
      values: [
        "#minecraft:is_overworld",
        { type: "neoforge:not", value: "minecraft:deep_dark" },
      ],
    },
    features: "cobblemon:overworld_decoration",
    step: "vegetal_decoration",
  });

  // Delete any extra configured/placed features
  for (const [dir, kind] of [
    [cfgDir, "configured_feature"],
    [placedDir, "placed_feature"],
  ]) {
    if (!fs.existsSync(dir)) continue;
    for (const name of fs.readdirSync(dir)) {
      const base = name.replace(/\.json$/i, "");
      if (!SAFE_FEATURE_IDS.includes(base)) {
        const p = path.join(dir, name);
        stats.worldgenDeleted += rmRecursiveIfExists(p);
        console.log("  deleted extra", kind + ":", name);
      }
    }
  }

  // Delete coded + any other biome_modifier besides the two safe ones
  if (fs.existsSync(modDir)) {
    for (const name of fs.readdirSync(modDir)) {
      if (!SAFE_BIOME_MODIFIERS.includes(name)) {
        const p = path.join(modDir, name);
        stats.worldgenDeleted += rmRecursiveIfExists(p);
        console.log("  deleted biome_modifier:", name);
      }
    }
  }
  const coded = path.join(modDir, "coded.json");
  if (fs.existsSync(coded)) {
    stats.worldgenDeleted += rmRecursiveIfExists(coded);
    console.log("  deleted biome_modifier: coded.json");
  }

  // processor_list must not exist
  const procList = path.join(DATA, "worldgen", "processor_list");
  if (fs.existsSync(procList)) {
    stats.worldgenDeleted += rmRecursiveIfExists(procList);
    console.log("  deleted worldgen/processor_list");
  }

  console.log(
    "  safe features:",
    SAFE_FEATURE_IDS.join(", "),
    "| modifiers:",
    SAFE_BIOME_MODIFIERS.join(", ")
  );
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
  // Keep our runtime-only keys (porter overwrites lang; always re-apply port HUD strings)
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
    // Client HUD tips (not in official porter lang)
    "hud.cobblemon.throw_title": "Throw out your Cobblemon",
    "hud.cobblemon.throw_hint": "Press R to send out / recall",
    "hud.cobblemon.choose_starter": "Press M to choose a starter",
    "hud.cobblemon.throw": "Throw out your Cobblemon",
    // Starter select UI (our screen keys)
    "screen.cobblemon.starter_title": "Choose your starter!",
    "screen.cobblemon.starter_subtitle": "Pick one to begin your journey",
    "screen.cobblemon.starter_picked": "You chose %s",
    "screen.cobblemon.starter_confirm": "I choose you!",
  };
  // Always force port-only HUD / starter keys; other extras only if missing
  const forceKeys = new Set([
    "hud.cobblemon.throw_title",
    "hud.cobblemon.throw_hint",
    "hud.cobblemon.choose_starter",
    "hud.cobblemon.throw",
    "screen.cobblemon.starter_title",
    "screen.cobblemon.starter_subtitle",
    "screen.cobblemon.starter_picked",
    "screen.cobblemon.starter_confirm",
    "screen.cobblemon.starter",
  ]);
  let added = 0;
  for (const [k, v] of Object.entries(extras)) {
    if (forceKeys.has(k) || lang[k] == null) {
      if (lang[k] !== v) added++;
      lang[k] = v;
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
  // Port registers many plants as plain Blocks (no age/half) — flatten those blockstates
  restoreSafePlantBlockstates();

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
  syncTree(path.join(PORTER, "09_data_species"), DATA, isSafeDatapackRel);

  console.log("[data] recipes / loot / advancements");
  for (const [sub, dest] of [
    ["recipe", "recipe"],
    ["loot_table", "loot_table"],
    ["advancement", "advancement"],
  ]) {
    const from = path.join(PORTER, "10_data_recipes_loot", sub);
    if (fs.existsSync(from)) syncTree(from, path.join(DATA, dest), isSafeDatapackRel);
  }

  console.log("[data] spawning");
  for (const sub of ["spawn_pool_world", "spawn_detail_presets", "spawn_bait_effects", "spawn_rules", "spawning"]) {
    const from = path.join(PORTER, "11_data_spawning", sub);
    if (fs.existsSync(from)) syncTree(from, path.join(DATA, sub), isSafeDatapackRel);
  }

  console.log("[data] tags (skip structure / worldgen tags that break worlds)");
  syncTree(path.join(PORTER, "12_data_tags"), path.join(DATA, "tags"), isSafeDatapackRel);

  console.log("[data] other datapacks (skip structures / unsafe worldgen / biome_modifiers)");
  syncTree(path.join(PORTER, "13_data_other"), DATA, isSafeDatapackRel);

  // Force safe worldgen after any data tree sync (cannot re-break worlds)
  restoreSafeWorldgenPack();

  // --- IDs + item defs ---
  console.log("\n[content] ids + items/*.json");
  const { items, blocks } = loadPorterIds();
  const catalog = writeContentLists(items, blocks);
  const defs = generateItemDefinitions(items.concat(blocks));
  console.log("  items listed:", catalog.counts.items, "blocks:", catalog.counts.blocks, "item defs written:", defs);

  // Also ensure block items have items/*.json
  generateItemDefinitions(blocks);

  // Balls + Pokédex: GUI flat icon, hand/world 3D (generateItemDefinitions would flatten them)
  console.log("[content] wire ball + pokedex item models (2D gui / 3D world)");
  for (const script of ["wire_ball_item_models.js", "wire_pokedex_item_models.js"]) {
    try {
      require("child_process").execFileSync(
        process.execPath,
        [path.join(__dirname, script)],
        { stdio: "inherit", cwd: ROOT }
      );
    } catch (e) {
      console.warn("  " + script + " failed:", e.message);
    }
  }

  // Summary report
  const report = {
    at: new Date().toISOString(),
    porter: PORTER,
    stats,
    catalog: catalog.counts,
    safeWorldgen: {
      features: SAFE_FEATURE_IDS,
      biomeModifiers: SAFE_BIOME_MODIFIERS,
    },
  };
  ensureDir(GEN);
  fs.writeFileSync(path.join(GEN, "sync_report.json"), JSON.stringify(report, null, 2), "utf8");

  console.log("\n=== DONE ===");
  console.log(JSON.stringify(stats, null, 2));
  console.log("Report:", path.join(GEN, "sync_report.json"));
}

main();

/**
 * Full merge: wipe-overlay our resources with Desktop/porter Cobblemon extract,
 * then rebuild runtime indexes. Porter wins on all asset/data conflicts.
 *
 * Does NOT touch Java source. Re-applies only post-import wiring we need:
 *  - species_assets (prefer base-form geo, not alolan)
 *  - disabled no-model species list
 *  - prune spawn rules for disabled
 *  - evo_index from species data
 *  - ball item defs: GUI flat + world 3D
 *
 * Usage: node tools/full_merge_from_porter.js
 */
const fs = require("fs");
const path = require("path");
const { spawnSync } = require("child_process");

const ROOT = path.join(__dirname, "..");
const PORTER = process.env.PORTER || "C:\\Users\\conne\\Desktop\\porter";

function run(label, script, args = []) {
  console.log("\n########", label, "########");
  const r = spawnSync(process.execPath, [path.join(__dirname, script), ...args], {
    cwd: ROOT,
    env: { ...process.env, PORTER },
    encoding: "utf8",
    maxBuffer: 64 * 1024 * 1024,
  });
  if (r.stdout) process.stdout.write(r.stdout);
  if (r.stderr) process.stderr.write(r.stderr);
  if (r.status !== 0) {
    console.error("FAILED:", script, "exit", r.status);
    process.exit(r.status || 1);
  }
}

function rebuildDisabledAndPruneSpawns() {
  console.log("\n######## disabled species + spawn prune ########");
  const content = path.join(ROOT, "src/main/resources/data/cobblemon/content");
  const assetsPath = path.join(content, "species_assets.json");
  const indexPath = path.join(content, "species_index.json");
  const spawnPath = path.join(content, "spawn_index.json");
  const evoPath = path.join(content, "evo_index.json");

  const assetsRoot = JSON.parse(fs.readFileSync(assetsPath, "utf8"));
  const speciesRoot = JSON.parse(fs.readFileSync(indexPath, "utf8"));
  const spawnRoot = JSON.parse(fs.readFileSync(spawnPath, "utf8"));

  // Any species without a real (non-placeholder) geo is disabled
  const playable = new Set();
  const disabled = [];
  for (const [id, a] of Object.entries(assetsRoot.assets || {})) {
    if (a && a.geo && !a.placeholder && !String(a.folder || "").startsWith("_placeholder_")) {
      playable.add(id);
    }
  }
  for (const id of Object.keys(speciesRoot.species || {})) {
    if (!playable.has(id)) disabled.push(id);
  }
  disabled.sort();

  fs.writeFileSync(
    path.join(content, "disabled_species.json"),
    JSON.stringify({
      version: 2,
      reason: "no_real_cobblemon_model_in_porter",
      count: disabled.length,
      species: disabled,
    })
  );
  console.log("Playable (real models):", playable.size);
  console.log("Disabled (no model):", disabled.length);

  // Mark assets for disabled if missing
  for (const id of disabled) {
    if (!assetsRoot.assets[id]) {
      assetsRoot.assets[id] = {
        folder: null,
        dex: speciesRoot.species[id]?.nationalPokedexNumber || 0,
        texture: null,
        geo: null,
        animation: null,
        placeholder: true,
      };
    } else {
      assetsRoot.assets[id].placeholder = true;
    }
  }
  assetsRoot.count = Object.keys(assetsRoot.assets).length;
  assetsRoot.version = 5;
  fs.writeFileSync(assetsPath, JSON.stringify(assetsRoot));

  // Prune spawns
  const before = (spawnRoot.entries || []).length;
  const disabledSet = new Set(disabled);
  spawnRoot.entries = (spawnRoot.entries || []).filter(
    (e) => !disabledSet.has(String(e.speciesId || "").toLowerCase())
  );
  spawnRoot.count = spawnRoot.entries.length;
  spawnRoot.version = 3;
  fs.writeFileSync(spawnPath, JSON.stringify(spawnRoot));
  console.log("Spawn rules:", before, "->", spawnRoot.count);

  // Rebuild evo_index from species_index (level_up only, skip disabled targets)
  const evolutions = {};
  let evoCount = 0;
  for (const [id, sp] of Object.entries(speciesRoot.species || {})) {
    if (disabledSet.has(id)) continue;
    for (const ev of sp.evolutions || []) {
      if (!ev || ev.variant !== "level_up") continue;
      const req = (ev.requirements || []).find(
        (r) => r && (r.variant === "level" || r.minLevel != null)
      );
      const level = req ? req.minLevel ?? req.level ?? 0 : 0;
      if (!level) continue;
      let into = typeof ev.result === "string" ? ev.result.split(/\s+/)[0].toLowerCase() : null;
      if (!into || disabledSet.has(into)) continue;
      if (evolutions[id]) continue;
      evolutions[id] = { into, level };
      evoCount++;
    }
  }
  fs.writeFileSync(evoPath, JSON.stringify({ version: 3, count: evoCount, evolutions }));
  console.log("Level-up evo chains:", evoCount);
}

function main() {
  console.log("====================================================");
  console.log(" FULL MERGE FROM PORTER (porter wins on conflicts)");
  console.log(" Porter:", PORTER);
  console.log(" Mod:   ", ROOT);
  console.log("====================================================");

  if (!fs.existsSync(PORTER)) {
    console.error("Porter folder not found:", PORTER);
    process.exit(1);
  }

  // 1) Full asset/data import — overwrites resources with porter
  run("import_porter_all.js (porter → assets/data)", "import_porter_all.js");

  // 2) Species/spawn indexes (porter data packs)
  // import_porter_all already requires build_data_indexes — run again to be safe
  run("build_data_indexes.js", "build_data_indexes.js");

  // 3) Species assets: correct geo/texture binding (base form, not alolan first)
  run("build_species_assets.js", "build_species_assets.js");

  // 4) Disabled list + prune spawns + evo index
  rebuildDisabledAndPruneSpawns();

  // 5) Balls: GUI 2D icons, world 3D cubes
  run("wire_ball_item_models.js", "wire_ball_item_models.js");

  // 6) Keep party/UI sprites if present (not in porter — leave alone)
  const sprites = path.join(
    ROOT,
    "src/main/resources/assets/cobblemon/textures/entity/sprites/front"
  );
  if (fs.existsSync(sprites)) {
    const n = fs.readdirSync(sprites).filter((f) => f.endsWith(".png")).length;
    console.log("\nUI sprites still present:", n, "(kept — not from porter)");
  } else {
    console.log("\nNo UI sprites folder — optional: node tools/download_pokemondb_sprites.js");
  }

  console.log("\n====================================================");
  console.log(" MERGE COMPLETE");
  console.log(" Next: .\\gradlew.bat compileJava  then  .\\gradlew.bat runClient");
  console.log("====================================================");
}

main();

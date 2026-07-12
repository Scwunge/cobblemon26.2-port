/**
 * Scan textures/bedrock pokemon folders → data/cobblemon/content/species_assets.json
 * Keys are species ids (bulbasaur), not folder names.
 *
 * Prefers base-form geo/texture (not alolan/galarian/etc.) so Rattata doesn't
 * silently bind to rattata_alolan.geo.json.
 */
const fs = require("fs");
const path = require("path");

const ROOT = path.join(__dirname, "..");
const TEX = path.join(ROOT, "src/main/resources/assets/cobblemon/textures/pokemon");
const GEO = path.join(ROOT, "src/main/resources/assets/cobblemon/bedrock/pokemon/models");
const ANIM = path.join(ROOT, "src/main/resources/assets/cobblemon/bedrock/pokemon/animations");
const OUT = path.join(ROOT, "src/main/resources/data/cobblemon/content/species_assets.json");
const INDEX = path.join(ROOT, "src/main/resources/data/cobblemon/content/species_index.json");

const FORM_HINT = /alolan|galarian|hisuian|paldean|mega|gmax|gigantamax|white|black|therian|origin|attack|defense|speed|sandy|trash|plant|east|west|spring|summer|autumn|winter|school|solo|dusk|midday|midnight|zen|complete|10|50|shield|blade|crowned|ice|shadow|dawn|dusk|baile|pom|pau|sensu|amber|blade|bloodmoon/i;

function scoreGeo(file, id) {
  const f = file.toLowerCase();
  const base = f.replace(/\.geo\.json$/, "");
  // Exact base form
  if (base === id) return 100;
  if (base === id + "_male") return 90;
  if (base === id + "_female") return 85;
  // Starts with id but is a regional/form variant — deprioritize hard
  if (FORM_HINT.test(base) && base.startsWith(id)) return 5;
  if (base.startsWith(id + "_") && !FORM_HINT.test(base)) return 40;
  if (base.startsWith(id)) return 20;
  return 0;
}

function pickGeo(files, id) {
  if (!files.length) return null;
  const ranked = files
    .map((f) => ({ f, s: scoreGeo(f, id) }))
    .sort((a, b) => b.s - a.s || a.f.localeCompare(b.f));
  return ranked[0].f;
}

function pickAnim(files, id) {
  const usable = files.filter((f) => !f.toLowerCase().includes("sleep"));
  const list = usable.length ? usable : files;
  // Prefer exact species animation over form variants
  const exact = list.find((f) => f.toLowerCase() === id + ".animation.json");
  if (exact) return exact;
  const base = list.find((f) => {
    const n = f.toLowerCase();
    return n.startsWith(id) && !FORM_HINT.test(n);
  });
  if (base) return base;
  return list[0] || null;
}

function pickTexture(files, id) {
  const noShiny = files.filter((f) => f.endsWith(".png") && !f.toLowerCase().includes("shiny"));
  const exact = noShiny.find((f) => f.toLowerCase() === id + ".png");
  if (exact) return exact;
  // Prefer non-form textures
  const ranked = noShiny
    .map((f) => {
      const n = f.toLowerCase().replace(/\.png$/, "");
      let s = 0;
      if (n === id) s = 100;
      else if (n === id + "_male") s = 90;
      else if (n.startsWith(id) && !FORM_HINT.test(n)) s = 50;
      else if (n.startsWith(id)) s = 10;
      return { f, s };
    })
    .sort((a, b) => b.s - a.s);
  return ranked[0] ? ranked[0].f : null;
}

const out = {};
// Preserve placeholder entries from previous full build if present
let prev = {};
if (fs.existsSync(OUT)) {
  try {
    prev = JSON.parse(fs.readFileSync(OUT, "utf8")).assets || {};
  } catch (_) {}
}

if (!fs.existsSync(TEX)) {
  console.error("missing", TEX);
  process.exit(1);
}

for (const name of fs.readdirSync(TEX)) {
  const full = path.join(TEX, name);
  if (!fs.statSync(full).isDirectory()) continue;
  if (name.startsWith("_placeholder_")) {
    // Keep existing placeholder entry if any
    const m = name.match(/^_placeholder_\d+_(.+)$/);
    const id = (m ? m[1] : name).toLowerCase();
    if (prev[id] && prev[id].placeholder) {
      out[id] = prev[id];
    }
    continue;
  }
  const m = name.match(/^(\d+)_(.+)$/);
  const id = (m ? m[2] : name).toLowerCase();
  const files = fs.readdirSync(full);
  const png = pickTexture(files, id);
  if (!png) continue;

  let geoFile = null;
  const gdir = path.join(GEO, name);
  if (fs.existsSync(gdir)) {
    const gs = fs.readdirSync(gdir).filter((f) => f.endsWith(".geo.json"));
    geoFile = pickGeo(gs, id);
  }
  let animFile = null;
  const adir = path.join(ANIM, name);
  if (fs.existsSync(adir)) {
    const as = fs.readdirSync(adir).filter((f) => f.endsWith(".animation.json"));
    animFile = pickAnim(as, id);
  }
  out[id] = {
    folder: name,
    dex: m ? parseInt(m[1], 10) : 0,
    texture: name + "/" + png,
    geo: geoFile ? name + "/" + geoFile : null,
    animation: animFile ? name + "/" + animFile : null,
    placeholder: false,
  };
}

// Ensure every species_index entry still has an asset row (placeholders already in prev)
if (fs.existsSync(INDEX)) {
  const species = JSON.parse(fs.readFileSync(INDEX, "utf8")).species || {};
  for (const id of Object.keys(species)) {
    if (!out[id] && prev[id]) {
      out[id] = prev[id];
    }
  }
}

fs.mkdirSync(path.dirname(OUT), { recursive: true });
fs.writeFileSync(
  OUT,
  JSON.stringify({ version: 4, count: Object.keys(out).length, assets: out })
);
console.log("species_assets:", Object.keys(out).length);
console.log(" rattata:", out.rattata);
console.log(" raichu:", out.raichu);
console.log(" raticate:", out.raticate);
console.log(" bulbasaur:", out.bulbasaur);

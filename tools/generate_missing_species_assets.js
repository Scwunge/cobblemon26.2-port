/**
 * For the ~174 species with data but no Cobblemon art in the extract:
 * - create a simple colored texture (type-based)
 * - reuse a tiny generic geo + animation as placeholder 3D
 * - register them in species_assets.json
 *
 * Also rebuilds species_assets for all existing folders.
 *
 * Usage: node tools/generate_missing_species_assets.js
 */
const fs = require("fs");
const path = require("path");
const zlib = require("zlib");

const ROOT = path.join(__dirname, "..");
const TEX_ROOT = path.join(ROOT, "src/main/resources/assets/cobblemon/textures/pokemon");
const GEO_ROOT = path.join(ROOT, "src/main/resources/assets/cobblemon/bedrock/pokemon/models");
const ANIM_ROOT = path.join(ROOT, "src/main/resources/assets/cobblemon/bedrock/pokemon/animations");
const INDEX = path.join(ROOT, "src/main/resources/data/cobblemon/content/species_index.json");
const OUT = path.join(ROOT, "src/main/resources/data/cobblemon/content/species_assets.json");

const TYPE_RGB = {
  fire: [232, 93, 4], water: [76, 201, 240], grass: [82, 183, 136], electric: [254, 228, 64],
  ground: [212, 163, 115], flying: [202, 240, 248], fighting: [193, 18, 31], dragon: [90, 24, 154],
  dark: [60, 9, 108], psychic: [255, 107, 203], rock: [173, 181, 189], ice: [144, 224, 239],
  steel: [141, 153, 174], fairy: [255, 175, 204], bug: [167, 201, 87], poison: [155, 93, 229],
  ghost: [123, 44, 191], normal: [222, 226, 230],
};

function crc32(buf) {
  let c = ~0;
  for (let i = 0; i < buf.length; i++) {
    c ^= buf[i];
    for (let k = 0; k < 8; k++) c = (c >>> 1) ^ (0xedb88320 & -(c & 1));
  }
  return ~c >>> 0;
}

function chunk(type, data) {
  const len = Buffer.alloc(4);
  len.writeUInt32BE(data.length);
  const t = Buffer.from(type);
  const crc = Buffer.alloc(4);
  crc.writeUInt32BE(crc32(Buffer.concat([t, data])));
  return Buffer.concat([len, t, data, crc]);
}

/** Minimal solid-color 32x32 PNG */
function solidPng(r, g, b) {
  const w = 32, h = 32;
  const raw = Buffer.alloc((w * 4 + 1) * h);
  for (let y = 0; y < h; y++) {
    const row = y * (w * 4 + 1);
    raw[row] = 0; // filter none
    for (let x = 0; x < w; x++) {
      const i = row + 1 + x * 4;
      // simple ball-like gradient
      const cx = x - 15.5, cy = y - 15.5;
      const d = Math.sqrt(cx * cx + cy * cy);
      const shade = d < 14 ? 1 - d / 28 : 0.15;
      raw[i] = Math.min(255, Math.floor(r * shade + 40));
      raw[i + 1] = Math.min(255, Math.floor(g * shade + 40));
      raw[i + 2] = Math.min(255, Math.floor(b * shade + 40));
      raw[i + 3] = d < 15 ? 255 : 0;
    }
  }
  const ihdr = Buffer.alloc(13);
  ihdr.writeUInt32BE(w, 0);
  ihdr.writeUInt32BE(h, 4);
  ihdr[8] = 8; ihdr[9] = 6; // 8-bit RGBA
  const compressed = zlib.deflateSync(raw);
  return Buffer.concat([
    Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]),
    chunk("IHDR", ihdr),
    chunk("IDAT", compressed),
    chunk("IEND", Buffer.alloc(0)),
  ]);
}

// Minimal sphere-like geo (reused for all placeholders)
const PLACEHOLDER_GEO = {
  format_version: "1.12.0",
  "minecraft:geometry": [{
    description: {
      identifier: "geometry.cobblemon_placeholder",
      texture_width: 32,
      texture_height: 32,
      visible_bounds_width: 2,
      visible_bounds_height: 2.5,
      visible_bounds_offset: [0, 0.75, 0],
    },
    bones: [
      { name: "root", pivot: [0, 0, 0] },
      {
        name: "body", parent: "root", pivot: [0, 8, 0],
        cubes: [{ origin: [-6, 2, -6], size: [12, 12, 12], uv: [0, 0] }],
      },
    ],
  }],
};

const PLACEHOLDER_ANIM = {
  format_version: "1.8.0",
  animations: {
    "animation.placeholder.ground_idle": {
      loop: true,
      animation_length: 2,
      bones: {
        body: {
          rotation: {
            "0.0": [0, 0, 0],
            "1.0": [0, 8, 0],
            "2.0": [0, 0, 0],
          },
        },
      },
    },
  },
};

function ensureDir(d) {
  fs.mkdirSync(d, { recursive: true });
}

function buildExistingAssets() {
  const out = {};
  if (!fs.existsSync(TEX_ROOT)) return out;
  for (const name of fs.readdirSync(TEX_ROOT)) {
    const full = path.join(TEX_ROOT, name);
    if (!fs.statSync(full).isDirectory()) continue;
    if (name.startsWith("_placeholder_")) continue;
    const m = name.match(/^(\d+)_(.+)$/);
    const id = (m ? m[2] : name).toLowerCase();
    const files = fs.readdirSync(full).filter((f) => f.endsWith(".png") && !f.includes("shiny"));
    let png =
      files.find((f) => f.toLowerCase() === id + ".png") ||
      files.find((f) => f.toLowerCase().startsWith(id)) ||
      files[0];
    if (!png) continue;
    let geoFile = null, animFile = null;
    const gdir = path.join(GEO_ROOT, name);
    if (fs.existsSync(gdir)) {
      const gs = fs.readdirSync(gdir).filter((f) => f.endsWith(".geo.json"));
      geoFile = gs.find((f) => f.toLowerCase().startsWith(id)) || gs[0] || null;
    }
    const adir = path.join(ANIM_ROOT, name);
    if (fs.existsSync(adir)) {
      const as = fs.readdirSync(adir).filter((f) => f.endsWith(".animation.json"));
      animFile =
        as.find((f) => f.toLowerCase().startsWith(id) && !f.includes("sleep")) ||
        as[0] ||
        null;
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
  return out;
}

function main() {
  console.log("=== Full species asset rebuild + placeholders ===");
  const index = JSON.parse(fs.readFileSync(INDEX, "utf8"));
  const assets = buildExistingAssets();
  console.log("Existing real assets:", Object.keys(assets).length);

  // Shared placeholder geo/anim once
  const phFolder = "_placeholder_generic";
  const phGeoDir = path.join(GEO_ROOT, phFolder);
  const phAnimDir = path.join(ANIM_ROOT, phFolder);
  ensureDir(phGeoDir);
  ensureDir(phAnimDir);
  fs.writeFileSync(path.join(phGeoDir, "placeholder.geo.json"), JSON.stringify(PLACEHOLDER_GEO));
  fs.writeFileSync(path.join(phAnimDir, "placeholder.animation.json"), JSON.stringify(PLACEHOLDER_ANIM));

  let created = 0;
  for (const [id, sp] of Object.entries(index.species)) {
    if (assets[id] && assets[id].geo) continue;

    const dex = sp.nationalPokedexNumber || 0;
    const folder = `_placeholder_${String(dex).padStart(4, "0")}_${id}`;
    const texDir = path.join(TEX_ROOT, folder);
    ensureDir(texDir);

    const type = (sp.primaryType || "normal").toLowerCase();
    const rgb = TYPE_RGB[type] || TYPE_RGB.normal;
    fs.writeFileSync(path.join(texDir, id + ".png"), solidPng(rgb[0], rgb[1], rgb[2]));

    // point geo/anim at shared placeholder (or per-folder copies for path simplicity)
    const gdir = path.join(GEO_ROOT, folder);
    const adir = path.join(ANIM_ROOT, folder);
    ensureDir(gdir);
    ensureDir(adir);
    fs.writeFileSync(path.join(gdir, id + ".geo.json"), JSON.stringify(PLACEHOLDER_GEO));
    fs.writeFileSync(
      path.join(adir, id + ".animation.json"),
      JSON.stringify(PLACEHOLDER_ANIM)
    );

    assets[id] = {
      folder,
      dex,
      texture: folder + "/" + id + ".png",
      geo: folder + "/" + id + ".geo.json",
      animation: folder + "/" + id + ".animation.json",
      placeholder: true,
    };
    created++;
  }

  ensureDir(path.dirname(OUT));
  fs.writeFileSync(
    OUT,
    JSON.stringify({ version: 2, count: Object.keys(assets).length, assets }, null, 0)
  );
  console.log("Placeholders created:", created);
  console.log("Total species assets now:", Object.keys(assets).length);
  console.log("Wrote", OUT);
}

main();

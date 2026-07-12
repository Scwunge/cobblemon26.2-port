/**
 * Replace cube placeholders with sprite-billboard 3D models using the
 * pokemondb minecraftified front sprites we already generated.
 *
 * For each species marked placeholder in species_assets.json:
 *  - copy textures/entity/sprites/front/<id>.png → pokemon texture folder
 *  - write crossed-plane geo (looks like the mon, not a hollow cube)
 *  - simple idle bob animation
 *
 * Usage: node tools/upgrade_placeholders_to_sprites.js
 */
const fs = require("fs");
const path = require("path");

const ROOT = path.join(__dirname, "..");
const ASSETS = path.join(ROOT, "src/main/resources/assets/cobblemon");
const INDEX = path.join(ROOT, "src/main/resources/data/cobblemon/content/species_assets.json");
const SPRITES = path.join(ASSETS, "textures/entity/sprites/front");
const TEX = path.join(ASSETS, "textures/pokemon");
const GEO = path.join(ASSETS, "bedrock/pokemon/models");
const ANIM = path.join(ASSETS, "bedrock/pokemon/animations");

/** Crossed sprite planes — classic MC fake-3D, uses full 64×64 portrait. */
function spriteGeo(id) {
  return {
    format_version: "1.12.0",
    "minecraft:geometry": [
      {
        description: {
          identifier: "geometry.cobblemon." + id + "_sprite",
          texture_width: 64,
          texture_height: 64,
          visible_bounds_width: 3,
          visible_bounds_height: 3.5,
          visible_bounds_offset: [0, 1.25, 0],
        },
        bones: [
          { name: "root", pivot: [0, 0, 0] },
          {
            name: "body",
            parent: "root",
            pivot: [0, 8, 0],
            cubes: [
              // Front-facing plate (main view)
              { origin: [-8, 0, -0.5], size: [16, 16, 1], uv: [0, 0] },
              // Side-facing plate (cross)
              { origin: [-0.5, 0, -8], size: [1, 16, 16], uv: [0, 0] },
              // Slight thickness so it reads as solid from more angles
              { origin: [-7, 1, -1], size: [14, 14, 2], inflate: -0.5, uv: [0, 0] },
            ],
          },
          {
            name: "base",
            parent: "root",
            pivot: [0, 0, 0],
            cubes: [
              // tiny ground shadow plate
              { origin: [-4, 0, -4], size: [8, 0.5, 8], uv: [0, 56] },
            ],
          },
        ],
      },
    ],
  };
}

function spriteAnim(id) {
  return {
    format_version: "1.8.0",
    animations: {
      ["animation." + id + ".ground_idle"]: {
        loop: true,
        animation_length: 2,
        bones: {
          body: {
            position: {
              "0.0": [0, 0, 0],
              "1.0": [0, 0.8, 0],
              "2.0": [0, 0, 0],
            },
            rotation: {
              "0.0": [0, 0, 0],
              "1.0": [0, 6, 0],
              "2.0": [0, 0, 0],
            },
          },
        },
      },
    },
  };
}

function main() {
  const root = JSON.parse(fs.readFileSync(INDEX, "utf8"));
  const assets = root.assets || {};
  let ok = 0,
    skip = 0,
    fail = 0;

  for (const [id, entry] of Object.entries(assets)) {
    if (!entry.placeholder) {
      skip++;
      continue;
    }
    const spritePath = path.join(SPRITES, id + ".png");
    if (!fs.existsSync(spritePath)) {
      console.warn("no sprite", id);
      fail++;
      continue;
    }

    const folder = entry.folder || `_placeholder_${String(entry.dex || 0).padStart(4, "0")}_${id}`;
    const texDir = path.join(TEX, folder);
    const geoDir = path.join(GEO, folder);
    const animDir = path.join(ANIM, folder);
    fs.mkdirSync(texDir, { recursive: true });
    fs.mkdirSync(geoDir, { recursive: true });
    fs.mkdirSync(animDir, { recursive: true });

    // Real portrait as entity texture
    fs.copyFileSync(spritePath, path.join(texDir, id + ".png"));

    fs.writeFileSync(path.join(geoDir, id + ".geo.json"), JSON.stringify(spriteGeo(id)));
    fs.writeFileSync(path.join(animDir, id + ".animation.json"), JSON.stringify(spriteAnim(id)));

    entry.texture = folder + "/" + id + ".png";
    entry.geo = folder + "/" + id + ".geo.json";
    entry.animation = folder + "/" + id + ".animation.json";
    entry.placeholder = true;
    entry.spriteBillboard = true;
    ok++;
  }

  fs.writeFileSync(INDEX, JSON.stringify({ version: 3, count: Object.keys(assets).length, assets }));
  console.log("Upgraded placeholders to sprite billboards:", ok);
  console.log("Skipped (real models):", skip);
  console.log("Failed (no sprite):", fail);
}

main();

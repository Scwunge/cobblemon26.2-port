/**
 * Point every catch-ball items/*.json at:
 *   GUI / FIXED → flat 2D icon
 *   everything else (hand, ground, throw) → 3D cube model
 *
 * Usage: node tools/wire_ball_item_models.js
 */
const fs = require("fs");
const path = require("path");

const ROOT = path.join(__dirname, "..");
const ITEMS = path.join(ROOT, "src/main/resources/assets/cobblemon/items");
const MODELS = path.join(ROOT, "src/main/resources/assets/cobblemon/models/item");

const SKIP = new Set(["iron_ball", "light_ball", "smoke_ball"]);

function hasModel(id) {
  return fs.existsSync(path.join(MODELS, id + "_model.json"));
}

function hasIcon(id) {
  return fs.existsSync(path.join(MODELS, id + ".json"));
}

function buildSelect(id) {
  // Flat for GUI inventory; 3D cube for world / hands / ground (thrown)
  return {
    model: {
      type: "minecraft:select",
      property: "minecraft:display_context",
      cases: [
        {
          when: ["gui", "fixed"],
          model: {
            type: "minecraft:model",
            model: `cobblemon:item/${id}`,
          },
        },
      ],
      fallback: {
        type: "minecraft:model",
        model: `cobblemon:item/${id}_model`,
      },
    },
  };
}

function main() {
  const files = fs.readdirSync(ITEMS).filter((f) => f.endsWith("_ball.json") || f === "poke_ball.json");
  // also match *ball.json without model suffix
  const ids = new Set();
  for (const f of fs.readdirSync(ITEMS)) {
    if (!f.endsWith(".json")) continue;
    if (f.endsWith("_model.json")) continue;
    if (!f.includes("ball")) continue;
    const id = f.replace(/\.json$/, "");
    if (SKIP.has(id)) continue;
    if (!id.endsWith("_ball") && id !== "poke_ball") continue;
    ids.add(id);
  }

  let n = 0;
  for (const id of [...ids].sort()) {
    if (!hasIcon(id)) {
      console.warn("skip no icon model", id);
      continue;
    }
    if (!hasModel(id)) {
      console.warn("skip no 3d model", id);
      continue;
    }
    const out = path.join(ITEMS, id + ".json");
    fs.writeFileSync(out, JSON.stringify(buildSelect(id)));
    n++;
  }
  console.log("Wired", n, "ball item defs → 2D gui / 3D world");
}

main();

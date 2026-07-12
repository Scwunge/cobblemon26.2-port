/**
 * Point each real Pokédex item (7 colours) at:
 *   GUI / FIXED → flat 2D icon
 *   hand / world → 3D model (pokedex_<color>_model)
 *
 * Usage: node tools/wire_pokedex_item_models.js
 */
const fs = require("fs");
const path = require("path");

const ROOT = path.join(__dirname, "..");
const ITEMS = path.join(ROOT, "src/main/resources/assets/cobblemon/items");
const MODELS = path.join(ROOT, "src/main/resources/assets/cobblemon/models/item");

const COLORS = ["red", "blue", "green", "yellow", "pink", "black", "white"];

function buildSelect(color) {
  const id = "pokedex_" + color;
  return {
    model: {
      type: "minecraft:select",
      property: "minecraft:display_context",
      cases: [
        {
          when: ["gui", "fixed"],
          model: { type: "minecraft:model", model: "cobblemon:item/" + id },
        },
      ],
      fallback: {
        type: "minecraft:model",
        model: "cobblemon:item/" + id + "_model",
      },
    },
  };
}

function main() {
  fs.mkdirSync(ITEMS, { recursive: true });
  let n = 0;
  for (const color of COLORS) {
    const id = "pokedex_" + color;
    const icon = path.join(MODELS, id + ".json");
    const model = path.join(MODELS, id + "_model.json");
    if (!fs.existsSync(icon)) {
      console.warn("skip missing icon model", id);
      continue;
    }
    if (!fs.existsSync(model)) {
      console.warn("skip missing 3d model", id + "_model");
      continue;
    }
    fs.writeFileSync(path.join(ITEMS, id + ".json"), JSON.stringify(buildSelect(color)));
    n++;
  }
  console.log("Wired", n, "pokedex item defs → 2D gui / 3D world");
}

main();

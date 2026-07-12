# Pack assets — Cobblemon originals

## Full content import

```bat
node tools/import_porter_all.js
```

Imports **everything** from `C:\Users\conne\Desktop\porter` into `assets/fakemon` + `data/fakemon`:
textures, item/block models, blockstates, sounds, bedrock extras, lang, tags, and generates
`data/fakemon/content/item_ids.txt` + `block_ids.txt` for Java registration
(`ContentItems` / `ContentBlocks`).

Creative tabs: **Fakemons**, **Items**, **Blocks**, **Balls**, **Berries**.

## Sources

| Path | Role |
|------|------|
| `C:\Users\conne\Desktop\porter` | **Primary** Cobblemon 1.7.3 NeoForge extract (spawns, species JSON, tags, items, sounds, bedrock) |
| `C:\Users\conne\Desktop\fakemons\originals` | Older unedited Cobblemon asset tree (optional re-copy) |

## Imported into this mod

| Content | Path under `assets/fakemon/` / Java | Count / notes |
|---------|--------------------------------------|---------------|
| Pokémon textures | `textures/pokemon/**` | 851 folders, 2872 PNGs |
| Geo models | `bedrock/pokemon/models/**` | 1097 `.geo.json` |
| Animations | `bedrock/pokemon/animations/**` | 931 |
| Posers | `bedrock/pokemon/posers/**` | 598 |
| Resolvers | `bedrock/pokemon/resolvers/**` | 1024 |
| **Gen 1 spawn pool** | `species/Gen1SpawnPool.java` | ~335 rules from `porter/11_data_spawning/spawn_pool_world` |
| **Base stats + catch** | `species/CobblemonBaseStats.java` | 151 from `porter/09_data_species/species/generation1` |
| Ball item icons | `textures/item/*cube_ball*.png` | from Cobblemon poke/great/ultra ball |

## Species / entities

- **151 Gen 1** species in `MonSpecies` (full Kanto dex)
- **One entity type** `wild_mon` — species is data on the entity (not 151 EntityTypes)
- Starters: Bulbasaur, Charmander, Squirtle
- Textures + geos: `SpeciesTextures` → original Cobblemon paths (`0004_charmander/charmander.png`, etc.)
- **Wild spawns** use Cobblemon Gen 1 pools + best-spawner bucket weights (94.3 / 5 / 0.5 / 0.2)

## Re-import from porter

```powershell
# Regen spawn pool + base stats (Gen 1 only)
node tools/import_cobblemon_spawns.js

# Optional: re-copy pokemon textures / bedrock from porter
$p = "C:\Users\conne\Desktop\porter"
$mod = "src\main\resources\assets\fakemon"
robocopy "$p\01_textures\pokemon" "$mod\textures\pokemon" /E
robocopy "$p\06_bedrock_models_animations\pokemon" "$mod\bedrock\pokemon" /E
```

Regenerate species Java from Cobblemon species JSON (if present):

```powershell
node tools/gen_gen1_species.js
```

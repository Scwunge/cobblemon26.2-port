# Cobblemon 26.1 Port Notes

## Source material

- Jar extract: Cobblemon NeoForge `1.7.3+1.21.1`
- Layout: `Desktop/porter` (01_textures … 18_compat_data)
- Official repo: https://gitlab.com/cable-mc/cobblemon (Kotlin / Architectury)

## Why not a direct jar load?

Minecraft **26.1** changed mappings, registries, networking, and client GUI extract paths.
The 1.21.1 bytecode will not run. This project:

1. Keeps **100% of shippable assets/datapacks** under `cobblemon:`
2. Reimplements **systems** in Java against NeoForge 26.1
3. Avoids **structure worldgen tags** that reference missing structure defs (world-create hang)

## Critical lessons for official port

1. **Structure tags without structure JSON** → `Unbound values in minecraft:worldgen/structure` → failed registry freeze on world create.
2. **Huge creature spawn during chunk gen** can hang “Loading terrain”; prefer delayed player-centric spawner.
3. **Bedrock geo** is Y-down; bake + LivingEntity scale must match Cobblemon’s `TexturedModel.create` conventions or models float/Naruto-run.
4. Item models use nested texture paths (`item/poke_balls/poke_ball`) — keep folder structure from `01_textures/item/`.
5. MC 26 needs `assets/cobblemon/items/<id>.json` item definitions pointing at models.

## Implemented vs data-only

- **Implemented (playable):** Gen 1 species entities, catch, party/PC, battle, machines (PC/heal/pasture/fossil), berries, held items, medicine, evolution stones, creative tabs, recipes.
- **Data present, not fully simulated:** 1025 species JSON, 2800+ spawn rules (filtered to implemented species), dex entries, behaviours, marks, seasonings, etc.

## Next milestones (for contributors)

1. ~~Dynamic entity species beyond Gen 1~~ — **done (alpha):** `SpeciesHandle` + `speciesId` on `OwnedMon` / entity / spawns / textures.
2. ~~Deeper battle~~ — **partial:** accuracy, dual-type, crits, status, STAB, held items, battle GUI chrome (Fight/Catch/Run).
3. PC wallpapers, pasture AI, fossil machine NBT from datapacks.
4. ~~Structure worldgen gate~~ — **documented** in `StructureBootstrap` (still deferred; do not enable unbound tags).
5. Kotlin multi-loader reintegration with official sources — **started:** see `KOTLIN.md` (mixed Java/Kotlin toolchain + util ports from porter).
6. Full Showdown/move DB parity and 1025 unique entity models without enum fallback.

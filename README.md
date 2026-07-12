# Cobblemon (Community Port) — Project README

**Mod ID:** `cobblemon`  
**Version:** `1.0.0-26.2-beta`  
**Minecraft:** `26.2`  
**Loader:** NeoForge `26.2.0.11-beta`  
**Workspace:** `C:\Users\conne\Desktop\pixelmontest\fakemons`  
**Nature of this project:** A **reimplementation / community port** of Cobblemon-style gameplay on MC 26.2 NeoForge — **not** a line-for-line port of official Cobblemon (Fabric/Quilt). Full official Cobblemon feature parity is **not** claimed.

Use this file to resume work later (with Grok or alone): what works, what is partial, what is missing, and how to build/test.

---

## Quick start

```bash
# From project root
.\gradlew.bat compileJava
# Client run (if NeoForge run config is set up)
.\gradlew.bat runClient
```

**After major worldgen changes:** create a **new world** (feature-order / modifier fixes do not always apply cleanly to existing chunks).

---

## Controls (client)

| Key | Action |
|-----|--------|
| **M** | Starter select (empty party) |
| **P** | Party screen |
| **R** | Send out / recall companion |
| **H** | Flip party HUD side |
| **O** | Toggle party HUD |
| **J** | Pokédex UI |
| **V** | Mount / dismount companion (send out first with R) |

### Useful commands

There is **no** `/cobblemon ...` subcommand tree. Commands are **top-level** names matching official Cobblemon / porter aliases (see `CobblemonCommands.java`). Party slots are **1-based** (1 = lead). Most mutation commands require **permission level 2** (gamemaster); UI openers do not.

| Command | Permission | Purpose |
|---------|------------|---------|
| `/levelup [slot]` · `/levelup <player> <slot>` | op | Level up one party mon |
| `/healpokemon [player]` · `/pokeheal [player]` | op | Heal party |
| `/givepokemon <species> [level]` · `/pokegive …` | op | Give mon to self (default Lv.5) |
| `/givepokemonother <player> <species> [level]` · `/pokegiveother …` | op | Give mon to another player |
| `/spawnpokemon <species> [level]` · `/pokespawn …` | op | Spawn wild mon near you (default Lv.10) |
| `/takepokemon <slot>` · `/takepokemon <player> <slot>` | op | Remove mon from party slot |
| `/clearparty [player]` | op | Clear party |
| `/clearpc [player]` | op | Clear PC boxes |
| `/openstarterscreen` | player | Open starter select UI |
| `/pc` | player | Open PC UI |
| `/pokedex` | player | Print seen/caught counts |
| `/teach <slot> <move>` | op | Teach a move (e.g. `tackle`, `ember_snap`) |
| `/pokemonedit <slot> level <n>` · `/pokeedit …` | op | Set level (or `nickname <name>`) |
| `/pokemoneditother` · `/pokeeditother` | op | Same edits for another player |
| `/helditem <slot> <item>` · `/helditem <slot> clear` | op | Set / clear held item |
| `/stopbattle` | op | Force-end current battle |
| `/pokemonrestart` · `/pokerestart` · `[…]other` | op | Clear party (+ PC unless `partyonly`) |
| `/freezepokemon` | op | Stub (look-target debug; use `/stopbattle` for battles) |

Keys **M / P / R / H / O / J / V** (table above) remain the primary non-command UX.

---

## Architecture snapshot

```
src/main/java/com/cobblemon/mod/
  battle/       # Wild battles, calc, moves, AI, berries
  block/        # PC, machines, crops, apricorn, saplings
  block/entity/ # Fossil, pasture
  client/       # HUD, renderers, screens, keys
  content/      # Mass content registration (items/blocks from datapack lists)
  entity/       # Wild mon, cube ball, spawner
  item/         # Balls, medicine, bait, rods, vitamins, stones
  network/      # Party/PC/battle/ride payloads
  party/        # Party, PC, Pokédex attachments
  species/      # OwnedMon, registry, spawn, evo, held items, IVs/EVs
  worldgen/     # Ores, decoration, mini ruins, trees
```

**Datapack content** lives under `src/main/resources/data/cobblemon/` (species, spawns, recipes, loot, fossils, etc.).  
**Assets** under `src/main/resources/assets/cobblemon/`.

**Worldgen (safe mode):** only **two** NeoForge biome modifiers:

- `neoforge/biome_modifier/01_ores.json` → `cobblemon:cobblemon_ore` (underground_ores)
- `neoforge/biome_modifier/02_vegetation.json` → `cobblemon:overworld_decoration` (vegetal_decoration)

Both **exclude** `minecraft:deep_dark` to avoid feature-order cycle crashes.

**Structures:** full Cobblemon NBT structure pack is **intentionally deferred** (`StructureBootstrap`) — using unbound structure tags freezes world creation. Mini-ruins are a **feature**, not a structure.

---

## What is COMPLETE (usable)

### Core loop
- [x] Starter selection (M)
- [x] Party (6 slots, lead, swap, release, heal action)
- [x] Send out / recall companion (R)
- [x] Party HUD (side flip, toggle)
- [x] Wild spawn near players (custom spawner, multi-dim)
- [x] Throw Poké Balls + in-battle catch
- [x] Wild 1v1 battles (moves, type chart, switch, run, catch)
- [x] Level-up + EXP + learnset wiring (datapack aliases)
- [x] Level evolutions (datapack `evo_index` + Gen1 chains)
- [x] Stone / item evolutions (hardcoded + `item_interact` scan)
- [x] PC deposit / withdraw
- [x] Healing machine (with center vs standalone rules)
- [x] Faint mid-battle → switch; all faint → **blackout** (1 HP + warp to bed/spawn)
- [x] IVs (0–31) + EVs (252/510) in stats
- [x] Pasture deposit/withdraw + slow heal
- [x] Pasture breeding (same species, opposite gender, timed)
- [x] Fossil insert → timer → mon to party (fixed empty-hand collect bug)
- [x] Galar dual-fossil pairs (bird/drake/fish/dino)
- [x] Multi-block fossil detect (analyzer + tank) + organic fill
- [x] Floating fossil timer / Ready popup
- [x] Evolution stone ores (mineable pickaxe + needs_stone_tool tags)
- [x] Apricorn trees + fruit + saplings
- [x] Saccharine trees
- [x] Berry crops / mint / herb patches (via overworld decoration feature)
- [x] Mini ruins (feature) + fossil dig chance
- [x] Pokédex UI (J) — seen/caught list + search
- [x] Riding companion (V) after send-out
- [x] Trainer mon spawn command
- [x] Held items (core combat set) + battle berries auto-use
- [x] Vitamins / EV training via mon picker
- [x] Bait (attract + force spawn)
- [x] Pokerods (tiered cast into water)
- [x] MC 26.2 API renames (screens, animation Y invert, etc. as ported earlier)

### Content volume (datapack present)
- ~1025 species JSON
- ~824 spawn pool files
- ~600+ recipes
- ~1000+ dex entry files
- Fossils, berries, balls, machines registered via content lists

---

## What is PARTIAL (works, but not full Cobblemon)

### Core / battle
- [ ] Wild spawn packing vs full Cobblemon best-spawner (better now; still custom)
- [ ] Full unique move pool (datapack names → ~60 internal `MonMove` via aliases/inference)
- [ ] Abilities (small set + maps; most datapack abilities cosmetic)
- [ ] Weather / terrain / hazards / double battles / PvP
- [ ] Trainer NPCs (command proxy only, not dialogue AI teams)
- [ ] Ball matrix (context mults exist; not every official ball edge case)
- [ ] Forms (mega/gmax/regional) — lite only
- [ ] Friendship / happiness evo paths (Espeon/Umbreon etc. incomplete)

### World
- [ ] Full structures (labs, ships, towers, centers) — **deferred**
- [ ] Dig sites as real structures
- [ ] Tumblestone growth simulation
- [ ] Berry mutation / full mulch system
- [ ] Loot injection reliability (tables present; confirm NeoForge inject hooks)

### Machines / UI
- [ ] Full Resurrection Machine (12 min, data monitor GUI, exact Cobblemon multi-block)
- [ ] Pasture ranch GUI / egg system
- [ ] PC wallpapers fully synced (themes + names exist; not full unlock set)
- [ ] Pokédex regional dexes / full lore entries
- [ ] Battle UI polish / move FX / full cries

### Items
- [ ] Every decorative content item still not wired
- [ ] Mega stones / Z / Tera / Dynamax
- [ ] Full held-item list (expanded core; not complete)
- [ ] Cooking pot / seasonings / mochi / full bait AI pack

### Technical
- [ ] Some chest models fail bake (`gilded_chest` UV bounds — log warnings)
- [ ] Missing particle textures on some ball/pokedex models
- [ ] `@OnlyIn` warnings on client-only classes (harmless noise on NeoForge 26.2)
- [ ] Performance under heavy spawn/model load

---

## What is MISSING (not really implemented)

- [ ] Official Cobblemon structure NBT pack + structure registry
- [ ] Real NPC / dialogue / gym / story systems
- [ ] Player↔player Pokémon trading UI
- [ ] Daycare / eggs / egg moves
- [ ] Full ride_settings vehicle system (only basic mount)
- [ ] Double/multi battles and competitive battle facilities
- [ ] Shiny hunting systems / marks / ribbons gameplay
- [ ] Full Showdown-parity battle engine
- [ ] Fabric official Cobblemon interoperability

---

## Important fixes already applied (don’t re-break)

1. **Fossil collect** — empty hand must run collect in `useItemOn` (MC only calls `useWithoutItem` on `TRY_WITH_EMPTY_HAND`).
2. **Feature order cycle** — never add long feature lists to every overworld biome; use single `overworld_decoration` + `cobblemon_ore`; exclude deep dark.
3. **Structures** — do not enable structure tags without matching structure definitions.
4. **Ores** — `requiresCorrectToolForDrops` needs `minecraft:mineable/pickaxe` + `needs_stone_tool`.
5. **Moves** — normalize datapack move ids via `MoveAliases.resolve` (don’t drop unknown names).
6. **OwnedMon codec** — IVs/EVs nested under `genetics` to stay within 16-field RecordCodecBuilder limit.
7. **X-ray / OreWallHack** — removed by design; ores use normal textures.

---

## Key classes (resume anchors)

| Area | Class / path |
|------|----------------|
| Spawner | `entity/WildMonSpawner.java` |
| Catch math | `item/CatchCalc.java`, `item/BallContext.java` |
| Thrown ball | `entity/CubeBallEntity.java` |
| Battle | `battle/BattleManager.java`, `BattleCalc.java`, `BattleBerry.java` |
| Moves | `battle/MonMove.java`, `MoveAliases.java`, `DatapackLearnsets.java` |
| Party/PC/Dex | `party/PlayerParty.java`, `PlayerPc.java`, `PlayerPokedex.java` |
| Mon instance | `species/OwnedMon.java` (IVs/EVs, evo) |
| Item evo | `species/ItemEvolutionLookup.java` |
| Held items | `species/HeldItems.java` |
| Fossils | `block/entity/FossilMachineBlockEntity.java`, `FossilMachineRenderer.java` |
| Machines | `block/MachineBlock.java` |
| Worldgen | `worldgen/OverworldDecorationFeature.java`, `CobblemonOreFeature.java`, `MiniRuinFeature.java` |
| Pokédex UI | `client/screen/PokedexScreen.java` |
| Ride | `network/RidePayload.java` |
| Content register | `content/ContentItems.java`, `ContentBlocks.java`, `ContentKind.java` |

---

## Suggested “next session” priorities

If continuing from a cold start, pick one track:

1. **Battle fidelity** — real move defs from datapack, more abilities, weather  
2. **Structures** — careful structure registry for 26.2  
3. **Social** — NPC trainers + dialogue  
4. **Breeding** — eggs + egg moves  
5. **Riding** — consume `ride_settings` JSON fully  
6. **Polish** — fix gilded chest models, missing particles, Pokédex lore  

---

## How to talk to Grok when resuming

Paste or reference this README and say e.g.:

- “Continue from README section **Suggested next session** — do battle fidelity”
- “Re-check worldgen feature cycle”
- “Finish PC wallpaper sync to server”

Include any new crash logs under `run/logs/latest.log` or `run/crash-reports/`.

---

## Honest parity estimate

| Area | Rough % vs full Cobblemon |
|------|---------------------------|
| Catch / party / PC basics | ~75% |
| Battle | ~40–50% |
| Species visuals | ~50–60% |
| Worldgen / structures | ~30% |
| Machines | ~45–55% |
| Items depth | ~50% |
| Social / story | ~10% |
| **Overall** | **~45–55% playable Cobblemon-like** |

---

## Changelog notes (this build era)

- Ported toward MC 26.2 NeoForge  
- Gap-fill pass: battle/fossils/dex/ride/breed/IVs/items  
- Core-loop pass: spawn, balls, AI, stone evo, PC boxes, heal center, blackout  
- Items pass: ball context, medicine, battle berries, held items, bait, rods, fossil digs  
- Worldgen crash fix: single decoration feature + deep dark exclusion  
- Removed temporary ore x-ray / wall-hack  

---

*Generated for handoff. Last focused session work: core-loop + items completion passes. Rebuild before playtesting.*
)

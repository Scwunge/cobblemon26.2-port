# Continue here — Cobblemon community port (MC 26.2 NeoForge)

**For the next Grok / developer.**  
**Date snapshot:** 2026-07-12  
**Workspace:** `C:\Users\conne\Desktop\pixelmontest\fakemons`  
**Mod ID:** `cobblemon` · **Version:** `1.0.0-26.2-beta`  
**MC / loader:** Minecraft **26.2** · NeoForge **26.2.0.11-beta** · Java **25**  
**Nature:** Community reimplementation of Cobblemon-style gameplay — **not** official Fabric Cobblemon.

This file is the **authoritative handoff**. A frozen copy of the task board is at:

- `docs/handoff/PARITY_TASKS_snapshot_2026-07-12.md` (copy of live board at handoff time)
- `docs/handoff/README_snapshot_2026-07-12.md`
- Live board remains: **`PARITY_TASKS.md`** (root) — update that as you work

---

## How to build / run

```powershell
cd C:\Users\conne\Desktop\pixelmontest\fakemons
.\gradlew.bat compileJava compileKotlin   # quick compile
.\gradlew.bat runClient                   # singleplayer / integrated
.\gradlew.bat runServer                   # dedicated multiplayer host
.\gradlew.bat build                       # jar → build\libs\cobblemon-1.0.0-26.2-beta.jar
```

**Players do NOT need a separate Kotlin mod.** `kotlin-stdlib` + Graal/JS are **JarJar’d** into the mod jar (`META-INF/jarjar/`).

**Controls (client):**

| Key | Action |
|-----|--------|
| M | Starter select (empty party) |
| P | Party |
| R | Send out / recall (also open player interact when looking at player) |
| V | Mount / dismount companion |
| N | PokéNav wheel |
| J | Pokédex |
| H / O | Party HUD flip / toggle |

**Useful op commands:** `/spawntrainer "Name" [badge]`, `/doubleduel`, `/badges`, `/givebadge`, `/spawnpokemon`, `/givepokemon`, `/pc`, `/trade`, `/giveegg`, `/makeshiny`, `/stopbattle`  
See `CobblemonCommands.java` and README.

---

## What is COMPLETE (playable systems)

### Core loop
- Party / PC (attachments, sync, screens)
- Wild spawn, throw balls, catch, send-out companion
- Wild 1v1 battles: native calc + Showdown damage bridge + optional sim path
- Blackout = soft-heal in place (no bed teleport) — **do not reintroduce bed warp**
- Battle camera, music, hit FX, type-based move particles/SFX (`BattlePresentation.playMoveEffects`)

### Battle (v0–v1)
| ID | Status | Notes / key files |
|----|--------|-------------------|
| B1 Showdown sim | DONE v1 | Outbox bridge, `runAuthoritativeTurn`, native fallback. Still not 100% sim-authoritative every fight. `battle/graal/*`, `ShowdownSessionBridge` |
| B2 Weather/terrain/hazards | DONE | `BattleFieldEffects` |
| B3 Doubles | DONE v0 | `BattleSession.Format.DOUBLES`, `/doubleduel` |
| B4 PvP | DONE v0 | R→Battle challenge, `PvpChallengeManager`, clone lead fight + HP sync |

### Social
| ID | Status | Notes |
|----|--------|-------|
| N1 Trainers | DONE v0 | `TrainerNpcEntity`, `/spawntrainer` |
| N2 Dialogue | DONE v0 | JSON dialogues, `DialogueManager` |
| N3 Gyms/badges | DONE v1 | `PlayerBadges`, soft evo gates Lv.36/50 (`BadgeGates`), badge on gym win |
| T1 Trade UI | DONE | R-player request/accept GUI |
| T2 Trade evo | DONE | Trade finish + `LinkCableItem` → `tryTradeEvolve` |

### Eggs / breeding
| ID | Status | Notes |
|----|--------|-------|
| E1–E4 | DONE v0 | Egg item, pasture eggs, `EggMoveInheritance`, `PastureScreen` ranch GUI |

### Riding (recently heavily reworked)
| ID | Status | Notes |
|----|--------|-------|
| R1 Settings | DONE v0 | `RideSettingsLoader` |
| R2 Flight | DONE v0→tuning | **Hold Space to fly**, release = fall. Seat offset on mon. Prefer full `air_fly` anim not sparse `ride_air_fly`. Shift-dismount blocked (`RideEvents`). |
| R3 Water | DONE v0→boat | **Boat-style surface** + **hold Shift to dive**, release = float up. Not free-swim rocket. |
| R5 Stats | DONE v0 | Spe/size scale speed/jump |

**Ride code:** `ride/RideController.java`, `RideEvents.java`, `WildMonEntity` passenger seat + travel, `RidePayload`.

### Worldgen (safe — no structure tags)
| ID | Status | Notes |
|----|--------|-------|
| S1 Safe mode | DONE | No unbound structure tags |
| S2 Centers/towers | DONE v0 | `LandmarkFeature` via `OverworldDecorationFeature` |
| S4 Dig sites | DONE v0 | Dig landmark + chests |
| Loot | DONE v0 | `CobblemonLoot` wires **official datapack tables** (`ruins/…`, `villages/village_pokecenters`, `fossils/…`) onto feature chests. **New chunks only.** |

### Collectibles / UI sides
| ID | Status | Notes |
|----|--------|-------|
| C1 Shiny | DONE | 1/4096, form, render |
| C2 Marks | DONE v0 | Catch rolls, genetics field, summary UI |
| C3–C4 FX | DONE v0 | Type particles + SFX |
| K1 Cooking | DONE v0 | `campfire_pot*` + berry → heal lead |
| P1 PokéNav | DONE v0 | Key **N** |

### Tech
- Mixed Java + Kotlin (`src/main/java`, `src/main/kotlin`)
- Datapack: species_index (+ eggMoves), spawn_index, full species JSON under `data/cobblemon/species/`
- Content items/blocks from porter import
- Graal Showdown in `config/cobblemon/showdown` (extracted at runtime)

---

## What is NOT done / partial / risky

### Explicit TODO (parity board)
| ID | Task | Priority for next agent |
|----|------|-------------------------|
| **B5** | Mega / Z / Tera / Dynamax | High if battle depth; large design |
| **R4** | Multi-seat / passenger | Medium |
| **S3** | Ships / large NBT structures | High for exploration — **must stay feature-safe or full structure port** |
| **K2** | Seasonings / mochi full recipes | Low–medium |
| **P2** | Shoulder mounts | Low |
| **M1** | Mongo storage | DEFER |
| **F1** | Fabric multi-loader | DEFER |

### Known partial / polish debt (playable but rough)
1. **Showdown B1** — Often falls back to native; sim authority not guaranteed every turn. Dex-fallback vs full `startBattle` differs by boot.
2. **Doubles B3** — No full targeting UI; partner AI is native auto.
3. **PvP B4** — One-sided clone fight; not simultaneous dual clients.
4. **Ride seat** — Hitbox-based seat, not per-species Bedrock seat bones; some models still look offset.
5. **Ride flight anim** — Prefer `air_fly` / `air_idle`; verify Charizard no longer T-poses.
6. **Water dive** — Tuned boat + Shift dive; re-test Lapras/Gyarados in ocean.
7. **Structures** — Feature stubs only (mini ruins, center pad, tower, dig). Official NBT structure pack **not** registered (by design — unbound tags freeze worlds).
8. **Loot** — Feature chests use Cobblemon tables; old worlds keep old chests until new gen.
9. **ClientHooks** in common S2C payloads — works in practice; dedicated server should be smoke-tested.
10. **Marks** — Catch rolls only; full ribbon set / UI icons partial.
11. **Cooking** — Berry→heal only; no full seasoning/mochi pipeline.
12. **Gym story** — Badges + soft gates; no full badge case UI or gym building progression.

### Do not break
- Blackout soft-heal in place (`BattleManager.handleBlackout`)
- Single overworld decoration feature (no feature-order cycle)
- No enabling Cobblemon structure **tags** without full structure defs
- OwnedMon genetics nesting (16-field codec limit); marks live under genetics
- JarJar kotlin + graal for production jar

---

## Package map (where to edit)

```
src/main/java/com/cobblemon/mod/
  Cobblemon.java              # bootstrap, event bus
  battle/                     # BattleManager, Session, Presentation, Pvp, field effects
  battle/graal/               # Kotlin ShowdownSim, TurnProtocol, Interpreter, TeamPacker
  ride/                       # RideController, RideEvents, RideSettingsLoader
  entity/                     # WildMonEntity (ride seat, flight flag), TrainerNpc, balls
  party/                      # Party, PC, Badges, BadgeGates, Pokedex
  trade/                      # TradeManager
  network/                    # All payloads + ModNetwork
  client/                     # CobblemonClient, screens, geo models, camera, music
  worldgen/                   # Features, CobblemonLoot, StructureBootstrap
  species/                    # OwnedMon, SpeciesRegistry, marks, evolutions
  block/                      # PC, machines, pasture, crops
  command/                    # CobblemonCommands

src/main/resources/data/cobblemon/
  content/species_index.json  # runtime stats + levelMoves + eggMoves
  loot_table/ruins|fossils|villages|shipwreck_coves|…
  species/generation*/        # full species JSON (moves egg:… etc.)
  ride_settings/, marks/, dialogues/, …

src/main/resources/assets/cobblemon/bedrock/pokemon/
  animations/…/*.animation.json   # air_fly, ride_*, ground_*
```

---

## Recommended next work (priority)

1. **Smoke-test ride** — Charizard hold-Space fly + anim; Lapras Shift dive + no dismount; seat height.
2. **B5 gimmicks** or **S3 ships** (safe feature ships using existing shipwreck_cove loot) — pick one epic.
3. **B1 harden** — prove sim path in runClient logs; reduce silent native fallback.
4. **Dedicated server pass** — `runServer` + jar on NeoForge dedicated; fix classload if ClientHooks blows up.
5. **K2 seasonings** / **P2 shoulder** / **R4 multi-seat** as polish.

---

## Cleanup notes (this handoff)

- Live docs **left in place**: `PARITY_TASKS.md`, `README.md`, `BATTLE.md`, etc.
- **Copied** snapshots into `docs/handoff/` (this file + board/README copies).
- Root/tools **log clutter** moved or deleted (compile/run dump text files). Source, datapacks, and `run/` worlds left alone.
- Do **not** commit `run/` or huge `build/` unless intentional.

---

## Session recap (what the previous Grok did)

- Parity waves through B4, E4, R2–R5, N3, C2–C4, K1, P1, structure landmarks + **Cobblemon loot tables**
- Ride rework: player seat, hold-Space flight, boat water + Shift dive, block Shift-dismount
- Multiplayer-capable systems (trade, PvP challenge, pasture net) at v0
- Jar self-contained (Kotlin + Graal jarJar)

**When you finish a task:** update root `PARITY_TASKS.md` and add a short bullet under “Session recap” here or a new dated file under `docs/handoff/`.

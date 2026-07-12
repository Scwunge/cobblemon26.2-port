# Full Cobblemon Parity — Task Board

**Project:** Community MC 26.2 NeoForge port (`fakemons`)  
**Goal:** Bring every system below into the game (playable systems, not data-only).  
**Last updated:** 2026-07-12  
**Handoff for next agent:** see **`docs/handoff/CONTINUE_NEXT_SESSION.md`** (detailed complete/incomplete + code map). Snapshot copies of this board live under `docs/handoff/`.

Legend: `TODO` · `IN_PROGRESS` · `DONE` · `BLOCKED` · `DEFERRED`

---

## Wave 0 — Foundations (must stay green)

| ID | Task | Status | Notes |
|----|------|--------|-------|
| W0-1 | Mixed Java/Kotlin + NeoForge 26.2 build | DONE | |
| W0-2 | Party / PC / catch / wild 1v1 core loop | DONE | |
| W0-3 | Showdown move dex + optional JS damage | DONE | Not full sim |
| W0-4 | Blackout soft-heal in place (no teleport) | DONE | Canonical |
| W0-5 | Battle camera / music / hit FX | DONE | |

---

## Wave 1 — Battle depth (native + Showdown path)

| ID | Task | Status | Depends | Owner path |
|----|------|--------|---------|------------|
| **B1** | Full Showdown battle engine (authoritative sim every turn) | DONE (v1) | W0-3 | outbox + dual-format choices + drain on start; HP-change detection |
| B1.1 | Pack teams → Showdown request format | DONE | B1 | |
| B1.2 | Interpret sim lines → battle log + HP/status | DONE | B1.1 | `ShowdownSessionBridge` applies HP/status/faint/win |
| B1.3 | Choice protocol (move/switch) ↔ sim | DONE (v0) | B1.2 | p1+p2 move choices; switch helper present |
| B1.4 | Fall back to native if Graal fails | DONE | | `BattleEngine` modes |
| **B2** | Weather / terrain / hazards in battle | DONE | W0-2 | `BattleFieldEffects` wired into calc + residuals + switch-in |
| B2.1 | Weather: sun/rain/sand/snow + residual | DONE | B2 | |
| B2.2 | Terrain: electric/grassy/psychic/misty | DONE | B2 | |
| B2.3 | Hazards: SR / spikes / tspikes / sticky web | DONE | B2 | |
| **B3** | Double / multi battles | DONE (v0) | B1 or native multi | `Format.DOUBLES` + `/doubleduel`; partner + wild2 native acts |
| **B4** | PvP + challenge requests | DONE (v0) | B3 + net | R→Battle challenge; accept/decline; clone-lead fight + HP sync |
| **B5** | Mega / Z / Tera / Dynamax | TODO | B1 preferred | gimmick flags + items |

---

## Wave 2 — Social / world characters

| ID | Task | Status | Depends | Owner path |
|----|------|--------|---------|------------|
| **N1** | Trainer NPC entity + team data | DONE (v0) | W0-2 | `TrainerNpcEntity` + `/spawntrainer`; wild-style lead battle |
| **N2** | Dialogue system (JSON dialogues) | DONE (v0) | N1 | reuse `data/.../dialogues` |
| **N3** | Gym / story progression hooks | DONE (v1) | N2 | badges + soft evo gates (Lv.36/50) + wild level cap hints |
| **T1** | Player trade UI + session | DONE | party net | R on player → request → accept → GUI; Link Cable for trade evos |
| **T2** | Trade evolution triggers | DONE | T1 + evo | `tryTradeEvolve` on trade finish + Link Cable use |

---

## Wave 3 — Breeding / eggs

| ID | Task | Status | Depends | Owner path |
|----|------|--------|---------|------------|
| **E1** | Egg item + inventory hatch timer | DONE | pasture | `item/PokemonEggItem` |
| **E2** | Pasture → produce egg (not instant mon) | DONE | E1 | `PastureBlockEntity` |
| **E3** | Egg moves from parents | DONE | E1 | `EggMoveInheritance` + species_index eggMoves; breed writes moves into egg |
| **E4** | Daycare machine / ranch GUI | DONE (v0) | E2 | `PastureScreen` + Open/Action payloads; breed button; sneak-deposit still works |

---

## Wave 4 — Riding

| ID | Task | Status | Depends | Owner path |
|----|------|--------|---------|------------|
| **R1** | Consume `ride_settings` JSON | DONE (v0) | basic ride | `RideSettingsLoader` + `RideController` |
| **R2** | Flight mounts | DONE (v0) | R1 | Hold Space to fly; release fall; air_fly anim; Shift-dismount blocked |
| **R3** | Water / surface mounts | DONE (v0) | R1 | Boat surface + hold Shift dive; Cobblemon-like not free-swim rocket |
| **R4** | Multi-seat / passenger | TODO | R1 | |
| **R5** | Ride stats (speed/accel from species) | DONE (v0) | R1 | species base Speed scales mount `movementSpeed` |

---

## Wave 5 — Structures / exploration

| ID | Task | Status | Depends | Owner path |
|----|------|--------|---------|------------|
| **S1** | Safe structure registry (no unbound tags) | DONE | worldgen research | feature-only landmarks; no structure tags |
| **S2** | Labs / centers / towers (subset first) | DONE (v0) | S1 | `LandmarkFeature` + `CobblemonLoot` (pokecenter/ruins tables) |
| **S3** | Ships / large structures | TODO | S1 | |
| **S4** | Dig sites as structures | DONE (v0) | S1 | dig_site + fossil loot tables |

---

## Wave 6 — Collectibles & presentation

| ID | Task | Status | Depends | Owner path |
|----|------|--------|---------|------------|
| **C1** | Shiny roll + shiny form on entity/UI | DONE | spawn/catch | `OwnedMon`, spawner, renderer (1/4096) |
| **C2** | Marks / ribbons gameplay + UI | DONE (v0) | C1 | catch-roll marks; summary + genetics persist |
| **C3** | Snowstorm-style battle particles / action effects | DONE (v0) | battle | type-based particles on hit |
| **C4** | Move-specific SFX wiring | DONE (v0) | C3 | type-based SFX in `playMoveEffects` |

---

## Wave 7 — Side systems

| ID | Task | Status | Depends | Owner path |
|----|------|--------|---------|------------|
| **K1** | Cooking pot block + GUI | DONE (v0) | content | campfire_pot* cook berry → heal lead |
| **K2** | Seasonings / mochi recipes | TODO | K1 | |
| **P1** | PokeNav / interaction wheel UI | DONE (v0) | client | key N → Party/Dex/Send/Ride/Badges |
| **P2** | Shoulder mounts | TODO | entity | |
| **M1** | Advanced storage backends (optional Mongo) | DEFERRED | large servers | NBT attachments OK for SP |
| **F1** | Fabric official interoperability | DEFERRED | multi-loader | Architectury-scale rewrite |

---

## Dependency graph (high level)

```
W0 core
 ├─► B2 weather/hazards ──► B5 gimmicks
 ├─► B1 Showdown full ──► B3 multi ──► B4 PvP
 ├─► E1 eggs ──► E2 pasture eggs ──► E3 egg moves ──► E4 daycare UI
 ├─► R1 ride_settings ──► R2–R5
 ├─► N1 NPC ──► N2 dialogue ──► N3 gyms
 ├─► T1 trade ──► T2 trade evo
 ├─► C1 shiny ──► C2 marks
 ├─► S1 structures ──► S2–S4
 └─► K1 cooking · P1 PokeNav · C3 particles
```

---

## Session workstreams (agents)

| Stream | Focus | Parallel-safe? |
|--------|-------|----------------|
| A | Battle field effects (weather/terrain/hazards) | Yes |
| B | Eggs + pasture produce egg | Yes |
| C | Shiny system end-to-end | Yes |
| D | Showdown interpreter scaffold | Yes (battle package) |
| E | Structures research + safe registry plan | Yes (read-heavy) |
| F | Ride settings loader | Yes |
| G | NPC trainer + dialogue scaffold | Yes |
| H | Trade UI scaffold | Yes |

---

## Definition of Done (per epic)

- Playable in `runClient` without crash
- Server + client packeted where needed
- Logged in this file as `DONE`
- README parity table updated

---

## Not “data present” — systems required

Datapack already has many of these assets. **Done** means gameplay code uses them.

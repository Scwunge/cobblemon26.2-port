# Battles — port plan

## What we have now (native engine)

Server-side wild battles in Java/Kotlin:

- `BattleManager` / `BattleSession` — turns, switch, run, catch
- `BattleCalc` — damage with **full −6…+6 stages**, STAB, type chart, crit, multi-hit, accuracy/evasion
- **Showdown secondaries** — status chance, target/self boosts, drain heals
- **Residuals both sides** — burn/poison on player *and* wild mon end of turn
- Status (burn/poison/para/sleep/freeze), held items, berries
- **Blackout** — soft-heal party to **1 HP in place** (no bed/spawn teleport; see README)
- Learnsets from **species datapack** (`"12:razorleaf"`)
- Graal JS **spike** (engine only — not full Showdown battles yet)

## What official Cobblemon uses

| Piece | Role |
|-------|------|
| **Showdown** (`data/cobblemon/showdown.zip`) | Full Pokémon battle simulator (JS) |
| **GraalVM JS + Truffle + ICU** | Runs Showdown in-process |
| **KotlinForForge** | Kotlin mod loader |
| Packet bridge | `ShowdownService` ↔ client UI |

Graal is **pinned to Java 21** upstream (“often graal crashes on newer versions”). We run **Java 25 / MC 26.2**, so full Graal integration is a separate research spike.

## What we ported first (this work)

1. **Shipped** `data/cobblemon/showdown.zip` + `showdown.json` from porter
2. **`ShowdownMoveDex`** (Kotlin) — parses `data/moves.js` at bootstrap (~all official moves)
3. **`BattleMove`** — resolves id → Showdown stats, else `MonMove` / aliases
4. **Owned mon move ids** stay official (`vinewhip`, `ember`, …) instead of only our stand-ins
5. Battle UI / damage use **real power, type, accuracy, PP, priority**

So battles **feel** much closer to mainline without running the JS sim yet.

## Done this pass

1. ✅ **Wild residual status** — burn/poison ticks wild HP; can faint to residual
2. ✅ **Showdown secondary / drain / boosts** — parsed in `ShowdownMoveDex`, applied in battle
3. ✅ **Full stat stages** — `BattleStages` (−6…+6) for ATK/DEF/SPA/SPD/SPE/ACC/EVA both sides
4. ✅ **Graal spike** — Polyglot **25.1.3** on Java 25 works (see below)

## Done — Showdown runner v0 + combat bridge v1

1. ✅ **`ShowdownRunner`** — Graal `Context`, moves.js probe, JS power map
2. ✅ **`ShowdownCombatBridge`** — live **JS damage formula** for damaging hits; extracts zip → `config/cobblemon/showdown/`
3. ✅ **`BattleCalc`** tries JS damage first when bridge ready, else native
4. ✅ **`BattleEngine`** modes: `NATIVE` / `SHOWDOWN_PROBE` / `SHOWDOWN_COMBAT`
5. ✅ jarJar polyglot + js-language + truffle + icu4j

### Showdown sim + turn protocol (continued)

| Class | Role |
|-------|------|
| **`ShowdownSim`** | Extract zip → `config/cobblemon/showdown`, Graal Context with `js.commonjs-require`, eval root `index.js` (official Cobblemon-style). Falls back to `require('./sim/dex')` if index fails. |
| **`ShowdownTurnProtocol`** | Enrich moves from live Dex; `resolveDamage` → combat bridge; optional `tryStartWildProtocol` on wild engage |
| **`BattleManager.tryStart`** | Best-effort protocol start (ignored on failure) |

**Still partial:** full packed teams + interpreting sim output into our log (needs ShowdownInterpreter port). Browserify shims (`buffer/`, `path-browserify`) may be required for full `index.js` on some setups.

### Showdown runner v0 API

| API | Role |
|-----|------|
| `ShowdownRunner.bootstrap()` | Open JS `Context` (if Graal works), probe zip, inject registry — **never throws** |
| `ShowdownRunner.isAvailable()` | Whether JS probe context is live |
| `ShowdownRunner.getMovePower(id)` | Prefer `ShowdownMoveDex`, then JS `__cobbleGetPower` |
| `ShowdownRunner.shutdown()` | Close context |
| `BattleEngine.preferredMode()` | `SHOWDOWN_PROBE` if runner available, else `NATIVE` |
| `BattleEngine.describe()` | One-line log string |

Combat still uses **native** `BattleManager` / `BattleCalc`. Runner is a bridge hook for future full Showdown sim.

## Next steps (priority)

1. Full Showdown sim bootstrap (eval sim modules from zip, packet bridge)
2. jarJar remaining Graal transitves if production clients miss classes (`truffle-api`, `regex`, …)
3. Double battles / trainer battles

## How to verify

On game start, log should include:

```
showdown moves: <large number, e.g. 900+>
```

and Kotlin bootstrap:

```
vinewhip=45pwr/GRASS
```

and Graal spike:

```
GraalSpike: SUCCESS — js ok: 1+1=2; ...
```

and Showdown runner + engine preference:

```
ShowdownRunner: bootstrap SUCCESS — ok; basePowerCountInSnippet=...; js.vinewhip=45; native.vinewhip=45
BattleEngine mode=SHOWDOWN_PROBE · ShowdownRunner=available (...)
```

In battle, Bulbasaur should show **Vine Whip** at **45 PWR**, not a stand-in name.

## Graal spike (Java 25)

### Research

| Item | Finding |
|------|---------|
| Official Cobblemon | Graal **22.3.0** + **Java 21** lock (“graal crashes on newer versions”) |
| Maven Central (polyglot/js) | **25.0.0–25.0.3** (LTS line) and **25.1.3** (latest / Innovation) |
| Host JDK | This project: **OpenJDK 25.0.3** + NeoForge **26.2** |
| Docs | [Embedding Languages](https://www.graalvm.org/latest/reference-manual/embed-languages/) — OpenJDK/Oracle JDK support **fallback (interpreter-only)** out of the box; optimizing JIT needs GraalVM JDK or `--upgrade-module-path` + JVMCI |
| Coordinates | `org.graalvm.polyglot:polyglot` + `org.graalvm.polyglot:js` (**packaging=pom**) → `org.graalvm.js:js-language` + `org.graalvm.truffle:truffle-runtime` |

### Versions tried

| Version | Role | Result |
|---------|------|--------|
| **25.1.3** (chosen) | polyglot + js@pom + js-language + truffle-runtime | **Compile OK**; standalone OpenJDK 25 smoke: **`1+1=2`, langs=[js]** |
| 25.0.2 / 25.0.3 | Documented LTS line on graalvm.org | Not dual-built; same coordinate layout as 25.1.x — fine fallback if 25.1.x misbehaves |
| 22.3.0 | Official Cobblemon pin | **Not used** — targets older Graal/Java 21 era; wrong line for JDK 25 |
| Plain `implementation "…:js:25.1.3"` without `@pom` | Gradle default | **Broken** — resolves as empty library jar; **no** js-language/truffle on classpath |

### Build wiring (`build.gradle`)

- Flag: `ext.graalSpikeEnabled = true` (set `false` to drop deps if resolution ever breaks NeoForge)
- Kotlin plugin / stdlib pin: **2.2.20**
- `implementation org.graalvm.polyglot:polyglot:25.1.3`
- `implementation org.graalvm.polyglot:js:25.1.3@pom`  ← **required** (Maven `<type>pom</type>`)
- `runtimeOnly` `js-language` + `truffle-runtime` (explicit; also pulled via POM)
- **`jarJar` for production clients**: `polyglot`, `js-language`, `truffle-runtime`, `org.graalvm.shadowed:icu4j` (version range `[25.0,26.0)`, prefer `25.1.3`)
- Run JVM: `-Dpolyglot.engine.WarnInterpreterOnly=false`, `--enable-native-access=ALL-UNNAMED`

### Runtime approach (spike + runner v0)

- Class: `com.cobblemon.mod.battle.graal.GraalSpike` — smoke: `1+1`, zip size probe
- Class: `com.cobblemon.mod.battle.graal.ShowdownRunner` — durable `Context`, moves snippet + JS registry
- Class: `com.cobblemon.mod.battle.BattleEngine` — `NATIVE` / `SHOWDOWN_PROBE` preference
- Called from `Cobblemon.commonSetup` **after** `ShowdownMoveDex.bootstrap` in order: spike → runner → engine log
- Catches **Throwable** — never crashes mod bootstrap
- Logs `GraalSpike: SUCCESS — …`, `ShowdownRunner: bootstrap SUCCESS — …`, `BattleEngine mode=…`

Standalone smoke (outside Minecraft, same jars as Gradle cache):

```
SUCCESS 1+1=2 langs=[js]
```

Warnings observed (non-fatal on OpenJDK 25): Truffle `System::load` restricted-method notice; `sun.misc.Unsafe::objectFieldOffset` deprecation.

### Recommendation

1. **Full Showdown is viable on Java 25** from a polyglot/JS engine standpoint — **25.1.3 (or 25.0.x LTS) works on OpenJDK 25** in interpreter mode. The old “pin Java 21 + Graal 22.3” constraint is **not** required for a trivial embed.
2. **Performance caveat**: without GraalVM JDK / JVMCI compiler upgrade, Showdown will run **interpreter-only** (slower warm-up and throughput). Fine for a spike and likely OK for single wild battles; measure before doubles / multiplayer.
3. **Packaging**: production mod jar **jarJar**s `js-language` + `truffle-runtime` + shadowed ICU (+ polyglot). Further transitves may still need packaging if clients hit `ClassNotFoundException`.
4. **Next integration step**: load full Showdown sim modules into the runner `Context`, bridge moves/species packets — keep native `BattleManager` as fallback when `ShowdownRunner.isAvailable()` is false / mode `NATIVE`.
5. If anything destabilizes NeoForge, set `graalSpikeEnabled = false` and rebuild; spike/runner remain safe (ClassNotFound → FAILURE log only if deps removed but classes still present — prefer disabling the call when flag is off).

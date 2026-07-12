# Kotlin in this port

Official Cobblemon is almost entirely Kotlin (Architectury multi-loader).
This NeoForge **26.2** reimplementation is **mixed Java + Kotlin**.

## Build

| Setting | Value |
|--------|--------|
| Plugin | `org.jetbrains.kotlin.jvm` **2.2.20** |
| Java toolchain | **25** (NeoForge / MC 26.2) |
| Kotlin `jvmTarget` | **24** (runs on Java 25) |
| Validation | `kotlin.jvm.target.validation.mode=WARNING` |
| Stdlib | jarJar `kotlin-stdlib:2.2.20` |
| Graal (optional) | `graalSpikeEnabled` → polyglot + js-language + truffle + icu jarJar |

Sources: `src/main/java/...` + `src/main/kotlin/...`

## Kotlin modules (current)

| Module | Role |
|--------|------|
| `kotlin.CobblemonKotlin` | Bootstrap probe |
| `species.StarterCatalog` | Regional starters; loads `data/cobblemon/starter_config.json` |
| `util.*` | Lang, CobblemonResource, BitUtils, Collections, DataKeys |
| `battle.ShowdownMoveDex` / `BattleMove` / `BattleStages` | Native battle stats + stages |
| `battle.graal.GraalSpike` | Polyglot JS smoke test |
| `battle.graal.ShowdownRunner` | Durable JS context + move power probe |
| `battle.BattleEngine` | NATIVE vs SHOWDOWN_PROBE preference (combat still native) |
| `pokedex.PokedexProgress` | NONE / SEEN / CAUGHT enum |
| `molang.MoLangBootstrap` / `MoLangEngine` / `MoLangScript` | MoLang resource scan + tiny expression scaffold |

## DataKeys + saves

`OwnedMon` / PC codecs write official-style keys (`Species`, `Level`, `Health`, …) via `DataKeys`.
Legacy lowercase keys still **read** (dual codec / optional dual fields). Next save rewrites modern names.

## Starter config

Edit `src/main/resources/data/cobblemon/starter_config.json` to change regions/species without recompiling.
`StarterCatalog.bootstrap()` runs at common setup.

## Battle engine policy (v1)

1. **`BattleManager`** — turns, switch, catch, log (always)
2. **`ShowdownCombatBridge`** — when ready, **damaging hits** use Graal JS Showdown-style formula
3. **Native `BattleCalc`** — accuracy, stages, status, drain; **fallback** if JS fails
4. **Full BattleStream sim** — not yet

## In-game log checklist

```
StarterCatalog: 10 categories ...
ShowdownMoveDex: loaded N moves ...
GraalSpike: SUCCESS — js ok: 1+1=2 ...
ShowdownRunner: bootstrap SUCCESS — ... js.vinewhip=45 ...
ShowdownCombatBridge: READY — smokeDamage=...
ShowdownSim: READY — ok; startBattle=...   (or DEX FALLBACK / FAILURE)
MoLangBootstrap: files=N ...
BattleEngine mode=SHOWDOWN_SIM|SHOWDOWN_COMBAT · damage=JS(...) · sim(...) · runner=ok
```

If Graal fails: mode stays `NATIVE`; game still playable.
Force native damage: `ShowdownCombatBridge.enabled = false`.

## MoLang scaffold (not full bedrockk)

Cobblemon ships many `data/cobblemon/molang/**/*.molang` scripts (AI tasks, NPC callbacks, client gestures).
Official / multi-loader builds use a full MoLang runtime (often via **bedrockk** / Bedrock-compatible evaluator).

This port has a **small pure-Kotlin scaffold** only:

| Piece | Role |
|-------|------|
| `MoLangBootstrap` | Scan classpath under `/data/cobblemon/molang/` (nested), count `.molang` files, load known scripts, log once. Never throws. |
| `MoLangEngine` | Minimal expression eval: numbers, `+ - * /`, `()`, comparisons, `&&` `||` `!`, `math.random` / `math.mod` / a few math helpers, variables from a `Map` (`q.grounded`, `variable.x`, …) |
| `MoLangScript` | `path` + `source` (+ optional `lastEval` if a line was simple enough) |

**Not implemented:** assignments (`t.x = …`), blocks `{ }`, entity/query APIs (`q.entity.walk_to`), string query args as real side effects, animation/poser graph. Real scripts are **stored** on load; eval is best-effort for the first simple line or pure expressions.

Wire-up: `MoLangBootstrap.bootstrap()` from `Cobblemon.commonSetup` after `ShowdownRunner`.

When a full runtime is needed later: prefer integrating bedrockk (or upstream Cobblemon MoLang) rather than growing this scaffold into a second language implementation.

## Next (future)

1. CommonJS load of `sim/battle.js` + turn protocol (true Showdown sim)
2. Full MoLang / Bedrock poser runtime (replace scaffold when ready)
3. KotlinForForge only if switching to a Kotlin `@Mod` entrypoint

## Calling Kotlin from Java

```java
StarterCatalog.bootstrap();
List<StarterCatalog.Category> cats = StarterCatalog.categories();
PokedexProgress p = dex.progress("bulbasaur");
BattleEngine.Mode mode = BattleEngine.preferredMode();
int power = BattleEngine.resolveMovePower("vinewhip");
MoLangBootstrap.bootstrap();
Double v = MoLangEngine.eval("1 + 2 * 3", java.util.Map.of("q.grounded", 1.0));
```

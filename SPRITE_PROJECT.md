# Fakemon Sprite Project — 50 (+ extras) Checklist

Tied to the **Fakemons** NeoForge mod (`MonSpecies`).  
Code already owns names, types, lore blurbs, abilities, stage/evo links, and base HP/Power.  
**This doc tracks 2D sprite art + final dex polish.**

---

## Planning

| Item | Status | Decision (locked for Fakemons) |
|------|--------|--------------------------------|
| Overall region theme | ✅ | **Overworld / frontier wilderness** — camps, rivers, peaks, ruins; plus nether-adjacent heat for Fire. Cosmic **Grok** line is peak/night myth. |
| Art style | ⬜ | **Recommend: Gen 5 style** (clear silhouette, soft shading, ~96×96 or 80×80 battle) with Gen 4 outline crispness. Alternative: Gen 3 chunky if you want faster batch art. |
| Naming convention | ✅ | **Single English portmanteau**, lowercase id: `flameling`, `abysshell`, `grokmon`. No real Pokémon names. |
| Type distribution | ✅ | All 18 types represented. Starters: Fire / Water / Grass / Electric / Flying. |
| Evolution lines | ✅ | Max **2 evolutions (3 stages)**. Most lines are 2–3 stage level evo; gem lines (Cinderwhelp, Dewkit) are 2-stage item evo. |
| Starters reserved | ✅ | Flameling, Ripplin, Sprouton, Voltkit, Zephyrpuff (and mids/finals). |
| Legendaries / mythicals | ✅ | **Grokling → Grokmon → Groknova** (legendary, not starter). Subshah = showcase rare. Drakonox = pseudo-feel ultra-rare. |

**Sprite count note:** Roster is **54** species (50 “core” + Grok line + Subshah). Treat milestones as **50 priority sprites**; Grok + Subshah are marquee (do early or as milestone bosses).

---

## Type distribution (current)

| Type | Count | Examples |
|------|------:|----------|
| Fire | 5 | Flameling line, Cinderwhelp line |
| Water | 6 | Ripplin line, Dewkit line, Subshah |
| Grass | 3 | Sprouton line |
| Electric | 3 | Voltkit line |
| Flying | 3 | Zephyrpuff line |
| Rock | 3 | Rockpup line |
| Ground | 3 | Terracub line |
| Dark | 3 | Nightflit line |
| Dragon | 3 | Drakeling line |
| Psychic | 6 | Mindwisp line + Grok line |
| Fighting | 3 | Fisthare line |
| Ice | 3 | Glaceling line |
| Steel | 2 | Ironbeetle line |
| Bug | 2 | Bloomite line |
| Poison | 2 | Venoot line |
| Ghost | 2 | Phasling line |
| Fairy | 1 | Gleamfae |
| Normal | 1 | Plainpup |

---

## Evolution map (for family art consistency)

| Line | Stage 1 → 2 → 3 | Method |
|------|-----------------|--------|
| Fire starter | Flameling → Flarepaw → Infernox | Level |
| Water starter | Ripplin → Torrentide → Abysshell | Level |
| Grass starter | Sprouton → Leafclaw → Verdantitan | Level |
| Electric starter | Voltkit → Staticlaw → Thundermaw | Level |
| Flying starter | Zephyrpuff → Galewing → Skyrend | Level |
| Fire gem | Cinderwhelp → Magmawyr | Fire Gem |
| Water gem | Dewkit → Oceanarch | Water Gem |
| Rock | Rockpup → Boulderback → Titanpeak | Level |
| Ground | Terracub → Terrahound → Quakejaw | Level |
| Dark | Nightflit → Shadeclaw → Umbrathorn | Level |
| Dragon | Drakeling → Wyrmling → Drakonox | Level |
| Psychic | Mindwisp → Psycheye → Cosmosage | Level |
| Fighting | Fisthare → Brawlbear → Ironfist | Level |
| Ice | Glaceling → Frostfang → Glaciarch | Level |
| Steel | Ironbeetle → Gearshell | Level |
| Bug | Bloomite → Silkwing | Level |
| Poison | Venoot → Toxipaw | Level |
| Ghost | Phasling → Wraithowl | Level |
| Fairy | Gleamfae | — |
| Normal | Plainpup | — |
| **Legendary** | **Grokling → Grokmon → Groknova** | Level (wild: peaks night) |
| Showcase | Subshah | — |

---

## For each Fakemon — task template

Copy per mon (or track in a sheet). **Code column** = already in `MonSpecies` / related systems.

| Task | Code | Sprite art |
|------|:----:|:----------:|
| Brainstorm concept | ✅ | — |
| Choose typing | ✅ | — |
| Short design brief | ✅ (category + habitat + trait) | polish if needed |
| Sketch silhouette | partial (3D chibi) | ⬜ 2D |
| Front sprite | ✅ | ✅ generated (all 54) |
| Back sprite | ✅ | ✅ generated (all 54) |
| Color palette | ✅ (primary/secondary hex) | ⬜ finalize |
| Shading / highlights | ❌ | ⬜ |
| Clean outlines | ❌ | ⬜ |
| Readability @ 100% | ❌ | ⬜ |
| Shiny colors | partial (form system) | ⬜ art |
| Name | ✅ | — |
| Pokédex entry | ✅ (`blurb`) | polish |
| Abilities | ✅ | — |
| Base stats | partial (HP/Power → StatBlock) | full BST table later |
| Signature move | partial (Grok line yes) | more as needed |
| Evolution method | ✅ | — |
| Originality review | ongoing | ⬜ per batch |

---

## Roster tracker (50+ priority)

**Legend:** `C` = code done · `S` = sprite done · `—` = n/a

### Batch A — Starters 1–10 (do first for identity)

| # | Id | Type | Stage | C | S | Notes |
|---|----|------|------|:-:|:-:|-------|
| 1 | flameling | Fire | 1 | ✅ | ⬜ | Starter |
| 2 | flarepaw | Fire | 2 | ✅ | ⬜ | |
| 3 | infernox | Fire | 3 | ✅ | ⬜ | |
| 4 | ripplin | Water | 1 | ✅ | ⬜ | Starter |
| 5 | torrentide | Water | 2 | ✅ | ⬜ | |
| 6 | abysshell | Water | 3 | ✅ | ⬜ | |
| 7 | sprouton | Grass | 1 | ✅ | ⬜ | Starter |
| 8 | leafclaw | Grass | 2 | ✅ | ⬜ | |
| 9 | verdantitan | Grass | 3 | ✅ | ⬜ | |
| 10 | voltkit | Electric | 1 | ✅ | ⬜ | Starter |

**Every 5 check (after 5 & 10):** variety · palettes · types · size · quality  

### Batch B — 11–20

| # | Id | Type | Stage | C | S |
|---|----|------|------|:-:|:-:|
| 11 | staticlaw | Electric | 2 | ✅ | ⬜ |
| 12 | thundermaw | Electric | 3 | ✅ | ⬜ |
| 13 | zephyrpuff | Flying | 1 | ✅ | ⬜ |
| 14 | galewing | Flying | 2 | ✅ | ⬜ |
| 15 | skyrend | Flying | 3 | ✅ | ⬜ |
| 16 | cinderwhelp | Fire | 1 | ✅ | ⬜ |
| 17 | magmawyr | Fire | 2 | ✅ | ⬜ |
| 18 | dewkit | Water | 1 | ✅ | ⬜ |
| 19 | oceanarch | Water | 2 | ✅ | ⬜ |
| 20 | rockpup | Rock | 1 | ✅ | ⬜ |

### Batch C — 21–30

| # | Id | Type | Stage | C | S |
|---|----|------|------|:-:|:-:|
| 21 | boulderback | Rock | 2 | ✅ | ⬜ |
| 22 | titanpeak | Rock | 3 | ✅ | ⬜ |
| 23 | terracub | Ground | 1 | ✅ | ⬜ |
| 24 | terrahound | Ground | 2 | ✅ | ⬜ |
| 25 | quakejaw | Ground | 3 | ✅ | ⬜ |
| 26 | nightflit | Dark | 1 | ✅ | ⬜ |
| 27 | shadeclaw | Dark | 2 | ✅ | ⬜ |
| 28 | umbrathorn | Dark | 3 | ✅ | ⬜ |
| 29 | drakeling | Dragon | 1 | ✅ | ⬜ |
| 30 | wyrmling | Dragon | 2 | ✅ | ⬜ |

### Batch D — 31–40

| # | Id | Type | Stage | C | S |
|---|----|------|------|:-:|:-:|
| 31 | drakonox | Dragon | 3 | ✅ | ⬜ |
| 32 | mindwisp | Psychic | 1 | ✅ | ⬜ |
| 33 | psycheye | Psychic | 2 | ✅ | ⬜ |
| 34 | cosmosage | Psychic | 3 | ✅ | ⬜ |
| 35 | fisthare | Fighting | 1 | ✅ | ⬜ |
| 36 | brawlbear | Fighting | 2 | ✅ | ⬜ |
| 37 | ironfist | Fighting | 3 | ✅ | ⬜ |
| 38 | glaceling | Ice | 1 | ✅ | ⬜ |
| 39 | frostfang | Ice | 2 | ✅ | ⬜ |
| 40 | glaciarch | Ice | 3 | ✅ | ⬜ |

### Batch E — 41–50 (core closeout)

| # | Id | Type | Stage | C | S |
|---|----|------|------|:-:|:-:|
| 41 | ironbeetle | Steel | 1 | ✅ | ⬜ |
| 42 | gearshell | Steel | 2 | ✅ | ⬜ |
| 43 | bloomite | Bug | 1 | ✅ | ⬜ |
| 44 | silkwing | Bug | 2 | ✅ | ⬜ |
| 45 | venoot | Poison | 1 | ✅ | ⬜ |
| 46 | toxipaw | Poison | 2 | ✅ | ⬜ |
| 47 | phasling | Ghost | 1 | ✅ | ⬜ |
| 48 | wraithowl | Ghost | 2 | ✅ | ⬜ |
| 49 | gleamfae | Fairy | 1 | ✅ | ⬜ |
| 50 | plainpup | Normal | 1 | ✅ | ⬜ |

### Marquee extras (after or between batches)

| Id | Type | Role | C | S |
|----|------|------|:-:|:-:|
| grokling | Psychic/Dark | Legendary pre | ✅ | ⬜ |
| **grokmon** | Psychic/Dark | **Legendary icon** | ✅ | ⬜ |
| groknova | Psychic/Dark | Mythic final | ✅ | ⬜ |
| subshah | Water/Psychic | Showcase | ✅ | ⬜ |

---

## Every 5 sprites

- [ ] Visual variety (no same blob twice)
- [ ] Palettes not too similar (use species hex as base, diverge accents)
- [ ] Type icons/colors readable
- [ ] Size ladder: stage1 small · stage2 mid · stage3 large
- [ ] Quality pass at 100% zoom

---

## Milestones

| Milestone | Target | Status |
|-----------|--------|:------:|
| Sprites 1–10 | Full starter trio lines + Voltkit | ✅ procedural set |
| Sprites 11–20 | Rest starters + gem lines + Rockpup | ✅ procedural set |
| Sprites 21–30 | Rock–Dragon mid bulk | ✅ procedural set |
| Sprites 31–40 | Dragon final + Psychic–Ice | ✅ procedural set |
| Sprites 41–50 | Steel–Normal closeout | ✅ procedural set |
| Grok trio + Subshah | Marquee | ✅ procedural set |

---

## Final polish

- [ ] Review all 50 together (sprite sheet wall)
- [ ] Standardize outline weight + shading language
- [ ] Fix inconsistent palettes
- [ ] Evolution families share shape language / motifs
- [ ] Distinct silhouettes (flip test / black silhouette test)
- [ ] Export format: **PNG, transparent, consistent canvas** (recommend 96×96 or 128×128)
- [ ] Folders: `sprites/front/`, `sprites/back/`, `sprites/shiny/`, `refs/`
- [ ] Master sprite sheet
- [ ] Backup (git + off-machine)
- [ ] Feedback pass
- [ ] Final revisions
- [ ] Publish Fakédex (site / discord / in-mod dex UI)

### Suggested folder layout

```
assets/fakemon/textures/entity/sprites/
  front/
    flameling.png
    ...
  back/
  shiny/
  sheet/
    fakemon_sheet.png
```

### Naming convention (files)

```
{id}.png           front
{id}_back.png      back
{id}_shiny.png     shiny front
```

Matches code ids: `flameling`, `grokmon`, etc.

---

## Recommended art style (decision prompt)

| Style | Pros | Cons |
|-------|------|------|
| **Gen 5 (recommended)** | Clear shapes, modern, good for 50-count | More time per mon |
| Gen 4 | Crisp, classic | Smaller detail budget |
| Gen 3 | Fast, chunky | Can look dated in bulk |
| Chibi hybrid | Matches current 3D models | Less “dex classic” |

**Default for this project: Gen 5 front/back, 96×96, 1px dark outline, 3–4 shade steps.**

---

## What the mod already covers (don’t redo from scratch)

- Names, categories, habitats, traits  
- Primary colors (use as palette anchors)  
- Dex blurbs  
- Types + some dual types  
- Abilities (`Ability.fromSpecies`)  
- Evolutions + methods  
- Spawn rarity / legendary Grok rules  
- 3D chibi stand-ins until sprites land  

**Sprite work is the remaining bulk of this checklist.**

---

## Next action

1. **Lock art style** (Gen 5 recommended).  
2. Draw **Flameling** as the style pilot (front only).  
3. If pilot looks good → finish Fire starter line (1–3), then batch A.  
4. Every 5: variety pass.  
5. Insert **Grokmon** as a showcase after batch A or B for motivation.

When you’re ready to produce art, we can: generate silhouette refs from in-game models, build a sprite import pipeline into the mod, or start with Flameling design briefs expanded for an artist.  

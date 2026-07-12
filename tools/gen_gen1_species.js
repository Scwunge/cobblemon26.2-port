/**
 * Generate full Gen1 MonSpecies + SpeciesTextures + lang entries
 * from Cobblemon originals species JSON + texture/geo folders.
 */
const fs = require("fs");
const path = require("path");

const SPECIES_DIR =
  "C:/Users/conne/Desktop/fakemons/models-gen1/species/generation1";
const TEX_DIR =
  "C:/Users/conne/Desktop/pixelmontest/fakemons/src/main/resources/assets/fakemon/textures/pokemon";
const GEO_DIR =
  "C:/Users/conne/Desktop/pixelmontest/fakemons/src/main/resources/assets/fakemon/bedrock/pokemon/models";
const OUT_MS =
  "C:/Users/conne/Desktop/pixelmontest/fakemons/src/main/java/ai/xai/fakemon/species/MonSpecies.java";
const OUT_ST =
  "C:/Users/conne/Desktop/pixelmontest/fakemons/src/main/java/ai/xai/fakemon/species/SpeciesTextures.java";
const LANG =
  "C:/Users/conne/Desktop/pixelmontest/fakemons/src/main/resources/assets/fakemon/lang/en_us.json";

const TYPE = {
  fire: "FIRE",
  water: "WATER",
  grass: "GRASS",
  electric: "ELECTRIC",
  ground: "GROUND",
  flying: "FLYING",
  fighting: "FIGHTING",
  dragon: "DRAGON",
  dark: "DARK",
  psychic: "PSYCHIC",
  rock: "ROCK",
  ice: "ICE",
  steel: "STEEL",
  fairy: "FAIRY",
  bug: "BUG",
  poison: "POISON",
  ghost: "GHOST",
  normal: "NORMAL",
};

const TYPE_COLOR = {
  FIRE: [0xe85d04, 0x370617],
  WATER: [0x4cc9f0, 0x023e8a],
  GRASS: [0x52b788, 0x1b4332],
  ELECTRIC: [0xfee440, 0x7b2cbf],
  GROUND: [0xd4a373, 0x6f4518],
  FLYING: [0xcaf0f8, 0x48cae4],
  FIGHTING: [0xc1121f, 0x780000],
  DRAGON: [0x5a189a, 0x240046],
  DARK: [0x3c096c, 0x10002b],
  PSYCHIC: [0xff6bcb, 0x8338ec],
  ROCK: [0xadb5bd, 0x495057],
  ICE: [0x90e0ef, 0x0077b6],
  STEEL: [0x8d99ae, 0x2b2d42],
  FAIRY: [0xffafcc, 0xc77dff],
  BUG: [0xa7c957, 0x386641],
  POISON: [0x9b5de5, 0x5a189a],
  GHOST: [0x7b2cbf, 0x240046],
  NORMAL: [0xdee2e6, 0x6c757d],
};

const CATEGORIES = {
  bulbasaur: "Seed",
  ivysaur: "Seed",
  venusaur: "Seed",
  charmander: "Lizard",
  charmeleon: "Flame",
  charizard: "Flame",
  squirtle: "Tiny Turtle",
  wartortle: "Turtle",
  blastoise: "Shellfish",
  caterpie: "Worm",
  metapod: "Cocoon",
  butterfree: "Butterfly",
  weedle: "Hairy Bug",
  kakuna: "Cocoon",
  beedrill: "Poison Bee",
  pidgey: "Tiny Bird",
  pidgeotto: "Bird",
  pidgeot: "Bird",
  rattata: "Mouse",
  raticate: "Mouse",
  spearow: "Tiny Bird",
  fearow: "Beak",
  ekans: "Snake",
  arbok: "Cobra",
  pikachu: "Mouse",
  raichu: "Mouse",
  sandshrew: "Mouse",
  sandslash: "Mouse",
  nidoranf: "Poison Pin",
  nidorina: "Poison Pin",
  nidoqueen: "Drill",
  nidoranm: "Poison Pin",
  nidorino: "Poison Pin",
  nidoking: "Drill",
  clefairy: "Fairy",
  clefable: "Fairy",
  vulpix: "Fox",
  ninetales: "Fox",
  jigglypuff: "Balloon",
  wigglytuff: "Balloon",
  zubat: "Bat",
  golbat: "Bat",
  oddish: "Weed",
  gloom: "Weed",
  vileplume: "Flower",
  paras: "Mushroom",
  parasect: "Mushroom",
  venonat: "Insect",
  venomoth: "Poison Moth",
  diglett: "Mole",
  dugtrio: "Mole",
  meowth: "Scratch Cat",
  persian: "Classy Cat",
  psyduck: "Duck",
  golduck: "Duck",
  mankey: "Pig Monkey",
  primeape: "Pig Monkey",
  growlithe: "Puppy",
  arcanine: "Legendary",
  poliwag: "Tadpole",
  poliwhirl: "Tadpole",
  poliwrath: "Tadpole",
  abra: "Psi",
  kadabra: "Psi",
  alakazam: "Psi",
  machop: "Superpower",
  machoke: "Superpower",
  machamp: "Superpower",
  bellsprout: "Flower",
  weepinbell: "Flycatcher",
  victreebel: "Flycatcher",
  tentacool: "Jellyfish",
  tentacruel: "Jellyfish",
  geodude: "Rock",
  graveler: "Rock",
  golem: "Megaton",
  ponyta: "Fire Horse",
  rapidash: "Fire Horse",
  slowpoke: "Dopey",
  slowbro: "Hermit Crab",
  magnemite: "Magnet",
  magneton: "Magnet",
  farfetchd: "Wild Duck",
  doduo: "Twin Bird",
  dodrio: "Triple Bird",
  seel: "Sea Lion",
  dewgong: "Sea Lion",
  grimer: "Sludge",
  muk: "Sludge",
  shellder: "Bivalve",
  cloyster: "Bivalve",
  gastly: "Gas",
  haunter: "Gas",
  gengar: "Shadow",
  onix: "Rock Snake",
  drowzee: "Hypnosis",
  hypno: "Hypnosis",
  krabby: "River Crab",
  kingler: "Pincer",
  voltorb: "Ball",
  electrode: "Ball",
  exeggcute: "Egg",
  exeggutor: "Coconut",
  cubone: "Lonely",
  marowak: "Bone Keeper",
  hitmonlee: "Kicking",
  hitmonchan: "Punching",
  lickitung: "Licking",
  koffing: "Poison Gas",
  weezing: "Poison Gas",
  rhyhorn: "Spikes",
  rhydon: "Drill",
  chansey: "Egg",
  tangela: "Vine",
  kangaskhan: "Parent",
  horsea: "Dragon",
  seadra: "Dragon",
  goldeen: "Goldfish",
  seaking: "Goldfish",
  staryu: "Star Shape",
  starmie: "Mysterious",
  mrmime: "Barrier",
  scyther: "Mantis",
  jynx: "Human Shape",
  electabuzz: "Electric",
  magmar: "Spitfire",
  pinsir: "Stag Beetle",
  tauros: "Wild Bull",
  magikarp: "Fish",
  gyarados: "Atrocious",
  lapras: "Transport",
  ditto: "Transform",
  eevee: "Evolution",
  vaporeon: "Bubble Jet",
  jolteon: "Lightning",
  flareon: "Flame",
  porygon: "Virtual",
  omanyte: "Spiral",
  omastar: "Spiral",
  kabuto: "Shellfish",
  kabutops: "Shellfish",
  aerodactyl: "Fossil",
  snorlax: "Sleeping",
  articuno: "Freeze",
  zapdos: "Electric",
  moltres: "Flame",
  dratini: "Dragon",
  dragonair: "Dragon",
  dragonite: "Dragon",
  mewtwo: "Genetic",
  mew: "New Species",
};

function enumName(slug) {
  if (slug === "nidoranf") return "NIDORAN_F";
  if (slug === "nidoranm") return "NIDORAN_M";
  if (slug === "mrmime") return "MR_MIME";
  if (slug === "farfetchd") return "FARFETCHD";
  return slug.toUpperCase();
}

function padDex(n) {
  return String(n).padStart(4, "0");
}

function bodyShape(j) {
  const t1 = j.primaryType;
  const t2 = j.secondaryType || "";
  const name = j.name.toLowerCase();
  if (t1 === "ghost" || name.includes("grimer") || name.includes("ditto") || name.includes("gastly"))
    return "AMORPH";
  if (t1 === "bug" || name.includes("paras") || name.includes("venonat")) return "INSECTOID";
  if (t1 === "dragon" || name.includes("ekans") || name.includes("onix") || name.includes("steelix"))
    return "SERPENT";
  if (t1 === "flying" || t2 === "flying" || name.includes("bird") || name.includes("bat") || name.includes("zubat"))
    return "AVIAN";
  if (t1 === "water" || name.includes("fish") || name.includes("karp") || name.includes("tentac") || name.includes("shellder") || name.includes("staryu") || name.includes("starmie") || name.includes("horsea") || name.includes("goldeen") || name.includes("seaking") || name.includes("magikarp") || name.includes("gyarados") || name.includes("lapras") || name.includes("omanyte") || name.includes("kabuto") || name.includes("seel") || name.includes("dewgong") || name.includes("poli") || name.includes("psyduck") || name.includes("golduck") || name.includes("slow") || name.includes("vaporeon") || name.includes("squirtle") || name.includes("wartortle") || name.includes("blastoise") || name.includes("tentacool") || name.includes("tentacruel"))
    return "AQUATIC";
  if (t1 === "rock" || t1 === "steel" || name.includes("geodude") || name.includes("golem") || name.includes("graveler") || name.includes("cloyster") || name.includes("forretress"))
    return "ARMORED";
  if (t1 === "fairy" || name.includes("clefairy") || name.includes("clefable") || name.includes("jiggly") || name.includes("wiggly") || name.includes("mew"))
    return "FEY";
  if (
    name.includes("machop") ||
    name.includes("machoke") ||
    name.includes("machamp") ||
    name.includes("hitmon") ||
    name.includes("alakazam") ||
    name.includes("kadabra") ||
    name.includes("abra") ||
    name.includes("primeape") ||
    name.includes("mankey") ||
    name.includes("charizard") ||
    name.includes("blastoise") ||
    name.includes("venusaur") ||
    name.includes("mewtwo") ||
    name.includes("gengar") ||
    name.includes("hypno") ||
    name.includes("jynx") ||
    name.includes("electabuzz") ||
    name.includes("magmar") ||
    name.includes("kangaskhan") ||
    name.includes("mr. mime") ||
    name.includes("mrmime") ||
    name.includes("snorlax") ||
    name.includes("dragonite")
  )
    return "BIPED";
  return "QUAD";
}

function findFolder(dex, slug) {
  const candidates = [
    `${padDex(dex)}_${slug}`,
    // nidoran folders
    slug === "nidoranf" ? `${padDex(dex)}_nidoranf` : null,
    slug === "nidoranm" ? `${padDex(dex)}_nidoranm` : null,
  ].filter(Boolean);
  for (const c of candidates) {
    if (fs.existsSync(path.join(TEX_DIR, c))) return c;
  }
  // scan
  const dirs = fs.readdirSync(TEX_DIR);
  const hit = dirs.find((d) => d.startsWith(padDex(dex) + "_") || d.endsWith("_" + slug));
  return hit || `${padDex(dex)}_${slug}`;
}

function pickTexture(folder, slug) {
  const dir = path.join(TEX_DIR, folder);
  if (!fs.existsSync(dir)) return `${folder}/${slug}.png`;
  const files = fs.readdirSync(dir).filter((f) => f.endsWith(".png") && !f.includes("shiny") && !f.includes("emissive"));
  const prefer = [
    `${slug}.png`,
    `${slug}_male.png`,
    `${slug}_female.png`,
    // nidoran
    "nidoranf.png",
    "nidoranm.png",
  ];
  for (const p of prefer) {
    if (files.includes(p)) return `${folder}/${p}`;
  }
  // first base-looking
  const base = files.find((f) => !f.includes("_") || f.startsWith(slug));
  return `${folder}/${base || files[0] || slug + ".png"}`;
}

function pickGeo(folder, slug) {
  const dir = path.join(GEO_DIR, folder);
  if (!fs.existsSync(dir)) return `${folder}/${slug}.geo.json`;
  const files = fs.readdirSync(dir).filter((f) => f.endsWith(".geo.json"));
  const prefer = [
    `${slug}.geo.json`,
    `${slug}_male.geo.json`,
    `${slug}_female.geo.json`,
  ];
  for (const p of prefer) {
    if (files.includes(p)) return `${folder}/${p}`;
  }
  return `${folder}/${files[0] || slug + ".geo.json"}`;
}

function stageFor(slug, evoIntoMap, preEvoSet) {
  // if nothing evolves into this and it evolves -> stage 1
  // if something evolves into this and it evolves -> stage 2
  // if something evolves into this and nothing further (or rare) -> stage 3
  const hasPre = preEvoSet.has(slug);
  const hasNext = evoIntoMap.has(slug);
  if (!hasPre && hasNext) return 1;
  if (hasPre && hasNext) return 2;
  if (hasPre && !hasNext) {
    // could be stage 2 of 2-stage or stage 3 of 3-stage
    // if pre's pre exists -> 3 else 2
    return 3; // refined below
  }
  return 1; // single stage
}

// load all species
const files = fs.readdirSync(SPECIES_DIR).filter((f) => f.endsWith(".json"));
const species = files
  .map((f) => {
    const j = JSON.parse(fs.readFileSync(path.join(SPECIES_DIR, f), "utf8"));
    const slug = f.replace(/\.json$/, "");
    return { slug, j };
  })
  .sort((a, b) => a.j.nationalPokedexNumber - b.j.nationalPokedexNumber);

// evo maps (only gen1 targets)
const slugSet = new Set(species.map((s) => s.slug));
const evoLinks = []; // {from, to, method, level}
const evoIntoMap = new Map(); // from -> to
const preEvoSet = new Set();

for (const { slug, j } of species) {
  const evos = j.evolutions || [];
  for (const e of evos) {
    let to = (e.result || "").split(" ")[0].toLowerCase();
    // normalize
    if (to === "nidoran♀" || to === "nidoran-f") to = "nidoranf";
    if (to === "nidoran♂" || to === "nidoran-m") to = "nidoranm";
    if (to === "mr.mime" || to === "mr-mime") to = "mrmime";
    if (to === "farfetch'd" || to === "farfetchd") to = "farfetchd";
    if (!slugSet.has(to)) continue; // skip gen2+
    let method = "LEVEL";
    let level = 20;
    if (e.variant === "level_up") {
      method = "LEVEL";
      const req = (e.requirements || []).find((r) => r.variant === "level");
      level = req ? req.minLevel : 20;
    } else if (e.variant === "item_interact") {
      const ctx = (e.requiredContext || "").toLowerCase();
      if (ctx.includes("fire")) method = "FIRE_GEM";
      else if (ctx.includes("water")) method = "WATER_GEM";
      else method = "FIRE_GEM"; // treat thunder/leaf/moon stones as fire gem proxy for now, or LEVEL
      // Better: use LEVEL with high level for stone evo as fallback via FIRE/WATER only where we have items
      if (ctx.includes("thunder") || ctx.includes("leaf") || ctx.includes("moon") || ctx.includes("sun") || ctx.includes("ice") || ctx.includes("dusk") || ctx.includes("dawn")) {
        // no item yet — use high level as proxy so line still progresses in-game
        method = "LEVEL";
        level = 36;
      }
      if (method !== "LEVEL") level = 0;
    } else {
      method = "LEVEL";
      level = 30;
    }
    evoLinks.push({ from: slug, to, method, level });
    if (!evoIntoMap.has(slug)) evoIntoMap.set(slug, to);
    preEvoSet.add(to);
  }
}

// refine stages
function computeStage(slug) {
  // walk back
  let depth = 0;
  let cur = slug;
  const rev = new Map([...evoIntoMap.entries()].map(([a, b]) => [b, a]));
  while (rev.has(cur) && depth < 5) {
    cur = rev.get(cur);
    depth++;
  }
  return depth + 1;
}

const legendaries = new Set(["articuno", "zapdos", "moltres", "mewtwo", "mew"]);
const starters = new Set(["bulbasaur", "charmander", "squirtle"]);

// Build enum entries
const enumEntries = [];
const texMap = [];
const geoMap = [];
const displayNames = [];

for (const { slug, j } of species) {
  const en = enumName(slug);
  const id = slug === "nidoranf" ? "nidoran_f" : slug === "nidoranm" ? "nidoran_m" : slug === "mrmime" ? "mr_mime" : slug === "farfetchd" ? "farfetchd" : slug;
  const el = TYPE[j.primaryType] || "NORMAL";
  const [c1, c2] = TYPE_COLOR[el];
  const shape = bodyShape(j);
  const hp = j.baseStats?.hp ?? 50;
  const atk = j.baseStats?.attack ?? 50;
  const spatk = j.baseStats?.special_attack ?? 50;
  const power = Math.round((atk + spatk) / 2);
  const stage = Math.min(3, computeStage(slug));
  const starter = starters.has(slug);
  const cat = CATEGORIES[slug] || j.name;
  const heightM = (j.height || 10) / 10;
  const weightKg = (j.weight || 100) / 10;
  const folder = findFolder(j.nationalPokedexNumber, slug);
  const tex = pickTexture(folder, slug);
  const geo = pickGeo(folder, slug);
  const habitat = (j.labels || []).join(", ") || "Kanto";
  const ability = (j.abilities && j.abilities[0]) || "none";
  const blurb = `${j.name}, the ${cat} Pokémon. National Dex #${j.nationalPokedexNumber}.`;

  enumEntries.push(
    `    ${en}("${id}", MonElement.${el}, BodyShape.${shape}, ${hp}, ${power}, 0x${c1.toString(16).toUpperCase().padStart(6, "0")}, 0x${c2.toString(16).toUpperCase().padStart(6, "0")}, ${starter}, ${stage},\n` +
      `            "${cat}", "${habitat.replace(/"/g, '\\"')}", "${ability}", ${heightM.toFixed(1)}f, ${weightKg.toFixed(1)}f,\n` +
      `            "${blurb.replace(/"/g, '\\"')}")`
  );
  texMap.push(`            case ${en} -> "${tex}";`);
  geoMap.push(`            case ${en} -> "${geo}";`);
  displayNames.push({ id, name: j.name });
}

// evo link lines
const linkLines = evoLinks.map((e) => {
  return `        link(${enumName(e.from)}, ${enumName(e.to)}, EvolutionMethod.${e.method}, ${e.level});`;
});

// secondary types
const secondaryCases = [];
for (const { slug, j } of species) {
  if (j.secondaryType && TYPE[j.secondaryType]) {
    secondaryCases.push(`            case ${enumName(slug)} -> Optional.of(MonElement.${TYPE[j.secondaryType]});`);
  }
}

// legendary spawn cases
const legEnums = [...legendaries].map(enumName);

const ms = `package ai.xai.fakemon.species;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.StringRepresentable;

/**
 * Full Gen 1 Kanto dex (151) — ids/names match Cobblemon originals.
 * One shared {@code wild_mon} entity carries any species via DATA_SPECIES.
 */
public enum MonSpecies implements StringRepresentable {
${enumEntries.join(",\n")};

    private final String id;
    private final MonElement element;
    private final BodyShape bodyShape;
    private final int baseHp;
    private final int basePower;
    private final int primaryColor;
    private final int secondaryColor;
    private final boolean starter;
    private final int stage;
    private final String category;
    private final String habitat;
    private final String trait;
    private final float heightM;
    private final float weightKg;
    private final String blurb;

    private MonSpecies evolvesInto;
    private EvolutionMethod evolutionMethod = EvolutionMethod.NONE;
    private int evolutionLevel;

    MonSpecies(
            String id,
            MonElement element,
            BodyShape bodyShape,
            int baseHp,
            int basePower,
            int primaryColor,
            int secondaryColor,
            boolean starter,
            int stage,
            String category,
            String habitat,
            String trait,
            float heightM,
            float weightKg,
            String blurb
    ) {
        this.id = id;
        this.element = element;
        this.bodyShape = bodyShape;
        this.baseHp = baseHp;
        this.basePower = basePower;
        this.primaryColor = primaryColor;
        this.secondaryColor = secondaryColor;
        this.starter = starter;
        this.stage = stage;
        this.category = category;
        this.habitat = habitat;
        this.trait = trait;
        this.heightM = heightM;
        this.weightKg = weightKg;
        this.blurb = blurb;
    }

    static {
${linkLines.join("\n")}
    }

    private static void link(MonSpecies from, MonSpecies to, EvolutionMethod method, int level) {
        from.evolvesInto = to;
        from.evolutionMethod = method;
        from.evolutionLevel = level;
    }

    public String id() { return id; }
    public MonElement element() { return element; }
    public BodyShape bodyShape() { return bodyShape; }
    public int baseHp() { return baseHp; }
    public int basePower() { return basePower; }

    public int baseStat(StatId stat) {
        return switch (stat) {
            case HP -> baseHp;
            case ATK -> Math.max(5, basePower);
            case DEF -> Math.max(5, baseHp / 3 + 5);
            case SP_ATK -> Math.max(5, (int) Math.round(basePower * 0.95));
            case SP_DEF -> Math.max(5, baseHp / 4 + 8);
            case SPEED -> Math.max(5, 35 + basePower / 2 + stage * 3);
        };
    }

    public MonElement primaryType() { return element; }

    public Optional<MonElement> secondaryType() {
        return switch (this) {
${secondaryCases.join("\n")}
            default -> Optional.empty();
        };
    }

    public Ability defaultAbility() {
        return Ability.fromSpecies(this);
    }

    public float maleRatio() {
        return switch (this) {
            case MAGNEMITE, MAGNETON, VOLTORB, ELECTRODE, STARYU, STARMIE, DITTO, ARTICUNO, ZAPDOS, MOLTRES, MEWTWO, MEW -> -1f;
            case NIDORAN_F, NIDORINA, NIDOQUEEN -> 0f;
            case NIDORAN_M, NIDORINO, NIDOKING -> 1f;
            default -> 0.5f;
        };
    }

    public float sizeScale() { return modelScale(); }
    public float heightVariance() { return heightM * 0.15f; }

    public java.util.Set<String> spawnTags() {
        if (isLegendary()) {
            return java.util.Set.of("mountain", "peaks", "legendary", "surface");
        }
        return switch (element) {
            case FIRE -> java.util.Set.of("hot", "nether", "desert", "surface");
            case WATER -> java.util.Set.of("water", "river", "ocean", "beach", "surface");
            case GRASS -> java.util.Set.of("forest", "plains", "surface");
            case ELECTRIC -> java.util.Set.of("plains", "mountain", "surface");
            case GROUND, ROCK -> java.util.Set.of("desert", "mountain", "cave", "surface");
            case ICE -> java.util.Set.of("cold", "mountain", "surface");
            case DARK, GHOST -> java.util.Set.of("dark", "cave", "night", "surface");
            case DRAGON -> java.util.Set.of("mountain", "surface");
            case FLYING -> java.util.Set.of("plains", "mountain", "surface");
            case PSYCHIC -> java.util.Set.of("surface", "dark");
            case STEEL -> java.util.Set.of("cave", "surface");
            case BUG -> java.util.Set.of("forest", "plains", "surface");
            case POISON -> java.util.Set.of("swamp", "forest", "surface");
            case FAIRY -> java.util.Set.of("forest", "plains", "surface");
            case FIGHTING -> java.util.Set.of("plains", "surface");
            default -> java.util.Set.of("surface");
        };
    }

    public SpawnRarity spawnRarity() {
        if (isLegendarySpecies()) return SpawnRarity.LEGENDARY;
        if (this == DRAGONITE || this == GYARADOS || this == SNORLAX || this == LAPRAS) return SpawnRarity.ULTRA_RARE;
        if (starter) return SpawnRarity.ULTRA_RARE;
        if (stage >= 3) return SpawnRarity.RARE;
        if (stage == 2) return SpawnRarity.UNCOMMON;
        return SpawnRarity.COMMON;
    }

    private boolean isLegendarySpecies() {
        return this == ARTICUNO || this == ZAPDOS || this == MOLTRES || this == MEWTWO || this == MEW;
    }

    public float spawnWeight() {
        if (this == MEWTWO) return 0.25f;
        if (this == MEW) return 0.15f;
        if (this == ARTICUNO || this == ZAPDOS || this == MOLTRES) return 0.4f;
        if (this == DRAGONITE) return 0.9f;
        if (starter) return 8.5f;
        if (stage >= 3) return 1.5f;
        if (stage == 2) return 3.5f;
        return switch (element) {
            case NORMAL, BUG, GRASS -> 8.0f;
            case WATER, FLYING, ELECTRIC -> 6.5f;
            case FIRE, ROCK, GROUND, FIGHTING -> 5.5f;
            case ICE, STEEL, POISON, FAIRY -> 4.5f;
            case DARK, GHOST, PSYCHIC, DRAGON -> 3.5f;
            default -> 5.0f;
        };
    }

    public java.util.Set<String> requiredBiomeKeywords() {
        return switch (this) {
            case ARTICUNO, ZAPDOS, MOLTRES, MEWTWO, MEW -> java.util.Set.of(
                    "jagged_peaks", "frozen_peaks", "stony_peaks", "snowy_slopes", "mountain"
            );
            case CHARMANDER -> java.util.Set.of("desert", "badland", "savanna", "nether", "basalt");
            case SQUIRTLE -> java.util.Set.of("river", "ocean", "beach", "swamp", "lush");
            case BULBASAUR -> java.util.Set.of("forest", "jungle", "taiga", "grove", "floral");
            case PIKACHU -> java.util.Set.of("plains", "savanna", "meadow", "windswept");
            case PIDGEY -> java.util.Set.of("plains", "forest", "meadow", "windswept");
            case MAGIKARP, TENTACOOL, HORSEA, SHELLDER, KRABBY, STARYU -> java.util.Set.of("ocean", "river", "beach", "water");
            default -> java.util.Set.of();
        };
    }

    public java.util.Set<String> bannedBiomeKeywords() {
        if (isLegendarySpecies()) {
            return java.util.Set.of("meadow", "grove", "beach", "ocean", "river", "desert");
        }
        return java.util.Set.of();
    }

    public boolean requiresNightSpawn() {
        return this == ZUBAT || this == GOLBAT || this == GASTLY || this == HAUNTER || this == GENGAR
                || this == MEOWTH || this == PERSIAN || isLegendarySpecies();
    }

    public boolean requiresOpenSky() {
        return this == PIDGEY || this == PIDGEOTTO || this == PIDGEOT
                || this == SPEAROW || this == FEAROW || isLegendarySpecies();
    }

    public boolean isLegendary() {
        return spawnRarity() == SpawnRarity.LEGENDARY;
    }

    public int wildDespawnSeconds() {
        if (isLegendary()) return 900;
        if (spawnRarity() == SpawnRarity.ULTRA_RARE) return 600;
        return 300 + stage * 60;
    }

    public int primaryColor() { return primaryColor; }
    public int secondaryColor() { return secondaryColor; }
    public boolean isStarter() { return starter; }
    public int stage() { return stage; }
    public String category() { return category; }
    public String habitat() { return habitat; }
    public String trait() { return trait; }
    public float heightM() { return heightM; }
    public float weightKg() { return weightKg; }
    public String blurb() { return blurb; }

    public Optional<MonSpecies> evolvesInto() { return Optional.ofNullable(evolvesInto); }
    public EvolutionMethod evolutionMethod() { return evolutionMethod; }
    public int evolutionLevel() { return evolutionLevel; }

    public boolean canEvolveByLevel(int level) {
        return evolutionMethod == EvolutionMethod.LEVEL && evolvesInto != null && level >= evolutionLevel;
    }
    public boolean canEvolveByFireGem() {
        return evolutionMethod == EvolutionMethod.FIRE_GEM && evolvesInto != null;
    }
    public boolean canEvolveByWaterGem() {
        return evolutionMethod == EvolutionMethod.WATER_GEM && evolvesInto != null;
    }

    public int maxHpForLevel(int level) {
        return StatBlock.compute(this, level, Nature.HARDY, MonForm.NORMAL).hp();
    }

    public float modelScale() {
        float base = switch (stage) {
            case 2 -> 1.15f;
            case 3 -> 1.35f;
            default -> 1.0f;
        };
        if (isLegendary()) base *= 1.15f;
        if (this == SNORLAX || this == GYARADOS || this == ONIX || this == DRAGONITE) base *= 1.25f;
        if (bodyShape == BodyShape.ARMORED || bodyShape == BodyShape.SERPENT) base += 0.08f;
        if (bodyShape == BodyShape.FEY || bodyShape == BodyShape.AMORPH) base -= 0.05f;
        return base;
    }

    public MutableComponent displayName() {
        return Component.translatable("species.fakemon." + id);
    }

    public MutableComponent categoryLine() {
        if (isLegendary()) {
            return Component.literal("Legendary · " + category + " Pokémon");
        }
        return Component.literal(category + " Pokémon");
    }

    public MutableComponent description() {
        if (blurb == null || blurb.isBlank()) {
            return Component.translatable("species.fakemon." + id + ".desc");
        }
        return Component.literal(blurb);
    }

    public MutableComponent evolutionHint() {
        if (evolvesInto == null || evolutionMethod == EvolutionMethod.NONE) {
            return Component.translatable("species.fakemon.no_evolution");
        }
        return switch (evolutionMethod) {
            case LEVEL -> Component.translatable("species.fakemon.evo_level", evolvesInto.displayName(), evolutionLevel);
            case FIRE_GEM -> Component.translatable("species.fakemon.evo_fire_gem", evolvesInto.displayName());
            case WATER_GEM -> Component.translatable("species.fakemon.evo_water_gem", evolvesInto.displayName());
            default -> Component.empty();
        };
    }

    public MutableComponent sizeLine() {
        return Component.literal(String.format(Locale.US, "Ht %.1fm  Wt %.1fkg", heightM, weightKg));
    }

    @Override
    public String getSerializedName() { return id; }

    public static List<MonSpecies> starters() {
        return Arrays.stream(values()).filter(MonSpecies::isStarter).toList();
    }

    public static int count() { return values().length; }

    public static Optional<MonSpecies> byId(String id) {
        if (id == null || id.isBlank()) return Optional.empty();
        String key = id.toLowerCase(Locale.ROOT);
        key = switch (key) {
            // old fakemon ids
            case "flameling" -> "charmander";
            case "flarepaw" -> "charmeleon";
            case "infernox" -> "charizard";
            case "ripplin" -> "squirtle";
            case "torrentide" -> "wartortle";
            case "abysshell" -> "blastoise";
            case "sprouton" -> "bulbasaur";
            case "leafclaw" -> "ivysaur";
            case "verdantitan" -> "venusaur";
            case "voltkit" -> "pikachu";
            case "staticlaw" -> "raichu";
            case "thundermaw" -> "electabuzz";
            case "zephyrpuff" -> "pidgey";
            case "galewing" -> "pidgeotto";
            case "skyrend" -> "pidgeot";
            case "cinderwhelp" -> "vulpix";
            case "magmawyr" -> "arcanine";
            case "dewkit" -> "poliwag";
            case "oceanarch" -> "vaporeon";
            case "rockpup" -> "geodude";
            case "boulderback" -> "graveler";
            case "titanpeak" -> "golem";
            case "terracub" -> "diglett";
            case "terrahound" -> "dugtrio";
            case "quakejaw" -> "rhydon";
            case "nightflit" -> "zubat";
            case "shadeclaw", "haunter_dark", "wraithowl" -> "haunter";
            case "umbrathorn" -> "gengar";
            case "drakeling" -> "dratini";
            case "wyrmling" -> "dragonair";
            case "drakonox" -> "dragonite";
            case "mindwisp" -> "abra";
            case "psycheye" -> "kadabra";
            case "cosmosage" -> "alakazam";
            case "fisthare" -> "machop";
            case "brawlbear" -> "machoke";
            case "ironfist" -> "machamp";
            case "glaceling" -> "seel";
            case "frostfang" -> "dewgong";
            case "glaciarch" -> "articuno";
            case "ironbeetle" -> "magnemite";
            case "gearshell" -> "magneton";
            case "bloomite" -> "caterpie";
            case "silkwing" -> "butterfree";
            case "venoot", "nidoranf" -> "nidoran_f";
            case "toxipaw" -> "nidorina";
            case "phasling" -> "gastly";
            case "gleamfae" -> "clefable";
            case "plainpup" -> "rattata";
            case "grokling" -> "mew";
            case "grokmon" -> "mewtwo";
            case "groknova" -> "moltres";
            case "subshah" -> "tentacruel";
            case "nidoran♀" -> "nidoran_f";
            case "nidoran♂" -> "nidoran_m";
            case "mr.mime", "mrmime" -> "mr_mime";
            default -> key;
        };
        String finalKey = key;
        return Arrays.stream(values()).filter(s -> s.id.equals(finalKey)).findFirst();
    }

    public static MonSpecies byIdOrDefault(String id) {
        return byId(id).orElse(RATTATA);
    }
}
`;

const st = `package ai.xai.fakemon.species;

import ai.xai.fakemon.Fakemon;
import net.minecraft.resources.Identifier;

/**
 * Gen 1 species → official Cobblemon original textures + Bedrock geo
 * (from Desktop/fakemons/originals).
 */
public final class SpeciesTextures {
    private SpeciesTextures() {}

    public static Identifier texture(MonSpecies species) {
        return Identifier.fromNamespaceAndPath(Fakemon.MOD_ID, "textures/pokemon/" + pathFor(species));
    }

    public static Identifier geo(MonSpecies species) {
        return Identifier.fromNamespaceAndPath(
                Fakemon.MOD_ID,
                "bedrock/pokemon/models/" + geoPathFor(species)
        );
    }

    public static String pathFor(MonSpecies species) {
        return switch (species) {
${texMap.join("\n")}
        };
    }

    public static String geoPathFor(MonSpecies species) {
        return switch (species) {
${geoMap.join("\n")}
        };
    }

    public static Identifier shinyTexture(MonSpecies species) {
        String path = pathFor(species);
        String shiny = path.replace(".png", "_shiny.png");
        return Identifier.fromNamespaceAndPath(Fakemon.MOD_ID, "textures/pokemon/" + shiny);
    }
}
`;

fs.writeFileSync(OUT_MS, ms, "utf8");
fs.writeFileSync(OUT_ST, st, "utf8");
console.log("Wrote MonSpecies:", species.length);
console.log("Evo links:", evoLinks.length);
console.log("Sample tex:", texMap[0], texMap[3]);
console.log("Sample geo:", geoMap[0], geoMap[24]);

// update lang
let lang = JSON.parse(fs.readFileSync(LANG, "utf8"));
// remove old species.* keys that are display names of mons
for (const k of Object.keys(lang)) {
  if (k.startsWith("species.fakemon.") && !k.includes("no_evolution") && !k.includes("evo_") && !k.endsWith(".desc")) {
    // keep structural keys only
    if (!["species.fakemon.no_evolution", "species.fakemon.evo_level", "species.fakemon.evo_fire_gem", "species.fakemon.evo_water_gem"].includes(k)) {
      delete lang[k];
    }
  }
}
for (const d of displayNames) {
  lang["species.fakemon." + d.id] = d.name;
}
fs.writeFileSync(LANG, JSON.stringify(lang, null, 2) + "\n", "utf8");
console.log("Lang species entries:", displayNames.length);

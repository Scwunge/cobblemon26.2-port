const fs = require("fs");
const p = "src/main/java/com/cobblemon/mod/species/OwnedMon.java";
let t = fs.readFileSync(p, "utf8");

if (!t.includes("private final String speciesId")) {
  t = t.replace(
    "private final MonSpecies species;",
    "private final String speciesId;\n    private final MonSpecies species;"
  );
}

// Both constructors assign this.species = species;
t = t.replaceAll(
  "this.species = species;\n        this.nickname = nickname == null ? \"\" : nickname;",
  "this.species = species == null ? MonSpecies.RATTATA : species;\n        this.speciesId = this.species.id();\n        this.nickname = nickname == null ? \"\" : nickname;"
);

// fromCodec
t = t.replace(
  `MonSpecies sp = MonSpecies.byIdOrDefault(speciesId);
        Ability ab = ability == null || ability.isBlank()
                ? sp.defaultAbility()
                : Ability.byId(ability);
        return new OwnedMon(
                sp, nickname, level, hp, exp, uuid, moves,
                Nature.byId(nature), MonGender.byId(gender), MonForm.byId(form), ab, size,
                MonStatus.byId(status), heldItem == null ? "" : heldItem
        );`,
  `SpeciesHandle handle = SpeciesHandle.of(speciesId);
        MonSpecies sp = handle.asEnumOrFallback();
        Ability ab = ability == null || ability.isBlank()
                ? handle.defaultAbility()
                : Ability.byId(ability);
        OwnedMon mon = new OwnedMon(
                sp, nickname, level, hp, exp, uuid, moves,
                Nature.byId(nature), MonGender.byId(gender), MonForm.byId(form), ab, size,
                MonStatus.byId(status), heldItem == null ? "" : heldItem
        );
        return mon.withSpeciesId(handle.id());`
);

t = t.replace(
  "ByteBufCodecs.STRING_UTF8.encode(buf, m.species.id());",
  "ByteBufCodecs.STRING_UTF8.encode(buf, m.speciesId());"
);

t = t.replace(
  `public StatBlock stats() {
        return StatBlock.compute(species, level, nature, form);
    }

    public MonSpecies species() {
        return species;
    }`,
  `public StatBlock stats() {
        return StatBlock.compute(SpeciesHandle.of(speciesId()), level, nature, form);
    }

    public String speciesId() {
        return speciesId != null && !speciesId.isBlank() ? speciesId : species.id();
    }

    public SpeciesHandle handle() {
        return SpeciesHandle.of(speciesId());
    }

    /** Preserve datapack species ids beyond Gen1 enum. */
    public OwnedMon withSpeciesId(String id) {
        if (id == null || id.isBlank()) {
            return this;
        }
        SpeciesHandle h = SpeciesHandle.of(id);
        OwnedMon base = new OwnedMon(
                h.asEnumOrFallback(), nickname, level, hp, exp, uuid, moveIds,
                nature, gender, form, ability, sizeScale, status, heldItem
        );
        return base.forceSpeciesId(h.id());
    }

    private OwnedMon forceSpeciesId(String id) {
        return new OwnedMon(
                id, species, nickname, level, hp, exp, uuid, moveIds,
                nature, gender, form, ability, sizeScale, status, heldItem
        );
    }

    public MonSpecies species() {
        return species;
    }`
);

// Add full constructor with speciesId if missing
if (!t.includes("OwnedMon(\n            String speciesId,")) {
  // Insert after the long constructor (the one with status, heldItem)
  const marker = `this.moveIds = normalizeMoves(species, this.level, moveIds);
        int max = stats().hp();
        this.hp = Math.max(0, Math.min(max, hp <= 0 ? max : hp));
    }`;
  const insert = `this.moveIds = normalizeMoves(this.species, this.level, moveIds);
        int max = stats().hp();
        this.hp = Math.max(0, Math.min(max, hp <= 0 ? max : hp));
    }

    /** Internal: preserve exact species id (for non-enum datapack species). */
    private OwnedMon(
            String speciesId,
            MonSpecies species,
            String nickname,
            int level,
            int hp,
            int exp,
            UUID uuid,
            List<String> moveIds,
            Nature nature,
            MonGender gender,
            MonForm form,
            Ability ability,
            float sizeScale,
            MonStatus status,
            String heldItem
    ) {
        this.species = species == null ? MonSpecies.RATTATA : species;
        this.speciesId = speciesId == null || speciesId.isBlank() ? this.species.id() : speciesId;
        this.nickname = nickname == null ? "" : nickname;
        this.level = Math.max(1, Math.min(MAX_LEVEL, level));
        this.exp = Math.max(0, exp);
        this.uuid = uuid == null ? UUID.randomUUID() : uuid;
        this.nature = nature == null ? Nature.HARDY : nature;
        this.gender = gender == null ? MonGender.GENDERLESS : gender;
        this.form = form == null ? MonForm.NORMAL : form;
        this.ability = ability == null || ability == Ability.NONE
                ? SpeciesHandle.of(this.speciesId).defaultAbility()
                : ability;
        this.sizeScale = sizeScale <= 0.01f ? 1.0f : Math.min(2.5f, Math.max(0.5f, sizeScale));
        this.status = status == null ? MonStatus.NONE : status;
        this.heldItem = heldItem == null ? "" : heldItem;
        this.moveIds = normalizeMoves(this.species, this.level, moveIds);
        int max = stats().hp();
        this.hp = Math.max(0, Math.min(max, hp <= 0 ? max : hp));
    }`;
  if (t.includes(marker)) {
    t = t.replace(marker, insert);
  } else {
    console.warn("constructor marker not found");
  }
}

t = t.replace(
  `public float renderScale() {
        float s = species.sizeScale() * sizeScale;`,
  `public float renderScale() {
        float s = SpeciesHandle.of(speciesId()).sizeScale() * sizeScale;`
);

t = t.replace(
  `public MutableComponent displayName() {
        if (!nickname.isBlank()) {
            return Component.literal(nickname);
        }
        return species.displayName();
    }`,
  `public MutableComponent displayName() {
        if (!nickname.isBlank()) {
            return Component.literal(nickname);
        }
        return SpeciesHandle.of(speciesId()).displayName();
    }`
);

// createWild from string
if (!t.includes("createWild(String speciesId")) {
  t = t.replace(
    `public static OwnedMon createWild(MonSpecies species, int level, RandomSource random) {
        int lvl = Math.max(1, Math.min(MAX_LEVEL, level));
        Nature nature = Nature.random(random);
        MonGender gender = MonGender.roll(random, species.maleRatio());
        MonForm form = MonForm.rollWild(random);
        Ability ability = species.defaultAbility();
        float size = 1.0f + (random.nextFloat() - 0.5f) * 0.2f;
        if (form == MonForm.ALPHA) {
            size *= 1.25f;
        }
        List<String> moves = Learnsets.movesKnownAtLevel(species, lvl).stream().map(MonMove::id).toList();
        OwnedMon mon = new OwnedMon(species, "", lvl, 1, 0, UUID.randomUUID(), moves, nature, gender, form, ability, size);
        return mon.withHp(mon.maxHp());
    }`,
    `public static OwnedMon createWild(MonSpecies species, int level, RandomSource random) {
        return createWild(species == null ? "rattata" : species.id(), level, random);
    }

    /** Create wild mon from any Cobblemon species id (Gen1+ datapack). */
    public static OwnedMon createWild(String speciesId, int level, RandomSource random) {
        SpeciesHandle handle = SpeciesHandle.of(speciesId);
        MonSpecies species = handle.asEnumOrFallback();
        int lvl = Math.max(1, Math.min(MAX_LEVEL, level));
        Nature nature = Nature.random(random);
        MonGender gender = MonGender.roll(random, handle.maleRatio());
        MonForm form = MonForm.rollWild(random);
        Ability ability = handle.defaultAbility();
        float size = 1.0f + (random.nextFloat() - 0.5f) * 0.2f;
        if (form == MonForm.ALPHA) {
            size *= 1.25f;
        }
        List<String> moves = Learnsets.movesKnownAtLevel(species, lvl).stream().map(MonMove::id).toList();
        OwnedMon mon = new OwnedMon(species, "", lvl, 1, 0, UUID.randomUUID(), moves, nature, gender, form, ability, size);
        mon = mon.withSpeciesId(handle.id());
        return mon.withHp(mon.maxHp());
    }`
  );
}

fs.writeFileSync(p, t);
console.log("OwnedMon patched OK");

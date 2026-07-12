package com.cobblemon.mod.species;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import com.cobblemon.mod.battle.Learnsets;
import com.cobblemon.mod.battle.MonMove;
import com.cobblemon.mod.battle.MoveAliases;
import com.cobblemon.mod.util.DataKeys;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.RandomSource;

/**
 * Individual creature instance — species template + rolled identity (nature, gender, form, ability, size).
 */
public final class OwnedMon {
    public static final int MAX_MOVES = 4;
    public static final int MAX_LEVEL = 100;

    /**
     * Persistence codec using official-style {@link DataKeys} field names.
     * Decodes legacy lowercase keys via {@link Codec#withAlternative} so existing worlds keep working.
     * New writes use the modern keys only.
     */
    private static final Codec<OwnedMon> CODEC_MODERN = RecordCodecBuilder.create(instance -> instance.group(
            // CRITICAL: use speciesId (full dex), never Gen1 enum stand-in (solrock→geodude, etc.)
            Codec.STRING.fieldOf(DataKeys.POKEMON_SPECIES_IDENTIFIER).forGetter(OwnedMon::speciesId),
            Codec.STRING.optionalFieldOf(DataKeys.POKEMON_NICKNAME, "").forGetter(OwnedMon::nickname),
            Codec.INT.fieldOf(DataKeys.POKEMON_LEVEL).forGetter(OwnedMon::level),
            Codec.INT.fieldOf(DataKeys.POKEMON_HEALTH).forGetter(OwnedMon::hp),
            Codec.INT.fieldOf(DataKeys.POKEMON_EXPERIENCE).forGetter(OwnedMon::exp),
            UUIDUtil.CODEC.fieldOf(DataKeys.POKEMON_UUID).forGetter(OwnedMon::uuid),
            Codec.STRING.listOf().optionalFieldOf(DataKeys.POKEMON_MOVESET, List.of()).forGetter(m -> m.moveIds),
            Codec.INT.listOf().optionalFieldOf(DataKeys.POKEMON_MOVESET_MOVEPP, List.of()).forGetter(m -> m.movePp),
            Codec.STRING.optionalFieldOf(DataKeys.POKEMON_NATURE, Nature.HARDY.getSerializedName()).forGetter(m -> m.nature.getSerializedName()),
            Codec.STRING.optionalFieldOf(DataKeys.POKEMON_GENDER, MonGender.GENDERLESS.id()).forGetter(m -> m.gender.id()),
            Codec.STRING.optionalFieldOf(DataKeys.POKEMON_FORM_ID, MonForm.NORMAL.id()).forGetter(m -> m.form.id()),
            Codec.STRING.optionalFieldOf(DataKeys.POKEMON_ABILITY_NAME, "").forGetter(m -> m.ability.id()),
            Codec.FLOAT.optionalFieldOf(DataKeys.POKEMON_SCALE_MODIFIER, 1.0f).forGetter(OwnedMon::sizeScale),
            Codec.STRING.optionalFieldOf(DataKeys.POKEMON_STATUS_NAME, MonStatus.NONE.id()).forGetter(m -> m.status.id()),
            Codec.STRING.optionalFieldOf(DataKeys.HELD_ITEM, "").forGetter(OwnedMon::heldItem),
            // Nested to stay within RecordCodecBuilder's 16-field limit
            Genetics.CODEC.optionalFieldOf("genetics", Genetics.EMPTY).forGetter(OwnedMon::genetics)
    ).apply(instance, OwnedMon::fromCodec));

    /** Pre-DataKeys field names (species/level/hp/…); kept for world migration. */
    private static final Codec<OwnedMon> CODEC_LEGACY = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("species").forGetter(OwnedMon::speciesId),
            Codec.STRING.optionalFieldOf("nickname", "").forGetter(OwnedMon::nickname),
            Codec.INT.fieldOf("level").forGetter(OwnedMon::level),
            Codec.INT.fieldOf("hp").forGetter(OwnedMon::hp),
            Codec.INT.fieldOf("exp").forGetter(OwnedMon::exp),
            UUIDUtil.CODEC.fieldOf("uuid").forGetter(OwnedMon::uuid),
            Codec.STRING.listOf().optionalFieldOf("moves", List.of()).forGetter(m -> m.moveIds),
            Codec.INT.listOf().optionalFieldOf("movePp", List.of()).forGetter(m -> m.movePp),
            Codec.STRING.optionalFieldOf("nature", Nature.HARDY.getSerializedName()).forGetter(m -> m.nature.getSerializedName()),
            Codec.STRING.optionalFieldOf("gender", MonGender.GENDERLESS.id()).forGetter(m -> m.gender.id()),
            Codec.STRING.optionalFieldOf("form", MonForm.NORMAL.id()).forGetter(m -> m.form.id()),
            Codec.STRING.optionalFieldOf("ability", "").forGetter(m -> m.ability.id()),
            Codec.FLOAT.optionalFieldOf("sizeScale", 1.0f).forGetter(OwnedMon::sizeScale),
            Codec.STRING.optionalFieldOf("status", MonStatus.NONE.id()).forGetter(m -> m.status.id()),
            Codec.STRING.optionalFieldOf("heldItem", "").forGetter(OwnedMon::heldItem),
            Genetics.CODEC.optionalFieldOf("genetics", Genetics.EMPTY).forGetter(OwnedMon::genetics)
    ).apply(instance, OwnedMon::fromCodec));

    public static final Codec<OwnedMon> CODEC = Codec.withAlternative(CODEC_MODERN, CODEC_LEGACY);

    public static final StreamCodec<ByteBuf, OwnedMon> STREAM_CODEC = StreamCodec.of(
            OwnedMon::encode,
            OwnedMon::decode
    );

    private final String speciesId;
    private final MonSpecies species;
    private final String nickname;
    private final int level;
    private final int hp;
    private final int exp;
    private final UUID uuid;
    private final List<String> moveIds;
    /** Current PP per move slot (same length as moveIds). */
    private final List<Integer> movePp;
    private final Nature nature;
    private final MonGender gender;
    private final MonForm form;
    private final Ability ability;
    private final float sizeScale;
    private final MonStatus status;
    private final String heldItem;
    /** HP, Atk, Def, SpA, SpD, Spe — 0–31. */
    private final int[] ivs;
    /** Same order — 0–252 each, total ≤ 510. */
    private final int[] evs;
    /** C2 mark id (e.g. mark_fishing), blank if none. Stored under genetics. */
    private final String mark;

    public OwnedMon(
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
            float sizeScale
    ) {
        this(species, nickname, level, hp, exp, uuid, moveIds, nature, gender, form, ability, sizeScale,
                MonStatus.NONE, "");
    }

    public OwnedMon(
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
        this(species == null ? MonSpecies.RATTATA.id() : species.id(),
                species, nickname, level, hp, exp, uuid, moveIds, null,
                nature, gender, form, ability, sizeScale, status, heldItem, null, null);
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
        this(speciesId, species, nickname, level, hp, exp, uuid, moveIds, null,
                nature, gender, form, ability, sizeScale, status, heldItem, null, null);
    }

    private OwnedMon(
            String speciesId,
            MonSpecies species,
            String nickname,
            int level,
            int hp,
            int exp,
            UUID uuid,
            List<String> moveIds,
            List<Integer> movePp,
            Nature nature,
            MonGender gender,
            MonForm form,
            Ability ability,
            float sizeScale,
            MonStatus status,
            String heldItem
    ) {
        this(speciesId, species, nickname, level, hp, exp, uuid, moveIds, movePp,
                nature, gender, form, ability, sizeScale, status, heldItem, null, null);
    }

    private OwnedMon(
            String speciesId,
            MonSpecies species,
            String nickname,
            int level,
            int hp,
            int exp,
            UUID uuid,
            List<String> moveIds,
            List<Integer> movePp,
            Nature nature,
            MonGender gender,
            MonForm form,
            Ability ability,
            float sizeScale,
            MonStatus status,
            String heldItem,
            int[] ivs,
            int[] evs
    ) {
        this(speciesId, species, nickname, level, hp, exp, uuid, moveIds, movePp,
                nature, gender, form, ability, sizeScale, status, heldItem, ivs, evs, "");
    }

    private OwnedMon(
            String speciesId,
            MonSpecies species,
            String nickname,
            int level,
            int hp,
            int exp,
            UUID uuid,
            List<String> moveIds,
            List<Integer> movePp,
            Nature nature,
            MonGender gender,
            MonForm form,
            Ability ability,
            float sizeScale,
            MonStatus status,
            String heldItem,
            int[] ivs,
            int[] evs,
            String mark
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
        this.ivs = StatBlock.normalizeSix(ivs, 0, StatBlock.IV_MAX, 15);
        this.evs = clampEvs(StatBlock.normalizeSix(evs, 0, StatBlock.EV_MAX_STAT, 0));
        this.mark = mark == null ? "" : mark;
        this.moveIds = normalizeMoves(this.speciesId, this.level, moveIds);
        this.movePp = normalizePp(this.moveIds, movePp);
        int max = stats().hp();
        // Allow 0 HP (fainted). Negative hp means "fill to max" for legacy create helpers.
        // IMPORTANT: hp<=0 used to force max — that made lethal hits fully heal the mon.
        this.hp = hp < 0 ? max : Math.max(0, Math.min(max, hp));
    }

    private static int[] clampEvs(int[] ev) {
        int total = 0;
        for (int v : ev) {
            total += v;
        }
        if (total <= StatBlock.EV_MAX_TOTAL) {
            return ev;
        }
        // Scale down proportionally if over cap
        double scale = StatBlock.EV_MAX_TOTAL / (double) total;
        int[] out = new int[6];
        int sum = 0;
        for (int i = 0; i < 6; i++) {
            out[i] = (int) Math.floor(ev[i] * scale);
            sum += out[i];
        }
        // Fix remainder on first stats
        int rem = StatBlock.EV_MAX_TOTAL - sum;
        for (int i = 0; i < 6 && rem > 0; i++) {
            int add = Math.min(rem, StatBlock.EV_MAX_STAT - out[i]);
            out[i] += add;
            rem -= add;
        }
        return out;
    }

    private static OwnedMon fromCodec(
            String speciesId, String nickname, int level, int hp, int exp, UUID uuid,
            List<String> moves, List<Integer> movePp, String nature, String gender, String form, String ability, float size,
            String status, String heldItem, Genetics genetics
    ) {
        SpeciesHandle handle = SpeciesHandle.of(speciesId);
        MonSpecies sp = handle.asEnumOrFallback();
        Ability ab = ability == null || ability.isBlank()
                ? handle.defaultAbility()
                : Ability.byId(ability);
        Genetics g = genetics == null ? Genetics.EMPTY : genetics;
        // Dual-read shiny: FormId "shiny" OR genetics.Shiny / legacy shiny boolean
        MonForm resolvedForm = resolveForm(form, g.shiny());
        return new OwnedMon(
                handle.id(), sp, nickname, level, hp, exp, uuid, moves, movePp,
                Nature.byId(nature), MonGender.byId(gender), resolvedForm, ab, size,
                MonStatus.byId(status), heldItem == null ? "" : heldItem, g.ivArray(), g.evArray(),
                g.mark() == null ? "" : g.mark()
        );
    }

    public String mark() {
        return mark == null ? "" : mark;
    }

    public boolean hasMark() {
        return mark != null && !mark.isBlank();
    }

    public OwnedMon withMark(String markId) {
        String m = markId == null ? "" : markId;
        if (m.equals(this.mark)) {
            return this;
        }
        return new OwnedMon(
                speciesId(), species, nickname, level, hp, exp, uuid, moveIds, movePp,
                nature, gender, form, ability, sizeScale, status, heldItem, ivs, evs, m
        );
    }

    /**
     * FormId is primary; optional {@link DataKeys#POKEMON_SHINY} (stored under genetics for field-count)
     * upgrades NORMAL → SHINY when true. Alpha/shadow forms are left as-is.
     */
    private static MonForm resolveForm(String formId, boolean shinyFlag) {
        MonForm f = MonForm.byId(formId);
        if (shinyFlag && f == MonForm.NORMAL) {
            return MonForm.SHINY;
        }
        return f;
    }

    private Genetics genetics() {
        // Persist shiny + mark under genetics (field-count limit on OwnedMon root)
        return new Genetics(ivList(), evList(), form == MonForm.SHINY, mark == null ? "" : mark);
    }

    private List<Integer> ivList() {
        return List.of(ivs[0], ivs[1], ivs[2], ivs[3], ivs[4], ivs[5]);
    }

    private List<Integer> evList() {
        return List.of(evs[0], evs[1], evs[2], evs[3], evs[4], evs[5]);
    }

    /**
     * Nested IV/EV + shiny blob for codec (keeps OwnedMon field count ≤ 16).
     * Shiny is dual-read with {@link DataKeys#POKEMON_SHINY} / legacy {@code shiny}
     * and merged with {@link DataKeys#POKEMON_FORM_ID} on decode.
     */
    public record Genetics(List<Integer> ivs, List<Integer> evs, boolean shiny, String mark) {
        public static final Genetics EMPTY = new Genetics(List.of(), List.of(), false, "");

        public Genetics(List<Integer> ivs, List<Integer> evs) {
            this(ivs, evs, false, "");
        }

        public Genetics(List<Integer> ivs, List<Integer> evs, boolean shiny) {
            this(ivs, evs, shiny, "");
        }

        /**
         * Dual-read IVs/EVs/shiny: official {@link DataKeys} names preferred on write;
         * legacy lowercase keys still accepted on read (cannot use {@code withAlternative}
         * here — optional fields would succeed empty and drop legacy values).
         */
        public static final Codec<Genetics> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.listOf().optionalFieldOf(DataKeys.POKEMON_IVS).forGetter(g -> Optional.of(g.ivs())),
                Codec.INT.listOf().optionalFieldOf("ivs").forGetter(g -> Optional.empty()),
                Codec.INT.listOf().optionalFieldOf(DataKeys.POKEMON_EVS).forGetter(g -> Optional.of(g.evs())),
                Codec.INT.listOf().optionalFieldOf("evs").forGetter(g -> Optional.empty()),
                Codec.BOOL.optionalFieldOf(DataKeys.POKEMON_SHINY).forGetter(g -> g.shiny() ? Optional.of(true) : Optional.empty()),
                Codec.BOOL.optionalFieldOf("shiny").forGetter(g -> Optional.empty()),
                Codec.STRING.optionalFieldOf("mark", "").forGetter(Genetics::mark)
        ).apply(i, (ivsModern, ivsLegacy, evsModern, evsLegacy, shinyModern, shinyLegacy, mark) -> new Genetics(
                ivsModern.or(() -> ivsLegacy).orElse(List.of()),
                evsModern.or(() -> evsLegacy).orElse(List.of()),
                shinyModern.or(() -> shinyLegacy).orElse(false),
                mark == null ? "" : mark
        )));

        int[] ivArray() {
            return listToSix(ivs, 15);
        }

        int[] evArray() {
            return listToSix(evs, 0);
        }

        private static int[] listToSix(List<Integer> list, int fill) {
            int[] out = new int[]{fill, fill, fill, fill, fill, fill};
            if (list != null) {
                for (int j = 0; j < 6 && j < list.size(); j++) {
                    if (list.get(j) != null) {
                        out[j] = list.get(j);
                    }
                }
            }
            return out;
        }
    }

    private static void encode(ByteBuf buf, OwnedMon m) {
        ByteBufCodecs.STRING_UTF8.encode(buf, m.speciesId());
        ByteBufCodecs.STRING_UTF8.encode(buf, m.nickname);
        ByteBufCodecs.VAR_INT.encode(buf, m.level);
        ByteBufCodecs.VAR_INT.encode(buf, m.hp);
        ByteBufCodecs.VAR_INT.encode(buf, m.exp);
        UUIDUtil.STREAM_CODEC.encode(buf, m.uuid);
        ByteBufCodecs.VAR_INT.encode(buf, m.moveIds.size());
        for (String id : m.moveIds) {
            ByteBufCodecs.STRING_UTF8.encode(buf, id);
        }
        ByteBufCodecs.VAR_INT.encode(buf, m.movePp.size());
        for (int pp : m.movePp) {
            ByteBufCodecs.VAR_INT.encode(buf, pp);
        }
        ByteBufCodecs.STRING_UTF8.encode(buf, m.nature.getSerializedName());
        ByteBufCodecs.STRING_UTF8.encode(buf, m.gender.id());
        ByteBufCodecs.STRING_UTF8.encode(buf, m.form.id());
        ByteBufCodecs.STRING_UTF8.encode(buf, m.ability.id());
        buf.writeFloat(m.sizeScale);
        ByteBufCodecs.STRING_UTF8.encode(buf, m.status.id());
        ByteBufCodecs.STRING_UTF8.encode(buf, m.heldItem);
        for (int i = 0; i < 6; i++) {
            ByteBufCodecs.VAR_INT.encode(buf, m.ivs[i]);
        }
        for (int i = 0; i < 6; i++) {
            ByteBufCodecs.VAR_INT.encode(buf, m.evs[i]);
        }
    }

    private static OwnedMon decode(ByteBuf buf) {
        String speciesId = ByteBufCodecs.STRING_UTF8.decode(buf);
        String nickname = ByteBufCodecs.STRING_UTF8.decode(buf);
        int level = ByteBufCodecs.VAR_INT.decode(buf);
        int hp = ByteBufCodecs.VAR_INT.decode(buf);
        int exp = ByteBufCodecs.VAR_INT.decode(buf);
        UUID uuid = UUIDUtil.STREAM_CODEC.decode(buf);
        int n = ByteBufCodecs.VAR_INT.decode(buf);
        List<String> moves = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            moves.add(ByteBufCodecs.STRING_UTF8.decode(buf));
        }
        List<Integer> pps = new ArrayList<>();
        int ppN = ByteBufCodecs.VAR_INT.decode(buf);
        for (int i = 0; i < ppN && i < MAX_MOVES + 4; i++) {
            pps.add(ByteBufCodecs.VAR_INT.decode(buf));
        }
        String nature = ByteBufCodecs.STRING_UTF8.decode(buf);
        String gender = ByteBufCodecs.STRING_UTF8.decode(buf);
        String form = ByteBufCodecs.STRING_UTF8.decode(buf);
        String ability = ByteBufCodecs.STRING_UTF8.decode(buf);
        float size = buf.readFloat();
        String status = ByteBufCodecs.STRING_UTF8.decode(buf);
        String held = ByteBufCodecs.STRING_UTF8.decode(buf);
        List<Integer> ivs = new ArrayList<>(6);
        List<Integer> evs = new ArrayList<>(6);
        // Prefer full IV/EV payload; if buffer short (old packet), fill defaults
        try {
            for (int i = 0; i < 6; i++) {
                ivs.add(ByteBufCodecs.VAR_INT.decode(buf));
            }
            for (int i = 0; i < 6; i++) {
                evs.add(ByteBufCodecs.VAR_INT.decode(buf));
            }
        } catch (Exception ignored) {
            ivs.clear();
            evs.clear();
        }
        MonForm formDecoded = MonForm.byId(form);
        return fromCodec(speciesId, nickname, level, hp, exp, uuid, moves, pps, nature, gender, form, ability, size, status, held,
                new Genetics(ivs, evs, formDecoded == MonForm.SHINY));
    }

    private static List<String> normalizeMoves(String speciesId, int level, List<String> raw) {
        List<String> out = new ArrayList<>();
        if (raw != null) {
            for (String id : raw) {
                if (id == null || id.isBlank()) {
                    continue;
                }
                // Keep official Showdown / datapack move ids (vinewhip, ember, …).
                // Battle resolves stats via ShowdownMoveDex; MonMove is only a fallback.
                String resolved = com.cobblemon.mod.battle.ShowdownMoveDex.canonicalize(id);
                if (resolved.isEmpty()) {
                    continue;
                }
                if (!out.contains(resolved)) {
                    out.add(resolved);
                }
                if (out.size() >= MAX_MOVES) {
                    break;
                }
            }
        }
        if (out.isEmpty()) {
            String sid = speciesId == null || speciesId.isBlank() ? MonSpecies.RATTATA.id() : speciesId;
            for (MonMove m : Learnsets.movesKnownAtLevel(sid, level)) {
                out.add(com.cobblemon.mod.battle.ShowdownMoveDex.canonicalize(m.id()));
                if (out.size() >= MAX_MOVES) {
                    break;
                }
            }
        }
        if (out.isEmpty()) {
            out.add("tackle");
        }
        return List.copyOf(out);
    }

    private static List<Integer> normalizePp(List<String> moves, List<Integer> raw) {
        List<Integer> out = new ArrayList<>(moves.size());
        for (int i = 0; i < moves.size(); i++) {
            com.cobblemon.mod.battle.BattleMove m = com.cobblemon.mod.battle.BattleMove.resolve(moves.get(i));
            int max = m.getMaxPp();
            int cur = max;
            if (raw != null && i < raw.size() && raw.get(i) != null) {
                cur = Math.max(0, Math.min(max, raw.get(i)));
            }
            out.add(cur);
        }
        return List.copyOf(out);
    }

    public static OwnedMon createWild(MonSpecies species, int level) {
        return createWild(species, level, RandomSource.create());
    }

    public static OwnedMon createWild(MonSpecies species, int level, RandomSource random) {
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
        List<String> moves = com.cobblemon.mod.battle.DatapackLearnsets.battleMovesKnownAtLevel(handle.id(), lvl)
                .stream()
                .map(com.cobblemon.mod.battle.BattleMove::getId)
                .toList();
        int[] ivs = StatBlock.rollIvs(random);
        // Use private ctor so datapack speciesId is preserved (not Gen1 type stand-in)
        OwnedMon mon = new OwnedMon(
                handle.id(), species, "", lvl, 1, 0, UUID.randomUUID(), moves, null,
                nature, gender, form, ability, size, MonStatus.NONE, "", ivs, new int[6]
        );
        return mon.withHp(mon.maxHp());
    }

    public StatBlock stats() {
        return StatBlock.compute(SpeciesHandle.of(speciesId()), level, nature, form, ivs, evs);
    }

    public int[] ivs() {
        return ivs.clone();
    }

    public int[] evs() {
        return evs.clone();
    }

    public int iv(int index) {
        return index >= 0 && index < 6 ? ivs[index] : 0;
    }

    public int ev(int index) {
        return index >= 0 && index < 6 ? evs[index] : 0;
    }

    /** Add EVs (clamped). Used by vitamins / battle yield lite. */
    public OwnedMon addEvs(int addHp, int addAtk, int addDef, int addSpa, int addSpd, int addSpe) {
        int[] next = evs.clone();
        int[] add = {addHp, addAtk, addDef, addSpa, addSpd, addSpe};
        for (int i = 0; i < 6; i++) {
            next[i] = Math.min(StatBlock.EV_MAX_STAT, next[i] + Math.max(0, add[i]));
        }
        next = clampEvs(next);
        return copy(nickname, level, this.hp, exp, uuid, moveIds, movePp, nature, gender, form, ability, sizeScale, status, heldItem, ivs, next);
    }

    /** Breed helper: inherit IVs from parents (per-stat coin flip). */
    public static int[] breedIvs(int[] parentA, int[] parentB, RandomSource random) {
        int[] a = StatBlock.normalizeSix(parentA, 0, StatBlock.IV_MAX, 15);
        int[] b = StatBlock.normalizeSix(parentB, 0, StatBlock.IV_MAX, 15);
        int[] out = new int[6];
        for (int i = 0; i < 6; i++) {
            out[i] = random.nextBoolean() ? a[i] : b[i];
            if (random.nextFloat() < 0.2f) {
                out[i] = Math.min(StatBlock.IV_MAX, out[i] + 2);
            }
        }
        return out;
    }

    public OwnedMon withIvs(int[] newIvs) {
        int[] iv = StatBlock.normalizeSix(newIvs, 0, StatBlock.IV_MAX, 15);
        OwnedMon next = copy(nickname, level, hp, exp, uuid, moveIds, movePp, nature, gender, form, ability, sizeScale, status, heldItem, iv, evs);
        int newMax = next.maxHp();
        int newHp = Math.min(newMax, Math.max(isFainted() ? 0 : 1, (int) Math.round(newMax * hpRatio())));
        return next.withHp(newHp);
    }

    public String speciesId() {
        return speciesId != null && !speciesId.isBlank() ? speciesId : species.id();
    }

    public SpeciesHandle handle() {
        return SpeciesHandle.of(speciesId());
    }

    /** Preserve datapack species ids beyond Gen1 enum. */
    public OwnedMon withSpeciesId(String id) {
        if (id == null || id.isBlank() || id.equalsIgnoreCase(speciesId())) {
            return this;
        }
        SpeciesHandle h = SpeciesHandle.of(id);
        return new OwnedMon(
                h.id(), h.asEnumOrFallback(), nickname, level, hp, exp, uuid, moveIds,
                nature, gender, form, ability, sizeScale, status, heldItem
        );
    }

    public MonSpecies species() {
        return species;
    }

    /** Real primary type from datapack (not Gen1 enum fallback). */
    public MonElement primaryType() {
        return handle().primaryType();
    }

    public java.util.Optional<MonElement> secondaryType() {
        return handle().secondaryType();
    }

    public String nickname() {
        return nickname;
    }

    public int level() {
        return level;
    }

    public int hp() {
        return hp;
    }

    public int exp() {
        return exp;
    }

    public UUID uuid() {
        return uuid;
    }

    public List<String> moveIds() {
        return moveIds;
    }

    public List<MonMove> moves() {
        return moveIds.stream().map(MonMove::byIdOrDefault).collect(Collectors.toList());
    }

    /** Moves with Showdown stats when available (preferred for battle). */
    public List<com.cobblemon.mod.battle.BattleMove> battleMoves() {
        return moveIds.stream().map(com.cobblemon.mod.battle.BattleMove::resolve).collect(Collectors.toList());
    }

    public com.cobblemon.mod.battle.BattleMove battleMove(int slot) {
        if (slot < 0 || slot >= moveIds.size()) {
            return com.cobblemon.mod.battle.BattleMove.of(MonMove.TACKLE);
        }
        return com.cobblemon.mod.battle.BattleMove.resolve(moveIds.get(slot));
    }

    public List<Integer> movePpList() {
        return movePp;
    }

    public int movePp(int slot) {
        if (slot < 0 || slot >= movePp.size()) {
            return 0;
        }
        return movePp.get(slot);
    }

    public int moveMaxPp(int slot) {
        if (slot < 0 || slot >= moveIds.size()) {
            return 0;
        }
        return battleMove(slot).getMaxPp();
    }

    public boolean knowsMoveId(String moveId) {
        if (moveId == null) {
            return false;
        }
        String key = com.cobblemon.mod.battle.ShowdownMoveDex.canonicalize(moveId);
        return moveIds.contains(key) || moveIds.contains(moveId);
    }

    public boolean hasPp(int slot) {
        return movePp(slot) > 0;
    }

    public boolean knowsMove(MonMove move) {
        return moveIds.contains(move.id());
    }

    /** Spend 1 PP on a move slot (no-op if empty). */
    public OwnedMon consumePp(int slot) {
        if (slot < 0 || slot >= movePp.size() || movePp.get(slot) <= 0) {
            return this;
        }
        List<Integer> next = new ArrayList<>(movePp);
        next.set(slot, next.get(slot) - 1);
        return copy(nickname, level, hp, exp, uuid, moveIds, next, nature, gender, form, ability, sizeScale, status, heldItem);
    }

    /** Restore one move's PP by amount (capped). Prefers first non-full slot if slot &lt; 0. */
    public OwnedMon restorePp(int slot, int amount) {
        if (amount <= 0 || moveIds.isEmpty()) {
            return this;
        }
        List<Integer> next = new ArrayList<>(movePp);
        int target = slot;
        if (target < 0 || target >= next.size()) {
            target = -1;
            for (int i = 0; i < next.size(); i++) {
                if (next.get(i) < moveMaxPp(i)) {
                    target = i;
                    break;
                }
            }
            if (target < 0) {
                return this;
            }
        }
        int max = moveMaxPp(target);
        int cur = next.get(target);
        if (cur >= max) {
            return this;
        }
        next.set(target, Math.min(max, cur + amount));
        return copy(nickname, level, hp, exp, uuid, moveIds, next, nature, gender, form, ability, sizeScale, status, heldItem);
    }

    /** Fully restore PP on all moves (PC / heal machine / elixir). */
    public OwnedMon restoreAllPp() {
        List<Integer> next = new ArrayList<>(moveIds.size());
        boolean changed = false;
        for (int i = 0; i < moveIds.size(); i++) {
            int max = moveMaxPp(i);
            int cur = i < movePp.size() ? movePp.get(i) : max;
            if (cur != max) {
                changed = true;
            }
            next.add(max);
        }
        if (!changed) {
            return this;
        }
        return copy(nickname, level, hp, exp, uuid, moveIds, next, nature, gender, form, ability, sizeScale, status, heldItem);
    }

    /** Max-ether: fully restore one move (first not full). */
    public OwnedMon restoreOneMoveFullPp() {
        for (int i = 0; i < moveIds.size(); i++) {
            if (movePp(i) < moveMaxPp(i)) {
                List<Integer> next = new ArrayList<>(movePp);
                next.set(i, moveMaxPp(i));
                return copy(nickname, level, hp, exp, uuid, moveIds, next, nature, gender, form, ability, sizeScale, status, heldItem);
            }
        }
        return this;
    }

    public Nature nature() {
        return nature;
    }

    public MonGender gender() {
        return gender;
    }

    public OwnedMon withGender(MonGender newGender) {
        MonGender g = newGender == null ? MonGender.GENDERLESS : newGender;
        if (g == gender) {
            return this;
        }
        return copy(nickname, level, hp, exp, uuid, moveIds, movePp, nature, g, form, ability, sizeScale, status, heldItem, ivs, evs);
    }

    public OwnedMon withSizeScale(float scale) {
        float s = scale <= 0.01f ? 1.0f : Math.min(2.5f, Math.max(0.5f, scale));
        if (Math.abs(s - sizeScale) < 0.0001f) {
            return this;
        }
        return copy(nickname, level, hp, exp, uuid, moveIds, movePp, nature, gender, form, ability, s, status, heldItem, ivs, evs);
    }

    public MonForm form() {
        return form;
    }

    /** True when this mon uses the shiny form (FormId {@code shiny} / DataKeys.Shiny). */
    public boolean isShiny() {
        return form == MonForm.SHINY || form.isShiny();
    }

    public OwnedMon withForm(MonForm newForm) {
        MonForm f = newForm == null ? MonForm.NORMAL : newForm;
        if (f == form) {
            return this;
        }
        OwnedMon next = copy(nickname, level, hp, exp, uuid, moveIds, movePp, nature, gender, f, ability, sizeScale, status, heldItem, ivs, evs);
        int newMax = next.maxHp();
        int newHp = Math.min(newMax, Math.max(isFainted() ? 0 : 1, (int) Math.round(newMax * hpRatio())));
        return next.withHp(newHp);
    }

    /** Force shiny form (or clear back to normal if {@code shiny} is false and currently shiny). */
    public OwnedMon withShiny(boolean shiny) {
        if (shiny) {
            return form == MonForm.SHINY ? this : withForm(MonForm.SHINY);
        }
        return form == MonForm.SHINY ? withForm(MonForm.NORMAL) : this;
    }

    public Ability ability() {
        return ability;
    }

    public float sizeScale() {
        return sizeScale;
    }

    public MonStatus status() {
        return status;
    }

    public String heldItem() {
        return heldItem;
    }

    public boolean hasHeldItem() {
        return heldItem != null && !heldItem.isBlank();
    }

    /** Render scale: species × individual × form. */
    public float renderScale() {
        float s = SpeciesHandle.of(speciesId()).sizeScale() * sizeScale;
        if (form == MonForm.ALPHA) {
            s *= 1.15f;
        }
        return s;
    }

    public int maxHp() {
        return stats().hp();
    }

    public float hpRatio() {
        int max = maxHp();
        return max <= 0 ? 0f : (float) hp / (float) max;
    }

    public boolean isFainted() {
        return hp <= 0;
    }

    public MutableComponent displayName() {
        if (!nickname.isBlank()) {
            return Component.literal(nickname);
        }
        return SpeciesHandle.of(speciesId()).displayName();
    }

    public OwnedMon withHp(int newHp) {
        return copy(nickname, level, newHp, exp, uuid, moveIds, nature, gender, form, ability, sizeScale, status, heldItem);
    }

    public OwnedMon withNickname(String newNickname) {
        return copy(newNickname == null ? "" : newNickname, level, hp, exp, uuid, moveIds, nature, gender, form, ability, sizeScale, status, heldItem);
    }

    public OwnedMon healed() {
        return withHp(maxHp()).withStatus(MonStatus.NONE).restoreAllPp();
    }

    public OwnedMon withStatus(MonStatus newStatus) {
        MonStatus s = newStatus == null ? MonStatus.NONE : newStatus;
        return copy(nickname, level, hp, exp, uuid, moveIds, nature, gender, form, ability, sizeScale, s, heldItem);
    }

    public OwnedMon withHeldItem(String itemId) {
        String id = itemId == null ? "" : itemId;
        return copy(nickname, level, hp, exp, uuid, moveIds, nature, gender, form, ability, sizeScale, status, id);
    }

    public OwnedMon cured(String medicineId) {
        if (status.isNone()) {
            return this;
        }
        if (status.curedBy(medicineId)) {
            return withStatus(MonStatus.NONE);
        }
        return this;
    }

    public OwnedMon withLevel(int newLevel) {
        int lvl = Math.max(1, Math.min(MAX_LEVEL, newLevel));
        OwnedMon next = copy(nickname, lvl, hp, exp, uuid, moveIds, nature, gender, form, ability, sizeScale, status, heldItem);
        int newMax = next.maxHp();
        int newHp = Math.min(newMax, Math.max(1, (int) Math.round(newMax * hpRatio())));
        return next.withHp(newHp);
    }

    public OwnedMon withNature(Nature newNature) {
        Nature n = newNature == null ? Nature.HARDY : newNature;
        OwnedMon next = copy(nickname, level, hp, exp, uuid, moveIds, n, gender, form, ability, sizeScale, status, heldItem);
        int newMax = next.maxHp();
        int newHp = Math.min(newMax, Math.max(isFainted() ? 0 : 1, (int) Math.round(newMax * hpRatio())));
        return next.withHp(newHp);
    }

    /** Heal by flat HP (capped at max). No-op if fainted or full. */
    public OwnedMon healBy(int amount) {
        if (amount <= 0 || isFainted() || hp >= maxHp()) {
            return this;
        }
        return withHp(Math.min(maxHp(), hp + amount));
    }

    /** Heal by fraction of max HP (0..1). */
    public OwnedMon healFraction(float fraction) {
        int amount = Math.max(1, Math.round(maxHp() * Math.max(0f, fraction)));
        return healBy(amount);
    }

    /** Revive fainted mon to half or full HP. */
    public OwnedMon revive(boolean full) {
        if (!isFainted()) {
            return this;
        }
        int target = full ? maxHp() : Math.max(1, maxHp() / 2);
        return withHp(target);
    }

    public OwnedMon withExp(int newExp) {
        return copy(nickname, level, hp, Math.max(0, newExp), uuid, moveIds, nature, gender, form, ability, sizeScale, status, heldItem);
    }

    public OwnedMon withMoves(List<MonMove> moves) {
        List<String> ids = moves.stream()
                .map(m -> com.cobblemon.mod.battle.ShowdownMoveDex.canonicalize(m.id()))
                .limit(MAX_MOVES)
                .toList();
        return copy(nickname, level, hp, exp, uuid, ids, null, nature, gender, form, ability, sizeScale, status, heldItem);
    }

    /** Replace moveset by official move ids (e.g. starter balanced kit). */
    public OwnedMon withMoveIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return this;
        }
        List<String> next = ids.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(s -> com.cobblemon.mod.battle.ShowdownMoveDex.canonicalize(s))
                .distinct()
                .limit(MAX_MOVES)
                .toList();
        if (next.isEmpty()) {
            return this;
        }
        return copy(nickname, level, hp, exp, uuid, next, null, nature, gender, form, ability, sizeScale, status, heldItem);
    }

    public OwnedMon learnMove(MonMove move) {
        if (move == null) {
            return this;
        }
        String id = com.cobblemon.mod.battle.ShowdownMoveDex.canonicalize(move.id());
        if (knowsMoveId(id) || knowsMove(move)) {
            return this;
        }
        com.cobblemon.mod.battle.BattleMove bm = com.cobblemon.mod.battle.BattleMove.resolve(id);
        List<String> next = new ArrayList<>(moveIds);
        List<Integer> ppNext = new ArrayList<>(movePp);
        if (next.size() < MAX_MOVES) {
            next.add(id);
            ppNext.add(bm.getMaxPp());
        } else {
            next.remove(0);
            if (!ppNext.isEmpty()) {
                ppNext.remove(0);
            }
            next.add(id);
            ppNext.add(bm.getMaxPp());
        }
        return copy(nickname, level, hp, exp, uuid, next, ppNext, nature, gender, form, ability, sizeScale, status, heldItem);
    }

    public OwnedMon evolveTo(MonSpecies next) {
        if (next == null) {
            return this;
        }
        return evolveToId(next.id());
    }

    /** Evolve into any datapack species id (preserves full-dex chains). */
    public OwnedMon evolveToId(String nextId) {
        if (nextId == null || nextId.isBlank()) {
            return this;
        }
        SpeciesHandle th = SpeciesHandle.of(nextId);
        List<String> kept = new ArrayList<>(moveIds);
        for (MonMove m : Learnsets.movesKnownAtLevel(th.id(), level)) {
            if (!kept.contains(m.id()) && kept.size() < MAX_MOVES) {
                kept.add(m.id());
            }
        }
        Ability ab = th.defaultAbility();
        OwnedMon mon = new OwnedMon(
                th.id(), th.asEnumOrFallback(), nickname, level, 1, exp, uuid, kept,
                nature, gender, form, ab, sizeScale, status, heldItem
        );
        return mon.withHp(mon.maxHp());
    }

    public boolean canLevelEvolve() {
        if (species.canEvolveByLevel(level)) {
            return true;
        }
        return datapackLevelEvoTarget().isPresent();
    }

    public OptionalEvo tryLevelEvolve() {
        // Gen1 enum chains first
        if (species.canEvolveByLevel(level)) {
            return species.evolutionForLevel(level)
                    .filter(b -> !DisabledSpecies.isDisabled(b.into().id()))
                    .map(b -> OptionalEvo.of(evolveTo(b.into()), b.into()))
                    .orElseGet(() -> OptionalEvo.none(this));
        }
        // Full dex datapack evolutions (skip targets with no real model)
        return datapackLevelEvoTarget()
                .map(targetId -> {
                    SpeciesHandle th = SpeciesHandle.of(targetId);
                    OwnedMon next = evolveToId(th.id());
                    return OptionalEvo.of(next, th.asEnumOrFallback());
                })
                .orElseGet(() -> OptionalEvo.none(this));
    }

    private java.util.Optional<String> datapackLevelEvoTarget() {
        return EvolutionLookup.levelUpTarget(speciesId(), level)
                .filter(id -> !DisabledSpecies.isDisabled(id));
    }

    public OptionalEvo tryGemEvolve(EvolutionMethod gem) {
        return species.evolutionForGem(gem)
                .map(b -> OptionalEvo.of(evolveTo(b.into()), b.into()))
                .orElseGet(() -> OptionalEvo.none(this));
    }

    /**
     * Trade evolution via Link Cable (or completed player trade).
     * Gen1 enum TRADE chains + datapack {@code trade} variant via item map.
     */
    public OptionalEvo tryTradeEvolve() {
        // Gen1 TRADE branches (Kadabra, Machoke, Graveler, Haunter, …)
        for (EvoBranch b : species.evolutions()) {
            if (b.method() == EvolutionMethod.TRADE && !DisabledSpecies.isDisabled(b.into().id())) {
                return OptionalEvo.of(evolveTo(b.into()), b.into());
            }
        }
        // Datapack: treat as item_interact with link_cable if registered
        var itemPath = tryItemEvolve("link_cable");
        if (itemPath.evolved()) {
            return itemPath;
        }
        // Hardcoded modern trade lines (common Cobblemon targets)
        String into = switch (speciesId().toLowerCase(java.util.Locale.ROOT)) {
            case "kadabra" -> "alakazam";
            case "machoke" -> "machamp";
            case "graveler" -> "golem";
            case "haunter" -> "gengar";
            case "boldore" -> "gigalith";
            case "gurdurr" -> "conkeldurr";
            case "phantump" -> "trevenant";
            case "pumpkaboo" -> "gourgeist";
            case "shelmet" -> "accelgor";
            case "karrablast" -> "escavalier";
            default -> null;
        };
        if (into != null && !DisabledSpecies.isDisabled(into)) {
            return OptionalEvo.of(evolveToId(into), SpeciesHandle.of(into).asEnumOrFallback());
        }
        return OptionalEvo.none(this);
    }

    /** Stone / item evolution using datapack {@code item_interact} chains. */
    public OptionalEvo tryItemEvolve(String stoneItemId) {
        return ItemEvolutionLookup.evolveWith(speciesId(), stoneItemId)
                .filter(id -> !DisabledSpecies.isDisabled(id))
                .map(id -> {
                    OwnedMon next = evolveToId(id);
                    return OptionalEvo.of(next, SpeciesHandle.of(id).asEnumOrFallback());
                })
                .orElseGet(() -> OptionalEvo.none(this));
    }

    public int expToNextLevel() {
        if (level >= MAX_LEVEL) {
            return Integer.MAX_VALUE / 4;
        }
        return 20 + level * 15;
    }

    public ExpResult addExp(int amount) {
        if (amount <= 0 || level >= MAX_LEVEL) {
            return new ExpResult(this, false, false, List.of());
        }
        OwnedMon cur = withExp(exp + amount);
        boolean leveled = false;
        boolean evolved = false;
        List<MonMove> learned = new ArrayList<>();
        while (cur.exp() >= cur.expToNextLevel() && cur.level() < MAX_LEVEL) {
            int leftover = cur.exp() - cur.expToNextLevel();
            int newLevel = cur.level + 1;
            cur = cur.copy(cur.nickname, newLevel, 1, leftover, cur.uuid, cur.moveIds, cur.movePp,
                    cur.nature, cur.gender, cur.form, cur.ability, cur.sizeScale, cur.status, cur.heldItem);
            cur = cur.withHp(cur.maxHp());
            leveled = true;

            MonMove move = Learnsets.learnAt(cur.speciesId(), newLevel);
            if (move != null && !cur.knowsMove(move)) {
                cur = cur.learnMove(move);
                learned.add(move);
            }

            OptionalEvo evo = cur.tryLevelEvolve();
            if (evo.evolved()) {
                // N3: soft badge gate applied by caller when player known; here always apply
                cur = evo.mon();
                evolved = true;
            }
        }
        if (cur.level() >= MAX_LEVEL && cur.exp() > 0) {
            cur = cur.withExp(0);
        }
        return new ExpResult(cur, leveled, evolved, List.copyOf(learned));
    }

    public Component statusLine() {
        Component line = Component.empty()
                .append(displayName());
        if (isShiny()) {
            line = Component.empty().append(line)
                    .append(Component.literal(" ★").withStyle(net.minecraft.ChatFormatting.YELLOW));
        }
        return Component.empty()
                .append(line)
                .append(Component.literal(" " + gender.symbol()).withStyle(net.minecraft.ChatFormatting.AQUA))
                .append(" ")
                .append(Component.literal("Lv." + level).withStyle(net.minecraft.ChatFormatting.GOLD))
                .append(" ")
                .append(primaryType().displayName())
                .append(Component.literal(" HP " + hp + "/" + maxHp()).withStyle(
                        isFainted() ? net.minecraft.ChatFormatting.DARK_RED : net.minecraft.ChatFormatting.GREEN));
    }

    /**
     * Copy this mon with field overrides, always keeping the real datapack {@link #speciesId}.
     * Never rebuild from Gen1 {@link MonSpecies} alone — that was wiping non-Gen1 ids
     * (tyranitar → geodude, gulpin → ekans, etc.).
     */
    private OwnedMon copy(
            String nickname, int level, int hp, int exp, UUID uuid, List<String> moveIds,
            Nature nature, MonGender gender, MonForm form, Ability ability, float sizeScale,
            MonStatus status, String heldItem
    ) {
        return copy(nickname, level, hp, exp, uuid, moveIds, this.movePp, nature, gender, form, ability, sizeScale, status, heldItem, this.ivs, this.evs);
    }

    private OwnedMon copy(
            String nickname, int level, int hp, int exp, UUID uuid, List<String> moveIds, List<Integer> movePp,
            Nature nature, MonGender gender, MonForm form, Ability ability, float sizeScale,
            MonStatus status, String heldItem
    ) {
        return copy(nickname, level, hp, exp, uuid, moveIds, movePp, nature, gender, form, ability, sizeScale, status, heldItem, this.ivs, this.evs);
    }

    private OwnedMon copy(
            String nickname, int level, int hp, int exp, UUID uuid, List<String> moveIds, List<Integer> movePp,
            Nature nature, MonGender gender, MonForm form, Ability ability, float sizeScale,
            MonStatus status, String heldItem, int[] ivs, int[] evs
    ) {
        return new OwnedMon(
                speciesId(), species, nickname, level, hp, exp, uuid, moveIds, movePp,
                nature, gender, form, ability, sizeScale, status, heldItem, ivs, evs, this.mark
        );
    }

    public record OptionalEvo(OwnedMon mon, boolean evolved, MonSpecies into) {
        public static OptionalEvo none(OwnedMon mon) {
            return new OptionalEvo(mon, false, null);
        }

        public static OptionalEvo of(OwnedMon mon, MonSpecies into) {
            return new OptionalEvo(mon, true, into);
        }
    }

    public record ExpResult(OwnedMon mon, boolean leveled, boolean evolved, List<MonMove> learnedMoves) {
        public ExpResult(OwnedMon mon, boolean leveled, boolean evolved) {
            this(mon, leveled, evolved, List.of());
        }
    }
}

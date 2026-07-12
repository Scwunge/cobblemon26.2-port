package com.cobblemon.mod.species;

import java.util.Locale;
import java.util.Optional;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * Universal species identity: works for Gen1 {@link MonSpecies} enum entries
 * and any datapack species loaded into {@link SpeciesRegistry}.
 * <p>
 * Prefer this over bare {@link MonSpecies} when reading string IDs from spawns/NBT.
 */
public final class SpeciesHandle {
    private final String id;
    private final MonSpecies enumSpecies; // nullable when beyond Gen1
    private final SpeciesRegistry.SpeciesData data; // nullable if missing from pack

    private SpeciesHandle(String id, MonSpecies enumSpecies, SpeciesRegistry.SpeciesData data) {
        this.id = id;
        this.enumSpecies = enumSpecies;
        this.data = data;
    }

    public static SpeciesHandle of(String rawId) {
        if (rawId == null || rawId.isBlank()) {
            return of(MonSpecies.RATTATA);
        }
        String id = rawId.toLowerCase(Locale.ROOT).trim();
        // strip form suffixes like "rattata alolan=true"
        id = id.split("[\\s|]")[0];
        Optional<MonSpecies> en = MonSpecies.byId(id);
        SpeciesRegistry.SpeciesData data = SpeciesRegistry.get(id).orElse(null);
        if (en.isEmpty() && data == null) {
            // try enum alias map fallback
            MonSpecies def = MonSpecies.byIdOrDefault(id);
            return new SpeciesHandle(def.id(), def, SpeciesRegistry.get(def.id()).orElse(null));
        }
        return new SpeciesHandle(id, en.orElse(null), data);
    }

    public static SpeciesHandle of(MonSpecies species) {
        if (species == null) {
            return of(MonSpecies.RATTATA);
        }
        return new SpeciesHandle(species.id(), species, SpeciesRegistry.get(species.id()).orElse(null));
    }

    public String id() {
        return id;
    }

    /** Gen1 runtime enum if available; null for pure datapack species. */
    public Optional<MonSpecies> asEnum() {
        return Optional.ofNullable(enumSpecies);
    }

    /** Prefer enum for gameplay systems that still require MonSpecies. */
    public MonSpecies asEnumOrFallback() {
        if (enumSpecies != null) {
            return enumSpecies;
        }
        // Fall back to a typed stand-in of same primary element for battle math
        MonElement el = primaryType();
        for (MonSpecies s : MonSpecies.values()) {
            if (s.element() == el) {
                return s;
            }
        }
        return MonSpecies.RATTATA;
    }

    public boolean isImplementedRuntime() {
        return enumSpecies != null;
    }

    public MutableComponent displayName() {
        if (data != null && data.name() != null && !data.name().isBlank()) {
            return Component.literal(data.name());
        }
        if (enumSpecies != null) {
            return enumSpecies.displayName();
        }
        // Title-case the id
        String pretty = id.replace('_', ' ');
        if (!pretty.isEmpty()) {
            pretty = Character.toUpperCase(pretty.charAt(0)) + pretty.substring(1);
        }
        return Component.literal(pretty);
    }

    public MonElement primaryType() {
        if (data != null) {
            return data.primaryElement();
        }
        if (enumSpecies != null) {
            return enumSpecies.element();
        }
        return MonElement.NORMAL;
    }

    public Optional<MonElement> secondaryType() {
        if (data != null) {
            return data.secondaryElement();
        }
        if (enumSpecies != null) {
            return enumSpecies.secondaryType();
        }
        return Optional.empty();
    }

    public float maleRatio() {
        if (data != null) {
            return data.maleRatio();
        }
        if (enumSpecies != null) {
            return enumSpecies.maleRatio();
        }
        return 0.5f;
    }

    public int catchRate() {
        if (data != null) {
            return data.catchRate();
        }
        CobblemonBaseStats.Six six = CobblemonBaseStats.ofId(id);
        return six != null ? six.catchRate() : 45;
    }

    public CobblemonBaseStats.Six baseStats() {
        CobblemonBaseStats.Six fromPack = CobblemonBaseStats.ofId(id);
        if (fromPack != null) {
            return fromPack;
        }
        if (enumSpecies != null) {
            return new CobblemonBaseStats.Six(
                    enumSpecies.baseStat(StatId.HP),
                    enumSpecies.baseStat(StatId.ATK),
                    enumSpecies.baseStat(StatId.DEF),
                    enumSpecies.baseStat(StatId.SP_ATK),
                    enumSpecies.baseStat(StatId.SP_DEF),
                    enumSpecies.baseStat(StatId.SPEED),
                    45
            );
        }
        return new CobblemonBaseStats.Six(50, 50, 50, 50, 50, 50, 45);
    }

    public int stage() {
        if (enumSpecies != null) {
            return enumSpecies.stage();
        }
        if (data != null && data.labels() != null) {
            // crude: legendary labels → stage 3
            for (String l : data.labels()) {
                if (l.contains("legendary") || l.contains("mythical")) {
                    return 3;
                }
                if (l.contains("pseudo")) {
                    return 3;
                }
            }
        }
        int dex = data != null ? data.nationalPokedexNumber() : 0;
        // default mid
        return dex > 0 && dex % 3 == 0 ? 3 : (dex % 3 == 2 ? 2 : 1);
    }

    public float sizeScale() {
        if (enumSpecies != null) {
            return enumSpecies.sizeScale();
        }
        if (data != null) {
            return Math.max(0.5f, Math.min(2.0f, data.baseScale()));
        }
        return 1.0f;
    }

    /**
     * Whether this species is large enough / configured to be rideable.
     * Mirrors official Cobblemon: only mons with riding seats (approx. height ≥ 1.0m
     * and weight ≥ 20kg). Small stage-1 mons like Charmander are not rideable.
     * <p>
     * Flying/Water types get a slightly softer size floor so mid-stage mounts
     * (e.g. large birds, Lapras-scale water) qualify more reliably.
     */
    public boolean isRideable() {
        int heightDm;
        int weightHg;
        if (data != null) {
            heightDm = data.height();
            weightHg = data.weight();
        } else if (enumSpecies != null) {
            heightDm = Math.max(1, Math.round(enumSpecies.heightM() * 10f));
            weightHg = Math.max(1, Math.round(enumSpecies.weightKg() * 10f));
        } else {
            return false;
        }
        boolean flyer = primaryType() == MonElement.FLYING
                || secondaryType().orElse(null) == MonElement.FLYING
                || primaryType() == MonElement.DRAGON;
        boolean swimmer = primaryType() == MonElement.WATER
                || secondaryType().orElse(null) == MonElement.WATER;
        // Charmander h=6 → false; Charizard h=17 → true; Rhyhorn h=10 → true
        if (flyer || swimmer) {
            return heightDm >= 9 && weightHg >= 150;
        }
        return heightDm >= 10 && weightHg >= 200;
    }

    /** Weight in hectograms (species JSON {@code weight}). */
    public int weightHg() {
        if (data != null) {
            return data.weight();
        }
        if (enumSpecies != null) {
            return Math.max(1, Math.round(enumSpecies.weightKg() * 10f));
        }
        return 100;
    }

    /** Height in decimetres (species JSON {@code height}). */
    public int heightDm() {
        if (data != null) {
            return data.height();
        }
        if (enumSpecies != null) {
            return Math.max(1, Math.round(enumSpecies.heightM() * 10f));
        }
        return 10;
    }

    /** National Pokédex number (0 if unknown). */
    public int nationalNumber() {
        if (data != null) {
            return data.nationalPokedexNumber();
        }
        if (enumSpecies != null) {
            return enumSpecies.ordinal() + 1;
        }
        return 0;
    }

    public Ability defaultAbility() {
        if (enumSpecies != null) {
            return enumSpecies.defaultAbility();
        }
        if (data != null && data.abilities() != null && !data.abilities().isEmpty()) {
            String raw = data.abilities().get(0);
            // strip hidden prefix h:
            if (raw.startsWith("h:")) {
                raw = raw.substring(2);
            }
            return Ability.byId(raw.replace("-", "").replace("_", ""));
        }
        return Ability.NONE;
    }

    public int nationalDex() {
        if (data != null) {
            return data.nationalPokedexNumber();
        }
        if (enumSpecies != null) {
            // enum order isn't national dex; use registry if present
            return SpeciesRegistry.get(id).map(SpeciesRegistry.SpeciesData::nationalPokedexNumber).orElse(0);
        }
        return 0;
    }

    @Override
    public String toString() {
        return "SpeciesHandle{" + id + "}";
    }
}

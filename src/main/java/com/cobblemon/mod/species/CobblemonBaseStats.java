package com.cobblemon.mod.species;

/**
 * AUTO-GENERATED from Cobblemon 1.7.3 generation1 species JSON.
 * Full 6-stat base stats + catch rate.
 */
public final class CobblemonBaseStats {
    private CobblemonBaseStats() {}

    public record Six(int hp, int atk, int def, int spa, int spd, int spe, int catchRate) {}

    public static Six of(MonSpecies species) {
        if (species == null) return null;
        return ofId(species.id());
    }

    public static Six ofId(String id) {
        if (id == null) return null;
        // Prefer datapack SpeciesRegistry (full national dex from Cobblemon 1.7.3)
        Six fromPack = SpeciesRegistry.statsOrNull(id);
        if (fromPack != null) {
            return fromPack;
        }
        return switch (id) {
            case "abra" -> new Six(25, 20, 15, 105, 55, 90, 200);
            case "aerodactyl" -> new Six(80, 105, 65, 60, 75, 130, 45);
            case "alakazam" -> new Six(55, 50, 45, 135, 95, 120, 50);
            case "arbok" -> new Six(60, 95, 69, 65, 79, 80, 90);
            case "arcanine" -> new Six(90, 110, 80, 100, 80, 95, 75);
            case "articuno" -> new Six(90, 85, 100, 95, 125, 85, 3);
            case "beedrill" -> new Six(65, 90, 40, 45, 80, 75, 45);
            case "bellsprout" -> new Six(50, 75, 35, 70, 30, 40, 255);
            case "blastoise" -> new Six(79, 83, 100, 85, 105, 78, 45);
            case "bulbasaur" -> new Six(45, 49, 49, 65, 65, 45, 45);
            case "butterfree" -> new Six(60, 45, 50, 90, 80, 70, 45);
            case "caterpie" -> new Six(45, 30, 35, 20, 20, 45, 255);
            case "chansey" -> new Six(250, 5, 5, 35, 105, 50, 30);
            case "charizard" -> new Six(78, 84, 78, 109, 85, 100, 45);
            case "charmander" -> new Six(39, 52, 43, 60, 50, 65, 45);
            case "charmeleon" -> new Six(58, 64, 58, 80, 65, 80, 45);
            case "clefable" -> new Six(95, 70, 73, 95, 90, 60, 25);
            case "clefairy" -> new Six(70, 45, 48, 60, 65, 35, 150);
            case "cloyster" -> new Six(50, 95, 180, 85, 45, 70, 60);
            case "cubone" -> new Six(50, 50, 95, 40, 50, 35, 190);
            case "dewgong" -> new Six(90, 70, 80, 70, 95, 70, 75);
            case "diglett" -> new Six(10, 55, 25, 35, 45, 95, 255);
            case "ditto" -> new Six(48, 48, 48, 48, 48, 48, 35);
            case "dodrio" -> new Six(60, 110, 70, 60, 60, 110, 45);
            case "doduo" -> new Six(35, 85, 45, 35, 35, 75, 190);
            case "dragonair" -> new Six(61, 84, 65, 70, 70, 70, 45);
            case "dragonite" -> new Six(91, 134, 95, 100, 100, 80, 45);
            case "dratini" -> new Six(41, 64, 45, 50, 50, 50, 45);
            case "drowzee" -> new Six(60, 48, 45, 43, 90, 42, 190);
            case "dugtrio" -> new Six(35, 100, 50, 50, 70, 120, 50);
            case "eevee" -> new Six(55, 55, 50, 45, 65, 55, 45);
            case "ekans" -> new Six(35, 60, 44, 40, 54, 55, 255);
            case "electabuzz" -> new Six(65, 83, 57, 95, 85, 105, 45);
            case "electrode" -> new Six(60, 50, 70, 80, 80, 150, 60);
            case "exeggcute" -> new Six(60, 40, 80, 60, 45, 40, 90);
            case "exeggutor" -> new Six(95, 95, 85, 125, 75, 55, 45);
            case "farfetchd" -> new Six(52, 90, 55, 58, 62, 60, 45);
            case "fearow" -> new Six(65, 90, 65, 61, 61, 100, 90);
            case "flareon" -> new Six(65, 130, 60, 95, 110, 65, 45);
            case "gastly" -> new Six(30, 35, 30, 100, 35, 80, 190);
            case "gengar" -> new Six(60, 65, 60, 130, 75, 110, 45);
            case "geodude" -> new Six(40, 80, 100, 30, 30, 20, 255);
            case "gloom" -> new Six(60, 65, 70, 85, 75, 40, 120);
            case "golbat" -> new Six(75, 80, 70, 65, 75, 90, 90);
            case "goldeen" -> new Six(45, 67, 60, 35, 50, 63, 225);
            case "golduck" -> new Six(80, 82, 78, 95, 80, 85, 75);
            case "golem" -> new Six(80, 120, 130, 55, 65, 45, 45);
            case "graveler" -> new Six(55, 95, 115, 45, 45, 35, 120);
            case "grimer" -> new Six(80, 80, 50, 40, 50, 25, 190);
            case "growlithe" -> new Six(55, 70, 45, 70, 50, 60, 190);
            case "gyarados" -> new Six(95, 125, 79, 60, 100, 81, 45);
            case "haunter" -> new Six(45, 50, 45, 115, 55, 95, 90);
            case "hitmonchan" -> new Six(50, 105, 79, 35, 110, 76, 45);
            case "hitmonlee" -> new Six(50, 120, 53, 35, 110, 87, 45);
            case "horsea" -> new Six(30, 40, 70, 70, 25, 60, 225);
            case "hypno" -> new Six(85, 73, 70, 73, 115, 67, 75);
            case "ivysaur" -> new Six(60, 62, 63, 80, 80, 60, 45);
            case "jigglypuff" -> new Six(115, 45, 20, 45, 25, 20, 170);
            case "jolteon" -> new Six(65, 65, 60, 110, 95, 130, 45);
            case "jynx" -> new Six(65, 50, 35, 115, 95, 95, 45);
            case "kabuto" -> new Six(30, 80, 90, 55, 45, 55, 45);
            case "kabutops" -> new Six(60, 115, 105, 65, 70, 80, 45);
            case "kadabra" -> new Six(40, 35, 30, 120, 70, 105, 100);
            case "kakuna" -> new Six(45, 25, 50, 25, 25, 35, 120);
            case "kangaskhan" -> new Six(105, 95, 80, 40, 80, 90, 45);
            case "kingler" -> new Six(55, 130, 115, 50, 50, 75, 60);
            case "koffing" -> new Six(40, 65, 95, 60, 45, 35, 190);
            case "krabby" -> new Six(30, 105, 90, 25, 25, 50, 225);
            case "lapras" -> new Six(130, 85, 80, 85, 95, 60, 45);
            case "lickitung" -> new Six(90, 55, 75, 60, 75, 30, 45);
            case "machamp" -> new Six(90, 130, 80, 65, 85, 55, 45);
            case "machoke" -> new Six(80, 100, 70, 50, 60, 45, 90);
            case "machop" -> new Six(70, 80, 50, 35, 35, 35, 180);
            case "magikarp" -> new Six(20, 10, 55, 15, 20, 80, 255);
            case "magmar" -> new Six(65, 95, 57, 100, 85, 93, 45);
            case "magnemite" -> new Six(25, 35, 70, 95, 55, 45, 190);
            case "magneton" -> new Six(50, 60, 95, 120, 70, 70, 60);
            case "mankey" -> new Six(40, 80, 35, 35, 45, 70, 190);
            case "marowak" -> new Six(60, 80, 110, 50, 80, 45, 75);
            case "meowth" -> new Six(40, 45, 35, 40, 40, 90, 255);
            case "metapod" -> new Six(50, 20, 55, 25, 25, 30, 120);
            case "mew" -> new Six(100, 100, 100, 100, 100, 100, 45);
            case "mewtwo" -> new Six(106, 110, 90, 154, 90, 130, 3);
            case "moltres" -> new Six(90, 100, 90, 125, 85, 90, 3);
            case "mr_mime" -> new Six(40, 45, 65, 100, 120, 90, 45);
            case "muk" -> new Six(105, 105, 75, 65, 100, 50, 75);
            case "nidoking" -> new Six(81, 102, 77, 85, 75, 85, 45);
            case "nidoqueen" -> new Six(90, 92, 87, 75, 85, 76, 45);
            case "nidoran_f" -> new Six(55, 47, 52, 40, 40, 41, 235);
            case "nidoran_m" -> new Six(46, 57, 40, 40, 40, 50, 235);
            case "nidorina" -> new Six(70, 62, 67, 55, 55, 56, 120);
            case "nidorino" -> new Six(61, 72, 57, 55, 55, 65, 120);
            case "ninetales" -> new Six(73, 76, 75, 81, 100, 100, 75);
            case "oddish" -> new Six(45, 50, 55, 75, 65, 30, 255);
            case "omanyte" -> new Six(35, 40, 100, 90, 55, 35, 45);
            case "omastar" -> new Six(70, 60, 125, 115, 70, 55, 45);
            case "onix" -> new Six(35, 45, 160, 30, 45, 70, 45);
            case "paras" -> new Six(35, 70, 55, 45, 55, 25, 190);
            case "parasect" -> new Six(60, 95, 80, 60, 80, 30, 75);
            case "persian" -> new Six(65, 70, 60, 65, 65, 115, 90);
            case "pidgeot" -> new Six(83, 80, 75, 70, 70, 101, 45);
            case "pidgeotto" -> new Six(63, 60, 55, 50, 50, 71, 120);
            case "pidgey" -> new Six(40, 45, 40, 35, 35, 56, 255);
            case "pikachu" -> new Six(35, 55, 40, 50, 50, 90, 190);
            case "pinsir" -> new Six(65, 125, 100, 55, 70, 85, 45);
            case "poliwag" -> new Six(40, 50, 40, 40, 40, 90, 255);
            case "poliwhirl" -> new Six(65, 65, 65, 50, 50, 90, 120);
            case "poliwrath" -> new Six(90, 95, 95, 70, 90, 70, 45);
            case "ponyta" -> new Six(50, 85, 55, 65, 65, 90, 190);
            case "porygon" -> new Six(65, 60, 70, 85, 75, 40, 45);
            case "primeape" -> new Six(65, 105, 60, 60, 70, 95, 75);
            case "psyduck" -> new Six(50, 52, 48, 65, 50, 55, 190);
            case "raichu" -> new Six(60, 90, 55, 90, 80, 110, 75);
            case "rapidash" -> new Six(65, 100, 70, 80, 80, 105, 60);
            case "raticate" -> new Six(55, 81, 60, 50, 70, 97, 127);
            case "rattata" -> new Six(30, 56, 35, 25, 35, 72, 255);
            case "rhydon" -> new Six(105, 130, 120, 45, 45, 40, 60);
            case "rhyhorn" -> new Six(80, 85, 95, 30, 30, 25, 120);
            case "sandshrew" -> new Six(50, 75, 85, 20, 30, 40, 255);
            case "sandslash" -> new Six(75, 100, 110, 45, 55, 65, 90);
            case "scyther" -> new Six(70, 110, 80, 55, 80, 105, 45);
            case "seadra" -> new Six(55, 65, 95, 95, 45, 85, 75);
            case "seaking" -> new Six(80, 92, 65, 65, 80, 68, 60);
            case "seel" -> new Six(65, 45, 55, 45, 70, 45, 190);
            case "shellder" -> new Six(30, 65, 100, 45, 25, 40, 190);
            case "slowbro" -> new Six(95, 75, 110, 100, 80, 30, 75);
            case "slowpoke" -> new Six(90, 65, 65, 40, 40, 15, 190);
            case "snorlax" -> new Six(160, 110, 65, 65, 110, 30, 25);
            case "spearow" -> new Six(40, 60, 30, 31, 31, 70, 255);
            case "squirtle" -> new Six(44, 48, 65, 50, 64, 43, 45);
            case "starmie" -> new Six(60, 75, 85, 100, 85, 115, 60);
            case "staryu" -> new Six(30, 45, 55, 70, 55, 85, 225);
            case "tangela" -> new Six(65, 55, 115, 100, 40, 60, 45);
            case "tauros" -> new Six(75, 100, 95, 40, 70, 110, 45);
            case "tentacool" -> new Six(40, 40, 35, 50, 100, 70, 190);
            case "tentacruel" -> new Six(80, 70, 65, 80, 120, 100, 60);
            case "vaporeon" -> new Six(130, 65, 60, 110, 95, 65, 45);
            case "venomoth" -> new Six(70, 65, 60, 90, 75, 90, 75);
            case "venonat" -> new Six(60, 55, 50, 40, 55, 45, 190);
            case "venusaur" -> new Six(80, 82, 83, 100, 100, 80, 45);
            case "victreebel" -> new Six(80, 105, 65, 100, 70, 70, 45);
            case "vileplume" -> new Six(75, 80, 85, 110, 90, 50, 45);
            case "voltorb" -> new Six(40, 30, 50, 55, 55, 100, 190);
            case "vulpix" -> new Six(38, 41, 40, 50, 65, 65, 190);
            case "wartortle" -> new Six(59, 63, 80, 65, 80, 58, 45);
            case "weedle" -> new Six(40, 35, 30, 20, 20, 50, 255);
            case "weepinbell" -> new Six(65, 90, 50, 85, 45, 55, 120);
            case "weezing" -> new Six(65, 90, 120, 85, 70, 60, 60);
            case "wigglytuff" -> new Six(140, 70, 45, 85, 50, 45, 50);
            case "zapdos" -> new Six(90, 90, 85, 125, 90, 100, 3);
            case "zubat" -> new Six(40, 45, 35, 30, 40, 55, 255);
            default -> null;
        };
    }
}

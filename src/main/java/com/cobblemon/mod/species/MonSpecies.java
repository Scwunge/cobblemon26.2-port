package com.cobblemon.mod.species;

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
    BULBASAUR("bulbasaur", MonElement.GRASS, BodyShape.QUAD, 45, 57, 0x52B788, 0x1B4332, true, 1,
            "Seed", "gen1, starter", "overgrow", 0.7f, 6.9f,
            "Bulbasaur, the Seed Pokémon. National Dex #1."),
    IVYSAUR("ivysaur", MonElement.GRASS, BodyShape.QUAD, 60, 71, 0x52B788, 0x1B4332, false, 2,
            "Seed", "gen1", "overgrow", 1.0f, 13.0f,
            "Ivysaur, the Seed Pokémon. National Dex #2."),
    VENUSAUR("venusaur", MonElement.GRASS, BodyShape.BIPED, 80, 91, 0x52B788, 0x1B4332, false, 3,
            "Seed", "gen1", "overgrow", 2.0f, 100.0f,
            "Venusaur, the Seed Pokémon. National Dex #3."),
    CHARMANDER("charmander", MonElement.FIRE, BodyShape.QUAD, 39, 56, 0xE85D04, 0x370617, true, 1,
            "Lizard", "gen1, starter", "blaze", 0.6f, 8.5f,
            "Charmander, the Lizard Pokémon. National Dex #4."),
    CHARMELEON("charmeleon", MonElement.FIRE, BodyShape.QUAD, 58, 72, 0xE85D04, 0x370617, false, 2,
            "Flame", "gen1", "blaze", 1.1f, 19.0f,
            "Charmeleon, the Flame Pokémon. National Dex #5."),
    CHARIZARD("charizard", MonElement.FIRE, BodyShape.AVIAN, 78, 97, 0xE85D04, 0x370617, false, 3,
            "Flame", "gen1", "blaze", 1.7f, 90.5f,
            "Charizard, the Flame Pokémon. National Dex #6."),
    SQUIRTLE("squirtle", MonElement.WATER, BodyShape.AQUATIC, 44, 49, 0x4CC9F0, 0x023E8A, true, 1,
            "Tiny Turtle", "gen1, starter", "torrent", 0.5f, 9.0f,
            "Squirtle, the Tiny Turtle Pokémon. National Dex #7."),
    WARTORTLE("wartortle", MonElement.WATER, BodyShape.AQUATIC, 59, 64, 0x4CC9F0, 0x023E8A, false, 2,
            "Turtle", "gen1", "torrent", 1.0f, 22.5f,
            "Wartortle, the Turtle Pokémon. National Dex #8."),
    BLASTOISE("blastoise", MonElement.WATER, BodyShape.AQUATIC, 79, 84, 0x4CC9F0, 0x023E8A, false, 3,
            "Shellfish", "gen1", "torrent", 1.6f, 85.5f,
            "Blastoise, the Shellfish Pokémon. National Dex #9."),
    CATERPIE("caterpie", MonElement.BUG, BodyShape.INSECTOID, 45, 25, 0xA7C957, 0x386641, false, 1,
            "Worm", "gen1", "shielddust", 0.3f, 2.9f,
            "Caterpie, the Worm Pokémon. National Dex #10."),
    METAPOD("metapod", MonElement.BUG, BodyShape.INSECTOID, 50, 23, 0xA7C957, 0x386641, false, 2,
            "Cocoon", "gen1", "shedskin", 0.7f, 9.9f,
            "Metapod, the Cocoon Pokémon. National Dex #11."),
    BUTTERFREE("butterfree", MonElement.BUG, BodyShape.INSECTOID, 60, 68, 0xA7C957, 0x386641, false, 3,
            "Butterfly", "gen1", "compoundeyes", 1.1f, 32.0f,
            "Butterfree, the Butterfly Pokémon. National Dex #12."),
    WEEDLE("weedle", MonElement.BUG, BodyShape.INSECTOID, 40, 28, 0xA7C957, 0x386641, false, 1,
            "Hairy Bug", "gen1", "shielddust", 0.3f, 3.2f,
            "Weedle, the Hairy Bug Pokémon. National Dex #13."),
    KAKUNA("kakuna", MonElement.BUG, BodyShape.INSECTOID, 45, 25, 0xA7C957, 0x386641, false, 2,
            "Cocoon", "gen1", "shedskin", 0.6f, 10.0f,
            "Kakuna, the Cocoon Pokémon. National Dex #14."),
    BEEDRILL("beedrill", MonElement.BUG, BodyShape.INSECTOID, 65, 68, 0xA7C957, 0x386641, false, 3,
            "Poison Bee", "gen1", "swarm", 1.0f, 29.5f,
            "Beedrill, the Poison Bee Pokémon. National Dex #15."),
    PIDGEY("pidgey", MonElement.NORMAL, BodyShape.AVIAN, 40, 40, 0xDEE2E6, 0x6C757D, false, 1,
            "Tiny Bird", "gen1", "keeneye", 0.3f, 1.8f,
            "Pidgey, the Tiny Bird Pokémon. National Dex #16."),
    PIDGEOTTO("pidgeotto", MonElement.NORMAL, BodyShape.AVIAN, 63, 55, 0xDEE2E6, 0x6C757D, false, 2,
            "Bird", "gen1", "keeneye", 1.1f, 30.0f,
            "Pidgeotto, the Bird Pokémon. National Dex #17."),
    PIDGEOT("pidgeot", MonElement.NORMAL, BodyShape.AVIAN, 83, 75, 0xDEE2E6, 0x6C757D, false, 3,
            "Bird", "gen1", "keeneye", 1.5f, 39.5f,
            "Pidgeot, the Bird Pokémon. National Dex #18."),
    RATTATA("rattata", MonElement.NORMAL, BodyShape.QUAD, 30, 41, 0xDEE2E6, 0x6C757D, false, 1,
            "Mouse", "gen1, kantonian_form", "runaway", 0.3f, 3.5f,
            "Rattata, the Mouse Pokémon. National Dex #19."),
    RATICATE("raticate", MonElement.NORMAL, BodyShape.QUAD, 55, 66, 0xDEE2E6, 0x6C757D, false, 2,
            "Mouse", "gen1, kantonian_form", "runaway", 0.7f, 18.5f,
            "Raticate, the Mouse Pokémon. National Dex #20."),
    SPEAROW("spearow", MonElement.NORMAL, BodyShape.AVIAN, 40, 46, 0xDEE2E6, 0x6C757D, false, 1,
            "Tiny Bird", "gen1", "keeneye", 0.3f, 2.0f,
            "Spearow, the Tiny Bird Pokémon. National Dex #21."),
    FEAROW("fearow", MonElement.NORMAL, BodyShape.AVIAN, 65, 76, 0xDEE2E6, 0x6C757D, false, 2,
            "Beak", "gen1", "keeneye", 1.2f, 38.0f,
            "Fearow, the Beak Pokémon. National Dex #22."),
    EKANS("ekans", MonElement.POISON, BodyShape.SERPENT, 35, 50, 0x9B5DE5, 0x5A189A, false, 1,
            "Snake", "gen1", "intimidate", 2.0f, 6.9f,
            "Ekans, the Snake Pokémon. National Dex #23."),
    ARBOK("arbok", MonElement.POISON, BodyShape.QUAD, 60, 80, 0x9B5DE5, 0x5A189A, false, 2,
            "Cobra", "gen1", "intimidate", 3.5f, 65.0f,
            "Arbok, the Cobra Pokémon. National Dex #24."),
    PIKACHU("pikachu", MonElement.ELECTRIC, BodyShape.QUAD, 35, 53, 0xFEE440, 0x7B2CBF, false, 1,
            "Mouse", "gen1, kantonian_form", "static", 0.4f, 6.0f,
            "Pikachu, the Mouse Pokémon. National Dex #25."),
    RAICHU("raichu", MonElement.ELECTRIC, BodyShape.QUAD, 60, 90, 0xFEE440, 0x7B2CBF, false, 2,
            "Mouse", "gen1, kantonian_form", "static", 0.8f, 30.0f,
            "Raichu, the Mouse Pokémon. National Dex #26."),
    SANDSHREW("sandshrew", MonElement.GROUND, BodyShape.QUAD, 50, 48, 0xD4A373, 0x6F4518, false, 1,
            "Mouse", "gen1, kantonian_form", "sandveil", 0.6f, 12.0f,
            "Sandshrew, the Mouse Pokémon. National Dex #27."),
    SANDSLASH("sandslash", MonElement.GROUND, BodyShape.QUAD, 75, 73, 0xD4A373, 0x6F4518, false, 2,
            "Mouse", "gen1, kantonian_form", "sandveil", 1.0f, 29.5f,
            "Sandslash, the Mouse Pokémon. National Dex #28."),
    NIDORAN_F("nidoran_f", MonElement.POISON, BodyShape.QUAD, 55, 44, 0x9B5DE5, 0x5A189A, false, 1,
            "Poison Pin", "gen1", "poisonpoint", 0.4f, 7.0f,
            "Nidoran-F, the Poison Pin Pokémon. National Dex #29."),
    NIDORINA("nidorina", MonElement.POISON, BodyShape.QUAD, 70, 59, 0x9B5DE5, 0x5A189A, false, 2,
            "Poison Pin", "gen1", "poisonpoint", 0.8f, 20.0f,
            "Nidorina, the Poison Pin Pokémon. National Dex #30."),
    NIDOQUEEN("nidoqueen", MonElement.POISON, BodyShape.QUAD, 90, 84, 0x9B5DE5, 0x5A189A, false, 3,
            "Drill", "gen1", "poisonpoint", 1.3f, 60.0f,
            "Nidoqueen, the Drill Pokémon. National Dex #31."),
    NIDORAN_M("nidoran_m", MonElement.POISON, BodyShape.QUAD, 46, 49, 0x9B5DE5, 0x5A189A, false, 1,
            "Poison Pin", "gen1", "poisonpoint", 0.5f, 9.0f,
            "Nidoran-M, the Poison Pin Pokémon. National Dex #32."),
    NIDORINO("nidorino", MonElement.POISON, BodyShape.QUAD, 61, 64, 0x9B5DE5, 0x5A189A, false, 2,
            "Poison Pin", "gen1", "poisonpoint", 0.9f, 19.5f,
            "Nidorino, the Poison Pin Pokémon. National Dex #33."),
    NIDOKING("nidoking", MonElement.POISON, BodyShape.QUAD, 81, 94, 0x9B5DE5, 0x5A189A, false, 3,
            "Drill", "gen1", "poisonpoint", 1.4f, 62.0f,
            "Nidoking, the Drill Pokémon. National Dex #34."),
    CLEFAIRY("clefairy", MonElement.FAIRY, BodyShape.FEY, 70, 53, 0xFFAFCC, 0xC77DFF, false, 1,
            "Fairy", "gen1", "cutecharm", 0.6f, 7.5f,
            "Clefairy, the Fairy Pokémon. National Dex #35."),
    CLEFABLE("clefable", MonElement.FAIRY, BodyShape.FEY, 95, 83, 0xFFAFCC, 0xC77DFF, false, 2,
            "Fairy", "gen1", "cutecharm", 1.3f, 40.0f,
            "Clefable, the Fairy Pokémon. National Dex #36."),
    VULPIX("vulpix", MonElement.FIRE, BodyShape.QUAD, 38, 46, 0xE85D04, 0x370617, false, 1,
            "Fox", "gen1, kantonian_form", "flashfire", 0.6f, 9.9f,
            "Vulpix, the Fox Pokémon. National Dex #37."),
    NINETALES("ninetales", MonElement.FIRE, BodyShape.QUAD, 73, 79, 0xE85D04, 0x370617, false, 2,
            "Fox", "gen1, kantonian_form", "flashfire", 1.1f, 19.9f,
            "Ninetales, the Fox Pokémon. National Dex #38."),
    JIGGLYPUFF("jigglypuff", MonElement.NORMAL, BodyShape.FEY, 115, 45, 0xDEE2E6, 0x6C757D, false, 1,
            "Balloon", "gen1", "cutecharm", 0.5f, 5.5f,
            "Jigglypuff, the Balloon Pokémon. National Dex #39."),
    WIGGLYTUFF("wigglytuff", MonElement.NORMAL, BodyShape.FEY, 140, 78, 0xDEE2E6, 0x6C757D, false, 2,
            "Balloon", "gen1", "cutecharm", 1.0f, 12.0f,
            "Wigglytuff, the Balloon Pokémon. National Dex #40."),
    ZUBAT("zubat", MonElement.POISON, BodyShape.AVIAN, 40, 38, 0x9B5DE5, 0x5A189A, false, 1,
            "Bat", "gen1", "innerfocus", 0.8f, 7.5f,
            "Zubat, the Bat Pokémon. National Dex #41."),
    GOLBAT("golbat", MonElement.POISON, BodyShape.AVIAN, 75, 73, 0x9B5DE5, 0x5A189A, false, 2,
            "Bat", "gen1", "innerfocus", 1.6f, 55.0f,
            "Golbat, the Bat Pokémon. National Dex #42."),
    ODDISH("oddish", MonElement.GRASS, BodyShape.QUAD, 45, 63, 0x52B788, 0x1B4332, false, 1,
            "Weed", "gen1", "chlorophyll", 0.5f, 5.4f,
            "Oddish, the Weed Pokémon. National Dex #43."),
    GLOOM("gloom", MonElement.GRASS, BodyShape.QUAD, 60, 75, 0x52B788, 0x1B4332, false, 2,
            "Weed", "gen1", "chlorophyll", 0.8f, 8.6f,
            "Gloom, the Weed Pokémon. National Dex #44."),
    VILEPLUME("vileplume", MonElement.GRASS, BodyShape.QUAD, 75, 95, 0x52B788, 0x1B4332, false, 3,
            "Flower", "gen1", "chlorophyll", 1.2f, 18.6f,
            "Vileplume, the Flower Pokémon. National Dex #45."),
    PARAS("paras", MonElement.BUG, BodyShape.INSECTOID, 35, 58, 0xA7C957, 0x386641, false, 1,
            "Mushroom", "gen1", "effectspore", 0.3f, 5.4f,
            "Paras, the Mushroom Pokémon. National Dex #46."),
    PARASECT("parasect", MonElement.BUG, BodyShape.INSECTOID, 60, 78, 0xA7C957, 0x386641, false, 2,
            "Mushroom", "gen1", "effectspore", 1.0f, 29.5f,
            "Parasect, the Mushroom Pokémon. National Dex #47."),
    VENONAT("venonat", MonElement.BUG, BodyShape.INSECTOID, 60, 48, 0xA7C957, 0x386641, false, 1,
            "Insect", "gen1", "compoundeyes", 1.0f, 30.0f,
            "Venonat, the Insect Pokémon. National Dex #48."),
    VENOMOTH("venomoth", MonElement.BUG, BodyShape.INSECTOID, 70, 78, 0xA7C957, 0x386641, false, 2,
            "Poison Moth", "gen1", "shielddust", 1.5f, 12.5f,
            "Venomoth, the Poison Moth Pokémon. National Dex #49."),
    DIGLETT("diglett", MonElement.GROUND, BodyShape.QUAD, 10, 45, 0xD4A373, 0x6F4518, false, 1,
            "Mole", "gen1, kantonian_form", "sandveil", 0.2f, 0.8f,
            "Diglett, the Mole Pokémon. National Dex #50."),
    DUGTRIO("dugtrio", MonElement.GROUND, BodyShape.QUAD, 35, 75, 0xD4A373, 0x6F4518, false, 2,
            "Mole", "gen1, kantonian_form", "sandveil", 0.7f, 33.3f,
            "Dugtrio, the Mole Pokémon. National Dex #51."),
    MEOWTH("meowth", MonElement.NORMAL, BodyShape.QUAD, 40, 43, 0xDEE2E6, 0x6C757D, false, 1,
            "Scratch Cat", "gen1, kantonian_form", "pickup", 0.4f, 4.2f,
            "Meowth, the Scratch Cat Pokémon. National Dex #52."),
    PERSIAN("persian", MonElement.NORMAL, BodyShape.QUAD, 65, 68, 0xDEE2E6, 0x6C757D, false, 2,
            "Classy Cat", "gen1, kantonian_form", "limber", 1.0f, 32.0f,
            "Persian, the Classy Cat Pokémon. National Dex #53."),
    PSYDUCK("psyduck", MonElement.WATER, BodyShape.AQUATIC, 50, 59, 0x4CC9F0, 0x023E8A, false, 1,
            "Duck", "gen1", "damp", 0.8f, 19.6f,
            "Psyduck, the Duck Pokémon. National Dex #54."),
    GOLDUCK("golduck", MonElement.WATER, BodyShape.AQUATIC, 80, 89, 0x4CC9F0, 0x023E8A, false, 2,
            "Duck", "gen1", "damp", 1.7f, 76.6f,
            "Golduck, the Duck Pokémon. National Dex #55."),
    MANKEY("mankey", MonElement.FIGHTING, BodyShape.BIPED, 40, 58, 0xC1121F, 0x780000, false, 1,
            "Pig Monkey", "gen1", "vitalspirit", 0.5f, 28.0f,
            "Mankey, the Pig Monkey Pokémon. National Dex #56."),
    PRIMEAPE("primeape", MonElement.FIGHTING, BodyShape.BIPED, 65, 83, 0xC1121F, 0x780000, false, 2,
            "Pig Monkey", "gen1", "vitalspirit", 1.0f, 32.0f,
            "Primeape, the Pig Monkey Pokémon. National Dex #57."),
    GROWLITHE("growlithe", MonElement.FIRE, BodyShape.QUAD, 55, 70, 0xE85D04, 0x370617, false, 1,
            "Puppy", "gen1, kantonian_form", "intimidate", 0.7f, 19.0f,
            "Growlithe, the Puppy Pokémon. National Dex #58."),
    ARCANINE("arcanine", MonElement.FIRE, BodyShape.QUAD, 90, 105, 0xE85D04, 0x370617, false, 2,
            "Legendary", "gen1, kantonian_form", "intimidate", 1.9f, 155.0f,
            "Arcanine, the Legendary Pokémon. National Dex #59."),
    POLIWAG("poliwag", MonElement.WATER, BodyShape.AQUATIC, 40, 45, 0x4CC9F0, 0x023E8A, false, 1,
            "Tadpole", "gen1", "waterabsorb", 0.6f, 12.4f,
            "Poliwag, the Tadpole Pokémon. National Dex #60."),
    POLIWHIRL("poliwhirl", MonElement.WATER, BodyShape.AQUATIC, 65, 58, 0x4CC9F0, 0x023E8A, false, 2,
            "Tadpole", "gen1", "waterabsorb", 1.0f, 20.0f,
            "Poliwhirl, the Tadpole Pokémon. National Dex #61."),
    POLIWRATH("poliwrath", MonElement.WATER, BodyShape.AQUATIC, 90, 83, 0x4CC9F0, 0x023E8A, false, 3,
            "Tadpole", "gen1", "waterabsorb", 1.3f, 54.0f,
            "Poliwrath, the Tadpole Pokémon. National Dex #62."),
    ABRA("abra", MonElement.PSYCHIC, BodyShape.BIPED, 25, 63, 0xFF6BCB, 0x8338EC, false, 1,
            "Psi", "gen1", "synchronize", 0.9f, 19.5f,
            "Abra, the Psi Pokémon. National Dex #63."),
    KADABRA("kadabra", MonElement.PSYCHIC, BodyShape.BIPED, 40, 78, 0xFF6BCB, 0x8338EC, false, 2,
            "Psi", "gen1", "synchronize", 1.3f, 56.5f,
            "Kadabra, the Psi Pokémon. National Dex #64."),
    ALAKAZAM("alakazam", MonElement.PSYCHIC, BodyShape.BIPED, 55, 93, 0xFF6BCB, 0x8338EC, false, 3,
            "Psi", "gen1", "synchronize", 1.5f, 48.0f,
            "Alakazam, the Psi Pokémon. National Dex #65."),
    MACHOP("machop", MonElement.FIGHTING, BodyShape.BIPED, 70, 58, 0xC1121F, 0x780000, false, 1,
            "Superpower", "gen1", "guts", 0.8f, 19.5f,
            "Machop, the Superpower Pokémon. National Dex #66."),
    MACHOKE("machoke", MonElement.FIGHTING, BodyShape.BIPED, 80, 75, 0xC1121F, 0x780000, false, 2,
            "Superpower", "gen1", "guts", 1.5f, 70.5f,
            "Machoke, the Superpower Pokémon. National Dex #67."),
    MACHAMP("machamp", MonElement.FIGHTING, BodyShape.BIPED, 90, 98, 0xC1121F, 0x780000, false, 3,
            "Superpower", "gen1", "guts", 1.6f, 130.0f,
            "Machamp, the Superpower Pokémon. National Dex #68."),
    BELLSPROUT("bellsprout", MonElement.GRASS, BodyShape.QUAD, 50, 73, 0x52B788, 0x1B4332, false, 1,
            "Flower", "gen1", "chlorophyll", 0.7f, 4.0f,
            "Bellsprout, the Flower Pokémon. National Dex #69."),
    WEEPINBELL("weepinbell", MonElement.GRASS, BodyShape.QUAD, 65, 88, 0x52B788, 0x1B4332, false, 2,
            "Flycatcher", "gen1", "chlorophyll", 1.0f, 6.4f,
            "Weepinbell, the Flycatcher Pokémon. National Dex #70."),
    VICTREEBEL("victreebel", MonElement.GRASS, BodyShape.QUAD, 80, 103, 0x52B788, 0x1B4332, false, 3,
            "Flycatcher", "gen1", "chlorophyll", 1.7f, 15.5f,
            "Victreebel, the Flycatcher Pokémon. National Dex #71."),
    TENTACOOL("tentacool", MonElement.WATER, BodyShape.AQUATIC, 40, 45, 0x4CC9F0, 0x023E8A, false, 1,
            "Jellyfish", "gen1", "clearbody", 0.9f, 45.5f,
            "Tentacool, the Jellyfish Pokémon. National Dex #72."),
    TENTACRUEL("tentacruel", MonElement.WATER, BodyShape.AQUATIC, 80, 75, 0x4CC9F0, 0x023E8A, false, 2,
            "Jellyfish", "gen1", "clearbody", 1.6f, 55.0f,
            "Tentacruel, the Jellyfish Pokémon. National Dex #73."),
    GEODUDE("geodude", MonElement.ROCK, BodyShape.ARMORED, 40, 55, 0xADB5BD, 0x495057, false, 1,
            "Rock", "gen1, kantonian_form", "rockhead", 0.4f, 20.0f,
            "Geodude, the Rock Pokémon. National Dex #74."),
    GRAVELER("graveler", MonElement.ROCK, BodyShape.ARMORED, 55, 70, 0xADB5BD, 0x495057, false, 2,
            "Rock", "gen1, kantonian_form", "rockhead", 1.0f, 105.0f,
            "Graveler, the Rock Pokémon. National Dex #75."),
    GOLEM("golem", MonElement.ROCK, BodyShape.ARMORED, 80, 88, 0xADB5BD, 0x495057, false, 3,
            "Megaton", "gen1, kantonian_form", "rockhead", 1.4f, 300.0f,
            "Golem, the Megaton Pokémon. National Dex #76."),
    PONYTA("ponyta", MonElement.FIRE, BodyShape.QUAD, 50, 75, 0xE85D04, 0x370617, false, 1,
            "Fire Horse", "gen1, kantonian_form", "runaway", 1.0f, 30.0f,
            "Ponyta, the Fire Horse Pokémon. National Dex #77."),
    RAPIDASH("rapidash", MonElement.FIRE, BodyShape.QUAD, 65, 90, 0xE85D04, 0x370617, false, 2,
            "Fire Horse", "gen1, kantonian_form", "runaway", 1.7f, 95.0f,
            "Rapidash, the Fire Horse Pokémon. National Dex #78."),
    SLOWPOKE("slowpoke", MonElement.WATER, BodyShape.AQUATIC, 90, 53, 0x4CC9F0, 0x023E8A, false, 1,
            "Dopey", "gen1, kantonian_form", "oblivious", 1.2f, 36.0f,
            "Slowpoke, the Dopey Pokémon. National Dex #79."),
    SLOWBRO("slowbro", MonElement.WATER, BodyShape.AQUATIC, 95, 88, 0x4CC9F0, 0x023E8A, false, 2,
            "Hermit Crab", "gen1, kantonian_form", "oblivious", 1.6f, 78.5f,
            "Slowbro, the Hermit Crab Pokémon. National Dex #80."),
    MAGNEMITE("magnemite", MonElement.ELECTRIC, BodyShape.QUAD, 25, 65, 0xFEE440, 0x7B2CBF, false, 1,
            "Magnet", "gen1", "magnetpull", 0.3f, 6.0f,
            "Magnemite, the Magnet Pokémon. National Dex #81."),
    MAGNETON("magneton", MonElement.ELECTRIC, BodyShape.QUAD, 50, 90, 0xFEE440, 0x7B2CBF, false, 2,
            "Magnet", "gen1", "magnetpull", 1.0f, 60.0f,
            "Magneton, the Magnet Pokémon. National Dex #82."),
    FARFETCHD("farfetchd", MonElement.NORMAL, BodyShape.AVIAN, 52, 74, 0xDEE2E6, 0x6C757D, false, 1,
            "Wild Duck", "gen1, kantonian_form", "keeneye", 0.8f, 15.0f,
            "Farfetch’d, the Wild Duck Pokémon. National Dex #83."),
    DODUO("doduo", MonElement.NORMAL, BodyShape.AVIAN, 35, 60, 0xDEE2E6, 0x6C757D, false, 1,
            "Twin Bird", "gen1", "runaway", 1.4f, 39.2f,
            "Doduo, the Twin Bird Pokémon. National Dex #84."),
    DODRIO("dodrio", MonElement.NORMAL, BodyShape.AVIAN, 60, 85, 0xDEE2E6, 0x6C757D, false, 2,
            "Triple Bird", "gen1", "runaway", 1.8f, 85.2f,
            "Dodrio, the Triple Bird Pokémon. National Dex #85."),
    SEEL("seel", MonElement.WATER, BodyShape.AQUATIC, 65, 45, 0x4CC9F0, 0x023E8A, false, 1,
            "Sea Lion", "gen1", "thickfat", 1.1f, 90.0f,
            "Seel, the Sea Lion Pokémon. National Dex #86."),
    DEWGONG("dewgong", MonElement.WATER, BodyShape.AQUATIC, 90, 70, 0x4CC9F0, 0x023E8A, false, 2,
            "Sea Lion", "gen1", "thickfat", 1.7f, 120.0f,
            "Dewgong, the Sea Lion Pokémon. National Dex #87."),
    GRIMER("grimer", MonElement.POISON, BodyShape.AMORPH, 80, 60, 0x9B5DE5, 0x5A189A, false, 1,
            "Sludge", "gen1, kantonian_form", "stench", 0.9f, 30.0f,
            "Grimer, the Sludge Pokémon. National Dex #88."),
    MUK("muk", MonElement.POISON, BodyShape.QUAD, 105, 85, 0x9B5DE5, 0x5A189A, false, 2,
            "Sludge", "gen1, kantonian_form", "stench", 1.2f, 30.0f,
            "Muk, the Sludge Pokémon. National Dex #89."),
    SHELLDER("shellder", MonElement.WATER, BodyShape.AQUATIC, 30, 55, 0x4CC9F0, 0x023E8A, false, 1,
            "Bivalve", "gen1", "shellarmor", 0.3f, 4.0f,
            "Shellder, the Bivalve Pokémon. National Dex #90."),
    CLOYSTER("cloyster", MonElement.WATER, BodyShape.AQUATIC, 50, 90, 0x4CC9F0, 0x023E8A, false, 2,
            "Bivalve", "gen1", "shellarmor", 1.5f, 132.5f,
            "Cloyster, the Bivalve Pokémon. National Dex #91."),
    GASTLY("gastly", MonElement.GHOST, BodyShape.AMORPH, 30, 68, 0x7B2CBF, 0x240046, false, 1,
            "Gas", "gen1", "levitate", 1.3f, 0.1f,
            "Gastly, the Gas Pokémon. National Dex #92."),
    HAUNTER("haunter", MonElement.GHOST, BodyShape.AMORPH, 45, 83, 0x7B2CBF, 0x240046, false, 2,
            "Gas", "gen1", "levitate", 1.6f, 0.1f,
            "Haunter, the Gas Pokémon. National Dex #93."),
    GENGAR("gengar", MonElement.GHOST, BodyShape.AMORPH, 60, 98, 0x7B2CBF, 0x240046, false, 3,
            "Shadow", "gen1", "cursedbody", 1.5f, 40.5f,
            "Gengar, the Shadow Pokémon. National Dex #94."),
    ONIX("onix", MonElement.ROCK, BodyShape.SERPENT, 35, 38, 0xADB5BD, 0x495057, false, 1,
            "Rock Snake", "gen1", "rockhead", 8.8f, 210.0f,
            "Onix, the Rock Snake Pokémon. National Dex #95."),
    DROWZEE("drowzee", MonElement.PSYCHIC, BodyShape.QUAD, 60, 46, 0xFF6BCB, 0x8338EC, false, 1,
            "Hypnosis", "gen1", "insomnia", 1.0f, 32.4f,
            "Drowzee, the Hypnosis Pokémon. National Dex #96."),
    HYPNO("hypno", MonElement.PSYCHIC, BodyShape.BIPED, 85, 73, 0xFF6BCB, 0x8338EC, false, 2,
            "Hypnosis", "gen1", "insomnia", 1.6f, 75.6f,
            "Hypno, the Hypnosis Pokémon. National Dex #97."),
    KRABBY("krabby", MonElement.WATER, BodyShape.AQUATIC, 30, 65, 0x4CC9F0, 0x023E8A, false, 1,
            "River Crab", "gen1", "hypercutter", 0.4f, 6.5f,
            "Krabby, the River Crab Pokémon. National Dex #98."),
    KINGLER("kingler", MonElement.WATER, BodyShape.AQUATIC, 55, 90, 0x4CC9F0, 0x023E8A, false, 2,
            "Pincer", "gen1", "hypercutter", 1.3f, 60.0f,
            "Kingler, the Pincer Pokémon. National Dex #99."),
    VOLTORB("voltorb", MonElement.ELECTRIC, BodyShape.QUAD, 40, 43, 0xFEE440, 0x7B2CBF, false, 1,
            "Ball", "gen1, kantonian_form", "soundproof", 0.5f, 10.4f,
            "Voltorb, the Ball Pokémon. National Dex #100."),
    ELECTRODE("electrode", MonElement.ELECTRIC, BodyShape.QUAD, 60, 65, 0xFEE440, 0x7B2CBF, false, 2,
            "Ball", "gen1, kantonian_form", "soundproof", 1.2f, 66.6f,
            "Electrode, the Ball Pokémon. National Dex #101."),
    EXEGGCUTE("exeggcute", MonElement.GRASS, BodyShape.QUAD, 60, 50, 0x52B788, 0x1B4332, false, 1,
            "Egg", "gen1, kantonian_form", "chlorophyll", 0.4f, 2.5f,
            "Exeggcute, the Egg Pokémon. National Dex #102."),
    EXEGGUTOR("exeggutor", MonElement.GRASS, BodyShape.QUAD, 95, 110, 0x52B788, 0x1B4332, false, 2,
            "Coconut", "gen1, kantonian_form", "chlorophyll", 2.0f, 120.0f,
            "Exeggutor, the Coconut Pokémon. National Dex #103."),
    CUBONE("cubone", MonElement.GROUND, BodyShape.QUAD, 50, 45, 0xD4A373, 0x6F4518, false, 1,
            "Lonely", "gen1, kantonian_form", "rockhead", 0.4f, 6.5f,
            "Cubone, the Lonely Pokémon. National Dex #104."),
    MAROWAK("marowak", MonElement.GROUND, BodyShape.QUAD, 60, 65, 0xD4A373, 0x6F4518, false, 2,
            "Bone Keeper", "gen1, kantonian_form", "rockhead", 1.0f, 45.0f,
            "Marowak, the Bone Keeper Pokémon. National Dex #105."),
    HITMONLEE("hitmonlee", MonElement.FIGHTING, BodyShape.BIPED, 50, 78, 0xC1121F, 0x780000, false, 1,
            "Kicking", "gen1", "limber", 1.5f, 49.8f,
            "Hitmonlee, the Kicking Pokémon. National Dex #106."),
    HITMONCHAN("hitmonchan", MonElement.FIGHTING, BodyShape.BIPED, 50, 70, 0xC1121F, 0x780000, false, 1,
            "Punching", "gen1", "keeneye", 1.4f, 50.2f,
            "Hitmonchan, the Punching Pokémon. National Dex #107."),
    LICKITUNG("lickitung", MonElement.NORMAL, BodyShape.QUAD, 90, 58, 0xDEE2E6, 0x6C757D, false, 1,
            "Licking", "gen1", "owntempo", 1.2f, 65.5f,
            "Lickitung, the Licking Pokémon. National Dex #108."),
    KOFFING("koffing", MonElement.POISON, BodyShape.QUAD, 40, 63, 0x9B5DE5, 0x5A189A, false, 1,
            "Poison Gas", "gen1, kantonian_form", "levitate", 0.6f, 1.0f,
            "Koffing, the Poison Gas Pokémon. National Dex #109."),
    WEEZING("weezing", MonElement.POISON, BodyShape.QUAD, 65, 88, 0x9B5DE5, 0x5A189A, false, 2,
            "Poison Gas", "gen1, kantonian_form", "levitate", 1.2f, 9.5f,
            "Weezing, the Poison Gas Pokémon. National Dex #110."),
    RHYHORN("rhyhorn", MonElement.GROUND, BodyShape.QUAD, 80, 58, 0xD4A373, 0x6F4518, false, 1,
            "Spikes", "gen1", "lightningrod", 1.0f, 115.0f,
            "Rhyhorn, the Spikes Pokémon. National Dex #111."),
    RHYDON("rhydon", MonElement.GROUND, BodyShape.QUAD, 105, 88, 0xD4A373, 0x6F4518, false, 2,
            "Drill", "gen1", "lightningrod", 1.9f, 120.0f,
            "Rhydon, the Drill Pokémon. National Dex #112."),
    CHANSEY("chansey", MonElement.NORMAL, BodyShape.QUAD, 250, 20, 0xDEE2E6, 0x6C757D, false, 1,
            "Egg", "gen1", "naturalcure", 1.1f, 34.6f,
            "Chansey, the Egg Pokémon. National Dex #113."),
    TANGELA("tangela", MonElement.GRASS, BodyShape.QUAD, 65, 78, 0x52B788, 0x1B4332, false, 1,
            "Vine", "gen1", "chlorophyll", 1.0f, 35.0f,
            "Tangela, the Vine Pokémon. National Dex #114."),
    KANGASKHAN("kangaskhan", MonElement.NORMAL, BodyShape.BIPED, 105, 68, 0xDEE2E6, 0x6C757D, false, 1,
            "Parent", "gen1", "earlybird", 2.2f, 80.0f,
            "Kangaskhan, the Parent Pokémon. National Dex #115."),
    HORSEA("horsea", MonElement.WATER, BodyShape.AQUATIC, 30, 55, 0x4CC9F0, 0x023E8A, false, 1,
            "Dragon", "gen1", "swiftswim", 0.4f, 8.0f,
            "Horsea, the Dragon Pokémon. National Dex #116."),
    SEADRA("seadra", MonElement.WATER, BodyShape.AQUATIC, 55, 80, 0x4CC9F0, 0x023E8A, false, 2,
            "Dragon", "gen1", "poisonpoint", 1.2f, 25.0f,
            "Seadra, the Dragon Pokémon. National Dex #117."),
    GOLDEEN("goldeen", MonElement.WATER, BodyShape.AQUATIC, 45, 51, 0x4CC9F0, 0x023E8A, false, 1,
            "Goldfish", "gen1", "swiftswim", 0.6f, 15.0f,
            "Goldeen, the Goldfish Pokémon. National Dex #118."),
    SEAKING("seaking", MonElement.WATER, BodyShape.AQUATIC, 80, 79, 0x4CC9F0, 0x023E8A, false, 2,
            "Goldfish", "gen1", "swiftswim", 1.3f, 39.0f,
            "Seaking, the Goldfish Pokémon. National Dex #119."),
    STARYU("staryu", MonElement.WATER, BodyShape.AQUATIC, 30, 58, 0x4CC9F0, 0x023E8A, false, 1,
            "Star Shape", "gen1", "illuminate", 0.8f, 34.5f,
            "Staryu, the Star Shape Pokémon. National Dex #120."),
    STARMIE("starmie", MonElement.WATER, BodyShape.AQUATIC, 60, 88, 0x4CC9F0, 0x023E8A, false, 2,
            "Mysterious", "gen1", "illuminate", 1.1f, 80.0f,
            "Starmie, the Mysterious Pokémon. National Dex #121."),
    MR_MIME("mr_mime", MonElement.PSYCHIC, BodyShape.BIPED, 40, 73, 0xFF6BCB, 0x8338EC, false, 1,
            "Barrier", "gen1, kantonian_form", "soundproof", 1.3f, 54.5f,
            "Mr. Mime, the Barrier Pokémon. National Dex #122."),
    SCYTHER("scyther", MonElement.BUG, BodyShape.INSECTOID, 70, 83, 0xA7C957, 0x386641, false, 1,
            "Mantis", "gen1", "swarm", 1.5f, 56.0f,
            "Scyther, the Mantis Pokémon. National Dex #123."),
    JYNX("jynx", MonElement.ICE, BodyShape.BIPED, 65, 83, 0x90E0EF, 0x0077B6, false, 1,
            "Human Shape", "gen1", "oblivious", 1.4f, 40.6f,
            "Jynx, the Human Shape Pokémon. National Dex #124."),
    ELECTABUZZ("electabuzz", MonElement.ELECTRIC, BodyShape.BIPED, 65, 89, 0xFEE440, 0x7B2CBF, false, 1,
            "Electric", "gen1", "static", 1.1f, 30.0f,
            "Electabuzz, the Electric Pokémon. National Dex #125."),
    MAGMAR("magmar", MonElement.FIRE, BodyShape.BIPED, 65, 98, 0xE85D04, 0x370617, false, 1,
            "Spitfire", "gen1", "flamebody", 1.3f, 44.5f,
            "Magmar, the Spitfire Pokémon. National Dex #126."),
    PINSIR("pinsir", MonElement.BUG, BodyShape.INSECTOID, 65, 90, 0xA7C957, 0x386641, false, 1,
            "Stag Beetle", "gen1", "hypercutter", 1.5f, 55.0f,
            "Pinsir, the Stag Beetle Pokémon. National Dex #127."),
    TAUROS("tauros", MonElement.NORMAL, BodyShape.QUAD, 75, 70, 0xDEE2E6, 0x6C757D, false, 1,
            "Wild Bull", "gen1, kantonian_form", "intimidate", 1.4f, 88.4f,
            "Tauros, the Wild Bull Pokémon. National Dex #128."),
    MAGIKARP("magikarp", MonElement.WATER, BodyShape.AQUATIC, 20, 13, 0x4CC9F0, 0x023E8A, false, 1,
            "Fish", "gen1", "swiftswim", 0.9f, 10.0f,
            "Magikarp, the Fish Pokémon. National Dex #129."),
    GYARADOS("gyarados", MonElement.WATER, BodyShape.AVIAN, 95, 93, 0x4CC9F0, 0x023E8A, false, 2,
            "Atrocious", "gen1", "intimidate", 6.5f, 235.0f,
            "Gyarados, the Atrocious Pokémon. National Dex #130."),
    LAPRAS("lapras", MonElement.WATER, BodyShape.AQUATIC, 130, 85, 0x4CC9F0, 0x023E8A, false, 1,
            "Transport", "gen1", "waterabsorb", 2.5f, 220.0f,
            "Lapras, the Transport Pokémon. National Dex #131."),
    DITTO("ditto", MonElement.NORMAL, BodyShape.AMORPH, 48, 48, 0xDEE2E6, 0x6C757D, false, 1,
            "Transform", "gen1", "limber", 0.3f, 4.0f,
            "Ditto, the Transform Pokémon. National Dex #132."),
    EEVEE("eevee", MonElement.NORMAL, BodyShape.QUAD, 55, 50, 0xDEE2E6, 0x6C757D, false, 1,
            "Evolution", "gen1", "runaway", 0.3f, 6.5f,
            "Eevee, the Evolution Pokémon. National Dex #133."),
    VAPOREON("vaporeon", MonElement.WATER, BodyShape.AQUATIC, 130, 88, 0x4CC9F0, 0x023E8A, false, 1,
            "Bubble Jet", "gen1", "waterabsorb", 1.0f, 29.0f,
            "Vaporeon, the Bubble Jet Pokémon. National Dex #134."),
    JOLTEON("jolteon", MonElement.ELECTRIC, BodyShape.QUAD, 65, 88, 0xFEE440, 0x7B2CBF, false, 2,
            "Lightning", "gen1", "voltabsorb", 0.8f, 24.5f,
            "Jolteon, the Lightning Pokémon. National Dex #135."),
    FLAREON("flareon", MonElement.FIRE, BodyShape.QUAD, 65, 113, 0xE85D04, 0x370617, false, 1,
            "Flame", "gen1", "flashfire", 0.9f, 25.0f,
            "Flareon, the Flame Pokémon. National Dex #136."),
    PORYGON("porygon", MonElement.NORMAL, BodyShape.QUAD, 65, 73, 0xDEE2E6, 0x6C757D, false, 1,
            "Virtual", "gen1", "trace", 0.8f, 36.5f,
            "Porygon, the Virtual Pokémon. National Dex #137."),
    OMANYTE("omanyte", MonElement.ROCK, BodyShape.AQUATIC, 35, 65, 0xADB5BD, 0x495057, false, 1,
            "Spiral", "gen1, fossil", "swiftswim", 0.4f, 7.5f,
            "Omanyte, the Spiral Pokémon. National Dex #138."),
    OMASTAR("omastar", MonElement.ROCK, BodyShape.ARMORED, 70, 88, 0xADB5BD, 0x495057, false, 2,
            "Spiral", "gen1, fossil", "swiftswim", 1.0f, 35.0f,
            "Omastar, the Spiral Pokémon. National Dex #139."),
    KABUTO("kabuto", MonElement.ROCK, BodyShape.AQUATIC, 30, 68, 0xADB5BD, 0x495057, false, 1,
            "Shellfish", "gen1, fossil", "swiftswim", 0.5f, 11.5f,
            "Kabuto, the Shellfish Pokémon. National Dex #140."),
    KABUTOPS("kabutops", MonElement.ROCK, BodyShape.AQUATIC, 60, 90, 0xADB5BD, 0x495057, false, 2,
            "Shellfish", "gen1, fossil", "swiftswim", 1.3f, 40.5f,
            "Kabutops, the Shellfish Pokémon. National Dex #141."),
    AERODACTYL("aerodactyl", MonElement.ROCK, BodyShape.AVIAN, 80, 83, 0xADB5BD, 0x495057, false, 1,
            "Fossil", "gen1, fossil", "rockhead", 1.8f, 59.0f,
            "Aerodactyl, the Fossil Pokémon. National Dex #142."),
    SNORLAX("snorlax", MonElement.NORMAL, BodyShape.BIPED, 160, 88, 0xDEE2E6, 0x6C757D, false, 1,
            "Sleeping", "gen1", "immunity", 2.1f, 460.0f,
            "Snorlax, the Sleeping Pokémon. National Dex #143."),
    ARTICUNO("articuno", MonElement.ICE, BodyShape.AVIAN, 90, 90, 0x90E0EF, 0x0077B6, false, 1,
            "Freeze", "gen1, legendary, kantonian_form", "pressure", 1.7f, 55.4f,
            "Articuno, the Freeze Pokémon. National Dex #144."),
    ZAPDOS("zapdos", MonElement.ELECTRIC, BodyShape.AVIAN, 90, 108, 0xFEE440, 0x7B2CBF, false, 1,
            "Electric", "gen1, legendary, kantonian_form", "pressure", 1.6f, 52.6f,
            "Zapdos, the Electric Pokémon. National Dex #145."),
    MOLTRES("moltres", MonElement.FIRE, BodyShape.AVIAN, 90, 113, 0xE85D04, 0x370617, false, 1,
            "Flame", "gen1, legendary, kantonian_form", "pressure", 2.0f, 60.0f,
            "Moltres, the Flame Pokémon. National Dex #146."),
    DRATINI("dratini", MonElement.DRAGON, BodyShape.SERPENT, 41, 57, 0x5A189A, 0x240046, false, 1,
            "Dragon", "gen1", "shedskin", 1.8f, 3.3f,
            "Dratini, the Dragon Pokémon. National Dex #147."),
    DRAGONAIR("dragonair", MonElement.DRAGON, BodyShape.SERPENT, 61, 77, 0x5A189A, 0x240046, false, 2,
            "Dragon", "gen1", "shedskin", 4.0f, 16.5f,
            "Dragonair, the Dragon Pokémon. National Dex #148."),
    DRAGONITE("dragonite", MonElement.DRAGON, BodyShape.SERPENT, 91, 117, 0x5A189A, 0x240046, false, 3,
            "Dragon", "gen1, powerhouse", "innerfocus", 2.2f, 210.0f,
            "Dragonite, the Dragon Pokémon. National Dex #149."),
    MEWTWO("mewtwo", MonElement.PSYCHIC, BodyShape.FEY, 106, 132, 0xFF6BCB, 0x8338EC, false, 1,
            "Genetic", "gen1, legendary, restricted", "pressure", 2.0f, 122.0f,
            "Mewtwo, the Genetic Pokémon. National Dex #150."),
    MEW("mew", MonElement.PSYCHIC, BodyShape.FEY, 100, 100, 0xFF6BCB, 0x8338EC, false, 1,
            "New Species", "gen1, mythical", "synchronize", 0.4f, 4.0f,
            "Mew, the New Species Pokémon. National Dex #151.");

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

    /** Ordered evolution options (multi-branch, e.g. Eevee). */
    private final java.util.List<EvoBranch> evolutions = new java.util.ArrayList<>();

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
        // ===== Level-up lines =====
        link(BULBASAUR, IVYSAUR, EvolutionMethod.LEVEL, 16);
        link(IVYSAUR, VENUSAUR, EvolutionMethod.LEVEL, 32);
        link(CHARMANDER, CHARMELEON, EvolutionMethod.LEVEL, 16);
        link(CHARMELEON, CHARIZARD, EvolutionMethod.LEVEL, 36);
        link(SQUIRTLE, WARTORTLE, EvolutionMethod.LEVEL, 16);
        link(WARTORTLE, BLASTOISE, EvolutionMethod.LEVEL, 36);
        link(CATERPIE, METAPOD, EvolutionMethod.LEVEL, 7);
        link(METAPOD, BUTTERFREE, EvolutionMethod.LEVEL, 10);
        link(WEEDLE, KAKUNA, EvolutionMethod.LEVEL, 7);
        link(KAKUNA, BEEDRILL, EvolutionMethod.LEVEL, 10);
        link(PIDGEY, PIDGEOTTO, EvolutionMethod.LEVEL, 18);
        link(PIDGEOTTO, PIDGEOT, EvolutionMethod.LEVEL, 36);
        link(RATTATA, RATICATE, EvolutionMethod.LEVEL, 20);
        link(SPEAROW, FEAROW, EvolutionMethod.LEVEL, 20);
        link(EKANS, ARBOK, EvolutionMethod.LEVEL, 22);
        link(SANDSHREW, SANDSLASH, EvolutionMethod.LEVEL, 22);
        link(NIDORAN_F, NIDORINA, EvolutionMethod.LEVEL, 16);
        link(NIDORAN_M, NIDORINO, EvolutionMethod.LEVEL, 16);
        link(ZUBAT, GOLBAT, EvolutionMethod.LEVEL, 22);
        link(ODDISH, GLOOM, EvolutionMethod.LEVEL, 21);
        link(PARAS, PARASECT, EvolutionMethod.LEVEL, 24);
        link(VENONAT, VENOMOTH, EvolutionMethod.LEVEL, 31);
        link(DIGLETT, DUGTRIO, EvolutionMethod.LEVEL, 26);
        link(MEOWTH, PERSIAN, EvolutionMethod.LEVEL, 28);
        link(PSYDUCK, GOLDUCK, EvolutionMethod.LEVEL, 33);
        link(MANKEY, PRIMEAPE, EvolutionMethod.LEVEL, 28);
        link(POLIWAG, POLIWHIRL, EvolutionMethod.LEVEL, 25);
        link(ABRA, KADABRA, EvolutionMethod.LEVEL, 16);
        link(MACHOP, MACHOKE, EvolutionMethod.LEVEL, 28);
        link(BELLSPROUT, WEEPINBELL, EvolutionMethod.LEVEL, 21);
        link(TENTACOOL, TENTACRUEL, EvolutionMethod.LEVEL, 30);
        link(GEODUDE, GRAVELER, EvolutionMethod.LEVEL, 25);
        link(PONYTA, RAPIDASH, EvolutionMethod.LEVEL, 40);
        link(SLOWPOKE, SLOWBRO, EvolutionMethod.LEVEL, 37);
        link(MAGNEMITE, MAGNETON, EvolutionMethod.LEVEL, 30);
        link(DODUO, DODRIO, EvolutionMethod.LEVEL, 31);
        link(SEEL, DEWGONG, EvolutionMethod.LEVEL, 34);
        link(GRIMER, MUK, EvolutionMethod.LEVEL, 38);
        link(GASTLY, HAUNTER, EvolutionMethod.LEVEL, 25);
        link(DROWZEE, HYPNO, EvolutionMethod.LEVEL, 26);
        link(KRABBY, KINGLER, EvolutionMethod.LEVEL, 28);
        link(VOLTORB, ELECTRODE, EvolutionMethod.LEVEL, 30);
        link(CUBONE, MAROWAK, EvolutionMethod.LEVEL, 28);
        link(KOFFING, WEEZING, EvolutionMethod.LEVEL, 35);
        link(RHYHORN, RHYDON, EvolutionMethod.LEVEL, 42);
        link(HORSEA, SEADRA, EvolutionMethod.LEVEL, 32);
        link(GOLDEEN, SEAKING, EvolutionMethod.LEVEL, 33);
        link(MAGIKARP, GYARADOS, EvolutionMethod.LEVEL, 20);
        link(OMANYTE, OMASTAR, EvolutionMethod.LEVEL, 40);
        link(KABUTO, KABUTOPS, EvolutionMethod.LEVEL, 40);
        link(DRATINI, DRAGONAIR, EvolutionMethod.LEVEL, 30);
        link(DRAGONAIR, DRAGONITE, EvolutionMethod.LEVEL, 55);

        // ===== Stone / gem evolutions =====
        link(PIKACHU, RAICHU, EvolutionMethod.THUNDER_GEM, 0);
        link(NIDORINA, NIDOQUEEN, EvolutionMethod.MOON_GEM, 0);
        link(NIDORINO, NIDOKING, EvolutionMethod.MOON_GEM, 0);
        link(CLEFAIRY, CLEFABLE, EvolutionMethod.MOON_GEM, 0);
        link(VULPIX, NINETALES, EvolutionMethod.FIRE_GEM, 0);
        link(JIGGLYPUFF, WIGGLYTUFF, EvolutionMethod.MOON_GEM, 0);
        link(GLOOM, VILEPLUME, EvolutionMethod.LEAF_GEM, 0);
        link(GROWLITHE, ARCANINE, EvolutionMethod.FIRE_GEM, 0);
        link(POLIWHIRL, POLIWRATH, EvolutionMethod.WATER_GEM, 0);
        link(WEEPINBELL, VICTREEBEL, EvolutionMethod.LEAF_GEM, 0);
        link(SHELLDER, CLOYSTER, EvolutionMethod.WATER_GEM, 0);
        link(EXEGGCUTE, EXEGGUTOR, EvolutionMethod.LEAF_GEM, 0);
        link(STARYU, STARMIE, EvolutionMethod.WATER_GEM, 0);
        // Eevee branches (all three Gen1 stones)
        link(EEVEE, VAPOREON, EvolutionMethod.WATER_GEM, 0);
        link(EEVEE, JOLTEON, EvolutionMethod.THUNDER_GEM, 0);
        link(EEVEE, FLAREON, EvolutionMethod.FIRE_GEM, 0);

        // ===== Trade evolutions (level proxy — no link cable yet) =====
        link(KADABRA, ALAKAZAM, EvolutionMethod.TRADE, 37);
        link(MACHOKE, MACHAMP, EvolutionMethod.TRADE, 37);
        link(GRAVELER, GOLEM, EvolutionMethod.TRADE, 37);
        link(HAUNTER, GENGAR, EvolutionMethod.TRADE, 37);
    }
    private static void link(MonSpecies from, MonSpecies to, EvolutionMethod method, int level) {
        from.evolutions.add(new EvoBranch(to, method, level));
    }

    public String id() { return id; }
    public MonElement element() { return element; }
    public BodyShape bodyShape() { return bodyShape; }
    public int baseHp() { return baseHp; }
    public int basePower() { return basePower; }

    public int baseStat(StatId stat) {
        // Prefer full 6-stat BST imported from Cobblemon species JSON
        CobblemonBaseStats.Six six = CobblemonBaseStats.of(this);
        if (six != null) {
            return switch (stat) {
                case HP -> six.hp();
                case ATK -> six.atk();
                case DEF -> six.def();
                case SP_ATK -> six.spa();
                case SP_DEF -> six.spd();
                case SPEED -> six.spe();
            };
        }
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
            case BULBASAUR -> Optional.of(MonElement.POISON);
            case IVYSAUR -> Optional.of(MonElement.POISON);
            case VENUSAUR -> Optional.of(MonElement.POISON);
            case CHARIZARD -> Optional.of(MonElement.FLYING);
            case BUTTERFREE -> Optional.of(MonElement.FLYING);
            case WEEDLE -> Optional.of(MonElement.POISON);
            case KAKUNA -> Optional.of(MonElement.POISON);
            case BEEDRILL -> Optional.of(MonElement.POISON);
            case PIDGEY -> Optional.of(MonElement.FLYING);
            case PIDGEOTTO -> Optional.of(MonElement.FLYING);
            case PIDGEOT -> Optional.of(MonElement.FLYING);
            case SPEAROW -> Optional.of(MonElement.FLYING);
            case FEAROW -> Optional.of(MonElement.FLYING);
            case NIDOQUEEN -> Optional.of(MonElement.GROUND);
            case NIDOKING -> Optional.of(MonElement.GROUND);
            case JIGGLYPUFF -> Optional.of(MonElement.FAIRY);
            case WIGGLYTUFF -> Optional.of(MonElement.FAIRY);
            case ZUBAT -> Optional.of(MonElement.FLYING);
            case GOLBAT -> Optional.of(MonElement.FLYING);
            case ODDISH -> Optional.of(MonElement.POISON);
            case GLOOM -> Optional.of(MonElement.POISON);
            case VILEPLUME -> Optional.of(MonElement.POISON);
            case PARAS -> Optional.of(MonElement.GRASS);
            case PARASECT -> Optional.of(MonElement.GRASS);
            case VENONAT -> Optional.of(MonElement.POISON);
            case VENOMOTH -> Optional.of(MonElement.POISON);
            case POLIWRATH -> Optional.of(MonElement.FIGHTING);
            case BELLSPROUT -> Optional.of(MonElement.POISON);
            case WEEPINBELL -> Optional.of(MonElement.POISON);
            case VICTREEBEL -> Optional.of(MonElement.POISON);
            case TENTACOOL -> Optional.of(MonElement.POISON);
            case TENTACRUEL -> Optional.of(MonElement.POISON);
            case GEODUDE -> Optional.of(MonElement.GROUND);
            case GRAVELER -> Optional.of(MonElement.GROUND);
            case GOLEM -> Optional.of(MonElement.GROUND);
            case SLOWPOKE -> Optional.of(MonElement.PSYCHIC);
            case SLOWBRO -> Optional.of(MonElement.PSYCHIC);
            case MAGNEMITE -> Optional.of(MonElement.STEEL);
            case MAGNETON -> Optional.of(MonElement.STEEL);
            case FARFETCHD -> Optional.of(MonElement.FLYING);
            case DODUO -> Optional.of(MonElement.FLYING);
            case DODRIO -> Optional.of(MonElement.FLYING);
            case DEWGONG -> Optional.of(MonElement.ICE);
            case CLOYSTER -> Optional.of(MonElement.ICE);
            case GASTLY -> Optional.of(MonElement.POISON);
            case HAUNTER -> Optional.of(MonElement.POISON);
            case GENGAR -> Optional.of(MonElement.POISON);
            case ONIX -> Optional.of(MonElement.GROUND);
            case EXEGGCUTE -> Optional.of(MonElement.PSYCHIC);
            case EXEGGUTOR -> Optional.of(MonElement.PSYCHIC);
            case RHYHORN -> Optional.of(MonElement.ROCK);
            case RHYDON -> Optional.of(MonElement.ROCK);
            case STARMIE -> Optional.of(MonElement.PSYCHIC);
            case MR_MIME -> Optional.of(MonElement.FAIRY);
            case SCYTHER -> Optional.of(MonElement.FLYING);
            case JYNX -> Optional.of(MonElement.PSYCHIC);
            case GYARADOS -> Optional.of(MonElement.FLYING);
            case LAPRAS -> Optional.of(MonElement.ICE);
            case OMANYTE -> Optional.of(MonElement.WATER);
            case OMASTAR -> Optional.of(MonElement.WATER);
            case KABUTO -> Optional.of(MonElement.WATER);
            case KABUTOPS -> Optional.of(MonElement.WATER);
            case AERODACTYL -> Optional.of(MonElement.FLYING);
            case ARTICUNO -> Optional.of(MonElement.FLYING);
            case ZAPDOS -> Optional.of(MonElement.FLYING);
            case MOLTRES -> Optional.of(MonElement.FLYING);
            case DRAGONITE -> Optional.of(MonElement.FLYING);
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
        // Legendaries: tiny in-bucket weights (legendary bucket is already rare)
        if (this == MEWTWO) return 0.08f;
        if (this == MEW) return 0.05f;
        if (this == ARTICUNO || this == ZAPDOS || this == MOLTRES) return 0.12f;
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
            // Birds + mythicals: strict peak locks (no generic "mountain")
            case ARTICUNO -> java.util.Set.of("frozen_peaks", "snowy_slopes", "ice_spikes", "snowy", "frozen");
            case ZAPDOS -> java.util.Set.of("jagged_peaks", "stony_peaks", "windswept_hills", "windswept_gravelly", "savanna_plateau");
            case MOLTRES -> java.util.Set.of("basalt", "nether", "soul_sand", "desert", "badland");
            case MEWTWO -> java.util.Set.of("jagged_peaks", "stony_peaks", "frozen_peaks");
            case MEW -> java.util.Set.of("jagged_peaks", "frozen_peaks", "mushroom", "lush_caves");
            case CHARMANDER -> java.util.Set.of("desert", "badland", "savanna", "nether", "basalt");
            case SQUIRTLE -> java.util.Set.of("river", "ocean", "beach", "swamp", "lush");
            case BULBASAUR -> java.util.Set.of("forest", "jungle", "taiga", "grove", "floral");
            case PIKACHU -> java.util.Set.of("plains", "savanna", "meadow", "windswept");
            case PIDGEY -> java.util.Set.of("plains", "forest", "meadow", "windswept");
            case MAGIKARP, TENTACOOL, HORSEA, SHELLDER, KRABBY, STARYU -> java.util.Set.of("ocean", "river", "beach", "water");
            case DRATINI, DRAGONAIR, DRAGONITE -> java.util.Set.of("river", "ocean", "beach", "swamp");
            default -> java.util.Set.of();
        };
    }

    public java.util.Set<String> bannedBiomeKeywords() {
        return switch (this) {
            case ARTICUNO -> java.util.Set.of("desert", "badland", "nether", "jungle", "savanna");
            case ZAPDOS -> java.util.Set.of("ocean", "deep_dark", "lush_caves", "mushroom");
            case MOLTRES -> java.util.Set.of("frozen", "snowy", "ice", "ocean", "cold");
            case MEWTWO, MEW -> java.util.Set.of("ocean", "beach", "river", "desert", "nether");
            default -> java.util.Set.of();
        };
    }

    public boolean requiresNightSpawn() {
        if (this == MEWTWO || this == MEW) return true;
        if (this == ARTICUNO || this == ZAPDOS || this == MOLTRES) return true; // birds only at night
        return this == ZUBAT || this == GOLBAT || this == GASTLY || this == HAUNTER || this == GENGAR
                || this == MEOWTH || this == PERSIAN;
    }

    public boolean requiresOpenSky() {
        // Legendaries want open sky (caves blocked) except Mew can appear in lush
        if (this == MEW) return false;
        if (isLegendarySpecies()) return true;
        return this == PIDGEY || this == PIDGEOTTO || this == PIDGEOT
                || this == SPEAROW || this == FEAROW
                || this == ARTICUNO || this == ZAPDOS || this == MOLTRES;
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

    public java.util.List<EvoBranch> evolutions() {
        return java.util.Collections.unmodifiableList(evolutions);
    }

    /** First listed evolution (for UI hints). */
    public Optional<MonSpecies> evolvesInto() {
        return evolutions.isEmpty() ? Optional.empty() : Optional.of(evolutions.get(0).into());
    }

    public Optional<EvoBranch> evolutionForGem(EvolutionMethod gem) {
        return evolutions.stream().filter(b -> b.matchesGem(gem)).findFirst();
    }

    public Optional<EvoBranch> evolutionForLevel(int monLevel) {
        return evolutions.stream()
                .filter(b -> (b.method() == EvolutionMethod.LEVEL || b.method() == EvolutionMethod.TRADE)
                        && monLevel >= b.level())
                .findFirst();
    }
    public EvolutionMethod evolutionMethod() {
        return evolutions.isEmpty() ? EvolutionMethod.NONE : evolutions.get(0).method();
    }
    public int evolutionLevel() {
        return evolutions.isEmpty() ? 0 : evolutions.get(0).level();
    }

    public boolean canEvolveByLevel(int level) {
        return evolutionForLevel(level).isPresent();
    }
    public boolean canEvolveByFireGem() {
        return evolutionForGem(EvolutionMethod.FIRE_GEM).isPresent();
    }
    public boolean canEvolveByWaterGem() {
        return evolutionForGem(EvolutionMethod.WATER_GEM).isPresent();
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
        // Cobblemon lang: cobblemon.species.<id>.name — never show raw keys in UI
        MutableComponent fromLang = Component.translatable("cobblemon.species." + id + ".name");
        String s = fromLang.getString();
        if (s == null || s.isBlank() || s.contains(".") || s.startsWith("cobblemon.") || s.startsWith("species.")) {
            // Title-case id: caterpie → Caterpie
            String pretty = id.isEmpty() ? "?"
                    : Character.toUpperCase(id.charAt(0)) + id.substring(1).replace('_', ' ');
            return Component.literal(pretty);
        }
        return Component.literal(s);
    }

    /** Plain English name for logs/UI. */
    public String englishName() {
        return displayName().getString();
    }

    public MutableComponent categoryLine() {
        if (isLegendary()) {
            return Component.literal("Legendary · " + category + " Pokémon");
        }
        return Component.literal(category + " Pokémon");
    }

    public MutableComponent description() {
        if (blurb == null || blurb.isBlank()) {
            return Component.translatable("cobblemon.species." + id + ".desc");
        }
        return Component.literal(blurb);
    }

    public MutableComponent evolutionHint() {
        if (evolutions.isEmpty()) {
            return Component.translatable("species.cobblemon.no_evolution");
        }
        EvoBranch b = evolutions.get(0);
        MutableComponent first = switch (b.method()) {
            case LEVEL, TRADE -> Component.translatable("species.cobblemon.evo_level", b.into().displayName(), b.level());
            case FIRE_GEM -> Component.translatable("species.cobblemon.evo_fire_gem", b.into().displayName());
            case WATER_GEM -> Component.translatable("species.cobblemon.evo_water_gem", b.into().displayName());
            case THUNDER_GEM -> Component.translatable("species.cobblemon.evo_thunder_gem", b.into().displayName());
            case LEAF_GEM -> Component.translatable("species.cobblemon.evo_leaf_gem", b.into().displayName());
            case MOON_GEM -> Component.translatable("species.cobblemon.evo_moon_gem", b.into().displayName());
            default -> Component.empty();
        };
        if (evolutions.size() > 1) {
            return first.append(Component.literal(" (+" + (evolutions.size() - 1) + " branches)"));
        }
        return first;
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
            // old Cobblemon ids
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

package com.cobblemon.mod.species;

import com.cobblemon.mod.Cobblemon;
import net.minecraft.resources.Identifier;

/**
 * Gen 1 species → official Cobblemon original textures + Bedrock geo
 * (from Desktop/cobblemon/originals).
 */
public final class SpeciesTextures {
    private SpeciesTextures() {}

    public static Identifier texture(MonSpecies species) {
        return texture(species == null ? "rattata" : species.id());
    }

    /** Resolve texture for any species id (Gen1 enum paths or national-dex folder convention). */
    public static Identifier texture(String speciesId) {
        SpeciesHandle h = SpeciesHandle.of(speciesId);
        if (h.asEnum().isPresent()) {
            return Identifier.fromNamespaceAndPath(Cobblemon.MOD_ID, "textures/pokemon/" + pathFor(h.asEnum().get()));
        }
        return Identifier.fromNamespaceAndPath(Cobblemon.MOD_ID, "textures/pokemon/" + dynamicPath(h));
    }

    public static Identifier geo(MonSpecies species) {
        return geo(species == null ? "rattata" : species.id());
    }

    public static Identifier geo(String speciesId) {
        SpeciesHandle h = SpeciesHandle.of(speciesId);
        if (h.asEnum().isPresent()) {
            return Identifier.fromNamespaceAndPath(
                    Cobblemon.MOD_ID,
                    "bedrock/pokemon/models/" + geoPathFor(h.asEnum().get())
            );
        }
        String folder = dynamicFolder(h);
        return Identifier.fromNamespaceAndPath(
                Cobblemon.MOD_ID,
                "bedrock/pokemon/models/" + folder + "/" + h.id() + ".geo.json"
        );
    }

    public static Identifier animation(MonSpecies species) {
        return animation(species == null ? "rattata" : species.id());
    }

    public static Identifier animation(String speciesId) {
        SpeciesHandle h = SpeciesHandle.of(speciesId);
        if (h.asEnum().isPresent()) {
            return Identifier.fromNamespaceAndPath(
                    Cobblemon.MOD_ID,
                    "bedrock/pokemon/animations/" + animationPathFor(h.asEnum().get())
            );
        }
        String folder = dynamicFolder(h);
        return Identifier.fromNamespaceAndPath(
                Cobblemon.MOD_ID,
                "bedrock/pokemon/animations/" + folder + "/" + h.id() + ".animation.json"
        );
    }

    private static String dynamicFolder(SpeciesHandle h) {
        int dex = h.nationalDex();
        if (dex > 0) {
            return String.format("%04d_%s", dex, h.id());
        }
        return h.id();
    }

    private static String dynamicPath(SpeciesHandle h) {
        return dynamicFolder(h) + "/" + h.id() + ".png";
    }

    /**
     * Animation path matching geo folder. Prefer the same base name as the geo
     * (including {@code _male}), then ungendered, then {@code _male} fallback.
     */
    public static String animationPathFor(MonSpecies species) {
        String geo = geoPathFor(species);
        int slash = geo.indexOf('/');
        String folder = slash >= 0 ? geo.substring(0, slash) : geo;
        String base = slash >= 0 ? geo.substring(slash + 1) : geo;
        base = base.replace(".geo.json", "");
        // Known gendered-only packs (geo may be ungendered while anim is male/female)
        String ungendered = base.replace("_male", "").replace("_female", "");
        // Prefer exact geo name, then male, then plain
        if (base.contains("_male") || base.contains("_female")) {
            return folder + "/" + base + ".animation.json";
        }
        // Prefer male when only gendered anims exist (kadabra/alakazam)
        return switch (species) {
            case KADABRA -> "0064_kadabra/kadabra_male.animation.json";
            case ALAKAZAM -> "0065_alakazam/alakazam_male.animation.json";
            default -> folder + "/" + ungendered + ".animation.json";
        };
    }

    public static String pathFor(MonSpecies species) {
        return switch (species) {
            case BULBASAUR -> "0001_bulbasaur/bulbasaur.png";
            case IVYSAUR -> "0002_ivysaur/ivysaur.png";
            case VENUSAUR -> "0003_venusaur/venusaur_male.png";
            case CHARMANDER -> "0004_charmander/charmander.png";
            case CHARMELEON -> "0005_charmeleon/charmeleon.png";
            case CHARIZARD -> "0006_charizard/charizard.png";
            case SQUIRTLE -> "0007_squirtle/squirtle.png";
            case WARTORTLE -> "0008_wartortle/wartortle.png";
            case BLASTOISE -> "0009_blastoise/blastoise.png";
            case CATERPIE -> "0010_caterpie/caterpie.png";
            case METAPOD -> "0011_metapod/metapod.png";
            case BUTTERFREE -> "0012_butterfree/butterfree.png";
            case WEEDLE -> "0013_weedle/weedle.png";
            case KAKUNA -> "0014_kakuna/kakuna.png";
            case BEEDRILL -> "0015_beedrill/beedrill.png";
            case PIDGEY -> "0016_pidgey/pidgey.png";
            case PIDGEOTTO -> "0017_pidgeotto/pidgeotto.png";
            case PIDGEOT -> "0018_pidgeot/pidgeot.png";
            case RATTATA -> "0019_rattata/rattata.png";
            case RATICATE -> "0020_raticate/raticate.png";
            case SPEAROW -> "0021_spearow/spearow.png";
            case FEAROW -> "0022_fearow/fearow.png";
            case EKANS -> "0023_ekans/ekans.png";
            case ARBOK -> "0024_arbok/arbok.png";
            case PIKACHU -> "0025_pikachu/pikachu.png";
            case RAICHU -> "0026_raichu/raichu.png";
            case SANDSHREW -> "0027_sandshrew/sandshrew.png";
            case SANDSLASH -> "0028_sandslash/sandslash.png";
            case NIDORAN_F -> "0029_nidoranf/nidoranf.png";
            case NIDORINA -> "0030_nidorina/nidorina.png";
            case NIDOQUEEN -> "0031_nidoqueen/nidoqueen.png";
            case NIDORAN_M -> "0032_nidoranm/nidoranm.png";
            case NIDORINO -> "0033_nidorino/nidorino.png";
            case NIDOKING -> "0034_nidoking/nidoking.png";
            case CLEFAIRY -> "0035_clefairy/clefairy.png";
            case CLEFABLE -> "0036_clefable/clefable.png";
            case VULPIX -> "0037_vulpix/vulpix.png";
            case NINETALES -> "0038_ninetales/ninetales.png";
            case JIGGLYPUFF -> "0039_jigglypuff/jigglypuff.png";
            case WIGGLYTUFF -> "0040_wigglytuff/wigglytuff.png";
            case ZUBAT -> "0041_zubat/zubat.png";
            case GOLBAT -> "0042_golbat/golbat.png";
            case ODDISH -> "0043_oddish/oddish.png";
            case GLOOM -> "0044_gloom/gloom.png";
            case VILEPLUME -> "0045_vileplume/vileplume_male.png";
            case PARAS -> "0046_paras/paras.png";
            case PARASECT -> "0047_parasect/parasect.png";
            case VENONAT -> "0048_venonat/venonat.png";
            case VENOMOTH -> "0049_venomoth/venomoth.png";
            case DIGLETT -> "0050_diglett/diglett.png";
            case DUGTRIO -> "0051_dugtrio/dugtrio.png";
            case MEOWTH -> "0052_meowth/meowth.png";
            case PERSIAN -> "0053_persian/persian.png";
            case PSYDUCK -> "0054_psyduck/psyduck.png";
            case GOLDUCK -> "0055_golduck/golduck.png";
            case MANKEY -> "0056_mankey/mankey.png";
            case PRIMEAPE -> "0057_primeape/primeape.png";
            case GROWLITHE -> "0058_growlithe/growlithe.png";
            case ARCANINE -> "0059_arcanine/arcanine.png";
            case POLIWAG -> "0060_poliwag/poliwag.png";
            case POLIWHIRL -> "0061_poliwhirl/poliwhirl.png";
            case POLIWRATH -> "0062_poliwrath/poliwrath.png";
            case ABRA -> "0063_abra/abra.png";
            case KADABRA -> "0064_kadabra/kadabra.png";
            case ALAKAZAM -> "0065_alakazam/alakazam.png";
            case MACHOP -> "0066_machop/machop.png";
            case MACHOKE -> "0067_machoke/machoke.png";
            case MACHAMP -> "0068_machamp/machamp.png";
            case BELLSPROUT -> "0069_bellsprout/bellsprout.png";
            case WEEPINBELL -> "0070_weepinbell/weepinbell.png";
            case VICTREEBEL -> "0071_victreebel/victreebel.png";
            case TENTACOOL -> "0072_tentacool/tentacool.png";
            case TENTACRUEL -> "0073_tentacruel/tentacruel.png";
            case GEODUDE -> "0074_geodude/geodude.png";
            case GRAVELER -> "0075_graveler/graveler.png";
            case GOLEM -> "0076_golem/golem.png";
            case PONYTA -> "0077_ponyta/ponyta.png";
            case RAPIDASH -> "0078_rapidash/rapidash.png";
            case SLOWPOKE -> "0079_slowpoke/slowpoke.png";
            case SLOWBRO -> "0080_slowbro/slowbro.png";
            case MAGNEMITE -> "0081_magnemite/magnemite.png";
            case MAGNETON -> "0082_magneton/magneton.png";
            case FARFETCHD -> "0083_farfetchd/farfetchd.png";
            case DODUO -> "0084_doduo/doduo.png";
            case DODRIO -> "0085_dodrio/dodrio.png";
            case SEEL -> "0086_seel/seel.png";
            case DEWGONG -> "0087_dewgong/dewgong.png";
            case GRIMER -> "0088_grimer/grimer.png";
            case MUK -> "0089_muk/muk.png";
            case SHELLDER -> "0090_shellder/shellder.png";
            case CLOYSTER -> "0091_cloyster/cloyster.png";
            case GASTLY -> "0092_gastly/gastly.png";
            case HAUNTER -> "0093_haunter/haunter.png";
            case GENGAR -> "0094_gengar/gengar.png";
            case ONIX -> "0095_onix/onix.png";
            case DROWZEE -> "0096_drowzee/drowzee.png";
            case HYPNO -> "0097_hypno/hypno.png";
            case KRABBY -> "0098_krabby/krabby.png";
            case KINGLER -> "0099_kingler/kingler.png";
            case VOLTORB -> "0100_voltorb/voltorb.png";
            case ELECTRODE -> "0101_electrode/electrode.png";
            case EXEGGCUTE -> "0102_exeggcute/exeggcute.png";
            case EXEGGUTOR -> "0103_exeggutor/exeggutor.png";
            case CUBONE -> "0104_cubone/cubone.png";
            case MAROWAK -> "0105_marowak/marowak.png";
            case HITMONLEE -> "0106_hitmonlee/hitmonlee.png";
            case HITMONCHAN -> "0107_hitmonchan/hitmonchan.png";
            case LICKITUNG -> "0108_lickitung/lickitung.png";
            case KOFFING -> "0109_koffing/koffing.png";
            case WEEZING -> "0110_weezing/weezing.png";
            case RHYHORN -> "0111_rhyhorn/rhyhorn.png";
            case RHYDON -> "0112_rhydon/rhydon.png";
            case CHANSEY -> "0113_chansey/chansey.png";
            case TANGELA -> "0114_tangela/tangela.png";
            case KANGASKHAN -> "0115_kangaskhan/kangaskhan.png";
            case HORSEA -> "0116_horsea/horsea.png";
            case SEADRA -> "0117_seadra/seadra.png";
            case GOLDEEN -> "0118_goldeen/goldeen.png";
            case SEAKING -> "0119_seaking/seaking.png";
            case STARYU -> "0120_staryu/staryu.png";
            case STARMIE -> "0121_starmie/starmie.png";
            case MR_MIME -> "0122_mrmime/mr_mime.png";
            case SCYTHER -> "0123_scyther/scyther.png";
            case JYNX -> "0124_jynx/jynx.png";
            case ELECTABUZZ -> "0125_electabuzz/electabuzz.png";
            case MAGMAR -> "0126_magmar/magmar.png";
            case PINSIR -> "0127_pinsir/pinsir.png";
            case TAUROS -> "0128_tauros/tauros.png";
            case MAGIKARP -> "0129_magikarp/magikarp.png";
            case GYARADOS -> "0130_gyarados/gyarados.png";
            case LAPRAS -> "0131_lapras/lapras.png";
            case DITTO -> "0132_ditto/ditto.png";
            case EEVEE -> "0133_eevee/eevee.png";
            case VAPOREON -> "0134_vaporeon/vaporeon.png";
            case JOLTEON -> "0135_jolteon/jolteon.png";
            case FLAREON -> "0136_flareon/flareon.png";
            case PORYGON -> "0137_porygon/porygon.png";
            case OMANYTE -> "0138_omanyte/omanyte.png";
            case OMASTAR -> "0139_omastar/omastar.png";
            case KABUTO -> "0140_kabuto/kabuto.png";
            case KABUTOPS -> "0141_kabutops/kabutops.png";
            case AERODACTYL -> "0142_aerodactyl/aerodactyl.png";
            case SNORLAX -> "0143_snorlax/snorlax.png";
            case ARTICUNO -> "0144_articuno/articuno.png";
            case ZAPDOS -> "0145_zapdos/zapdos.png";
            case MOLTRES -> "0146_moltres/moltres.png";
            case DRATINI -> "0147_dratini/dratini.png";
            case DRAGONAIR -> "0148_dragonair/dragonair.png";
            case DRAGONITE -> "0149_dragonite/dragonite.png";
            case MEWTWO -> "0150_mewtwo/mewtwo.png";
            case MEW -> "0151_mew/mew.png";
        };
    }

    public static String geoPathFor(MonSpecies species) {
        return switch (species) {
            case BULBASAUR -> "0001_bulbasaur/bulbasaur.geo.json";
            case IVYSAUR -> "0002_ivysaur/ivysaur.geo.json";
            case VENUSAUR -> "0003_venusaur/venusaur_male.geo.json";
            case CHARMANDER -> "0004_charmander/charmander.geo.json";
            case CHARMELEON -> "0005_charmeleon/charmeleon.geo.json";
            case CHARIZARD -> "0006_charizard/charizard.geo.json";
            case SQUIRTLE -> "0007_squirtle/squirtle.geo.json";
            case WARTORTLE -> "0008_wartortle/wartortle.geo.json";
            case BLASTOISE -> "0009_blastoise/blastoise.geo.json";
            case CATERPIE -> "0010_caterpie/caterpie.geo.json";
            case METAPOD -> "0011_metapod/metapod.geo.json";
            case BUTTERFREE -> "0012_butterfree/butterfree_male.geo.json";
            case WEEDLE -> "0013_weedle/weedle.geo.json";
            case KAKUNA -> "0014_kakuna/kakuna.geo.json";
            case BEEDRILL -> "0015_beedrill/beedrill.geo.json";
            case PIDGEY -> "0016_pidgey/pidgey.geo.json";
            case PIDGEOTTO -> "0017_pidgeotto/pidgeotto.geo.json";
            case PIDGEOT -> "0018_pidgeot/pidgeot.geo.json";
            case RATTATA -> "0019_rattata/rattata_male.geo.json";
            case RATICATE -> "0020_raticate/raticate_male.geo.json";
            case SPEAROW -> "0021_spearow/spearow.geo.json";
            case FEAROW -> "0022_fearow/fearow.geo.json";
            case EKANS -> "0023_ekans/ekans.geo.json";
            case ARBOK -> "0024_arbok/arbok.geo.json";
            case PIKACHU -> "0025_pikachu/pikachu_male.geo.json";
            case RAICHU -> "0026_raichu/raichu_male.geo.json";
            case SANDSHREW -> "0027_sandshrew/sandshrew.geo.json";
            case SANDSLASH -> "0028_sandslash/sandslash.geo.json";
            case NIDORAN_F -> "0029_nidoranf/nidoranf.geo.json";
            case NIDORINA -> "0030_nidorina/nidorina.geo.json";
            case NIDOQUEEN -> "0031_nidoqueen/nidoqueen.geo.json";
            case NIDORAN_M -> "0032_nidoranm/nidoranm.geo.json";
            case NIDORINO -> "0033_nidorino/nidorino.geo.json";
            case NIDOKING -> "0034_nidoking/nidoking.geo.json";
            case CLEFAIRY -> "0035_clefairy/clefairy.geo.json";
            case CLEFABLE -> "0036_clefable/clefable.geo.json";
            case VULPIX -> "0037_vulpix/vulpix.geo.json";
            case NINETALES -> "0038_ninetales/ninetales.geo.json";
            case JIGGLYPUFF -> "0039_jigglypuff/jigglypuff.geo.json";
            case WIGGLYTUFF -> "0040_wigglytuff/wigglytuff.geo.json";
            case ZUBAT -> "0041_zubat/zubat_male.geo.json";
            case GOLBAT -> "0042_golbat/golbat_male.geo.json";
            case ODDISH -> "0043_oddish/oddish.geo.json";
            case GLOOM -> "0044_gloom/gloom_male.geo.json";
            case VILEPLUME -> "0045_vileplume/vileplume.geo.json";
            case PARAS -> "0046_paras/paras.geo.json";
            case PARASECT -> "0047_parasect/parasect.geo.json";
            case VENONAT -> "0048_venonat/venonat.geo.json";
            case VENOMOTH -> "0049_venomoth/venomoth.geo.json";
            case DIGLETT -> "0050_diglett/diglett.geo.json";
            case DUGTRIO -> "0051_dugtrio/dugtrio.geo.json";
            case MEOWTH -> "0052_meowth/meowth.geo.json";
            case PERSIAN -> "0053_persian/persian.geo.json";
            case PSYDUCK -> "0054_psyduck/psyduck.geo.json";
            case GOLDUCK -> "0055_golduck/golduck.geo.json";
            case MANKEY -> "0056_mankey/mankey.geo.json";
            case PRIMEAPE -> "0057_primeape/primeape.geo.json";
            case GROWLITHE -> "0058_growlithe/growlithe.geo.json";
            case ARCANINE -> "0059_arcanine/arcanine.geo.json";
            case POLIWAG -> "0060_poliwag/poliwag.geo.json";
            case POLIWHIRL -> "0061_poliwhirl/poliwhirl.geo.json";
            case POLIWRATH -> "0062_poliwrath/poliwrath.geo.json";
            case ABRA -> "0063_abra/abra.geo.json";
            case KADABRA -> "0064_kadabra/kadabra_male.geo.json";
            case ALAKAZAM -> "0065_alakazam/alakazam_male.geo.json";
            case MACHOP -> "0066_machop/machop.geo.json";
            case MACHOKE -> "0067_machoke/machoke.geo.json";
            case MACHAMP -> "0068_machamp/machamp.geo.json";
            case BELLSPROUT -> "0069_bellsprout/bellsprout.geo.json";
            case WEEPINBELL -> "0070_weepinbell/weepinbell.geo.json";
            case VICTREEBEL -> "0071_victreebel/victreebel.geo.json";
            case TENTACOOL -> "0072_tentacool/tentacool.geo.json";
            case TENTACRUEL -> "0073_tentacruel/tentacruel.geo.json";
            case GEODUDE -> "0074_geodude/geodude.geo.json";
            case GRAVELER -> "0075_graveler/graveler.geo.json";
            case GOLEM -> "0076_golem/golem.geo.json";
            case PONYTA -> "0077_ponyta/ponyta.geo.json";
            case RAPIDASH -> "0078_rapidash/rapidash.geo.json";
            case SLOWPOKE -> "0079_slowpoke/slowpoke.geo.json";
            case SLOWBRO -> "0080_slowbro/slowbro.geo.json";
            case MAGNEMITE -> "0081_magnemite/magnemite.geo.json";
            case MAGNETON -> "0082_magneton/magneton.geo.json";
            case FARFETCHD -> "0083_farfetchd/farfetchd.geo.json";
            case DODUO -> "0084_doduo/doduo_male.geo.json";
            case DODRIO -> "0085_dodrio/dodrio_male.geo.json";
            case SEEL -> "0086_seel/seel.geo.json";
            case DEWGONG -> "0087_dewgong/dewgong.geo.json";
            case GRIMER -> "0088_grimer/grimer.geo.json";
            case MUK -> "0089_muk/muk.geo.json";
            case SHELLDER -> "0090_shellder/shellder.geo.json";
            case CLOYSTER -> "0091_cloyster/cloyster.geo.json";
            case GASTLY -> "0092_gastly/gastly.geo.json";
            case HAUNTER -> "0093_haunter/haunter.geo.json";
            case GENGAR -> "0094_gengar/gengar.geo.json";
            case ONIX -> "0095_onix/onix.geo.json";
            case DROWZEE -> "0096_drowzee/drowzee.geo.json";
            case HYPNO -> "0097_hypno/hypno_male.geo.json";
            case KRABBY -> "0098_krabby/krabby.geo.json";
            case KINGLER -> "0099_kingler/kingler.geo.json";
            case VOLTORB -> "0100_voltorb/voltorb.geo.json";
            case ELECTRODE -> "0101_electrode/electrode.geo.json";
            case EXEGGCUTE -> "0102_exeggcute/exeggcute.geo.json";
            case EXEGGUTOR -> "0103_exeggutor/exeggutor.geo.json";
            case CUBONE -> "0104_cubone/cubone.geo.json";
            case MAROWAK -> "0105_marowak/marowak.geo.json";
            case HITMONLEE -> "0106_hitmonlee/hitmonlee.geo.json";
            case HITMONCHAN -> "0107_hitmonchan/hitmonchan.geo.json";
            case LICKITUNG -> "0108_lickitung/lickitung.geo.json";
            case KOFFING -> "0109_koffing/koffing.geo.json";
            case WEEZING -> "0110_weezing/weezing.geo.json";
            case RHYHORN -> "0111_rhyhorn/rhyhorn_male.geo.json";
            case RHYDON -> "0112_rhydon/rhydon_male.geo.json";
            case CHANSEY -> "0113_chansey/chansey.geo.json";
            case TANGELA -> "0114_tangela/tangela.geo.json";
            case KANGASKHAN -> "0115_kangaskhan/kangaskhan.geo.json";
            case HORSEA -> "0116_horsea/horsea.geo.json";
            case SEADRA -> "0117_seadra/seadra.geo.json";
            case GOLDEEN -> "0118_goldeen/goldeen_male.geo.json";
            case SEAKING -> "0119_seaking/seaking_male.geo.json";
            case STARYU -> "0120_staryu/staryu.geo.json";
            case STARMIE -> "0121_starmie/starmie.geo.json";
            case MR_MIME -> "0122_mrmime/mr_mime.geo.json";
            case SCYTHER -> "0123_scyther/scyther_male.geo.json";
            case JYNX -> "0124_jynx/jynx.geo.json";
            case ELECTABUZZ -> "0125_electabuzz/electabuzz.geo.json";
            case MAGMAR -> "0126_magmar/magmar.geo.json";
            case PINSIR -> "0127_pinsir/pinsir.geo.json";
            case TAUROS -> "0128_tauros/tauros.geo.json";
            case MAGIKARP -> "0129_magikarp/magikarp_male.geo.json";
            case GYARADOS -> "0130_gyarados/gyarados_male.geo.json";
            case LAPRAS -> "0131_lapras/lapras.geo.json";
            case DITTO -> "0132_ditto/ditto.geo.json";
            case EEVEE -> "0133_eevee/eevee_male.geo.json";
            case VAPOREON -> "0134_vaporeon/vaporeon.geo.json";
            case JOLTEON -> "0135_jolteon/jolteon.geo.json";
            case FLAREON -> "0136_flareon/flareon.geo.json";
            case PORYGON -> "0137_porygon/porygon.geo.json";
            case OMANYTE -> "0138_omanyte/omanyte.geo.json";
            case OMASTAR -> "0139_omastar/omastar.geo.json";
            case KABUTO -> "0140_kabuto/kabuto.geo.json";
            case KABUTOPS -> "0141_kabutops/kabutops.geo.json";
            case AERODACTYL -> "0142_aerodactyl/aerodactyl.geo.json";
            case SNORLAX -> "0143_snorlax/snorlax.geo.json";
            case ARTICUNO -> "0144_articuno/articuno.geo.json";
            case ZAPDOS -> "0145_zapdos/zapdos.geo.json";
            case MOLTRES -> "0146_moltres/moltres.geo.json";
            case DRATINI -> "0147_dratini/dratini.geo.json";
            case DRAGONAIR -> "0148_dragonair/dragonair.geo.json";
            case DRAGONITE -> "0149_dragonite/dragonite.geo.json";
            case MEWTWO -> "0150_mewtwo/mewtwo.geo.json";
            case MEW -> "0151_mew/mew.geo.json";
        };
    }

    public static Identifier shinyTexture(MonSpecies species) {
        String path = pathFor(species);
        String shiny = path.replace(".png", "_shiny.png");
        return Identifier.fromNamespaceAndPath(Cobblemon.MOD_ID, "textures/pokemon/" + shiny);
    }
}

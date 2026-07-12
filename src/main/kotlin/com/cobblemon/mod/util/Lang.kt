package com.cobblemon.mod.util

import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent

/**
 * Official-style translation helpers (from Cobblemon `LocalizationUtilsKt`).
 *
 * Prefixes:
 * - [lang] → `cobblemon.<subKey>`
 * - [commandLang] → `cobblemon.command.<subKey>`
 * - [battleLang] → `cobblemon.battle.<subKey>`
 */
object Lang {
    @JvmStatic
    fun lang(subKey: String, vararg args: Any): MutableComponent =
        Component.translatable("cobblemon.$subKey", *args)

    @JvmStatic
    fun commandLang(subKey: String, vararg args: Any): MutableComponent =
        lang("command.$subKey", *args)

    @JvmStatic
    fun battleLang(subKey: String, vararg args: Any): MutableComponent =
        lang("battle.$subKey", *args)

    @JvmStatic
    fun ui(subKey: String, vararg args: Any): MutableComponent =
        lang("ui.$subKey", *args)

    @JvmStatic
    fun speciesName(speciesId: String): MutableComponent =
        Component.translatable(CobblemonResource.speciesLangKey(speciesId))

    @JvmStatic
    fun speciesDesc(speciesId: String): MutableComponent =
        Component.translatable(CobblemonResource.speciesDescKey(speciesId))

    @JvmStatic
    fun moveName(moveId: String): MutableComponent =
        Component.translatable(CobblemonResource.moveLangKey(moveId))

    @JvmStatic
    fun asTranslated(key: String, vararg args: Any): MutableComponent =
        Component.translatable(key, *args)
}

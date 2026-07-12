package com.cobblemon.mod.util

import com.cobblemon.mod.Cobblemon
import net.minecraft.resources.Identifier

/**
 * Official-style ID helpers (from Cobblemon `MiscUtilsKt.cobblemonResource`).
 * MC 26.2 uses [Identifier] (formerly ResourceLocation).
 */
object CobblemonResource {
    @JvmStatic
    fun id(path: String): Identifier =
        Identifier.fromNamespaceAndPath(Cobblemon.MOD_ID, path)

    @JvmStatic
    fun texture(path: String): Identifier = id(path)

    @JvmStatic
    fun guiTexture(path: String): Identifier = id("textures/gui/$path")

    @JvmStatic
    fun speciesLangKey(speciesId: String): String = "cobblemon.species.$speciesId.name"

    @JvmStatic
    fun speciesDescKey(speciesId: String): String = "cobblemon.species.$speciesId.desc"

    @JvmStatic
    fun moveLangKey(moveId: String): String = "cobblemon.move.$moveId"
}

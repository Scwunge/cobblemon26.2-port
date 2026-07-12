package com.cobblemon.mod.species

import com.cobblemon.mod.Cobblemon
import com.google.gson.Gson
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.concurrent.atomic.AtomicReference

/**
 * Official Cobblemon-style starter categories (regions) and their three starters each.
 *
 * Loads optional `data/cobblemon/starter_config.json` from the classpath; falls back to
 * built-in regional lists if missing or invalid.
 */
object StarterCatalog {
    /** @JvmRecord so Java callers can use record-style accessors (id(), displayName(), …). */
    @JvmRecord
    data class Category(
        val id: String,
        val displayName: String,
        val speciesIds: List<String>,
    ) {
        fun handles(): List<SpeciesHandle> = speciesIds.map { SpeciesHandle.of(it) }
    }

    private data class ConfigDto(
        val allowStarterOnJoin: Boolean = true,
        val promptStarterOnceOnly: Boolean = true,
        val categories: List<CategoryDto> = emptyList(),
    )

    private data class CategoryDto(
        val id: String = "",
        val displayName: String = "",
        val species: List<String> = emptyList(),
    )

    private val gson = Gson()
    private val state = AtomicReference(loadDefaults())

    // Not @JvmField — custom private setter is incompatible with @JvmField.
    var CATEGORIES: List<Category> = state.get().categories
        private set

    private var allowOnJoin: Boolean = true
    private var promptOnceOnly: Boolean = true
    private var allIds: Set<String> = emptySet()

    init {
        applyState(state.get())
    }

    private data class Loaded(
        val allowOnJoin: Boolean,
        val promptOnceOnly: Boolean,
        val categories: List<Category>,
    )

    private fun applyState(loaded: Loaded) {
        allowOnJoin = loaded.allowOnJoin
        promptOnceOnly = loaded.promptOnceOnly
        CATEGORIES = loaded.categories
        allIds = loaded.categories.flatMap { it.speciesIds }.map { it.lowercase(Locale.ROOT) }.toSet()
    }

    private fun loadDefaults(): Loaded = Loaded(
        allowOnJoin = true,
        promptOnceOnly = true,
        categories = listOf(
            cat("kanto", "Kanto", "bulbasaur", "charmander", "squirtle"),
            cat("johto", "Johto", "chikorita", "cyndaquil", "totodile"),
            cat("hoenn", "Hoenn", "treecko", "torchic", "mudkip"),
            cat("sinnoh", "Sinnoh", "turtwig", "chimchar", "piplup"),
            cat("unova", "Unova", "snivy", "tepig", "oshawott"),
            cat("kalos", "Kalos", "chespin", "fennekin", "froakie"),
            cat("alola", "Alola", "rowlet", "litten", "popplio"),
            cat("galar", "Galar", "grookey", "scorbunny", "sobble"),
            cat("hisui", "Hisui", "rowlet", "cyndaquil", "oshawott"),
            cat("paldea", "Paldea", "sprigatito", "fuecoco", "quaxly"),
        ),
    )

    private fun cat(id: String, name: String, a: String, b: String, c: String) =
        Category(id, name, listOf(a, b, c))

    /**
     * Reload from classpath JSON. Safe to call from common setup.
     */
    @JvmStatic
    fun bootstrap() {
        val loaded = tryLoadJson() ?: loadDefaults()
        state.set(loaded)
        applyState(loaded)
        Cobblemon.LOGGER.info(
            "StarterCatalog: {} categories ({} starters) allowOnJoin={} promptOnce={}",
            loaded.categories.size,
            allIds.size,
            loaded.allowOnJoin,
            loaded.promptOnceOnly,
        )
    }

    private fun tryLoadJson(): Loaded? {
        return try {
            val stream = StarterCatalog::class.java.getResourceAsStream("/data/cobblemon/starter_config.json")
                ?: return null
            stream.use { raw ->
                val dto = gson.fromJson(InputStreamReader(raw, StandardCharsets.UTF_8), ConfigDto::class.java)
                    ?: return null
                val cats = dto.categories.mapNotNull { c ->
                    val id = c.id.trim().lowercase(Locale.ROOT)
                    if (id.isEmpty()) return@mapNotNull null
                    val species = c.species.map { it.trim().lowercase(Locale.ROOT) }.filter { it.isNotEmpty() }
                    if (species.isEmpty()) return@mapNotNull null
                    val name = c.displayName.ifBlank { id.replaceFirstChar { ch -> ch.uppercase() } }
                    Category(id, name, species)
                }
                if (cats.isEmpty()) return null
                Loaded(dto.allowStarterOnJoin, dto.promptStarterOnceOnly, cats)
            }
        } catch (t: Throwable) {
            Cobblemon.LOGGER.warn("StarterCatalog: failed to load starter_config.json — using defaults", t)
            null
        }
    }

    @JvmStatic
    fun categories(): List<Category> = CATEGORIES

    @JvmStatic
    fun allowStarterOnJoin(): Boolean = allowOnJoin

    @JvmStatic
    fun promptStarterOnceOnly(): Boolean = promptOnceOnly

    @JvmStatic
    fun isValidStarter(speciesId: String?): Boolean {
        if (speciesId.isNullOrBlank()) return false
        return speciesId.trim().lowercase(Locale.ROOT) in allIds
    }

    @JvmStatic
    fun asMap(): Map<String, List<String>> =
        CATEGORIES.associate { it.id to it.speciesIds }
}

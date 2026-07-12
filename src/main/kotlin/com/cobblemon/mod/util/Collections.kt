package com.cobblemon.mod.util

import java.util.concurrent.ThreadLocalRandom
import kotlin.random.Random

/**
 * Collection helpers ported from Cobblemon `CollectionUtilsKt`.
 * Java-friendly overloads use [ThreadLocalRandom] when no RNG is supplied.
 */
object Collections {
    @JvmStatic
    fun <T> swap(list: MutableList<T>, i: Int, j: Int) {
        val tmp = list[i]
        list[i] = list[j]
        list[j] = tmp
    }

    /**
     * Weighted random pick. [weightOf] must return a non-negative weight.
     * Returns null if the iterable is empty or total weight is zero.
     */
    @JvmStatic
    fun <T> weightedSelection(
        items: Iterable<T>,
        random: Random,
        weightOf: (T) -> Number,
    ): T? {
        val list = items.toList()
        if (list.isEmpty()) return null
        var total = 0.0
        val weights = DoubleArray(list.size)
        for (i in list.indices) {
            val w = weightOf(list[i]).toDouble().coerceAtLeast(0.0)
            weights[i] = w
            total += w
        }
        if (total <= 0.0) return null
        var roll = random.nextDouble() * total
        for (i in list.indices) {
            roll -= weights[i]
            if (roll <= 0.0) return list[i]
        }
        return list.last()
    }

    /** Java-friendly: weight via [java.util.function.ToDoubleFunction], system RNG. */
    @JvmStatic
    fun <T> weightedSelection(
        items: Iterable<T>,
        weightOf: java.util.function.ToDoubleFunction<T>,
    ): T? = weightedSelection(items, Random(ThreadLocalRandom.current().nextLong())) { weightOf.applyAsDouble(it) }

    @JvmStatic
    fun <T> weightedSelection(
        items: Iterable<T>,
        javaRandom: java.util.Random,
        weightOf: java.util.function.ToDoubleFunction<T>,
    ): T? = weightedSelection(items, Random(javaRandom.nextLong())) { weightOf.applyAsDouble(it) }
}

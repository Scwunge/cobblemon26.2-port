package com.cobblemon.mod.molang

/**
 * A MoLang script loaded from classpath resources under `data/cobblemon/molang/`.
 *
 * [lastEval] is set when a simple expression subset could be evaluated; otherwise null
 * (full Bedrock/Cobblemon MoLang is not implemented — see KOTLIN.md).
 */
data class MoLangScript(
    val path: String,
    val source: String,
    val lastEval: Double? = null,
)

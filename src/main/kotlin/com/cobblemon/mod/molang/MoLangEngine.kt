package com.cobblemon.mod.molang

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * Tiny pure-Kotlin MoLang expression evaluator (scaffold).
 *
 * Supports numbers, `+ - * /`, parentheses, comparisons, `&&` `||` `!`,
 * dotted variable keys (`q.grounded`, `variable.x`) from [context], and a few
 * `math.*` helpers. Not a full Bedrock MoLang / bedrockk runtime.
 *
 * Booleans are MoLang-style doubles: true = 1.0, false = 0.0.
 */
object MoLangEngine {

    /**
     * Evaluate [expression] with optional variable [context].
     * Returns null on parse/eval failure (never throws to callers).
     */
    @JvmStatic
    @JvmOverloads
    fun eval(expression: String, context: Map<String, Double> = emptyMap()): Double? {
        return try {
            val parser = Parser(expression, context)
            val value = parser.parseExpression()
            parser.skipWs()
            if (!parser.eof()) {
                // Trailing junk (e.g. assignment / statements) — not a pure expression.
                null
            } else {
                value
            }
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * Try the first non-empty line (strip trailing `;`), then the whole source
     * if it looks like a single simple expression.
     */
    @JvmStatic
    @JvmOverloads
    fun tryEvalScript(source: String, context: Map<String, Double> = emptyMap()): Double? {
        val lines = source.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("//") }
            .toList()
        if (lines.isEmpty()) return null

        val first = lines.first().removeSuffix(";").trim()
        eval(first, context)?.let { return it }

        val whole = source.trim().removeSuffix(";").trim()
        if (whole.lines().size == 1) {
            return eval(whole, context)
        }
        return null
    }

    // ── tokenizer + recursive-descent parser ─────────────────────────────

    private class Parser(
        private val input: String,
        private val context: Map<String, Double>,
    ) {
        private var i = 0

        fun eof(): Boolean = i >= input.length

        fun skipWs() {
            while (i < input.length && input[i].isWhitespace()) i++
        }

        fun parseExpression(): Double = parseOr()

        private fun parseOr(): Double {
            var left = parseAnd()
            while (true) {
                skipWs()
                if (matchOp("||")) {
                    val right = parseAnd()
                    left = if (isTruthy(left) || isTruthy(right)) 1.0 else 0.0
                } else break
            }
            return left
        }

        private fun parseAnd(): Double {
            var left = parseCmp()
            while (true) {
                skipWs()
                if (matchOp("&&")) {
                    val right = parseCmp()
                    left = if (isTruthy(left) && isTruthy(right)) 1.0 else 0.0
                } else break
            }
            return left
        }

        private fun parseCmp(): Double {
            var left = parseAdd()
            while (true) {
                skipWs()
                when {
                    matchOp("==") -> left = if (nearlyEqual(left, parseAdd())) 1.0 else 0.0
                    matchOp("!=") -> left = if (!nearlyEqual(left, parseAdd())) 1.0 else 0.0
                    matchOp(">=") -> left = if (left >= parseAdd()) 1.0 else 0.0
                    matchOp("<=") -> left = if (left <= parseAdd()) 1.0 else 0.0
                    matchOp(">") -> left = if (left > parseAdd()) 1.0 else 0.0
                    matchOp("<") -> left = if (left < parseAdd()) 1.0 else 0.0
                    else -> break
                }
            }
            return left
        }

        private fun parseAdd(): Double {
            var left = parseMul()
            while (true) {
                skipWs()
                when {
                    matchChar('+') -> left += parseMul()
                    matchChar('-') -> left -= parseMul()
                    else -> break
                }
            }
            return left
        }

        private fun parseMul(): Double {
            var left = parseUnary()
            while (true) {
                skipWs()
                when {
                    matchChar('*') -> left *= parseUnary()
                    matchChar('/') -> {
                        val r = parseUnary()
                        left = if (r == 0.0) 0.0 else left / r
                    }
                    else -> break
                }
            }
            return left
        }

        private fun parseUnary(): Double {
            skipWs()
            return when {
                matchChar('!') -> if (isTruthy(parseUnary())) 0.0 else 1.0
                matchChar('+') -> parseUnary()
                matchChar('-') -> -parseUnary()
                else -> parsePrimary()
            }
        }

        private fun parsePrimary(): Double {
            skipWs()
            if (eof()) error("unexpected end")

            if (matchChar('(')) {
                val v = parseExpression()
                skipWs()
                if (!matchChar(')')) error("expected )")
                return v
            }

            // number
            if (peek().isDigit() || (peek() == '.' && i + 1 < input.length && input[i + 1].isDigit())) {
                return parseNumber()
            }

            // string literal (allowed only as function args; standalone → 0)
            if (peek() == '\'' || peek() == '"') {
                parseString()
                return 0.0
            }

            // identifier / dotted path / call
            if (peek().isLetter() || peek() == '_') {
                return parseIdentOrCall()
            }

            error("unexpected '${peek()}'")
        }

        private fun parseIdentOrCall(): Double {
            val name = parseDottedName()
            skipWs()
            if (matchChar('(')) {
                val args = mutableListOf<Double>()
                skipWs()
                if (!matchChar(')')) {
                    while (true) {
                        skipWs()
                        // allow string args (ignored as 0 for numeric math)
                        if (peek() == '\'' || peek() == '"') {
                            parseString()
                            args.add(0.0)
                        } else {
                            args.add(parseExpression())
                        }
                        skipWs()
                        if (matchChar(')')) break
                        if (!matchChar(',')) error("expected , or )")
                    }
                }
                return callBuiltin(name, args)
            }

            // boolean-ish literals
            when (name.lowercase()) {
                "true" -> return 1.0
                "false" -> return 0.0
            }

            return context[name]
                ?: context[name.lowercase()]
                ?: 0.0
        }

        private fun parseDottedName(): String {
            val sb = StringBuilder()
            while (!eof()) {
                val c = peek()
                if (c.isLetterOrDigit() || c == '_' || c == '.') {
                    sb.append(c)
                    i++
                } else break
            }
            // trim trailing dots
            return sb.toString().trimEnd('.')
        }

        private fun parseNumber(): Double {
            val start = i
            while (!eof() && peek().isDigit()) i++
            if (!eof() && peek() == '.') {
                i++
                while (!eof() && peek().isDigit()) i++
            }
            // scientific notation (rare in molang but cheap)
            if (!eof() && (peek() == 'e' || peek() == 'E')) {
                i++
                if (!eof() && (peek() == '+' || peek() == '-')) i++
                while (!eof() && peek().isDigit()) i++
            }
            return input.substring(start, i).toDouble()
        }

        private fun parseString() {
            val quote = peek()
            i++ // open
            while (!eof() && peek() != quote) {
                if (peek() == '\\' && i + 1 < input.length) i += 2 else i++
            }
            if (!eof()) i++ // close
        }

        private fun callBuiltin(name: String, args: List<Double>): Double {
            val n = name.lowercase()
            return when (n) {
                "math.random", "random" -> {
                    val a = args.getOrElse(0) { 0.0 }
                    val b = args.getOrElse(1) { 1.0 }
                    val lo = min(a, b)
                    val hi = max(a, b)
                    if (hi == lo) lo else lo + Random.nextDouble() * (hi - lo)
                }
                "math.mod", "mod" -> {
                    val a = args.getOrElse(0) { 0.0 }
                    val b = args.getOrElse(1) { 1.0 }
                    if (b == 0.0) 0.0 else a % b
                }
                "math.abs", "abs" -> abs(args.getOrElse(0) { 0.0 })
                "math.min", "min" -> {
                    if (args.isEmpty()) 0.0 else args.reduce { x, y -> min(x, y) }
                }
                "math.max", "max" -> {
                    if (args.isEmpty()) 0.0 else args.reduce { x, y -> max(x, y) }
                }
                "math.floor", "floor" -> kotlin.math.floor(args.getOrElse(0) { 0.0 })
                "math.ceil", "ceil" -> kotlin.math.ceil(args.getOrElse(0) { 0.0 })
                "math.round", "round" -> kotlin.math.round(args.getOrElse(0) { 0.0 })
                "math.sqrt", "sqrt" -> {
                    val v = args.getOrElse(0) { 0.0 }
                    if (v < 0) 0.0 else kotlin.math.sqrt(v)
                }
                "math.clamp", "clamp" -> {
                    val v = args.getOrElse(0) { 0.0 }
                    val lo = args.getOrElse(1) { 0.0 }
                    val hi = args.getOrElse(2) { 1.0 }
                    min(max(v, lo), hi)
                }
                else -> 0.0 // unknown query/function — soft zero
            }
        }

        private fun peek(): Char = if (i < input.length) input[i] else '\u0000'

        private fun matchChar(c: Char): Boolean {
            skipWs()
            if (!eof() && input[i] == c) {
                i++
                return true
            }
            return false
        }

        /** Match multi-char operator only if not a longer prefix (e.g. `>` vs `>=`). */
        private fun matchOp(op: String): Boolean {
            skipWs()
            if (i + op.length > input.length) return false
            if (!input.regionMatches(i, op, 0, op.length)) return false
            // For single-char ops that have a two-char form, already ordered by longer-first callers
            i += op.length
            return true
        }

        private fun error(msg: String): Nothing = throw IllegalArgumentException(msg)
    }

    private fun isTruthy(v: Double): Boolean = abs(v) > 1e-9

    private fun nearlyEqual(a: Double, b: Double): Boolean = abs(a - b) < 1e-9
}

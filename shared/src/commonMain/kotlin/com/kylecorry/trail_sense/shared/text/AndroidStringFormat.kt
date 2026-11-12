package com.kylecorry.trail_sense.shared.text

import com.kylecorry.andromeda.core.math.DecimalFormatter

/**
 * Fills a template written with Android string placeholders, as Android's getString(id, args) would:
 * %s and %S (text), %d (a whole number, in the locale's digits), %f and %.Nf (a decimal), each optionally
 * positional (%2$s), plus %% and %n. An unknown placeholder or a missing argument is left as written.
 */
object AndroidStringFormat {

    private val placeholder = Regex("""%(?:(\d+)\$)?(\.\d+)?([sSdf%n])""")

    fun format(template: String, vararg args: Any?): String {
        if (!template.contains('%')) return template
        var nextIndex = 0
        return placeholder.replace(template) { match ->
            val conversion = match.groupValues[3]
            when (conversion) {
                "%" -> return@replace "%"
                "n" -> return@replace "\n"
            }
            val index = match.groupValues[1].toIntOrNull()?.minus(1) ?: nextIndex++
            if (index !in args.indices) return@replace match.value
            val arg = args[index]
            val precision = match.groupValues[2].drop(1).toIntOrNull()
            when (conversion) {
                "s" -> arg.toString()
                "S" -> arg.toString().uppercase()
                "d" -> (arg as? Number)?.let { DecimalFormatter.format(it.toLong(), 0) } ?: match.value
                "f" -> (arg as? Number)?.let { DecimalFormatter.format(it.toDouble(), precision ?: 6) }
                    ?: match.value

                else -> match.value
            }
        }
    }
}

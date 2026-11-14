package com.kylecorry.trail_sense.tools.tools.infrastructure

import com.kylecorry.trail_sense.shared.text.RawTextReader
import com.kylecorry.trail_sense.shared.text.RawTexts

object ToolKeywordLoader {

    suspend fun load(reader: RawTextReader): Map<Long, Set<String>> {
        return parse(reader.read(RawTexts.tool_keywords))
    }

    /**
     * Reads lines of the form `ID (Tool Name): keyword1, keyword2, keyword3`.
     */
    fun parse(text: String): Map<Long, Set<String>> {
        val map = mutableMapOf<Long, Set<String>>()

        for (line in text.lines()) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue

            val colonIndex = trimmed.indexOf(':')
            if (colonIndex == -1) continue

            val prefix = trimmed.substring(0, colonIndex).trim()
            val id = prefix.substringBefore('(').trim().toLongOrNull() ?: continue

            val keywords = trimmed.substring(colonIndex + 1)
                .split(',')
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .toSet()

            map[id] = keywords
        }

        return map
    }
}

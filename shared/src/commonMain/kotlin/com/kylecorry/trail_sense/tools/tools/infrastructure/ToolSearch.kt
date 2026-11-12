package com.kylecorry.trail_sense.tools.tools.infrastructure

import com.kylecorry.trail_sense.shared.text.search.AggregateSearchStrategy
import com.kylecorry.trail_sense.shared.text.search.KeywordMatchSearchStrategy
import com.kylecorry.trail_sense.shared.text.search.MultilingualFuzzySearchStrategy
import com.kylecorry.trail_sense.shared.text.search.SearchItem
import com.kylecorry.trail_sense.shared.text.search.TextMatchStrategy
import com.kylecorry.trail_sense.shared.text.search.TitleMatchSearchStrategy

class ToolSearch<T : ToolDescriptor>(
    private val tools: List<T>,
    private val keywords: Map<Long, Set<String>>
) {

    private val strategy = AggregateSearchStrategy(
        TitleMatchSearchStrategy(TextMatchStrategy.StartsWith) to 0.5f,
        TitleMatchSearchStrategy(TextMatchStrategy.Contains) to 0.2f,
        KeywordMatchSearchStrategy() to 0.2f,
        MultilingualFuzzySearchStrategy() to 0.1f
    )

    fun search(query: String): List<T> {
        if (query.isBlank()) return emptyList()

        val scored = tools.mapNotNull { tool ->
            val item = SearchItem(
                tool.id.toString(),
                tool.name,
                keywords[tool.id] ?: emptySet()
            )
            val score = strategy.getSearchScore(query, item)
            if (score > 0f) tool to score else null
        }.sortedByDescending { it.second }

        return scored
            .filter { it.second > 0.1f }
            .map { it.first }
    }
}

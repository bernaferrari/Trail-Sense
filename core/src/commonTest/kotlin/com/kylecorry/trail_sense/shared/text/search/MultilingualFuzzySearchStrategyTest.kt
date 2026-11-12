package com.kylecorry.trail_sense.shared.text.search

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MultilingualFuzzySearchStrategyTest {

    private val strategy = MultilingualFuzzySearchStrategy()

    @Test
    fun boostsExactEnglishAndChineseTitleMatches() {
        val cases = listOf(
            "Navigation" to "Navigation",
            "NAVIGATION" to "Navigation",
            "导航" to "导航"
        )
        for ((query, title) in cases) {
            val item = SearchItem("navigation", title)
            assertEquals(1.1f, strategy.getSearchScore(query, item), 0.001f, "$query vs $title")
        }
    }

    @Test
    fun returnsAnUnboostedScoreForPartialChineseTitleMatches() {
        val item = SearchItem("navigation", "导航 工具")

        assertEquals(1f, strategy.getSearchScore("导航", item), 0.001f)
    }

    @Test
    fun returnsAnExactScoreForKeywordMatchWhenTitleDoesNotMatch() {
        val item = SearchItem(
            id = "navigation",
            title = "Compass",
            keywords = setOf("navigation")
        )

        assertEquals(1f, strategy.getSearchScore("navigation", item), 0.001f)
    }

    @Test
    fun givesAPositiveButUnboostedScoreForFuzzyEnglishMatch() {
        val item = SearchItem("navigation", "Navigation")
        val score = strategy.getSearchScore("navigtion", item)

        assertTrue(score > 0f)
        assertTrue(score < 1f)
    }

    @Test
    fun boostsKeywordScoreWhenParentTitleMatches() {
        val itemWithoutParent = SearchItem(
            id = "compass",
            title = "Compass",
            keywords = setOf("compass")
        )
        val itemWithMatchingParent = SearchItem(
            id = "compass",
            title = "Compass",
            keywords = setOf("compass"),
            parent = SearchItem("navigation", "Navigation")
        )

        val scoreWithoutParent = strategy.getSearchScore("navigation", itemWithoutParent)
        val scoreWithParent = strategy.getSearchScore("navigation", itemWithMatchingParent)

        assertTrue(scoreWithParent > scoreWithoutParent)
    }

    @Test
    fun returnsZeroWhenTheTokenizerCannotProduceTokens() {
        for (text in listOf("", "🧭", "---", "   ")) {
            val item = SearchItem("compass", text)
            val score = strategy.getSearchScore(text, item)

            assertFalse(score.isNaN(), "'$text'")
            assertEquals(0f, score, 0.001f, "'$text'")
        }
    }
}

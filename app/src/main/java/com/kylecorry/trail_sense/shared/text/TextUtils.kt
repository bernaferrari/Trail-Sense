package com.kylecorry.trail_sense.shared.text

import android.content.Context
import androidx.annotation.RawRes
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.markdown.MarkdownService
import com.kylecorry.trail_sense.shared.text.nlp.processors.EnglishContractionSplitter
import com.kylecorry.trail_sense.shared.text.nlp.processors.EnglishStopWordRemover
import com.kylecorry.trail_sense.shared.text.nlp.processors.LowercaseProcessor
import com.kylecorry.trail_sense.shared.text.nlp.processors.PorterStemmer
import com.kylecorry.trail_sense.shared.text.nlp.processors.SequentialProcessor
import com.kylecorry.trail_sense.shared.text.nlp.tokenizers.PostProcessedTokenizer
import com.kylecorry.trail_sense.shared.text.nlp.tokenizers.SimpleWordTokenizer
import com.kylecorry.trail_sense.shared.text.search.TextMatchStrategy

object TextUtils {

    fun <T> search(query: String, values: List<T>, propertySelector: (T) -> List<String>): List<T> {
        val modifiedQuery = query.trim().lowercase()
        return values.filter { value ->
            propertySelector(value).any { it.lowercase().contains(modifiedQuery) }
        }
    }

    fun getSections(markdown: String): List<TextSection> {
        val sections = mutableListOf<TextSection>()
        val lines = markdown.split("\n")
        var currentContent = ""
        var currentTitle: String? = null
        var currentLevel: Int? = null
        for (line in lines) {
            if (line.startsWith("#")) {
                if (currentTitle != null || currentContent.isNotBlank()) {
                    sections.add(TextSection(currentTitle, currentLevel, currentContent.trim()))
                }
                currentLevel = line.count { it == '#' }
                currentTitle = line.substringAfter("#".repeat(currentLevel)).trim()
                currentContent = ""
            } else {
                currentContent += line + "\n"
            }
        }

        if (currentTitle != null || currentContent.isNotBlank()) {
            sections.add(TextSection(currentTitle, currentLevel, currentContent.trim()))
        }

        return sections
    }

    /**
     * Groups sections by their level. Nested sections are grouped together.
     */
    fun groupSections(sections: List<TextSection>, splitLevel: Int?): List<List<TextSection>> {
        val grouped = mutableListOf<MutableList<TextSection>>()
        var currentGroup = mutableListOf<TextSection>()
        for (section in sections) {
            val first = currentGroup.firstOrNull()
            val isLowerLevel = if (splitLevel != null) {
                section.level == null || splitLevel < section.level
            } else if (first?.level == null) {
                false
            } else {
                section.level == null || first.level < section.level
            }


            if (currentGroup.isEmpty() || isLowerLevel) {
                currentGroup.add(section)
            } else {
                grouped.add(currentGroup)
                currentGroup = mutableListOf(section)
            }
        }

        if (currentGroup.isNotEmpty()) {
            grouped.add(currentGroup)
        }

        return grouped
    }

    fun loadTextFromResources(context: Context, @RawRes resource: Int): String {
        return context.resources.openRawResource(resource).use {
            it.bufferedReader().readText()
        }
    }

    fun getMarkdownKeywords(text: String): Set<String> {
        val regex = Regex("<!-- K: (.*?)-->")
        return regex.findAll(text)
            .take(1)
            .flatMap { it.groupValues[1].split(",") }
            .map { it.trim() }
            .toSet()
    }

    fun getMarkdownSummary(text: String): String? {
        val regex = Regex("<!-- S: (.*?)-->")
        return regex.find(text)?.groupValues?.get(1)
    }

    fun getKeywords(
        text: String,
        preservedWords: Set<String> = emptySet(),
        additionalContractions: Map<String, List<String>> = emptyMap(),
        additionalStopWords: Set<String> = emptySet(),
        additionalStemWords: Map<String, String> = emptyMap()
    ): Set<String> {
        val tokenizer = PostProcessedTokenizer(
            SimpleWordTokenizer(preservedWords),
            SequentialProcessor(
                LowercaseProcessor(),
                EnglishContractionSplitter(additionalContractions),
                EnglishStopWordRemover(additionalStopWords),
                PorterStemmer(additionalStemWords + preservedWords.associateWith { it })
            )
        )
        return tokenizer.tokenize(text).toSet()
    }

    fun isMatch(
        text: String,
        query: String,
        strategy: TextMatchStrategy,
        ignoreCase: Boolean = true
    ): Boolean {
        return when (strategy) {
            TextMatchStrategy.Contains -> text.contains(query, ignoreCase = ignoreCase)
            TextMatchStrategy.StartsWith -> text.startsWith(query, ignoreCase = ignoreCase)
            TextMatchStrategy.Equals -> text.equals(query, ignoreCase = ignoreCase)
        }
    }

    fun getQueryMatchPercent(
        query: String,
        text: String,
        preservedWords: Set<String> = emptySet(),
        synonyms: List<Set<String>> = emptyList(),
        additionalContractions: Map<String, List<String>> = emptyMap(),
        additionalStopWords: Set<String> = emptySet(),
        additionalStemWords: Map<String, String> = emptyMap()
    ): Float {
        val queryKeywords =
            getKeywords(
                query,
                preservedWords,
                additionalContractions,
                additionalStopWords,
                additionalStemWords
            )
        val textKeywords =
            getKeywords(
                text,
                preservedWords,
                additionalContractions,
                additionalStopWords,
                additionalStemWords
            ).toMutableSet()

        // Add the synonyms to the text keywords (but not query keywords, since that will skew the results)
        val toAdd = mutableSetOf<String>()
        for (keyword in textKeywords) {
            for (synonymSet in synonyms) {
                if (keyword in synonymSet) {
                    toAdd.addAll(synonymSet)
                }
            }
        }
        textKeywords.addAll(toAdd)

        val distanceMetric = LevenshteinDistance()
        val scores = mutableMapOf<String, Float>()

        for (qWord in queryKeywords) {
            if (qWord in textKeywords) {
                scores[qWord] = 1f
                continue
            }

            for (lWord in textKeywords) {
                val distance = distanceMetric.percentSimilarity(qWord, lWord)
                if (qWord !in scores) {
                    scores[qWord] = distance
                } else {
                    scores[qWord] = maxOf(scores[qWord] ?: 0f, distance)
                }
            }
        }

        var total = 0f
        for (word in queryKeywords) {
            if (word in scores) {
                total += scores[word] ?: 0f
            }
        }

        return total / queryKeywords.size
    }

    fun removeMarkdownComments(text: String): String {
        val commentRegex = Regex("<!--.*?-->")
        return text.replace(commentRegex, "").trim()
    }


    data class TextSection(val title: String?, val level: Int?, val content: String) {
        fun toMarkdown(
            shouldUppercaseTitle: Boolean = false,
            removeComments: Boolean = true
        ): String {
            val contentWithoutComments = if (removeComments) {
                removeMarkdownComments(content)
            } else {
                content
            }

            if (title == null || level == null) {
                return contentWithoutComments
            }

            return "#".repeat(level) + " ${if (shouldUppercaseTitle) title.uppercase() else title}\n$contentWithoutComments"
        }
    }
}

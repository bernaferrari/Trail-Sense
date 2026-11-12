package com.kylecorry.trail_sense.tools.survival_guide.infrastructure

import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.text.AppStrings

class SurvivalGuideSearch {

    private val loader = GuideLoader()
    private val strategy = if (getAppService<AppStrings>().language.startsWith("en")) {
        EnglishSurvivalGuideFuzzySearch(loader)
    } else {
        MultilingualSurvivalGuideFuzzySearch(loader)
    }

    suspend fun search(query: String): List<SurvivalGuideSearchResult> {
        return strategy.search(query)
    }

    suspend fun getSummary(result: SurvivalGuideSearchResult): String {
        val contents = loader.load(result.chapter, true)
        val section = contents.sections[result.headingIndex]

        val content = if (shouldUseSubsection(result)) {
            section.subsections[result.bestSubsection!!.headingIndex].content
        } else {
            section.content
        }

        return content.orEmpty().trim()
    }

    companion object {
        private const val MIN_SUBSECTION_SCORE = 0.6f

        fun shouldUseSubsection(result: SurvivalGuideSearchResult): Boolean {
            val subsection = result.bestSubsection ?: return false
            return subsection.score >= MIN_SUBSECTION_SCORE
        }

    }

}

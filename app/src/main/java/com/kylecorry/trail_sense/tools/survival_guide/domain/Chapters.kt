package com.kylecorry.trail_sense.tools.survival_guide.domain

import android.content.Context
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.icons.trail.*
import com.kylecorry.trail_sense.shared.text.RawTexts

object Chapters {

    fun getChapters(context: Context): List<Chapter> {
        return listOf(
            Chapter(
                context.getString(R.string.overview),
                RawTexts.guide_survival_chapter_overview,
                TrailIcons.OpenBook
            ),
            Chapter(
                context.getString(R.string.category_medical),
                RawTexts.guide_survival_chapter_medical,
                TrailIcons.CategoryMedical
            ),
            Chapter(
                context.getString(R.string.shelter),
                RawTexts.guide_survival_chapter_shelter,
                TrailIcons.CategoryShelter
            ),
            Chapter(
                context.getString(R.string.water),
                RawTexts.guide_survival_chapter_water,
                TrailIcons.CategoryWater
            ),
            Chapter(
                context.getString(R.string.category_fire),
                RawTexts.guide_survival_chapter_fire,
                TrailIcons.CategoryFire
            ),
            Chapter(
                context.getString(R.string.category_food),
                RawTexts.guide_survival_chapter_food,
                TrailIcons.CategoryFood
            ),
            Chapter(
                context.getString(R.string.navigation),
                RawTexts.guide_survival_chapter_navigation,
                TrailIcons.CategoryNavigation
            ),
            Chapter(
                context.getString(R.string.weather),
                RawTexts.guide_survival_chapter_weather,
                TrailIcons.Cloud
            ),
        )
    }

}

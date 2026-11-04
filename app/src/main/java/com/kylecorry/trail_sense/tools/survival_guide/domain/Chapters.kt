package com.kylecorry.trail_sense.tools.survival_guide.domain

import com.kylecorry.trail_sense.shared.icons.trail.*
import android.content.Context
import com.kylecorry.trail_sense.R

object Chapters {

    fun getChapters(context: Context): List<Chapter> {
        return listOf(
            Chapter(
                context.getString(R.string.overview),
                R.raw.guide_survival_chapter_overview,
                TrailIcons.OpenBook
            ),
            Chapter(
                context.getString(R.string.category_medical),
                R.raw.guide_survival_chapter_medical,
                TrailIcons.CategoryMedical
            ),
            Chapter(
                context.getString(R.string.shelter),
                R.raw.guide_survival_chapter_shelter,
                TrailIcons.CategoryShelter
            ),
            Chapter(
                context.getString(R.string.water),
                R.raw.guide_survival_chapter_water,
                TrailIcons.CategoryWater
            ),
            Chapter(
                context.getString(R.string.category_fire),
                R.raw.guide_survival_chapter_fire,
                TrailIcons.CategoryFire
            ),
            Chapter(
                context.getString(R.string.category_food),
                R.raw.guide_survival_chapter_food,
                TrailIcons.CategoryFood
            ),
            Chapter(
                context.getString(R.string.navigation),
                R.raw.guide_survival_chapter_navigation,
                TrailIcons.CategoryNavigation
            ),
            Chapter(
                context.getString(R.string.weather),
                R.raw.guide_survival_chapter_weather,
                TrailIcons.Cloud
            ),
        )
    }

}

package com.kylecorry.trail_sense.tools.survival_guide.domain

import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.generated.resources.*
import com.kylecorry.trail_sense.shared.icons.trail.*
import com.kylecorry.trail_sense.shared.text.AppStrings
import com.kylecorry.trail_sense.shared.text.RawTexts

object Chapters {

    fun getChapters(): List<Chapter> {
        return listOf(
            Chapter(
                getAppService<AppStrings>().get(Res.string.overview),
                RawTexts.guide_survival_chapter_overview,
                TrailIcons.OpenBook
            ),
            Chapter(
                getAppService<AppStrings>().get(Res.string.category_medical),
                RawTexts.guide_survival_chapter_medical,
                TrailIcons.CategoryMedical
            ),
            Chapter(
                getAppService<AppStrings>().get(Res.string.shelter),
                RawTexts.guide_survival_chapter_shelter,
                TrailIcons.CategoryShelter
            ),
            Chapter(
                getAppService<AppStrings>().get(Res.string.water),
                RawTexts.guide_survival_chapter_water,
                TrailIcons.CategoryWater
            ),
            Chapter(
                getAppService<AppStrings>().get(Res.string.category_fire),
                RawTexts.guide_survival_chapter_fire,
                TrailIcons.CategoryFire
            ),
            Chapter(
                getAppService<AppStrings>().get(Res.string.category_food),
                RawTexts.guide_survival_chapter_food,
                TrailIcons.CategoryFood
            ),
            Chapter(
                getAppService<AppStrings>().get(Res.string.navigation),
                RawTexts.guide_survival_chapter_navigation,
                TrailIcons.CategoryNavigation
            ),
            Chapter(
                getAppService<AppStrings>().get(Res.string.weather),
                RawTexts.guide_survival_chapter_weather,
                TrailIcons.Cloud
            ),
        )
    }

}

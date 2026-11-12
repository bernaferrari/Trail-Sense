package com.kylecorry.trail_sense.tools.packs.ui.mappers

import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.generated.resources.*
import com.kylecorry.trail_sense.shared.text.AppStrings
import com.kylecorry.trail_sense.tools.packs.domain.ItemCategory

class ItemCategoryStringMapper {

    fun getString(category: ItemCategory): String {
        return when (category) {
            ItemCategory.Other -> getAppService<AppStrings>().get(Res.string.other)
            ItemCategory.Food -> getAppService<AppStrings>().get(Res.string.category_food)
            ItemCategory.Hydration -> getAppService<AppStrings>().get(Res.string.category_hydration)
            ItemCategory.Tools -> getAppService<AppStrings>().get(Res.string.tools)
            ItemCategory.Clothing -> getAppService<AppStrings>().get(Res.string.category_clothing)
            ItemCategory.Medical -> getAppService<AppStrings>().get(Res.string.category_medical)
            ItemCategory.Fire -> getAppService<AppStrings>().get(Res.string.category_fire)
            ItemCategory.Shelter -> getAppService<AppStrings>().get(Res.string.category_shelter)
            ItemCategory.Safety -> getAppService<AppStrings>().get(Res.string.category_safety)
            ItemCategory.Natural -> getAppService<AppStrings>().get(Res.string.category_natural)
            ItemCategory.Navigation -> getAppService<AppStrings>().get(Res.string.navigation)
            ItemCategory.Electronics -> getAppService<AppStrings>().get(Res.string.electronics)
            ItemCategory.Documents -> getAppService<AppStrings>().get(Res.string.documents)
            ItemCategory.Hygiene -> getAppService<AppStrings>().get(Res.string.hygiene)
        }
    }

}

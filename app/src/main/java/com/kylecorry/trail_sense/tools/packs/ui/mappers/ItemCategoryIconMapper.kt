package com.kylecorry.trail_sense.tools.packs.ui.mappers

import androidx.compose.ui.graphics.vector.ImageVector
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.icons.trail.*
import com.kylecorry.trail_sense.tools.packs.domain.ItemCategory

class ItemCategoryIconMapper {

    fun getIcon(category: ItemCategory): ImageVector {
        return when (category) {
            ItemCategory.Other -> TrailIcons.CategoryOther
            ItemCategory.Food -> TrailIcons.CategoryFood
            ItemCategory.Hydration -> TrailIcons.CategoryWater
            ItemCategory.Tools -> TrailIcons.CategoryTools
            ItemCategory.Natural -> TrailIcons.CategoryNatural
            ItemCategory.Clothing -> TrailIcons.CategoryClothing
            ItemCategory.Medical -> TrailIcons.CategoryMedical
            ItemCategory.Fire -> TrailIcons.CategoryFire
            ItemCategory.Shelter -> TrailIcons.CategoryShelter
            ItemCategory.Safety -> TrailIcons.CategorySafety
            ItemCategory.Navigation -> TrailIcons.CategoryNavigation
            ItemCategory.Electronics -> TrailIcons.Sensors
            ItemCategory.Documents -> TrailIcons.File
            ItemCategory.Hygiene -> TrailIcons.Hygiene
        }
    }

    fun getIconVector(category: ItemCategory): ImageVector {
        return when (category) {
            ItemCategory.Other -> TrailIcons.CategoryOther
            ItemCategory.Food -> TrailIcons.CategoryFood
            ItemCategory.Hydration -> TrailIcons.CategoryWater
            ItemCategory.Tools -> TrailIcons.CategoryTools
            ItemCategory.Natural -> TrailIcons.CategoryNatural
            ItemCategory.Clothing -> TrailIcons.CategoryClothing
            ItemCategory.Medical -> TrailIcons.CategoryMedical
            ItemCategory.Fire -> TrailIcons.CategoryFire
            ItemCategory.Shelter -> TrailIcons.CategoryShelter
            ItemCategory.Safety -> TrailIcons.CategorySafety
            ItemCategory.Navigation -> TrailIcons.CategoryNavigation
            ItemCategory.Electronics -> TrailIcons.Sensors
            ItemCategory.Documents -> TrailIcons.File
            ItemCategory.Hygiene -> TrailIcons.Hygiene
        }
    }

}

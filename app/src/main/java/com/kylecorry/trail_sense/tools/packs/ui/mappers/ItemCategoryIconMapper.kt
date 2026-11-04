package com.kylecorry.trail_sense.tools.packs.ui.mappers

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.vector.ImageVector
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.icons.trail.*
import com.kylecorry.trail_sense.tools.packs.domain.ItemCategory

class ItemCategoryIconMapper {

    @DrawableRes
    fun getIcon(category: ItemCategory): Int {
        return when (category) {
            ItemCategory.Other -> R.drawable.ic_category_other
            ItemCategory.Food -> R.drawable.ic_category_food
            ItemCategory.Hydration -> R.drawable.ic_category_water
            ItemCategory.Tools -> R.drawable.ic_category_tools
            ItemCategory.Natural -> R.drawable.ic_category_natural
            ItemCategory.Clothing -> R.drawable.ic_category_clothing
            ItemCategory.Medical -> R.drawable.ic_category_medical
            ItemCategory.Fire -> R.drawable.ic_category_fire
            ItemCategory.Shelter -> R.drawable.ic_category_shelter
            ItemCategory.Safety -> R.drawable.ic_category_safety
            ItemCategory.Navigation -> R.drawable.ic_category_navigation
            ItemCategory.Electronics -> R.drawable.ic_sensors
            ItemCategory.Documents -> R.drawable.ic_file
            ItemCategory.Hygiene -> R.drawable.ic_hygiene
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

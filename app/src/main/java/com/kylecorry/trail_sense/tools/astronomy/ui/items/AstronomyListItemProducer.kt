package com.kylecorry.trail_sense.tools.astronomy.ui.items

import androidx.compose.ui.graphics.vector.ImageVector
import android.graphics.Bitmap
import com.kylecorry.sol.units.Coordinate
import java.time.LocalDate

interface AstronomyListItemProducer {
    suspend fun getListItem(
        date: LocalDate,
        location: Coordinate,
        declination: Float
    ): AstronomyListItem?
}

data class AstronomyListItem(
    val id: Long,
    val title: String,
    val subtitle: String? = null,
    val icon: AstronomyListIcon? = null,
    val data: List<AstronomyListItemData> = emptyList(),
    val onClick: (() -> Unit)? = null
)

data class AstronomyListIcon(
    val vector: ImageVector? = null,
    val tint: Int? = null,
    val rotation: Float = 0f,
    val bitmap: Bitmap? = null
)

data class AstronomyListItemData(
    val value: String,
    val label: String? = null
)

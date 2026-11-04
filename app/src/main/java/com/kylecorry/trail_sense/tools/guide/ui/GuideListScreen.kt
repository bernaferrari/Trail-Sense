@file:Suppress("FunctionNaming")
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.kylecorry.trail_sense.tools.guide.ui

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.icons.trail.KeyboardArrowRight
import com.kylecorry.trail_sense.shared.icons.trail.OpenBook
import com.kylecorry.trail_sense.shared.icons.trail.TrailIcons
import com.kylecorry.trail_sense.shared.icons.trail.UserGuide as UserGuideIcon
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseAccents
import com.kylecorry.trail_sense.shared.views.compose.TrailSensePage
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSearchBar
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseShapeBadge
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpotlight
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseTone
import com.kylecorry.trail_sense.shared.views.compose.trailSenseSection
import com.kylecorry.trail_sense.tools.guide.domain.UserGuide
import com.kylecorry.trail_sense.tools.guide.domain.UserGuideCategory
import com.kylecorry.trail_sense.tools.weather.ui.SkyEmptyState
import com.kylecorry.trail_sense.tools.weather.ui.SkyFocalShape
import com.kylecorry.trail_sense.tools.weather.ui.SkyValueRow

/** One hue and one shape per category, so each family of guides reads as its own shelf. */
private val categoryAccents = listOf(
    TrailSenseAccents.Stone,
    TrailSenseAccents.Sky,
    TrailSenseAccents.Leaf,
    TrailSenseAccents.Coral,
    TrailSenseAccents.Lavender,
    TrailSenseAccents.Sun,
    TrailSenseAccents.Seafoam,
    TrailSenseAccents.Rose,
    TrailSenseAccents.Apricot,
    TrailSenseAccents.Periwinkle
)

@Composable
private fun categoryShape(index: Int): Shape = when (index % 8) {
    0 -> MaterialShapes.Cookie6Sided
    1 -> MaterialShapes.Cookie9Sided
    2 -> MaterialShapes.Clover4Leaf
    3 -> MaterialShapes.Sunny
    4 -> MaterialShapes.Pentagon
    5 -> MaterialShapes.Flower
    6 -> MaterialShapes.Gem
    else -> MaterialShapes.Puffy
}.toShape()

/**
 * User guide: a hero with the number of guides and the search inside it, then every guide grouped under its tool
 * category, each category with its own hue and badge shape and each guide wearing its tool's icon. A search with no
 * match says so warmly and offers to clear itself.
 */
@Composable
internal fun GuideListScreen(
    guides: List<UserGuideCategory>,
    onOpenGuide: (UserGuide) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(guides, query) {
        filterGuides(guides, query)
    }
    val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val total = guides.sumOf { it.guides.size }

    TrailSensePage(
        title = stringResource(R.string.tool_user_guide_title),
        onBack = { dispatcher?.onBackPressed() },
        backLabel = stringResource(R.string.back)
    ) {
        item(key = "hero") {
            TrailSenseSpotlight(
                title = stringResource(R.string.sky_guides_count, total),
                supportingText = stringResource(R.string.sky_guides_supporting, guides.size),
                tone = TrailSenseTone.Primary,
                titleStyle = MaterialTheme.typography.displaySmall,
                badge = {
                    SkyFocalShape(
                        icon = TrailIcons.OpenBook,
                        shape = MaterialShapes.Cookie12Sided.toShape(),
                        size = 72.dp,
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                }
            ) {
                TrailSenseSearchBar(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = stringResource(R.string.search),
                    clearContentDescription = stringResource(android.R.string.cancel)
                )
            }
        }

        if (filtered.isEmpty()) {
            item(key = "empty") {
                SkyEmptyState(
                    icon = Icons.Default.SearchOff,
                    title = stringResource(R.string.no_results),
                    description = stringResource(R.string.sky_guides_no_results, query),
                    shape = MaterialShapes.Cookie9Sided.toShape(),
                    companionShape = MaterialShapes.Sunny.toShape(),
                    companionIcon = TrailIcons.UserGuideIcon,
                    action = {
                        FilledTonalButton(onClick = { query = "" }) {
                            Text(stringResource(R.string.guide_clear_search))
                        }
                    },
                    modifier = Modifier.animateItem()
                )
            }
        }

        filtered.forEach { category ->
            val index = guides.indexOfFirst { it.name == category.name }.coerceAtLeast(0)
            // The shelf's name carries its size, so a long list still has a sense of proportion.
            trailSenseSection(key = "category-${category.name}", title = "${category.name} · ${category.guides.size}") {
                category.guides.forEach { guide ->
                    row { position ->
                        GuideRow(guide, index, position) { onOpenGuide(guide) }
                    }
                }
            }
        }
    }
}

@Composable
private fun GuideRow(
    guide: UserGuide,
    categoryIndex: Int,
    position: com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupPosition,
    onClick: () -> Unit
) {
    SkyValueRow(
        // One line per guide: a uniform shelf reads better than rows that are sometimes one line and sometimes two.
        title = guide.name,
        subtitle = null,

        icon = guide.icon ?: TrailIcons.OpenBook,
        accent = categoryAccents[categoryIndex % categoryAccents.size],
        badgeShape = categoryShape(categoryIndex),
        position = position,
        // Each shelf carries a breath of its hue, so sections read as families rather than one grey list.
        containerColor = androidx.compose.ui.graphics.lerp(
            MaterialTheme.colorScheme.surfaceContainerLow,
            com.kylecorry.trail_sense.shared.views.compose.trailSenseWellColors(
                categoryAccents[categoryIndex % categoryAccents.size]
            ).container,
            0.3f
        ),
        onClick = onClick,
        trailing = {
            Icon(
                TrailIcons.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    )
}

private fun filterGuides(
    guides: List<UserGuideCategory>,
    query: String
): List<UserGuideCategory> {
    if (query.isBlank()) {
        return guides
    }

    return guides.mapNotNull { category ->
        if (category.name.contains(query, ignoreCase = true)) {
            category
        } else {
            val matchingGuides = category.guides.filter {
                it.name.contains(query, ignoreCase = true) ||
                        it.description?.contains(query, ignoreCase = true) == true
            }
            if (matchingGuides.isEmpty()) {
                null
            } else {
                UserGuideCategory(category.name, matchingGuides)
            }
        }
    }
}


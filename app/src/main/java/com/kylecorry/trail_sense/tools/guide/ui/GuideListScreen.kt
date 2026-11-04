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


/**
 * User guide: the search, then every guide grouped under its tool category, each wearing its tool's icon in one quiet
 * shape. A search with no match says so and offers to clear itself.
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
        // One question: how does this tool work? Search answers it; the guides follow, shelved by category.
        item(key = "search") {
            TrailSenseSearchBar(
                value = query,
                onValueChange = { query = it },
                placeholder = stringResource(R.string.search),
                clearContentDescription = stringResource(android.R.string.cancel)
            )
        }

        if (filtered.isEmpty()) {
            item(key = "empty") {
                SkyEmptyState(
                    icon = Icons.Default.SearchOff,
                    title = stringResource(R.string.no_results),
                    description = stringResource(R.string.sky_guides_no_results, query),
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
            // The shelf's name carries its size, so a long list still has a sense of proportion.
            trailSenseSection(key = "category-${category.name}", title = category.name) {
                category.guides.forEach { guide ->
                    row { position ->
                        GuideRow(guide, position) { onOpenGuide(guide) }
                    }
                }
            }
        }
    }
}

@Composable
private fun GuideRow(
    guide: UserGuide,
    position: com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupPosition,
    onClick: () -> Unit
) {
    SkyValueRow(
        // One line per guide: a uniform shelf reads better than rows that are sometimes one line and sometimes two.
        title = guide.name,
        subtitle = null,

        icon = guide.icon ?: TrailIcons.OpenBook,
        position = position,
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


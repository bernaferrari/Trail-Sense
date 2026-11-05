@file:Suppress("FunctionNaming")
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.kylecorry.trail_sense.tools.guide.ui

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SearchOff
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
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseListItem
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
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseEmptyState
import com.kylecorry.trail_sense.tools.weather.ui.SkyFocalShape
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.trail_sense.shared.navigation.Route
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.kylecorry.trail_sense.tools.guide.infrastructure.Guides


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
                TrailSenseEmptyState(
                    icon = Icons.Default.SearchOff,
                    title = stringResource(R.string.no_results),
                    description = stringResource(R.string.sky_guides_no_results, query),
                    actionLabel = stringResource(R.string.guide_clear_search),
                    onAction = { query = "" },
                    modifier = Modifier
                        .fillParentMaxHeight(0.9f)
                        .animateItem()
                )
            }
        }

        // A category with a single guide doesn't get a header and group of its own: it joins its neighbour, and the
        // header names both, so the page is a few long shelves rather than many one-row ones.
        mergeSparseCategories(filtered).forEach { shelf ->
            trailSenseSection(key = "category-${shelf.name}", title = shelf.name) {
                shelf.guides.forEach { guide ->
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
    // The family's row: the guide's icon in the 40 dp well, its name, a chevron. One line per guide: a uniform shelf
    // reads better than rows that are sometimes one line and sometimes two.
    TrailSenseListItem(
        title = guide.name,
        icon = androidx.compose.ui.graphics.vector.rememberVectorPainter(guide.icon ?: TrailIcons.OpenBook),
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

private fun mergeSparseCategories(categories: List<UserGuideCategory>): List<UserGuideCategory> {
    val shelves = mutableListOf<UserGuideCategory>()
    var pending: UserGuideCategory? = null
    for (category in categories) {
        val joined = pending?.let { UserGuideCategory("${it.name} · ${category.name}", it.guides + category.guides) }
            ?: category
        if (joined.guides.size < 2) {
            pending = joined
        } else {
            shelves += joined
            pending = null
        }
    }
    pending?.let { last ->
        val previous = shelves.removeLastOrNull()
        shelves += if (previous == null) last else {
            UserGuideCategory("${previous.name} · ${last.name}", previous.guides + last.guides)
        }
    }
    return shelves
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

/** Every user guide, by category; tapping one opens it. */
@Composable
fun GuideListToolScreen(host: TrailSenseDestinationHost) {
    GuideListScreen(
        guides = Guides.guides(host.requireContext()),
        onOpenGuide = {
            tryOrNothing {
                host.findNavigator().navigate(Route.Guide(name = it.name, contents = it.contents))
            }
        }
    )
}

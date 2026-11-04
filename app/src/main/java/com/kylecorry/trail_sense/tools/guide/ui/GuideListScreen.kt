@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.guide.ui

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseEmptyState
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseListItem
import com.kylecorry.trail_sense.shared.views.compose.TrailSensePage
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSearchBar
import com.kylecorry.trail_sense.shared.views.compose.trailSenseSection
import com.kylecorry.trail_sense.tools.guide.domain.UserGuide
import com.kylecorry.trail_sense.tools.guide.domain.UserGuideCategory

/**
 * User guide: a search bar, then every guide grouped under its tool category. A search with no match says so plainly
 * and offers to clear itself.
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
    val guideIcon = rememberVectorPainter(Icons.AutoMirrored.Filled.MenuBook)

    TrailSensePage(
        title = stringResource(R.string.tool_user_guide_title),
        onBack = { dispatcher?.onBackPressed() },
        backLabel = stringResource(R.string.back)
    ) {
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
            trailSenseSection(key = "category-${category.name}", title = category.name) {
                category.guides.forEach { guide ->
                    row { position ->
                        TrailSenseListItem(
                            title = guide.name,
                            subtitle = guide.description,
                            icon = guideIcon,
                            position = position,
                            onClick = { onOpenGuide(guide) }
                        )
                    }
                }
            }
        }
    }
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
                it.name.contains(query, ignoreCase = true)
            }
            if (matchingGuides.isEmpty()) {
                null
            } else {
                UserGuideCategory(category.name, matchingGuides)
            }
        }
    }
}

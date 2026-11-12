package com.kylecorry.trail_sense.tools.survival_guide.ui

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import com.kylecorry.andromeda.core.cache.DependencyRegistry
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.icons.trail.SurvivalGuide
import com.kylecorry.trail_sense.shared.icons.trail.TrailIcons
import com.kylecorry.trail_sense.shared.navigation.Route
import com.kylecorry.trail_sense.shared.navigation.TrailSenseNavigator
import com.kylecorry.trail_sense.shared.views.compose.AppHost
import com.kylecorry.trail_sense.shared.views.compose.AppOverlay
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSearchBar
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.compose.from
import com.kylecorry.trail_sense.tools.survival_guide.infrastructure.SurvivalGuideSearch
import com.kylecorry.trail_sense.tools.survival_guide.infrastructure.SurvivalGuideSearchResult
import kotlinx.coroutines.delay

/**
 * The survival guide quick action: a sheet over the current screen with a search and its best answer, as upstream's
 * survival guide bottom sheet had. The guide button continues the search in the full list.
 */
fun showSurvivalGuideQuickSearch(context: Context, navigator: TrailSenseNavigator) {
    var overlay: AppOverlay? = null
    overlay = AppHost.from(context).addOverlay {
        SurvivalGuideQuickSearchSheet(
            onDismiss = { overlay?.remove() },
            onOpenList = { query ->
                overlay?.remove()
                navigator.navigate(Route.SurvivalGuideList(searchQuery = query))
            },
            onOpenResult = { result ->
                overlay?.remove()
                navigator.navigate(
                    Route.SurvivalGuideReader(
                        chapter = result.chapter.resource,
                        headerIndex = result.headingIndex
                    )
                )
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SurvivalGuideQuickSearchSheet(
    onDismiss: () -> Unit,
    onOpenList: (String) -> Unit,
    onOpenResult: (SurvivalGuideSearchResult) -> Unit
) {
    val context = LocalContext.current
    val search = remember { SurvivalGuideSearch() }
    var query by remember { mutableStateOf("") }
    var summary by remember { mutableStateOf("") }
    var firstResult by remember { mutableStateOf<SurvivalGuideSearchResult?>(null) }

    LaunchedEffect(query) {
        if (query.isBlank()) {
            summary = ""
            firstResult = null
            return@LaunchedEffect
        }
        // Search after a short pause in typing, like the list
        delay(200)
        val result = search.search(query).firstOrNull()
        firstResult = result
        summary = if (result != null && result.score >= 0.5f) search.getSummary(result) else ""
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
        )
    ) {
        Column(
            modifier = Modifier
                // The sheet is its own window, so it repeats the app root's test tag setting for UiAutomator
                .semantics { testTagsAsResourceId = true }
                .fillMaxWidth()
                .padding(horizontal = TrailSenseSpacing.screen)
                .padding(bottom = TrailSenseSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.survival_guide),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = { onOpenList(query) },
                    modifier = Modifier.testTag("survival_guide_search_list_btn")
                ) {
                    Icon(
                        painter = rememberVectorPainter(TrailIcons.SurvivalGuide),
                        contentDescription = stringResource(R.string.survival_guide)
                    )
                }
            }
            TrailSenseSearchBar(
                value = query,
                onValueChange = { query = it },
                placeholder = stringResource(R.string.search),
                clearContentDescription = stringResource(R.string.survival_guide_clear_search),
                modifier = Modifier.testTag("search")
            )
            val result = firstResult
            if (summary.isNotBlank() && result != null) {
                SurvivalGuideSummary(
                    summary = summary,
                    result = result,
                    markdown = DependencyRegistry.get(),
                    onSummaryClick = onOpenResult
                )
            } else {
                Text(
                    text = stringResource(R.string.survival_guide_empty_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("empty_view_description")
                )
            }
        }
    }
}

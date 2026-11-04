package com.kylecorry.trail_sense.tools.paths.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Place
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseEmptyState
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupHeader
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupPosition
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.tools.paths.domain.PathPoint

/**
 * Every point of a path as one group of rows (newest first), under a header that says how many there are. Tap a
 * point to show it on the map; its menu (or a long press) follows, navigates to, saves or deletes it.
 */
@Composable
fun PathPointsContent(
    points: List<PathPoint>,
    formatService: FormatService,
    baseDistanceUnits: DistanceUnits,
    onCreateBeacon: (PathPoint) -> Unit,
    onDelete: (PathPoint) -> Unit,
    onNavigate: (PathPoint) -> Unit,
    onNavigateDirect: (PathPoint) -> Unit,
    onView: (PathPoint) -> Unit,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) {
        TrailSenseEmptyState(
            icon = Icons.Rounded.Place,
            title = stringResource(R.string.no_data),
            modifier = modifier.testTag("path_points_list")
        )
        return
    }

    LazyColumn(
        modifier = modifier.testTag("path_points_list"),
        contentPadding = PaddingValues(
            start = TrailSenseSpacing.lg,
            end = TrailSenseSpacing.lg,
            bottom = TrailSenseSpacing.xxxl
        ),
        verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xxs)
    ) {
        item(key = "header") {
            TrailSenseGroupHeader(
                pluralStringResource(R.plurals.path_points_count, points.size, points.size)
            )
        }
        itemsIndexed(
            items = points,
            key = { _, point -> point.id }
        ) { index, point ->
            PathPointRow(
                point = point,
                formatService = formatService,
                baseDistanceUnits = baseDistanceUnits,
                onCreateBeacon = onCreateBeacon,
                onDelete = onDelete,
                onNavigate = onNavigate,
                onNavigateDirect = onNavigateDirect,
                onView = onView,
                position = TrailSenseGroupPosition.at(index, points.size),
                modifier = Modifier.animateItem()
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PathPointsContentPreview() {
    TrailSenseTheme {
        PathPointsContent(
            points = emptyList(),
            formatService = FormatService.getInstance(LocalContext.current),
            baseDistanceUnits = UserPreferences(LocalContext.current).baseDistanceUnits,
            onCreateBeacon = {},
            onDelete = {},
            onNavigate = {},
            onNavigateDirect = {},
            onView = {}
        )
    }
}

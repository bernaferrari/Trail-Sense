package com.kylecorry.trail_sense.tools.paths.ui

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDivider
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.tools.paths.domain.PathPoint

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
    LazyColumn(
        modifier = modifier.testTag("path_points_list"),
        contentPadding = PaddingValues(vertical = TrailSenseSpacing.sm)
    ) {
        items(
            items = points,
            key = { it.id }
        ) { point ->
            PathPointRow(
                point = point,
                formatService = formatService,
                baseDistanceUnits = baseDistanceUnits,
                onCreateBeacon = onCreateBeacon,
                onDelete = onDelete,
                onNavigate = onNavigate,
                onNavigateDirect = onNavigateDirect,
                onView = onView,
                modifier = Modifier.padding(horizontal = TrailSenseSpacing.screen)
            )
            TrailSenseDivider()
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

package com.kylecorry.trail_sense.tools.paths.ui

import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseComposeRootView
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDivider
import com.kylecorry.trail_sense.tools.paths.domain.PathPoint

class PathPointsSheet {

    private var points: List<PathPoint> = emptyList()
    private var updatePoints: ((List<PathPoint>) -> Unit)? = null
    private var sheet: BottomSheetDialog? = null
    private var formatService: FormatService? = null
    var onCreateBeaconListener: (point: PathPoint) -> Unit = {}
    var onDeletePointListener: (point: PathPoint) -> Unit = {}
    var onNavigateToPointListener: (point: PathPoint) -> Unit = {}
    var onViewPointListener: (point: PathPoint) -> Unit = {}

    fun setPoints(points: List<PathPoint>) {
        this.points = points
        updatePoints?.invoke(points)
    }

    fun show(fragment: Fragment): BottomSheetDialog {
        val context = fragment.requireContext()
        val dialog = BottomSheetDialog(context)
        sheet = dialog
        val formatter = FormatService.getInstance(context)
        formatService = formatter
        dialog.setContentView(
            TrailSenseComposeRootView(
                context,
                ViewCompositionStrategy.DisposeOnDetachedFromWindow
            ) {
                var displayedPoints by remember { mutableStateOf(points) }
                DisposableEffect(Unit) {
                    updatePoints = { displayedPoints = it }
                    onDispose {
                        updatePoints = null
                    }
                }
                PathPointsContent(
                    points = displayedPoints,
                    formatService = formatter,
                    baseDistanceUnits = UserPreferences(context).baseDistanceUnits,
                    onCreateBeacon = {
                        onCreateBeaconListener(it)
                        sheet?.dismiss()
                    },
                    onDelete = { onDeletePointListener(it) },
                    onNavigate = {
                        onNavigateToPointListener(it)
                        sheet?.dismiss()
                    },
                    onView = {
                        onViewPointListener(it)
                        sheet?.dismiss()
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        )
        dialog.setOnDismissListener {
            updatePoints = null
            sheet = null
            formatService = null
        }
        dialog.show()
        return dialog
    }

}

@Composable
private fun PathPointsContent(
    points: List<PathPoint>,
    formatService: FormatService,
    baseDistanceUnits: DistanceUnits,
    onCreateBeacon: (PathPoint) -> Unit,
    onDelete: (PathPoint) -> Unit,
    onNavigate: (PathPoint) -> Unit,
    onView: (PathPoint) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.testTag("path_points_list")
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
                onView = onView
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
            onView = {}
        )
    }
}

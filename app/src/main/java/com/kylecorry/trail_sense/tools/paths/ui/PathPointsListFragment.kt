package com.kylecorry.trail_sense.tools.paths.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.kylecorry.andromeda.signal.CellNetwork
import com.kylecorry.sol.time.Time.toZonedDateTime
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDivider
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseListItem
import com.kylecorry.trail_sense.tools.paths.domain.PathPoint

class PathPointsListFragment : BottomSheetDialogFragment() {

    private var points: List<PathPoint> = emptyList()
    private var updatePoints: ((List<PathPoint>) -> Unit)? = null
    private val formatService by lazy { FormatService.getInstance(requireContext()) }
    var onCreateBeaconListener: (point: PathPoint) -> Unit = {}
    var onDeletePointListener: (point: PathPoint) -> Unit = {}
    var onNavigateToPointListener: (point: PathPoint) -> Unit = {}
    var onViewPointListener: (point: PathPoint) -> Unit = {}

    fun setPoints(points: List<PathPoint>) {
        this.points = points
        updatePoints?.invoke(points)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TrailSenseTheme {
                    var displayedPoints by remember { mutableStateOf(points) }
                    DisposableEffect(Unit) {
                        updatePoints = { displayedPoints = it }
                        onDispose {
                            updatePoints = null
                        }
                    }
                    PathPointsContent(
                        points = displayedPoints,
                        formatService = formatService,
                        baseDistanceUnits = UserPreferences(requireContext()).baseDistanceUnits,
                        onCreateBeacon = {
                            onCreateBeaconListener(it)
                            dismiss()
                        },
                        onDelete = { onDeletePointListener(it) },
                        onNavigate = {
                            onNavigateToPointListener(it)
                            dismiss()
                        },
                        onView = {
                            onViewPointListener(it)
                            dismiss()
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    override fun onDestroyView() {
        updatePoints = null
        super.onDestroyView()
    }

}

@Composable
private fun PathPointsContent(
    points: List<PathPoint>,
    formatService: FormatService,
    baseDistanceUnits: com.kylecorry.sol.units.DistanceUnits,
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

@Composable
private fun PathPointRow(
    point: PathPoint,
    formatService: FormatService,
    baseDistanceUnits: com.kylecorry.sol.units.DistanceUnits,
    onCreateBeacon: (PathPoint) -> Unit,
    onDelete: (PathPoint) -> Unit,
    onNavigate: (PathPoint) -> Unit,
    onView: (PathPoint) -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    val title = point.time?.toZonedDateTime()?.let { date ->
        stringResource(
            R.string.waypoint_time_format,
            formatService.formatRelativeDate(date.toLocalDate()),
            formatService.formatTime(date.toLocalTime(), false)
        )
    } ?: stringResource(R.string.waypoint)
    val subtitle = point.elevation?.let {
        val elevation = Distance.meters(it).convertTo(baseDistanceUnits)
        val slope = stringResource(R.string.slope_amount, formatService.formatPercentage(point.slope))
        "${formatService.formatDistance(elevation, Units.getDecimalPlaces(elevation.units), false)}    $slope"
    }
    val cellNetwork = point.cellSignal?.network?.id?.let { id ->
        CellNetwork.values().firstOrNull { it.id == id }
    }

    TrailSenseListItem(
        title = title,
        subtitle = subtitle,
        icon = point.cellSignal?.let {
            painterResource(com.kylecorry.trail_sense.shared.sensors.CellSignalUtils.getCellQualityImage(it.quality))
        },
        iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
        trailing = {
            IconButton(
                onClick = { menuOpen = true },
                modifier = Modifier.testTag("waypoint_menu_btn")
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_menu_dots),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false }
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.navigate)) },
                    onClick = {
                        menuOpen = false
                        onNavigate(point)
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.create_beacon)) },
                    onClick = {
                        menuOpen = false
                        onCreateBeacon(point)
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.delete)) },
                    onClick = {
                        menuOpen = false
                        onDelete(point)
                    }
                )
            }
        },
        onClick = { onView(point) },
        animateEntrance = false,
        modifier = Modifier.testTag("waypoint_time")
    )

    if (cellNetwork != null) {
        Text(
            text = formatService.formatCellNetwork(cellNetwork),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag("signal_strength")
        )
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

package com.kylecorry.trail_sense.tools.paths.ui

import com.kylecorry.trail_sense.shared.icons.trail.*
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import com.kylecorry.andromeda.signal.CellNetwork
import com.kylecorry.sol.time.Time.toZonedDateTime
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.sensors.CellSignalUtils
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupPosition
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseListItem
import com.kylecorry.trail_sense.tools.paths.domain.PathPoint

@Composable
fun PathPointRow(
    point: PathPoint,
    formatService: FormatService,
    baseDistanceUnits: DistanceUnits,
    onCreateBeacon: (PathPoint) -> Unit,
    onDelete: (PathPoint) -> Unit,
    onNavigate: (PathPoint) -> Unit,
    onNavigateDirect: (PathPoint) -> Unit,
    onView: (PathPoint) -> Unit,
    modifier: Modifier = Modifier,
    position: TrailSenseGroupPosition? = null,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow
) {
    var menuOpen by remember { mutableStateOf(false) }
    val title = point.time?.toZonedDateTime()?.let { date ->
        stringResource(
            R.string.waypoint_time_format,
            formatService.formatRelativeDate(date.toLocalDate()),
            formatService.formatTime(date.toLocalTime(), false)
        )
    } ?: stringResource(R.string.waypoint)
    val subtitle = getPathPointSubtitle(
        point,
        formatService,
        baseDistanceUnits,
        stringResource(R.string.slope_amount, formatService.formatPercentage(point.slope))
    )
    val cellNetwork = point.cellSignal?.network?.id?.let { id ->
        CellNetwork.values().firstOrNull { it.id == id }
    }
    val networkText = cellNetwork?.let { formatService.formatCellNetwork(it) }

    TrailSenseListItem(
        title = title,
        subtitle = listOfNotNull(subtitle, networkText).joinToString(" · ").ifBlank { null },
        icon = point.cellSignal?.let {
            rememberVectorPainter(CellSignalUtils.getCellQualityImage(it.quality))
        },
        iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
        trailing = {
            IconButton(
                onClick = { menuOpen = true },
                modifier = Modifier.testTag("waypoint_menu_btn")
            ) {
                Icon(
                    painter = rememberVectorPainter(TrailIcons.MenuDots),
                    contentDescription = stringResource(R.string.details),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false }
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.path_navigation_follow_to_point)) },
                    onClick = {
                        menuOpen = false
                        onNavigate(point)
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.path_navigation_direct)) },
                    onClick = {
                        menuOpen = false
                        onNavigateDirect(point)
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
        onLongClick = { menuOpen = true },
        position = position,
        containerColor = containerColor,
        animateEntrance = false,
        modifier = modifier.testTag("waypoint_time")
    )
}

private fun getPathPointSubtitle(
    point: PathPoint,
    formatService: FormatService,
    baseDistanceUnits: DistanceUnits,
    slopeText: String
): String? {
    return point.elevation?.let {
        val elevation = Distance.meters(it).convertTo(baseDistanceUnits)
        "${formatService.formatDistance(elevation, Units.getDecimalPlaces(elevation.units), false)} · $slopeText"
    }
}

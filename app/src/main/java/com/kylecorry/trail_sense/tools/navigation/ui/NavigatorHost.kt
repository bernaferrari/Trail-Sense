package com.kylecorry.trail_sense.tools.navigation.ui

import androidx.compose.ui.platform.testTag
import com.kylecorry.trail_sense.shared.copy
import androidx.compose.ui.text.font.FontWeight
import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.isInvisible
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.icons.TrailSenseIcons
import com.kylecorry.trail_sense.shared.map_layers.ui.MapAttributionState
import com.kylecorry.trail_sense.shared.quickactions.ComposeQuickActionButtonView
import com.kylecorry.trail_sense.shared.views.NorthReferenceBadgeState
import com.kylecorry.trail_sense.shared.views.SensorStatusBadgeView
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.tools.map.ui.MapView

internal data class NavigatorViews(
    val navigationTitle: NavigationTitleState,
    val altitude: NavigatorDataPointState,
    val speed: NavigatorDataPointState,
    val radarCompassMap: MapView,
    val radarCompass: RadarCompassView,
    val linearCompass: LinearSightingCompassView,
    val mapAttribution: MapAttributionState,
    val northReferenceIndicator: NorthReferenceBadgeState,
    val accuracyView: SensorStatusBadgeView,
    val navigationSheet: NavigationSheetView
) {
    var onBeaconClick by mutableStateOf<(() -> Unit)?>(null)
    var onBeaconLongClick by mutableStateOf<(() -> Unit)?>(null)
}

internal class NavigationTitleState {
    val leftButton = ComposeQuickActionButtonView()
    val rightButton = ComposeQuickActionButtonView()
    var title by mutableStateOf("")
    var direction by mutableStateOf("")
    var subtitle by mutableStateOf("")
    var isTitleVisible by mutableStateOf(true)
    var onSubtitleClick by mutableStateOf<(() -> Unit)?>(null)
    var onSubtitleLongClick by mutableStateOf<(() -> Boolean)?>(null)
}

internal class NavigatorDataPointState(
    private val icon: ImageVector
) {
    var title by mutableStateOf("")
    var onClick by mutableStateOf<(() -> Unit)?>(null)

    @Composable
    fun Content(modifier: Modifier = Modifier) {
        // Quiet supporting text, not a chip: altitude and speed support the heading, they don't compete with it.
        Text(
            text = title,
            modifier = modifier
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(TrailSenseSpacing.sm))
                .clickable(enabled = onClick != null) { onClick?.invoke() }
                .padding(horizontal = TrailSenseSpacing.xs, vertical = TrailSenseSpacing.xxs),
            style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NavigationTitle(
    state: NavigationTitleState,
    modifier: Modifier = Modifier
) {
    if (!state.isTitleVisible) return
    // The heading sits inside the dial, like a watch face: the number on the lubber line, the direction under it.
    Column(modifier.testTag("navigation_title"), horizontalAlignment = Alignment.CenterHorizontally) {
        // The number is centred on its own; the degree sign hangs to its right (an invisible twin on the left
        // balances it), so "40°" never sits left of centre.
        val digits = state.title.removeSuffix("°")
        val headingStyle = MaterialTheme.typography.displayLarge.copy(fontFeatureSettings = "tnum")
        val headingColor = MaterialTheme.colorScheme.onPrimaryContainer
        Row {
            Text("°", style = headingStyle, fontWeight = FontWeight.Medium, color = androidx.compose.ui.graphics.Color.Transparent, maxLines = 1)
            Text(digits, style = headingStyle, fontWeight = FontWeight.Medium, color = headingColor, maxLines = 1)
            Text("°", style = headingStyle, fontWeight = FontWeight.Medium, color = headingColor, maxLines = 1)
        }
        Text(
            text = state.direction,
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
            maxLines = 1
        )
    }
}

/** Where you are: the coordinates, then altitude and speed in one quiet line. Each part opens its own detail. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NavigationWhereabouts(
    state: NavigationTitleState,
    altitude: NavigatorDataPointState,
    speed: NavigatorDataPointState,
    modifier: Modifier = Modifier
) {
    // Two calm lines, Apple Compass-style: where you are, then how high (and how fast, while moving).
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = state.subtitle,
            modifier = Modifier
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(TrailSenseSpacing.sm))
                .combinedClickable(
                    onClick = { state.onSubtitleClick?.invoke() },
                    onLongClick = { state.onSubtitleLongClick?.invoke() }
                )
                .padding(horizontal = TrailSenseSpacing.sm, vertical = TrailSenseSpacing.xxs),
            style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (altitude.title.isNotEmpty()) {
                Text(
                    stringResource(R.string.navigation_elevation_value, altitude.title),
                    modifier = Modifier
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(TrailSenseSpacing.sm))
                        .clickable(enabled = altitude.onClick != null) { altitude.onClick?.invoke() }
                        .padding(horizontal = TrailSenseSpacing.xs, vertical = TrailSenseSpacing.xxs),
                    style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            if (speed.title.isNotEmpty()) {
                NavigationDot()
                speed.Content()
            }
        }
    }
}

@Composable
private fun NavigationDot() {
    Text(
        text = "·",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
internal fun NavigatorHost(
    views: NavigatorViews,
    modifier: Modifier = Modifier
) {
    // Which way am I facing, and which way do I go? One object answers: the dial, with the heading in a cookie at its
    // centre that turns as you turn. Everything else is quiet: sensor trouble in the top bar's empty corner, the quick
    // actions as plain icons, one centred where-you-are line, and one labelled action.
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = TrailSenseSpacing.lg)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // The sensor status, then which north the dial points to (upstream's north reference badge)
                Row(
                    Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(TrailSenseSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    views.accuracyView.Content(quiet = true)
                    views.northReferenceIndicator.Content()
                }
                views.navigationTitle.leftButton.Content(offContainerColor = androidx.compose.ui.graphics.Color.Transparent)
                views.navigationTitle.rightButton.Content(offContainerColor = androidx.compose.ui.graphics.Color.Transparent)
            }
            // The dial and where-you-are are one group, centred in all the room between the top bar and the actions;
            // the dial takes nearly the full width.
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                // The dial is the screen's one object: it runs past the page margins to nearly the full width.
                val compassSize = minOf(maxWidth, maxHeight - 72.dp)
                Column(
                    Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(Modifier.size(compassSize)) {
                        val radarModifier = Modifier.fillMaxSize()
                        views.radarCompass.Content(radarModifier.testTag("radar_compass"))
                        // Nearby paths are a quiet hint under the dial's marks, not a stripe across it.
                        views.radarCompassMap.Content(radarModifier.then(Modifier.alpha(0.4f)))
                        NavigationTitle(views.navigationTitle, Modifier.align(Alignment.Center))
                        views.mapAttribution.Content(
                            Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                        )
                    }
                    NavigationWhereabouts(
                        views.navigationTitle,
                        views.altitude,
                        views.speed,
                        Modifier.padding(top = TrailSenseSpacing.xs)
                    )
                }
                views.linearCompass.Content(
                    Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth()
                        .testTag("linear_compass")
                )
            }
            views.navigationSheet.Content()
            // The two things most people come here for: go back to a place they saved, or save where they stand
            // (the car, the camp). Two plain, labelled buttons; no hidden long press.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = TrailSenseSpacing.xs, bottom = TrailSenseSpacing.lg),
                horizontalArrangement = Arrangement.spacedBy(TrailSenseSpacing.sm)
            ) {
                com.kylecorry.trail_sense.shared.views.compose.TrailSenseButton(
                    text = stringResource(R.string.navigation_places),
                    onClick = { views.onBeaconClick?.invoke() },
                    icon = TrailSenseIcons.Filled.LocationOn,
                    style = com.kylecorry.trail_sense.shared.views.compose.TrailSenseButtonStyle.Tonal,
                    modifier = Modifier.weight(1f)
                )
                com.kylecorry.trail_sense.shared.views.compose.TrailSenseButton(
                    text = stringResource(R.string.navigation_save_spot),
                    onClick = { views.onBeaconLongClick?.invoke() },
                    icon = TrailSenseIcons.Filled.Add,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

internal fun createNavigatorViews(
    context: Context,
    onNorthReferenceSettingsClick: (() -> Unit)? = null,
    requestCameraPermission: (((Boolean) -> Unit) -> Unit)? = null,
    onCameraPermissionDenied: (() -> Unit)? = null,
    onOpenBeacon: ((Long) -> Unit)? = null
): NavigatorViews {
    val title = NavigationTitleState()
    val altitude = NavigatorDataPointState(TrailSenseIcons.Filled.Straighten)
    val speed = NavigatorDataPointState(TrailSenseIcons.Filled.Speed)
    val radarCompassMap = MapView(context).apply {
        id = R.id.radar_compass_map
    }

    val radarCompass = RadarCompassView(context).apply {
        isInvisible = true
    }

    val linearCompass = LinearSightingCompassView(context).apply {
        this.requestCameraPermission = requestCameraPermission
        this.onCameraPermissionDenied = onCameraPermissionDenied
    }

    val mapAttribution = MapAttributionState()
    val northReferenceIndicator = NorthReferenceBadgeState().apply {
        onSettingsClick = onNorthReferenceSettingsClick
    }

    val accuracyView = SensorStatusBadgeView(context).apply {
        alpha = 0.6f
    }

    val navigationSheet = NavigationSheetView().apply {
        this.onOpenBeacon = onOpenBeacon
    }

    return NavigatorViews(
        navigationTitle = title,
        altitude = altitude,
        speed = speed,
        radarCompassMap = radarCompassMap,
        radarCompass = radarCompass,
        linearCompass = linearCompass,
        mapAttribution = mapAttribution,
        northReferenceIndicator = northReferenceIndicator,
        accuracyView = accuracyView,
        navigationSheet = navigationSheet
    )
}

/** The beacon button: a standard tonal FAB; long press navigates to the last beacon. */
@OptIn(ExperimentalFoundationApi::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun NavigatorIconButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // An extended FAB that says what it does (ChangeDetection's "Watch a page"): open the beacons; a long press
    // places a beacon here.
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
    androidx.compose.material3.Surface(
        modifier = modifier
            .height(56.dp)
            .clip(shape)
            .combinedClickable(
                onClickLabel = label,
                onLongClickLabel = stringResource(R.string.create_beacon),
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = shape,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(24.dp))
            Text(label, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NavigatorHostPreview() {
    TrailSenseTheme {
        NavigatorHost(
            views = createNavigatorViews(LocalContext.current)
        )
    }
}

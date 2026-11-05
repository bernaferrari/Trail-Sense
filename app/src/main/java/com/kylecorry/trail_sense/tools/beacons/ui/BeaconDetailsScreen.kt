@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.beacons.ui

import com.kylecorry.trail_sense.shared.views.compose.routeAs

import com.kylecorry.trail_sense.shared.navigation.Route

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.Surface
import androidx.compose.material3.toShape
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupHeader
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseShapeBadge
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpotlight
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpotlightAction
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseTone
import com.kylecorry.trail_sense.shared.views.compose.trailSenseGridCellShape
import com.kylecorry.trail_sense.shared.icons.trail.*
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.graphics.vector.ImageVector
import android.os.Bundle
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.openTool
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.toRelativeDistance
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupCard
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupPosition
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseHeroCard
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseListItem
import com.kylecorry.trail_sense.shared.views.compose.TrailSensePage
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.compose.trailSenseSection
import com.kylecorry.trail_sense.tools.navigation.domain.NavigationService
import com.kylecorry.trail_sense.tools.astronomy.domain.AstronomyService
import com.kylecorry.trail_sense.tools.beacons.domain.Beacon
import com.kylecorry.trail_sense.tools.beacons.infrastructure.persistence.BeaconEntity
import com.kylecorry.trail_sense.tools.beacons.infrastructure.persistence.BeaconRepo
import com.kylecorry.trail_sense.tools.beacons.infrastructure.share.BeaconSender
import com.kylecorry.trail_sense.tools.tides.subsystem.TidesSubsystem
import com.kylecorry.trail_sense.tools.tides.ui.TideFormatter
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.weather.infrastructure.subsystem.WeatherSubsystem
import com.kylecorry.trail_sense.tools.weather.ui.dialogs.ShowHighLowTemperatureDialogCommand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

@Composable
fun BeaconDetailsToolScreen(host: TrailSenseDestinationHost) {
    val controller = remember(host) { BeaconDetailsController(host) }
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(controller) {
        controller.loadBeacon()
    }

    DisposableEffect(controller, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> controller.onResume()
                Lifecycle.Event.ON_PAUSE -> controller.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            controller.onResume()
        }
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            controller.onPause()
        }
    }

    BeaconDetailsScreen(
        beacon = controller.beacon,
        distance = controller.distance,
        direction = controller.direction,
        altitude = controller.altitude,
        temperature = controller.temperature,
        sunrise = controller.sunrise,
        sunset = controller.sunset,
        tide = controller.tide,
        tideIcon = controller.tideIcon,
        onShare = controller::shareBeacon,
        onDelete = controller::confirmDelete,
        onNavigate = controller::navigateToBeacon,
        onEdit = controller::editBeacon,
        onTemperatureClick = controller::showTemperatureDetails
    )
}

private class BeaconDetailsController(private val host: TrailSenseDestinationHost) {

    private val beaconRepo by lazy { BeaconRepo.getInstance(host.requireContext()) }
    private val formatService by lazy { FormatService.getInstance(host.requireContext()) }
    private val prefs by lazy { UserPreferences(host.requireContext()) }
    private val gps by lazy { SensorService(host.requireContext()).getGPS() }
    private val weather by lazy { WeatherSubsystem.getInstance(host.requireContext()) }
    private val tides by lazy { TidesSubsystem.getInstance(host.requireContext()) }
    private val tideFormatter by lazy { TideFormatter(host.requireContext()) }
    private val astronomy = AstronomyService()

    private val beaconId: Long by lazy {
        host.routeAs<Route.BeaconDetails>().beaconId
    }
    var beacon by mutableStateOf<Beacon?>(null)
        private set
    var distance by mutableStateOf("")
        private set
    var direction by mutableStateOf<String?>(null)
        private set
    var altitude by mutableStateOf<String?>(null)
        private set
    var temperature by mutableStateOf("")
        private set
    var sunrise by mutableStateOf<String?>(null)
        private set
    var sunset by mutableStateOf<String?>(null)
        private set
    var tide by mutableStateOf<String?>(null)
        private set
    var tideIcon by mutableStateOf(TrailIcons.TideHalf)
        private set

    fun onResume() {
        gps.start(this::onGpsUpdate)
    }

    fun onPause() {
        gps.stop(this::onGpsUpdate)
    }

    fun loadBeacon() {
        host.lifecycleScope.launch {
            val loaded = withContext(Dispatchers.IO) {
                beaconRepo.getBeacon(beaconId)?.toBeacon()
            }
            beacon = loaded
            loaded ?: return@launch

            altitude = loaded.elevation?.let {
                val d = Distance.meters(it).convertTo(prefs.baseDistanceUnits)
                formatService.formatDistance(d, Units.getDecimalPlaces(d.units), false)
            }

            updateDistance()
            updateBeaconTemperature(loaded)
            updateBeaconSunTimes(loaded)
            updateBeaconTides(loaded)
        }
    }

    private fun onGpsUpdate(): Boolean {
        updateDistance()
        return true
    }

    private fun updateDistance() {
        val currentBeacon = beacon ?: return
        val meters = currentBeacon.coordinate.distanceTo(gps.location)
        // Standing on it: "0 ft · 0° N" was true and useless; say it plainly and drop the direction.
        if (meters < HERE_METERS) {
            distance = host.getString(R.string.place_you_are_here)
            direction = ""
            return
        }
        val d = Distance.meters(meters)
            .convertTo(prefs.baseDistanceUnits)
            .toRelativeDistance()
        distance = formatService.formatDistance(d, Units.getDecimalPlaces(d.units), false)
        val bearing = NavigationService().navigate(gps.location, currentBeacon.coordinate, 0f).direction
        direction = host.getString(
            R.string.beacon_direction_value,
            formatService.formatDegrees(bearing.value, replace360 = true),
            formatService.formatDirection(bearing.direction)
        )
    }

    private suspend fun updateBeaconSunTimes(beacon: Beacon) {
        val times = withContext(Dispatchers.Default) {
            astronomy.getSunTimes(
                beacon.coordinate,
                prefs.astronomy.sunTimesMode,
                LocalDate.now()
            )
        }

        sunrise = times.rise?.let {
            formatService.formatTime(it.toLocalTime(), includeSeconds = false)
        }
        sunset = times.set?.let {
            formatService.formatTime(it.toLocalTime(), includeSeconds = false)
        }
    }

    private suspend fun updateBeaconTides(beacon: Beacon) {
        val nearestTide = tides.getNearestTide(beacon.coordinate)
        tide = nearestTide?.let { tideFormatter.getTideTypeName(it.now.type) }
        tideIcon = tideFormatter.getTideTypeIcon(nearestTide?.now?.type)
    }

    private suspend fun updateBeaconTemperature(beacon: Beacon) {
        val temperatureRange = weather.getTemperatureRange(
            LocalDate.now(),
            beacon.coordinate,
            Distance.meters(beacon.elevation ?: 0f)
        )

        val units = prefs.temperatureUnits
        val lowValue = formatService.formatTemperature(
            temperatureRange.start.convertTo(units)
        )
        val highValue = formatService.formatTemperature(
            temperatureRange.end.convertTo(units)
        )

        temperature = host.getString(R.string.slash_separated_pair, highValue, lowValue)
    }

    fun showTemperatureDetails() {
        val currentBeacon = beacon ?: return
        host.lifecycleScope.launch {
            ShowHighLowTemperatureDialogCommand(
                host.requireContext(),
                currentBeacon.coordinate,
                Distance.meters(currentBeacon.elevation ?: 0f)
            ).execute()
        }
    }

    fun navigateToBeacon() {
        host.findNavigator().navigate(Route.Navigation(destinationBeaconId = beaconId))
    }

    fun editBeacon() {
        host.findNavigator().navigate(Route.PlaceBeacon(editBeaconId = beaconId))
    }

    fun shareBeacon() {
        val currentBeacon = beacon ?: return
        BeaconSender(host).send(currentBeacon)
    }

    fun confirmDelete() {
        val beacon = beacon ?: return
        Alerts.dialog(
            host.requireContext(),
            host.getString(R.string.delete),
            beacon.name
        ) { cancelled ->
            if (!cancelled) {
                host.lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        beaconRepo.deleteBeacon(BeaconEntity.from(beacon))
                    }
                    host.findNavigator().navigateUp()
                }
            }
        }
    }
}

/**
 * Beacon details: the beacon's name as the page title, a hero with the one thing you open a beacon for (how far it is,
 * which way, and where), Navigate as the hero's action, then the conditions at the beacon and its notes as groups.
 * Edit, share and delete live in the top bar.
 */
@Composable
private fun BeaconDetailsScreen(
    beacon: Beacon?,
    distance: String,
    direction: String?,
    altitude: String?,
    temperature: String,
    sunrise: String?,
    sunset: String?,
    tide: String?,
    tideIcon: ImageVector,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onNavigate: () -> Unit,
    onEdit: () -> Unit,
    onTemperatureClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val coordinates = beacon?.coordinate?.let {
        FormatService.getInstance(context).formatLocation(it)
    }.orEmpty()
    val comment = beacon?.comment.orEmpty()
    val placeholder = VALUE_PLACEHOLDER
    val detailsTitle = stringResource(R.string.details)
    val notesTitle = stringResource(R.string.notes)

    TrailSensePage(
        title = beacon?.name.orEmpty(),
        onBack = { dispatcher?.onBackPressed() },
        backLabel = stringResource(R.string.back),
        modifier = modifier.testTag("beacon_title"),
        actions = {
            if (beacon != null) {
                BeaconDetailsActions(
                    showEdit = !beacon.temporary,
                    onEdit = onEdit,
                    onShare = onShare,
                    onDelete = onDelete
                )
            }
        }
    ) {
        if (beacon == null) return@TrailSensePage

        // How far is this place, and which way? The distance answers it; Navigate is the one filled action.
        item(key = "hero") {
            // The beacon's own mark (the shape, colour and icon it has on the map) leads the header, so the page
            // is recognisably this place.
            Row(verticalAlignment = Alignment.Top) {
            BeaconShapeMark(
                shape = beacon.shape,
                color = ComposeColor(beacon.color),
                icon = beacon.icon?.icon ?: TrailIcons.Location,
                size = 56.dp,
                modifier = Modifier.padding(start = TrailSenseSpacing.lg, top = TrailSenseSpacing.md)
            )
            com.kylecorry.trail_sense.tools.beacons.ui.places.PlacesKeyValue(
                modifier = Modifier
                    .weight(1f)
                    .testTag("beacon_distance"),
                value = distance.ifBlank { placeholder },
                verdict = direction?.ifBlank { null },
                below = {
                    if (coordinates.isNotBlank()) {
                        SelectionContainer {
                            Text(
                                text = coordinates,
                                style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(Modifier.height(TrailSenseSpacing.sm))
                    com.kylecorry.trail_sense.shared.views.compose.TrailSenseButton(
                        text = stringResource(R.string.navigate),
                        onClick = onNavigate,
                        icon = TrailIcons.NavigationArrow
                    )
                }
            )
            }
        }

        item(key = "conditions") {
            val tiles = listOfNotNull(
                altitude?.let {
                    BeaconTile("beacon_altitude", stringResource(R.string.elevation), it, TrailIcons.Altitude)
                },
                BeaconTile(
                    "beacon_temperature",
                    stringResource(R.string.temperature_high_low),
                    temperature.ifBlank { placeholder },
                    TrailIcons.TemperatureRange,
                    onClick = onTemperatureClick
                ),
                sunrise?.let {
                    BeaconTile("beacon_sunrise", stringResource(R.string.sunrise), it, TrailIcons.SunriseNotification, warm = true)
                },
                sunset?.let {
                    BeaconTile("beacon_sunset", stringResource(R.string.sunset), it, TrailIcons.SunsetNotification, warm = true)
                },
                tide?.let {
                    BeaconTile("beacon_tide", stringResource(R.string.tide), it, tideIcon)
                }
            )
            Column {
                TrailSenseGroupHeader(detailsTitle)
                BeaconTileGrid(tiles)
            }
        }

        if (comment.isNotBlank()) {
            trailSenseSection(key = "notes", title = notesTitle) {
                row { position ->
                    TrailSenseGroupCard(position = position) {
                        SelectionContainer(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("comment_text")
                        ) {
                            Text(
                                text = comment,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BeaconDetailsActions(
    showEdit: Boolean,
    onEdit: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    var isMenuOpen by remember { mutableStateOf(false) }
    if (showEdit) {
        IconButton(
            onClick = onEdit,
            modifier = Modifier.testTag("edit_btn")
        ) {
            Icon(
                painter = rememberVectorPainter(TrailIcons.Edit),
                contentDescription = stringResource(R.string.edit)
            )
        }
    }
    Box {
        IconButton(
            onClick = { isMenuOpen = true },
            modifier = Modifier.testTag("beacon_menu_btn")
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = stringResource(R.string.beacon_more_options)
            )
        }
        DropdownMenu(
            expanded = isMenuOpen,
            onDismissRequest = { isMenuOpen = false }
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.share_ellipsis)) },
                leadingIcon = { Icon(rememberVectorPainter(TrailIcons.Share), contentDescription = null) },
                onClick = {
                    isMenuOpen = false
                    onShare()
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.delete)) },
                leadingIcon = { Icon(rememberVectorPainter(TrailIcons.Delete), contentDescription = null) },
                onClick = {
                    isMenuOpen = false
                    onDelete()
                }
            )
        }
    }
}

private data class BeaconTile(
    val tag: String,
    val label: String,
    val value: String,
    val icon: ImageVector,
    val warm: Boolean = false,
    val onClick: (() -> Unit)? = null
)

/**
 * Conditions at the beacon as connected quiet value tiles, two to a row. Each tile keeps its test tag.
 */
@Composable
private fun BeaconTileGrid(tiles: List<BeaconTile>) {
    val rows = tiles.chunked(2)
    val scheme = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xxs)) {
        rows.forEachIndexed { rowIndex, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xxs)) {
                row.forEachIndexed { columnIndex, tile ->
                    // Every tile quiet: the distance above is the screen's one emphasised value.
                    val container = scheme.surfaceContainer
                    val content = scheme.onSurface
                    val shape = trailSenseGridCellShape(columnIndex, if (row.size == 1 && rows.size > 1) 2 else row.size, rowIndex, rows.size)
                    val body: @Composable () -> Unit = {
                        Column(
                            Modifier
                                .heightIn(min = 96.dp)
                                .padding(TrailSenseSpacing.lg),
                            verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xs)
                        ) {
                            Icon(
                                rememberVectorPainter(tile.icon),
                                contentDescription = null,
                                tint = scheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                tile.value,
                                style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                                fontWeight = FontWeight.Medium,
                                maxLines = 1
                            )
                            Text(
                                tile.label,
                                style = MaterialTheme.typography.labelMedium,
                                color = content.copy(alpha = 0.75f),
                                maxLines = 1
                            )
                        }
                    }
                    val tileModifier = Modifier
                        .weight(1f)
                        .testTag(tile.tag)
                    if (tile.onClick != null) {
                        Surface(onClick = tile.onClick, modifier = tileModifier, shape = shape, color = container, contentColor = content) { body() }
                    } else {
                        Surface(modifier = tileModifier, shape = shape, color = container, contentColor = content) { body() }
                    }
                }
                if (row.size == 1 && rows.size > 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BeaconDetailsScreenPreview() {
    TrailSenseTheme {
        BeaconDetailsScreen(
            beacon = null,
            distance = "1.2 mi",
            direction = "45° NE",
            altitude = "1000 ft",
            temperature = "80 °F / 60 °F",
            sunrise = "6:00 AM",
            sunset = "7:30 PM",
            tide = "High",
            tideIcon = TrailIcons.TideHigh,
            onShare = {},
            onDelete = {},
            onNavigate = {},
            onEdit = {},
            onTemperatureClick = {}
        )
    }
}

private const val VALUE_PLACEHOLDER = "--"

/** Closer than this, GPS can't tell you from the place: you are there. */
private const val HERE_METERS = 10f

@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.beacons.ui

import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kylecorry.sol.math.Range
import com.kylecorry.sol.science.astronomy.RiseSetTransitTimes
import com.kylecorry.sol.science.astronomy.SunTimesMode
import com.kylecorry.sol.units.Temperature
import com.kylecorry.trail_sense.shared.navigation.LocalNavigator
import com.kylecorry.trail_sense.shared.views.compose.LocalScreenEffects
import com.kylecorry.trail_sense.tools.tides.domain.TideDetails
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow


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
fun BeaconDetailsToolScreen(route: Route.BeaconDetails) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val navigator = LocalNavigator.current
    val effects = LocalScreenEffects.current
    val scope = rememberCoroutineScope()
    val prefs = remember(appContext) { UserPreferences(appContext) }
    val viewModel = viewModel {
        BeaconDetailsViewModel(
            beaconId = route.beaconId,
            beaconRepo = BeaconRepo.getInstance(appContext),
            weather = WeatherSubsystem.getInstance(appContext),
            tides = TidesSubsystem.getInstance(appContext),
            sunTimesMode = prefs.astronomy.sunTimesMode
        )
    }
    val formatService = remember(appContext) { FormatService.getInstance(appContext) }
    val tideFormatter = remember(appContext) { TideFormatter(appContext) }
    val gps = remember(appContext) { SensorService(appContext).getGPS() }
    var location by remember { mutableStateOf(gps.location) }

    // Loads each time the page is shown, so an edit made in the editor shows on return.
    LaunchedEffect(viewModel) {
        viewModel.load()
    }
    LaunchedEffect(viewModel) {
        viewModel.deleted.collect { navigator.navigateUp() }
    }

    // The distance and direction follow you while the page is shown.
    LifecycleResumeEffect(gps) {
        val onGpsUpdate: () -> Boolean = {
            location = gps.location
            true
        }
        gps.start(onGpsUpdate)
        onPauseOrDispose {
            gps.stop(onGpsUpdate)
        }
    }

    val beacon = viewModel.beacon
    val loaded = viewModel.isLoaded
    val meters = beacon?.coordinate?.distanceTo(location)
    // Standing on it: "0 ft · 0° N" was true and useless; say it plainly and drop the direction.
    val isHere = meters != null && meters < HERE_METERS
    val distance = when {
        meters == null || !loaded -> ""
        isHere -> stringResource(R.string.place_you_are_here)
        else -> {
            val d = Distance.meters(meters).convertTo(prefs.baseDistanceUnits).toRelativeDistance()
            formatService.formatDistance(d, Units.getDecimalPlaces(d.units), false)
        }
    }
    val direction = when {
        beacon == null || !loaded -> null
        isHere -> ""
        else -> {
            val bearing = NavigationService().navigate(location, beacon.coordinate, 0f).direction
            stringResource(
                R.string.beacon_direction_value,
                formatService.formatDegrees(bearing.value, replace360 = true),
                formatService.formatDirection(bearing.direction)
            )
        }
    }
    val altitude = beacon?.elevation?.let {
        val d = Distance.meters(it).convertTo(prefs.baseDistanceUnits)
        formatService.formatDistance(d, Units.getDecimalPlaces(d.units), false)
    }
    val temperature = viewModel.temperatureRange?.let { range ->
        val units = prefs.temperatureUnits
        stringResource(
            R.string.slash_separated_pair,
            formatService.formatTemperature(range.end.convertTo(units)),
            formatService.formatTemperature(range.start.convertTo(units))
        )
    } ?: ""
    val sunTimes = viewModel.sunTimes
    val tideDetails = viewModel.nearestTide

    BeaconDetailsScreen(
        beacon = beacon,
        distance = distance,
        direction = direction,
        altitude = altitude,
        temperature = temperature,
        sunrise = sunTimes?.rise?.let { formatService.formatTime(it.toLocalTime(), includeSeconds = false) },
        sunset = sunTimes?.set?.let { formatService.formatTime(it.toLocalTime(), includeSeconds = false) },
        tide = tideDetails?.let { tideFormatter.getTideTypeName(it.now.type) },
        tideIcon = if (viewModel.isTideLoaded) {
            tideFormatter.getTideTypeIcon(tideDetails?.now?.type)
        } else {
            TrailIcons.TideHalf
        },
        onShare = {
            viewModel.beacon?.let { BeaconSender(context, effects, navigator, scope).send(it) }
        },
        onDelete = {
            val current = viewModel.beacon
            if (current != null) {
                Alerts.dialog(
                    context,
                    context.getString(R.string.delete),
                    current.name
                ) { cancelled ->
                    if (!cancelled) {
                        viewModel.delete(current)
                    }
                }
            }
        },
        onNavigate = { navigator.navigate(Route.Navigation(destinationBeaconId = route.beaconId)) },
        onEdit = { navigator.navigate(Route.PlaceBeacon(editBeaconId = route.beaconId)) },
        onTemperatureClick = {
            val current = viewModel.beacon
            if (current != null) {
                scope.launch {
                    ShowHighLowTemperatureDialogCommand(
                        context,
                        current.coordinate,
                        Distance.meters(current.elevation ?: 0f)
                    ).execute()
                }
            }
        }
    )
}

/**
 * A beacon's details: the beacon and the conditions at it (temperature range, sun times, the nearest tide), loaded
 * from the repositories. Plain Kotlin; the screen turns the values into text and reports [deleted] once it is gone.
 */
internal class BeaconDetailsViewModel(
    private val beaconId: Long,
    private val beaconRepo: BeaconRepo,
    private val weather: WeatherSubsystem,
    private val tides: TidesSubsystem,
    private val sunTimesMode: SunTimesMode
) : ViewModel() {

    private val astronomy = AstronomyService()

    var beacon by mutableStateOf<Beacon?>(null)
        private set

    /** Whether [load] has finished at least once. */
    var isLoaded by mutableStateOf(false)
        private set
    var temperatureRange by mutableStateOf<Range<Temperature>?>(null)
        private set
    var sunTimes by mutableStateOf<RiseSetTransitTimes?>(null)
        private set
    var nearestTide by mutableStateOf<TideDetails?>(null)
        private set
    var isTideLoaded by mutableStateOf(false)
        private set

    private val _deleted = Channel<Unit>(Channel.CONFLATED)

    /** One event once the beacon is deleted. */
    val deleted: Flow<Unit> = _deleted.receiveAsFlow()

    suspend fun load() {
        val loaded = withContext(Dispatchers.IO) {
            beaconRepo.getBeacon(beaconId)?.toBeacon()
        }
        beacon = loaded
        loaded ?: return
        isLoaded = true

        temperatureRange = weather.getTemperatureRange(
            LocalDate.now(),
            loaded.coordinate,
            Distance.meters(loaded.elevation ?: 0f)
        )
        sunTimes = withContext(Dispatchers.Default) {
            astronomy.getSunTimes(loaded.coordinate, sunTimesMode, LocalDate.now())
        }
        nearestTide = tides.getNearestTide(loaded.coordinate)
        isTideLoaded = true
    }

    fun delete(beacon: Beacon) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                beaconRepo.deleteBeacon(BeaconEntity.from(beacon))
            }
            _deleted.trySend(Unit)
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

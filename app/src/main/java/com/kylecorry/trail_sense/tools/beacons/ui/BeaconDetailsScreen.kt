@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.beacons.ui

import com.kylecorry.trail_sense.shared.icons.trail.*
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import android.os.Bundle
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.res.painterResource
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
        host.destinationArguments?.getLong("beacon_id") ?: error("Missing beacon id")
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
    var tideIcon by mutableStateOf(R.drawable.ic_tide_half)
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
        val d = Distance.meters(currentBeacon.coordinate.distanceTo(gps.location))
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
        tideIcon = tideFormatter.getTideTypeImage(nearestTide?.now?.type)
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
        val bundle = Bundle().apply {
            putLong("destination", beaconId)
        }
        host.findNavController().openTool(Tools.NAVIGATION, bundle)
    }

    fun editBeacon() {
        val bundle = Bundle().apply {
            putLong("edit_beacon", beaconId)
        }
        host.findNavController().navigate(R.id.action_beacon_details_to_beacon_edit, bundle)
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
                    host.findNavController().navigateUp()
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
    @DrawableRes tideIcon: Int,
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

        item(key = "hero") {
            TrailSenseHeroCard(
                modifier = Modifier.testTag("beacon_distance"),
                eyebrow = stringResource(R.string.distance),
                title = distance.ifBlank { placeholder },
                supportingText = direction,
                icon = painterResource(beacon.icon?.icon ?: R.drawable.ic_location),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                if (coordinates.isNotBlank()) {
                    SelectionContainer {
                        Text(
                            text = coordinates,
                            style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.82f)
                        )
                    }
                }
                Button(
                    onClick = onNavigate,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .padding(top = TrailSenseSpacing.xs)
                ) {
                    Icon(
                        painter = rememberVectorPainter(TrailIcons.NavigationArrow),
                        contentDescription = null,
                        modifier = Modifier.size(ButtonDefaults.IconSize)
                    )
                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                    Text(stringResource(R.string.navigate))
                }
            }
        }

        trailSenseSection(key = "conditions", title = detailsTitle) {
            altitude?.let {
                row { position ->
                    DetailRow(
                        tag = "beacon_altitude",
                        title = it,
                        description = stringResource(R.string.elevation),
                        icon = R.drawable.ic_altitude,
                        position = position
                    )
                }
            }
            row { position ->
                DetailRow(
                    tag = "beacon_temperature",
                    title = temperature.ifBlank { placeholder },
                    description = stringResource(R.string.temperature_high_low),
                    icon = R.drawable.ic_temperature_range,
                    position = position,
                    onClick = onTemperatureClick
                )
            }
            sunrise?.let {
                row { position ->
                    DetailRow(
                        tag = "beacon_sunrise",
                        title = it,
                        description = stringResource(R.string.sunrise),
                        icon = R.drawable.ic_sunrise_notification,
                        position = position
                    )
                }
            }
            sunset?.let {
                row { position ->
                    DetailRow(
                        tag = "beacon_sunset",
                        title = it,
                        description = stringResource(R.string.sunset),
                        icon = R.drawable.ic_sunset_notification,
                        position = position
                    )
                }
            }
            tide?.let {
                row { position ->
                    DetailRow(
                        tag = "beacon_tide",
                        title = it,
                        description = stringResource(R.string.tide),
                        icon = tideIcon,
                        position = position
                    )
                }
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

@Composable
private fun DetailRow(
    tag: String,
    title: String,
    description: String,
    @DrawableRes icon: Int,
    position: TrailSenseGroupPosition,
    onClick: (() -> Unit)? = null
) {
    TrailSenseListItem(
        title = title,
        subtitle = description,
        icon = painterResource(icon),
        position = position,
        onClick = onClick,
        modifier = Modifier.testTag(tag)
    )
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
            tideIcon = R.drawable.ic_tide_high,
            onShare = {},
            onDelete = {},
            onNavigate = {},
            onEdit = {},
            onTemperatureClick = {}
        )
    }
}

private const val VALUE_PLACEHOLDER = "--"

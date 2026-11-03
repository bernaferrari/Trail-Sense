@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.beacons.ui

import android.os.Bundle
import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseButton
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseCard
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
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

@Composable
private fun BeaconDetailsScreen(
    beacon: Beacon?,
    distance: String,
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
    val coordinates = beacon?.coordinate?.let {
        FormatService.getInstance(context).formatLocation(it)
    }.orEmpty()
    val comment = beacon?.comment.orEmpty()

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(TrailSenseSpacing.screen),
                verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xl)
            ) {
                BeaconHero(
                    title = beacon?.name.orEmpty(),
                    coordinates = coordinates,
                    distance = distance,
                    onShare = onShare,
                    onDelete = onDelete
                )

                BeaconDetailsCard(
                    altitude = altitude,
                    distance = distance,
                    temperature = temperature,
                    sunrise = sunrise,
                    sunset = sunset,
                    tide = tide,
                    tideIcon = tideIcon,
                    onTemperatureClick = onTemperatureClick
                )

                if (comment.isNotBlank()) {
                    NotesCard(comment = comment)
                }
            }

            ActionRow(
                showEdit = beacon?.temporary != true,
                onNavigate = onNavigate,
                onEdit = onEdit
            )
        }
    }
}

@Composable
private fun BeaconHero(
    title: String,
    coordinates: String,
    distance: String,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    var isMenuOpen by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(TrailSenseSpacing.xl)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = title,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("beacon_title"),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Box {
                    IconButton(
                        onClick = { isMenuOpen = true },
                        modifier = Modifier.testTag("beacon_menu_btn")
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_menu_dots),
                            contentDescription = stringResource(R.string.details),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    DropdownMenu(
                        expanded = isMenuOpen,
                        onDismissRequest = { isMenuOpen = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.share_ellipsis)) },
                            onClick = {
                                isMenuOpen = false
                                onShare()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.delete)) },
                            onClick = {
                                isMenuOpen = false
                                onDelete()
                            }
                        )
                    }
                }
            }

            if (distance.isNotBlank()) {
                Text(
                    text = distance,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = stringResource(R.string.distance),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }

            if (coordinates.isNotBlank()) {
                Text(
                    text = coordinates,
                    modifier = Modifier.padding(top = TrailSenseSpacing.sm),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun BeaconDetailsCard(
    altitude: String?,
    distance: String,
    temperature: String,
    sunrise: String?,
    sunset: String?,
    tide: String?,
    @DrawableRes tideIcon: Int,
    onTemperatureClick: () -> Unit
) {
    TrailSenseCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.details),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = TrailSenseSpacing.md),
            verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.md)
        ) {
            altitude?.let {
                DetailRow(
                    tag = "beacon_altitude",
                    title = it,
                    description = stringResource(R.string.elevation),
                    icon = R.drawable.ic_altitude
                )
            }
            DetailRow(
                tag = "beacon_distance",
                title = distance,
                description = stringResource(R.string.distance),
                icon = R.drawable.ruler
            )
            DetailRow(
                tag = "beacon_temperature",
                title = temperature,
                description = stringResource(R.string.temperature_high_low),
                icon = R.drawable.ic_temperature_range,
                onClick = onTemperatureClick
            )
            sunrise?.let {
                DetailRow(
                    tag = "beacon_sunrise",
                    title = it,
                    description = stringResource(R.string.sunrise),
                    icon = R.drawable.ic_sunrise_notification
                )
            }
            sunset?.let {
                DetailRow(
                    tag = "beacon_sunset",
                    title = it,
                    description = stringResource(R.string.sunset),
                    icon = R.drawable.ic_sunset_notification
                )
            }
            tide?.let {
                DetailRow(
                    tag = "beacon_tide",
                    title = it,
                    description = stringResource(R.string.tide),
                    icon = tideIcon
                )
            }
        }
    }
}

@Composable
private fun DetailRow(
    tag: String,
    title: String,
    description: String,
    @DrawableRes icon: Int,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        Column(modifier = Modifier.padding(start = TrailSenseSpacing.lg)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun NotesCard(comment: String) {
    TrailSenseCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.notes),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
        SelectionContainer(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = TrailSenseSpacing.md)
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

@Composable
private fun ActionRow(
    showEdit: Boolean,
    onNavigate: () -> Unit,
    onEdit: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(TrailSenseSpacing.screen),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TrailSenseSpacing.md)
        ) {
            TrailSenseButton(
                text = stringResource(R.string.navigate),
                onClick = onNavigate,
                modifier = Modifier.weight(1f)
            )
            if (showEdit) {
                FilledTonalIconButton(
                    onClick = onEdit,
                    modifier = Modifier
                        .size(56.dp)
                        .testTag("edit_btn"),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_edit),
                        contentDescription = stringResource(R.string.edit)
                    )
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

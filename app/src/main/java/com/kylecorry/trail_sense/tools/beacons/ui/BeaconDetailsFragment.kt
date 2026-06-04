package com.kylecorry.trail_sense.tools.beacons.ui

import android.os.Bundle
import android.widget.ImageButton
import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.openTool
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.toRelativeDistance
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
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

class BeaconDetailsFragment : Fragment() {

    private val beaconRepo by lazy { BeaconRepo.getInstance(requireContext()) }
    private val formatService by lazy { FormatService.getInstance(requireContext()) }
    private val prefs by lazy { UserPreferences(requireContext()) }
    private val gps by lazy { SensorService(requireContext()).getGPS() }
    private val weather by lazy { WeatherSubsystem.getInstance(requireContext()) }
    private val tides by lazy { TidesSubsystem.getInstance(requireContext()) }
    private val tideFormatter by lazy { TideFormatter(requireContext()) }
    private val astronomy = AstronomyService()

    private var beaconId: Long = 0L
    private var beacon by mutableStateOf<Beacon?>(null)
    private var distance by mutableStateOf("")
    private var altitude by mutableStateOf<String?>(null)
    private var temperature by mutableStateOf("")
    private var sunrise by mutableStateOf<String?>(null)
    private var sunset by mutableStateOf<String?>(null)
    private var tide by mutableStateOf<String?>(null)
    private var tideIcon by mutableStateOf(R.drawable.ic_tide_half)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        beaconId = requireArguments().getLong("beacon_id")
    }

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: android.view.ViewGroup?,
        savedInstanceState: Bundle?
    ): android.view.View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TrailSenseTheme {
                    BeaconDetailsScreen(
                        beacon = beacon,
                        distance = distance,
                        altitude = altitude,
                        temperature = temperature,
                        sunrise = sunrise,
                        sunset = sunset,
                        tide = tide,
                        tideIcon = tideIcon,
                        onMenuClick = ::showMenu,
                        onNavigate = ::navigateToBeacon,
                        onEdit = ::editBeacon,
                        onTemperatureClick = ::showTemperatureDetails
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: android.view.View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadBeacon()
    }

    override fun onResume() {
        super.onResume()
        gps.start(this::onGpsUpdate)
    }

    override fun onPause() {
        gps.stop(this::onGpsUpdate)
        super.onPause()
    }

    private fun loadBeacon() {
        viewLifecycleOwner.lifecycleScope.launch {
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

        temperature = getString(R.string.slash_separated_pair, highValue, lowValue)
    }

    private fun showTemperatureDetails() {
        val currentBeacon = beacon ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            ShowHighLowTemperatureDialogCommand(
                this@BeaconDetailsFragment,
                currentBeacon.coordinate,
                Distance.meters(currentBeacon.elevation ?: 0f)
            ).execute()
        }
    }

    private fun navigateToBeacon() {
        val bundle = Bundle().apply {
            putLong("destination", beaconId)
        }
        findNavController().openTool(Tools.NAVIGATION, bundle)
    }

    private fun editBeacon() {
        val bundle = Bundle().apply {
            putLong("edit_beacon", beaconId)
        }
        findNavController().navigate(R.id.action_beacon_details_to_beacon_edit, bundle)
    }

    private fun showMenu(anchor: android.view.View) {
        val currentBeacon = beacon ?: return
        Pickers.menu(
            anchor,
            listOf(getString(R.string.share_ellipsis), getString(R.string.delete))
        ) { idx ->
            when (idx) {
                0 -> BeaconSender(this).send(currentBeacon)
                1 -> confirmDelete(currentBeacon)
            }
            true
        }
    }

    private fun confirmDelete(beacon: Beacon) {
        Alerts.dialog(
            requireContext(),
            getString(R.string.delete),
            beacon.name
        ) { cancelled ->
            if (!cancelled) {
                viewLifecycleOwner.lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        beaconRepo.deleteBeacon(BeaconEntity.from(beacon))
                    }
                    findNavController().navigateUp()
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
    onMenuClick: (android.view.View) -> Unit,
    onNavigate: () -> Unit,
    onEdit: () -> Unit,
    onTemperatureClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            BeaconHeader(
                title = beacon?.name.orEmpty(),
                subtitle = beacon?.coordinate?.let {
                    FormatService.getInstance(androidx.compose.ui.platform.LocalContext.current)
                        .formatLocation(it)
                }.orEmpty(),
                onMenuClick = onMenuClick
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                BeaconDataGrid(
                    altitude = altitude,
                    distance = distance,
                    temperature = temperature,
                    sunrise = sunrise,
                    sunset = sunset,
                    tide = tide,
                    tideIcon = tideIcon,
                    onTemperatureClick = onTemperatureClick
                )
                CommentText(comment = beacon?.comment.orEmpty())
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
private fun BeaconHeader(
    title: String,
    subtitle: String,
    onMenuClick: (android.view.View) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    modifier = Modifier.testTag("beacon_title"),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            AndroidView(
                factory = { context ->
                    ImageButton(context).apply {
                        id = R.id.beacon_menu_btn
                        background = null
                        setImageResource(R.drawable.ic_menu_dots)
                        setOnClickListener { onMenuClick(this) }
                    }
                }
            )
        }
    }
}

@Composable
private fun BeaconDataGrid(
    altitude: String?,
    distance: String,
    temperature: String,
    sunrise: String?,
    sunset: String?,
    tide: String?,
    @DrawableRes tideIcon: Int,
    onTemperatureClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            altitude?.let {
                DataPoint(
                    tag = "beacon_altitude",
                    title = it,
                    description = stringResource(R.string.elevation),
                    icon = R.drawable.ic_altitude,
                    modifier = Modifier.weight(1f)
                )
            }
            DataPoint(
                tag = "beacon_distance",
                title = distance,
                description = stringResource(R.string.distance),
                icon = R.drawable.ruler,
                modifier = Modifier.weight(1f)
            )
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            DataPoint(
                tag = "beacon_temperature",
                title = temperature,
                description = stringResource(R.string.temperature_high_low),
                icon = R.drawable.ic_temperature_range,
                onClick = onTemperatureClick,
                modifier = Modifier.weight(1f)
            )
            sunrise?.let {
                DataPoint(
                    tag = "beacon_sunrise",
                    title = it,
                    description = stringResource(R.string.sunrise),
                    icon = R.drawable.ic_sunrise_notification,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            sunset?.let {
                DataPoint(
                    tag = "beacon_sunset",
                    title = it,
                    description = stringResource(R.string.sunset),
                    icon = R.drawable.ic_sunset_notification,
                    modifier = Modifier.weight(1f)
                )
            }
            tide?.let {
                DataPoint(
                    tag = "beacon_tide",
                    title = it,
                    description = stringResource(R.string.tide),
                    icon = tideIcon,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun DataPoint(
    tag: String,
    title: String,
    description: String,
    @DrawableRes icon: Int,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .padding(8.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier
                .padding(end = 16.dp)
                .size(20.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
        )
        Column {
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
private fun CommentText(comment: String) {
    SelectionContainer(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp)
            .testTag("comment_text")
    ) {
        Text(
            text = comment,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ActionRow(
    showEdit: Boolean,
    onNavigate: () -> Unit,
    onEdit: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center
    ) {
        Button(onClick = onNavigate) {
            Icon(
                painter = painterResource(R.drawable.ic_beacon),
                contentDescription = null
            )
            Spacer(modifier = Modifier.padding(4.dp))
            Text(text = stringResource(R.string.navigate))
        }
        if (showEdit) {
            Spacer(modifier = Modifier.padding(8.dp))
            IconButton(
                onClick = onEdit,
                modifier = Modifier.testTag("edit_btn")
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_edit),
                    contentDescription = null
                )
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
            onMenuClick = {},
            onNavigate = {},
            onEdit = {},
            onTemperatureClick = {}
        )
    }
}

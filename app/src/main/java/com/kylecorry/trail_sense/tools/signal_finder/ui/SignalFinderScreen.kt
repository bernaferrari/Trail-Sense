@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.signal_finder.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.DisposableEffect
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.core.system.GeoUri
import com.kylecorry.andromeda.markdown.MarkdownService
import com.kylecorry.andromeda.signal.CellSignal
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.science.geology.Geofence
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.ApproximateCoordinate
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.openTool
import com.kylecorry.trail_sense.shared.sensors.CellSignalUtils
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseLinkedText
import com.kylecorry.trail_sense.tools.navigation.infrastructure.Navigator
import com.kylecorry.trail_sense.tools.signal_finder.infrastructure.CellTowerModel
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SignalFinderToolScreen(fragment: TrailSenseDestinationHost) {
    val controller = remember(fragment) {
        SignalFinderController(fragment)
    }
    val lifecycleOwner = LocalLifecycleOwner.current
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
            controller.onDestroy()
        }
    }

    SignalFinderScreen(
        signals = controller.signals,
        towers = controller.nearbyTowers,
        location = controller.location,
        loading = controller.loading,
        disclaimer = controller.disclaimer,
        formatter = controller.formatter,
        prefs = controller.prefs,
        onNavigateToTower = controller::navigateToTower,
        onCreateBeacon = controller::createBeacon
    )
}

private class SignalFinderController(private val fragment: TrailSenseDestinationHost) {

    private val sensorService by lazy { SensorService(fragment.requireContext()) }
    private val cellSignal by lazy { sensorService.getCellSignal() }
    private val gps by lazy { sensorService.getGPS() }
    val formatter by lazy { FormatService.getInstance(fragment.requireContext()) }
    val prefs by lazy { UserPreferences(fragment.requireContext()) }
    private val markdown by lazy { AppServiceRegistry.get<MarkdownService>() }
    private val navigator by lazy { Navigator.getInstance(fragment.requireContext()) }
    private val handler = Handler(Looper.getMainLooper())

    var signals by mutableStateOf(emptyList<CellSignal>())
        private set
    var nearbyTowers by mutableStateOf(emptyList<ApproximateCoordinate>())
        private set
    var location by mutableStateOf(Coordinate.zero)
        private set
    var loading by mutableStateOf(false)
        private set
    val disclaimer by lazy {
        markdown.toMarkdown(fragment.getString(R.string.cell_tower_disclaimer))
    }
    private var lastTowerLocation: Coordinate? = null
    private var towerJob: Job? = null

    private val sensorListener = {
        refreshReadings()
        true
    }
    private val refreshRunnable = object : Runnable {
        override fun run() {
            refreshReadings()
            handler.postDelayed(this, 1000)
        }
    }

    fun onResume() {
        cellSignal.start(sensorListener)
        gps.start(sensorListener)
        refreshReadings()
        handler.removeCallbacks(refreshRunnable)
        handler.postDelayed(refreshRunnable, 1000)
    }

    fun onPause() {
        handler.removeCallbacks(refreshRunnable)
        cellSignal.stop(sensorListener)
        gps.stop(sensorListener)
        towerJob?.cancel()
    }

    fun onDestroy() {
        handler.removeCallbacks(refreshRunnable)
        cellSignal.stop(sensorListener)
        gps.stop(sensorListener)
        towerJob?.cancel()
    }

    private fun refreshReadings() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            handler.post { refreshReadings() }
            return
        }

        signals = cellSignal.signals
        val currentLocation = gps.location
        location = currentLocation

        val lastLocation = lastTowerLocation
        if (lastLocation == null || lastLocation.distanceTo(currentLocation) >= 100f) {
            loadNearbyTowers(currentLocation)
        }
    }

    private fun loadNearbyTowers(currentLocation: Coordinate) {
        lastTowerLocation = currentLocation
        towerJob?.cancel()
        towerJob = fragment.lifecycleScope.launch {
            loading = true
            val towers = withContext(Dispatchers.IO) {
                CellTowerModel.getTowers(
                    CoordinateBounds.from(Geofence(currentLocation, Distance.kilometers(20f))),
                    5
                ).sortedBy { currentLocation.distanceTo(it.coordinate) }
            }
            nearbyTowers = towers
            loading = false
        }
    }

    fun navigateToTower(tower: ApproximateCoordinate) {
        navigator.navigateTo(tower.coordinate, fragment.getString(R.string.cell_tower))
        fragment.findNavController().openTool(Tools.NAVIGATION)
    }

    fun createBeacon(tower: ApproximateCoordinate) {
        val bundle = Bundle().apply {
            putParcelable("initial_location", GeoUri(tower.coordinate))
        }
        fragment.findNavController().navigate(R.id.placeBeaconFragment, bundle)
    }
}

@Composable
private fun SignalFinderScreen(
    signals: List<CellSignal>,
    towers: List<ApproximateCoordinate>,
    location: Coordinate,
    loading: Boolean,
    disclaimer: CharSequence,
    formatter: FormatService,
    prefs: UserPreferences,
    onNavigateToTower: (ApproximateCoordinate) -> Unit,
    onCreateBeacon: (ApproximateCoordinate) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("title"),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text(
                        text = stringResource(R.string.cell_towers),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (loading) {
                        Text(
                            text = stringResource(R.string.loading),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                if (signals.isEmpty() && towers.isEmpty()) {
                    Text(
                        text = stringResource(R.string.no_cell_towers_found),
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("list")
                ) {
                    items(signals) { signal ->
                        CellSignalRow(signal, formatter, prefs)
                        HorizontalDivider()
                    }
                    items(towers) { tower ->
                        CellTowerRow(
                            tower = tower,
                            location = location,
                            formatter = formatter,
                            prefs = prefs,
                            onNavigate = { onNavigateToTower(tower) },
                            onCreateBeacon = { onCreateBeacon(tower) }
                        )
                        HorizontalDivider()
                    }
                }
            }

            SignalDisclaimer(disclaimer)
        }
    }
}

@Composable
private fun CellSignalRow(
    signal: CellSignal,
    formatter: FormatService,
    prefs: UserPreferences,
    modifier: Modifier = Modifier
) {
    val distance = signal.timingDistanceMeters?.let {
        Distance.meters(it).convertTo(prefs.baseDistanceUnits).toRelativeDistance()
    }
    val maxDistance = distance?.let {
        signal.timingDistanceErrorMeters?.let { error ->
            val errorDistance = Distance.meters(error).convertTo(distance.units)
            Distance.from(distance.value + errorDistance.value, distance.units).toRelativeDistance()
        }
    }
    val description = formatter.join(
        formatter.formatPercentage(signal.strength),
        distance?.let {
            formatter.formatDistance(distance, Units.getDecimalPlaces(distance.units)) +
                    if (maxDistance != null) {
                        " - ${
                            formatter.formatDistance(
                                maxDistance,
                                Units.getDecimalPlaces(maxDistance.units)
                            )
                        }"
                    } else {
                        ""
                    }
        },
        formatter.formatTime(signal.time),
        if (signal.isRegistered) {
            stringResource(R.string.full_service)
        } else {
            stringResource(R.string.emergency_calls_only)
        },
        separator = FormatService.Separator.Dot
    )

    SignalFinderRow(
        title = formatter.formatCellNetwork(signal.network),
        description = description,
        icon = CellSignalUtils.getCellQualityImage(signal.quality),
        tint = Color(CustomUiUtils.getQualityColor(signal.quality)),
        modifier = modifier
    )
}

@Composable
private fun CellTowerRow(
    tower: ApproximateCoordinate,
    location: Coordinate,
    formatter: FormatService,
    prefs: UserPreferences,
    onNavigate: () -> Unit,
    onCreateBeacon: () -> Unit,
    modifier: Modifier = Modifier
) {
    val distance = Distance.meters(location.distanceTo(tower.coordinate))
        .convertTo(prefs.baseDistanceUnits)
        .toRelativeDistance()
    val direction = location.bearingTo(tower.coordinate)
    val description = formatter.join(
        formatter.formatDistance(distance, Units.getDecimalPlaces(distance.units)),
        "${formatter.formatDegrees(direction.value, replace360 = true)} ${
            formatter.formatDirection(direction.direction)
        }",
        separator = FormatService.Separator.Dot
    )

    SignalFinderRow(
        title = stringResource(R.string.cell_tower),
        description = description,
        icon = R.drawable.cell_tower,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        trailing = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onNavigate) {
                    Text(stringResource(R.string.navigate))
                }
                TextButton(onClick = onCreateBeacon) {
                    Text(stringResource(R.string.create_beacon))
                }
            }
        },
        modifier = modifier
    )
}

@Composable
private fun SignalFinderRow(
    title: String,
    description: String,
    icon: Int,
    tint: Color,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier
                    .padding(end = 12.dp)
                    .size(24.dp),
                tint = tint
            )
            Column(modifier = Modifier.weight(1f)) {
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
        if (trailing != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                trailing()
            }
        }
    }
}

@Composable
private fun SignalDisclaimer(
    disclaimer: CharSequence,
    modifier: Modifier = Modifier
) {
    TrailSenseLinkedText(
        text = disclaimer,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
            .testTag("disclaimer"),
        style = MaterialTheme.typography.labelSmall,
        textAlign = TextAlign.Center
    )
}

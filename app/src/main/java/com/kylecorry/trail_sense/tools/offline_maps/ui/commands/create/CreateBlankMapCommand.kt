@file:Suppress("FunctionName")

package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.create

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import androidx.core.net.toUri
import com.kylecorry.andromeda.alerts.CoroutineAlerts
import com.kylecorry.andromeda.alerts.loading.ILoadingIndicator
import com.kylecorry.andromeda.core.math.DecimalFormatter
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.luna.text.toDoubleCompat
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.CompassDirection
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.DistanceUtils
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.andromeda_temp.Result
import com.kylecorry.trail_sense.shared.andromeda_temp.map
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.shared.sensors.LocationSubsystem
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.sensors.SensorSubsystem
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseComposeRootView
import com.kylecorry.trail_sense.tools.beacons.infrastructure.BeaconPickers
import com.kylecorry.trail_sense.tools.beacons.infrastructure.persistence.BeaconService
import com.kylecorry.trail_sense.tools.beacons.infrastructure.sort.ClosestBeaconSort
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.MapCalibrationPoint
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.PercentCoordinate
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.PhotoMap
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.MapService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CreateBlankMapCommand(
    private val context: Context
) : ICreateMapCommand {

    private val files = FileSubsystem.getInstance(context)
    private val location = LocationSubsystem.getInstance(context)
    private val prefs = UserPreferences(context)
    private val formatter = FormatService.getInstance(context)

    override suspend fun execute(): Result<CreateOfflineMapRequest, CreateOfflineMapError> = onIO {
        val calibration = getCalibration() ?: return@onIO Result.Err(CreateOfflineMapError.Cancelled)

        val file = files.createTemp(".webp")
        val bitmap = createBitmap(100, 100)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        files.save(files.getLocalPath(file), bitmap, 10, true)

        CreateMapFromUriCommand(
            context,
            file.toUri()
        ).execute().map {
            it.copy(photoMapCalibration = calibration, visible = false)
        }
    }

    private suspend fun getCalibration(): List<MapCalibrationPoint>? {
        val allUnits = formatter.sortDistanceUnits(DistanceUtils.hikingDistanceUnits)

        val defaultDistance =
            Distance.kilometers(2f).convertTo(prefs.baseDistanceUnits).toRelativeDistance()
        var distance = defaultDistance
        var center = location.location

        val cancelled = onMain {
            val view = createBlankMapCalibrationView(
                center = center,
                distance = distance,
                units = allUnits,
                onCenterChange = {
                    center = it ?: location.location
                },
                onDistanceChange = {
                    distance = it ?: defaultDistance
                }
            )

            CoroutineAlerts.dialog(
                context,
                context.getString(R.string.create_blank_map),
                contentView = view
            )
        }

        if (cancelled) {
            return null
        }

        val topLeft = center.plus(distance, Bearing.from(CompassDirection.NorthWest))
        return listOf(
            MapCalibrationPoint(center, PercentCoordinate(0.5f, 0.5f)),
            MapCalibrationPoint(topLeft, PercentCoordinate(0f, 0f)),
        )
    }

    private fun createBlankMapCalibrationView(
        center: Coordinate?,
        distance: Distance?,
        units: List<DistanceUnits>,
        onCenterChange: (Coordinate?) -> Unit,
        onDistanceChange: (Distance?) -> Unit
    ): TrailSenseComposeRootView {
        return TrailSenseComposeRootView(
            context,
            ViewCompositionStrategy.DisposeOnDetachedFromWindow
        ) {
            BlankMapCalibrationContent(
                center = center,
                distance = distance,
                units = units,
                onCenterChange = onCenterChange,
                onDistanceChange = onDistanceChange
            )
        }
    }

}

@Composable
private fun BlankMapCalibrationContent(
    center: Coordinate?,
    distance: Distance?,
    units: List<DistanceUnits>,
    onCenterChange: (Coordinate?) -> Unit,
    onDistanceChange: (Distance?) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        BlankMapLabel(text = stringResource(R.string.center))
        Spacer(modifier = Modifier.height(4.dp))
        BlankMapCoordinateInput(
            coordinate = center,
            onCoordinateChange = onCenterChange
        )
        Spacer(modifier = Modifier.height(8.dp))
        BlankMapLabel(text = stringResource(R.string.size))
        Spacer(modifier = Modifier.height(4.dp))
        BlankMapDistanceInput(
            distance = distance,
            units = units,
            onDistanceChange = onDistanceChange
        )
    }
}

@Composable
private fun BlankMapLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
@Suppress("LongMethod")
private fun BlankMapCoordinateInput(
    coordinate: Coordinate?,
    onCoordinateChange: (Coordinate?) -> Unit
) {
    val context = LocalContext.current
    val formatter = remember { FormatService.getInstance(context) }
    val sensors = remember { SensorSubsystem.getInstance(context) }
    val gps = remember { SensorService(context).getGPS() }
    val scope = rememberCoroutineScope()
    var coordinateText by remember(coordinate, formatter) {
        mutableStateOf(coordinate?.let { formatter.formatLocation(it) }.orEmpty())
    }
    val parsedCoordinate = remember(coordinateText) {
        Coordinate.parse(coordinateText)
    }
    var isLoadingLocation by remember { mutableStateOf(false) }
    var showHelp by remember { mutableStateOf(false) }

    fun autofillFromGps() {
        scope.launch {
            isLoadingLocation = true
            val updated = withContext(Dispatchers.IO) {
                sensors.getLocation(SensorSubsystem.SensorRefreshPolicy.Refresh)
            }
            coordinateText = formatter.formatLocation(updated)
            onCoordinateChange(updated)
            isLoadingLocation = false
        }
    }

    LaunchedEffect(coordinate) {
        val formatted = coordinate?.let { formatter.formatLocation(it) }.orEmpty()
        if (coordinateText != formatted) {
            coordinateText = formatted
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        if (isLoadingLocation) {
            CircularProgressIndicator(modifier = Modifier.size(40.dp))
        } else {
            IconButton(onClick = {
                Pickers.item(
                    context,
                    context.getString(R.string.autofill_source),
                    listOf(
                        context.getString(R.string.gps),
                        context.getString(R.string.beacon)
                    ),
                    0
                ) { selection ->
                    when (selection) {
                        0 -> autofillFromGps()
                        1 -> scope.launch {
                            val beacon = BeaconPickers.pickBeacon(
                                context,
                                sort = ClosestBeaconSort(BeaconService(context), gps::location)
                            ) ?: return@launch
                            coordinateText = formatter.formatLocation(beacon.coordinate)
                            onCoordinateChange(beacon.coordinate)
                        }
                    }
                }
            }) {
                Icon(
                    painter = painterResource(R.drawable.ic_location),
                    contentDescription = stringResource(R.string.gps)
                )
            }
        }
        Spacer(modifier = Modifier.size(8.dp))
        OutlinedTextField(
            value = coordinateText,
            onValueChange = {
                coordinateText = it
                onCoordinateChange(Coordinate.parse(it))
            },
            modifier = Modifier.weight(1f),
            label = { Text(stringResource(R.string.location)) },
            isError = coordinateText.isNotBlank() && parsedCoordinate == null,
            supportingText = {
                if (coordinateText.isNotBlank() && parsedCoordinate == null) {
                    Text(stringResource(R.string.coordinate_input_invalid_location))
                }
            },
            singleLine = true,
            enabled = !isLoadingLocation
        )
        Spacer(modifier = Modifier.size(8.dp))
        IconButton(onClick = { showHelp = true }) {
            Icon(
                painter = painterResource(R.drawable.ic_help),
                contentDescription = stringResource(R.string.location_input_help_title)
            )
        }
    }

    if (showHelp) {
        AlertDialog(
            onDismissRequest = { showHelp = false },
            confirmButton = {
                TextButton(onClick = { showHelp = false }) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            title = { Text(stringResource(R.string.location_input_help_title)) },
            text = { Text(stringResource(R.string.location_input_help)) }
        )
    }
}

@Composable
@Suppress("LongMethod")
private fun BlankMapDistanceInput(
    distance: Distance?,
    units: List<DistanceUnits>,
    onDistanceChange: (Distance?) -> Unit
) {
    val context = LocalContext.current
    val formatter = remember { FormatService.getInstance(context) }
    var unit by remember(units) {
        mutableStateOf(distance?.units ?: units.firstOrNull() ?: DistanceUnits.Meters)
    }
    var distanceText by remember {
        mutableStateOf(formatDistanceAmount(distance, unit))
    }
    var isUnitOpen by remember { mutableStateOf(false) }

    fun publish(text: String = distanceText, newUnit: DistanceUnits = unit) {
        onDistanceChange(parseDistance(text, newUnit))
    }

    LaunchedEffect(distance) {
        val current = parseDistance(distanceText, unit)
        if (current != distance) {
            val newUnit = distance?.units ?: unit
            unit = newUnit
            distanceText = formatDistanceAmount(distance, newUnit)
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        OutlinedTextField(
            value = distanceText,
            onValueChange = {
                distanceText = it
                publish(text = it)
            },
            modifier = Modifier.weight(1f),
            label = { Text(stringResource(R.string.distance)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true
        )
        Spacer(modifier = Modifier.size(8.dp))
        Box(modifier = Modifier.weight(1f)) {
            OutlinedButton(
                onClick = { isUnitOpen = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(formatter.getDistanceUnitName(unit))
            }
            DropdownMenu(
                expanded = isUnitOpen,
                onDismissRequest = { isUnitOpen = false }
            ) {
                units.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(formatter.getDistanceUnitName(option)) },
                        onClick = {
                            isUnitOpen = false
                            val converted = parseDistance(distanceText, unit)?.convertTo(option)
                            unit = option
                            distanceText = formatDistanceAmount(converted, option)
                            publish(text = distanceText, newUnit = option)
                        }
                    )
                }
            }
        }
    }
}

private fun parseDistance(text: String, unit: DistanceUnits): Distance? {
    return text.toDoubleCompat()?.let {
        Distance.from(it.toFloat(), unit)
    }
}

private fun formatDistanceAmount(distance: Distance?, unit: DistanceUnits): String {
    val converted = distance?.convertTo(unit) ?: return ""
    return DecimalFormatter.format(
        converted.value,
        Units.getDecimalPlaces(converted.units),
        strict = false
    )
}

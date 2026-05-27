package com.kylecorry.trail_sense.tools.ruler.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.kylecorry.sol.science.geography.Geography
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.extensions.TrailSenseComposeFragment
import com.kylecorry.trail_sense.shared.extensions.compose.useService
import com.kylecorry.trail_sense.shared.extensions.compose.useState
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseCard
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseChip
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSectionHeader
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing

enum class MapScaleMode {
    Fractional, Relational
}

class RulerFragment : TrailSenseComposeFragment() {

    @Composable
    override fun FragmentContent() {
        val prefs = useService<UserPreferences>()
        val formatService = useService<FormatService>()

        val (currentDistance, setCurrentDistance) = useState(
            Distance.from(0f, DistanceUnits.Centimeters)
        )
        val (rulerUnits, setRulerUnits) = useState(
            if (prefs.distanceUnits == UserPreferences.DistanceUnits.Meters) DistanceUnits.Centimeters
            else DistanceUnits.Inches
        )
        val (scaleMode, setScaleMode) = useState(MapScaleMode.Fractional)
        val (fractionalDenominator, setFractionalDenominator) = useState("25000")
        val (verbalFromValue, setVerbalFromValue) = useState("1")
        val (verbalFromUnit, setVerbalFromUnit) = useState(DistanceUnits.Centimeters)
        val (verbalToValue, setVerbalToValue) = useState("1")
        val (verbalToUnit, setVerbalToUnit) = useState(DistanceUnits.Kilometers)

        RulerContent(
            currentDistance = currentDistance,
            rulerUnits = rulerUnits,
            scaleMode = scaleMode,
            fractionalDenominator = fractionalDenominator,
            verbalFromValue = verbalFromValue,
            verbalFromUnit = verbalFromUnit,
            verbalToValue = verbalToValue,
            verbalToUnit = verbalToUnit,
            formatService = formatService,
            onRulerDistanceChange = { distance ->
                setCurrentDistance(distance.convertTo(DistanceUnits.Centimeters))
            },
            onUnitToggle = {
                setRulerUnits(
                    if (rulerUnits == DistanceUnits.Centimeters) DistanceUnits.Inches else DistanceUnits.Centimeters
                )
            },
            onScaleModeChange = setScaleMode,
            onFractionalDenominatorChange = setFractionalDenominator,
            onVerbalChange = { fromVal, fromUnit, toVal, toUnit ->
                setVerbalFromValue(fromVal)
                setVerbalFromUnit(fromUnit)
                setVerbalToValue(toVal)
                setVerbalToUnit(toUnit)
            }
        )
    }

    companion object {
        const val precision = 4
        const val mapPrecision = 2
    }
}

@Composable
private fun RulerContent(
    currentDistance: Distance,
    rulerUnits: DistanceUnits,
    scaleMode: MapScaleMode,
    fractionalDenominator: String,
    verbalFromValue: String,
    verbalFromUnit: DistanceUnits,
    verbalToValue: String,
    verbalToUnit: DistanceUnits,
    formatService: FormatService,
    onRulerDistanceChange: (Distance) -> Unit,
    onUnitToggle: () -> Unit,
    onScaleModeChange: (MapScaleMode) -> Unit,
    onFractionalDenominatorChange: (String) -> Unit,
    onVerbalChange: (String, DistanceUnits, String, DistanceUnits) -> Unit,
    modifier: Modifier = Modifier
) {
    val displayDistance = currentDistance.convertTo(rulerUnits)

    val mapDistanceText = rememberMapDistanceText(
        currentDistance = currentDistance,
        scaleMode = scaleMode,
        fractionalDenominator = fractionalDenominator,
        verbalFromValue = verbalFromValue,
        verbalFromUnit = verbalFromUnit,
        verbalToValue = verbalToValue,
        verbalToUnit = verbalToUnit,
        rulerUnits = rulerUnits,
        formatService = formatService
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(TrailSenseSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.lg)
    ) {
        TrailSenseCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(TrailSenseSpacing.md)
            ) {
                Text(
                    text = formatService.formatDistance(displayDistance, RulerFragment.precision, false),
                    style = MaterialTheme.typography.displayMedium
                )
                if (mapDistanceText != null) {
                    Text(
                        text = "Map distance: $mapDistanceText",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Text(
                        text = "Enter map scale to calculate real-world distance",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        AndroidView(
            factory = { ctx ->
                RulerView(ctx).apply {
                    metric = rulerUnits.isMetric
                    setOnTouchListener { distance -> onRulerDistanceChange(distance) }
                }
            },
            update = { view -> view.metric = rulerUnits.isMetric },
            modifier = Modifier.fillMaxWidth().height(140.dp)
        )

        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            TrailSenseChip(
                text = if (rulerUnits == DistanceUnits.Centimeters) "cm" else "in",
                selected = true,
                onClick = onUnitToggle
            )
        }

        TrailSenseSectionHeader("Map Scale")

        Row(
            horizontalArrangement = Arrangement.spacedBy(TrailSenseSpacing.sm),
            modifier = Modifier.fillMaxWidth()
        ) {
            TrailSenseChip(
                text = "1 : X (Fractional)",
                selected = scaleMode == MapScaleMode.Fractional,
                onClick = { onScaleModeChange(MapScaleMode.Fractional) },
                modifier = Modifier.weight(1f)
            )
            TrailSenseChip(
                text = "Verbal (1 cm = 1 km)",
                selected = scaleMode == MapScaleMode.Relational,
                onClick = { onScaleModeChange(MapScaleMode.Relational) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(TrailSenseSpacing.sm))

        when (scaleMode) {
            MapScaleMode.Fractional -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(TrailSenseSpacing.md),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("1 :", style = MaterialTheme.typography.titleLarge)
                    OutlinedTextField(
                        value = fractionalDenominator,
                        onValueChange = onFractionalDenominatorChange,
                        label = { Text("Denominator") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Text(
                    text = "Common: 1:24,000  •  1:25,000  •  1:50,000  •  1:100,000",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            MapScaleMode.Relational -> {
                Column(
                    verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.sm),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(TrailSenseSpacing.sm)
                    ) {
                        OutlinedTextField(
                            value = verbalFromValue,
                            onValueChange = { newVal ->
                                onVerbalChange(newVal, verbalFromUnit, verbalToValue, verbalToUnit)
                            },
                            label = { Text("Map distance") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        TrailSenseChip(
                            text = if (verbalFromUnit == DistanceUnits.Centimeters) "cm" else "in",
                            selected = true,
                            onClick = {
                                val newUnit = if (verbalFromUnit == DistanceUnits.Centimeters) DistanceUnits.Inches else DistanceUnits.Centimeters
                                onVerbalChange(verbalFromValue, newUnit, verbalToValue, verbalToUnit)
                            }
                        )
                    }

                    Text("=", style = MaterialTheme.typography.titleLarge, modifier = Modifier.align(Alignment.CenterHorizontally))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(TrailSenseSpacing.sm)
                    ) {
                        OutlinedTextField(
                            value = verbalToValue,
                            onValueChange = { newVal ->
                                onVerbalChange(verbalFromValue, verbalFromUnit, newVal, verbalToUnit)
                            },
                            label = { Text("Real world distance") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        TrailSenseChip(
                            text = when (verbalToUnit) {
                                DistanceUnits.Kilometers -> "km"
                                DistanceUnits.Miles -> "mi"
                                DistanceUnits.NauticalMiles -> "nm"
                                DistanceUnits.Meters -> "m"
                                else -> "km"
                            },
                            selected = true,
                            onClick = {
                                val next = when (verbalToUnit) {
                                    DistanceUnits.Kilometers -> DistanceUnits.Miles
                                    DistanceUnits.Miles -> DistanceUnits.NauticalMiles
                                    DistanceUnits.NauticalMiles -> DistanceUnits.Meters
                                    else -> DistanceUnits.Kilometers
                                }
                                onVerbalChange(verbalFromValue, verbalFromUnit, verbalToValue, next)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun rememberMapDistanceText(
    currentDistance: Distance,
    scaleMode: MapScaleMode,
    fractionalDenominator: String,
    verbalFromValue: String,
    verbalFromUnit: DistanceUnits,
    verbalToValue: String,
    verbalToUnit: DistanceUnits,
    rulerUnits: DistanceUnits,
    formatService: FormatService
): String? {
    return androidx.compose.runtime.remember(
        currentDistance, scaleMode, fractionalDenominator,
        verbalFromValue, verbalFromUnit, verbalToValue, verbalToUnit, rulerUnits
    ) {
        when (scaleMode) {
            MapScaleMode.Fractional -> {
                val from = 1f
                val to = fractionalDenominator.toFloatOrNull() ?: 0f
                if (to > 0f) {
                    val result = Geography.getMapDistance(currentDistance, from, to)
                    formatService.formatDistance(result.convertTo(rulerUnits), RulerFragment.mapPrecision, false)
                } else null
            }
            MapScaleMode.Relational -> {
                val fromVal = verbalFromValue.toFloatOrNull() ?: 0f
                val toVal = verbalToValue.toFloatOrNull() ?: 0f
                if (fromVal > 0f && toVal > 0f) {
                    val fromDist = Distance.from(fromVal, verbalFromUnit)
                    val toDist = Distance.from(toVal, verbalToUnit)
                    val result = Geography.getMapDistance(currentDistance, fromDist, toDist)
                    formatService.formatDistance(result.convertTo(toDist.units), RulerFragment.mapPrecision, false)
                } else null
            }
        }
    }
}

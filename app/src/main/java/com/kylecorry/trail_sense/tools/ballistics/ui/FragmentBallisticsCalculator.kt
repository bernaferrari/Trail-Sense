package com.kylecorry.trail_sense.tools.ballistics.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.core.math.DecimalFormatter
import com.kylecorry.andromeda.core.tryOrDefault
import com.kylecorry.luna.text.toFloatCompat
import com.kylecorry.sol.math.arithmetic.Arithmetic
import com.kylecorry.sol.science.physics.NoDragModel
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.sol.units.Speed
import com.kylecorry.sol.units.TimeUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.DistanceUtils
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseCard
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.tools.ballistics.domain.BallisticsCalculator
import com.kylecorry.trail_sense.tools.ballistics.domain.G1DragModel
import com.kylecorry.trail_sense.tools.ballistics.domain.TrajectoryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Duration

@Composable
fun BallisticsCalculatorPage(
    modifier: Modifier = Modifier
) {
    val formatter = remember { AppServiceRegistry.get<FormatService>() }
    val prefs = remember { AppServiceRegistry.get<UserPreferences>() }
    val normalScopeHeight = remember { Distance.from(1.5f, DistanceUnits.Inches) }
    val (zeroDistance, setZeroDistance) = rememberDistancePreference("cache-ballistics-sight-in-range")
    val (storedScopeHeight, setScopeHeight) = rememberDistancePreference("cache-ballistics-scope-height")
    val scopeHeight = storedScopeHeight ?: normalScopeHeight
    val (bulletSpeed, setBulletSpeed) = rememberSpeedPreference("cache-ballistics-bullet-speed")
    val (ballisticCoefficient, setBallisticCoefficient) = rememberFloatPreference("cache-ballistics-ballistic-coefficient")
    var ballisticCoefficientText by remember {
        mutableStateOf(ballisticCoefficient?.let { DecimalFormatter.format(it, 5, false) } ?: "")
    }
    var trajectory by remember { mutableStateOf(emptyList<TrajectoryPoint>()) }
    var loading by remember { mutableStateOf(false) }

    LaunchedEffect(zeroDistance, scopeHeight, bulletSpeed, ballisticCoefficient) {
        loading = true
        trajectory = withContext(Dispatchers.Default) {
            if (bulletSpeed != null && bulletSpeed.value > 0 && zeroDistance != null) {
                calculateTrajectory(zeroDistance, scopeHeight, bulletSpeed, ballisticCoefficient)
            } else {
                emptyList()
            }
        }
        loading = false
    }

    BallisticsForm(modifier = modifier) {
        // Inputs group
        TrailSenseCard {
            Column(
                verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.md)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TonalIconChip(Icons.Default.Tune)
                    Spacer(modifier = Modifier.width(TrailSenseSpacing.md))
                    Text(
                        text = stringResource(R.string.ballistics),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                DistanceField(
                    label = stringResource(R.string.sight_in_range),
                    value = zeroDistance,
                    units = formatter.sortDistanceUnits(DistanceUtils.hikingDistanceUnits),
                    defaultUnit = DistanceUnits.Yards,
                    onValueChange = { setZeroDistance(it ?: Distance.from(0f, DistanceUnits.Yards)) },
                    modifier = Modifier.testTag("zero_distance")
                )
                DistanceField(
                    label = stringResource(R.string.scope_height),
                    value = scopeHeight,
                    units = formatter.sortDistanceUnits(DistanceUtils.rulerDistanceUnits),
                    defaultUnit = DistanceUnits.Inches,
                    onValueChange = { setScopeHeight(it ?: Distance.from(0f, DistanceUnits.Inches)) },
                    modifier = Modifier.testTag("scope_height")
                )
                SpeedField(
                    label = stringResource(R.string.muzzle_velocity),
                    value = bulletSpeed,
                    units = formatter.sortDistanceUnits(listOf(DistanceUnits.Feet, DistanceUnits.Meters)),
                    defaultUnit = DistanceUnits.Feet,
                    onValueChange = setBulletSpeed,
                    modifier = Modifier.testTag("bullet_speed")
                )
                OutlinedTextField(
                    value = ballisticCoefficientText,
                    onValueChange = {
                        ballisticCoefficientText = it
                        setBallisticCoefficient(it.toFloatCompat())
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ballistic_coefficient"),
                    label = { Text(stringResource(R.string.ballistic_coefficient)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
            }
        }

        // Result group (calculated trajectory)
        TrailSenseCard {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ballistics_table"),
                contentAlignment = Alignment.TopCenter
            ) {
                when {
                    loading -> CircularProgressIndicator(
                        modifier = Modifier
                            .padding(vertical = TrailSenseSpacing.xl)
                            .testTag("loading")
                    )

                    trajectory.isEmpty() -> Text(
                        text = stringResource(R.string.no_data),
                        modifier = Modifier.padding(vertical = TrailSenseSpacing.xl),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    else -> BallisticsTable(
                        trajectory = trajectory,
                        zeroDistance = zeroDistance,
                        bulletSpeed = bulletSpeed,
                        formatter = formatter,
                        prefs = prefs
                    )
                }
            }
        }
    }
}

@Composable
private fun TonalIconChip(icon: ImageVector) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.size(40.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun BallisticsTable(
    trajectory: List<TrajectoryPoint>,
    zeroDistance: Distance?,
    bulletSpeed: Speed?,
    formatter: FormatService,
    prefs: UserPreferences
) {
    val smallUnits = if (prefs.distanceUnits == UserPreferences.DistanceUnits.Feet) {
        DistanceUnits.Inches
    } else {
        DistanceUnits.Centimeters
    }
    val headers = listOf(
        stringResource(
            R.string.time_header_units,
            formatter.formatDuration(
                Duration.ZERO,
                short = true,
                includeSeconds = true
            ).replace("0", "").trim()
        ),
        stringResource(
            R.string.range_header_units,
            formatter.getDistanceUnitName(zeroDistance?.units ?: DistanceUnits.Feet, true)
        ),
        stringResource(R.string.velocity_header_units, "fps"),
        stringResource(
            R.string.drop_header_units,
            formatter.getDistanceUnitName(smallUnits, true)
        )
    )

    LazyColumn(
        modifier = Modifier.fillMaxWidth()
    ) {
        item {
            BallisticsTableRow(
                values = headers,
                header = true
            )
        }
        items(trajectory) { point ->
            val values = listOf(
                DecimalFormatter.format(point.time, 2),
                DecimalFormatter.format(
                    point.distance.convertTo(zeroDistance?.units ?: DistanceUnits.Feet).value,
                    Units.getDecimalPlaces(zeroDistance?.units ?: DistanceUnits.Feet)
                ),
                DecimalFormatter.format(
                    point.velocity.convertTo(
                        bulletSpeed?.distanceUnits ?: DistanceUnits.Feet,
                        bulletSpeed?.timeUnits ?: TimeUnits.Seconds
                    ).value,
                    0
                ),
                DecimalFormatter.format(point.drop.convertTo(smallUnits).value, 1)
            )
            BallisticsTableRow(values = values)
        }
    }
}

@Composable
private fun BallisticsTableRow(
    values: List<String>,
    header: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (header) {
                    MaterialTheme.colorScheme.surfaceVariant
                } else {
                    MaterialTheme.colorScheme.surface
                }
            )
    ) {
        values.forEach { value ->
            Text(
                text = value,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (header) FontWeight.SemiBold else FontWeight.Normal,
                color = if (header) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}

private fun calculateTrajectory(
    zeroDistance: Distance,
    scopeHeight: Distance,
    bulletSpeed: Speed,
    ballisticCoefficient: Float?
): List<TrajectoryPoint> {
    return tryOrDefault(emptyList()) {
        val dragModel = if (ballisticCoefficient == null || Arithmetic.isZero(ballisticCoefficient)) {
            NoDragModel()
        } else {
            G1DragModel(ballisticCoefficient)
        }

        BallisticsCalculator().calculateTrajectory(zeroDistance, scopeHeight, bulletSpeed, dragModel)
    }
}

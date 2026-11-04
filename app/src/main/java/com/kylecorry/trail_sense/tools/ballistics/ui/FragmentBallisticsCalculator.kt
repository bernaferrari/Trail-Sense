package com.kylecorry.trail_sense.tools.ballistics.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.height
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.kylecorry.andromeda.core.cache.DependencyRegistry
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
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupCard
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupPosition
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseInputField
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseRowGroup
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.tools.ballistics.domain.BallisticsCalculator
import com.kylecorry.trail_sense.tools.ballistics.domain.G1DragModel
import com.kylecorry.trail_sense.tools.ballistics.domain.TrajectoryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Duration

/** Trajectory: the rifle as one group of inputs, then the table it produces as one card. */
@Composable
fun BallisticsCalculatorPage(
    modifier: Modifier = Modifier
) {
    val formatter = remember { DependencyRegistry.get<FormatService>() }
    val prefs = remember { DependencyRegistry.get<UserPreferences>() }
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
    // Starts loading so the empty hint never flashes before the first calculation.
    var loading by remember { mutableStateOf(true) }

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
        TrailSenseRowGroup {
            row { position ->
                DistanceField(
                    label = stringResource(R.string.sight_in_range),
                    value = zeroDistance,
                    units = formatter.sortDistanceUnits(DistanceUtils.hikingDistanceUnits),
                    defaultUnit = DistanceUnits.Yards,
                    onValueChange = { setZeroDistance(it ?: Distance.from(0f, DistanceUnits.Yards)) },
                    position = position,
                    modifier = Modifier.testTag("zero_distance")
                )
            }
            row { position ->
                DistanceField(
                    label = stringResource(R.string.scope_height),
                    value = scopeHeight,
                    units = formatter.sortDistanceUnits(DistanceUtils.rulerDistanceUnits),
                    defaultUnit = DistanceUnits.Inches,
                    onValueChange = { setScopeHeight(it ?: Distance.from(0f, DistanceUnits.Inches)) },
                    position = position,
                    modifier = Modifier.testTag("scope_height")
                )
            }
            row { position ->
                SpeedField(
                    label = stringResource(R.string.muzzle_velocity),
                    value = bulletSpeed,
                    units = formatter.sortDistanceUnits(listOf(DistanceUnits.Feet, DistanceUnits.Meters)),
                    defaultUnit = DistanceUnits.Feet,
                    onValueChange = setBulletSpeed,
                    position = position,
                    modifier = Modifier.testTag("bullet_speed")
                )
            }
            row { position ->
                TrailSenseInputField(
                    value = ballisticCoefficientText,
                    onValueChange = {
                        ballisticCoefficientText = it
                        setBallisticCoefficient(it.toFloatCompat())
                    },
                    label = stringResource(R.string.ballistic_coefficient),
                    position = position,
                    modifier = Modifier.testTag("ballistic_coefficient")
                )
            }
        }

        TrailSenseRowGroup(title = stringResource(R.string.ballistics_trajectory)) {
            row { position ->
                TrajectoryCard(
                    position = position,
                    loading = loading,
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

@Composable
private fun TrajectoryCard(
    position: TrailSenseGroupPosition,
    loading: Boolean,
    trajectory: List<TrajectoryPoint>,
    zeroDistance: Distance?,
    bulletSpeed: Speed?,
    formatter: FormatService,
    prefs: UserPreferences
) {
    TrailSenseGroupCard(
        position = position,
        contentPadding = PaddingValues(vertical = TrailSenseSpacing.sm),
        modifier = Modifier.animateContentSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("ballistics_table"),
            contentAlignment = Alignment.TopCenter
        ) {
            when {
                // Keep the last table on screen while a new one is worked out.
                loading && trajectory.isEmpty() -> CircularProgressIndicator(
                    modifier = Modifier
                        .padding(vertical = TrailSenseSpacing.xl)
                        .testTag("loading")
                )

                trajectory.isEmpty() -> Text(
                    text = stringResource(R.string.ballistics_trajectory_empty),
                    modifier = Modifier.padding(
                        horizontal = TrailSenseSpacing.xl,
                        vertical = TrailSenseSpacing.xl
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                else -> Column {
                    TrajectoryChart(
                        trajectory = trajectory,
                        zeroDistance = zeroDistance,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .padding(horizontal = TrailSenseSpacing.lg, vertical = TrailSenseSpacing.sm)
                    )
                    BallisticsTable(
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

/**
 * The flight of the bullet: line of sight dashed, the path rising through it and dropping away as a thick primary
 * curve over a soft fill, with a dot where it crosses back at the zero distance.
 */
@Composable
private fun TrajectoryChart(
    trajectory: List<TrajectoryPoint>,
    zeroDistance: Distance?,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val line = scheme.primary
    val sight = scheme.onSurfaceVariant.copy(alpha = 0.5f)
    val zeroColor = scheme.tertiary
    Canvas(modifier = modifier) {
        if (trajectory.size < 2) return@Canvas
        val maxX = trajectory.maxOf { it.distance.meters().value }.coerceAtLeast(1f)
        val drops = trajectory.map { it.drop.meters().value }
        val top = drops.max().coerceAtLeast(0f)
        val bottom = drops.min().coerceAtMost(0f)
        val range = (top - bottom).takeIf { it > 0f } ?: 1f
        val pad = 10.dp.toPx()
        fun px(m: Float) = pad + (m / maxX) * (size.width - pad * 2)
        fun py(m: Float) = pad + (top - m) / range * (size.height - pad * 2)

        drawLine(
            sight,
            Offset(0f, py(0f)),
            Offset(size.width, py(0f)),
            2.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 6.dp.toPx()))
        )
        val curve = Path().apply {
            trajectory.forEachIndexed { i, p ->
                val x = px(p.distance.meters().value)
                val y = py(p.drop.meters().value)
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
        }
        val area = Path().apply {
            addPath(curve)
            lineTo(px(trajectory.last().distance.meters().value), size.height)
            lineTo(px(trajectory.first().distance.meters().value), size.height)
            close()
        }
        drawPath(area, Brush.verticalGradient(listOf(line.copy(alpha = 0.28f), line.copy(alpha = 0f))))
        drawPath(curve, line, style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        zeroDistance?.let {
            val zx = px(it.meters().value)
            if (zx in 0f..size.width) {
                drawCircle(zeroColor.copy(alpha = 0.3f), 11.dp.toPx(), Offset(zx, py(0f)))
                drawCircle(zeroColor, 6.dp.toPx(), Offset(zx, py(0f)))
            }
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

    // A plain column: the page already scrolls, and a trajectory is a few dozen rows at most.
    Column(modifier = Modifier.fillMaxWidth()) {
        BallisticsTableRow(values = headers, header = true)
        trajectory.forEachIndexed { index, point ->
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
            BallisticsTableRow(values = values, striped = index % 2 == 0)
        }
    }
}

@Composable
private fun BallisticsTableRow(
    values: List<String>,
    header: Boolean = false,
    striped: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = TrailSenseSpacing.sm)
            .background(
                if (striped) MaterialTheme.colorScheme.surfaceContainerHigh else androidx.compose.ui.graphics.Color.Transparent,
                MaterialTheme.shapes.medium
            ),
        horizontalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xs)
    ) {
        values.forEach { value ->
            Text(
                text = value,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = TrailSenseSpacing.sm, vertical = TrailSenseSpacing.sm),
                style = if (header) {
                    MaterialTheme.typography.labelMedium
                } else {
                    MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum")
                },
                fontWeight = if (header) androidx.compose.ui.text.font.FontWeight.SemiBold else null,
                color = if (header) {
                    MaterialTheme.colorScheme.primary
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

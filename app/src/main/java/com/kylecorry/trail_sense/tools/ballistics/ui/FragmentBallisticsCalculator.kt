package com.kylecorry.trail_sense.tools.ballistics.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import com.kylecorry.trail_sense.shared.extensions.TrailSenseComposeFragment
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseCard

@Composable
fun Fragment.BallisticsCalculatorContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        TrailSenseCard {
            Text(
                "Ballistics Calculator (Pure Compose placeholder)",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

class FragmentBallisticsCalculator : TrailSenseComposeFragment() {

    @Composable
    override fun FragmentContent() {
        BallisticsCalculatorContent()
    }
}
        }
        val zeroDistanceView = useView<DistanceInputView>(R.id.zero_distance)
        val scopeHeightView = useView<DistanceInputView>(R.id.scope_height)
        val bulletSpeedView = useView<BulletSpeedInputView>(R.id.bullet_speed)
        val loadingView = useView<LoadingIndicator>(R.id.loading)
        // TODO: Picker for different bullet types with option for Custom BC
        val ballisticCoefficientView = useView<TextInputEditText>(R.id.ballistic_coefficient)

        val formatter = useService<FormatService>()
        val prefs = useService<UserPreferences>()
        val queue = useCoroutineQueue()

        // TODO: Determine normal value for metric
        val normalScopeHeight = useMemo {
            Distance.from(1.5f, DistanceUnits.Inches)
        }

        val (zeroDistance, setZeroDistance) = useDistancePreference("cache-ballistics-sight-in-range")
        val (scopeHeight, setScopeHeight) = useDistancePreference("cache-ballistics-scope-height")
        val (bulletSpeed, setBulletSpeed) = useSpeedPreference("cache-ballistics-bullet-speed")
        val (ballisticCoefficient, setBallisticCoefficient) = useFloatPreference("cache-ballistics-ballistic-coefficient")
        val (trajectory, setTrajectory) = useState(emptyList<TrajectoryPoint>())
        val (loading, setLoading) = useState(false)

        useEffect(scopeHeightView, scopeHeight, normalScopeHeight) {
            if (scopeHeight == null) {
                setScopeHeight(normalScopeHeight)
                scopeHeightView.value = normalScopeHeight
            }
        }

        // Intentionally not re-running when the values change
        useEffect(
            zeroDistanceView,
            scopeHeightView,
            bulletSpeedView,
            ballisticCoefficientView,
            ballisticsGrid
        ) {
            zeroDistanceView.units =
                formatter.sortDistanceUnits(DistanceUtils.hikingDistanceUnits)
            zeroDistanceView.hint = getString(R.string.sight_in_range)
            if (zeroDistance != null) {
                zeroDistanceView.value = zeroDistance
            }
            zeroDistanceView.setOnValueChangeListener {
                setZeroDistance(it ?: Distance.from(0f, DistanceUnits.Yards))
            }

            scopeHeightView.units = formatter.sortDistanceUnits(DistanceUtils.rulerDistanceUnits)
            scopeHeightView.hint = getString(R.string.scope_height)
            if (scopeHeight != null) {
                scopeHeightView.value = scopeHeight
            }
            scopeHeightView.setOnValueChangeListener {
                setScopeHeight(it ?: Distance.from(0f, DistanceUnits.Inches))
            }

            bulletSpeedView.units =
                formatter.sortDistanceUnits(listOf(DistanceUnits.Feet, DistanceUnits.Meters))
            bulletSpeedView.hint = getString(R.string.muzzle_velocity)

            if (bulletSpeed != null) {
                bulletSpeedView.value = bulletSpeed
            }

            bulletSpeedView.setOnValueChangeListener {
                setBulletSpeed(it)
            }

            if (ballisticCoefficient != null) {
                ballisticCoefficientView.setText(
                    DecimalFormatter.format(
                        ballisticCoefficient,
                        5,
                        false
                    )
                )
            }
            ballisticCoefficientView.addTextChangedListener {
                setBallisticCoefficient(it?.toString()?.toFloatCompat())
            }

            ballisticsGrid.addLineSeparator()
        }

        useBackgroundEffect(zeroDistance, scopeHeight, bulletSpeed, ballisticCoefficient) {
            queue.replace {
                setLoading(true)
                setTrajectory(
                    if (bulletSpeed != null && bulletSpeed.value > 0 && zeroDistance != null && scopeHeight != null) {
                        calculateTrajectory(
                            zeroDistance,
                            scopeHeight,
                            bulletSpeed,
                            ballisticCoefficient
                        )
                    } else {
                        emptyList()
                    }
                )
                setLoading(false)
            }
        }

        useEffect(ballisticsTableView, trajectory, zeroDistance, bulletSpeed) {
            val smallUnits = if (prefs.distanceUnits == UserPreferences.DistanceUnits.Feet) {
                DistanceUnits.Inches
            } else {
                DistanceUnits.Centimeters
            }
            // TODO: Set units in header
            val listItems = listOf(
                getString(
                    R.string.time_header_units, formatter.formatDuration(
                        Duration.ZERO, short = true,
                        includeSeconds = true
                    ).replace("0", "").trim()
                ),
                getString(
                    R.string.range_header_units,
                    formatter.getDistanceUnitName(zeroDistance?.units ?: DistanceUnits.Feet, true)
                ),
                getString(R.string.velocity_header_units, "fps"),
                getString(
                    R.string.drop_header_units,
                    formatter.getDistanceUnitName(smallUnits, true)
                )
            ) + trajectory.flatMap { point ->
                val seconds = DecimalFormatter.format(
                    point.time,
                    2
                )
                val distance = DecimalFormatter.format(
                    point.distance.convertTo(zeroDistance?.units ?: DistanceUnits.Feet).value,
                    Units.getDecimalPlaces(zeroDistance?.units ?: DistanceUnits.Feet)
                )
                val drop = DecimalFormatter.format(
                    point.drop.convertTo(smallUnits).value,
                    1
                )

                val velocity = DecimalFormatter.format(
                    point.velocity.convertTo(
                        bulletSpeed?.distanceUnits ?: DistanceUnits.Feet,
                        bulletSpeed?.timeUnits ?: TimeUnits.Seconds
                    ).value,
                    0
                )

                listOf(
                    seconds,
                    distance,
                    velocity,
                    drop
                )
            }

            ballisticsGrid.setData(listItems)
        }

        useEffect(loadingView, ballisticsTableView, loading) {
            ballisticsTableView.isVisible = !loading
            loadingView.isVisible = loading
        }
    }


    private fun calculateTrajectory(
        zeroDistance: Distance,
        scopeHeight: Distance,
        bulletSpeed: Speed,
        ballisticCoefficient: Float?
    ): List<TrajectoryPoint> {
        return tryOrDefault(emptyList()) {
            val dragModel =
                if (ballisticCoefficient == null || Arithmetic.isZero(ballisticCoefficient)) {
                    NoDragModel()
                } else {
                    G1DragModel(ballisticCoefficient)
                }

            val calculator = BallisticsCalculator()
            calculator.calculateTrajectory(zeroDistance, scopeHeight, bulletSpeed, dragModel)
        }
    }

}

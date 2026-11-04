package com.kylecorry.trail_sense.tools.ballistics.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.kylecorry.andromeda.core.cache.DependencyRegistry
import com.kylecorry.andromeda.core.math.DecimalFormatter
import com.kylecorry.sol.science.physics.Physics
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.sol.units.EnergyUnits
import com.kylecorry.sol.units.WeightUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseRowGroup

/** Muzzle energy: the energy leads, the two things it comes from are one group under it. */
@Composable
fun EnergyCalculatorPage(
    modifier: Modifier = Modifier
) {
    val formatter = remember { DependencyRegistry.get<FormatService>() }
    val (bulletSpeed, setBulletSpeed) = rememberSpeedPreference("cache-ballistics-bullet-speed")
    val (bulletWeight, setBulletWeight) = rememberWeightPreference("cache-ballistics-bullet-weight")
    val energy = remember(bulletSpeed, bulletWeight) {
        Physics.getKineticEnergy(
            bulletWeight ?: return@remember null,
            bulletSpeed ?: return@remember null
        ).convertTo(
            if (bulletSpeed.distanceUnits.isMetric) {
                EnergyUnits.Joules
            } else {
                EnergyUnits.FootPounds
            }
        )
    }
    val energyText = energy?.let {
        val formatted = DecimalFormatter.format(it.value, 1)
        if (it.units == EnergyUnits.Joules) {
            stringResource(R.string.format_joules, formatted)
        } else {
            stringResource(R.string.format_fpe, formatted)
        }
    } ?: stringResource(R.string.dash)

    BallisticsForm(modifier = modifier) {
        BallisticsHero(
            label = stringResource(R.string.energy),
            value = energyText,
            valueTag = "energy_amount"
        )

        TrailSenseRowGroup {
            row { position ->
                SpeedField(
                    label = stringResource(R.string.muzzle_velocity),
                    value = bulletSpeed,
                    units = formatter.sortDistanceUnits(
                        listOf(DistanceUnits.Feet, DistanceUnits.Meters)
                    ),
                    defaultUnit = DistanceUnits.Feet,
                    onValueChange = setBulletSpeed,
                    position = position,
                    modifier = Modifier.testTag("bullet_speed")
                )
            }
            row { position ->
                WeightField(
                    label = stringResource(R.string.bullet_weight),
                    value = bulletWeight,
                    units = formatter.sortWeightUnits(WeightUnits.entries),
                    defaultUnit = WeightUnits.Grains,
                    onValueChange = setBulletWeight,
                    position = position,
                    modifier = Modifier.testTag("bullet_weight")
                )
            }
        }
    }
}

package com.kylecorry.trail_sense.tools.ballistics.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.core.math.DecimalFormatter
import com.kylecorry.sol.science.physics.Physics
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.sol.units.EnergyUnits
import com.kylecorry.sol.units.WeightUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService

@Composable
fun EnergyCalculatorPage(
    modifier: Modifier = Modifier
) {
    val formatter = remember { AppServiceRegistry.get<FormatService>() }
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
    }.orEmpty()

    BallisticsForm(modifier = modifier) {
        SpeedField(
            label = stringResource(R.string.muzzle_velocity),
            value = bulletSpeed,
            units = formatter.sortDistanceUnits(listOf(DistanceUnits.Feet, DistanceUnits.Meters)),
            defaultUnit = DistanceUnits.Feet,
            onValueChange = setBulletSpeed,
            modifier = Modifier.testTag("bullet_speed")
        )
        WeightField(
            label = stringResource(R.string.bullet_weight),
            value = bulletWeight,
            units = formatter.sortWeightUnits(WeightUnits.entries),
            defaultUnit = WeightUnits.Grains,
            onValueChange = setBulletWeight,
            modifier = Modifier.testTag("bullet_weight")
        )
        SectionLabel(text = stringResource(R.string.energy))
        Text(
            text = energyText,
            modifier = Modifier.testTag("energy_amount"),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

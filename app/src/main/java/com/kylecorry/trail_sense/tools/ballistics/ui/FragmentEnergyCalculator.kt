package com.kylecorry.trail_sense.tools.ballistics.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kylecorry.andromeda.core.cache.DependencyRegistry
import com.kylecorry.andromeda.core.math.DecimalFormatter
import com.kylecorry.sol.science.physics.Physics
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.sol.units.EnergyUnits
import com.kylecorry.sol.units.WeightUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseCard
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing

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
    }.orEmpty()

    BallisticsForm(modifier = modifier) {
        EnergyHeroCard(energyText = energyText)

        TrailSenseCard {
            Column(
                verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.lg)
            ) {
                Text(
                    text = stringResource(R.string.energy),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(TrailSenseSpacing.lg)
                ) {
                    TonalIconChip(iconRes = R.drawable.ic_speed)
                    SpeedField(
                        label = stringResource(R.string.muzzle_velocity),
                        value = bulletSpeed,
                        units = formatter.sortDistanceUnits(
                            listOf(DistanceUnits.Feet, DistanceUnits.Meters)
                        ),
                        defaultUnit = DistanceUnits.Feet,
                        onValueChange = setBulletSpeed,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("bullet_speed")
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(TrailSenseSpacing.lg)
                ) {
                    TonalIconChip(iconRes = R.drawable.ic_weight)
                    WeightField(
                        label = stringResource(R.string.bullet_weight),
                        value = bulletWeight,
                        units = formatter.sortWeightUnits(WeightUnits.entries),
                        defaultUnit = WeightUnits.Grains,
                        onValueChange = setBulletWeight,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("bullet_weight")
                    )
                }
            }
        }
    }
}

@Composable
private fun EnergyHeroCard(energyText: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(TrailSenseSpacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xs)
        ) {
            Text(
                text = stringResource(R.string.energy),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = energyText,
                modifier = Modifier.testTag("energy_amount"),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun TonalIconChip(iconRes: Int) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.size(40.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

package com.kylecorry.trail_sense.tools.ballistics.ui

import android.widget.TextView
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import com.kylecorry.andromeda.core.math.DecimalFormatter
import com.kylecorry.sol.science.physics.Physics
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.sol.units.EnergyUnits
import com.kylecorry.sol.units.WeightUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.extensions.TrailSenseComposeFragment
import com.kylecorry.trail_sense.shared.views.BulletSpeedInputView
import com.kylecorry.trail_sense.shared.views.WeightInputView
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseCard

@Composable
fun Fragment.EnergyCalculatorContent(modifier: Modifier = Modifier) {
    val formatter = FormatService.getInstance(requireContext())

    var bulletSpeed by remember { mutableStateOf<com.kylecorry.sol.units.Speed?>(null) }
    var bulletWeight by remember { mutableStateOf<com.kylecorry.sol.units.Weight?>(null) }

    val energy = remember(bulletSpeed, bulletWeight) {
        if (bulletSpeed == null || bulletWeight == null) return@remember null
        Physics.getKineticEnergy(bulletWeight!!, bulletSpeed!!).convertTo(
            if (bulletSpeed!!.distanceUnits.isMetric) EnergyUnits.Joules else EnergyUnits.FootPounds
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        AndroidView(
            factory = { ctx ->
                BulletSpeedInputView(ctx).apply {
                    units = formatter.sortDistanceUnits(listOf(DistanceUnits.Feet, DistanceUnits.Meters))
                    hint = ctx.getString(R.string.muzzle_velocity)
                    setOnValueChangeListener { value ->
                        bulletSpeed = value
                    }
                    if (bulletSpeed != null) value = bulletSpeed!!
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        AndroidView(
            factory = { ctx ->
                WeightInputView(ctx).apply {
                    units = formatter.sortWeightUnits(WeightUnits.entries)
                    unit = WeightUnits.Grains
                    hint = ctx.getString(R.string.bullet_weight)
                    setOnValueChangeListener { value ->
                        bulletWeight = value
                    }
                    if (bulletWeight != null) value = bulletWeight!!
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(24.dp))

        TrailSenseCard {
            Column(Modifier.padding(16.dp)) {
                Text(
                    text = getString(R.string.energy),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = energy?.let {
                        val formatted = DecimalFormatter.format(it.value, 1)
                        if (it.units == EnergyUnits.Joules) {
                            getString(R.string.format_joules, formatted)
                        } else {
                            getString(R.string.format_fpe, formatted)
                        }
                    } ?: "",
                    style = MaterialTheme.typography.headlineSmall
                )
            }
        }
    }
}

class FragmentEnergyCalculator : TrailSenseComposeFragment() {

    @Composable
    override fun FragmentContent() {
        EnergyCalculatorContent()
    }
}

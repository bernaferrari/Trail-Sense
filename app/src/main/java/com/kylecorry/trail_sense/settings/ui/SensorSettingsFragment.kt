package com.kylecorry.trail_sense.settings.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.sense.Sensors
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDivider
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseListItem
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSectionHeader
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing

class SensorSettingsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TrailSenseTheme {
                    val hasBarometer = remember { Sensors.hasBarometer(requireContext()) }
                    val items = remember(hasBarometer) {
                        sensorSettingsItems(hasBarometer)
                    }

                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = TrailSenseSpacing.lg,
                                end = TrailSenseSpacing.lg,
                                top = TrailSenseSpacing.lg,
                                bottom = TrailSenseSpacing.xxxl
                            )
                        ) {
                            item("header") {
                                TrailSenseSectionHeader(title = stringResource(R.string.sensors))
                            }
                            items.forEach { item ->
                                item(item.key) {
                                    TrailSenseListItem(
                                        title = stringResource(item.title),
                                        icon = painterResource(item.icon),
                                        onClick = {
                                            findNavController().navigate(item.destination)
                                        },
                                        animateEntrance = false,
                                        modifier = Modifier.padding(
                                            horizontal = TrailSenseSpacing.lg
                                        )
                                    )
                                    TrailSenseDivider()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun sensorSettingsItems(hasBarometer: Boolean): List<SensorSettingsItem> {
        return listOfNotNull(
            SensorSettingsItem(
                key = "compass",
                title = R.string.pref_compass_sensor_title,
                icon = R.drawable.ic_compass_icon,
                destination = R.id.calibrateCompassFragment
            ),
            SensorSettingsItem(
                key = "gps",
                title = R.string.gps,
                icon = R.drawable.satellite,
                destination = R.id.calibrateGPSFragment
            ),
            SensorSettingsItem(
                key = "altimeter",
                title = R.string.pref_altimeter_calibration_title,
                icon = R.drawable.ic_altitude,
                destination = R.id.calibrateAltimeterFragment
            ),
            if (hasBarometer) {
                SensorSettingsItem(
                    key = "barometer",
                    title = R.string.barometer,
                    icon = R.drawable.ic_barometer,
                    destination = R.id.calibrateBarometerFragment
                )
            } else {
                null
            },
            SensorSettingsItem(
                key = "temperature",
                title = R.string.tool_thermometer_title,
                icon = R.drawable.thermometer,
                destination = R.id.thermometerSettingsFragment
            ),
            SensorSettingsItem(
                key = "camera",
                title = R.string.camera,
                icon = R.drawable.ic_camera,
                destination = R.id.cameraSettingsFragment
            ),
            SensorSettingsItem(
                key = "cell_signal",
                title = R.string.cell_signal,
                icon = R.drawable.signal_cellular_3,
                destination = R.id.cellSignalSettingsFragment
            ),
            SensorSettingsItem(
                key = "details",
                title = R.string.pref_sensor_details_title,
                icon = R.drawable.ic_sensors,
                destination = R.id.sensorDetailsFragment
            )
        )
    }

    private data class SensorSettingsItem(
        val key: String,
        val title: Int,
        @DrawableRes val icon: Int,
        @IdRes val destination: Int
    )
}

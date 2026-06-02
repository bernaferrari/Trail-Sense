package com.kylecorry.trail_sense.settings.ui

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.Fragment
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.andromeda.sense.Sensors
import com.kylecorry.sol.science.meteorology.forecast.ForecastSource
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.alerts.NotificationSubsystem.GroupBehavior
import com.kylecorry.trail_sense.shared.debugging.isDebug
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.requireMainActivity
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDivider
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSectionHeader
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

class ExperimentalSettingsFragment : Fragment() {

    private val cache by lazy { PreferencesSubsystem.getInstance(requireContext()).preferences }
    private val userPrefs by lazy { UserPreferences(requireContext()) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TrailSenseTheme {
                    val hasMetalDirectionSupport = remember {
                        Sensors.hasGyroscope(requireContext()) &&
                                SensorService(requireContext()).hasCompass()
                    }
                    val showForecastSource = remember {
                        Tools.isToolAvailable(requireContext(), Tools.WEATHER) && isDebug()
                    }
                    val showNotificationGrouping = remember {
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA
                    }
                    val forecastOptions = remember { forecastOptions() }
                    val notificationGroupingOptions = remember {
                        notificationGroupingOptions()
                    }

                    var showMetalDirection by remember {
                        mutableStateOf(getBoolean(R.string.pref_experimental_metal_direction, false))
                    }
                    var useZeroShutterLag by remember {
                        mutableStateOf(userPrefs.camera.useZeroShutterLag)
                    }
                    var isCliffHeightEnabled by remember {
                        mutableStateOf(userPrefs.isCliffHeightEnabled)
                    }
                    var useFilteredGps by remember {
                        mutableStateOf(userPrefs.useFilteredGPS)
                    }
                    var forecastSource by remember {
                        mutableStateOf(forecastSourceValue(userPrefs.weather.forecastSource))
                    }
                    var showPressureForecast by remember {
                        mutableStateOf(userPrefs.weather.showPressureForecast)
                    }
                    var notificationGrouping by remember {
                        mutableStateOf(
                            notificationGroupingValue(userPrefs.notificationGroupingBehavior)
                        )
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
                                TrailSenseSectionHeader(
                                    title = stringResource(R.string.experimental)
                                )
                            }
                            if (hasMetalDirectionSupport) {
                                item("metal_direction") {
                                    SettingsSwitchRow(
                                        label = stringResource(R.string.pref_experimental_metal_direction_title),
                                        summary = stringResource(R.string.tool_metal_detector_title),
                                        checked = showMetalDirection,
                                        onCheckedChange = {
                                            showMetalDirection = it
                                            putBoolean(
                                                R.string.pref_experimental_metal_direction,
                                                it
                                            )
                                        },
                                        modifier = Modifier.padding(
                                            horizontal = TrailSenseSpacing.lg
                                        )
                                    )
                                    TrailSenseDivider()
                                }
                            }
                            item("zero_shutter_lag") {
                                SettingsSwitchRow(
                                    label = stringResource(R.string.zero_shutter_lag),
                                    summary = stringResource(R.string.camera),
                                    checked = useZeroShutterLag,
                                    onCheckedChange = {
                                        useZeroShutterLag = it
                                        putBoolean(R.string.pref_use_zero_shutter_lag, it)
                                    },
                                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                )
                                TrailSenseDivider()
                            }
                            item("cliff_height") {
                                SettingsSwitchRow(
                                    label = stringResource(R.string.tool_cliff_height_title),
                                    summary = stringResource(R.string.tool_cliff_height_description),
                                    checked = isCliffHeightEnabled,
                                    onCheckedChange = {
                                        isCliffHeightEnabled = it
                                        userPrefs.isCliffHeightEnabled = it
                                        requireMainActivity().updateBottomNavigation()
                                    },
                                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                )
                                TrailSenseDivider()
                            }
                            item("filtered_gps") {
                                SettingsSwitchRow(
                                    label = stringResource(R.string.smooth_gps_readings),
                                    checked = useFilteredGps,
                                    onCheckedChange = {
                                        useFilteredGps = it
                                        putBoolean(R.string.pref_use_filtered_gps, it)
                                    },
                                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                )
                                TrailSenseDivider()
                            }
                            if (showForecastSource) {
                                item("forecast_source") {
                                    SettingsPickerRow(
                                        label = stringResource(R.string.forecast_algorithm),
                                        selected = optionLabel(
                                            forecastOptions,
                                            forecastSource
                                        ),
                                        onClick = {
                                            pickOption(
                                                getString(R.string.forecast_algorithm),
                                                forecastOptions,
                                                forecastSource
                                            ) {
                                                forecastSource = it
                                                putString(
                                                    R.string.pref_weather_forecast_source,
                                                    it
                                                )
                                            }
                                        },
                                        modifier = Modifier.padding(
                                            horizontal = TrailSenseSpacing.lg
                                        )
                                    )
                                }
                            }
                            item("pressure_forecast") {
                                SettingsSwitchRow(
                                    label = stringResource(R.string.show_pressure_forecast),
                                    summary = stringResource(R.string.weather),
                                    checked = showPressureForecast,
                                    onCheckedChange = {
                                        showPressureForecast = it
                                        putBoolean(R.string.pref_show_pressure_forecast, it)
                                    },
                                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                )
                                TrailSenseDivider()
                            }
                            if (showNotificationGrouping) {
                                item("notification_grouping") {
                                    SettingsPickerRow(
                                        label = stringResource(R.string.notification_grouping),
                                        selected = optionLabel(
                                            notificationGroupingOptions,
                                            notificationGrouping
                                        ),
                                        onClick = {
                                            pickOption(
                                                getString(R.string.notification_grouping),
                                                notificationGroupingOptions,
                                                notificationGrouping
                                            ) {
                                                notificationGrouping = it
                                                putString(
                                                    R.string.pref_notification_group_behavior,
                                                    it
                                                )
                                            }
                                        },
                                        modifier = Modifier.padding(
                                            horizontal = TrailSenseSpacing.lg
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun pickOption(
        title: String,
        options: List<ExperimentalOption>,
        selected: String,
        onSelected: (String) -> Unit
    ) {
        Pickers.item(
            requireContext(),
            title,
            options.map { it.label },
            options.indexOfFirst { it.value == selected }.coerceAtLeast(0)
        ) { index ->
            if (index != null) {
                onSelected(options[index].value)
            }
        }
    }

    private fun optionLabel(
        options: List<ExperimentalOption>,
        selected: String
    ): String {
        return options.firstOrNull { it.value == selected }?.label ?: selected
    }

    private fun forecastOptions(): List<ExperimentalOption> {
        return ForecastSource.entries.map {
            ExperimentalOption(forecastSourceValue(it), it.name)
        }
    }

    private fun notificationGroupingOptions(): List<ExperimentalOption> {
        val labels = mapOf(
            GroupBehavior.System to getString(R.string.theme_system),
            GroupBehavior.UngroupAll to getString(R.string.ungroup_all),
            GroupBehavior.UngroupHigh to getString(R.string.ungroup_alerts)
        )
        return GroupBehavior.entries.map {
            ExperimentalOption(
                notificationGroupingValue(it),
                labels[it] ?: it.name
            )
        }
    }

    private fun forecastSourceValue(source: ForecastSource): String {
        return when (source) {
            ForecastSource.Sol -> "1"
            ForecastSource.Zambretti -> "2"
        }
    }

    private fun notificationGroupingValue(behavior: GroupBehavior): String {
        return when (behavior) {
            GroupBehavior.System -> "1"
            GroupBehavior.UngroupAll -> "2"
            GroupBehavior.UngroupHigh -> "3"
        }
    }

    private fun getBoolean(key: Int, default: Boolean): Boolean {
        return cache.getBoolean(getString(key)) ?: default
    }

    private fun putBoolean(key: Int, value: Boolean) {
        cache.putBoolean(getString(key), value)
    }

    private fun putString(key: Int, value: String) {
        cache.putString(getString(key), value)
    }

    private data class ExperimentalOption(
        val value: String,
        val label: String
    )
}

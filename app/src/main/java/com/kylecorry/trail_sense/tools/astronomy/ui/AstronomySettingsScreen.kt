@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.astronomy.ui

import android.os.Bundle
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kylecorry.trail_sense.shared.extensions.asLiveData
import com.kylecorry.trail_sense.shared.dialogs.Pickers
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseShape
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSize
import com.kylecorry.trail_sense.tools.astronomy.AstronomyToolRegistration
import com.kylecorry.trail_sense.tools.astronomy.infrastructure.receivers.SunriseAlarmReceiver
import com.kylecorry.trail_sense.tools.astronomy.infrastructure.receivers.SunsetAlarmReceiver
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import java.time.Duration

@Composable
fun AstronomySettingsToolScreen(host: TrailSenseDestinationHost) {
    val controller = remember(host) { AstronomySettingsController(host) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(controller, lifecycleOwner) {
        controller.onCreate()
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> controller.onResume()
                Lifecycle.Event.ON_PAUSE -> controller.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            controller.onDestroy()
        }
    }

    controller.Content()
}

private class AstronomySettingsController(private val host: TrailSenseDestinationHost) {

    private val cache by lazy { PreferencesSubsystem.getInstance(host.requireContext()).preferences }
    private val prefs by lazy { UserPreferences(host.requireContext()) }
    private val formatter by lazy { FormatService.getInstance(host.requireContext()) }
    private val sunsetService by lazy {
        Tools.getService(
            host.requireContext(),
            AstronomyToolRegistration.SERVICE_SUNSET_ALERTS
        )!!
    }
    private val sunriseService by lazy {
        Tools.getService(
            host.requireContext(),
            AstronomyToolRegistration.SERVICE_SUNRISE_ALERTS
        )!!
    }

    private var setSunsetAlertsState: ((Boolean) -> Unit)? = null
    private var setSunriseAlertsState: ((Boolean) -> Unit)? = null

    @Composable
    fun Content() {
                    val sunTimeOptions = remember {
                        options(R.array.sun_times_entries, R.array.sun_times_values)
                    }
                    val quickActionOptions = remember {
                        Tools.getQuickActions(host.requireContext()).map {
                            AstronomyOption(it.id.toString(), it.name)
                        }
                    }
                    val sunsetTimeOptions = remember {
                        timeOptions(listOf(30L, 60L, 90L, 120L, 150L, 180L))
                    }
                    val sunriseTimeOptions = remember {
                        timeOptions(listOf(0L, 15L, 30L, 45L, 60L, 90L, 120L))
                    }
                    val showStartCameraIn3D = remember {
                        Tools.isToolAvailable(host.requireContext(), Tools.AUGMENTED_REALITY)
                    }

                    var sunTimeMode by remember {
                        mutableStateOf(getStringPreference(R.string.pref_sun_time_mode, "actual"))
                    }
                    var centerSunAndMoon by remember {
                        mutableStateOf(getBoolean(R.string.pref_center_sun_and_moon, false))
                    }
                    var showAstronomyBands by remember {
                        mutableStateOf(prefs.astronomy.showAstronomyBands)
                    }
                    var leftQuickAction by remember {
                        mutableStateOf(prefs.astronomy.leftButton.toString())
                    }
                    var rightQuickAction by remember {
                        mutableStateOf(prefs.astronomy.rightButton.toString())
                    }
                    var startCameraIn3D by remember {
                        mutableStateOf(
                            getBoolean(R.string.pref_start_camera_in_3d_view, true)
                        )
                    }
                    var sunsetAlerts by remember {
                        mutableStateOf(prefs.astronomy.sendSunsetAlerts)
                    }
                    var sunsetAlertTime by remember {
                        mutableStateOf(
                            getStringPreference(R.string.pref_sunset_alert_time, "60")
                        )
                    }
                    var useAlarmForSunset by remember {
                        mutableStateOf(
                            getBoolean(
                                R.string.pref_astronomy_use_alarm_for_sunset_alert,
                                false
                            )
                        )
                    }
                    var sunriseAlerts by remember {
                        mutableStateOf(prefs.astronomy.sendSunriseAlerts)
                    }
                    var sunriseAlertTime by remember {
                        mutableStateOf(
                            getStringPreference(R.string.pref_sunrise_alert_time, "0")
                        )
                    }
                    var useAlarmForSunrise by remember {
                        mutableStateOf(
                            getBoolean(
                                R.string.pref_astronomy_use_alarm_for_sunrise_alert,
                                false
                            )
                        )
                    }
                    var meteorAlerts by remember {
                        mutableStateOf(prefs.astronomy.sendMeteorShowerAlerts)
                    }
                    var lunarEclipseAlerts by remember {
                        mutableStateOf(prefs.astronomy.sendLunarEclipseAlerts)
                    }
                    var solarEclipseAlerts by remember {
                        mutableStateOf(prefs.astronomy.sendSolarEclipseAlerts)
                    }

                    setSunsetAlertsState = {
                        sunsetAlerts = prefs.astronomy.sendSunsetAlerts
                    }
                    setSunriseAlertsState = {
                        sunriseAlerts = prefs.astronomy.sendSunriseAlerts
                    }

                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .widthIn(max = TrailSenseSize.maximumContentWidth),
                                contentPadding = PaddingValues(
                                    start = TrailSenseSpacing.screen,
                                    end = TrailSenseSpacing.screen,
                                    top = TrailSenseSpacing.lg,
                                    bottom = TrailSenseSpacing.xxxl
                                ),
                                verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.lg)
                            ) {
                            item("title") {
                                Text(
                                    text = stringResource(R.string.astronomy),
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(
                                        top = TrailSenseSpacing.sm,
                                        bottom = TrailSenseSpacing.xs
                                    )
                                )
                            }
                            item("general_section") {
                                AstronomySettingsSection(
                                    title = stringResource(R.string.general)
                                ) {
                                    AstronomySettingsClickRow(
                                        title = stringResource(R.string.pref_sun_time_mode_title),
                                        subtitle = optionLabel(sunTimeOptions, sunTimeMode),
                                        icon = painterResource(R.drawable.ic_sun),
                                        onClick = {
                                            pickOption(
                                                getString(R.string.pref_sun_time_mode_title),
                                                sunTimeOptions,
                                                sunTimeMode
                                            ) {
                                                sunTimeMode = it
                                                putString(R.string.pref_sun_time_mode, it)
                                            }
                                        }
                                    )
                                    AstronomySettingsCardDivider()
                                    AstronomySettingsSwitchRow(
                                        label = stringResource(R.string.pref_center_sun_and_moon_title),
                                        checked = centerSunAndMoon,
                                        onCheckedChange = {
                                            centerSunAndMoon = it
                                            putBoolean(R.string.pref_center_sun_and_moon, it)
                                        }
                                    )
                                    AstronomySettingsCardDivider()
                                    AstronomySettingsSwitchRow(
                                        label = stringResource(R.string.pref_show_astronomy_bands_title),
                                        checked = showAstronomyBands,
                                        onCheckedChange = {
                                            showAstronomyBands = it
                                            prefs.astronomy.showAstronomyBands = it
                                        }
                                    )
                                    AstronomySettingsCardDivider()
                                    AstronomySettingsClickRow(
                                        title = stringResource(R.string.left_quick_action),
                                        subtitle = optionLabel(quickActionOptions, leftQuickAction),
                                        onClick = {
                                            pickOption(
                                                getString(R.string.left_quick_action),
                                                quickActionOptions,
                                                leftQuickAction
                                            ) {
                                                leftQuickAction = it
                                                putString(
                                                    R.string.pref_astronomy_quick_action_left,
                                                    it
                                                )
                                            }
                                        }
                                    )
                                    AstronomySettingsCardDivider()
                                    AstronomySettingsClickRow(
                                        title = stringResource(R.string.right_quick_action),
                                        subtitle = optionLabel(quickActionOptions, rightQuickAction),
                                        onClick = {
                                            pickOption(
                                                getString(R.string.right_quick_action),
                                                quickActionOptions,
                                                rightQuickAction
                                            ) {
                                                rightQuickAction = it
                                                putString(
                                                    R.string.pref_astronomy_quick_action_right,
                                                    it
                                                )
                                            }
                                        }
                                    )
                                    if (showStartCameraIn3D) {
                                        AstronomySettingsCardDivider()
                                        AstronomySettingsSwitchRow(
                                            label = stringResource(R.string.pref_start_camera_in_3d_view_title),
                                            summary = stringResource(R.string.pref_start_camera_in_3d_view_summary),
                                            checked = startCameraIn3D,
                                            onCheckedChange = {
                                                startCameraIn3D = it
                                                putBoolean(
                                                    R.string.pref_start_camera_in_3d_view,
                                                    it
                                                )
                                            }
                                        )
                                    }
                                }
                            }

                            item("alerts_section") {
                                AstronomySettingsSection(
                                    title = stringResource(R.string.alerts)
                                ) {
                                    AstronomySettingsSwitchRow(
                                        label = stringResource(R.string.sunset),
                                        icon = painterResource(R.drawable.ic_sunset_notification),
                                        checked = sunsetAlerts,
                                        onCheckedChange = {
                                            sunsetAlerts = it
                                            prefs.astronomy.sendSunsetAlerts = it
                                            onSunsetAlertsPreferenceChanged(it)
                                        }
                                    )
                                    AstronomySettingsCardDivider()
                                    AstronomySettingsClickRow(
                                        title = stringResource(R.string.pref_sunset_alert_time_title),
                                        subtitle = optionLabel(sunsetTimeOptions, sunsetAlertTime),
                                        enabled = sunsetAlerts,
                                        onClick = {
                                            pickOption(
                                                getString(R.string.pref_sunset_alert_time_title),
                                                sunsetTimeOptions,
                                                sunsetAlertTime
                                            ) {
                                                sunsetAlertTime = it
                                                putString(R.string.pref_sunset_alert_time, it)
                                                restartSunsetAlerts()
                                            }
                                        }
                                    )
                                    AstronomySettingsCardDivider()
                                    AstronomySettingsSwitchRow(
                                        label = stringResource(R.string.use_alarm_for_sunset_alert),
                                        summary = stringResource(R.string.alarm_description),
                                        checked = useAlarmForSunset,
                                        onCheckedChange = {
                                            useAlarmForSunset = it
                                            putBoolean(
                                                R.string.pref_astronomy_use_alarm_for_sunset_alert,
                                                it
                                            )
                                        }
                                    )
                                    AstronomySettingsCardDivider()
                                    AstronomySettingsSwitchRow(
                                        label = stringResource(R.string.sunrise),
                                        icon = painterResource(R.drawable.ic_sunrise_notification),
                                        checked = sunriseAlerts,
                                        onCheckedChange = {
                                            sunriseAlerts = it
                                            prefs.astronomy.sendSunriseAlerts = it
                                            onSunriseAlertsPreferenceChanged(it)
                                        }
                                    )
                                    AstronomySettingsCardDivider()
                                    AstronomySettingsClickRow(
                                        title = stringResource(R.string.pref_sunrise_alert_time_title),
                                        subtitle = optionLabel(sunriseTimeOptions, sunriseAlertTime),
                                        enabled = sunriseAlerts,
                                        onClick = {
                                            pickOption(
                                                getString(R.string.pref_sunrise_alert_time_title),
                                                sunriseTimeOptions,
                                                sunriseAlertTime
                                            ) {
                                                sunriseAlertTime = it
                                                putString(R.string.pref_sunrise_alert_time, it)
                                                restartSunriseAlerts()
                                            }
                                        }
                                    )
                                    AstronomySettingsCardDivider()
                                    AstronomySettingsSwitchRow(
                                        label = stringResource(R.string.use_alarm_for_sunrise_alert),
                                        summary = stringResource(R.string.alarm_description),
                                        checked = useAlarmForSunrise,
                                        onCheckedChange = {
                                            useAlarmForSunrise = it
                                            putBoolean(
                                                R.string.pref_astronomy_use_alarm_for_sunrise_alert,
                                                it
                                            )
                                        }
                                    )
                                    AstronomySettingsCardDivider()
                                    AstronomySettingsSwitchRow(
                                        label = stringResource(R.string.pref_meteor_showers_title),
                                        icon = painterResource(R.drawable.ic_meteor),
                                        checked = meteorAlerts,
                                        onCheckedChange = {
                                            meteorAlerts = it
                                            prefs.astronomy.sendMeteorShowerAlerts = it
                                        }
                                    )
                                    AstronomySettingsCardDivider()
                                    AstronomySettingsSwitchRow(
                                        label = stringResource(R.string.lunar_eclipses),
                                        icon = painterResource(R.drawable.ic_moon_total_eclipse),
                                        checked = lunarEclipseAlerts,
                                        onCheckedChange = {
                                            lunarEclipseAlerts = it
                                            prefs.astronomy.sendLunarEclipseAlerts = it
                                        }
                                    )
                                    AstronomySettingsCardDivider()
                                    AstronomySettingsSwitchRow(
                                        label = stringResource(R.string.solar_eclipses),
                                        icon = painterResource(R.drawable.ic_total_solar_eclipse),
                                        checked = solarEclipseAlerts,
                                        onCheckedChange = {
                                            solarEclipseAlerts = it
                                            prefs.astronomy.sendSolarEclipseAlerts = it
                                        }
                                    )
                                }
                            }
                            }
                        }
                    }
    }

    fun onCreate() {
        val sunsetAlertTimeKey = getString(R.string.pref_sunset_alert_time)
        val sunriseAlertTimeKey = getString(R.string.pref_sunrise_alert_time)

        cache.onChange.asLiveData().observe(host.lifecycleOwner) {
            if (it == sunsetAlertTimeKey) {
                restartSunsetAlerts()
            } else if (it == sunriseAlertTimeKey) {
                restartSunriseAlerts()
            }
        }
    }

    fun onResume() {
        Tools.subscribe(
            AstronomyToolRegistration.BROADCAST_SUNSET_ALERTS_ENABLED,
            ::onSunsetAlertsChanged
        )
        Tools.subscribe(
            AstronomyToolRegistration.BROADCAST_SUNSET_ALERTS_DISABLED,
            ::onSunsetAlertsChanged
        )
        Tools.subscribe(
            AstronomyToolRegistration.BROADCAST_SUNRISE_ALERTS_ENABLED,
            ::onSunriseAlertsChanged
        )
        Tools.subscribe(
            AstronomyToolRegistration.BROADCAST_SUNRISE_ALERTS_DISABLED,
            ::onSunriseAlertsChanged
        )
    }

    fun onPause() {
        Tools.unsubscribe(
            AstronomyToolRegistration.BROADCAST_SUNSET_ALERTS_ENABLED,
            ::onSunsetAlertsChanged
        )
        Tools.unsubscribe(
            AstronomyToolRegistration.BROADCAST_SUNSET_ALERTS_DISABLED,
            ::onSunsetAlertsChanged
        )
        Tools.unsubscribe(
            AstronomyToolRegistration.BROADCAST_SUNRISE_ALERTS_ENABLED,
            ::onSunriseAlertsChanged
        )
        Tools.unsubscribe(
            AstronomyToolRegistration.BROADCAST_SUNRISE_ALERTS_DISABLED,
            ::onSunriseAlertsChanged
        )
    }

    fun onDestroy() {
        setSunsetAlertsState = null
        setSunriseAlertsState = null
    }

    private fun onSunsetAlertsPreferenceChanged(enabled: Boolean) {
        host.inBackground {
            if (enabled) {
                SunsetAlarmReceiver.enable(host, true)
            } else {
                sunsetService.disable()
            }
        }
    }

    private fun onSunriseAlertsPreferenceChanged(enabled: Boolean) {
        host.inBackground {
            if (enabled) {
                SunriseAlarmReceiver.enable(host, true)
            } else {
                sunriseService.disable()
            }
        }
    }

    private fun restartSunsetAlerts() {
        host.inBackground {
            sunsetService.restart()
        }
    }

    private fun restartSunriseAlerts() {
        host.inBackground {
            sunriseService.restart()
        }
    }

    private fun pickOption(
        title: String,
        options: List<AstronomyOption>,
        selected: String,
        onSelected: (String) -> Unit
    ) {
        Pickers.item(
            host.requireContext(),
            title,
            options.map { it.label },
            options.indexOfFirst { it.value == selected }.coerceAtLeast(0)
        ) { index ->
            if (index != null) {
                onSelected(options[index].value)
            }
        }
    }

    private fun options(entriesId: Int, valuesId: Int): List<AstronomyOption> {
        val entries = host.getResources().getStringArray(entriesId)
        val values = host.getResources().getStringArray(valuesId)
        return values.mapIndexed { index, value ->
            AstronomyOption(value, entries.getOrElse(index) { value })
        }
    }

    private fun timeOptions(minutes: List<Long>): List<AstronomyOption> {
        return minutes.map {
            AstronomyOption(
                it.toString(),
                formatter.formatDuration(Duration.ofMinutes(it))
            )
        }
    }

    private fun optionLabel(options: List<AstronomyOption>, selected: String): String {
        return options.firstOrNull { it.value == selected }?.label ?: selected
    }

    private fun getStringPreference(key: Int, default: String): String {
        return cache.getString(getString(key)) ?: default
    }

    private fun getBoolean(key: Int, default: Boolean): Boolean {
        return cache.getBoolean(getString(key)) ?: default
    }

    private fun putString(key: Int, value: String) {
        cache.putString(getString(key), value)
    }

    private fun putBoolean(key: Int, value: Boolean) {
        cache.putBoolean(getString(key), value)
    }

    private fun getString(@StringRes id: Int, vararg args: Any): String {
        return host.getString(id, *args)
    }

    private fun onSunsetAlertsChanged(data: Bundle): Boolean {
        setSunsetAlertsState?.invoke(prefs.astronomy.sendSunsetAlerts)
        return true
    }

    private fun onSunriseAlertsChanged(data: Bundle): Boolean {
        setSunriseAlertsState?.invoke(prefs.astronomy.sendSunriseAlerts)
        return true
    }

    private data class AstronomyOption(
        val value: String,
        val label: String
    )
}

@Composable
private fun AstronomySettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(
                start = TrailSenseSpacing.md,
                bottom = TrailSenseSpacing.sm
            )
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = Color.Transparent
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xxs)) {
                content()
            }
        }
    }
}

@Composable
private fun AstronomySettingsCardDivider() {
    Spacer(modifier = Modifier.height(0.dp))
}

@Composable
private fun AstronomySettingsIconChip(icon: Painter) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.size(TrailSenseSize.leadingIconContainer)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(TrailSenseSize.leadingIcon)
            )
        }
    }
}

@Composable
private fun AstronomySettingsClickRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: Painter? = null,
    enabled: Boolean = true
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(TrailSenseShape.groupInner),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .alpha(if (enabled) 1f else 0.5f)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = TrailSenseSpacing.lg, vertical = TrailSenseSpacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            AstronomySettingsIconChip(icon)
            Spacer(Modifier.width(TrailSenseSpacing.lg))
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xxs)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(Modifier.width(TrailSenseSpacing.md))
        Box(
            modifier = Modifier.size(TrailSenseSize.minimumTouchTarget),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    }
}

@Composable
private fun AstronomySettingsSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    icon: Painter? = null,
    enabled: Boolean = true
) {
    val containerColor = if (checked && enabled) {
        lerp(MaterialTheme.colorScheme.surfaceContainerLow, MaterialTheme.colorScheme.primaryContainer, 0.35f)
    } else MaterialTheme.colorScheme.surfaceContainerLow
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(TrailSenseShape.groupInner),
        color = containerColor
    ) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .alpha(if (enabled) 1f else 0.5f)
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(horizontal = TrailSenseSpacing.lg, vertical = TrailSenseSpacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            AstronomySettingsIconChip(icon)
            Spacer(Modifier.width(TrailSenseSpacing.lg))
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xxs)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (summary != null) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.width(TrailSenseSpacing.md))
        Switch(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled
        )
        }
    }
}

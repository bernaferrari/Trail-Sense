@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.pedometer.ui

import android.os.Bundle
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
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.system.Intents
import com.kylecorry.andromeda.notify.Notify
import com.kylecorry.andromeda.permissions.Permissions
import com.kylecorry.luna.time.CoroutineTimer
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.DistanceUtils
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseShape
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSize
import com.kylecorry.trail_sense.tools.pedometer.PedometerToolRegistration
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.StepCounterService
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.subsystem.PedometerSubsystem
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

@Composable
fun PedometerSettingsToolScreen(host: TrailSenseDestinationHost) {
    val controller = remember(host) { PedometerSettingsController(host) }
    val lifecycleOwner = LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(controller, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> controller.onResume()
                Lifecycle.Event.ON_PAUSE -> controller.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            controller.onResume()
        }
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            controller.onDestroy()
        }
    }

    PedometerSettingsContent(controller, host)
}

private class PedometerSettingsController(private val host: TrailSenseDestinationHost) {

    val userPrefs by lazy { UserPreferences(host.requireContext()) }
    val cache by lazy { PreferencesSubsystem.getInstance(host.requireContext()).preferences }
    val formatter by lazy { FormatService.getInstance(host.requireContext()) }
    private val strideLengthUnits by lazy {
        formatter.sortDistanceUnits(DistanceUtils.humanDistanceUnits)
    }

    var setEnabledState: ((Boolean) -> Unit)? = null
    var refreshPermissionState: (() -> Unit)? = null
    var refreshNotificationState: (() -> Unit)? = null

    private val intervalometer = CoroutineTimer {
        refreshPermissionState?.invoke()
    }

    fun onResume() {
        refreshPermissionState?.invoke()
        refreshNotificationState?.invoke()
        intervalometer.interval(20)
        Tools.subscribe(PedometerToolRegistration.BROADCAST_PEDOMETER_ENABLED, ::onPedometerEnabled)
        Tools.subscribe(
            PedometerToolRegistration.BROADCAST_PEDOMETER_DISABLED,
            ::onPedometerDisabled
        )
    }

    fun onPause() {
        intervalometer.stop()
        Tools.unsubscribe(
            PedometerToolRegistration.BROADCAST_PEDOMETER_ENABLED,
            ::onPedometerEnabled
        )
        Tools.unsubscribe(
            PedometerToolRegistration.BROADCAST_PEDOMETER_DISABLED,
            ::onPedometerDisabled
        )
    }

    fun onDestroy() {
        onPause()
        setEnabledState = null
        refreshPermissionState = null
        refreshNotificationState = null
    }

    fun onPedometerEnabledChanged(
        enabled: Boolean,
        updateEnabled: (Boolean) -> Unit
    ) {
        if (enabled) {
            host.requestActivityRecognition { hasPermission ->
                if (hasPermission) {
                    if (cache.getBoolean("pedometer_battery_sent") != true) {
                        Alerts.dialog(
                            host.requireContext(),
                            host.getString(R.string.pedometer),
                            host.getString(R.string.pedometer_disclaimer),
                            cancelText = null
                        )
                        cache.putBoolean("pedometer_battery_sent", true)
                    }
                    PedometerSubsystem.getInstance(host.requireContext()).enable()
                } else {
                    PedometerSubsystem.getInstance(host.requireContext()).disable()
                    updateEnabled(false)
                    host.alertNoActivityRecognitionPermission()
                }
            }
        } else {
            PedometerSubsystem.getInstance(host.requireContext()).disable()
        }
    }

    fun pickStrideLength(
        current: Distance,
        onPicked: (Distance) -> Unit
    ) {
        CustomUiUtils.pickDistance(
            host.requireContext(),
            strideLengthUnits,
            current,
            host.getString(R.string.pref_stride_length_title),
            showFeetAndInches = true
        ) { distance, cancelled ->
            if (cancelled || distance == null || distance.value <= 0f) {
                return@pickDistance
            }

            onPicked(distance)
        }
    }

    fun formatStrideLength(distance: Distance): String {
        return formatter.formatDistance(distance, 2)
    }

    fun isPermissionRequestVisible(enabled: Boolean): Boolean {
        return (enabled && !Permissions.canRecognizeActivity(host.requireContext())) ||
                (!enabled && !Permissions.isBackgroundLocationEnabled(host.requireContext()))
    }

    fun notificationSummary(): String {
        val isBlocked = Notify.isChannelBlocked(host.requireContext(), StepCounterService.CHANNEL_ID)
        return if (isBlocked) host.getString(R.string.off) else host.getString(R.string.on)
    }

    fun getBoolean(key: Int, defaultValue: Boolean = false): Boolean {
        return cache.getBoolean(host.getString(key)) ?: defaultValue
    }

    fun putBoolean(key: Int, value: Boolean) {
        cache.putBoolean(host.getString(key), value)
    }

    private fun onPedometerEnabled(_data: Bundle): Boolean {
        setEnabledState?.invoke(true)
        return true
    }

    private fun onPedometerDisabled(_data: Bundle): Boolean {
        setEnabledState?.invoke(false)
        return true
    }
}

@Composable
private fun PedometerSettingsContent(
    controller: PedometerSettingsController,
    host: TrailSenseDestinationHost
) {
                    val userPrefs = controller.userPrefs
                    var enabled by remember { mutableStateOf(userPrefs.pedometer.isEnabled) }
                    var strideLength by remember {
                        mutableStateOf(
                            userPrefs.pedometer.strideLength.convertTo(userPrefs.baseDistanceUnits)
                        )
                    }
                    var resetDaily by remember {
                        mutableStateOf(controller.getBoolean(R.string.pref_odometer_reset_daily))
                    }
                    var useAlarm by remember {
                        mutableStateOf(
                            controller.getBoolean(R.string.pref_pedometer_use_alarm_for_distance_alert)
                        )
                    }
                    var permissionVisible by remember {
                        mutableStateOf(controller.isPermissionRequestVisible(enabled))
                    }
                    var notificationSummary by remember {
                        mutableStateOf(controller.notificationSummary())
                    }

                    controller.setEnabledState = {
                        enabled = it
                        permissionVisible = controller.isPermissionRequestVisible(it)
                    }
                    controller.refreshPermissionState = {
                        permissionVisible = controller.isPermissionRequestVisible(enabled)
                    }
                    controller.refreshNotificationState = {
                        notificationSummary = controller.notificationSummary()
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
                                    text = stringResource(R.string.pedometer),
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(
                                        top = TrailSenseSpacing.sm,
                                        bottom = TrailSenseSpacing.xs
                                    )
                                )
                            }

                            item("pedometer_section") {
                                PedometerSettingsSection(
                                    title = stringResource(R.string.pedometer)
                                ) {
                                    PedometerSettingsSwitchRow(
                                        label = stringResource(R.string.pedometer),
                                        icon = painterResource(R.drawable.steps),
                                        checked = enabled,
                                        onCheckedChange = {
                                            enabled = it
                                            userPrefs.pedometer.isEnabled = it
                                            controller.onPedometerEnabledChanged(it) { newEnabled ->
                                                enabled = newEnabled
                                                permissionVisible =
                                                    controller.isPermissionRequestVisible(newEnabled)
                                            }
                                            permissionVisible =
                                                controller.isPermissionRequestVisible(it)
                                        }
                                    )
                                    PedometerSettingsCardDivider()
                                    PedometerSettingsClickRow(
                                        title = stringResource(R.string.pref_stride_length_title),
                                        subtitle = controller.formatStrideLength(strideLength),
                                        icon = painterResource(R.drawable.ruler),
                                        onClick = {
                                            controller.pickStrideLength(strideLength) {
                                                strideLength = it
                                                userPrefs.pedometer.strideLength = it
                                            }
                                        }
                                    )
                                    PedometerSettingsCardDivider()
                                    PedometerSettingsClickRow(
                                        title = stringResource(R.string.estimate_stride_length),
                                        onClick = {
                                            host.findNavController().navigate(
                                                R.id.action_calibrate_pedometer_to_estimate_stride_length
                                            )
                                        }
                                    )
                                    PedometerSettingsCardDivider()
                                    PedometerSettingsClickRow(
                                        title = host.getString(
                                            R.string.notifications_channel,
                                            host.getString(R.string.pedometer)
                                        ),
                                        subtitle = notificationSummary,
                                        onClick = {
                                            host.startActivity(
                                                Intents.notificationSettings(
                                                    host.requireContext(),
                                                    StepCounterService.CHANNEL_ID
                                                )
                                            )
                                        }
                                    )
                                    if (permissionVisible) {
                                        PedometerSettingsCardDivider()
                                        PedometerSettingsClickRow(
                                            title = stringResource(R.string.request_permission),
                                            onClick = {
                                                host.startActivity(
                                                    Intents.appSettings(host.requireContext())
                                                )
                                            }
                                        )
                                    }
                                }
                            }

                            item("distance_section") {
                                PedometerSettingsSection(
                                    title = stringResource(R.string.distance)
                                ) {
                                    PedometerSettingsSwitchRow(
                                        label = stringResource(R.string.pref_odometer_reset_daily_title),
                                        checked = resetDaily,
                                        onCheckedChange = {
                                            resetDaily = it
                                            controller.putBoolean(
                                                R.string.pref_odometer_reset_daily,
                                                it
                                            )
                                        }
                                    )
                                    PedometerSettingsCardDivider()
                                    PedometerSettingsSwitchRow(
                                        label = stringResource(R.string.use_alarm_for_distance_alert),
                                        summary = stringResource(R.string.alarm_description),
                                        checked = useAlarm,
                                        onCheckedChange = {
                                            useAlarm = it
                                            controller.putBoolean(
                                                R.string.pref_pedometer_use_alarm_for_distance_alert,
                                                it
                                            )
                                        }
                                    )
                                }
                            }
                            }
                        }
                    }
}

@Composable
private fun PedometerSettingsSection(
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
private fun PedometerSettingsCardDivider() {
    Spacer(modifier = Modifier.height(0.dp))
}

@Composable
private fun PedometerSettingsIconChip(icon: Painter) {
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
private fun PedometerSettingsClickRow(
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
            PedometerSettingsIconChip(icon)
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
        Box(modifier = Modifier.size(TrailSenseSize.minimumTouchTarget), contentAlignment = Alignment.Center) {
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
private fun PedometerSettingsSwitchRow(
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
            PedometerSettingsIconChip(icon)
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

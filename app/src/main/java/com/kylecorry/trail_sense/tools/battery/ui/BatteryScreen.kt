@file:Suppress("FunctionNaming")
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.kylecorry.trail_sense.tools.battery.ui

import com.kylecorry.trail_sense.tools.level.ui.CalmKeyValue
import androidx.compose.material3.LinearProgressIndicator
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseListItem

import androidx.compose.foundation.layout.navigationBarsPadding
import com.kylecorry.trail_sense.shared.views.compose.trailSenseGridCellShape
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import androidx.compose.ui.text.font.FontWeight
import com.kylecorry.trail_sense.shared.icons.trail.Check
import com.kylecorry.trail_sense.shared.icons.trail.ToolBattery
import com.kylecorry.trail_sense.shared.icons.trail.ToolClock
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseShapeBadge
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseStat
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseStatGrid
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseTone
import com.kylecorry.trail_sense.shared.views.compose.trailSenseGroupShape
import com.kylecorry.trail_sense.shared.views.compose.trailSenseToneColors
import androidx.compose.ui.graphics.vector.ImageVector
import com.kylecorry.trail_sense.shared.icons.trail.Help
import com.kylecorry.trail_sense.shared.icons.trail.CheckOutline
import com.kylecorry.trail_sense.shared.icons.trail.Alert
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import com.kylecorry.trail_sense.shared.icons.trail.TrailIcons
import com.kylecorry.trail_sense.shared.icons.trail.Cancel
import com.kylecorry.trail_sense.shared.icons.trail.Chart
import com.kylecorry.trail_sense.shared.icons.trail.KeyboardArrowRight
import com.kylecorry.trail_sense.shared.icons.trail.Power
import com.kylecorry.trail_sense.shared.icons.trail.Settings
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.kylecorry.andromeda.battery.Battery
import com.kylecorry.andromeda.battery.BatteryChargingMethod
import com.kylecorry.andromeda.battery.BatteryChargingStatus
import com.kylecorry.andromeda.battery.BatteryHealth
import com.kylecorry.andromeda.core.system.Intents
import com.kylecorry.sol.math.filters.MedianFilter
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupCard
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupPosition
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseListItem
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseMetricItem
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseMetricRow
import com.kylecorry.trail_sense.shared.views.compose.TrailSensePage
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSize
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSwitchRow
import com.kylecorry.trail_sense.shared.views.compose.rememberTrailSenseBack
import com.kylecorry.trail_sense.shared.views.compose.trailSenseSection
import com.kylecorry.trail_sense.tools.battery.domain.BatteryReading
import com.kylecorry.trail_sense.tools.battery.domain.BatteryUsage
import com.kylecorry.trail_sense.tools.battery.domain.RunningService
import com.kylecorry.trail_sense.tools.battery.domain.SystemBatteryTip
import com.kylecorry.trail_sense.tools.battery.infrastructure.BatteryService
import com.kylecorry.trail_sense.tools.battery.infrastructure.LowPowerMode
import com.kylecorry.trail_sense.tools.battery.infrastructure.persistence.BatteryRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant
import kotlin.math.absoluteValue

private const val CURRENT_FILTER_SIZE = 100

@Composable
fun BatteryToolScreen(
    onShowHistory: (List<BatteryReading>, FormatService) -> Unit,
    onOpenPowerSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val batteryService = remember { BatteryService() }
    val battery = remember { Battery(context) }
    val batteryRepo = remember { BatteryRepo.getInstance(context) }
    val lowPowerMode = remember { LowPowerMode(context) }
    val prefs = remember { UserPreferences(context) }
    val formatter = remember { FormatService.getInstance(context) }
    val handler = remember { Handler(Looper.getMainLooper()) }
    var currentFilter by remember { mutableStateOf(MedianFilter(CURRENT_FILTER_SIZE)) }

    var percent by remember { mutableStateOf(0f) }
    var capacity by remember { mutableStateOf(0f) }
    var health by remember { mutableStateOf(BatteryHealth.Unknown) }
    var current by remember { mutableStateOf(0f) }
    var chargingStatus by remember { mutableStateOf(BatteryChargingStatus.Unknown) }
    var chargeMethod by remember { mutableStateOf(BatteryChargingMethod.Unknown) }
    var rawReadings by remember { mutableStateOf(emptyList<BatteryReading>()) }
    var readings by remember { mutableStateOf(emptyList<BatteryReading>()) }
    var runningServices by remember { mutableStateOf(emptyList<RunningService>()) }
    var systemTips by remember { mutableStateOf(emptyList<SystemBatteryTip>()) }
    var isLowPowerEnabled by remember { mutableStateOf(false) }

    fun updateReadings() {
        readings = rawReadings + listOfNotNull(
            if (battery.hasValidReading) {
                BatteryReading(
                    Instant.now(),
                    percent,
                    capacity,
                    chargingStatus == BatteryChargingStatus.Charging,
                    Duration.ofMillis(SystemClock.uptimeMillis()),
                    Duration.ofMillis(SystemClock.elapsedRealtime())
                )
            } else {
                null
            }
        )
    }

    fun refreshBattery() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            handler.post { refreshBattery() }
            return
        }

        val newChargingStatus = battery.chargingStatus
        if (newChargingStatus != chargingStatus) {
            currentFilter = MedianFilter(CURRENT_FILTER_SIZE)
        }
        chargingStatus = newChargingStatus
        val isCharging = newChargingStatus == BatteryChargingStatus.Charging
        current = currentFilter.filter(battery.current.absoluteValue * if (isCharging) 1 else -1)
        capacity = battery.capacity
        percent = battery.percent
        health = battery.health
        chargeMethod = battery.chargingMethod
        updateReadings()
    }

    fun refreshRunningServices() {
        lifecycleOwner.lifecycleScope.launch {
            runningServices = withContext(Dispatchers.IO) {
                batteryService.getRunningServices(context)
            }
        }
    }

    fun refreshSystemTips() {
        lifecycleOwner.lifecycleScope.launch {
            systemTips = withContext(Dispatchers.IO) {
                batteryService.getSystemBatteryTips(context)
            }
        }
    }

    fun showHistory() {
        if (prefs.power.enableBatteryLog) {
            onShowHistory(readings, formatter)
        } else {
            Alerts.dialog(
                context,
                context.getString(R.string.pref_tiles_battery_log),
                context.getString(R.string.pref_dialog_battery_log_summary),
                okText = context.getString(R.string.settings),
                onClose = {
                    if (!it) {
                        onOpenPowerSettings()
                    }
                }
            )
        }
    }

    fun openBatterySettings() {
        val settingsIntent = Intent(Intent.ACTION_POWER_USAGE_SUMMARY)
        if (Intents.hasReceiver(context, settingsIntent)) {
            context.startActivity(settingsIntent)
        }
    }

    fun canOpenSystemBatterySettings(): Boolean {
        return Intents.hasReceiver(context, Intent(Intent.ACTION_POWER_USAGE_SUMMARY))
    }

    fun toggleLowPower() {
        if (lowPowerMode.isEnabled()) {
            prefs.power.userEnabledLowPower = false
            lowPowerMode.disable()
        } else {
            prefs.power.userEnabledLowPower = true
            lowPowerMode.enable()
        }
        isLowPowerEnabled = lowPowerMode.isEnabled()
        refreshRunningServices()
    }

    fun disableService(service: RunningService) {
        lifecycleOwner.lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                service.disable()
            }
            refreshRunningServices()
        }
    }

    fun openTip(tip: SystemBatteryTip) {
        tip.manage?.invoke(context)
    }

    DisposableEffect(lifecycleOwner, batteryRepo) {
        val observer = androidx.lifecycle.Observer<List<BatteryReading>> { allReadings ->
            rawReadings = allReadings.sortedBy { it.time }
            updateReadings()
        }
        batteryRepo.get().observe(lifecycleOwner, observer)
        onDispose {
            batteryRepo.get().removeObserver(observer)
        }
    }

    DisposableEffect(lifecycleOwner, battery) {
        val batteryListener = {
            refreshBattery()
            true
        }
        val batteryRefreshRunnable = object : Runnable {
            override fun run() {
                refreshBattery()
                handler.postDelayed(this, 500)
            }
        }
        val servicesRefreshRunnable = object : Runnable {
            override fun run() {
                refreshRunningServices()
                handler.postDelayed(this, 1000)
            }
        }
        val tipsRefreshRunnable = object : Runnable {
            override fun run() {
                refreshSystemTips()
                handler.postDelayed(this, 10000)
            }
        }

        fun start() {
            currentFilter = MedianFilter(CURRENT_FILTER_SIZE)
            battery.start(batteryListener)
            refreshBattery()
            refreshRunningServices()
            refreshSystemTips()
            isLowPowerEnabled = lowPowerMode.isEnabled()
            handler.removeCallbacks(batteryRefreshRunnable)
            handler.removeCallbacks(servicesRefreshRunnable)
            handler.removeCallbacks(tipsRefreshRunnable)
            handler.postDelayed(batteryRefreshRunnable, 500)
            handler.postDelayed(servicesRefreshRunnable, 1000)
            handler.postDelayed(tipsRefreshRunnable, 10000)
        }

        fun stop() {
            handler.removeCallbacks(batteryRefreshRunnable)
            handler.removeCallbacks(servicesRefreshRunnable)
            handler.removeCallbacks(tipsRefreshRunnable)
            battery.stop(batteryListener)
        }

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> start()
                Lifecycle.Event.ON_PAUSE -> stop()
                else -> Unit
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            start()
        }

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            stop()
        }
    }

    BatteryScreen(
        percent = percent,
        capacity = capacity,
        health = health,
        current = current,
        chargingStatus = chargingStatus,
        chargeMethod = chargeMethod,
        readings = readings,
        services = runningServices,
        tips = systemTips,
        isLowPowerEnabled = isLowPowerEnabled,
        formatter = formatter,
        battery = battery,
        batteryService = batteryService,
        canOpenSystemBatterySettings = canOpenSystemBatterySettings(),
        onShowHistory = ::showHistory,
        onOpenBatterySettings = ::openBatterySettings,
        onToggleLowPower = ::toggleLowPower,
        onDisableService = ::disableService,
        onOpenTip = ::openTip,
        modifier = modifier
    )
}

@Composable
private fun BatteryScreen(
    percent: Float,
    capacity: Float,
    health: BatteryHealth,
    current: Float,
    chargingStatus: BatteryChargingStatus,
    chargeMethod: BatteryChargingMethod,
    readings: List<BatteryReading>,
    services: List<RunningService>,
    tips: List<SystemBatteryTip>,
    isLowPowerEnabled: Boolean,
    formatter: FormatService,
    battery: Battery,
    batteryService: BatteryService,
    canOpenSystemBatterySettings: Boolean,
    onShowHistory: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onToggleLowPower: () -> Unit,
    onDisableService: (RunningService) -> Unit,
    onOpenTip: (SystemBatteryTip) -> Unit,
    modifier: Modifier = Modifier
) {
    // One question: how much charge is left, and how long will it last?
    val isCharging = chargingStatus == BatteryChargingStatus.Charging
    val isLow = percent < 25f
    val time = if (isCharging) {
        batteryService.getTimeUntilFull(battery, readings)
    } else {
        batteryService.getTimeUntilEmpty(battery, readings)
    }
    val currentText = formatCurrentText(current, isCharging, chargeMethod, formatter)
    val deepSleep = batteryService.getDeepSleep(readings, Duration.ofHours(6))
    val deepSleepLabel = stringResource(R.string.battery_deep_sleep_label)
    val deepSleepWindow = deepSleep?.let {
        stringResource(R.string.battery_last_window, formatter.formatDuration(it.duration, true))
    }

    val timeText = time?.let {
        stringResource(
            if (isCharging) R.string.battery_full_in else R.string.battery_lasts_about,
            formatter.formatDuration(it)
        )
    }
    val verdict = listOf(currentText.ifEmpty { if (isLow) stringResource(R.string.low) else "" }, timeText ?: "")
        .filter { it.isNotEmpty() }
        .joinToString(" · ")

    val servicesTitle = stringResource(R.string.app_name)
    val servicesFooter = stringResource(R.string.battery_tip_disable_services)
    val systemTitle = stringResource(R.string.system)
    val systemFooter = stringResource(R.string.battery_tip_system)

    TrailSensePage(
        title = stringResource(R.string.tool_battery_title),
        onBack = rememberTrailSenseBack(),
        backLabel = stringResource(R.string.back),
        modifier = modifier.testTag("battery_title"),
        actions = {
            if (readings.size >= 2) {
                IconButton(
                    onClick = onShowHistory,
                    modifier = Modifier.testTag("battery_history_btn")
                ) {
                    Icon(
                        painter = rememberVectorPainter(TrailIcons.Chart),
                        contentDescription = stringResource(
                            R.string.battery_usage,
                            stringResource(R.string.app_name)
                        )
                    )
                }
            }
            if (canOpenSystemBatterySettings) {
                IconButton(
                    onClick = onOpenBatterySettings,
                    modifier = Modifier.testTag("battery_settings_btn")
                ) {
                    Icon(
                        painter = rememberVectorPainter(TrailIcons.Settings),
                        contentDescription = stringResource(R.string.settings)
                    )
                }
            }
        }
    ) {
        item(key = "hero") {
            val status = stringResource(
                when {
                    isCharging -> R.string.battery_charging
                    isLow -> R.string.battery_low
                    else -> R.string.battery_on_battery
                }
            )
            val progress by animateFloatAsState(
                targetValue = (percent / 100f).coerceIn(0f, 1f),
                animationSpec = MaterialTheme.motionScheme.slowSpatialSpec(),
                label = "batteryProgress"
            )
            CalmKeyValue(
                value = formatter.formatPercentage(percent),
                valueTag = "battery_percentage",
                verdict = verdict.ifEmpty { status },
                verdictTag = "battery_current"
            ) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = TrailSenseSpacing.md)
                        .height(12.dp)
                        .testTag("battery_level_progress"),
                    color = if (isLow && !isCharging) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    gapSize = 4.dp,
                    drawStopIndicator = {}
                )
            }
        }

        trailSenseSection(key = "details") {
            if (capacity != 0f) {
                row { position ->
                    DetailRow(
                        label = stringResource(R.string.battery_capacity_label),
                        value = formatter.formatElectricalCapacity(capacity),
                        position = position
                    )
                }
            }
            if (deepSleep != null) {
                row { position ->
                    DetailRow(
                        label = deepSleepLabel,
                        value = formatter.formatPercentage(deepSleep.percent),
                        supportingText = deepSleepWindow,
                        position = position,
                        modifier = Modifier.testTag("battery_deep_sleep")
                    )
                }
            }
            if (health != BatteryHealth.Good) {
                row { position ->
                    DetailRow(
                        label = stringResource(R.string.battery_health_label),
                        value = formatter.formatBatteryHealth(health),
                        position = position
                    )
                }
            }
        }

        trailSenseSection(
            key = "running_services",
            title = servicesTitle,
            footer = servicesFooter
        ) {
            services.forEach { service ->
                row { position ->
                    RunningServiceRow(
                        service = service,
                        formatter = formatter,
                        position = position,
                        onDisable = { onDisableService(service) }
                    )
                }
            }
        }

        trailSenseSection(
            key = "system_tips",
            title = systemTitle,
            footer = systemFooter
        ) {
            row { position ->
                TrailSenseSwitchRow(
                    label = stringResource(R.string.pref_low_power_mode_title),
                    checked = isLowPowerEnabled,
                    onCheckedChange = { onToggleLowPower() },
                    position = position,
                    modifier = Modifier.testTag("low_power_mode_switch")
                )
            }
            tips.forEach { tip ->
                row { position ->
                    SystemTipRow(
                        tip = tip,
                        position = position,
                        onOpen = { onOpenTip(tip) }
                    )
                }
            }
        }
    }
}

/** A quiet detail: its label on the left and its value on the right. */
@Composable
private fun DetailRow(
    label: String,
    value: String,
    position: TrailSenseGroupPosition,
    modifier: Modifier = Modifier,
    supportingText: String? = null
) {
    TrailSenseListItem(
        title = label,
        subtitle = supportingText,
        position = position,
        modifier = modifier,
        trailing = {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                fontWeight = FontWeight.SemiBold
            )
        }
    )
}

/** A running service: its cost in the subtitle, and a clear Stop button instead of a bare cross. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RunningServiceRow(
    service: RunningService,
    formatter: FormatService,
    position: TrailSenseGroupPosition,
    onDisable: () -> Unit
) {
    val usage = getBatteryUsage(service)
    UsageRow(
        title = service.name,
        subtitle = formatter.join(
            formatFrequency(service.frequency, formatter),
            formatBatteryUsage(usage),
            separator = FormatService.Separator.Dot
        ),
        usage = usage,
        position = position,
        onClick = null
    ) {
        OutlinedButton(
            onClick = onDisable,
            modifier = Modifier
                .heightIn(min = TrailSenseSize.minimumTouchTarget)
                .testTag("battery_service_stop_btn"),
            contentPadding = PaddingValues(horizontal = TrailSenseSpacing.lg)
        ) {
            Text(stringResource(R.string.stop))
        }
    }
}

@Composable
private fun SystemTipRow(
    tip: SystemBatteryTip,
    position: TrailSenseGroupPosition,
    onOpen: () -> Unit
) {
    UsageRow(
        title = tip.name,
        subtitle = tip.description,
        usage = tip.batteryUsage,
        calm = true,
        position = position,
        onClick = if (tip.manage != null) onOpen else null
    ) {
        if (tip.manage != null) {
            Icon(
                painter = rememberVectorPainter(TrailIcons.KeyboardArrowRight),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun UsageRow(
    title: String,
    subtitle: String,
    usage: BatteryUsage,
    position: TrailSenseGroupPosition,
    onClick: (() -> Unit)?,
    calm: Boolean = false,
    trailing: @Composable () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val body: @Composable () -> Unit = {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp)
                .padding(horizontal = TrailSenseSpacing.lg, vertical = TrailSenseSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TrailSenseShapeBadge(
                icon = getUsageIcon(usage),
                shape = MaterialShapes.Cookie9Sided.toShape(),
                size = 44.dp,
                containerColor = scheme.secondaryContainer,
                contentColor = scheme.onSecondaryContainer
            )
            Spacer(Modifier.width(TrailSenseSpacing.lg))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xxs)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(TrailSenseSpacing.sm))
            trailing()
        }
    }
    if (onClick != null) {
        Surface(
            onClick = onClick,
            shape = trailSenseGroupShape(position),
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) { body() }
    } else {
        Surface(
            shape = trailSenseGroupShape(position),
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) { body() }
    }
}

@Composable
private fun formatCurrentText(
    current: Float,
    isCharging: Boolean,
    chargeMethod: BatteryChargingMethod,
    formatter: FormatService
): String {
    return if (current.absoluteValue >= 0.5f) {
        val formattedCurrent = formatter.formatCurrent(current.absoluteValue)
        when {
            current > 500 -> stringResource(R.string.charging_fast, formattedCurrent)
            current > 0 -> stringResource(R.string.charging_slow, formattedCurrent)
            current < -500 -> stringResource(R.string.discharging_fast, formattedCurrent)
            else -> stringResource(R.string.discharging_slow, formattedCurrent)
        }
    } else {
        when {
            isCharging && chargeMethod == BatteryChargingMethod.AC -> stringResource(
                R.string.charging_fast,
                stringResource(R.string.battery_power_ac)
            )
            isCharging && chargeMethod == BatteryChargingMethod.USB -> stringResource(
                R.string.charging_slow,
                stringResource(R.string.battery_power_usb)
            )
            isCharging && chargeMethod == BatteryChargingMethod.Wireless -> stringResource(
                R.string.charging_wireless,
                stringResource(R.string.battery_power_wireless)
            )
            else -> ""
        }
    }
}

@Composable
private fun formatFrequency(frequency: Duration, formatter: FormatService): String {
    return if (frequency == Duration.ZERO) {
        stringResource(R.string.always_on)
    } else {
        stringResource(R.string.service_update_frequency, formatter.formatDuration(frequency))
    }
}

@Composable
private fun formatBatteryUsage(usage: BatteryUsage): String {
    // Short, so the row stays on one line; the badge already carries the colour of the cost.
    val usageString = when (usage) {
        BatteryUsage.Unknown -> stringResource(R.string.unknown)
        BatteryUsage.Low -> stringResource(R.string.low)
        BatteryUsage.Moderate -> stringResource(R.string.moderate)
        BatteryUsage.High -> stringResource(R.string.high)
    }
    return stringResource(R.string.battery_use_short, usageString)
}

private fun getBatteryUsage(service: RunningService): BatteryUsage {
    return when {
        service.frequency < Duration.ofMinutes(15) -> BatteryUsage.High
        service.frequency <= Duration.ofMinutes(25) -> BatteryUsage.Moderate
        else -> BatteryUsage.Low
    }
}

private fun getUsageIcon(usage: BatteryUsage): ImageVector {
    return when (usage) {
        BatteryUsage.Unknown -> TrailIcons.Help
        BatteryUsage.Low -> TrailIcons.Check
        BatteryUsage.Moderate -> TrailIcons.Alert
        BatteryUsage.High -> TrailIcons.Alert
    }
}

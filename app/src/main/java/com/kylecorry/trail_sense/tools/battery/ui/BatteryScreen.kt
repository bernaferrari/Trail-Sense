@file:Suppress("FunctionNaming")
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.kylecorry.trail_sense.tools.battery.ui

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
import androidx.compose.material3.FilledTonalButton
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
    val isCharging = chargingStatus == BatteryChargingStatus.Charging
    val isLow = percent < 25f
    val time = if (isCharging) {
        batteryService.getTimeUntilFull(battery, readings)
    } else {
        batteryService.getTimeUntilEmpty(battery, readings)
    }
    val currentText = formatCurrentText(current, isCharging, chargeMethod, formatter)
    val deepSleep = batteryService.getDeepSleep(readings, Duration.ofHours(6))
    val deepSleepText = deepSleep?.let { sleep ->
        stringResource(
            R.string.battery_deep_sleep,
            formatter.formatPercentage(sleep.percent),
            formatter.formatDuration(sleep.duration, true)
        )
    }

    val metrics = buildList {
        if (time != null) {
            add(
                TrailSenseStat(
                    label = stringResource(
                        if (isCharging) R.string.battery_time_to_full else R.string.battery_time_left
                    ),
                    value = formatter.formatDuration(time),
                    icon = TrailIcons.ToolClock
                )
            )
        }
        if (capacity != 0f) {
            add(
                TrailSenseStat(
                    label = stringResource(R.string.battery_capacity_label),
                    value = formatter.formatElectricalCapacity(capacity),
                    icon = TrailIcons.ToolBattery
                )
            )
        }
        if (health != BatteryHealth.Good) {
            add(
                TrailSenseStat(
                    label = stringResource(R.string.battery_health_label),
                    value = formatter.formatBatteryHealth(health),
                    tone = TrailSenseTone.Caution,
                    icon = TrailIcons.Alert
                )
            )
        }
    }

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
            Column(verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xxs)) {
                BatteryHeroCard(
                    metrics = metrics,
                    percent = percent,
                    isCharging = isCharging,
                    isLow = isLow,
                    statusText = currentText,
                    formatter = formatter,
                    deepSleepText = deepSleepText,
                    position = TrailSenseGroupPosition.Single
                )

            }
        }

        trailSenseSection(key = "low_power") {
            row { position ->
                TrailSenseSwitchRow(
                    label = stringResource(R.string.pref_low_power_mode_title),
                    checked = isLowPowerEnabled,
                    onCheckedChange = { onToggleLowPower() },
                    position = position,
                    modifier = Modifier.testTag("low_power_mode_switch")
                )
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

/**
 * The hero: the charge as a huge tabular percentage on a saturated card that is itself the gauge — a tide of the
 * accent colour fills it from the bottom up to the charge, rolling while it charges. Primary normally, good while
 * charging, error when low.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BatteryHeroCard(
    metrics: List<TrailSenseStat>,
    percent: Float,
    isCharging: Boolean,
    isLow: Boolean,
    statusText: String,
    formatter: FormatService,
    deepSleepText: String?,
    position: TrailSenseGroupPosition
) {
    val tone = when {
        isCharging -> TrailSenseTone.Good
        isLow -> TrailSenseTone.Error
        else -> TrailSenseTone.Primary
    }
    val colors = trailSenseToneColors(tone)
    val scheme = MaterialTheme.colorScheme
    val effects = MaterialTheme.motionScheme.defaultEffectsSpec<Color>()
    val container by animateColorAsState(colors.container, effects, label = "batteryContainer")
    val content by animateColorAsState(colors.content, effects, label = "batteryContent")
    val accent by animateColorAsState(colors.accent, effects, label = "batteryAccent")
    val animatedProgress by animateFloatAsState(
        targetValue = (percent / 100f).coerceIn(0f, 1f),
        animationSpec = MaterialTheme.motionScheme.slowSpatialSpec(),
        label = "batteryProgress"
    )
    val phase by rememberInfiniteTransition(label = "tide").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(if (isCharging) 2200 else 7000, easing = LinearEasing), RepeatMode.Restart),
        label = "tidePhase"
    )
    val supporting = statusText.ifEmpty {
        if (isLow) stringResource(R.string.low) else ""
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = trailSenseGroupShape(position),
        color = container,
        contentColor = content
    ) {
        Box {
            Canvas(
                modifier = Modifier
                    .matchParentSize()
                    .testTag("battery_level_progress")
            ) {
                val level = size.height * (1f - animatedProgress)
                val amplitude = if (animatedProgress >= 0.999f) 0f else if (isCharging) 6.dp.toPx() else 3.dp.toPx()
                val wavelength = size.width / 2f
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(0f, size.height)
                    lineTo(0f, level)
                    var x = 0f
                    while (x <= size.width) {
                        lineTo(x, level + amplitude * kotlin.math.sin((x / wavelength + phase) * 2 * Math.PI).toFloat())
                        x += 6f
                    }
                    lineTo(size.width, size.height)
                    close()
                }
                drawPath(path, accent.copy(alpha = 0.22f))
            }
            Column(
                modifier = Modifier.padding(TrailSenseSpacing.xl),
                verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.sm)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TrailSenseShapeBadge(
                        icon = if (isCharging) TrailIcons.Power else TrailIcons.ToolBattery,
                        shape = if (isCharging) MaterialShapes.SoftBurst.toShape() else MaterialShapes.Cookie9Sided.toShape(),
                        size = 56.dp,
                        containerColor = accent,
                        contentColor = colors.onAccent
                    )
                    Spacer(Modifier.width(TrailSenseSpacing.lg))
                    Text(
                        text = stringResource(
                            when {
                                isCharging -> R.string.battery_charging
                                isLow -> R.string.battery_low
                                else -> R.string.battery_on_battery
                            }
                        ),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    text = formatter.formatPercentage(percent),
                    modifier = Modifier.testTag("battery_percentage"),
                    style = MaterialTheme.typography.displayLarge.copy(fontFeatureSettings = "tnum"),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                if (supporting.isNotEmpty()) {
                    Text(
                        text = supporting,
                        modifier = Modifier.testTag("battery_current"),
                        style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                        color = content.copy(alpha = 0.85f)
                    )
                }
                if (deepSleepText != null) {
                    Text(
                        text = deepSleepText,
                        modifier = Modifier.testTag("battery_deep_sleep"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = content.copy(alpha = 0.75f)
                    )
                }
                // Time, capacity and health ride in the hero as frosted tiles instead of a crowded card under it.
                if (metrics.isNotEmpty()) {
                    Row(
                        modifier = Modifier.padding(top = TrailSenseSpacing.sm),
                        horizontalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xxs)
                    ) {
                        metrics.forEachIndexed { index, stat ->
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = trailSenseGridCellShape(index, metrics.size, 0, 1),
                                color = scheme.surfaceContainerLowest.copy(alpha = 0.6f),
                                contentColor = content
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg, vertical = TrailSenseSpacing.md)
                                ) {
                                    Text(
                                        stat.label,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = content.copy(alpha = 0.78f),
                                        maxLines = 1
                                    )
                                    Text(
                                        stat.value,
                                        style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum"),
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** A running service: its battery cost as a toned shape badge, and a clear Stop button instead of a bare cross. */
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
        FilledTonalButton(
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
    trailing: @Composable () -> Unit
) {
    val colors = trailSenseToneColors(getUsageTone(usage))
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
                shape = getUsageShape(usage).toShape(),
                size = 44.dp,
                containerColor = colors.container,
                contentColor = colors.content
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

private fun getUsageTone(usage: BatteryUsage): TrailSenseTone {
    return when (usage) {
        BatteryUsage.Unknown -> TrailSenseTone.Secondary
        BatteryUsage.Low -> TrailSenseTone.Good
        BatteryUsage.Moderate -> TrailSenseTone.Caution
        BatteryUsage.High -> TrailSenseTone.Error
    }
}

/** Calm shapes for low cost, spikier ones as the cost rises. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
private fun getUsageShape(usage: BatteryUsage): androidx.graphics.shapes.RoundedPolygon {
    return when (usage) {
        BatteryUsage.Unknown -> MaterialShapes.Cookie6Sided
        BatteryUsage.Low -> MaterialShapes.Cookie9Sided
        BatteryUsage.Moderate -> MaterialShapes.SoftBurst
        BatteryUsage.High -> MaterialShapes.Burst
    }
}

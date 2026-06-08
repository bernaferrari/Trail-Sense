@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.battery.ui

import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.kylecorry.andromeda.alerts.Alerts
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
                    chargingStatus == BatteryChargingStatus.Charging
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
    val time = if (isCharging) {
        batteryService.getTimeUntilFull(battery, readings)
    } else {
        batteryService.getTimeUntilEmpty(battery, readings)
    }
    val progressColor = when {
        percent >= 75 -> Color(AppColor.Green.color)
        percent >= 50 -> Color(AppColor.Yellow.color)
        percent >= 25 -> Color(AppColor.Orange.color)
        else -> Color(AppColor.Red.color)
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            BatteryHeader(
                title = time?.let { formatter.formatDuration(it) },
                subtitle = time?.let {
                    stringResource(if (isCharging) R.string.time_until_full else R.string.time_until_empty)
                },
                showHistory = readings.size >= 2,
                canOpenSystemBatterySettings = canOpenSystemBatterySettings,
                onShowHistory = onShowHistory,
                onOpenBatterySettings = onOpenBatterySettings
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("running_services")
            ) {
                item {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = formatter.formatPercentage(percent),
                                modifier = Modifier.testTag("battery_percentage"),
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (capacity != 0f) {
                                Spacer(modifier = Modifier.weight(1f))
                                Text(
                                    text = formatter.formatElectricalCapacity(capacity),
                                    modifier = Modifier.testTag("battery_capacity"),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        LinearProgressIndicator(
                            progress = { percent / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                                .testTag("battery_level_progress"),
                            color = progressColor
                        )
                        val currentText = formatCurrentText(
                            current,
                            isCharging,
                            chargeMethod,
                            formatter
                        )
                        if (currentText.isNotEmpty()) {
                            Text(
                                text = currentText,
                                modifier = Modifier
                                    .padding(top = 8.dp)
                                    .testTag("battery_current"),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (health != BatteryHealth.Good) {
                            Text(
                                text = stringResource(
                                    R.string.battery_health,
                                    formatter.formatBatteryHealth(health)
                                ),
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .testTag("battery_health"),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 24.dp)
                                .clickable { onToggleLowPower() },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.pref_low_power_mode_title),
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Switch(
                                checked = isLowPowerEnabled,
                                onCheckedChange = { onToggleLowPower() },
                                modifier = Modifier.testTag("low_power_mode_switch")
                            )
                        }
                    }
                    HorizontalDivider()
                }

                if (services.isNotEmpty()) {
                    item {
                        BatterySectionHeader(
                            title = stringResource(R.string.app_name),
                            subtitle = stringResource(R.string.battery_tip_disable_services)
                        )
                    }
                    items(services) { service ->
                        RunningServiceRow(
                            service = service,
                            formatter = formatter,
                            onDisable = { onDisableService(service) }
                        )
                        HorizontalDivider()
                    }
                }

                item {
                    BatterySectionHeader(
                        title = stringResource(R.string.system),
                        subtitle = stringResource(R.string.battery_tip_system)
                    )
                }
                items(tips) { tip ->
                    SystemTipRow(
                        tip = tip,
                        onOpen = { onOpenTip(tip) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun BatteryHeader(
    title: String?,
    subtitle: String?,
    showHistory: Boolean,
    canOpenSystemBatterySettings: Boolean,
    onShowHistory: () -> Unit,
    onOpenBatterySettings: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("battery_title"),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showHistory) {
                IconButton(onClick = onShowHistory, modifier = Modifier.testTag("battery_history_btn")) {
                    Icon(
                        painter = painterResource(R.drawable.ic_chart),
                        contentDescription = null
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(48.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title ?: stringResource(R.string.tool_battery_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (canOpenSystemBatterySettings) {
                IconButton(
                    onClick = onOpenBatterySettings,
                    modifier = Modifier.testTag("battery_settings_btn")
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_settings),
                        contentDescription = null
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(48.dp))
            }
        }
    }
}

@Composable
private fun BatterySectionHeader(
    title: String,
    subtitle: String
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    HorizontalDivider()
}

@Composable
private fun RunningServiceRow(
    service: RunningService,
    formatter: FormatService,
    onDisable: () -> Unit
) {
    val usage = getBatteryUsage(service)
    BatteryListRow(
        title = service.name,
        subtitle = formatter.join(
            formatFrequency(service.frequency, formatter),
            formatBatteryUsage(usage),
            separator = FormatService.Separator.Dash
        ),
        icon = getUsageIcon(usage),
        iconTint = Color(getUsageColor(usage)),
        trailing = {
            IconButton(onClick = onDisable, modifier = Modifier.testTag("battery_service_stop_btn")) {
                Icon(
                    painter = painterResource(R.drawable.ic_cancel),
                    contentDescription = null
                )
            }
        }
    )
}

@Composable
private fun SystemTipRow(
    tip: SystemBatteryTip,
    onOpen: () -> Unit
) {
    BatteryListRow(
        title = tip.name,
        subtitle = tip.description,
        icon = getUsageIcon(tip.batteryUsage),
        iconTint = Color(getUsageColor(tip.batteryUsage)),
        trailing = if (tip.manage != null) {
            {
                Icon(
                    painter = painterResource(R.drawable.ic_keyboard_arrow_right),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            null
        },
        onClick = if (tip.manage != null) onOpen else null
    )
}

@Composable
private fun BatteryListRow(
    title: String,
    subtitle: String,
    icon: Int,
    iconTint: Color,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier
                .padding(end = 12.dp)
                .size(24.dp),
            tint = iconTint
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        trailing?.invoke()
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
    val usageString = when (usage) {
        BatteryUsage.Unknown -> stringResource(R.string.unknown)
        BatteryUsage.Low -> stringResource(R.string.low)
        BatteryUsage.Moderate -> stringResource(R.string.moderate)
        BatteryUsage.High -> stringResource(R.string.high)
    }
    return stringResource(R.string.battery_usage, usageString)
}

private fun getBatteryUsage(service: RunningService): BatteryUsage {
    return when {
        service.frequency < Duration.ofMinutes(15) -> BatteryUsage.High
        service.frequency <= Duration.ofMinutes(25) -> BatteryUsage.Moderate
        else -> BatteryUsage.Low
    }
}

private fun getUsageIcon(usage: BatteryUsage): Int {
    return when (usage) {
        BatteryUsage.Unknown -> R.drawable.ic_help
        BatteryUsage.Low -> R.drawable.ic_check_outline
        BatteryUsage.Moderate -> R.drawable.ic_alert
        BatteryUsage.High -> R.drawable.ic_alert
    }
}

private fun getUsageColor(usage: BatteryUsage): Int {
    return when (usage) {
        BatteryUsage.Unknown -> AppColor.Gray.color
        BatteryUsage.Low -> AppColor.Green.color
        BatteryUsage.Moderate -> AppColor.Yellow.color
        BatteryUsage.High -> AppColor.Red.color
    }
}

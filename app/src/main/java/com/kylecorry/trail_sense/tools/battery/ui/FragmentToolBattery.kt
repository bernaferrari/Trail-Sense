package com.kylecorry.trail_sense.tools.battery.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import com.kylecorry.trail_sense.shared.extensions.TrailSenseComposeFragment
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseCard
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseProgressBar
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSwitchRow

@Composable
fun Fragment.BatteryContent(modifier: Modifier = Modifier) {
    var batteryPercent by remember { mutableStateOf(85) }
    var isCharging by remember { mutableStateOf(false) }
    var timeUntilEmpty by remember { mutableStateOf("23h") }

    LaunchedEffect(Unit) {
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        TrailSenseCard {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "$batteryPercent%",
                    style = MaterialTheme.typography.displayLarge
                )
                Spacer(Modifier.height(8.dp))
                TrailSenseProgressBar(progress = batteryPercent / 100f)
                Text(timeUntilEmpty, style = MaterialTheme.typography.labelLarge)
            }
        }

        Spacer(Modifier.height(16.dp))


        val stats = listOf(
            "Charging: ${if (isCharging) "Yes" else "No"}",
            "Health: Good",
            "Temperature: 28°C",
            "Voltage: 4.2V"
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(stats) { stat ->
                TrailSenseCard {
                    Text(
                        stat,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))


        var showOnLock by remember { mutableStateOf(true) }
        TrailSenseSwitchRow(
            label = "Show on lock screen",
            checked = showOnLock,
            onCheckedChange = { showOnLock = it }
        )
    }
}

class FragmentToolBattery : TrailSenseComposeFragment() {

    @Composable
    override fun FragmentContent() {
        BatteryContent()
    }
}
        val isCharging = chargingStatus == BatteryChargingStatus.Charging
        val rawReadings = useLiveData(batteryRepo.get(), emptyList()) { allReadings ->
            allReadings.sortedBy { it.time }
        }
        val readings = useMemo(rawReadings, percent, capacity, isCharging) {
            rawReadings + listOfNotNull(
                if (battery.hasValidReading)
                    BatteryReading(
                        Instant.now(),
                        percent,
                        capacity,
                        isCharging
                    )
                else null
            )
        }
        val (services, triggerServicesUpdate) = useRunningServices(batteryService)
        val tips = useSystemBatteryTips(batteryService)
        val time = useMemo(isCharging, readings, current, percent) {
            if (isCharging) {
                batteryService.getTimeUntilFull(battery, readings)
            } else {
                batteryService.getTimeUntilEmpty(battery, readings)
            }
        }

        // Battery
        // TODO: Extract battery reading to a custom hook
        useEffect(battery, batteryKey) {
            setChargingStatus(battery.chargingStatus)
            val isCharging = battery.chargingStatus == BatteryChargingStatus.Charging

            // If charging and current is negative, invert current
            setCurrent(currentFilter.filter(battery.current.absoluteValue * if (isCharging) 1 else -1))
            setCapacity(battery.capacity)
            setPercent(battery.percent)
            setHealth(battery.health)
            setChargeMethod(battery.chargingMethod)
        }

        useTopic(battery) {
            triggerBatteryUpdate()
        }

        useTimer(INTERVAL_30_FPS) {
            triggerBatteryUpdate()
        }


        // View - Services List
        val onServiceDisable = useCallback<RunningService, Unit> { service: RunningService ->
            runInBackground {
                service.disable()
                triggerServicesUpdate()
            }
        }

        useEffect(servicesListView, context, services, onServiceDisable, tips) {
            val serviceMapper = RunningServiceListItemMapper(context, onServiceDisable)
            val tipMapper = SystemBatteryTipListItemMapper(context)
            val items = mutableListOf<ListItem>()
            if (services.isNotEmpty()) {
                items.add(
                    ListItem(
                        -1,
                        getString(R.string.app_name),
                        getString(R.string.battery_tip_disable_services)
                    )
                )
            }
            items.addAll(services.map { serviceMapper.map(it) })

            items.add(
                ListItem(
                    -2,
                    getString(R.string.system),
                    getString(R.string.battery_tip_system)
                )
            )
            items.addAll(tips.map { tipMapper.map(it) })

            servicesListView.setItems(items)
        }

        // View - Percentage
        useEffect(percentageTextView, progressView, percent) {
            percentageTextView.text = formatter.formatPercentage(percent)
            progressView.progress = percent / 100f
            progressView.trackOpacity = 50
            progressView.progressColor = when {
                percent >= 75 -> AppColor.Green.color
                percent >= 50 -> AppColor.Yellow.color
                percent >= 25 -> AppColor.Orange.color
                else -> AppColor.Red.color
            }
        }

        // View - Capacity
        useEffect(capacityTextView, capacity) {
            capacityTextView.isVisible = capacity != 0f
            capacityTextView.text = formatter.formatElectricalCapacity(capacity)
        }

        // View - Title
        useEffect(titleView, time, isCharging) {
            titleView.title.isVisible = time != null

            if (time != null) {
                titleView.title.text = formatter.formatDuration(time)
            }

            if (time != null && !isCharging) {
                titleView.subtitle.isVisible = true
                titleView.subtitle.text = getString(R.string.time_until_empty)
            } else if (time != null) {
                titleView.subtitle.isVisible = true
                titleView.subtitle.text = getString(R.string.time_until_full)
            } else {
                titleView.subtitle.isVisible = false
            }
        }

        useEffect(titleView, readings) {
            titleView.leftButton.isVisible = readings.size >= 2
            titleView.leftButton.setOnClickListener {
                if (prefs.power.enableBatteryLog) {
                    val readingDuration =
                        Duration.between(readings.firstOrNull()?.time, readings.lastOrNull()?.time)
                    CustomUiUtils.showChart(
                        this,
                        getString(
                            R.string.battery_history,
                            formatter.formatDuration(readingDuration, false)
                        )
                    ) {
                        val chart = BatteryChart(it)
                        chart.plot(readings, false)
                    }
                } else {
                    Alerts.dialog(
                        requireContext(),
                        getString(R.string.pref_tiles_battery_log),
                        getString(R.string.pref_dialog_battery_log_summary),
                        okText = getString(R.string.settings),
                        onClose = {
                            if (it.not()) {
                                navController.navigate(
                                    R.id.action_settings_to_power_settings,
                                )
                            }

                        }
                    )
                }
            }
        }

        useEffect(titleView) {
            CustomUiUtils.setButtonState(titleView.leftButton, false)
            CustomUiUtils.setButtonState(titleView.rightButton, false)

            val settingsIntent = Intent(Intent.ACTION_POWER_USAGE_SUMMARY)
            titleView.rightButton.isVisible = Intents.hasReceiver(context, settingsIntent)
            titleView.rightButton.setOnClickListener {
                startActivity(settingsIntent)
            }
        }

        // View - Health
        useEffect(healthTextView, health) {
            healthTextView.isVisible = health != BatteryHealth.Good
            healthTextView.text =
                getString(R.string.battery_health, formatter.formatBatteryHealth(health))
        }

        // View - Current
        useShowCurrent(
            currentTextView,
            current,
            isCharging,
            chargeMethod,
            formatter
        )

        // View - Low power toggle
        useLowPowerToggle(lowPowerSwitchView, lowPowerMode, prefs, triggerServicesUpdate)
    }

    private fun useRunningServices(batteryService: BatteryService): Pair<List<RunningService>, () -> Unit> {
        val context = useAndroidContext()
        val (servicesKey, triggerServicesUpdate) = useTrigger()
        val (services, setServices) = useState(emptyList<RunningService>())

        useEffect(servicesKey) {
            setServices(batteryService.getRunningServices(context))
        }

        useTimer(1000) {
            triggerServicesUpdate()
        }

        return services to triggerServicesUpdate
    }

    private fun useSystemBatteryTips(batteryService: BatteryService): List<SystemBatteryTip> {
        val context = useAndroidContext()
        val (tipsKey, triggerTipsUpdate) = useTrigger()
        val (tips, setTips) = useState(emptyList<SystemBatteryTip>())

        useEffect(tipsKey, resetOnResume) {
            setTips(batteryService.getSystemBatteryTips(context))
        }

        useTimer(10000) {
            triggerTipsUpdate()
        }

        return tips
    }

    private fun useShowCurrent(
        textView: TextView,
        current: Float,
        isCharging: Boolean,
        chargeMethod: BatteryChargingMethod,
        formatter: FormatService
    ) {
        useEffect(textView, current, isCharging, chargeMethod) {
            if (current.absoluteValue >= 0.5f) {
                val formattedCurrent = formatter.formatCurrent(current.absoluteValue)
                textView.text = when {
                    current > 500 -> getString(R.string.charging_fast, formattedCurrent)
                    current > 0 -> getString(R.string.charging_slow, formattedCurrent)
                    current < -500 -> getString(R.string.discharging_fast, formattedCurrent)
                    else -> getString(R.string.discharging_slow, formattedCurrent)
                }
            } else {
                textView.text = when {
                    isCharging && chargeMethod == BatteryChargingMethod.AC -> getString(
                        R.string.charging_fast,
                        getString(R.string.battery_power_ac)
                    )

                    isCharging && chargeMethod == BatteryChargingMethod.USB -> getString(
                        R.string.charging_slow,
                        getString(R.string.battery_power_usb)
                    )

                    isCharging && chargeMethod == BatteryChargingMethod.Wireless -> getString(
                        R.string.charging_wireless,
                        getString(R.string.battery_power_wireless)
                    )

                    else -> ""
                }
            }
        }
    }

    private fun useLowPowerToggle(
        switch: SwitchCompat,
        lowPowerMode: LowPowerMode,
        prefs: UserPreferences,
        onChange: () -> Unit
    ) {
        useEffect(switch, lowPowerMode, prefs, onChange) {
            switch.isChecked = lowPowerMode.isEnabled()

            switch.setOnClickListener {
                if (lowPowerMode.isEnabled()) {
                    prefs.power.userEnabledLowPower = false
                    lowPowerMode.disable()
                } else {
                    prefs.power.userEnabledLowPower = true
                    lowPowerMode.enable()
                }
                onChange()
            }
        }
    }

}

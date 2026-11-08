package com.kylecorry.andromeda.device

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.ksensor.core.context.KSensorContext
import com.kylecorry.andromeda.battery.BatteryChargingMethod

internal actual fun platformBatteryDetails(percent: Int?): BatteryDetails {
    val context = KSensorContext.get()
    val manager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
    val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    fun property(key: Int): Int? = manager?.getIntProperty(key)?.takeUnless { it == Int.MIN_VALUE }
    val charge = property(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)?.takeIf { it > 0 }?.div(1000f)
    val current = property(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)?.div(1000f)
    val voltage = intent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)?.takeIf { it > 0 }?.div(1000f)
    val method = when (intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)) {
        BatteryManager.BATTERY_PLUGGED_AC -> BatteryChargingMethod.AC
        BatteryManager.BATTERY_PLUGGED_USB -> BatteryChargingMethod.USB
        BatteryManager.BATTERY_PLUGGED_WIRELESS -> BatteryChargingMethod.Wireless
        0 -> BatteryChargingMethod.NotCharging
        else -> BatteryChargingMethod.Unknown
    }
    val full = if (charge != null && percent != null && percent > 0) charge * 100f / percent else null
    return BatteryDetails(voltage, current, charge, full, method)
}

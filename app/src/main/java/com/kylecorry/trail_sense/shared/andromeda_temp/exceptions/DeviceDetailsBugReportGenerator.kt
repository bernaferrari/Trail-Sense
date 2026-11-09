package com.kylecorry.trail_sense.shared.andromeda_temp.exceptions

import android.content.Context
import com.kylecorry.trail_sense.shared.andromeda_temp.core.system.Android

class DeviceDetailsBugReportGenerator : IBugReportGenerator {
    override fun generate(context: Context, throwable: Throwable): String {
        val device = "${Android.fullDeviceName} (${Android.model})"
        return "Device: $device"
    }
}
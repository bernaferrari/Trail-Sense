package com.kylecorry.trail_sense.shared.device

import android.content.Context
import com.kylecorry.trail_sense.shared.andromeda_temp.core.system.Device
class DeviceSubsystem(private val context: Context) {

    fun getAvailableBitmapMemoryBytes(): Long {
        return Device.getAvailableBitmapMemoryBytes(context)
    }

}

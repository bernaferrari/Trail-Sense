package com.kylecorry.andromeda.core.system

import platform.UIKit.UIDevice

actual fun deviceInfo(): DeviceInfo {
    val device = UIDevice.currentDevice
    return DeviceInfo(device.systemName, device.systemVersion, device.model, "Apple")
}

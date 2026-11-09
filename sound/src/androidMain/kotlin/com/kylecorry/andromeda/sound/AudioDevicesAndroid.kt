package com.kylecorry.andromeda.sound

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import com.ksensor.core.context.KSensorContext

/** Requires KSensor's application context to be initialized. */
public actual fun audioDevices(): List<AudioDevice> {
    val manager = KSensorContext.get().getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return emptyList()
    return manager.getDevices(AudioManager.GET_DEVICES_INPUTS or AudioManager.GET_DEVICES_OUTPUTS).map { device ->
        val kind = when (device.type) {
            AudioDeviceInfo.TYPE_BUILTIN_EARPIECE, AudioDeviceInfo.TYPE_BUILTIN_SPEAKER,
            AudioDeviceInfo.TYPE_BUILTIN_MIC -> AudioDeviceKind.BuiltIn
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_WIRED_HEADSET,
            AudioDeviceInfo.TYPE_LINE_ANALOG, AudioDeviceInfo.TYPE_LINE_DIGITAL -> AudioDeviceKind.Wired
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
            AudioDeviceInfo.TYPE_BLE_HEADSET, AudioDeviceInfo.TYPE_BLE_SPEAKER -> AudioDeviceKind.Bluetooth
            AudioDeviceInfo.TYPE_USB_DEVICE, AudioDeviceInfo.TYPE_USB_HEADSET,
            AudioDeviceInfo.TYPE_USB_ACCESSORY -> AudioDeviceKind.Usb
            else -> AudioDeviceKind.Other
        }
        AudioDevice(device.id.toString(), device.productName.toString(), kind, device.isSource, device.isSink)
    }
}

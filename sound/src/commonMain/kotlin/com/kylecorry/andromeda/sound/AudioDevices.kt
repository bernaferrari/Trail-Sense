package com.kylecorry.andromeda.sound

enum class AudioDeviceKind { BuiltIn, Wired, Bluetooth, Usb, Other }

data class AudioDevice(
    val id: String,
    val name: String,
    val kind: AudioDeviceKind,
    val input: Boolean,
    val output: Boolean,
)

/**
 * Lists audio endpoints. iOS exposes the current route; Android and JVM enumerate available endpoints.
 * Android requires KSensor's application context to be initialized.
 */
expect fun audioDevices(): List<AudioDevice>

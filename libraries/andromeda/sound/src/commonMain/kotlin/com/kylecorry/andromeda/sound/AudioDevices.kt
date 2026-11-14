package com.kylecorry.andromeda.sound

public enum class AudioDeviceKind { BuiltIn, Wired, Bluetooth, Usb, Other }

public data class AudioDevice(
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
public expect fun audioDevices(): List<AudioDevice>

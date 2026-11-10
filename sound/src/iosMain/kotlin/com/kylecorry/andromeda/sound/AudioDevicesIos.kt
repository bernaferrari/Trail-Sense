package com.kylecorry.andromeda.sound

import platform.AVFAudio.*

public actual fun audioDevices(): List<AudioDevice> {
    val route = AVAudioSession.sharedInstance().currentRoute
    fun devices(ports: List<*>, input: Boolean) = ports.filterIsInstance<AVAudioSessionPortDescription>().map { port ->
        val kind = when (port.portType) {
            AVAudioSessionPortBluetoothA2DP, AVAudioSessionPortBluetoothHFP, AVAudioSessionPortBluetoothLE -> AudioDeviceKind.Bluetooth
            AVAudioSessionPortUSBAudio -> AudioDeviceKind.Usb
            AVAudioSessionPortHeadphones, AVAudioSessionPortHeadsetMic, AVAudioSessionPortLineIn, AVAudioSessionPortLineOut -> AudioDeviceKind.Wired
            AVAudioSessionPortBuiltInMic, AVAudioSessionPortBuiltInSpeaker, AVAudioSessionPortBuiltInReceiver -> AudioDeviceKind.BuiltIn
            else -> AudioDeviceKind.Other
        }
        AudioDevice(port.UID, port.portName, kind, input, !input)
    }
    return devices(route.inputs, true) + devices(route.outputs, false)
}

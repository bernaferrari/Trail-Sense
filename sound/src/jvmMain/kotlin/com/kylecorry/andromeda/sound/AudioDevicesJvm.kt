package com.kylecorry.andromeda.sound

import javax.sound.sampled.AudioSystem

actual fun audioDevices(): List<AudioDevice> = AudioSystem.getMixerInfo().map { info ->
    val mixer = AudioSystem.getMixer(info)
    AudioDevice("${info.name}:${info.vendor}:${info.version}", info.name, AudioDeviceKind.Other,
        mixer.targetLineInfo.isNotEmpty(), mixer.sourceLineInfo.isNotEmpty())
}

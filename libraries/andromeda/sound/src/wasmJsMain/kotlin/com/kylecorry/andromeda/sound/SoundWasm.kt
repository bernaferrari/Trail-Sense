package com.kylecorry.andromeda.sound

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

public actual fun audioDevices(): List<AudioDevice> = emptyList()

public actual fun defaultSystemSoundUri(sound: DefaultSystemSound): String? = null

public actual fun openSystemSound(uri: String, channel: SoundChannel): SystemSound? = null

public actual fun microphoneFrames(sampleRate: Int, processing: MicrophoneProcessing): Flow<MicrophoneFrame> = flow {
    requireDefaultMicrophoneProcessing(processing)
    require(sampleRate > 0)
    throw UnsupportedOperationException("Microphone capture is unavailable on this platform")
}

public actual class PcmSpeaker actual constructor(sampleRate: Int) : AutoCloseable {
    private var closed = false

    init {
        require(sampleRate > 0)
    }

    public actual suspend fun write(samples: ShortArray) {
        check(!closed) { "PCM output is closed" }
    }

    public actual suspend fun drain() {
        check(!closed) { "PCM output is closed" }
    }

    actual override fun close() {
        closed = true
    }
}

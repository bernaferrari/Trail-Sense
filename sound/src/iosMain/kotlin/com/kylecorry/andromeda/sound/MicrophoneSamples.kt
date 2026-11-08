package com.kylecorry.andromeda.sound

import dev.piotrprus.audiocapture.AudioCapture
import dev.piotrprus.audiocapture.CaptureConfig
import dev.piotrprus.audiocapture.stream
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

actual fun microphoneSamples(sampleRate: Int): Flow<FloatArray> {
    require(sampleRate > 0)
    return AudioCapture().stream(CaptureConfig(sampleRate = sampleRate)).map { it.toFloatArray() }
}

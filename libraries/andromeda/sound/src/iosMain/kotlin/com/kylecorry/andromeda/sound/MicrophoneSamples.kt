package com.kylecorry.andromeda.sound

import dev.piotrprus.audiocapture.AudioCapture
import dev.piotrprus.audiocapture.CaptureConfig
import dev.piotrprus.audiocapture.stream
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

public actual fun microphoneFrames(sampleRate: Int, processing: MicrophoneProcessing): Flow<MicrophoneFrame> {
    requireDefaultMicrophoneProcessing(processing)
    require(sampleRate > 0)
    return AudioCapture().stream(CaptureConfig(sampleRate = sampleRate)).map { MicrophoneFrame(it.toFloatArray(), null) }
}

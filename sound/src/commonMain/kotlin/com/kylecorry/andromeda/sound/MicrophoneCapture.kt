package com.kylecorry.andromeda.sound

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Null leaves a processing setting under platform control. */
data class MicrophoneProcessing(
    val noiseSuppression: Boolean? = null,
    val echoCancellation: Boolean? = null,
    val automaticGainControl: Boolean? = null,
)

/** Available calibration for the active microphone, in dB SPL and dBFS/Pa. */
data class MicrophoneCalibration(
    val minimumSpl: Float?,
    val maximumSpl: Float?,
    val sensitivity: Float?,
)

data class MicrophoneFrame(val samples: FloatArray, val calibration: MicrophoneCalibration?)

/**
 * Streams normalized mono PCM and any available device calibration. Microphone permission is
 * required. Processing requests are best effort on Android; other platforms currently reject them.
 */
expect fun microphoneFrames(
    sampleRate: Int = 16000,
    processing: MicrophoneProcessing = MicrophoneProcessing(),
): Flow<MicrophoneFrame>

/** Streams normalized mono PCM while collected. */
fun microphoneSamples(
    sampleRate: Int = 16000,
    processing: MicrophoneProcessing = MicrophoneProcessing(),
): Flow<FloatArray> = microphoneFrames(sampleRate, processing).map { it.samples }

internal fun requireDefaultMicrophoneProcessing(processing: MicrophoneProcessing) {
    require(processing == MicrophoneProcessing()) { "Microphone processing options are unavailable on this platform" }
}

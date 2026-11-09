package com.kylecorry.andromeda.sound

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.MicrophoneInfo
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AudioEffect
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive

@SuppressLint("MissingPermission")
actual fun microphoneFrames(sampleRate: Int, processing: MicrophoneProcessing): Flow<MicrophoneFrame> = flow {
    require(sampleRate > 0)
    val minimum = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
    check(minimum > 0) { "Unsupported microphone sample rate" }
    val recorder = AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, maxOf(minimum, sampleRate / 5 * 2))
    val effects = mutableListOf<AudioEffect>()
    try {
        check(recorder.state == AudioRecord.STATE_INITIALIZED) { "Could not initialize microphone" }
        fun configure(requested: Boolean?, create: () -> AudioEffect?) {
            if (requested != null) create()?.let { effect ->
                effects.add(effect)
                effect.enabled = requested
            }
        }
        configure(processing.noiseSuppression) { if (NoiseSuppressor.isAvailable()) NoiseSuppressor.create(recorder.audioSessionId) else null }
        configure(processing.echoCancellation) { if (AcousticEchoCanceler.isAvailable()) AcousticEchoCanceler.create(recorder.audioSessionId) else null }
        configure(processing.automaticGainControl) { if (AutomaticGainControl.isAvailable()) AutomaticGainControl.create(recorder.audioSessionId) else null }
        recorder.startRecording()
        val calibration = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) runCatching {
            recorder.activeMicrophones.firstOrNull()?.let { microphone ->
                MicrophoneCalibration(
                    microphone.minSpl.takeUnless { it == MicrophoneInfo.SPL_UNKNOWN },
                    microphone.maxSpl.takeUnless { it == MicrophoneInfo.SPL_UNKNOWN },
                    microphone.sensitivity.takeUnless { it == MicrophoneInfo.SENSITIVITY_UNKNOWN },
                )
            }
        }.getOrNull() else null
        val buffer = ShortArray(maxOf(1, sampleRate / 10))
        while (currentCoroutineContext().isActive) {
            val count = recorder.read(buffer, 0, buffer.size)
            check(count >= 0) { "Microphone read failed: $count" }
            if (count > 0) emit(MicrophoneFrame(FloatArray(count) { buffer[it] / 32768f }, calibration))
        }
    } finally {
        effects.forEach { runCatching { it.release() } }
        try {
            if (recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING) recorder.stop()
        } finally {
            recorder.release()
        }
    }
}.flowOn(Dispatchers.IO)

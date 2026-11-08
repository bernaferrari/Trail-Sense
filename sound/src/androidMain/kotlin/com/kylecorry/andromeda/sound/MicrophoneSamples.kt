package com.kylecorry.andromeda.sound

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive

@SuppressLint("MissingPermission")
actual fun microphoneSamples(sampleRate: Int): Flow<FloatArray> = flow {
    require(sampleRate > 0)
    val minimum = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
    check(minimum > 0) { "Unsupported microphone sample rate" }
    val recorder = AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, maxOf(minimum, sampleRate / 5 * 2))
    try {
        check(recorder.state == AudioRecord.STATE_INITIALIZED) { "Could not initialize microphone" }
        recorder.startRecording()
        val buffer = ShortArray(maxOf(1, sampleRate / 10))
        while (currentCoroutineContext().isActive) {
            val count = recorder.read(buffer, 0, buffer.size)
            check(count >= 0) { "Microphone read failed: $count" }
            if (count > 0) emit(FloatArray(count) { buffer[it] / 32768f })
        }
    } finally {
        if (recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING) recorder.stop()
        recorder.release()
    }
}.flowOn(Dispatchers.IO)

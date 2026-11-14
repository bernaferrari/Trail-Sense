package com.kylecorry.andromeda.sound

import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.TargetDataLine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive

public actual fun microphoneFrames(sampleRate: Int, processing: MicrophoneProcessing): Flow<MicrophoneFrame> = flow {
    requireDefaultMicrophoneProcessing(processing)
    require(sampleRate > 0)
    val format = AudioFormat(sampleRate.toFloat(), 16, 1, true, false)
    val line = AudioSystem.getLine(DataLine.Info(TargetDataLine::class.java, format)) as TargetDataLine
    try {
        line.open(format)
        line.start()
        val buffer = ByteArray(maxOf(2, sampleRate / 10 * 2))
        while (currentCoroutineContext().isActive) {
            val count = line.read(buffer, 0, buffer.size)
            if (count > 0) emit(MicrophoneFrame(pcm16ToFloat(buffer.copyOf(count - count % 2)), null))
        }
    } finally {
        line.stop()
        line.close()
    }
}.flowOn(Dispatchers.IO)

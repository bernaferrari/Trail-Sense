package com.kylecorry.andromeda.sound.stream

interface AudioStream {
    suspend fun next(sampleRate: Int): Float
    suspend fun reset()
}

interface SeekableAudioStream : AudioStream {
    suspend fun peek(time: Double, sampleRate: Int): Float
}

suspend fun AudioStream.nextBuffer(sampleRate: Int, buffer: ShortArray) {
    for (i in buffer.indices) {
        val sample = next(sampleRate).coerceIn(-1f, 1f)
        buffer[i] = (sample * Short.MAX_VALUE).toInt().toShort()
    }
}


package com.kylecorry.andromeda.sound.stream

public interface AudioStream {
    public suspend fun next(sampleRate: Int): Float
    public suspend fun reset()
}

public interface SeekableAudioStream : AudioStream {
    public suspend fun peek(time: Double, sampleRate: Int): Float
}

public suspend fun AudioStream.nextBuffer(sampleRate: Int, buffer: ShortArray) {
    for (i in buffer.indices) {
        val sample = next(sampleRate).coerceIn(-1f, 1f)
        buffer[i] = (sample * Short.MAX_VALUE).toInt().toShort()
    }
}


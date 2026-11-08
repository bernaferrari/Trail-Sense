package com.kylecorry.andromeda.sound

import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.SourceDataLine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

actual class PcmSpeaker actual constructor(sampleRate: Int) : AutoCloseable {
    private val mutex = Mutex()
    private val line: SourceDataLine
    init {
        require(sampleRate > 0)
        val format = AudioFormat(sampleRate.toFloat(), 16, 1, true, false)
        line = AudioSystem.getSourceDataLine(format)
        line.open(format)
        line.start()
    }

    actual suspend fun write(samples: ShortArray) = mutex.withLock {
        check(line.isOpen) { "PCM output is closed" }
        require(samples.size <= Int.MAX_VALUE / 2)
        val bytes = ByteArray(samples.size * 2)
        samples.forEachIndexed { index, sample ->
            bytes[2 * index] = sample.toByte()
            bytes[2 * index + 1] = (sample.toInt() shr 8).toByte()
        }
        withContext(Dispatchers.IO) {
            var offset = 0
            while (offset < bytes.size) {
                val count = line.write(bytes, offset, bytes.size - offset)
                check(count > 0) { "PCM output is closed" }
                offset += count
            }
        }
    }

    actual suspend fun drain() = mutex.withLock {
        check(line.isOpen) { "PCM output is closed" }
        withContext(Dispatchers.IO) { line.drain() }
    }

    actual override fun close() {
        line.close()
    }
}

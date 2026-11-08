package com.kylecorry.andromeda.sound

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

actual class PcmSpeaker actual constructor(sampleRate: Int) : AutoCloseable {
    private val mutex = Mutex()
    private val closed = AtomicBoolean(false)
    private var framesWritten = 0L
    private val track: AudioTrack

    init {
        require(sampleRate > 0)
        val size = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        require(size > 0) { "Unsupported PCM output format" }
        track = AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
            .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(sampleRate).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
            .setBufferSizeInBytes(size)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        check(track.state == AudioTrack.STATE_INITIALIZED) { track.release(); "Unable to initialize PCM output" }
        track.play()
    }

    actual suspend fun write(samples: ShortArray) = mutex.withLock {
        check(!closed.get()) { "PCM output is closed" }
        withContext(Dispatchers.IO) {
            var offset = 0
            while (offset < samples.size) {
                val count = track.write(samples, offset, samples.size - offset, AudioTrack.WRITE_BLOCKING)
                check(count > 0) { "PCM output failed: $count" }
                offset += count
                framesWritten += count
            }
        }
    }

    actual suspend fun drain() = mutex.withLock {
        check(!closed.get()) { "PCM output is closed" }
        while (true) {
            check(!closed.get()) { "PCM output is closed" }
            if ((track.playbackHeadPosition.toLong() and 0xffffffffL) == (framesWritten and 0xffffffffL)) break
            delay(5)
        }
    }

    actual override fun close() {
        if (closed.compareAndSet(false, true)) track.release()
    }
}

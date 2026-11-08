@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.kylecorry.andromeda.sound

import kotlinx.cinterop.get
import kotlinx.cinterop.set
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import platform.AVFAudio.AVAudioEngine
import platform.AVFAudio.AVAudioFormat
import platform.AVFAudio.AVAudioPCMBuffer
import platform.AVFAudio.AVAudioPlayerNode
import platform.AVFAudio.AVAudioPlayerNodeCompletionDataConsumed
import platform.AVFAudio.AVAudioPlayerNodeCompletionDataPlayedBack
import kotlin.coroutines.resume

actual class PcmSpeaker actual constructor(sampleRate: Int) : AutoCloseable {
    private val mutex = Mutex()
    private val engine = AVAudioEngine()
    private val player = AVAudioPlayerNode()
    private val format: AVAudioFormat
    private val closed = atomic(false)

    init {
        require(sampleRate > 0)
        format = requireNotNull(AVAudioFormat(standardFormatWithSampleRate = sampleRate.toDouble(), channels = 1u))
        engine.attachNode(player)
        engine.connect(player, engine.mainMixerNode, format)
        check(engine.startAndReturnError(null)) { "Unable to start PCM output" }
        player.play()
    }

    actual suspend fun write(samples: ShortArray) = mutex.withLock {
        check(!closed.value) { "PCM output is closed" }
        if (samples.isEmpty()) return@withLock
        val buffer = requireNotNull(AVAudioPCMBuffer(pCMFormat = format, frameCapacity = samples.size.toUInt()))
        buffer.frameLength = samples.size.toUInt()
        val channel = requireNotNull(buffer.floatChannelData)[0]!!
        samples.forEachIndexed { index, sample -> channel[index] = sample.toFloat() / 32768f }
        suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation { if (!closed.value) player.stop() }
            player.scheduleBuffer(buffer, completionCallbackType = AVAudioPlayerNodeCompletionDataConsumed) {
                if (closed.value) continuation.cancel(CancellationException("PCM output is closed"))
                else if (continuation.isActive) continuation.resume(Unit)
            }
            player.play()
        }
    }

    actual suspend fun drain() = mutex.withLock {
        check(!closed.value) { "PCM output is closed" }
        // A silent frame acts as a playback fence after all previously scheduled buffers.
        val fence = requireNotNull(AVAudioPCMBuffer(pCMFormat = format, frameCapacity = 1u))
        fence.frameLength = 1u
        requireNotNull(fence.floatChannelData)[0]!![0] = 0f
        suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation { if (!closed.value) player.stop() }
            player.scheduleBuffer(fence, completionCallbackType = AVAudioPlayerNodeCompletionDataPlayedBack) {
                if (closed.value) continuation.cancel(CancellationException("PCM output is closed"))
                else if (continuation.isActive) continuation.resume(Unit)
            }
            player.play()
        }
    }

    actual override fun close() {
        if (closed.compareAndSet(expect = false, update = true)) {
            player.stop()
            engine.stop()
        }
    }
}

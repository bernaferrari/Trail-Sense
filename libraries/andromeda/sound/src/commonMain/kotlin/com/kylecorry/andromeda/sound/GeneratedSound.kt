package com.kylecorry.andromeda.sound

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlin.math.PI
import kotlin.math.sin
import kotlin.time.Duration
import kotlin.time.DurationUnit

/**
 * Plays generated mono samples until [duration] elapses or the coroutine is cancelled.
 * Samples use the range -1 to 1; values outside that range are clipped. Use
 * [Duration.INFINITE] for continuous playback, and cancel the coroutine to stop it.
 */
public suspend fun playGeneratedSound(
    sampleRate: Int = 44100,
    duration: Duration = Duration.INFINITE,
    sample: (index: Long) -> Double,
) {
    require(sampleRate > 0)
    require(!duration.isNegative())
    val sampleCount = if (duration.isInfinite()) Long.MAX_VALUE else {
        val count = duration.toDouble(DurationUnit.SECONDS) * sampleRate
        require(count < Long.MAX_VALUE.toDouble()) { "Sound duration is too long" }
        count.toLong()
    }
    if (sampleCount == 0L) return

    val speaker = PcmSpeaker(sampleRate)
    try {
        var position = 0L
        val chunkSize = (sampleRate / 20).coerceAtLeast(1)
        while (position < sampleCount) {
            currentCoroutineContext().ensureActive()
            val size = minOf(chunkSize.toLong(), sampleCount - position).toInt()
            val samples = ShortArray(size) { offset ->
                val value = sample(position + offset)
                require(value.isFinite()) { "Generated samples must be finite" }
                (value.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
            }
            speaker.write(samples)
            position += size
        }
        speaker.drain()
    } finally {
        speaker.close()
    }
}

/** Plays a sine tone, continuously by default. */
public suspend fun playTone(
    frequencyHz: Double,
    sampleRate: Int = 44100,
    duration: Duration = Duration.INFINITE,
    volume: Float = 1f,
) {
    require(frequencyHz.isFinite() && frequencyHz >= 0)
    require(volume.isFinite() && volume in 0f..1f)
    require(sampleRate > 0)
    val step = 2.0 * PI * (frequencyHz % sampleRate) / sampleRate
    var phase = 0.0
    playGeneratedSound(sampleRate, duration) {
        val value = sin(phase) * volume
        phase = (phase + step) % (2.0 * PI)
        value
    }
}

package com.kylecorry.andromeda.sound

import kotlin.math.PI
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin

/** Converts little-endian signed 16-bit PCM to normalized floating-point samples. */
fun pcm16ToFloat(bytes: ByteArray): FloatArray {
    require(bytes.size % 2 == 0)
    return FloatArray(bytes.size / 2) { index ->
        val value = (bytes[index * 2].toInt() and 0xff) or (bytes[index * 2 + 1].toInt() shl 8)
        value.toShort().toFloat() / 32768f
    }
}

/** Generates little-endian signed 16-bit PCM from normalized sample values. */
fun generatePcm16(sampleRate: Int, durationSeconds: Float, sample: (Int) -> Double): ByteArray {
    require(sampleRate > 0 && durationSeconds >= 0 && durationSeconds.isFinite())
    val sampleCount = sampleRate.toDouble() * durationSeconds
    require(sampleCount <= Int.MAX_VALUE / 2)
    val count = sampleCount.toInt()
    return ByteArray(count * 2).also { bytes ->
        repeat(count) { index ->
            val value = (sample(index).coerceIn(-1.0, 1.0) * 32767).toInt()
            bytes[index * 2] = value.toByte()
            bytes[index * 2 + 1] = (value shr 8).toByte()
        }
    }
}

fun generateTone(frequency: Float, sampleRate: Int = 64000, durationSeconds: Float = 1f): ByteArray {
    require(frequency >= 0 && frequency.isFinite())
    return generatePcm16(sampleRate, durationSeconds) { index ->
        sin(frequency * 2 * PI * index / sampleRate)
    }
}

fun amplitudeToDecibels(
    amplitude: Float,
    decibelRange: ClosedFloatingPointRange<Float>,
    sensitivity: Float = 0f,
    referenceSoundPressure: Float = 0.00002f,
): Float {
    require(referenceSoundPressure > 0)
    val minimum = (decibelRange.start + sensitivity).coerceAtLeast(0f)
    val maximum = (decibelRange.endInclusive + sensitivity).coerceAtLeast(0f)
    val low = referenceSoundPressure * 10f.pow(minimum / 20f)
    val high = referenceSoundPressure * 10f.pow(maximum / 20f)
    return 20f * log10((low + amplitude * (high - low)) / referenceSoundPressure)
}

/** Wraps little-endian PCM16 samples in a WAV container. */
fun pcm16Wav(pcm: ByteArray, sampleRate: Int, channels: Int = 1): ByteArray {
    require(sampleRate > 0 && channels in 1..32767 && pcm.size % (channels * 2) == 0)
    require(pcm.size <= Int.MAX_VALUE - 44 && sampleRate.toLong() * channels * 2 <= Int.MAX_VALUE)
    val result = ByteArray(44 + pcm.size)
    fun text(offset: Int, value: String) = value.forEachIndexed { index, char -> result[offset + index] = char.code.toByte() }
    fun write16(offset: Int, value: Int) {
        result[offset] = value.toByte()
        result[offset + 1] = (value ushr 8).toByte()
    }
    fun write32(offset: Int, value: Int) {
        repeat(4) { result[offset + it] = (value ushr (it * 8)).toByte() }
    }
    text(0, "RIFF")
    write32(4, 36 + pcm.size)
    text(8, "WAVE")
    text(12, "fmt ")
    write32(16, 16)
    write16(20, 1)
    write16(22, channels)
    write32(24, sampleRate)
    write32(28, sampleRate * channels * 2)
    write16(32, channels * 2)
    write16(34, 16)
    text(36, "data")
    write32(40, pcm.size)
    pcm.copyInto(result, 44)
    return result
}

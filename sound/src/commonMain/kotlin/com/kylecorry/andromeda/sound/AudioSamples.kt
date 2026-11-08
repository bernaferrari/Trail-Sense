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

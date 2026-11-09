package com.kylecorry.andromeda.tensorflow

import com.kylecorry.andromeda.bitmaps.PixelImage

import kotlin.math.exp
import kotlin.math.roundToInt

/** Packs row-major RGB channels as little-endian float32, applying `(channel - mean) / standardDeviation`. */
public fun PixelImage.rgbFloat32(mean: Float = 0f, standardDeviation: Float = 1f): ByteArray {
    require(mean.isFinite() && standardDeviation.isFinite() && standardDeviation > 0f)
    require(pixels.size <= Int.MAX_VALUE / 12)
    val output = ByteArray(pixels.size * 12)
    var offset = 0
    for (pixel in pixels) for (channel in 0..2) {
        val shift = 16 - channel * 8
        val value = (((pixel ushr shift) and 0xff) - mean) / standardDeviation
        val bits = value.toBits()
        repeat(4) { byte -> output[offset++] = (bits ushr (byte * 8)).toByte() }
    }
    return output
}

/** Packs row-major RGB channels as unsigned bytes. */
public fun PixelImage.rgbUint8(): ByteArray {
    require(pixels.size <= Int.MAX_VALUE / 3)
    val output = ByteArray(pixels.size * 3)
    var offset = 0
    for (pixel in pixels) {
        output[offset++] = (pixel ushr 16).toByte()
        output[offset++] = (pixel ushr 8).toByte()
        output[offset++] = pixel.toByte()
    }
    return output
}

/** Packs normalized RGB values using the tensor's quantization scale and zero point. */
public fun PixelImage.rgbQuantized(
    scale: Float,
    zeroPoint: Int,
    signed: Boolean,
    mean: Float = 0f,
    standardDeviation: Float = 1f,
): ByteArray {
    require(scale.isFinite() && scale > 0f && mean.isFinite() && standardDeviation.isFinite() && standardDeviation > 0f)
    val range = if (signed) -128..127 else 0..255
    require(zeroPoint in range && pixels.size <= Int.MAX_VALUE / 3)
    val output = ByteArray(pixels.size * 3)
    var offset = 0
    for (pixel in pixels) for (channel in 0..2) {
        val shift = 16 - channel * 8
        val real = (((pixel ushr shift) and 0xff) - mean) / standardDeviation
        val quantized = (real / scale + zeroPoint).coerceIn(range.first.toFloat(), range.last.toFloat()).roundToInt()
        output[offset++] = quantized.toByte()
    }
    return output
}

/** Reads little-endian float32 output tensor values. */
public fun decodeFloat32Tensor(bytes: ByteArray): List<Float> {
    require(bytes.size % 4 == 0)
    return List(bytes.size / 4) { index ->
        val offset = index * 4
        val bits = (bytes[offset].toInt() and 0xff) or ((bytes[offset + 1].toInt() and 0xff) shl 8) or
            ((bytes[offset + 2].toInt() and 0xff) shl 16) or (bytes[offset + 3].toInt() shl 24)
        Float.fromBits(bits)
    }
}

/** Dequantizes signed or unsigned 8-bit output values. */
public fun decodeQuantizedTensor(bytes: ByteArray, scale: Float, zeroPoint: Int, signed: Boolean): List<Float> {
    require(scale.isFinite() && scale > 0f && zeroPoint in if (signed) -128..127 else 0..255)
    return bytes.map { ((if (signed) it.toInt() else it.toInt() and 0xff) - zeroPoint) * scale }
}

/** Converts finite logits to probabilities without overflowing for large inputs. */
public fun List<Float>.softmax(): List<Float> {
    if (isEmpty()) return emptyList()
    require(all(Float::isFinite))
    val maximum = max()
    val values = map { exp((it - maximum).toDouble()) }
    val sum = values.sum()
    return values.map { (it / sum).toFloat() }
}

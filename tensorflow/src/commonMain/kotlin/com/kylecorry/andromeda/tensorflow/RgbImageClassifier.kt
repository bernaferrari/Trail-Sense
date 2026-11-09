package com.kylecorry.andromeda.tensorflow

import com.kylecorry.andromeda.bitmaps.ImageInterpolation

/** RGB encoding and normalization expected by a model's input tensor. */
public sealed interface RgbTensorEncoding {
    public data class Float32(val mean: Float = 0f, val standardDeviation: Float = 1f) : RgbTensorEncoding {
        init { require(mean.isFinite() && standardDeviation.isFinite() && standardDeviation > 0f) }
    }
    public data object Uint8 : RgbTensorEncoding
    public data class Quantized(
        val scale: Float,
        val zeroPoint: Int,
        val signed: Boolean,
        val mean: Float = 0f,
        val standardDeviation: Float = 1f,
    ) : RgbTensorEncoding {
        init {
            require(scale.isFinite() && scale > 0f && mean.isFinite() && standardDeviation.isFinite() && standardDeviation > 0f)
            require(zeroPoint in if (signed) -128..127 else 0..255)
        }
    }
}

/** Output tensor encoding and, for quantized output, its quantization parameters. */
public sealed interface ClassifierOutputEncoding {
    public data object Float32 : ClassifierOutputEncoding
    public data class Quantized(val scale: Float, val zeroPoint: Int, val signed: Boolean) : ClassifierOutputEncoding {
        init { require(scale.isFinite() && scale > 0f && zeroPoint in if (signed) -128..127 else 0..255) }
    }
}

public data class RgbModelInput(
    val width: Int,
    val height: Int,
    val encoding: RgbTensorEncoding,
    val resizeMode: ImageResizeMode = ImageResizeMode.Stretch,
    val interpolation: ImageInterpolation = ImageInterpolation.Bilinear,
    val background: Int = 0xff000000.toInt(),
) {
    init { require(width > 0 && height > 0 && width.toLong() * height <= Int.MAX_VALUE) }
}

/** Loads a single-input RGB classifier with built-in image preparation and output decoding. */
public fun rgbImageClassifier(
    modelPath: String,
    input: RgbModelInput,
    output: ClassifierOutputEncoding,
    options: ClassifierOptions = ClassifierOptions(),
): ImageClassifier = imageClassifier(
    modelPath = modelPath,
    prepareInput = { image ->
        val resized = if (image.width == input.width && image.height == input.height) image
        else image.resizeForModel(input.width, input.height, input.resizeMode, input.interpolation, input.background)
        when (val encoding = input.encoding) {
            is RgbTensorEncoding.Float32 -> resized.rgbFloat32(encoding.mean, encoding.standardDeviation)
            RgbTensorEncoding.Uint8 -> resized.rgbUint8()
            is RgbTensorEncoding.Quantized -> resized.rgbQuantized(
                encoding.scale, encoding.zeroPoint, encoding.signed, encoding.mean, encoding.standardDeviation,
            )
        }
    },
    decodeOutput = { bytes ->
        when (output) {
            ClassifierOutputEncoding.Float32 -> decodeFloat32Tensor(bytes)
            is ClassifierOutputEncoding.Quantized -> decodeQuantizedTensor(bytes, output.scale, output.zeroPoint, output.signed)
        }
    },
    options = options,
)

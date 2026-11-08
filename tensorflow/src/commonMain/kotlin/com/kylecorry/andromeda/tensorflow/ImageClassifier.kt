package com.kylecorry.andromeda.tensorflow

class ImagePixels(val width: Int, val height: Int, val argb: IntArray) {
    init { require(width > 0 && height > 0 && argb.size.toLong() == width.toLong() * height) }
}

interface ImageClassifier : AutoCloseable {
    suspend fun classify(image: ImagePixels): List<Float>
}

/** Loads a fixed-size, single-input, single-output model from a filesystem path. */
expect fun imageClassifier(
    modelPath: String,
    prepareInput: (ImagePixels) -> ByteArray,
    decodeOutput: (ByteArray) -> List<Float>,
): ImageClassifier

package com.kylecorry.andromeda.tensorflow

import com.kylecorry.andromeda.bitmaps.PixelImage

interface ImageClassifier : AutoCloseable {
    suspend fun classify(image: PixelImage): List<Float>
}

/** Loads a fixed-size, single-input, single-output model from a filesystem path. */
expect fun imageClassifier(
    modelPath: String,
    prepareInput: (PixelImage) -> ByteArray,
    decodeOutput: (ByteArray) -> List<Float>,
): ImageClassifier

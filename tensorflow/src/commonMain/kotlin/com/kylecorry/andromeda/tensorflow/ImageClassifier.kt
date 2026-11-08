package com.kylecorry.andromeda.tensorflow

import com.kylecorry.andromeda.bitmaps.PixelImage

data class ClassifierOptions(val numThreads: Int = 4, val applySoftmax: Boolean = false) {
    init { require(numThreads > 0) }
}

interface ImageClassifier : AutoCloseable {
    suspend fun classify(image: PixelImage): List<Float>
}

/** Loads a fixed-size, single-input, single-output model from a filesystem path. */
expect fun imageClassifier(
    modelPath: String,
    prepareInput: (PixelImage) -> ByteArray,
    decodeOutput: (ByteArray) -> List<Float>,
    options: ClassifierOptions = ClassifierOptions(),
): ImageClassifier

package com.kylecorry.andromeda.tensorflow

import android.graphics.Bitmap

fun imageClassifier(classifier: TensorflowImageClassifier): ImageClassifier = object : ImageClassifier {
    override suspend fun classify(image: ImagePixels): List<Float> {
        val bitmap = Bitmap.createBitmap(image.argb, image.width, image.height, Bitmap.Config.ARGB_8888)
        return try {
            classifier.classify(bitmap)
        } finally {
            bitmap.recycle()
        }
    }
    override fun close() = classifier.close()
}

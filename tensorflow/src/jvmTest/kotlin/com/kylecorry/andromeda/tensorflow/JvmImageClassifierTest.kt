package com.kylecorry.andromeda.tensorflow

import com.kylecorry.andromeda.bitmaps.PixelImage

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class JvmImageClassifierTest {
    @Test
    fun runsModelAndClosesInterpreter() = runBlocking {
        // TensorFlow's Apache-2.0 test model: tensorflow/lite/testdata/add.bin (v2.21.0).
        val path = javaClass.getResource("/add.tflite")!!.toURI().path
        val classifier = imageClassifier(path, {
            ByteBuffer.allocate(768).order(ByteOrder.nativeOrder()).putFloat(1f).putFloat(3f).array()
        }, { bytes ->
            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.nativeOrder())
            List(bytes.size / 4) { buffer.float }
        }, ClassifierOptions(numThreads = 2))
        val image = PixelImage(1, 1, intArrayOf(0))
        assertEquals(listOf(3f, 9f), classifier.classify(image).take(2))
        classifier.close()
        classifier.close()
        assertFailsWith<IllegalStateException> { classifier.classify(image) }
        Unit
    }

    @Test
    fun runsRgbClassifierWithBuiltInTensorConversion() = runBlocking {
        val path = javaClass.getResource("/add.tflite")!!.toURI().path
        val classifier = rgbImageClassifier(
            path,
            RgbModelInput(64, 1, RgbTensorEncoding.Float32()),
            ClassifierOutputEncoding.Float32,
            ClassifierOptions(numThreads = 2),
        )
        try {
            val pixels = IntArray(64)
            pixels[0] = 0xff010300.toInt()
            assertEquals(listOf(3f, 9f), classifier.classify(PixelImage(64, 1, pixels)).take(2))
        } finally { classifier.close() }
    }

}

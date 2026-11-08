package com.kylecorry.andromeda.tensorflow

import com.kylecorry.andromeda.bitmaps.PixelImage

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

actual fun imageClassifier(
    modelPath: String,
    prepareInput: (PixelImage) -> ByteArray,
    decodeOutput: (ByteArray) -> List<Float>,
    options: ClassifierOptions,
): ImageClassifier {
    val model = FileInputStream(modelPath).use { stream ->
        stream.channel.map(FileChannel.MapMode.READ_ONLY, 0, stream.channel.size())
    }
    val interpreter = Interpreter(model, Interpreter.Options().setNumThreads(options.numThreads))
    try {
        require(interpreter.inputTensorCount == 1 && interpreter.outputTensorCount == 1) { "Expected a model with one input and one output" }
        val inputSize = interpreter.getInputTensor(0).numBytes()
        val outputSize = interpreter.getOutputTensor(0).numBytes()
        require(inputSize > 0 && outputSize > 0) { "Variable-sized tensors are not supported" }
        val lock = Any()
        var closed = false
        return object : ImageClassifier {
            override suspend fun classify(image: PixelImage): List<Float> = withContext(Dispatchers.Default) {
                val bytes = prepareInput(image)
                require(bytes.size == inputSize) { "Expected $inputSize input bytes, got ${bytes.size}" }
                val input = ByteBuffer.allocateDirect(inputSize).order(ByteOrder.nativeOrder()).put(bytes).apply { rewind() }
                val output = ByteBuffer.allocateDirect(outputSize).order(ByteOrder.nativeOrder())
                synchronized(lock) {
                    check(!closed) { "Classifier is closed" }
                    interpreter.run(input, output)
                    check(interpreter.getOutputTensor(0).numBytes() == outputSize) { "Variable-sized output tensors are not supported" }
                }
                output.rewind()
                val result = ByteArray(outputSize)
                output.get(result)
                decodeOutput(result).let { if (options.applySoftmax) it.softmax() else it }
            }
            override fun close() = synchronized(lock) {
                if (!closed) {
                    closed = true
                    interpreter.close()
                }
            }
        }
    } catch (error: Throwable) {
        interpreter.close()
        throw error
    }
}

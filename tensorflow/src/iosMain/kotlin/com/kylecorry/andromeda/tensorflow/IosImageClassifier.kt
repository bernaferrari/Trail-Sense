package com.kylecorry.andromeda.tensorflow

import dev.kursor.ktensorflow.Interpreter
import dev.kursor.ktensorflow.InterpreterOptions
import dev.kursor.ktensorflow.ModelDesc
import dev.kursor.ktensorflow.run
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Loads a single-input, single-output TensorFlow Lite model from a filesystem path.
 * The app must link TensorFlow Lite using the `dev.kursor.ktensorflow.link` version 2.0
 * Gradle plugin and CocoaPods.
 * [prepareInput] and [decodeOutput] must match the model's tensor types and quantization.
 */
fun imageClassifier(
    modelPath: String,
    prepareInput: (ImagePixels) -> ByteArray,
    decodeOutput: (ByteArray) -> List<Float>,
): ImageClassifier {
    val interpreter = Interpreter(ModelDesc.PathInBundle(modelPath), InterpreterOptions())
    try {
        val metadata = interpreter.getModelMeta()
        require(metadata.inputData.size == 1 && metadata.outputData.size == 1) { "Expected a model with one input and one output" }
        val inputSize = metadata.inputData.single().totalByteSize
        val outputSize = metadata.outputData.single().totalByteSize
        require(inputSize > 0 && outputSize > 0) { "Variable-sized tensors are not supported" }
        return object : ImageClassifier {
            override suspend fun classify(image: ImagePixels): List<Float> = withContext(Dispatchers.Default) {
                val input = prepareInput(image)
                require(input.size == inputSize) { "Expected $inputSize input bytes, got ${input.size}" }
                val output = ByteArray(outputSize)
                interpreter.run(input, output)
                decodeOutput(output)
            }
            override fun close() = interpreter.close()
        }
    } catch (error: Throwable) {
        interpreter.close()
        throw error
    }
}

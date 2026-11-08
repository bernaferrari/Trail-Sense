package com.kylecorry.andromeda.tensorflow

import com.kylecorry.andromeda.bitmaps.PixelImage

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bytedeco.javacpp.BytePointer
import org.bytedeco.tensorflowlite.global.tensorflowlite.*

/** Loads a fixed-size, single-input, single-output TensorFlow Lite model from a file. */
actual fun imageClassifier(
    modelPath: String,
    prepareInput: (PixelImage) -> ByteArray,
    decodeOutput: (ByteArray) -> List<Float>,
    options: ClassifierOptions,
): ImageClassifier {
    val model = TfLiteModelCreateFromFile(modelPath)
    require(model != null && !model.isNull) { "Unable to load model: $modelPath" }
    val interpreterOptions = TfLiteInterpreterOptionsCreate()
    if (interpreterOptions == null || interpreterOptions.isNull) {
        TfLiteModelDelete(model)
        error("Unable to create TensorFlow Lite options")
    }
    val interpreter = try {
        TfLiteInterpreterOptionsSetNumThreads(interpreterOptions, options.numThreads)
        TfLiteInterpreterCreate(model, interpreterOptions)
    } finally {
        TfLiteInterpreterOptionsDelete(interpreterOptions)
        TfLiteModelDelete(model)
    }
    require(interpreter != null && !interpreter.isNull) { "Unable to create TensorFlow Lite interpreter" }
    try {
        check(TfLiteInterpreterAllocateTensors(interpreter) == 0) { "Unable to allocate tensors" }
        require(TfLiteInterpreterGetInputTensorCount(interpreter) == 1 && TfLiteInterpreterGetOutputTensorCount(interpreter) == 1) {
            "Expected a model with one input and one output"
        }
        val input = TfLiteInterpreterGetInputTensor(interpreter, 0)
        val output = TfLiteInterpreterGetOutputTensor(interpreter, 0)
        check(input != null && !input.isNull && output != null && !output.isNull) { "Unable to access tensors" }
        val inputSize = TfLiteTensorByteSize(input)
        val outputSize = TfLiteTensorByteSize(output)
        require(inputSize in 1..Int.MAX_VALUE.toLong() && outputSize in 1..Int.MAX_VALUE.toLong()) { "Unsupported tensor size" }
        val lock = Any()
        var closed = false
        return object : ImageClassifier {
            override suspend fun classify(image: PixelImage): List<Float> = withContext(Dispatchers.Default) {
                val bytes = prepareInput(image)
                require(bytes.size.toLong() == inputSize) { "Expected $inputSize input bytes, got ${bytes.size}" }
                val result = ByteArray(outputSize.toInt())
                synchronized(lock) {
                    check(!closed) { "Classifier is closed" }
                    BytePointer(*bytes).use { source ->
                        check(TfLiteTensorCopyFromBuffer(input, source, inputSize) == 0) { "Unable to copy input tensor" }
                    }
                    check(TfLiteInterpreterInvoke(interpreter) == 0) { "TensorFlow Lite inference failed" }
                    val currentOutput = TfLiteInterpreterGetOutputTensor(interpreter, 0)
                    check(currentOutput != null && !currentOutput.isNull && TfLiteTensorByteSize(currentOutput) == outputSize) {
                        "Variable-sized output tensors are not supported"
                    }
                    BytePointer(outputSize).use { destination ->
                        check(TfLiteTensorCopyToBuffer(currentOutput, destination, outputSize) == 0) { "Unable to copy output tensor" }
                        destination.get(result)
                    }
                }
                decodeOutput(result).let { if (options.applySoftmax) it.softmax() else it }
            }
            override fun close() = synchronized(lock) {
                if (!closed) {
                    closed = true
                    TfLiteInterpreterDelete(interpreter)
                }
            }
        }
    } catch (error: Throwable) {
        TfLiteInterpreterDelete(interpreter)
        throw error
    }
}

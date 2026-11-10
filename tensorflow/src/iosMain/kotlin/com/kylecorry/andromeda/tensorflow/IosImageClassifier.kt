@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.kylecorry.andromeda.tensorflow

import com.kylecorry.andromeda.bitmaps.PixelImage

import com.kylecorry.andromeda.tensorflow.litert.*
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Foundation.NSLock

/**
 * Loads a single-input, single-output `.tflite` model from a filesystem path.
 * Link the TensorFlowLite product from Google's LiteRT Swift package into the iOS app;
 * Kotlin frameworks consuming this module must be static so the app can link the C runtime.
 * The model file must remain unchanged until the classifier is closed.
 * [prepareInput] and [decodeOutput] must match the model's tensor types and quantization.
 */
public actual fun imageClassifier(
    modelPath: String,
    prepareInput: (PixelImage) -> ByteArray,
    decodeOutput: (ByteArray) -> List<Float>,
    options: ClassifierOptions,
): ImageClassifier {
    require(options.acceleration == ClassifierAcceleration.Cpu) { "NNAPI is only available on Android" }
    val model = requireNotNull(TfLiteModelCreateFromFile(modelPath)) { "Unable to load model: $modelPath" }
    val interpreterOptions = TfLiteInterpreterOptionsCreate()
    if (interpreterOptions == null) {
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
    requireNotNull(interpreter) { "Unable to create TensorFlow Lite interpreter" }
    try {
        check(TfLiteInterpreterAllocateTensors(interpreter) == 0) { "Unable to allocate tensors" }
        require(TfLiteInterpreterGetInputTensorCount(interpreter) == 1 && TfLiteInterpreterGetOutputTensorCount(interpreter) == 1) {
            "Expected a model with one input and one output"
        }
        val input = requireNotNull(TfLiteInterpreterGetInputTensor(interpreter, 0))
        val output = requireNotNull(TfLiteInterpreterGetOutputTensor(interpreter, 0))
        val inputSize = TfLiteTensorByteSize(input)
        val outputSize = TfLiteTensorByteSize(output)
        require(inputSize > 0u && inputSize <= Int.MAX_VALUE.toULong() && outputSize > 0u && outputSize <= Int.MAX_VALUE.toULong()) {
            "Unsupported tensor size"
        }
        val lock = NSLock()
        var closed = false
        return object : ImageClassifier {
            override suspend fun classify(image: PixelImage): List<Float> = withContext(Dispatchers.Default) {
                val bytes = prepareInput(image)
                require(bytes.size.toULong() == inputSize) { "Expected $inputSize input bytes, got ${bytes.size}" }
                val result = ByteArray(outputSize.toInt())
                lock.lock()
                try {
                    check(!closed) { "Classifier is closed" }
                    check(bytes.usePinned { TfLiteTensorCopyFromBuffer(input, it.addressOf(0), inputSize) } == 0) { "Unable to copy input tensor" }
                    check(TfLiteInterpreterInvoke(interpreter) == 0) { "TensorFlow Lite inference failed" }
                    val currentOutput = requireNotNull(TfLiteInterpreterGetOutputTensor(interpreter, 0))
                    check(TfLiteTensorByteSize(currentOutput) == outputSize) { "Variable-sized output tensors are not supported" }
                    check(result.usePinned { TfLiteTensorCopyToBuffer(currentOutput, it.addressOf(0), outputSize) } == 0) { "Unable to copy output tensor" }
                } finally {
                    lock.unlock()
                }
                decodeOutput(result).let { if (options.applySoftmax) it.softmax() else it }
            }
            override fun close() {
                lock.lock()
                try {
                    if (!closed) {
                        closed = true
                        TfLiteInterpreterDelete(interpreter)
                    }
                } finally {
                    lock.unlock()
                }
            }
        }
    } catch (error: Throwable) {
        TfLiteInterpreterDelete(interpreter)
        throw error
    }
}

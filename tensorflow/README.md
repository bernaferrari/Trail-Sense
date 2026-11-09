# TensorFlow image classification

`imageClassifier` is available from common Kotlin on Android, iOS and JVM. It reads a model from a filesystem path; supply callbacks that encode the input and decode the output in the format expected by that model. `ClassifierOptions` controls inference threads, optional softmax, and Android NNAPI acceleration. Other platforms reject the NNAPI option. Close the classifier when finished. The model file must not change while the classifier is open.

On JVM, the module includes TensorFlow Lite and native runtimes for macOS (Apple Silicon and Intel), Linux (ARM64 and x86-64), and Windows x86-64.

On iOS, add Google's [LiteRT Swift package](https://github.com/google-ai-edge/LiteRT) to the app and link its `TensorFlowLite` product (for example, release `2.3.0`). Configure the Kotlin framework as static so its TensorFlow Lite C references are resolved when Xcode links the app:

```kotlin
listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
    target.binaries.framework { isStatic = true }
}
```

The app must also link the C++ standard library and Accelerate framework.

The C API bindings target LiteRT 2.3.0. Only models with one fixed-size input tensor and one fixed-size output tensor are supported by this image-classification adapter.

For common RGB models, `PixelImage.rgbFloat32(mean, standardDeviation)`, `rgbUint8()`, and `rgbQuantized(...)` create input tensors. `decodeFloat32Tensor` and `decodeQuantizedTensor` decode outputs, and `softmax()` converts logits to probabilities. Choose normalization and quantization parameters from the model; these helpers do not resize or reorder images.

`PixelImage.resizeForModel(width, height, mode)` prepares model dimensions using stretching, aspect-preserving padding, or center cropping. It uses the shared bitmap module and supports nearest, bilinear, and bicubic interpolation.

For a standard single-input RGB model, `rgbImageClassifier(modelPath, RgbModelInput(...), outputEncoding)` combines resizing, normalization, tensor conversion, inference, and output decoding. Set the tensor formats and quantization values to match the model; use `imageClassifier` when its input or output needs custom handling.

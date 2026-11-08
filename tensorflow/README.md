# TensorFlow image classification

On iOS, add Google's [LiteRT Swift package](https://github.com/google-ai-edge/LiteRT) to the app and link its `TensorFlowLite` product (for example, release `2.3.0`). Configure the Kotlin framework as static so its TensorFlow Lite C references are resolved when Xcode links the app:

```kotlin
listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
    target.binaries.framework { isStatic = true }
}
```

The app must also link the C++ standard library and Accelerate framework. The classifier takes a path to a `.tflite` file and callbacks that encode the input and decode the output in the format expected by that model. The file must not change while the classifier is open. Close the classifier when finished.

The C API bindings target LiteRT 2.3.0. Only models with one fixed-size input tensor and one fixed-size output tensor are supported by this image-classification adapter.

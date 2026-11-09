package com.kylecorry.andromeda.torch

import com.ksensor.core.context.KSensorContext

actual fun deviceTorch(): Torch = AndroidTorch(KSensorContext.get())

/**
 * Checks if the device has a camera flashlight.
 * @param onlyCheckSystemFeature true to only check the system feature, which is faster than inspecting the cameras.
 */
fun isTorchAvailable(context: android.content.Context, onlyCheckSystemFeature: Boolean = false): Boolean =
    AndroidTorch.isAvailable(context, onlyCheckSystemFeature)

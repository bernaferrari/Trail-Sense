package com.kylecorry.andromeda.torch

import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFoundation.*

/** Camera torch on iOS. Brightness is continuous and exposed as 100 levels. */
@OptIn(ExperimentalForeignApi::class)
internal class IosTorch : Torch {
    private val device get() = AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo)
    override val brightnessLevels: Int get() = if (isAvailable()) 100 else 0
    override fun isAvailable(): Boolean = device?.hasTorch == true && device?.torchAvailable == true
    override fun isDimmable(): Boolean = isAvailable()
    override fun on() = on(1f)
    override fun on(brightness: Float) {
        if (brightness <= 0f) return off()
        val camera = device ?: return
        if (!camera.hasTorch || !camera.torchAvailable || !camera.lockForConfiguration(null)) return
        try {
            camera.setTorchModeOnWithLevel(brightness.coerceIn(0.01f, 1f), null)
        } finally {
            camera.unlockForConfiguration()
        }
    }
    override fun off() {
        val camera = device ?: return
        if (!camera.hasTorch || !camera.lockForConfiguration(null)) return
        try {
            camera.torchMode = AVCaptureTorchModeOff
        } finally {
            camera.unlockForConfiguration()
        }
    }
}

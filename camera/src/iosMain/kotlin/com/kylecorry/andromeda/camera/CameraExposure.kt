@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.kylecorry.andromeda.camera

import com.kashif.cameraK.controller.CameraController
import com.kashif.cameraK.enums.CameraDeviceType
import platform.AVFoundation.*

internal fun selectedCameraDevice(camera: CameraController): AVCaptureDevice? {
    val lens = camera.getCameraLens() ?: return null
    val requested = camera.getPreferredCameraDeviceType()
    val type = if (requested == CameraDeviceType.DEFAULT) CameraDeviceType.WIDE_ANGLE else requested
    val matches = camera.getCameraCapabilities().allLenses.filter {
        it.lens == lens && (type == CameraDeviceType.MACRO || it.deviceType == type)
    }
    return matches.singleOrNull()?.id?.let(AVCaptureDevice::deviceWithUniqueID)
}

internal actual fun cameraExposure(camera: CameraController): CameraExposure? {
    val device = selectedCameraDevice(camera) ?: return null
    return CameraExposure(device.exposureTargetBias, device.minExposureTargetBias, device.maxExposureTargetBias)
}

internal actual fun setCameraExposure(camera: CameraController, value: Float) {
    val device = checkNotNull(selectedCameraDevice(camera)) { "Exposure compensation is unavailable" }
    require(value.isFinite() && value in device.minExposureTargetBias..device.maxExposureTargetBias)
    check(device.lockForConfiguration(null)) { "Unable to configure camera exposure" }
    try {
        device.setExposureTargetBias(value, null)
    } finally {
        device.unlockForConfiguration()
    }
}

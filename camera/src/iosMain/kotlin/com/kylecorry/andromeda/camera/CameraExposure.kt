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

internal actual fun cameraManualFocusSupported(camera: CameraController): Boolean =
    selectedCameraDevice(camera)?.lockingFocusWithCustomLensPositionSupported == true

internal actual fun setCameraFocus(camera: CameraController, position: Float?) {
    val device = checkNotNull(selectedCameraDevice(camera)) { "Focus control is unavailable" }
    if (position != null) {
        require(position.isFinite() && position in 0f..1f)
        check(device.lockingFocusWithCustomLensPositionSupported) { "Manual focus is unavailable" }
    }
    val autoMode = if (position == null) when {
        device.isFocusModeSupported(AVCaptureFocusModeContinuousAutoFocus) -> AVCaptureFocusModeContinuousAutoFocus
        device.isFocusModeSupported(AVCaptureFocusModeAutoFocus) -> AVCaptureFocusModeAutoFocus
        else -> error("Autofocus is unavailable")
    } else null
    check(device.lockForConfiguration(null)) { "Unable to configure camera focus" }
    try {
        if (position != null) device.setFocusModeLockedWithLensPosition(position, null)
        else device.focusMode = checkNotNull(autoMode)
    } finally {
        device.unlockForConfiguration()
    }
}

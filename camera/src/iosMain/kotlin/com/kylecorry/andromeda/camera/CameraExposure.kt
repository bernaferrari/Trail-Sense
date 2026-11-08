@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.kylecorry.andromeda.camera

import com.kashif.cameraK.controller.CameraController
import com.kashif.cameraK.enums.CameraDeviceType
import platform.AVFoundation.*
import platform.CoreMedia.CMTimeGetSeconds
import platform.CoreMedia.CMTimeMake
import platform.CoreGraphics.CGPointMake
import kotlinx.cinterop.useContents
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

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

internal actual fun cameraManualExposure(camera: CameraController): ManualExposureRange? {
    val device = selectedCameraDevice(camera) ?: return null
    if (!device.isExposureModeSupported(AVCaptureExposureModeCustom)) return null
    val format = device.activeFormat
    val minimum = CMTimeGetSeconds(format.minExposureDuration)
    val maximum = CMTimeGetSeconds(format.maxExposureDuration)
    val minIso = ceil(format.minISO).toInt()
    val maxIso = floor(format.maxISO).toInt()
    if (!minimum.isFinite() || !maximum.isFinite() || minimum <= 0 || maximum < minimum || minIso > maxIso) return null
    return ManualExposureRange(minimum.seconds..maximum.seconds, minIso..maxIso)
}

internal actual fun setCameraManualExposure(camera: CameraController, duration: Duration?, iso: Int?) {
    require((duration == null) == (iso == null)) { "Provide both duration and ISO, or neither" }
    val device = checkNotNull(selectedCameraDevice(camera)) { "Camera is unavailable" }
    if (duration != null) {
        val range = checkNotNull(cameraManualExposure(camera)) { "Manual exposure is unavailable" }
        require(duration.isFinite() && duration in range.duration && checkNotNull(iso) in range.iso)
    }
    val autoMode = if (duration == null) when {
        device.isExposureModeSupported(AVCaptureExposureModeContinuousAutoExposure) -> AVCaptureExposureModeContinuousAutoExposure
        device.isExposureModeSupported(AVCaptureExposureModeAutoExpose) -> AVCaptureExposureModeAutoExpose
        else -> error("Auto exposure is unavailable")
    } else null
    check(device.lockForConfiguration(null)) { "Unable to configure camera exposure" }
    try {
        if (duration == null) device.exposureMode = checkNotNull(autoMode)
        else device.setExposureModeCustomWithDuration(CMTimeMake(duration.inWholeNanoseconds, 1_000_000_000), checkNotNull(iso).toFloat(), null)
    } finally {
        device.unlockForConfiguration()
    }
}

internal actual fun cameraMeteringSupported(camera: CameraController): Boolean {
    val device = selectedCameraDevice(camera) ?: return false
    return (device.focusPointOfInterestSupported && device.isFocusModeSupported(AVCaptureFocusModeAutoFocus)) ||
        (device.exposurePointOfInterestSupported && device.isExposureModeSupported(AVCaptureExposureModeAutoExpose))
}

internal actual fun focusCameraAt(camera: CameraController, x: Float, y: Float) {
    require(x.isFinite() && x in 0f..1f && y.isFinite() && y in 0f..1f)
    val device = checkNotNull(selectedCameraDevice(camera)) { "Camera is unavailable" }
    check(cameraMeteringSupported(camera)) { "Focus and metering are unavailable" }
    val layer = checkNotNull(camera.getCameraPreviewLayer()) { "Camera preview is unavailable" }
    val point = layer.bounds.useContents {
        check(size.width > 0 && size.height > 0) { "Camera preview is not laid out" }
        layer.captureDevicePointOfInterestForPoint(CGPointMake(size.width * x, size.height * y))
    }
    check(device.lockForConfiguration(null)) { "Unable to configure camera focus" }
    try {
        if (device.focusPointOfInterestSupported && device.isFocusModeSupported(AVCaptureFocusModeAutoFocus)) {
            device.focusPointOfInterest = point
            device.focusMode = AVCaptureFocusModeAutoFocus
        }
        if (device.exposurePointOfInterestSupported && device.isExposureModeSupported(AVCaptureExposureModeAutoExpose)) {
            device.exposurePointOfInterest = point
            device.exposureMode = AVCaptureExposureModeAutoExpose
        }
    } finally {
        device.unlockForConfiguration()
    }
}

internal actual fun resetCameraMetering(camera: CameraController) {
    val device = checkNotNull(selectedCameraDevice(camera)) { "Camera is unavailable" }
    check(device.lockForConfiguration(null)) { "Unable to configure camera focus" }
    try {
        if (device.focusPointOfInterestSupported) device.focusPointOfInterest = CGPointMake(0.5, 0.5)
        if (device.isFocusModeSupported(AVCaptureFocusModeContinuousAutoFocus)) device.focusMode = AVCaptureFocusModeContinuousAutoFocus
        if (device.exposurePointOfInterestSupported) device.exposurePointOfInterest = CGPointMake(0.5, 0.5)
        if (device.isExposureModeSupported(AVCaptureExposureModeContinuousAutoExposure)) device.exposureMode = AVCaptureExposureModeContinuousAutoExposure
    } finally {
        device.unlockForConfiguration()
    }
}

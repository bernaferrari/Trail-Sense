@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.kylecorry.andromeda.camera

import com.kashif.cameraK.controller.CameraController
import com.kashif.cameraK.enums.CameraDeviceType
import com.kashif.cameraK.enums.DeviceOrientation
import com.kylecorry.andromeda.camera.ar.CameraCalibrationSource
import com.kylecorry.andromeda.camera.ar.ImageBounds
import com.kylecorry.andromeda.camera.ar.estimateCameraIntrinsics
import kotlinx.cinterop.useContents
import platform.AVFoundation.AVCaptureDevice
import platform.CoreMedia.CMVideoFormatDescriptionGetDimensions

/** Estimates centered intrinsics from the selected device's active video format. */
internal actual fun cameraCalibrationSource(camera: CameraController): CameraCalibrationSource? = object : CameraCalibrationSource {
    private var selectedKey: Pair<Any?, CameraDeviceType>? = null
    private var selectedDevice: AVCaptureDevice? = null

    private fun device(): AVCaptureDevice? {
        val lens = camera.getCameraLens() ?: return null
        val requested = camera.getPreferredCameraDeviceType()
        val key = lens to requested
        if (key != selectedKey || selectedDevice == null) {
            val effectiveType = if (requested == CameraDeviceType.DEFAULT) CameraDeviceType.WIDE_ANGLE else requested
            val matches = camera.getCameraCapabilities().allLenses.filter {
                it.lens == lens && (effectiveType == CameraDeviceType.MACRO || it.deviceType == effectiveType)
            }
            // Do not guess which physical device CameraK selected when there are multiple matches.
            val id = matches.singleOrNull()?.id
            selectedDevice = id?.let { AVCaptureDevice.deviceWithUniqueID(it) }
            selectedKey = key
        }
        return selectedDevice
    }

    private fun dimensions(device: AVCaptureDevice): Pair<Int, Int> =
        CMVideoFormatDescriptionGetDimensions(device.activeFormat.formatDescription).useContents { width to height }

    override val calibrationId: String?
        get() {
            val current = device() ?: return null
            val (width, height) = dimensions(current)
            return "${current.uniqueID}:$width:$height:${current.activeFormat.videoFieldOfView}:$sensorRotationDegrees"
        }
    // AVFoundation video dimensions are landscape-oriented.
    override val sensorRotationDegrees: Int get() = when (camera.getDeviceOrientation()) {
        DeviceOrientation.PORTRAIT, DeviceOrientation.PORTRAIT_UPSIDE_DOWN -> 90
        DeviceOrientation.LANDSCAPE_LEFT, DeviceOrientation.LANDSCAPE_RIGHT -> 0
    }
    override val zoomRatio: Float get() = camera.getZoom()

    override fun intrinsicCalibration(onlyUseEstimated: Boolean): FloatArray? {
        val current = device() ?: return null
        val (width, height) = dimensions(current)
        return estimateCameraIntrinsics(width, height, current.activeFormat.videoFieldOfView)
    }

    override fun activeArray(preCorrection: Boolean): ImageBounds? {
        val current = device() ?: return null
        val (width, height) = dimensions(current)
        if (width <= 0 || height <= 0) return null
        return ImageBounds(0f, 0f, width.toFloat(), height.toFloat())
    }

    override fun distortionCorrection(): FloatArray? = null
}

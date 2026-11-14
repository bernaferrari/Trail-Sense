@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.kylecorry.andromeda.camera

import com.kashif.cameraK.controller.CameraController
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.get
import kotlinx.cinterop.reinterpret
import kotlinx.coroutines.CoroutineScope
import platform.AVFoundation.AVCaptureConnection
import platform.AVFoundation.AVCaptureOutput
import platform.AVFoundation.AVCaptureVideoDataOutput
import platform.AVFoundation.AVCaptureVideoDataOutputSampleBufferDelegateProtocol
import platform.CoreMedia.CMSampleBufferGetImageBuffer
import platform.CoreMedia.CMSampleBufferRef
import platform.CoreVideo.CVPixelBufferGetBaseAddress
import platform.CoreVideo.CVPixelBufferGetBytesPerRow
import platform.CoreVideo.CVPixelBufferGetHeight
import platform.CoreVideo.CVPixelBufferGetPixelFormatType
import platform.CoreVideo.CVPixelBufferGetWidth
import platform.CoreVideo.CVPixelBufferLockBaseAddress
import platform.CoreVideo.CVPixelBufferUnlockBaseAddress
import platform.CoreVideo.kCVPixelBufferLock_ReadOnly
import platform.CoreVideo.kCVPixelBufferPixelFormatTypeKey
import platform.CoreVideo.kCVPixelFormatType_32BGRA
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_queue_create
import kotlin.concurrent.Volatile

internal actual fun startCameraFrameAnalysis(
    camera: CameraController,
    scope: CoroutineScope,
    onFrame: (width: Int, height: Int, argb: IntArray) -> Unit,
    onError: (Throwable) -> Unit,
): AutoCloseable {
    val delegate = FrameDelegate(onFrame, onError)
    val output = AVCaptureVideoDataOutput().apply {
        videoSettings = mapOf(kCVPixelBufferPixelFormatTypeKey to kCVPixelFormatType_32BGRA)
        setAlwaysDiscardsLateVideoFrames(true)
        setSampleBufferDelegate(delegate, dispatch_queue_create("andromeda.camera.analysis", null))
    }
    camera.safeAddOutput(output)
    return AutoCloseable {
        delegate.stop()
        output.setSampleBufferDelegate(null, null)
        camera.safeRemoveOutput(output)
    }
}

private class FrameDelegate(
    private val onFrame: (Int, Int, IntArray) -> Unit,
    private val onError: (Throwable) -> Unit,
) : NSObject(), AVCaptureVideoDataOutputSampleBufferDelegateProtocol {
    @Volatile private var active = true
    fun stop() { active = false }

    override fun captureOutput(
        output: AVCaptureOutput,
        didOutputSampleBuffer: CMSampleBufferRef?,
        fromConnection: AVCaptureConnection,
    ) {
        if (!active) return
        val buffer = didOutputSampleBuffer?.let(::CMSampleBufferGetImageBuffer) ?: return
        try {
            check(CVPixelBufferGetPixelFormatType(buffer) == kCVPixelFormatType_32BGRA) { "Unsupported camera pixel format" }
            check(CVPixelBufferLockBaseAddress(buffer, kCVPixelBufferLock_ReadOnly) == 0) { "Unable to access camera frame" }
            try {
                val width = CVPixelBufferGetWidth(buffer).toInt()
                val height = CVPixelBufferGetHeight(buffer).toInt()
                val rowBytes = CVPixelBufferGetBytesPerRow(buffer).toInt()
                val source = checkNotNull(CVPixelBufferGetBaseAddress(buffer)).reinterpret<ByteVar>()
                val pixels = IntArray(width * height)
                for (y in 0 until height) for (x in 0 until width) {
                    val offset = y * rowBytes + x * 4
                    val blue = source[offset].toInt() and 0xff
                    val green = source[offset + 1].toInt() and 0xff
                    val red = source[offset + 2].toInt() and 0xff
                    val alpha = source[offset + 3].toInt() and 0xff
                    pixels[y * width + x] = (alpha shl 24) or (red shl 16) or (green shl 8) or blue
                }
                if (active) onFrame(width, height, pixels)
            } finally {
                CVPixelBufferUnlockBaseAddress(buffer, kCVPixelBufferLock_ReadOnly)
            }
        } catch (error: Exception) {
            dispatch_async(dispatch_get_main_queue()) { if (active) onError(error) }
        }
    }
}

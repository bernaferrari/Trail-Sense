package com.kylecorry.trail_sense.tools.qr.infrastructure

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.CoreGraphics.CGColorSpaceCreateDeviceRGB
import platform.CoreGraphics.CGColorSpaceRelease
import platform.CoreGraphics.CGSizeMake
import platform.CoreImage.CIDetector
import platform.CoreImage.CIDetectorAccuracy
import platform.CoreImage.CIDetectorAccuracyHigh
import platform.CoreImage.CIDetectorTypeQRCode
import platform.CoreImage.CIImage
import platform.CoreImage.CIQRCodeFeature
import platform.CoreImage.kCIFormatBGRA8
import platform.Foundation.NSData
import platform.Foundation.create

private val qrDetector: CIDetector? by lazy {
    CIDetector.detectorOfType(
        CIDetectorTypeQRCode,
        context = null,
        options = mapOf(CIDetectorAccuracy to CIDetectorAccuracyHigh)
    )
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual suspend fun decodeQrFromFrame(width: Int, height: Int, argb: IntArray): String? {
    if (width <= 0 || height <= 0 || argb.size < width * height) return null
    val detector = qrDetector ?: return null
    val data = argb.usePinned { pinned ->
        NSData.create(
            bytes = pinned.addressOf(0),
            length = (width * height * 4).toULong()
        )
    }
    val colorSpace = CGColorSpaceCreateDeviceRGB() ?: return null
    return try {
        val ciImage = CIImage.imageWithBitmapData(
            data = data,
            bytesPerRow = (width * 4).toULong(),
            size = CGSizeMake(width.toDouble(), height.toDouble()),
            format = kCIFormatBGRA8,
            colorSpace = colorSpace
        )
        val feature = detector.featuresInImage(ciImage).firstOrNull() as? CIQRCodeFeature
        feature?.messageString
    } finally {
        CGColorSpaceRelease(colorSpace)
    }
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual suspend fun decodeQrFromImageBytes(bytes: ByteArray): String? {
    if (bytes.isEmpty()) return null
    val detector = qrDetector ?: return null
    val data = bytes.usePinned { pinned ->
        NSData.create(
            bytes = pinned.addressOf(0),
            length = bytes.size.toULong()
        )
    }
    val ciImage = CIImage.imageWithData(data) ?: return null
    val feature = detector.featuresInImage(ciImage).firstOrNull() as? CIQRCodeFeature
    return feature?.messageString
}

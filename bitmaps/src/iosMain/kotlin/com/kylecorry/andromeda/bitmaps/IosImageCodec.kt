@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.kylecorry.andromeda.bitmaps

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.CoreGraphics.*
import platform.Foundation.NSData
import platform.Foundation.create
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImagePNGRepresentation
import platform.posix.memcpy

actual fun decodeImage(bytes: ByteArray): PixelImage {
    require(bytes.isNotEmpty()) { "Unable to decode image" }
    val data = bytes.usePinned { NSData.create(bytes = it.addressOf(0), length = bytes.size.toULong()) }
    val image = CGImageRetain(requireNotNull(UIImage.imageWithData(data)?.CGImage) { "Unable to decode image" })
        ?: error("Unable to retain image")
    try { return pixelImageFromCGImage(image) } finally { CGImageRelease(image) }
}

internal fun pixelImageFromCGImage(image: CGImageRef): PixelImage {
    val width = CGImageGetWidth(image).toInt()
    val height = CGImageGetHeight(image).toInt()
    require(width > 0 && height > 0 && width.toLong() * height <= Int.MAX_VALUE / 4)
    val rgba = ByteArray(width * height * 4)
    val space = requireNotNull(CGColorSpaceCreateDeviceRGB())
    try {
        rgba.usePinned { pinned ->
            val context = requireNotNull(CGBitmapContextCreate(pinned.addressOf(0), width.toULong(), height.toULong(), 8u, (width * 4).toULong(), space, CGImageAlphaInfo.kCGImageAlphaPremultipliedLast.value or kCGBitmapByteOrder32Big))
            try { CGContextDrawImage(context, CGRectMake(0.0, 0.0, width.toDouble(), height.toDouble()), image) } finally { CGContextRelease(context) }
        }
    } finally { CGColorSpaceRelease(space) }
    return PixelImage(width, height, IntArray(width * height) { i ->
        val offset = i * 4
        val alpha = rgba[offset + 3].toInt() and 255
        fun channel(index: Int): Int = if (alpha == 0) 0 else (((rgba[offset + index].toInt() and 255) * 255 + alpha / 2) / alpha).coerceAtMost(255)
        (alpha shl 24) or (channel(0) shl 16) or (channel(1) shl 8) or channel(2)
    })
}

actual fun PixelImage.encodePng(): ByteArray = encode { UIImagePNGRepresentation(it) }
actual fun PixelImage.encodeJpeg(quality: Int): ByteArray {
    require(quality in 0..100)
    return encode { UIImageJPEGRepresentation(it, quality / 100.0) }
}

private fun PixelImage.encode(convert: (UIImage) -> NSData?): ByteArray {
    require(pixels.size <= Int.MAX_VALUE / 4)
    val rgba = ByteArray(pixels.size * 4)
    pixels.forEachIndexed { i, color ->
        val alpha = color ushr 24
        for (channel in 0..2) {
            val shift = 16 - channel * 8
            rgba[i * 4 + channel] = ((((color ushr shift) and 255) * alpha + 127) / 255).toByte()
        }
        rgba[i * 4 + 3] = alpha.toByte()
    }
    val space = requireNotNull(CGColorSpaceCreateDeviceRGB())
    return try {
        rgba.usePinned { pinned ->
            val context = requireNotNull(CGBitmapContextCreate(pinned.addressOf(0), width.toULong(), height.toULong(), 8u, (width * 4).toULong(), space, CGImageAlphaInfo.kCGImageAlphaPremultipliedLast.value or kCGBitmapByteOrder32Big))
            try {
                val image = requireNotNull(CGBitmapContextCreateImage(context))
                try {
                    val data = requireNotNull(convert(UIImage.imageWithCGImage(image))) { "Unable to encode image" }
                    require(data.length <= Int.MAX_VALUE.toULong())
                    ByteArray(data.length.toInt()).also { output ->
                        if (output.isNotEmpty()) output.usePinned { memcpy(it.addressOf(0), data.bytes, data.length) }
                    }
                } finally { CGImageRelease(image) }
            } finally { CGContextRelease(context) }
        }
    } finally { CGColorSpaceRelease(space) }
}

@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.kylecorry.andromeda.bitmaps

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.CoreGraphics.*
import platform.Foundation.NSData
import platform.Foundation.create
import platform.UIKit.UIImage

actual class ImageRegionDecoder actual constructor(bytes: ByteArray) : AutoCloseable {
    private val image: CGImageRef
    private var closed = false
    actual val width: Int
    actual val height: Int

    init {
        require(bytes.isNotEmpty()) { "Unable to decode image" }
        val data = bytes.usePinned { NSData.create(bytes = it.addressOf(0), length = bytes.size.toULong()) }
        image = CGImageRetain(requireNotNull(UIImage.imageWithData(data)?.CGImage) { "Unable to decode image" })
            ?: error("Unable to retain image")
        width = CGImageGetWidth(image).toInt()
        height = CGImageGetHeight(image).toInt()
    }

    actual fun decode(left: Int, top: Int, width: Int, height: Int): PixelImage {
        check(!closed) { "Image decoder is closed" }
        requireImageRegion(left, top, width, height, this.width, this.height)
        val region = requireNotNull(CGImageCreateWithImageInRect(image, CGRectMake(left.toDouble(), top.toDouble(), width.toDouble(), height.toDouble()))) { "Unable to decode image region" }
        return try { pixelImageFromCGImage(region) } finally { CGImageRelease(region) }
    }

    actual override fun close() {
        if (!closed) { closed = true; CGImageRelease(image) }
    }
}

@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.kylecorry.andromeda.bitmaps

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.CoreGraphics.*
import platform.Foundation.NSData
import platform.Foundation.create
import platform.UIKit.UIImage

private fun retainedImage(image: UIImage?): CGImageRef =
    CGImageRetain(requireNotNull(image?.CGImage) { "Unable to decode image" }) ?: error("Unable to retain image")

private fun imageFromBytes(bytes: ByteArray): CGImageRef {
    require(bytes.isNotEmpty()) { "Unable to decode image" }
    val data = bytes.usePinned { NSData.create(bytes = it.addressOf(0), length = bytes.size.toULong()) }
    return retainedImage(UIImage.imageWithData(data))
}

public actual class ImageRegionDecoder private constructor(private val image: CGImageRef) : AutoCloseable {
    public actual constructor(bytes: ByteArray) : this(imageFromBytes(bytes))
    public actual constructor(path: String) : this(retainedImage(UIImage.imageWithContentsOfFile(path)))
    private var closed = false
    public actual val width: Int = CGImageGetWidth(image).toInt()
    public actual val height: Int = CGImageGetHeight(image).toInt()

    public actual fun decode(left: Int, top: Int, width: Int, height: Int): PixelImage {
        check(!closed) { "Image decoder is closed" }
        requireImageRegion(left, top, width, height, this.width, this.height)
        val region = requireNotNull(CGImageCreateWithImageInRect(image, CGRectMake(left.toDouble(), top.toDouble(), width.toDouble(), height.toDouble()))) { "Unable to decode image region" }
        return try { pixelImageFromCGImage(region) } finally { CGImageRelease(region) }
    }

    actual override fun close() {
        if (!closed) { closed = true; CGImageRelease(image) }
    }
}

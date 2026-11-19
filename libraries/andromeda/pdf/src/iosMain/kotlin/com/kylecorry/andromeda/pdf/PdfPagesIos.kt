@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.kylecorry.andromeda.pdf

import com.kylecorry.andromeda.bitmaps.PixelImage
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import platform.CoreFoundation.CFDataCreate
import platform.CoreFoundation.CFRelease
import platform.CoreGraphics.*
import platform.Foundation.NSLock

public actual class PdfPages actual constructor(bytes: ByteArray) : AutoCloseable {
    private val lock = NSLock()
    private val document: CGPDFDocumentRef
    private var closed = false

    init {
        require(bytes.isNotEmpty()) { "PDF is empty" }
        val data = bytes.usePinned { CFDataCreate(null, it.addressOf(0).reinterpret(), bytes.size.toLong()) }
            ?: error("Unable to allocate PDF data")
        try {
            val provider = requireNotNull(CGDataProviderCreateWithCFData(data))
            try { document = requireNotNull(CGPDFDocumentCreateWithProvider(provider)) { "Unable to open PDF" } }
            finally { CGDataProviderRelease(provider) }
        } finally { CFRelease(data) }
    }

    private inline fun <T> locked(block: () -> T): T {
        lock.lock()
        try { check(!closed) { "PDF is closed" }; return block() } finally { lock.unlock() }
    }
    private fun pageAt(index: Int): CGPDFPageRef =
        requireNotNull(CGPDFDocumentGetPage(document, (index + 1).toULong())) { "Invalid PDF page" }

    public actual val pageCount: Int get() = locked { CGPDFDocumentGetNumberOfPages(document).toInt() }
    public actual fun pageSize(page: Int): PdfPageSize = locked {
        val source = pageAt(page)
        val box = CGPDFPageGetBoxRect(source, kCGPDFCropBox)
        val (width, height) = box.useContents { size.width.toFloat() to size.height.toFloat() }
        if (CGPDFPageGetRotationAngle(source) % 180 == 0) PdfPageSize(width, height) else PdfPageSize(height, width)
    }
    public actual fun render(page: Int, width: Int, height: Int, background: Int): PixelImage =
        renderRegion(page, width, height, 0.0, 0.0, 1.0, 1.0, background)

    public actual fun renderRegion(
        page: Int, width: Int, height: Int,
        left: Double, top: Double, right: Double, bottom: Double, background: Int
    ): PixelImage = locked {
        requirePageRegion(left, top, right, bottom)
        val pageWidth = width / (right - left)
        val pageHeight = height / (bottom - top)
        requireRenderSize(width, height)
        val source = pageAt(page)
        val rgba = ByteArray(width * height * 4)
        val space = requireNotNull(CGColorSpaceCreateDeviceRGB())
        try {
            rgba.usePinned { pinned ->
                val context = requireNotNull(CGBitmapContextCreate(pinned.addressOf(0), width.toULong(), height.toULong(), 8u,
                    (width * 4).toULong(), space, CGImageAlphaInfo.kCGImageAlphaPremultipliedLast.value or kCGBitmapByteOrder32Big))
                try {
                    CGContextSetRGBFillColor(context, ((background ushr 16) and 255) / 255.0,
                        ((background ushr 8) and 255) / 255.0, (background and 255) / 255.0, (background ushr 24) / 255.0)
                    CGContextFillRect(context, CGRectMake(0.0, 0.0, width.toDouble(), height.toDouble()))
                    val transform = CGPDFPageGetDrawingTransform(source, kCGPDFCropBox,
                        CGRectMake(-left * pageWidth, -(1.0 - bottom) * pageHeight, pageWidth, pageHeight), 0, false)
                    CGContextConcatCTM(context, transform)
                    CGContextDrawPDFPage(context, source)
                } finally { CGContextRelease(context) }
            }
        } finally { CGColorSpaceRelease(space) }
        PixelImage(width, height, IntArray(width * height) { index ->
            val offset = index * 4
            val alpha = rgba[offset + 3].toInt() and 255
            fun channel(i: Int): Int = if (alpha == 0) 0 else (((rgba[offset + i].toInt() and 255) * 255 + alpha / 2) / alpha).coerceAtMost(255)
            (alpha shl 24) or (channel(0) shl 16) or (channel(1) shl 8) or channel(2)
        })
    }
    actual override fun close() {
        lock.lock()
        try { if (!closed) { closed = true; CGPDFDocumentRelease(document) } } finally { lock.unlock() }
    }
}
